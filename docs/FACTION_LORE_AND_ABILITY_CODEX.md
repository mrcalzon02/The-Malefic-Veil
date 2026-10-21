# THE MALEFIC VEIL — Faction Bible & Nine-Class Gameplay Codex
**Status:** Canonical narrative and ability-design specification, 21 October 2026. Numbers below are **provisional balancing targets**, NOT runtime-implemented mechanics. Existing Warden/Medic/Shade/Occultist abilities remain active legacy behavior until explicitly migrated and tested. No weapon or construction-development work is included.

## The fracture of the world

There was no single apocalypse. Across the continent, people began hearing a second voice speaking one instant before their own thoughts. The night skies acquired wrong constellations, coastlines wandered, and entire districts were found facing inward toward empty squares. The phenomenon was called **the Malefic Veil**: not a gateway that opened, but a boundary that forgot which side was real. The first incursion gave the dead strange shapes; the second gave the living convincing reasons to follow them.

Everyone who remained was changed. **Sanity** is the ability to resist imposed patterns, not moral virtue. A Penitent can be serene while following an atrocious belief; a Begotten can remember being human. The Unraveled are not uniformly heroic. All factions are mixtures of survivors, opportunists and fanatics, and gameplay should avoid making any faction's horror a consequence-free upgrade.

A single persistent character belongs to one faction and one of its three exclusive classes. Faction is an allegiance and narrative reputation; class is the ability kit; appearance is a separate licensed visual choice. Death and respawn do not rewrite faction identity. Defection, if ever added, must be a costly explicit server-authoritative campaign event rather than a free menu switch.

## Three signs of madness — visual identity and doctrine

### I. THE UNRAVELED — “We were here first.”

**Symbol:** *The Broken Halo*: an open ring of rough, overlapping metal, interrupted at the top by a single upright needle/stitch. Symbol must survive at 16×16 monochrome; the ring gap always points upward. Never use a closed halo, which signifies the Penitent's enclosed fate. **Palette:** oxidized teal `#497E80`, bone `#C5C0AC`, rust `#905945`. **Audio signature:** irregular three-tap metal knocks, short radio hiss, a breath held before speaking. **Markings:** hand-painted doorframes, patched fabric, white rescue chevrons, broken-circle chalk signs.

**Origin:** People who kept counting days after time stopped behaving. Their first shelters were transit stations and hospital basements. They archive street maps even as the streets change, and bind names to the living to prevent the Veil from “misplacing” them. Their loose intersettlement coalition is called the **Ledger of Names**. No leader commands the entire faction: ward captains, caravaneers and infirmary keepers negotiate survival.

**Belief:** Reality can be anchored by memory, witness and engineering. **Vice:** fear and scarcity turn protection into exclusion, theft and abandonment. **Territory:** barricaded clinics, exhausted substations, repurposed buses, rooftop signals and underpass camps. **Faction objective:** recover people, maps, power and intact records. **Enemy story:** Penitent rites invite breach growth; Begotten are terrifying but not always beyond communication.

**Banner:** broken halo above three horizontal road lines, no text required. **Spawn identity:** radio relay points, aid rooms and marked shelters.

### II. THE PENITENT — “The wound is the doorway.”

**Symbol:** *The Weeping Eye*: a pointed eye inscribed in a closed ring, with one vertical tear crossing and extending beneath the ring. At icon scale retain eye outline, single pupil and one tear only. **Palette:** candle gold `#C59C58`, dried crimson `#722C35`, ash `#B1A99E`. **Audio:** distant unison hum, one bell that rings slightly out of tune, layered whispers that fall silent together. **Markings:** stitched veils, wax seals, knotted cords, numbered confessions, devotional graffiti.

**Origin:** Survivors discovered that repeating certain phrases calmed the sky for minutes, and mistook this for proof of salvation. Their scattered choirs became the **Congregation of the Open Wound**. Their catechism holds that sanity is an infected memory of a world that never truly existed, and that the Veil is a revelation to be borne, not closed. Their hierarchy is ceremonial: an absent **First Witness** is said to hear every confession, but no one agrees on whether that person exists.

**Belief:** Suffering is evidence that one remains capable of revelation. **Vice:** ritual certainty excuses coercion, sacrifice and cruelty. **Territory:** flooded chapels, candle-lit plazas, bell towers, confession shelters and repurposed courthouses. **Faction objective:** establish pilgrimage roads, preserve relics and spread rites. **Enemy story:** Unraveled maps are blasphemous attempts to freeze truth; Begotten are heralds or warnings, disputed between sects.

