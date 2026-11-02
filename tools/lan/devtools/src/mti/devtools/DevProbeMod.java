package mti.devtools;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.actions.Actions;
import arc.scene.ui.Label;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.mod.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Dev-only client probe for the LAN UI-validation stack (tools/lan/README.md).
 * Not part of the shipped mod; installed by lan:up into the LAN run dirs only.
 *
 * Protocol (file-based IPC inside the instance's MINDUSTRY_DATA_DIR):
 *   write a command to <data>/mti-probe:
 *     "dump"              -> walk the scene graph, write <data>/mti-scene.json
 *     "highlight <text>"  -> blink matched visible elements red/white, and
 *                            write their bounds to mti-scene.json
 *     "clear"             -> remove highlight actions, write mti-scene.json
 *   The probe file is cleared after each command. The watcher polls every 300ms,
 *   so: mise lan:probe p1 dump && sleep 1 && mise lan:scene p1
 *
 * Coordinates: "x,y,w,h" are stage coords (y-up, origin bottom-left);
 * "wx,wy" are window-relative y-down coords matching tools/lan/input.py.
 */
public class DevProbeMod extends Mod{
    private Fi probe, out, heartbeat;
    private long lastStamp = -1;
    private String lastCmd = "";
    private final Seq<Element> highlighted = new Seq<>();
    private final Map<Element, Color> originalColors = new HashMap<>();

    @Override
    public void init(){
        if(Vars.headless) return;
        probe = Vars.dataDirectory.child("mti-probe");
        out = Vars.dataDirectory.child("mti-scene.json");
        heartbeat = Vars.dataDirectory.child("mti-probe-heartbeat");
        if(!probe.exists()) probe.writeString("");

        Thread watcher = new Thread(() -> {
            while(true){
                try{
                    long stamp = new java.io.File(probe.absolutePath()).lastModified();
                    String cmd = probe.exists() ? probe.readString().trim() : "";
                    heartbeat.writeString(String.valueOf(System.currentTimeMillis()));
                    // trigger on mtime change OR on new non-empty content (mtime
                    // granularity can miss rapid successive commands)
                    if(!cmd.isEmpty() && (stamp != lastStamp || !cmd.equals(lastCmd))){
                        lastStamp = stamp;
                        lastCmd = cmd;
                        probe.writeString("");
                        handle(cmd);
                    }
                    Thread.sleep(250);
                }catch(Throwable ignored){
                    // keep watching
                }
            }
        });
        watcher.setDaemon(true);
        watcher.setName("mti-devtools-probe");
        watcher.start();
    }

    private void doTap(int x, int y, boolean right, boolean yDown){
        arc.scene.Scene scene = Core.scene;
        int sy = yDown ? (int)(Core.graphics.getHeight() - y) : y;
        scene.mouseMoved(x, sy);
        arc.input.KeyCode btn = right ? arc.input.KeyCode.mouseRight : arc.input.KeyCode.mouseLeft;
        scene.touchDown(x, sy, 0, btn);
        scene.touchUp(x, sy, 0, btn);
    }

    private void handle(String cmd){
        // scene access must happen on the main thread
        Core.app.post(() -> {
            try{
                if(cmd.startsWith("highlight")){
                    String sel = cmd.length() > 9 ? cmd.substring(9).trim() : "";
                    int n = highlight(sel);
                    writeScene(sel, n);
                }else if(cmd.equals("clear")){
                    clearHighlights();
                    writeScene(null, 0);
                }else if(cmd.equals("root")){
                    writeRoot();
                }else if(cmd.startsWith("hit ")){
                    String[] parts = cmd.substring(4).trim().split("\\s+");
                    int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]);
                    int sy = (int)(Core.graphics.getHeight() - y);
                    Element hit = Core.scene.hit(x, sy, true);
                    StringBuilder h = new StringBuilder("{\"hit\":");
                    if(hit == null) h.append("null");
                    else h.append("{\"cls\":\"").append(esc(hit.getClass().getSimpleName()))
                        .append("\",\"name\":\"").append(esc(hit.name == null ? "" : hit.name))
                        .append("\",\"text\":\"").append(esc(String.valueOf(textOf(hit))))
                        .append("\",\"wx\":").append(hit.localToStageCoordinates(new Vec2(0, 0)).x)
                        .append(",\"w\":").append(hit.getWidth()).append('}');
                    out.writeString(h.append('}').toString());
                }else if(cmd.equals("state")){
                    out.writeString("{\"sector\":\"" + esc(String.valueOf(Vars.state.rules.sector)) + "\"}");
                }else if(cmd.equals("presets")){
                    StringBuilder p = new StringBuilder("{\"presets\":[");
                    boolean[] f = {true};
                    for(mindustry.type.SectorPreset sp : Vars.content.sectors()){
                        if(!f[0]) p.append(',');
                        f[0] = false;
                        p.append("{\"name\":\"").append(esc(sp.name))
                         .append("\",\"planet\":\"").append(esc(sp.sector == null ? "?" : sp.sector.planet.name))
                         .append("\",\"sector\":").append(sp.sector == null ? -1 : sp.sector.id).append('}');
                    }
                    out.writeString(p.append("]}").toString());
                }else if(cmd.startsWith("setname ")){
                    String n = cmd.substring(8).trim();
                    Core.settings.put("name", n);
                    Vars.player.name = n;
                    out.writeString("{\"name\":\"" + esc(n) + "\"}");
                }else if(cmd.equals("host")){
                    // same as HostDialog.runHost(): net.host on the configured port
                    if(Core.settings.getString("name").trim().isEmpty()){
                        Core.settings.put("name", "LAN" + (int)(Math.random() * 10000));
                    }
                    Vars.player.name = Core.settings.getString("name");
                    try{
                        int port = Core.settings.getInt("port", 6567);
                        Vars.net.host(port);
                        Vars.player.admin = true;
                        arc.Events.fire(new mindustry.game.EventType.HostEvent());
                        out.writeString("{\"hosting\":true,\"port\":" + port + ",\"name\":\"" + esc(Core.settings.getString("name")) + "\"}");
                    }catch(Exception e){
                        out.writeString("{\"error\":\"" + esc(String.valueOf(e)) + "\"}");
                    }
                }else if(cmd.startsWith("join ")){
                    // same sequence as JoinDialog.connect(ip, port)
                    String[] parts = cmd.substring(5).trim().split("\\s+");
                    if(Core.settings.getString("name").trim().isEmpty()){
                        Core.settings.put("name", "LAN" + (int)(Math.random() * 10000));
                    }
                    Vars.player.name = Core.settings.getString("name");
                    try{
                        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6567;
                        Vars.logic.reset();
                        Vars.net.reset();
                        Vars.netClient.beginConnecting();
                        Vars.net.connect(parts[0], port, () -> {});
                        out.writeString("{\"connecting\":\"" + esc(parts[0]) + ":" + port + "\"}");
                    }catch(Exception e){
                        out.writeString("{\"error\":\"" + esc(String.valueOf(e)) + "\"}");
                    }
                }else if(cmd.startsWith("play ")){
                    // launch a sector preset directly (bypasses the 3D planet globe,
                    // which is impractical to click on a software-rendered Xvfb)
                    String name = cmd.substring(5).trim();
                    if(Core.settings.getString("name").trim().isEmpty()){
                        Core.settings.put("name", "LAN" + (int)(Math.random() * 10000));
                    }
                    Vars.player.name = Core.settings.getString("name");
                    // match by preset name or by planet name
                    mindustry.type.SectorPreset preset = Vars.content.sectors().find(p ->
                        p.name.equals(name) || p.sector.planet.name.equals(name));
                    if(preset == null){
                        out.writeString("{\"error\":\"no sector preset '" + esc(name) + "'\"}");
                    }else{
                        Vars.control.playSector(preset.sector);
                        out.writeString("{\"playing\":\"" + esc(name) + "\",\"planet\":\"" +
                            esc(String.valueOf(preset.sector.planet.name)) + "\"}");
                    }
                }else if(cmd.startsWith("tap ") || cmd.startsWith("rtap ")){
                    boolean right = cmd.startsWith("rtap");
                    String[] parts = cmd.substring(4).trim().split("\\s+");
                    int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]);
                    doTap(x, y, right, false);
                    writeScene(null, -1);
                }else if(cmd.startsWith("tapc ") || cmd.startsWith("rtapc ")){
                    // window-relative y-down coords (same as the dump's wx/wy and
                    // as screenshots); converted to stage y-up internally
                    boolean right = cmd.startsWith("rtapc");
                    String[] parts = cmd.substring(5).trim().split("\\s+");
                    int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]);
                    doTap(x, y, right, true);
                    writeScene(null, -1);
                }else if(cmd.startsWith("key ")){
                    arc.input.KeyCode k = arc.input.KeyCode.valueOf(cmd.substring(4).trim());
                    Core.scene.keyDown(k);
                    Core.scene.keyUp(k);
                    writeScene(null, -1);
                }else{
                    writeScene(null, -1);
                }
            }catch(Throwable t){
                out.writeString("{\"error\":\"" + esc(String.valueOf(t)) + "\"}");
            }
        });
    }

    private int highlight(String sel){
        clearHighlights();
        if(sel.isEmpty()) return 0;
        int[] count = {0};
        arc.func.Cons<Element> cons = e -> {
            if(effectiveVisible(e) && matches(e, sel)){
                originalColors.putIfAbsent(e, e.color.cpy());
                e.actions(Actions.forever(Actions.sequence(
                    Actions.color(Color.red, 0.15f),
                    Actions.color(Color.white, 0.15f))));
                highlighted.add(e);
                count[0]++;
            }
        };
        walk(Core.scene.root, 0, cons::get);
        if(Core.scene.hasDialog()) walk(Core.scene.getDialog(), 0, cons::get);
        return count[0];
    }

    private void clearHighlights(){
        for(Element e : highlighted){
            e.clearActions();
            Color orig = originalColors.get(e);
            if(orig != null) e.setColor(orig);
        }
        highlighted.clear();
    }

    private boolean matches(Element e, String sel){
        String s = sel.toLowerCase();
        if(e.name != null && e.name.toLowerCase().contains(s)) return true;
        // match on the element's own label only (direct child), not ancestors
        String text = null;
        if(e instanceof Label) text = ((Label)e).getText().toString();
        else if(e instanceof Group){
            String[] t = {null};
            ((Group)e).forEach(c -> {
                if(t[0] == null && c instanceof Label) t[0] = ((Label)c).getText().toString();
            });
            text = t[0];
        }
        return text != null && !text.isEmpty() && text.toLowerCase().contains(s);
    }

    /** Debug: list the direct children of scene.root (and the current dialog). */
    private void writeRoot(){
        StringBuilder sb = new StringBuilder("{\"rootChildren\":[");
        boolean[] first = {true};
        Core.scene.root.forEach(c -> {
            if(!first[0]) sb.append(',');
            first[0] = false;
            int[] n = {0};
            if(c instanceof Group) ((Group)c).forEach(ignored -> n[0]++);
            sb.append("{\"cls\":\"").append(esc(c.getClass().getSimpleName()))
              .append("\",\"name\":\"").append(esc(c.name == null ? "" : c.name))
              .append("\",\"children\":").append(n[0])
              .append(",\"visible\":").append(c.visible).append('}');
        });
        sb.append("],\"hasDialog\":").append(Core.scene.hasDialog());
        if(Core.scene.hasDialog()){
            arc.scene.ui.Dialog d = Core.scene.getDialog();
            sb.append(",\"dialog\":\"").append(esc(d.getClass().getSimpleName()))
              .append("\",\"dialogShown\":").append(d.isShown())
              .append(",\"dialogVisible\":").append(d.visible);
        }
        sb.append('}');
        out.writeString(sb.toString());
    }

    private void writeScene(String highlightSel, int highlightCount){
        StringBuilder sb = new StringBuilder();
        sb.append("{\"gameW\":").append(Core.graphics.getWidth())
          .append(",\"gameH\":").append(Core.graphics.getHeight());
        if(highlightSel != null){
            sb.append(",\"highlight\":\"").append(esc(highlightSel))
              .append("\",\"highlightCount\":").append(highlightCount);
        }
        sb.append(",\"elements\":[");
        float gh = Core.graphics.getHeight();
        float gw = Core.graphics.getWidth();
        boolean[] first = {true};
        int[] count = {0};
        // walk the current modal dialog FIRST (when open it is what matters),
        // then the scene root; the cap must not be consumed by HUD noise
        if(Core.scene.hasDialog()){
            walk(Core.scene.getDialog(), 0, e -> writeElement(sb, first, count, e, gh, gw));
        }
        walk(Core.scene.root, 0, e -> writeElement(sb, first, count, e, gh, gw));
        sb.append("]}");
        out.writeString(sb.toString());
    }

    private void writeElement(StringBuilder sb, boolean[] first, int[] count, Element e, float gh, float gw){
        // only visible, on-screen, non-zero-area elements: hidden fragments
        // parked at the origin would otherwise flood the dump
        if(count[0] >= 60000 || !effectiveVisible(e) || (e.getWidth() <= 0 || e.getHeight() <= 0)) return;
        Vec2 pos = e.localToStageCoordinates(new Vec2(0, 0));
        float w = e.getWidth(), h = e.getHeight();
        if(pos.x < -1 || pos.y < -1 || pos.x + w > gw + 1 || pos.y + h > gh + 1) return;
        String text = textOf(e);
        if(text != null && text.length() > 200) text = text.substring(0, 200) + "...";
        if(!first[0]) sb.append(',');
        first[0] = false;
        count[0]++;
        sb.append("{\"cls\":\"").append(esc(e.getClass().getSimpleName()))
          .append("\",\"name\":\"").append(esc(e.name == null ? "" : e.name))
          .append("\",\"text\":\"").append(esc(text == null ? "" : text))
          .append("\",\"visible\":").append(effectiveVisible(e))
          .append(",\"x\":").append(pos.x)
          .append(",\"y\":").append(pos.y)
          .append(",\"w\":").append(w)
          .append(",\"h\":").append(h)
          .append(",\"wx\":").append(pos.x)
          .append(",\"wy\":").append(gh - pos.y - h)
          .append('}');
    }

    private interface ConsE{ void get(Element e); }

    private void walk(Element e, int depth, ConsE cons){
        if(depth > 16) return;
        cons.get(e);
        if(e instanceof Group){
            ((Group)e).forEach(child -> walk(child, depth + 1, cons));
        }
    }

    private boolean effectiveVisible(Element e){
        Element cur = e;
        while(cur != null){
            if(!cur.visible) return false;
            // dialogs can stay in the scene with visible=true but not shown
            if(cur instanceof arc.scene.ui.Dialog && !((arc.scene.ui.Dialog)cur).isShown()) return false;
            cur = cur.parent;
        }
        return true;
    }

    /** First non-empty Label text among the element's close descendants. */
    private String textOf(Element e){
        return textOf(e, 0);
    }

    private String textOf(Element e, int depth){
        if(depth > 4) return null;
        if(e instanceof Label){
            String t = ((Label)e).getText().toString();
            if(!t.isEmpty()) return t;
        }
        String[] found = {null};
        if(e instanceof Group){
            ((Group)e).forEach(child -> {
                if(found[0] == null) found[0] = textOf(child, depth + 1);
            });
        }
        return found[0];
    }

    private static String esc(String s){
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
