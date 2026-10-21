# Implementation status — October 20, 2026

| System | Implementation | Verification |
|---|---|---|
| Independent GitHub main | Active commits and workflows on main | Remote branch-head and Actions evidence verified |
| Asset catalog | 633 Drive source references (33 initial + 600 additional observed), including converted GLBs, textures and source meshes; capped folders still partial | Drive folder/file presence observed; no asset bytes imported |
| City | 132 x 132 seeded grid, road intersections, walled lots, doors and courtyards | Seven seeded city checks executed PASS on Windows and Linux |
| Rendering | Java2D 320 x 200 raycaster and alpha-composited remote players | Headless Swing paint PASS; PNG screenshot artifact archived (not human UI playtest) |
| Combat | Host-owned handgun/shotgun damage, ammo, obstruction and NPCs | Host rules and localhost socket combat suites executed PASS |
| Gameplay | Class choice, host-computed visibility, independent health/sanity and inventory | Host mutation, opacity and pickup tests executed PASS |
| Deployables | Host-owned turret, healing, automatic revival, barricade | Functional effects and construction tests executed PASS |
| Respawn | Three sectors, death lockout and host-owned state | Deterministic respawn test executed PASS |
| LAN | Protocol v2, host combat, opacity, items and construction | Real TCP localhost host-client test PASS; internet security not validated |
| AI | Host-controlled NPC pursuit and damage, streamed to peers | Runtime game-host simulation exercised; detailed NPC behavior stress pending |
| Source licenses | PSX Mega Pack purchase rights noted in shared index | Other packs pending verification |
| Release | GitHub Actions produces executable .jar | Test JAR and screenshot artifacts uploaded; no installer, human Windows playtest or archival GLB integration |

**Verified CI baseline:** [Actions run 39532114037](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39532114037) completed successfully across Ubuntu/Windows and JDK 17/21, including real Java compilation, deterministic city tests, 46 arena assertions, real localhost PvP, headless Swing screenshot rendering and JAR artifact. [Multiplayer soak run 39532113979](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39532113979) also passed. These tests do **not** imply a physical Windows GUI playtest, production security, full asset integration or large-scale server performance proof.

### Immediate defects/limitations to resolve

1. Host-authoritative movement has basic collision/speed limits, but still trusts stance and lacks reconciliation/prediction, high-ping interpolation, authenticated identity and adversarial netcode tests.
2. Procedural map currently finite 132 x 132; no streamed large city tiles, interiors are skeletal and no actual URBAN source geometry yet.
3. Revival stations now actually revive nearby fallen players after delay; team targeting, station ownership, balance and persistent profiles remain to implement.
4. Software raycaster does not parse GLB. Need format ingestion/adapter, materials, floors/ceilings and collision import to represent archived urban sets.
5. CI tests are green, but physical Windows input/gameplay and larger hosted matches still require playtesting.