**Banner:** weeping eye over a vertical row of three candle-flame shapes. **Spawn identity:** sanctuaries, vigil stations and consecrated crossings.

### III. THE BEGOTTEN — “We are what entered when you answered.”

**Symbol:** *The Split Chrysalis*: a pointed vertical cocoon cracked down its center; from the crack emerge three unequal inward-pointing tendrils. At 16×16 use a cleft lozenge and three notches, not fine organic linework. **Palette:** bruise violet `#654D79`, sickly pearl `#BAC6A6`, abyssal black `#24212D`. **Audio:** wet harmonic resonance, reversed breaths, an insect rattle that slows when watched. **Markings:** asymmetric scar patterns, phosphorescent growth, bone/keratin ridges, warped human clothing and remembered name-tags.

**Origin:** The Begotten are not a coherent alien nation. Some crossed from beyond; others were transformed when the Veil overwrote their bodies. They hear a shared pulse called **the Undertone**, but it gives no clear commands. Their temporary packs, known as **Broods**, form around feeding grounds, memories and territorial instincts. The oldest recall a human childhood; others imitate one from stolen thoughts.

**Belief:** Identity is a mutable organism; survival is adaptation. **Vice:** hunger and involuntary transformation can erase consent and compassion. **Territory:** root-choked tunnels, collapsed hospitals, flooded basements, flesh-like ruins and silent parks. **Faction objective:** reclaim spawning grounds, stabilize transformations, or deepen the breach depending on brood. **Enemy story:** Unraveled insist on impossible fixed bodies; Penitent try to own and interpret creatures who owe them nothing.

**Banner:** split chrysalis against three asymmetrical strokes suggesting limbs. **Spawn identity:** husk nests, abandoned incubators and resonant fissures. Monster faction should not spawn directly inside protected human shelters.

## Shared combat rules — mandatory class balance

All nine classes share server-owned authoritative health, sanity, locomotion, hit verification, cooldowns, energy/resources and animation telegraphs. Different silhouettes **do not** mean larger hidden hitboxes unless a documented mechanic explicitly provides that drawback. Persistent PvP must preserve readable threats and accessible counterplay at PSX resolution.

**Ability slots:** P = passive, A = primary skill, B = secondary skill, U = signature/ultimate. U uses a per-class meter (0–100) awarded for meaningful combat/support/objective actions, with server-side diminishing returns from repeat farming. Never gain U meter for idling, self-inflicted loops, allied friendly-fire or repeatedly exploiting one trapped NPC. Cooldowns count in real authoritative simulation milliseconds; UI receives next-ready server tick and meter updates. All timed effects have a stable instance ID and one host-side expiry. Default balance reference: health 100, sanity 100, and movement baseline 1.0×. Listed numerical values are starting targets only.

**Universal limits:** control-immunity grace after hard stun (5 s), cumulative movement-speed cap 1.35×, incoming damage reduction cap 40%, nonstacking identical debuffs, max 1 class U effect per player at a time, clear team-readable VFX, explicit friendly/hostile targeting. A class cannot resurrect an offline character or bypass safe spawning/raid protection. Bosses and elite NPCs use resistance tables rather than total blanket immunity. FFA faction rules are configurable by mode and must not falsely imply all faction peers are allies.

## The Unraveled — measured human survival

### 1. WARDEN — “Keep the line.”
**Role:** area defense / anchor. **Appearance:** taped heavy plates, patched respirator, broad unmistakable shoulder profile. **Motif:** one vertical stitch through the broken halo. **Class cost:** setup time and weak chase potential.
- **P — Brace:** while crouched or stationary ≥1.5 s, reduce incoming stagger by 35%; immediately lost after sprinting. Never stacks with U mitigation over global 40% cap.
- **A — Interlock:** deploy a temporary waist-high ballistic screen in permitted combat space (15 s duration; 24 s cooldown). Follows existing immutable-world and safe-spawn checks. Has 160 HP, blocks line of sight/projectiles from both sides, may be dismantled by enemies. *This is a combat ability device, not part of the parallel permanent construction system.*
- **B — Holdfast:** mark a 5 m defensive circle for 8 s (28 s cooldown); allies inside recover 20% faster from stagger and receive +15% resistance to knockback, no raw damage resistance.
- **U — Last Redoubt:** 12 s, 80 meter cost; generate a conspicuous 7 m field with up to 25% damage reduction to nearby allies and Warden, no damage amplification. Field breaks if anchor loses consciousness or becomes disabled.
- **Counters:** flank the screen, force movement, attack from higher ground, deny anchor sightlines. **Server checks:** footprint, ownership, duration, LOS, capped mitigation, team membership.

