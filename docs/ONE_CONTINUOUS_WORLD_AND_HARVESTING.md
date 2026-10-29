# One Continuous World — Interaction and Harvesting Integration (2026-10-21)

**Canonical design and code contract.** The Malefic Veil is a single connected landscape, **not** a city game plus a separate wilderness session or dimension. The city, other cities, intercity highways, forest, hills, fields, settlements, enemies, player claims, vehicles, pickups, resources and combat use one server-owned World coordinate plane and one actor position. A render transition may select a different level-of-detail technique; it MUST NOT teleport, migrate, disable combat, or invoke a different player/network identity.

## Coordinate model

The original detailed 132x132 city sector is the authored City A district of the procedural 224x224 continental blueprint. A macro parcel represents `CityLayout.CELL = 6` world metres. World maps the center of continental City A to (66,66) world metres, keeping legacy city positions/save identity unchanged. Transform:
`worldX = 66 + (parcelX - cityA.x)*6`, `worldY = 66 + (parcelY - cityA.y)*6`.
The inverse is `floor((worldX-worldOffsetX)/6)`. City A's existing detailed geometry/collision is authoritative within 0..131 on each axis; exterior geometry and collision come from the same continent layout, in the same units. City A has explicit pedestrian openings at its northwest road edge; moving outside it is ordinary `STATE` movement, never an expedition or teleport packet. City B-G currently use coarser macro terrain/city metadata pending full physically authored roads, structures, interiors, props and obstacle colliders. That visual/geometry expansion is outstanding, **not a second world**.

## Implementation corrected

- `ArenaSystems.Player` has a single `x,y,angle` and no `inWilderness` flag. Movement always goes through the existing `ArenaSystems.move`, now with swept step/collision checking rather than endpoint-only checks. PvP, NPC targeting, city pickups, cargo, vehicle handling and survival all use the SAME coordinates everywhere.
- `LanServer` has no `WILD_ENTER`, `WILD_EXIT`, `WILD_MOVE` or permission to route to another world. The normal `STATE`, `PEER`, `SELF`, `PICKUP`, `FIRE` and `HARVEST` messages apply irrespective of terrain type.
- `WorldHarvesting` holds actual server-controlled resource depletion, cooldown, sight/range, inventory preflight/debit, respawn and the durable `wilderness-resources.txt` journal. `HARVEST|nodeId` supplies **no coordinates, no rewards and no identity**; the host takes the position and aim from its existing `ArenaSystems.Player` record. `RESOURCE_NODE` broadcasts the current world's actual resource coordinates, replenishment state, type and capacity.
- The Beckenburg centre-ray `InteractionFocus` selects the single visible nearby object and shows a compact hand icon with E. The one E action is used for normal city pickups, nearby vehicle/cargo items and wilderness harvesting. Items harvested enter the SAME `SurvivalCrafting` inventory used for crafting and saved characters.
- The client now chooses City A authored raycast or coarser procedural continental terrain and 3D node meshes **based on the camera's position only**. There is no mode, toggle key, return command, expedition spawn or alternate movement loop. Beckenburg CC0 broadleaf/conifer GLBs are present in `assets/game/beckenburg`; other node meshes have temporary original low-poly authored geometry.

## Source verification and remaining acceptance gates

`WorldHarvestingTest` uses the real arena player profile with a world-position resource, verifies actor-authoritative focus, material credit, multiple-player depletion, spoof rejection and journal save/reload. `InteractionFocusTest` verifies center-ray selection/occlusion and a drawn prompt. Both are registered in `QualityGateTest`. These test source changes were committed; **a real compilation, client/host soak run and visual traversal are separate checks** and must not be claimed until they have actually run.

Further release work: author macro city B-G physical roads/colliders/plots, instantiate connected forest/terrain meshes and transition zones around City A, bind varied foraging actions/tools to maturity/work-time rules, implement actual fauna/farming nodes, add obstacle-specific picking/harvesting animation, and exercise cross-area combat, convoy and anti-air systems in the same world. Do NOT reintroduce dimensional scene switches to work around renderer deficiencies.
