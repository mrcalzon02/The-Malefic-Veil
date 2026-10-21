# Design contract

**Identity:** The Malefic Veil. Multiplayer-capable PSX-era arena horror shooter. Fast, lethal gunfights like classic Doom/Unreal Tournament, more involved survival and occult pressures.

**Presentation:** Java 17 application, Swing desktop shell and interface. Start with a deterministic low-resolution raycast renderer drawn in Java2D; future mesh/texturing renderer can replace this without replacing simulation/data schemas. Software raycasting is 2.5D, NOT evidence that the source GLB models are rendered.

**World:** Infinite-feeling large tile city assembled in seeded 12x12-block modules, containing roads, alley networks, outdoor courtyards, accessible interiors, intersections, loot and player infrastructure. Keep building grammar modular: tiles and collision/visibility authoritative, decoration/assets referenced through content manifest. Always reserve traversable routes between player spawns.

**Actors:** Human players choose a character/ability before each round. First four gameplay archetypes: Warden (fortification), Medic (support), Shade (stealth), Occultist (sanity control); these are provisional GAMEPLAY entries, not claims to be licensed/rigged mesh identities. PvE roamers and PvP can occupy the same city.

**Combat:** Handgun, shotgun first; separate magazines/ammo, hitscan with obstruction, reload timers, pickups, weapon switching, respawn delay. Future: alternate fires, accuracy, loadouts and status effects.

**Survival:** Health tracks physical injury; sanity separately tracks occult pressure (hallucinations, disorientation and perception degradation at low values). Visibility gem bottom-left indicates visibility as a spectrum, influenced by lights, sprinting, crouching, firing, concealment and active Shade ability. The gem is a deliberate functional nod to Thief, NOT use of its artwork.

**Economy:** Persistent match inventory for weapons/ammo/equipment/supplies/cash. Death and respawn policies configurable per mode. Construction consumes cash + supplies and places server-owned, collidable turrets/revive stations/healing stations. Deployables have build limits, repair and destruction paths.

**Network authority target:** Server decides movement legality, shots, damage, pickup acquisition, inventory expenses, deployable placement, NPCs, deaths and respawns. Deterministic map seed and packet version shared across clients. LAN first; no promise of secure public internet play or anti-cheat until verified.

**Modes:** Early FFA deathmatch and PvE scavenging/holdout; subsequent team deathmatch, capture zones, defend/extract and character progression. Maps favor fights and escape routes over unbroken featureless building walls.

**Persistence target:** Account/profile (local), selectable character builds, inventory across sessions only in modes permitting it. Define profile schema and host/server authority before exposing multiplayer persistence.

**HUD:** Small, information-dense: health, sanity, visibility gem, cash, ammo, ability cooldown, equipment and spawn selection. Avoid verbose menu cards; character selection on one concise screen.

**Asset intake:** Use exact Drive references, pack licenses and conversion manifests. Prioritize URBAN.zip, Kenney Retro Urban, PSX-style cars and Characters_psx_1.2; PSX Mega Pack weapons/props and converted road models supplement. Validate geometry, animations, textures, provenance and in-game draw before calling an asset integrated.
