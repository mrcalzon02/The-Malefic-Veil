# The Malefic Veil — Player Quality-of-Life Must-Haves
**Multiplayer/open-world shooter complaint review · 2026-10-20 · Living release-gate specification**

> Make the game frightening and dangerous, **not the interface, netcode, spawns, inventory, construction controls, or ability to play with friends**. Death by an opponent is gameplay. Death because the game lied about a hit, silently ignored an action, or spawned someone into a known kill zone is a defect.

**Scope.** The Malefic Veil is a Java 17, PSX-style urban horror arena shooter inspired by Doom and Unreal Tournament. Systems already under development include health/sanity, handgun/shotgun, roaming hostiles, PvP stealth-opacity jewel, selectable respawns, ammo/equipment/cash/supplies and constructed turrets, barricades and healing/revival stations. This document defines acceptance criteria for those systems and planned multiplayer expansion. **It does not assert that the requirements are already implemented.**

## Evidence and method

We reviewed internal project design, plus the owner's [Gaming defaults mistakes](https://docs.google.com/document/d/1HWiIyY4Flg58PPWLW4Eac5nB2Let_3p1ZFF99w0z-As/edit), [Gameplay & Cheats](https://docs.google.com/document/d/1tfVKW8pLQJjCaa1YURNKno18-gVYam8o1qPFftlgex0/edit), [Editor and construction guidance](https://docs.google.com/document/d/1sHKDYA_gDE_RKUhWFIbCmQ3OMFQtnsPFIke7Co9tC6Q/edit), and [The Fractured Frontlines](https://docs.google.com/document/d/1apJj4QegEMU5notk8vmIEdgO4-GX-p5HdYkmFecwvyQ/edit). Internal guidance emphasizes no forced intro/volume/blur, remappable controls, compact usable UI, legible item quality, nonblocking blueprint ghosts, meaningful placement failure reasons, inventory shortcuts, hitbox/network diagnostics, server permissions and moderation. The ARC Raiders essay is the owner's interpretation of divergent PvP and co-op audiences, **not an independently proven population trend**.

External evidence combines contemporary player discussions in Battlefield, The Finals, ARC Raiders, Escape from Tarkov, Helldivers and PlanetSide with first-party guidance from EA/Respawn, Ubisoft, Microsoft and Valve. Community posts are qualitative complaints and counterpoints, not a representative survey, measured defect rate or scientifically ranked list. Themes repeated across different games and recognized by developers receive more weight. Source links and dates are preserved at the end.

## Highest-risk player complaints, translated into Malefic Veil requirements

| Repeated frustration | External signal | Malefic Veil response | Priority |
|---|---|---|---|
| Shots appear to land but deal no damage; deaths behind cover; missing/late feedback | Battlefield netcode/hitreg discussions [E01, E02], Valve network HUD [E13] | Server-confirmed hit markers, shared damage accounting, lag/loss test matrix, visible connection diagnostics | **P0** |
| Enemies memorize/force spawns; revivals become a continuous death loop; spawns too far away | The Finals [E03, E04] | Ranked spawn candidates, safe route checks, enemy/turret exclusion, visible spawn risk and alternatives | **P0** |
| Cheaters, teaming, griefing and worthless reporting | Apex/EA developer update [E05], Rainbow Six/Ubisoft [E06] | Host authority, audit logs, private servers, rate limits, report/mute/block/moderation; no blanket invasive surveillance | **P0** |
| Unfair matchmaking and solos versus coordinated premades | Apex/EA [E05], The Finals [E04] | Explicit public/private/PvP/PvE mode and party rules, measured ping/skill checks, honest bot labels | **P0 for public matchmaking** |
| Five minutes preparing the same gear for three minutes of actual combat | ARC Raiders [E07, E08], Tarkov [E09] | Saved/repeat loadouts, auto-stack, rapid replenishment and predictable death inventory policy | **P1** |
| Audio cues disappear, cannot be located, or lie about an opponent | Battlefield [E10], Apex audio telemetry [E11] | Reliable positional sound event system, caption option, independent volume sliders, diagnostic event comparison | **P0 when audio ships** |
| Late host kicks, toxic text/voice, friends unable to stay together | Helldivers [E12], Ubisoft moderation [E06] | Prevent kick-to-deny-rewards, rejoin, party keep-together, mute/block and accountable host settings | **P0 for public play** |
| No persistent servers or understandable joining/rejoining | Battlefield community [E14], Valve networking/relays [E15] | LAN/direct IP/invite/server browser, rejoin, clear connection errors, explicit region/host status | **P1** |
| Inventory saturation, purchase-to-stash instead of equipped slots, poor sorting | ARC Raiders [E08, E16] | Predictable stock/clip rules, cost breakdown, sensible stacks, favorites, no silent loss | **P1** |
| Overlong traversal, predictable map choke points, uninteresting city scale | The Finals [E03] and PlanetSide [E19] | Objective density, multiple routes, landmarks, accessible interior paths, tested travel budgets | **P1** |
| Pay-to-win/progression that appears arbitrarily throttled | Battlefield user report [E17]; allegation, not independently audited | No paid combat advantage, no invisible XP throttles, viable starter equipment, openly displayed balancing | **P1** |
| Tiny text, forced shake/blur, unmappable controls, blinding intros | Microsoft accessibility guidance [E18], internal defaults | First-launch comfort controls, bindable input, scalable compact UI, default-low-motion visuals | **P0/P1** |
| Player-built traps, hidden collision rules, wasted building resources | Internal construction rules; PlanetSide community building concerns [E19] | Preview, named invalid-placement reason, atomic purchase and nav clearance tests | **P0** |
| Camped extraction/evacuation chokepoints | ARC Raiders [E20] | **Only if extraction mode is added:** multiple viable evacuation routes, counterplay and risk transparency | **Conditional P0** |

