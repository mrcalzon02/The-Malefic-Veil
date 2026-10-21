# Implementation status — October 20, 2026

| System | Implementation | Verification |
|---|---|---|
| Independent GitHub main | Active commits and workflows on main | Remote branch-head and Actions evidence verified |
| Asset catalog | 633 Drive source references; 124 original CC0 Kenney GLBs + 22 source PNGs in complete ZIP; 42 placed city model types | Complete ZIP source-readback, geometry tests and packaged-JAR tests |
| City | 132 x 132 seeded grid, road intersections, walled lots, doors and courtyards | Seven seeded city checks executed PASS on Windows and Linux |
| Rendering | Java2D raycast, original Kenney GLB triangles, UV-centroid source texels, 18 authored class/weapon/item/device models, remote mesh alpha | Cross-platform material/mesh regressions, headless render, virtual desktop input; manual Windows acceptance pending |
| Combat | Host-owned handgun/shotgun damage, ammo, obstruction and NPCs | Host rules and localhost socket combat suites executed PASS |
| Gameplay | Class choice, host-computed visibility, independent health/sanity and inventory | Host mutation, opacity and pickup tests executed PASS |
| Deployables | Host-owned turret, healing, automatic revival and barricade; spawn clearance, named rejection and atomic costs | Host functional/concurrency checks and 20-seed build regression PASS |
| Respawn | Three selectable sectors with threat-score labels, death lockout and brief nonoffensive spawn protection | 20 seeds × 100 threat permutations in SpawnSafetyTest; CI PASS |
| LAN | Protocol v3, correlated host shot receipts, anti-replay IDs, authoritative items/builds, heartbeat/RTT and explicit disconnect UI | Actual localhost TCP tests PASS; packet-loss reconciliation, authenticated reconnect and internet security remain open |
| AI | Host-controlled NPC pursuit and damage, streamed to peers | Runtime game-host simulation exercised; detailed NPC behavior stress pending |
| Source licenses | PSX Mega Pack purchase rights noted in shared index | Other packs pending verification |
| Release | GitHub Actions produces executable .jar with complete CC0 urban ZIP and authored PSX gameplay models | Cross-platform JAR/source tests and screenshots; no installer, rigged-character import or physical user acceptance |

**Verified CI baseline:** [Actions run 39532114037](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39532114037) completed successfully across Ubuntu/Windows and JDK 17/21, including real Java compilation, deterministic city tests, 46 arena assertions, real localhost PvP, headless Swing screenshot rendering and JAR artifact. [Multiplayer soak run 39532113979](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39532113979) also passed. These tests do **not** imply a physical Windows GUI playtest, production security, full asset integration or large-scale server performance proof.

### Immediate defects/limitations to resolve

1. Host-authoritative movement has basic collision/speed limits, but still trusts stance and lacks reconciliation/prediction, high-ping interpolation, authenticated identity and adversarial netcode tests.
2. Procedural map currently finite 132 x 132; no streamed large city tiles, interiors are skeletal and no actual URBAN source geometry yet.
3. Revival stations now actually revive nearby fallen players after delay; team targeting, station ownership, balance and persistent profiles remain to implement.
4. All 124 CC0 GLBs now parse; static UV-centroid texel selection and 18 authored gameplay models run in real game. Per-pixel UV, complete occlusion, floors/ceilings, rigged source characters and animations remain missing.
5. Real Swing keyboard/mouse automation passes under a virtual Linux desktop, but two physical Windows clients, accessibility and large hosted matches still require human playtests. Automatic reconnect is not implemented.

## Current development increment — verified archive intake / urban model code

Retrieved **Kenney Retro Urban Kit 2.0** archive and its `License.txt` directly from shared Drive. License states Creative Commons Zero (CC0), including commercial use. The specific `road-asphalt-straight.glb` source SHA-256 matched the separately staged Drive GLB exactly (`33d5a25df63fdbfd696564972326de0dc7bdcf100649b8beb7bcd4dffec19c4e`). Five selected GLB originals are now source files in `assets/game/kenney`; the runtime Java loader extracts their **actual mesh triangles**, projecting them into the first-person scene with provisional flat material colors. The currently supported subset does **not** sample texture PNGs, evaluate animations, skinning or node hierarchies, or supply geometric physics for decorative props. That is an explicit R1 follow-up, not a completed claim.

The authoritative release-gated phase plan is in [ROADMAP.md](ROADMAP.md). GitHub Actions now must verify GLB geometry and rendered framebuffer, and test packaged-JAR classpath asset access. Human desktop playtesting remains an independent gate.

## QoL host correctness increment — October 20, 2026

The release-blocking [player QoL contract](PLAYER_QOL_MUST_HAVES.md) is now being implemented in the active game:

- **QX-01 (partial):** client shot sequence IDs; host-confirmed `SHOT` replies carry accepted/miss/rejection, target, damage and remaining ammo. Duplicate requests are rejected without new damage or ammo deductions. Unreliable/high-latency hit compensation and frame-by-frame forensic playback remain future gates.
- **QX-02 (partial):** three selectable spawn sectors scored for players, hostile NPCs, turrets, recent early deaths and walkability. Risk labels are displayed to dead clients. Respawns receive 1.2 seconds of *nonoffensive* protection, canceled by firing or leaving the immediate arrival zone. Full ranked spawn alternatives and 10-death anti-camp human checks remain open.
- **QX-03 (partial):** authoritative construction reviews explain resource shortages, blocked geometry, nearby devices, protected spawn areas and closed spawn-to-spawn routes. Cost deductions, placement and device index creation are one synchronized host transaction; the multithreaded contention test requires exactly one debit for a competing footprint. A continuously displayed green/red ghost preview is still missing.
- **QX-04 (partial):** a LAN heartbeat measures RTT and the HUD visibly distinguishes an active host from a disconnected session. Automatic rejoin/persistent player identity, packet-loss and network-jitter statistics, timeout recovery and host migrations are still unimplemented.

**Verified suite:** [cross-platform full game verification, commit `b268afc`](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39537661060), [virtual desktop input](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39537661115), [12-run multiplayer soak](https://github.com/mrcalzon02/The-Malefic-Veil/actions/runs/39537661153). These links should be checked for final run conclusions before claiming release acceptance; an individual green job is not proof of a completed whole workflow.

**Next engineering queue:** finish QX-04 recoverable disconnect/rejoin flow with preserved host state, then QX-06 persistent control rebinding and comfort settings, QX-07 build ghost/status UX, and active hit-latency instrumentation. QX-01/02/03/04 are **not complete R4 public multiplayer release gates** despite passing narrower checks.

## Full source-asset/runtime reconciliation

Source rights, 124-city-model ingestion, 22 texture sources, 42 deterministic district model types, four project-authored character meshes, two first-person weapon models, seven item/pickup models, and four structure/device models are detailed in [ASSET_PIPELINE_AUDIT.md](ASSET_PIPELINE_AUDIT.md). Purchased/private `Characters_psx_1.2.zip` has 138 rigged and 138 unrigged FBX files and 136 source PNGs but no license document in its ZIP. Those 276 FBX sources are **not** embedded, converted, or advertised as playable animated models. Java's current character meshes are interim source-owned geometry.

Cross-platform CI checks for the latest source-UV and 18-model gameplay renderer are linked in the current GitHub Actions run list; release A4 visual parity and A5 character rigging are still **open**.
