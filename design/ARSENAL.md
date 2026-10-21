# Arsenal proposal — 32 weapons

Original arena-horror arsenal for The Malefic Veil. Fast swaps, readable projectiles, expressive alternate fires, and a reason to carry more than one range profile. All damage/range values are tuning proposals; these assets do not expand authoritative multiplayer combat by themselves.

## Build contract

Use the local mech-detail and Greebles libraries. Keep weapon root, receiver, grip, magazine, barrel/rotor, breech/hinge, vents and muzzle/eject sockets separate. Idle, fire, alternate and reload clips use rigid component movement. Build the silhouette before optimizing; reduce each mesh separately and retain the editable assembly. No blind boolean union across moving seams. Artillery, multi-barrel, pronged electric and ring-field silhouettes must read differently without color.

## Roster

| Weapon / shape | Primary | Alternate | Counterplay | Motion |
|---|---|---|---|---|
| w01-vesper — pistol | Accurate semi-auto flechette; 24 damage, 4 shots/s | Three-round fan; 3 x 15 damage, 0.7 s recovery | Weak against armor; fan loses range | slide |
| w02-ossuary — revolver | Six-shot hand cannon; 68 damage, 1.2 shots/s | Piercing charged shot; 95 damage, consumes two rounds | Charge whistle and slow follow-up | cylinder |
| w03-needle-choir — smg | 18 microdarts/s, 7 damage each; climbs with heat | Six-dart tight burst; 0.35 s pause | Short reach and exposure while spraying | bolt |
| w04-rivet-saint — rifle | Heavy rivets, 32 damage, 5 shots/s | Pinning rivet slows for 0.6 s; 2 ammo | Slow visible projectiles can be dodged | bolt |
| w05-grave-bell — double | Two-barrel shotgun; 10 x 11 damage, 0.9 shots/s | Both barrels; 20 x 9 damage with strong self recoil | Low effective range; break-action reload | hinge |
| w06-cerberus — triple | Three-barrel sequential buckshot; 8 x 10 damage | Triple blast; 24 x 9 damage, 1.8 s recovery | Three-shell reload leaves an opening | hinge |
| w07-reliquary — shotgun | Pump shotgun; 12 x 9 damage, 1 shot/s | Tight slug; 85 damage, 1.1 s recovery | Pump tells enemies when to advance | pump |
| w08-furnace-organ — rotary | Six-barrel minigun; 20 x 8 damage/s after spin-up | Brake rotor and fire wide 12-pellet burst | 0.65 s spin-up; heat forces venting | rotor |
| w09-hailstorm — rotary_heavy | Twin rotary banks; 24 x 9 damage/s | Anchor stance halves spread, reduces movement | Heavy and conspicuous; must cool after sustained fire | rotor |
| w10-cautery — laser | Instant red laser pulse; 42 damage, 3 shots/s | Continuous cutting beam; 85 damage/s, heat limited | Beam traces expose position; poor burst | capacitor |
| w11-prism-writ — prism | Three split laser rays; 3 x 22 damage | Focus all prisms; 100 damage after 0.8 s charge | Prongs expose charge state; cover breaks line of sight | prongs |
| w12-longwake — rail | Rail shot; 125 damage, 1.4 s cooldown, pierces one target | Zoomed low-power shot; 78 damage, 0.7 s cooldown | Bright charge and long trail reveal sniper | bolt |
| w13-ion-psalm — plasma | Large plasma bolts; 38 damage, 5 shots/s | Slow plasma orb; 110 radial damage, 4 ammo | Orbs are visible and can be outrun | capacitor |
| w14-sun-eater — beam_heavy | Short-range beam; 150 damage/s, ramps over 0.6 s | Overload lance; 200 damage, 2 s charge, entire core | Long vent and obvious glow after overload | vents |
| w15-storm-rosary — tesla | Chain lightning; 48 first hit, 0.7 falloff, 3 jumps | Conductive anchor orb; chains near impact for 3 s | 8 m link limit; cover and spacing break chains | prongs |
| w16-forked-benediction — fork | Twin direct arcs; 2 x 28 damage, 5 pulses/s | Ground fork creates 2 s electrical tripline | Cannot chain through walls; visible ground tells | prongs |
| w17-ball-lightning — orb | Slow seeking orb; 75 damage, 5 s lifetime | Detonate orb early; 110 radial damage | Shootable orb and broad electric sound | capacitor |
| w18-thunder-loom — arc_rotary | Rotating arc stream; 95 damage/s with 2 chain jumps | Discharge all heat into a 4 m pulse, 80 damage | Danger at close range but ineffective across open lanes | rotor |
| w19-pilgrim — rocket | Straight rocket; 105 direct + 70 splash damage | Hold three rockets for a staggered salvo | Self splash; charge creates audible lock rattle | rack |
| w20-widow-choir — missile | Four micro-missiles; 4 x 32 direct damage | Paint target for 0.8 s then limited-turn homing volley | Lock warning; obstacles and sharp turns defeat guidance | rack |
| w21-basilisk — rocket_heavy | Heavy rocket; 150 direct + 110 radial damage | Airburst at sighted range; 18 x 10 fragments | Slow reload and visible flight; fragments respect walls | bolt |
| w22-censer — grenade | Bouncing grenade; 100 damage, 1.8 s fuse, 3 bounces | Sticky grenade; 80 damage with manual detonation | Grenade glow and audible bounce; owner splash | drum |
| w23-vigil-mortar — mortar | Arcing mortar; 130 radial damage, 2.4 s flight cap | Low lob cluster; 5 x 38 damage bomblets | Ground marker for 0.7 s; roof and cover block blast | hinge |
| w24-cathedral — artillery | Deploy braces and lob siege shell; 220 damage, 4 s cycle | Smoke shell hides an 8 m lane for 6 s | Immobile while braced; smoke also obscures owner | braces |
| w25-shrapnel-mass — flak | Direct hot fragments; 14 x 10 damage | Lob flak shell; 90 radial damage and short-lived shards | Falls off fast; shards have one bounce and 0.6 s life | pump |
| w26-ricochet-gospel — disc | Saw disc; 65 damage, 3 predictable wall bounces | Recall disc; 45 return damage, 1 hit per target per leg | Owner can be struck; bright trajectory ribbon | disc |
| w27-bankshot — bounce | Elastic bolt; 50 damage, gains 15 on first wall bounce | Three-bolt bank fan; 3 x 32 damage | Maximum 4 bounces / 3 s; sharp bounce sound | capacitor |
| w28-scrap-cyclone — scrap | Scrap cone; 18 x 8 damage, consumes 4 scrap | Magnet charge and release penetrator; 115 damage | Charged scrap is loud; cannot consume quest items | drum |
| w29-miasma — bio | Corrosive glob; 28 direct + 36 over 3 s | Lob gel puddle; 25 damage/s for 4 s in 2 m radius | Distinct green puddles; burn damage never stacks indefinitely | tank |
| w30-winter-verdict — cryo | Cryo spray; 55 damage/s, caps slow at 25 percent | Ice lance; 80 damage, shatters own chilled targets for +30 | Short spray range; no hard stun or input lock | tank |
| w31-eventide — gravity | Gravity slug; 60 direct damage with slight lateral pull | 2 s vortex pulls projectiles within 3 m; 120 damage cap | Visible warmup; never pulls players through cover | rings |
| w32-veilbreaker — singularity | Rare superweapon; 240 damage implosion in 5 m after 1 s | Focused 180 damage lance without radial pull | Loud global cue, 2.5 s windup, one-core magazine | rings |

## Asset delivery and limits

Editable high-detail assemblies, component-animated GLBs and reduced static GLBs are local prototypes. Textures are an authored shared trim atlas; material families use dark steel, ceramic, brass, rubber, warning bands and emissive accents. Firing/charge effects are proposed in data; emissive cores and moving parts are visual placeholders, not full particle or sound effects.

Runtime integration must preserve host authority, finite projectile lifetime/bounces, line-of-sight chain damage, damage caps, owner splash, and distinct ammo consumption. The existing two-weapon live game remains intact until these combat families are implemented and tested. Food/water are inventory design entries only; hunger/thirst is deferred.

The public repository forbids publishing complete purchased packs. Source and derived mesh/texture binaries stay in ignored local folders pending redistribution evidence. Reproducible recipes, source fingerprints, reports and original text/code are committed.