**Important disagreements:** Some Battlefield players report not personally seeing widespread netcode problems [E21]. Some extraction-shooter players believe ambush risk is a defining feature of PvP [E22]. The response is **controlled verification and transparent mode contracts**, not a blanket abolition of ambushes or competitive play. A hybrid PvE/co-op option must be clearly selected; a PvP option remains genuinely competitive.

## Product rules we will not compromise

**Fair failure:** a player should understand cause of death, weapon, approximate direction, relevant health/sanity changes and whether a hit was host-confirmed. Death recap must not leak live opponent locations to their teammates.

**Authoritative state:** competitive shot, ammo, cash, material, pickup, position, stealth opacity, damage, turret and construction results come from a single server model, not whichever client speaks first. Reject malformed/replayed/duplicate commands. Visible client prediction is allowed; unreconciled competitive truth is not.

**Minimum friction:** do not require repeating the same loadout/crafting/admin steps on every death; preserve user preferences. Tutorials explain unobvious systems but never interrupt a live PvP firefight. No fake buttons, inert controls, endless nested cards or explanatory essays around obvious labels.

**Danger without softlocks:** randomized streets, multistory buildings and defensive structures can be tactically punishing, but they must not trap players behind an unremovable wall, close every route to an objective, or put a revive station inside enemy turret range without warning.

**Legitimate choice:** no opaque switch between co-op survival, PvPvE and arena PvP. Do not secretly insert bots represented as players. Local solo play must not depend on a third-party online service.

**Privacy and moderation:** role-based server commands, mute/block/report, proportionate evidence-based bans and opt-in diagnostics. Do not import invasive Steam/GOG/HWID/IP cross-identification from generic internal command brainstorming as an automatic default.

## P0: hard stops before any unrestricted public multiplayer release

### QOL-001 — Combat hit-registration contract

Implement shot IDs, host-accepted or host-rejected results, normalized projectile/weapon rules, authoritative health deltas and reloads, bounded lag compensation, server shot log and ping/jitter/loss overlay. A tracer is never automatically a confirmed hit. Show reason when server rejects a shot (out of ammo, blocked, late, dead, ability).

**Gate:** 1,000 simulated engagements covering 0/50/100/150 ms round-trip and 0/1/3% packet loss, plus real two-client playtests. Eventually consistent host/clients for health and ammo; no false confirmed hit markers; no exploitable shoot-through-solid-cover case without a documented reconciliation decision.

### QOL-002 — Safe selectable respawns

Score spawn candidates by distance to enemies and their turret cones, recent spawn deaths, lines of sight, route connectivity and objective relevance. Show location and risk on the map. Make brief respawn protection nonoffensive: shooting cancels it. Offer a safe alternative when possible; don't give one team a predictable spawn manipulation exploit.

