# The Malefic Veil — Source-driven Wiki

> **Repository-backed reference**, authored against source on 23 October 2026. [GitHub repository](https://github.com/mrcalzon02/The-Malefic-Veil) · [Implementation status](https://github.com/mrcalzon02/The-Malefic-Veil/blob/main/docs/STATUS.md). `Implemented` means code/data exists; it does **not** imply a feature has passed full client integration or release acceptance.

This wiki is stored in the **main repository's `wiki/` folder** so changes are versioned with the game, reviewable, and linked directly to authoritative source. It is **not** automatically published into GitHub's separate native Wiki service. GitHub renders these Markdown pages directly.

| Subject | What you'll find |
|---|---|
| [Getting Started](Getting-Started.md) | Run modes, what works and what is provisional |
| [Gameplay](Gameplay.md) | Combat, PvP, survival, death, crafting, vehicles, deployment |
| [Items](Items.md) | All 157 exported item records, icon provenance, recipes that produce and consume them |
| [Item Dependencies](Item-Dependencies.md) | Full reverse index of every item producer and consumer, including quantities |
| [Recipes](Recipes.md) | All 22 hand and 62 industrial formulas with input amounts, output, workstation, era and faction |
| [Structures](Structures.md) | All 69 facility definitions, dependencies, costs, footprint, utilities, and production connections |
| [Factions](Factions.md) | Three playable factions and separate economies, aesthetics and alliance rules |
| [Classes](Classes.md) | Nine faction-exclusive classes with actual-versus-planned behavior |
| [The Lost](The-Lost.md) | Spawn ownership, enemy families, encounters, weak points, tactics and progression design |
| [Lost Arsenal](Lost-Arsenal.md) | 25 named attack profiles, physical/sanity effects, telegraphs, counterplay and enemy assignments |
| [World](World.md) | City sites, wilderness, roads, miasma, build restrictions and visual gates |
| [Source and Status](Source-and-Status.md) | Source-of-truth map and outstanding integration gates |

**Reading convention.** `Runtime`: directly exercised by existing server/client code. `Registered`: a real Java/data registry that is not yet fully used by live gameplay. `Design`: approved intention or new proposed content not operating in the game. Balancing columns in `economy/*.csv` are proposals unless adopted by the host.

**Keeping this accurate.** The item, recipe and facility tables below were rendered from committed CSV exports corresponding to `SurvivalCrafting` and `CraftingEraRegistry`. When those change, regenerate the affected tables from the same source, run `CraftingEconomyCsvTest` and `tools/validate_economy_csv.py`, and update this wiki in the same commit. Never present a designed feature as shipping.

## Working player arsenal

[32 verified player weapons with eight shared city pickups and 24 faction-specific base recipes](Weapons.md). The Lost NPC arsenal is separate.
