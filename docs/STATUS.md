# Implementation status — October 20, 2026

| System | Implementation | Verification |
|---|---|---|
| Independent GitHub main | Three completed source commits | Remote branch-head and full-tree read-back observed |
| Asset catalog | 33 exact Drive locators, including 19 converted GLBs | Drive folder/file presence observed; no asset bytes imported |
| City | 132 x 132 seeded grid, road intersections, walled lots, doors and courtyards | Source present, headless source test committed, no executed JVM test |
| Rendering | Java2D 320 x 200 raycaster in scalable Swing view, primitive actor billboards | Source present, no UI frame or visual proof |
| Combat | Handgun, shotgun, range/obstruction, reserves/reload, NPC hostiles | Source present, no runtime proof |
| Gameplay | Class choice, stealth visibility jewel, health/sanity, cash/supplies, pickups | Source present, no runtime proof |
| Deployables | Turret, healing station, revive-station placeholder | Turret/heal local-only; revive-station deployment does NOT enable true revive yet |
| Respawn | Three distinct selectable world sectors on death | Source present, untested |
| LAN | Server socket, map seed, peer updates, shot validation, damage relay | Source present, unversioned, untested, PvP partial only |
| AI | Hostiles chase, attack and occasionally roam | Per-client local simulation only, not server-authoritative |
| Source licenses | PSX Mega Pack purchase rights noted in shared index | Other packs pending verification |
| Release | None | No packaged installer or asset-integrated build |

**No Java compile, automated test, in-game screenshot, multiplayer connection test, performance test, or Windows release has been executed in this connector-only run.** The included `play.sh`, `play.bat`, `WorldSelfTest` and `LanSmokeTest` make these concrete local gates.

### Immediate defects/limitations to resolve

1. Network owner: authoritative movement/collision, weapons/ammo, death/revive, NPCs, world loot, deployed devices.
2. Procedural map currently finite 132 x 132; no streamed large city tiles, interiors are skeletal and no actual URBAN source geometry yet.
3. Revive stations are placeable but have no resurrection service; no persistence profiles.
4. Software raycaster does not parse GLB. Need format ingestion/adapter, materials, floors/ceilings and collision import to represent archived urban sets.
5. User interface and collision/playability require real Windows/JDK 17 sessions; tests currently unexecuted.