**Gate:** 20 seeds × 100 cycles × multiple combat scenarios, zero in-wall/in-prop spawns, no repeatedly forced known kill spot when safer choices exist. Check turret camp and moving-opponent cases, not merely tile walkability. A player must never have to die ten times to get one usable starting position.

### QOL-003 — Mode and matchmaking truth

Explicitly choose solo, private LAN, public FFA, team arena or cooperative/PvE as implemented. Show region/ping, map/rules, bot use, friendly fire, team limits, persistence and inventory-loss policy before join. Do not make a false guarantee of ranked skill balancing until such a matchmaker exists.

**Gate:** server PvE rule forbids human-on-human damage; PvP mode is clearly disclosed; teams/friends remain grouped as permitted; mode UI matches authoritative actual rules.

### QOL-004 — Abuse, exploits and host governance

Validate movement, rapid fire, inventory costs, pickups, turrets, enemy ownership, colliders, build counts, revive privilege, rate limits and protocol parsing. Distinguish admin from players, with moderator action log. Provide mute/block/report and reasoned kick/ban where public communications exist.

**Gate:** invalid-packet, duplicate pickup/build, negative stock, impossible speed, out-of-range shot, unprivileged admin and build-into-spawn attacks fail safely. Admin controls unavailable to ordinary clients.

### QOL-005 — Loss/recovery and understandable failures

Networking shows connection/host/tick health and finite timeouts. Expose ping, jitter, packet loss and FPS optionally. Disconnect → reconnect attempt, explanation and route back to menu; no endless connecting animation or vanishing equipment. Private hosts can shut down cleanly with warning.

**Gate:** host restart, client crash/rejoin, mid-revive loss, late join, 150 ms/3% loss and protocol version mismatch scenarios; no inventory duplication, UI freeze, stuck session or silent failure.

### QOL-006 — Functional and non-exploitable construction

Simple placement is preview + confirm. Green/red ghost preview must never have collision. Show exact cost, available supplies and invalid reason (blocked, too close, too steep, spawn path, enemy zone, insufficient funds). Construction must not close critical passageways, spawn exits or objective access; refunds/deconstruction cannot dupe money. Clear ownership, repair, damage and limits.

**Gate:** 100 construction/reclaim loops, zero resource inflation, no charge on rejected build and no softlocks across 20 procedural seeds. Test turret line of sight, friendly target discrimination and barricade placement near doors.

### QOL-007 — Truthful healing/sanity/revive and inventory

Health, sanity, healing after damage, medkit consumption, stamina/ability state, death, revive and cash/supplies all follow one host ledger. Visible status when regen is stopped or item unavailable; no mystery cooldown or silent consume. Define whether death loses kit in each mode.

**Gate:** simultaneous pickup and revive attempts, disconnected player, 0 medkits, max health, double use, and full stash conserve resources; after round the client matches the authoritative event log.

### QOL-008 — Playable controls and visual comfort

Full in-game remapping, hold/toggle crouch/sprint/aim, adjustable mouse sensitivity/invert/raw input, safe escape/quit, key navigation, UI scaling, color-independent cues, low-motion preset, subtitles when voices exist and no forced blur/shake. Start master audio conservatively; no required movie/splash every launch.

**Gate:** first-run, left-handed/rebound-input, 720p/1080p/4K, keyboard-only and readability tests. Use Microsoft XAG as an **assessment checklist**, not a claim of certified accessibility. No mandatory key permanently locked to a gameplay function.

## P1: mandatory for an enjoyable open alpha

### QOL-009 — Fast recovery, favorite gear and loadout presets

Saved class/weapon/equipment preset and one-action “repeat last” where materials exist; re-equip starter gear if paid kit is unavailable; configurable automatic ammo/health pickup and auto-stack; favorites protected from accidental disposal. Avoid crafting one piece of ammunition at a time. Design target: **≤3 user actions from death screen to accepted respawn** when a valid preset is available. Time budget is a proposed project gate, not an externally sourced benchmark.

### QOL-010 — Compact transparent HUD

One reasonably dense view: health, sanity, regeneration conditions, gem/stealth %, weapon/clip/reserve, cash/supplies, selected ability and cooldown, current item/device and objective. Hover/help only for genuinely ambiguous concepts. Every button works or gives a concise reason for being disabled. UI scale and contrast adjustable. No repeating one control across five giant panels.