### 2. STITCHER — “You can die later.”
**Role:** emergency healing / rescue. **Appearance:** bloodstained clinic vest, surgery lamp, pack and identifiable white band. **Motif:** broken halo with two crossing sutures. **Cost:** exposed while stabilizing; limited burst protection.
- **P — Triage:** reveals allied injury severity at 15 m line-of-sight; 10% faster use time on medical items, no free generation of supplies.
- **A — Suture Shot:** heal an allied target 30 HP over 3 s (22 s cooldown; 10 m LOS). Incoming damage pauses, but does not reset, the heal; no overheal.
- **B — Field Tourniquet:** grant an ally 8 s of bleed/status resistance and clear one ordinary wound debuff (30 s cooldown). Does not clear eldritch curses or stun-immunity.
- **U — Keep Breathing:** 80 meter cost, 8 m radius, 8 s. Stabilize incapacitated teammates and pulse 12 HP every 2 s (maximum 48) to living allies, interruptible and visible. Does **not** revive fully dead players outside the server's explicit revival policy.
- **Counters:** pressure the healer, separate them from allies, interrupt line of sight. **Server checks:** range, LOS, dead/incapacitated state, healing caps and per-target application IDs.

### 3. HOLLOW — “If they see you, you waited too long.”
**Role:** reconnaissance / infiltration. **Appearance:** hood, hollow chest silhouette, veiling cloth. **Motif:** ring with its open gap visibly widened. **Cost:** fragile during exposure.
- **P — Stillness:** stationary after 1.5 s reduces visibility-gem brightness by 20% but cannot defeat proximity or objective reveal systems.
- **A — Thinning:** 7 s semi-transparent veil (28 s cooldown); exact host-computed opacity, not total invisibility. Firing or interacting with an objective immediately ends it; sprint creates a readable trailing distortion.
- **B — False Footsteps:** project an audible, non-damaging decoy to a reachable position within 12 m for 5 s (25 s cooldown); false signature is identifiable on careful observation.
- **U — Between Seconds:** 80 meter cost, 6 s; 30% movement bonus and a 2 s low-opacity onset, terminated early by firing, item pickup or objective interaction. Not a teleport or invulnerability.
- **Counters:** acoustic tracking, area denial, movement prediction, close proximity; the host still reports detectable shimmer. **Server checks:** legal position, opacity, cooldown, break conditions, decoy provenance.

## The Penitent — terror made sacrament

### 4. CONFESSOR — “Speak the thing you refuse to know.”
**Role:** sanity control / displacement. **Appearance:** severe mask with single downward tear, tall collar. **Motif:** weeping eye enclosed by crossed lines. **Cost:** slow rituals with audible windup.
- **P — Litany:** within 8 m, allied sanity drain from ambient eldritch effects decreases by 15%; does not neutralize enemy active skills.
- **A — Rebuke:** a 2 m psychic push against nearby hostiles (24 s cooldown), 0.6 s audible cast; deals modest 12 damage and displaces at most 1.5 m after collision checks. This is the migrated future form of existing Occultist repulse.
- **B — Forced Truth:** mark one target at ≤10 m LOS for 6 s (26 s cooldown); marked target's recent-sighting silhouette is revealed to the Confessor for 1 s after breaking sight; never global wallhacks.
- **U — The Great Confession:** 80 meter cost; 5 m ritual zone for 8 s. Enemies entering suffer 15 sanity damage once and muffled sound for 3 s; allies recover 25 sanity distributed over duration. Hostiles receive clear screen/audio warning.
- **Counters:** interrupt cast, retreat beyond zone, break sight before mark. **Server checks:** sanity bounds, one mark per caster, target LOS, impulse legality.

### 5. FLAGELLANT — “Pain is the proof.”
**Role:** high-risk skirmisher / sacrifice. **Appearance:** scarred arms, chains and knotted devotional cloth, no glamorized gore. **Motif:** tear crossing three notches. **Cost:** spends real health that cannot be made free through self-heal loops.
- **P — Testament:** after taking **enemy-sourced** damage, gain up to 15% speed for 4 s; cannot activate from self/environmental damage loops, 10 s internal cooldown.
- **A — Blood Price:** spend 12 current HP (cannot reduce HP below 20), gain +20% melee damage for 7 s (22 s cooldown). Buff ends on death; never boosts firearms.
- **B — Thorned Vow:** for 5 s return up to 20% of received valid enemy melee damage, capped at 25 total per cast (30 s cooldown); reflected damage cannot reflect again.
- **U — Ecstasy of Ruin:** 80 meter cost, 10 s. Up to +25% melee damage and 20% speed while visible as blazing crimson glyphs; when it ends lose 15 sanity. Cannot be activated while under 25 HP.
- **Counters:** ranged pressure, avoid direct melee trades, wait out the short window. **Server checks:** health floor, source attribution, reflection cycles, buff caps.

