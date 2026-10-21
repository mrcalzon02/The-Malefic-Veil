# Development roadmap / verification gates

## Phase 0 — repository and catalog
- [x] Independent GitHub main repository verified.
- [x] Archive/source locator register created with exact Google Drive identifiers.
- [ ] Recursive ZIP inventories, attribution and hashes for each archive (source metadata != reviewed content).

## Phase 1 — runnable local vertical slice
- [x] Compile with `javac --release 17` and render Swing offscreen in CI (interactive desktop session not yet verified).
- [x] Seeded city map and walkable building grammar with collision and raycast view (source implemented; tests pending).
- [x] Player movement, input, handgun/shotgun, pickup loop, NPCs (source implemented; playtest pending).
- [x] Health/sanity and functional visibility gem, class ability (source implemented; playtest pending).
- [x] Spawn selection on death and basic defensive deployables (source implemented; multiplayer sync pending).
- [x] Automated headless city/combat/health/pickups/deployables, live LAN and Swing tests executed in GitHub Actions.

## Phase 2 — multiplayer correctness
- [x] Protocol v2 greeting, authoritative visibility snapshots and remote alpha blending (interpolation and hardening pending).
- [x] Host resolves PvP shots, speed-bounded collision, inventory, cash, AI, devices, items and respawn (security hardening and prediction pending).
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
