# Design: HUD reward choice box

## Network flow

```
wave cleared (server)
  → onWaveComplete(): generate 3 choices per player (seeded)
  → chat announcement (fallback/log) + RewardChoicesPacket(wave, choices) → each player.con
client: RewardChoicesPacket.handleClient() → ClientRewardState.set(wave, choices)
  → RewardChoicePanel becomes visible, renders 3 icon buttons
hover a button → tooltip label shows displayName + description (rarity-colored)
click a button → RewardVotePacket(index) → net.send(packet, true)   [client→server]
server: RewardVotePacket.handleServer(con) → serverController.handleVote(player, index, player)
  → applies the reward, broadcasts the choice in chat as before
all voted → startWave() → broadcast RewardChoicesPacket(wave, []) to clear the boxes
stopRun() → same empty broadcast
```

## Packets

`Net.registerPacket(...)` in the mod constructor; dispatch by overriding
`handleClient()` / `handleServer(NetConnection)` (same pattern as the old
`SchematicUnlockPacket`).

- `RewardChoicesPacket`: `write`: `i(wave)`, `i(count)`, then `Reward.write` per
  choice. `read` mirrors. `handleClient` → `ClientRewardState.instance.set(...)`.
- `RewardVotePacket`: `i(index)`. `handleServer` → `MindTheIndustryMod.serverController`
  null-guarded → `handleVote(con.player, index, con.player)`.

Sending server→one player: `player.con.send(packet, true)` (null-guarded for
headless tests where `con == null`). Client→server: `net.send(packet, true)`.

## Client state (`client/ClientRewardState`)

```java
public class ClientRewardState{
    public static final ClientRewardState instance = new ClientRewardState();
    public int wave;
    public Seq<Reward> choices = new Seq<>();
    public boolean visible;
    public void set(int wave, Seq<Reward> choices){...}
    public void clear(){...}
}
```

Pure data — unit-testable headless.

## Panel (`ui/RewardChoicePanel extends Table`)

Added once in `Events.on(ClientLoadEvent.class, ...)` via `Vars.ui.hudGroup.addChild(panel)`.
Positioned top-center under the wave HUD (`setPosition`/`align(Align.top)` in a
container, or `table.top()` inside hudGroup with `setFillParent(true)` and
`align(Align.top | Align.center)` — must not block input: `setTouchable(Touchable.enabled)`
on the buttons only; the panel background uses `Touchable.childrenOnly`).

- `act()`: `setVisible(ClientRewardState.instance.visible && (net.client() || net.server()))`;
  rebuild buttons when `wave` changes.
- Each choice → `Button` with `Image(icon).size(40)`, a 4px rarity-color bar
  (`Image` tinted with the rarity color), `ClickListener` → vote, `InputListener`
  enter/exit → tooltip label.
- Icon resolution: TRADEOFF → `Icon.chartBar`; PLAN → first plan content's
  `uiIcon` (fallback `Icon.hammer`); RESOURCE → first item's `uiIcon`
  (fallback `Icon.distribution`).
- Rarity color: `Rarity` gains a `Color hex` field alongside the chat markup.

## Server guarantee (solo + multi)

Mindustry solo runs an integrated server, so `ServerLoadEvent` fires and
`net.server()` is true; hosted/dedicated games fire it on the host. The mod
keeps three init paths: `ServerLoadEvent`, `PlayEvent` fallback,
`WorldLoadEvent` fallback — first one wins. `MindTheIndustryMod.serverController`
is the single access point for packets/commands.

## Tests (headless)

- `MindTheIndustryPacketsTest`: write→read round-trip of both packets
  (choices preserve id/type/rarity/specific fields; vote preserves index).
- `ClientRewardStateTest`: set/clear semantics, visibility flag.
- Existing controller tests keep passing (`handleVote` unchanged; packet send
  is null-guarded when `player.con == null`).
- The panel itself is `arc.scene` UI — not headless-testable; it only renders
  `ClientRewardState`.