### 6. VOTARY — “No prayer is spoken alone.”
**Role:** positional ritual support. **Appearance:** chained votive lamps, covered eyes, layered candles/charms. **Motif:** three flames below the eye. **Cost:** static, conspicuous rituals.
- **P — Shared Burden:** within 6 m of an ally, receive 10% less ambient sanity drain; does not convert allied injury into damage to self.
- **A — Vigil Flame:** establish a luminous stationary 5 m circle for 10 s (28 s cooldown). Allies inside receive +15% status-recovery speed; enemies see the location clearly, and Hollow opacity is suppressed to a visible threshold within it.
- **B — Psalm of Binding:** channel for 1 s to slow one hostile by 20% for 3 s at ≤9 m LOS (30 s cooldown); target gains 5 s hard-control grace afterward.
- **U — Ninefold Vigil:** 80 meter cost, 12 s; 7 m audible choir zone granting allies 15% damage reduction and 15 sanity recovery over duration, and revealing approaching Begotten silhouettes at ≤7 m with clear warning. No immunity to damage.
- **Counters:** leave ritual footprint, interrupt channels, punish clustering. **Server checks:** one zone per source, allies by authoritative mode, LOS, expiry and speed limits.

## The Begotten — bodies remade by the Undertone

### 7. HARROWER — “The sound behind the last sound.”
**Role:** predator / pursuit. **Appearance:** long uneven arms, bent upper back, distinct triple-notch chrysalis. **Motif:** one tendril separated from the chrysalis. **Cost:** must isolate prey; limited direct armor.
- **P — Scent of Fear:** at ≤14 m gain directional awareness (not exact position) of enemies below 30 sanity, sampled no more than every 2 s. No through-world targeting.
- **A — Skitter:** sudden 3 m grounded dash (20 s cooldown), collision-swept and interruptible by hard control; cannot pass walls or travel through closed doors.
- **B — Listening Horror:** 5 s reveal pulse every 1.5 s of moving targets within 9 m, including noisy Hollow movement (26 s cooldown); no exact tracking of motionless targets.
- **U — The Long Hunt:** 80 meter cost, 10 s; mark one *currently visible* hostile and gain 20% movement toward their last observed location, fading after LOS lost >2 s. Mark ends on death, respawn or zone transfer failure.
- **Counters:** stop moving briefly, use sight breaks, suppress pursuer with coordinated fire. **Server checks:** collision swept dash, last-seen expiry, enemy-source checks, no occlusion bypass.

### 8. DEVOURER — “All things return to the mouth.”
**Role:** bruiser / attrition. **Appearance:** thick torso with recognizable jaw silhouette, asymmetric exposed ridge. **Motif:** enlarged cleft chrysalis. **Cost:** noisy and slow, easier to spot.
- **P — Carrion Memory:** after personally damaging a hostile who then dies within 8 s, regain 8 HP (max once per target per 30 s). No friendly kills, summons, duplicate credits or spawn-farming.
- **A — Rending Rush:** 2.5 m charge with a 0.6 s windup (26 s cooldown); 20 damage to one collided enemy and short displacement, stops on walls and vehicles.
- **B — Hardened Husk:** 5 s of 30% damage reduction (32 s cooldown), speed reduced 20% while active, conspicuous audible carapace cracking.
- **U — Hunger Made Flesh:** 80 meter cost, 10 s, +15% melee damage and 25% temporary healing from **valid enemy-sourced melee damage**, capped at 40 health per use and capped at regular max HP. No ranged lifesteal.
- **Counters:** maintain distance, evade telegraphed charge, concentrate fire after carapace expires. **Server checks:** caps, true damage attribution, collision, stacking ceiling.

