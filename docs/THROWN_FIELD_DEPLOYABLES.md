# Thrown field equipment — independent deployment system

## Architectural boundary

**These are consumable inventory items thrown into the world. They are not player-built structures.** They do not enter `World.Device`, do not require settlement plots, do not occupy permanent facility slots, do not consume the settlement structure limit, and never upgrade through the permanent building system. Code authority: `ThrownDeployables`. Render source: `ThrownDeployableMeshes` (21 independent original low-poly meshes and one harmless burst mesh).

A host-validated throw consumes one crafted kit, launches a temporary airborne entity for 700 ms, and activates it upon landing at a walkable and collision-cleared location. A separate clock starts on **landing** and expires after **3,600,000 ms (one hour)**. Its hit points decay over its lifetime and weapons can destroy it early. Destruction or expiry removes the entity, releases a visual burst, and deals **zero explosion damage** to players, NPCs, vehicles, or scenery. Never use `BUILD` for field equipment; the network verbs are `FIELD_CRAFT` and `FIELD_THROW`. A client cannot supply the faction or final impact coordinates.

The runtime limits simultaneous field deployments to 84 globally and 12 per authenticated owner. A deployed item's physical mass counts against the player's inventory carrying capacity. Deployable barriers affect authoritative movement and line-of-sight while alive; the city map and authored city geometry remain unchanged.

## The seven roles: all 21 faction variants

| Role | Unraveled — engineered | Penitent — liturgical | Begotten — biological |
| --- | --- | --- | --- |
| **Wall** | Segmenting reinforced metal blast panels with hard braces and signal lamps; heavy HP, solid cover | Carved defensive votive wall with crenellations and a sigil core; allied sanity-restoring presence | Living bone-rib wall with thorn protrusions and exposed growth nodes; contact harm against enemies |
| **Barricade** | Folding anti-personnel tripods, rails, and entangling braces; suppresses hostile movement to 65% speed | Two reliquary pillars linked by crossed warding bars; reduces hostile speed and drains sanity | Root web and grasping cartilage spikes; slows enemies to 48% speed and wounds them |
| **Turret** | A swivel gun on a fixed metal base with twin visible barrels and sensor assembly; rapid physical fire | An incense-driven sigil emitter surrounded by a crown of prongs; physical and psychological damage | A fleshy stalk with exposed eye/maw, piercing mandibles and gland; spore impact plus toxic pulse |
| **Health station** | Fold-out medical box with injector reservoirs and lit medical cross; restores health quickly | Portable altar with candles and consecrated emblem; smaller health restoration plus stronger sanity recovery | Heart-like nutrient pod surrounded by vascular spikes; stronger biological health regeneration |
| **Ammo dispenser** | Cartridge-magazine vending case with exposed cartridges; higher mechanical supply volume | Orbital shrine for consecrating charges; supplies compatible ammunition and supports sanity | Organic egg cluster that grows living munitions; supplies compatible ammunition plus a small heal |
| **Miasma generator** | Industrial cylinder with two rising exhaust pipes; reduces nearby enemy visibility | Incense censer surrounded by raised censers and etched prongs; damages enemy sanity | Bloated spore sac with erupting cysts; causes poisonous health and sanity damage |
| **Shield bubble** | Four-legged projection hub and multi-ring open field; absorbs 80% of allied incoming HP damage up to shield HP | Sacred lantern-and-spire projector; absorbs 65% and restores nearby allied sanity | Living membrane projector with outstretched growths; absorbs 55% but regenerates shield integrity |

Shield meshes include a low-poly multi-ring visual envelope rather than opaque polygons that would hide battlefield actors. Cover geometry and bubble interception use different collision logic: only walls and barricades stop normal movement.

## Three manufacturing streams

The current playable first-pass material costs are *distinct* and are enforced by the host, using the presently functioning survival material inventory, rather than silently assigning every faction the same universal recipe.

- **Unraveled**: produced at an owned or allied workbench from machine parts, delivered metal stock, and charcoal. Turrets and shields require extra metal. This is an engineered portable assembly.
- **Penitent**: prepared beside an owned or allied field fire from ritualized cloth, herbal bundles, and fired bricks, with higher herb demand for sacred healing/shielding and heavier masonry for walls/barricades.
- **Begotten**: hand-cultivated seed kits from compost, gathered edibles, and medicinal herbs, with greater nutrients for healing/ammo and richer biological feedstock for spores/shields.

The existing wider faction industry registry describes additional manufacturing stages that are not all executed in live gameplay yet. These first-pass throwable material costs are not a claim that mature Penitent and Begotten production chains are complete.

All 21 exact equipment items are append-only values in `SurvivalCrafting.Item` and each appears as a unique faction-locked `throw:` recipe in `CraftingRecipeIndex`; they are physical kits in carried inventory. `GpuCraftingBook` can inspect their recipes and the in-world crafting overlay can request the host craft transaction. Same-faction and cross-faction rights are validated **at the server**, not by hiding a button.

## Play controls and host authority

In the playable desktop client: **Z** cycles the seven throwable roles, **Y** throws the equipped faction variant; the current role and carried quantity appear on the live HUD. Use the in-game crafting overlay to craft a `field:<faction>-<role>` kit using the appropriate station. The thrown item flies and deploys; do not press legacy **T/H/B/V** construction controls, which still operate the separate fixed structure system.

The LAN host broadcasts transient field states, kills and harmless bursts. Clients render both airborne and deployed meshes, never decide hits, and never mint kits locally. Host combat controls the rate of automated attacks, collision, shield absorption, ammunition replenishment, and death.

## Deliberate limitations / follow-up verification

- Field entities are **ephemeral session state**, not permanent realm structures; restarting the host clears them. A future persistent timed-state policy requires an explicit save-format version change, not silently writing them into the legacy permanent device list.
- In-game equipment icons are not bespoke artwork yet; the 21 independently modeled 3D silhouettes are present in the actual render catalog.
- Regression source: `ThrownDeployablesTest`. Verify with the full Java 17 compile, its dedicated test, the inventory/arsenal/faction tests, and a real GPU-hosted throw/damage/expiry smoke run before labelling the build release-ready.
