# Development roadmap / verification gates

## Phase 0 — repository and catalog
- [x] Independent GitHub main repository verified.
- [x] Archive/source locator register created with exact Google Drive identifiers.
- [ ] Recursive ZIP inventories, attribution and hashes for each archive (source metadata != reviewed content).

## Phase 1 — runnable local vertical slice
- [ ] Compile with `javac --release 17`, run desktop Swing (source committed; execution unverified here).
- [x] Seeded city map and walkable building grammar with collision and raycast view (source implemented; tests pending).
- [x] Player movement, input, handgun/shotgun, pickup loop, NPCs (source implemented; playtest pending).
- [x] Health/sanity and functional visibility gem, class ability (source implemented; playtest pending).
- [x] Spawn selection on death and basic defensive deployables (source implemented; multiplayer sync pending).
- [x] Automated headless world invariants and LAN smoke-check source committed; execution pending.

## Phase 2 — multiplayer correctness
- [ ] Versioned server protocol, remote player rendering and state snapshots (experimental unversioned LAN protocol and peer visuals exist; versioning/interpolation pending).
- [ ] Host-authoritative shots, collision, inventory, cash, NPC AI, devices and respawn (server resolves PvP shots, but client movement, ammo, devices and NPCs are NOT authoritative).
- [ ] LAN soak testing (two clients, reconnect and join-in-progress), packet misuse tests.
- [ ] Ping/interpolation, dedicated server mode and optional LAN discovery.

## Phase 3 — actual archival asset integration
- [ ] Download individual permitted source packs, record hashes/rights and per-file paths.
- [ ] Convert urban GLB/tile meshes to runtime format; material and geometry proof.
- [ ] Character mesh/rig/animation and skins sourced from archive; approved roster.
- [ ] Rebuild city visual themes, weapons, pickups, cars and NPCs using source material.
- [ ] Audio, 3D collision, dynamic lighting/visibility and occlusion verification.

## Acceptance discipline
Status chain: ARCHIVED → INVENTORIED → LICENSED → CONVERTED → COMMITTED → RUNTIME_RENDERED → REGRESSION_TESTED → PLAYTESTED → RELEASED. Never skip. No sandbox-only progress claims and no GitHub Actions unless requested. Verify branch head remotely after commits.