### 9. MOURNER — “The city grieves through us.”
**Role:** disruption / denial. **Appearance:** hunched veiled body with hanging rib-like tendrils and slow distinctive gait. **Motif:** three drooping tendrils below the cleft. **Cost:** vulnerable while maintaining territory.
- **P — Funeral Hush:** hostile footsteps within 5 m lose 10% audio volume to Mourner only; provides no position coordinates, no global silence.
- **A — Grief Pool:** create a 4 m radius visible corrupted ground region for 7 s (28 s cooldown); hostiles inside suffer 12% slow and 2 sanity loss per second; no stacked duplicate pools.
- **B — Echo of Loss:** cast a 6 m auditory disruption field for 4 s (30 s cooldown); interrupts minimap sound indicators but never disconnects controls, removes HUD or causes photosensitivity flashes.
- **U — Black Lament:** 80 meter cost, 8 m field for 10 s. Hostiles lose up to 20 sanity over duration and have their ambient visibility reduced (never completely black screen); allies gain clear visual silhouettes of trapped opponents within zone. Visible perimeter and a distinctive ascending tone announce it.
- **Counters:** retreat from zone, long-range elimination, disperse rather than bunching. **Server checks:** field bounds, per-target tick caps, no forced blind/total control suppression.

## Faction interactions, diplomacy and objectives

| Pair | Central conflict | Gameplay objective hook |
|---|---|---|
| Unraveled ↔ Penitent | Records and engineering against ecstatic revelation | Rescue archive witnesses vs occupy a confession station |
| Unraveled ↔ Begotten | Shelter preservation against mutation and feeding | Escort survivors vs capture anomalous nests |
| Penitent ↔ Begotten | Worshippers seeking control of what cannot be owned | Consecrate a breach vs defend its untamed pulse |

**World events:** *The Names Go Missing* (Unraveled archive rescue); *Bell Without Tower* (Penitent rite that bends sound); *The Undertone Rising* (Begotten territorial awakening); *False Dawn* (all three contest a briefly stabilized city district). Events should permit neutral and cross-faction noncombatant NPCs; all Begotten are not automatically “evil mob fodder.” Faction advantages must never grant unrestricted access to another faction's protected settlements.

**Optional reputation:** favor with home faction and separate bilateral standing; reputation gains from objective completion not idle PvP farming. Faction NPC aggression and world spawn points can read server-owned standings. Do not hardcode “every different faction is an enemy” in PvE or alliance modes.

## Iconography delivery specifications

Provide a common **icon sheet** with one 64×64 source icon per faction, one 32×32 monochrome HUD glyph and one 16×16 reduced version. Colors are *secondary* to recognizable shape. Bake no text into icons. Icons must show on light/dark backgrounds, distinguishable in grayscale and for common color-vision differences. A class crest combines its faction's fixed outer sign with a unique interior mark. Prototype linework is described above; finished graphic art is a separate illustration/asset task.

**Selection UI:** one compact faction row, three class columns, preview, P/A/B/U strips with precise cooldown and counterplay tooltip, playable/pending status, and separate appearance chooser. Never let preview faction visuals change the saved server affiliation. Tooltip must distinguish “concept target” versus “live skill.” Be robust to 320×200 gameplay rendering and 1024×640 Swing shell.

## Integration roadmap / proof gates

1. **Lore + registry freeze:** stable IDs for faction, class, symbols, abilities, events. This document is the balance/lore contract; not yet executable.
2. **Simulation layer:** event source tagging, status effects, duration/cooldown/charges, ability meters, enemy/ally filtering, LOS and collision sweeps; invariant tests for server tick, pause and replay.
3. **Implement five missing classes:** Flagellant, Votary, Harrower, Devourer, Mourner with functional P/A/B/U, actual VFX and combat logs. Migrate four legacy roles without breaking existing PvP.
4. **Character persistence and authority:** faction/class/appearance as independent verified server-owned fields; reconcile sessions, deaths, save/restores, cross-zone transfer and profile migrations.
5. **Art + audio:** licensed archive model selection, custom monster meshes, 3 LODs, skeletal animation or explicitly accepted low-fi animation, verified audio telegraphs, favicon/crest graphics.
6. **Balance QA:** 1v1/3v3/9-player mixed-faction 100-match deterministic bot suites; counterplay matrices, spawn camping prevention, cooldown exploit tests, RTT 50–250 ms and packet loss. Tune targets with measured logs.
7. **Public release:** physical two-client playtesting, accessibility/flash/audio options, fairness telemetry and dedicated-host rollback procedures.

**Current code truth:** `FactionClasses.java` has all nine planned class identifiers; `ArenaSystems` recognizes four legacy class abilities; only four fallback hero models are playable. This codex does not upgrade unavailable classes to playable or authorize use of unlicensed FBX.