### QOL-011 — Stealth with fair counterplay

The visibility jewel directly sets actual **server-replicated percentage alpha for opponents**, not only the player's HUD. Test crouch, sprint, dark/light, firing flash and Shade ability separately and together. Stealth must not mysteriously alter hitboxes or obstruct legitimate counters. Do not reveal active hidden players through a debugging overlay in public match. Record whether audio cues change, and disclose the logic to players.

### QOL-012 — Cities that are worth traversing

Multiple coherent lanes, side/back doors, traversable interiors, vertical routes and recognisable districts rather than only huge empty street grids. Show map/compass, landmarks, active fight/objective, direction and squad rendezvous. Reserve escape routes near turrets/barricades. Test real time to cross city segments rather than only generator connectivity. A player searching for a fight should not spend most of their session in empty running.

### QOL-013 — One-step field engineering

Hotkey/radial access to turrets, heal/revive stations and barricades; meaningful projected footprint, cost and direction; “repeat placement” for barricade lines; fast repair and clear disassembly rules. Never permit a device to be visually shown but not functionally assembled. Progress bars should indicate actual server-confirmed construction, not a cosmetic timer.

### QOL-014 — Competitive fairness and progression

Default handgun and shotgun remain practical. Classes are differentiated but have downsides; no mandatory grind to become capable of basic PvP, and no sale of competitive combat superiority. Damage, armor, sanity impairment and movement penalties are documented. Matchmaking does not invisibly change weapon damage to force outcomes.

### QOL-015 — Reliable positional audio and accessibility alternatives

Once audio exists: distinct footsteps, reload, shot and turret sound events; meaningful directional mix/occlusion; independent master/music/ambience/effects/voice sliders and mute. Critical information that normally comes only from sound should have optional appropriately balanced captions or indicators. Measure triggered audio events against played events to investigate “silent footsteps”.

### QOL-016 — Friends, community, server choice

Private party code/direct IP/LAN browser, optional server list, named persistent private server settings and readable game rules, reconnect to the correct session, mute/block, clear quit path. Prevent abuse of host authority to steal accrued rewards or sabotage teams. Avoid forced third-party identity or service dependency in solo/LAN.

## P2: beta/release-candidate integrity

- **QOL-017 Performance:** explicit budget for median and 1%-low frame times, max NPCs/devices and memory. Measure large-city streaming, effects and active PvP, not just empty menu FPS; 30-minute sustained test without increasing heap or stutter.
- **QOL-018 Geometry/visibility:** rendered wall depth and collision truth agree, including glass, doors, character alpha, multistory rooms and urban assets; no wallhack-like translucent rendering or shooting through invisible/incorrect colliders.
- **QOL-019 Persistence:** safe local profile/settings/host-state saves, versioned migrations, crashes and reconnection; optional private server save/restore. Death cannot corrupt a persistent account or create duplicates.
- **QOL-020 Community safety:** server moderation roles, diagnostics, report receipt and ban appeals, without blanket fingerprinting or retention of excessive personal data.
- **QOL-021 Player-frustration telemetry:** opt-in or privacy-preserving aggregates for rejected shots, deaths within 5 seconds of spawn, time in menus/actually fighting, build rejection reasons, reconnect failures, audio event omissions and 1%-low frames. Tests produce auditable artifacts.
- **QOL-022 Patch honesty:** public version/build IDs, documented rule changes, known defects, source asset licenses, installation/rollback path and never claim a fix without executable proof.

## Release-gate integration

| Project release | QoL gate required | Evidence beyond code existence |
|---|---|---|
| **R1 — private prototype / controlled alpha** | QOL-001/002/005/006/007/008/010/011 for available systems | JDK 17 Windows/Linux build, real network hit/regen/build regressions, virtual desktop input, **manual two-client** playtest |
| **R2 — expanded urban alpha** | QOL-002/006/012/013/017/018 | 20 map seeds, safe spawns, unobstructed paths and 30-minute walk/combat/collision run |
| **R3 — character/combat alpha** | QOL-001/008/009/010/011/014/015 | Real approved character rigs, correct animations/hitboxes/stealth and usable settings |
| **R4 — open multiplayer alpha** | **Every P0** plus QOL-003/004/005/016/020 | 4–16 participants, join/rejoin and moderation, packet fuzz, loss/ping replay, 2-hour host soak |
| **R5 — public beta** | Every P0/P1 and the shipped P2 subsystems | Structured casual/competitive/accessibility tests, observed friction metrics, feedback fixes |
| **R6 — candidate release** | All shipped-system requirements, including QOL-017–022 | Clean install, rollback, 60-minute genuine multiplayer session, verified artifacts and no open critical issue |

