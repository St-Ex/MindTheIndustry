package rogueindustry.ui;

import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * HUD de compte à rebours affiché avant la première vague.
 * Gros chiffre au centre de l'écran qui décompte.
 */
public class CountdownHUD{

    private static Table overlay;
    private static float remaining;
    private static boolean visible = false;

    /** Affiche le compte à rebours. */
    public static void show(int seconds){
        remaining = seconds;
        visible = true;

        if(overlay == null){
            overlay = new Table();
            overlay.fillParent = true;
            overlay.touchable = Touchable.enabled;
            overlay.center();
        }

        if(!overlay.hasParent()){
            ui.hudGroup.addChild(overlay);
        }

        updateLabel();

        // Timer de mise à jour
        Timer.schedule(() -> {
            if(!visible) return;
            remaining -= 1f;
            if(remaining <= 0){
                visible = false;
                overlay.remove();
            }else{
                updateLabel();
            }
        }, 0f, 1f);
    }

    private static void updateLabel(){
        if(overlay == null) return;
        overlay.clear();

        int secs = (int)Math.ceil(remaining);

        if(secs > 0){
            overlay.label(() -> String.valueOf(secs))
                .fontScale(8f)
                .color(secs > 3 ? Color.white : Color.orange)
                .row();
            overlay.label(() -> "Première vague dans...")
                .fontScale(1.5f)
                .color(Color.lightGray);
        }else{
            overlay.label(() -> "GO!")
                .fontScale(8f)
                .color(Color.green);
        }
    }

    /** Cache le HUD. */
    public static void hide(){
        visible = false;
        if(overlay != null) overlay.remove();
    }
}