A nominally P1/P2 feature becomes **P0 immediately** when its broken implementation can steal progress, bypass authority, trap the player, or make the session effectively unplayable. AI and automation never substitute static checks for live end-to-end verification.

## Recommended execution backlog

| Ticket | Priority | Implementation target | Required proof |
|---|---|---|---|
| QX-01 Server shot IDs, confirmed hit feedback and network overlay | P0 | ArenaSystems, LanServer, NetLink, HUD | 0–150 ms RTT matrix and host/client damage event agreement |
| QX-02 Ranked safe spawn candidate selector and preview | P0 | World, ArenaSystems.respawn, Swing map | 2,000 simulated spawns, turret/LOS checks, desktop flow |
| QX-03 Construction footprint and corridor conservation | P0 | World urban props, build authority, UI | 20 seeds, failed build reasons, zero cost on failure |
| QX-04 Disconnection recovery, bounded host timeouts | P0 | LanServer, NetLink, app lifecycle | Rejoin, host-exit and mid-revive tests |
| QX-05 Network-visible death ledger and medkit/sanity reasons | P0 | ArenaSystems, HUD | Loss, heal/revive, item/double-consume audit |
| QX-06 Settings: remap, mouse, hold/toggle, display/audio comfort | P0 | New persisted settings module and UI | Real Swing robot control tests + reboot |
| QX-07 Compact HUD revision and contextual reject messages | P1 | Swing | 720p/1080p interface checks and input usability |
| QX-08 Class/loadout presets, quick refill/favorites | P1 | Profile/equipment and inventory | 3-step respawn measurement |
| QX-09 PvP/co-op/private server rules and labels | P0 public | Lobby and server config | No mode/rule mismatch, friend session test |
| QX-10 Usable map guidance and timed urban travel | P1 | Map generator + HUD | Objective path timings/variety across seeds |
| QX-11 Structure ownership, team filtering, repair and removal | P0 public | Device state and protocol | No friendly turret grief or refund exploits |
| QX-12 Diagnostics, moderation and exploit audit | P0 public | Permissioned host console and QA tools | Negative privileges and malformed packet tests |
| QX-13 Directional audio event/caption pipeline | P0 when shipped | Audio bus and world event layer | Sound trigger/replay evidence |
| QX-14 Clean client packaging and updates | P2 | Build/release tooling | Real clean-machine acceptance and rollback |

**Next engineering order:** QX-01 → QX-02 → QX-03 → QX-04; QX-05/06/07 can proceed in parallel without bypassing network authority. QX-08–14 follow release-gate readiness. The current successful smoke tests are a baseline, not proof of a complete public-ready shooter.

## Ongoing quality checks: measure instead of guessing

Test separately with a competitive arena player, a newcomer, a co-op/survival-oriented player, a player using alternative controls and two friends connecting from different computers. Observe the *time to reach a fight, repetitions of menu work, deaths shortly after respawning, unclear damage/ability outcomes, rejected building actions and reconnect failures*. Ask “what prevented what you intended?”, not just whether they had fun. Reproduce each report with game/host evidence before changing combat balance; log errors and rerun targeted regressions.

## Research sources — external, reviewed 20 October 2026

Evidence classes: **D** developer/provider guidance; **C** dated community experience. C sources document complaints, not independently verified implementation defects or population size.

- [E01 · C · 2025-10-17] Battlefield netcode and hitreg reports: https://www.reddit.com/r/Battlefield/comments/1o8ypyt/the_current_netcode_is_the_elephant_in_the_room/
- [E02 · C · 2026-06-18] Battlefield hit registration, corner-peek complaints: https://www.reddit.com/r/Battlefield/comments/1u8ynn5/my_biggest_and_mostly_only_problem_is_the_netcode/
- [E03 · C · 2025-06-03] The Finals spawn distances and predictability: https://www.reddit.com/r/thefinals/comments/1l21hsi
- [E04 · C · 2025-07-01] The Finals predictable spawns/match imbalance: https://www.reddit.com/r/thefinals/comments/1loyvny
- [E05 · D · 2025] Apex Legends matchmaking, teaming, botting and anti-cheat: https://www.ea.com/ea-studios/motive/news/dev-update-0225
- [E06 · D · 2025-08-14] Rainbow Six Siege anti-cheat / anti-toxicity: https://www.ubisoft.com/en-us/game/rainbow-six/siege/news-updates/1JLAM9uc3HwK5N04IgR5pI
- [E07 · C · 2025-12-22] ARC Raiders prep/menu time and saved loadouts: https://www.reddit.com/r/ArcRaiders/comments/1pt41m4/loadout_presets_and_autocrafting_should_be_the/
- [E08 · C · 2026-04-04] ARC Raiders inventory/loadout friction: https://www.reddit.com/r/ArcRaiders/comments/1sc3eq1/its_about_time_the_inventoryload_out_was/
- [E09 · C · 2026-09-25] Tarkov regear/preset frustration: https://www.reddit.com/r/Tarkov/comments/1wqapis/fix_presets_bsg_regearing_fucking_sucks/
- [E10 · C · 2026-10-12] Battlefield footstep/spatial sound complaints: https://www.reddit.com/r/Battlefield6/comments/1x44hna/thisn_one_goes_out_to_all_the_audio_is_perfect/
- [E11 · D · 2025-05-05] Apex update on dropped audio event diagnostics: https://www.ea.com/games/apex-legends/apex-legends/news/prodigy-patch-notes
- [E12 · C · 2026-07-01] Helldivers late host kicks/griefing: https://www.reddit.com/r/Helldivers/comments/1ukz5a1/psa_for_griefers/
- [E13 · D] Valve network/telemetry HUD for ping and tick misses: https://help.steampowered.com/en/faqs/view/5E6F-5B36-5485-F6B9
- [E14 · C · 2026-09-28] Battlefield server-browser/persistent lobby discussion: https://www.reddit.com/r/Battlefield/comments/1wsty0w/battlefield_6_has_lost_a_key_feature_and_part_of/
- [E15 · D] Valve networking/relay security, privacy and connection quality: https://partner.steamgames.com/doc/features/multiplayer/steamdatagramrelay
- [E16 · C · 2026-08-25] ARC Raiders stash/ammo management: https://www.reddit.com/r/ArcRaiders/comments/1vxuguw/how_does_everyone_feel_about_the_current/
- [E17 · C · 2026-10-14] Battlefield player complaint about progression cap/pay-to-skip (not independently audited): https://www.reddit.com/r/Battlefield/comments/1x5njba/the_maximum_xp_per_minute_for_weapons_is_300_xp/
- [E18 · D] Microsoft Xbox Accessibility Guidelines v3.2: https://learn.microsoft.com/en-us/xbox/accessibility/guidelines
- [E19 · C · 2026-07-28] PlanetSide builder/logistics and construction grief discussions: https://www.reddit.com/r/Planetside/comments/1v9ewkt/building_infrastructure_is_more_fun_to_me_than/
- [E20 · C · 2025-11-21] ARC Raiders extraction campers: https://www.reddit.com/r/ArcRaiders/comments/1p32tho/im_so_done_with_extract_campers/
- [E21 · C · 2026-09-21] Counterpoint: Battlefield player not personally seeing widespread netcode bugs: https://www.reddit.com/r/Battlefield6/comments/1wmfcwh/why_do_i_not_experience_any_of_the_issues_people/
- [E22 · C · 2025-12-22] Counterpoint: PvP-focused player accepts ambush danger as intended: https://www.reddit.com/r/ArcRaiders/comments/1psmmp3/why_are_people_so_antipvp_in_a_pvp_game/

**Maintainer contract:** tie every newly raised complaint to a reproducible scenario, priority, owner/module, linked test output and a status of PROPOSED → IMPLEMENTED → HOST-VERIFIED → DESKTOP-PLAYTESTED → RELEASE-VERIFIED. Do not mark “fixed” solely because code was committed.
