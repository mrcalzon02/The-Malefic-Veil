# Thrown field deployables: individual purpose review

**Decision: reject all 21 current candidates for production. Redesign required.**

Reviewed October 24, 2026. I built a shared shape generator before resolving each object's design. That produced repeated cabinets and pedestals for two factions and repeated smooth pods for the third. This was an authoring failure. Texture baking, named animation clips and geometry checks do not correct it.

Evidence: every current closeup inspected individually; the previously inspected front, rear and clay roster views; four sampled car/character reference renders; the supplied production specification; and the generator and runtime source. This review does not claim that the animations have all been visually watched or that these candidates passed a live GPU gameplay test. No candidate has been promoted into the approved runtime directory.

## Batch failures

- **Independent faction design is missing.** Penitent reuses matching Unraveled geometry with color and ornament changes. Begotten support roles converge on almost interchangeable pods.
- **Purpose is indicated by symbols instead of components.** Crosses identify medical boxes; exposed cartridges identify ammunition boxes; glowing beads identify energy devices. Their actual treatment, feeding and projection mechanisms are missing.
- **Portability is unproven.** The generator scales the entire root to 0.19 for packed/thrown/impact states, then enlarges it during deployment. Shared hinge rotations do not prove that rigid components can nest inside a throwable capsule. Biological growth can add volume within the fiction, but needs specific connected growth rather than an enlarged finished organism.
- **Animation names overstate behavior.** Nine clips exist, but most joints share rotation formulas and timing. Changed vertices do not establish a functioning linkage, constrained recoil, chamber contraction or membrane growth.
- **Persistent damage and depletion are missing.** The pose selector does not use health fraction or ammunition stock to produce enduring visible deterioration or depletion. A brief wobble does not meet that requirement.
- **The authored collapse is not integrated.** Removed entities disappear and receive the shared harmless burst; the authored DESTROYED clip does not play through that path. Zero explosion damage must remain, but the requested visual collapse is absent.
- **Materials lack information.** Generic mottling does not establish fasteners, seams, relief carving, tissue attachment, membrane thickness or vascular routing. Low resolution needs carefully selected detail, not uniform noise.
- **Technical validation is limited.** Loader/transform/atlas tests do not prove manufacture, packing, collision agreement, effect visibility or art suitability. The GPU build remained unverified after dependency/certificate failures.

## Unraveled

### 01. Bastion Panel — wall

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-wall/closeup.png>)

**Reject.** The role is recognizable, but the construction is not convincing.

The brief calls for a compact armored capsule becoming roughly 2.2 m-wide, 1.8 m-high reinforced cover through hinged plates, hydraulic braces and locking feet. The actual object is two broad rectangular plates with three regular strips each, cyan squares and the repeated small central stand. It resembles two oversized doors on a pedestal. Thickness, repair construction, hinge knuckles and structural connections are poorly articulated.

The central seam and narrow support arrangement weaken its protective and load-bearing story. Rotating the plates while enlarging the entire object does not explain how full-height rigid armor fits into a small capsule. The rear needs an understandable load path, not decorative diagonals.

**Redesign:** start with a split armored cassette containing nested overlapping plates. Use real hinge barrels, travel stops, diagonal extending braces and broad feet aligned with the panel loads. Establish the packed plate stack first.

**Acceptance:** demonstrate useful continuous cover, supported plate edges, physical nesting without arbitrary shrinkage, and collision checked against the full deployed and moving envelope.

### 02. Razorframe — barricade

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-barricade/closeup.png>)

**Reject.** It reads as a fence fragment rather than a deployable obstruction.

The visible X is an appropriate starting motif, but its center has no convincing pivot, its ends have no sliding joints, and the plain posts do not telescope. Short upright rods suggest a fence rather than deliberately deployed anti-personnel rails. The isolated middle housing does not explain what drives or retains the outer frame.

There is no visible chain of forces from anchors through braces to obstruction rails. A shared rotation applied to the crossed groups is not a scissor linkage: the crossing members need offset thicknesses, pinned ends, constrained movement and a locking position. Small generic feet do not convincingly stabilize the full span.

**Redesign:** two nested end frames joined by an actual scissor assembly, with an offset center pin, sliding end blocks, expanding rails and folding ground claws. Define how each rail clears its neighbor while stowed and deploying.

**Acceptance:** prove compact stowage, nonintersecting expansion and stable anchoring. Its lower, porous silhouette must remain distinct from the solid wall at gameplay distance.

### 03. Sentry Mk.I — turret

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-turret/closeup.png>)

**Reject.** Two barrels identify a gun, but the weapon assembly is largely absent.

The current model is a cuboid receiver, two uniform rods, a cyan bead and a narrow stalk on the shared base. It has no readable ammunition storage, feed path, breech, ejection, barrel support or recoil guide. The sensor is a colored bead rather than protected optics. The tall pedestal gives little sense of stability under fire.

The specified independent yaw and elevation need a turntable and trunnioned cradle that can actually move through the required envelope. Named nodes alone do not establish that. The generated operating clip rotates recoil groups and shifts them; meaningful recoil should follow the barrel axis through visible guides and return hardware.

**Redesign:** a low ammunition/power cassette, supported yaw bearing, elevation cradle, segmented receiver housings, paired guided barrels and an offset sensor cluster. Make the ammunition route and muzzle direction obvious.

**Acceptance:** check aim limits and clear firing arcs. Confirmed host attacks must drive bore-aligned recoil and feed movement; decorative idle motion must not simulate firing.

### 04. Field Medic — health station

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-health/closeup.png>)

**Reject.** The cross carries nearly all of the medical identification.

The brief requires a folding case with instrument trays, pharmaceutical cartridges, injector reservoirs, hoses and treatment indicators. The render shows a dark cabinet, empty shelves, small caps and a bright cross. There is no readable patient-facing injector, tool arm, dispensing socket or medicine delivery route.

The model does not answer how an ally receives treatment, where consumables are protected or how the case closes. Tray rotation is relevant, but generic motion of the shelves is not a treatment cycle. The actual pumps and instruments should perform the operational action; lights should support it.

**Redesign:** a rugged clamshell case with a recessed drug cassette, hose reels, a fold-out injector arm and instrument trays with retained tools. Differentiate supply access from the patient-facing side. Use the emblem as a secondary cue.

**Acceptance:** hide the cross and lights; it should still read as medical equipment. Show packed closure, usable access and a pump/tool action linked to successful authoritative healing.

### 05. Munition Feeder — ammunition dispenser

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-ammo/closeup.png>)

**Reject.** It displays an ammunition symbol rather than a supply mechanism.

Six oversized vertical rounds sit on the face of a box above a flat ledge. No magazine bank, indexing drive, retention hardware or delivery path is visible. The common stalk/plinth supports it without serving a specific dispensing purpose.

The viewer cannot distinguish stored ammunition, a selected charge and supplied ammunition. There is no protected volume that plausibly contains the finite supply, and no geometry that explains how a round reaches the tray. Rotating the entire feed rack does not animate ammunition advancing through a guide. Remaining stock is present in the host view but unused by candidate pose selection.

**Redesign:** a low opening case with a magazine carousel or belt cassette, indexing pawl, guide rails and an accessible receiving bay. Make selection and delivery separate visible steps.

**Acceptance:** follow one item from storage to delivery. Each successful resupply must cause a clear indexed action; empty stock must stop delivery and leave a readable depleted state.

### 06. Vapor Engine — miasma generator

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-miasma/closeup.png>)

**Reject.** It suggests industrial exhaust hardware but does not explain chemical dispensing.

The two cylinders and bent extensions establish a useful broad motif. However, the tiny ring-like valves, undifferentiated pipe ends and central assembly do not define filling, regulation, fan flow or pressure indication. There is no readable gauge or clear separation between reservoir, intake and output.

The exhaust heads appear capped or visually ambiguous. The viewer cannot trace a flow path from chemical storage to discharge. Generic vent rotations do not establish a valve opening, and a rotor node is insufficient without correctly oriented fan geometry. These images do not demonstrate vapor emitted at a real aperture or matched to the authoritative effect radius.

**Redesign:** a squat pressure skid with strapped bottles, a regulator manifold, readable gauge face, venturi/fan housing and open or perforated exhaust heads. Route short visible pipes between them.

**Acceptance:** show pressure/fan action and emission from actual outlets. The field must follow authoritative activation and the three-metre effect radius without changing gameplay calculations.

### 07. Aegis Projector — shield generator

[Inspected closeup](<../previews/field-deployables/thrown-unraveled-shield/closeup.png>)

**Reject.** The defining protective surface is missing.

A central glowing lump sits between two rings and four thin lamp-topped rods on the repeated base. This reads as an ornamental electrical stand. The rods do not communicate stabilization, the emitter has no engineered housing, and the spheres/rings are symbols of energy rather than a coherent projection mechanism.

No candidate dome is authored. As a result, there is no visible expansion into the protective radius and no surface to flash or deform on absorption. Generic rotations of rods and rings do not establish clamps engaging, a projector rising and an emitter aligning.

**Redesign:** a low power cassette with four substantial folding outriggers feeding a protected gimballed projector. Use directional coils or segmented emitter petals with a defined deployed alignment. Author the translucent protective field separately from the physical generator.

**Acceptance:** match the actual 2.4 m protective radius, keep actors readable through the dome and respond to absorbed attacks. Show clear support locking and field expansion from the emitter.

## Penitent

### 08. Wall of Contrition — wall

[Inspected closeup](<../previews/field-deployables/thrown-penitent-wall/closeup.png>)

**Reject.** This is the Bastion Panel with palette and ornament changes.

The prescribed carved masonry, opening reliquary, rising slab segments and retaining ironwork are absent. The same rectangular plates, strips, central housing and feet remain. Amber squares, red spheres and a red rectangle do not establish religious manufacture or an independent silhouette.

The construction narrative is wrong: shared hinged-panel motion cannot represent ascending masonry. The slabs lack carving, stepped joints and thickness hierarchy. The hanging ornaments do not read as worked objects with meaningful suspension. Even neutral clay reveals the same engineering-faction asset.

**Redesign:** an iron-bound reliquary whose opening leaves become retaining arms. Raise interlocking carved slabs into a stepped defensive arrangement, with an integrated devotional relief and carefully placed votive recesses. Design the actual stowed slab arrangement.

**Acceptance:** distinguish it from Bastion Panel with materials and lights removed. Prove how stone segments rise, engage and lock. Every ornament must have understandable construction and attachment rather than being a bead added to the base.

### 09. Warding Cross — barricade

[Inspected closeup](<../previews/field-deployables/thrown-penitent-barricade/closeup.png>)

**Reject.** The appropriate crossed motif has not become a distinct ritual device.

The Unraveled X-frame is reused with pale bars, the same upright rod row, red ornaments and the same base. The plain end stakes do not read as reliquary pillars; there are no engraved mechanisms, nested collars or suspended censers with believable vessels.

Its construction remains an engineering barricade with ceremonial colors. Shared rod rotations do not demonstrate extending pillars, independently swinging ornaments or a precise locking sequence. The frame needs to explain how the two sides are physically connected and how their warding members deploy together.

**Redesign:** paired iron-caged, carved pillar capsules. Pivot the protective rods from separate collars and engage them at the crossing. Hang recognizable pierced censer bowls below their engagement points, on properly attached chains.

**Acceptance:** prove that pillars and rods pack together and deploy through connected mechanisms. Make a low ritual obstruction, clearly different from both the Unraveled barricade and the Penitent solid wall.

### 10. Litany Engine — turret

[Inspected closeup](<../previews/field-deployables/thrown-penitent-turret/closeup.png>)

**Reject.** A conventional twin-barrel gun has been decorated rather than redesigned.

The Sentry receiver, stalk and barrels remain dominant. Pale crown prongs and an amber bead do not supply a sanctification chamber, sacred emitter aperture, rotating prayer mechanism or incense vents. It reads as the same mechanical weapon with a crown.

This fails the requested independent operating principle. The inherited gun-style groups do not stage an intelligible charge and emission cycle; moving crown pieces cannot carry that entire story. A ritual weapon still needs an unambiguous attack axis and a mechanism that points it.

**Redesign:** an iron reliquary cradle surrounding a directional sanctification chamber. Place the attack aperture behind articulated seal plates. Use a geared prayer wheel or engraved rotating shutters to expose, charge and reseal the chamber; include actual incense discharge hardware.

**Acceptance:** neutral clay must separate it from Sentry Mk.I. Host aim must align the aperture, and a confirmed attack must drive a specific charge/open/discharge/recover sequence rather than decorative looping.

### 11. Shrine of Mercy — health station

[Inspected closeup](<../previews/field-deployables/thrown-penitent-health/closeup.png>)

**Reject.** It remains the mechanical medical cabinet in ceremonial colors.

The amber cross, pale shelves and red flap do not create an altar. There is no useful devotional surface, recessed reliquary chamber, articulated column or legible lantern/candle construction. Small caps on top do not carry the requested sacred lighting function.

The object does not explain where the protected relic sits, how an ally approaches it or how its manufacture differs from medical equipment. Inheriting the tray groups also inherits the wrong unfolding story. The healing action needs a specific relic/lantern mechanism rather than generalized shelf movement and glow.

**Redesign:** a hinged altar chest whose leaves unfold into a compact devotional surface. Reveal a recessed relic chamber, low extending columns and hanging lanterns. Give the ceremonial cloth visible folds, thickness and attachment points.

**Acceptance:** it must read as an altar when the cross and lighting are hidden. Successful healing should visibly engage its own mechanism while the existing host health and sanity calculations remain authoritative.

### 12. Arsenal Reliquary — ammunition dispenser

[Inspected closeup](<../previews/field-deployables/thrown-penitent-ammo/closeup.png>)

**Reject.** The defining circular supply and consecration architecture is missing.

It reuses the Munition Feeder cuboid, cartridge display and tray with red rounds and pale tips. The red spheres and plaque below are a repeated ornament package. They do not explain sacred storage or supply.

There is no circular rack, central chamber or selected receptacle travelling toward the ally. The viewer cannot follow ammunition through storage, consecration and delivery. The inherited rack rotation is not a distinctive prayer mechanism, and the generic box does not communicate a finite reliquary inventory.

**Redesign:** an opening reliquary around an annular ammunition rack. Index one charge through an enclosed central chamber, then slide or lower a receiving cup toward the user. Put seals and inscriptions on actual structural surfaces rather than using detached symbols.

**Acceptance:** trace a selected charge through all three stages. Successful server-authorized supply must trigger delivery; depleted stock must be readable. Its clay silhouette must differ from the Unraveled case and Begotten incubator.

### 13. Censer of Dread — miasma generator

[Inspected closeup](<../previews/field-deployables/thrown-penitent-miasma/closeup.png>)

**Reject.** Recolored industrial exhaust pipes do not constitute a censer.

The Vapor Engine cylinders and bent stacks are retained in pale/dark materials, with the same small red spheres below. There is no substantial suspended main vessel. A cross on the body cannot explain an incense burner.

The defining working components are missing: bowl, lid, perforations, combustion chamber and suspension. These are the object's functional anatomy. Shared vent rotations do not become chain sway, vessel rotation or lifting censer motion. The current assembly is fundamentally the wrong substrate.

**Redesign:** a folding iron tripod or small gantry raising a pierced censer bowl on a chain. Add a few smaller suspended vessels at unequal heights, with lids, vents and actual chain attachment points. Give the support frame and lifting mechanism enough substance to carry them.

**Acceptance:** smoke must originate from the vessels and follow host activation/radius. Verify suspension, clear pendulum travel and a visibly distinct raising/opening ritual instead of industrial valve motion.

### 14. Sanctuary Ward — shield generator

[Inspected closeup](<../previews/field-deployables/thrown-penitent-shield/closeup.png>)

**Reject.** It is the Aegis stand recolored, and the ward itself is absent.

The same thin rods, bead tops, rings and central glowing lump remain, supplemented by the standard plaque and spheres. The supports lack the massing and detailing of ceremonial pillars; the center is not a protected lantern with an interior.

The four-way arrangement is relevant, but does not establish independent sacred construction. Generic rotations cannot demonstrate ascending pillar capsules and unfolding ornamentation. No transparent field exists for the required illuminated impact sigils, so the main protective function is not represented.

**Redesign:** four independently rising iron-bound pillar capsules around a hanging lantern. Open articulated top leaves inward toward the light; use a few properly suspended relic boxes to articulate its ritual mechanism. Author the amber field as a separate transparent surface.

**Acceptance:** prove pillar extension and lantern activation. Match the 2.4 m field radius, retain actor visibility and drive protective sigils from real absorption events on the field surface.

## Begotten

### 15. Ossuary Rampart — wall

[Inspected closeup](<../previews/field-deployables/thrown-begotten-wall/closeup.png>)

**Reject.** A connected purple mass has not become a skeletal defensive organism.

The current object is a smooth comb-like body with six pale tapered strips on its front. The strips resemble attached paddles. There are no vertebral joints, readable rib arches, cartilage seams or vascular transitions to explain their anatomy.

The broad mass gives some wall recognition, but smooth blending and the unstructured upper gaps weaken the distinction between rigid bone and flexible tissue. Actual remeshing solves connectivity, not anatomy. Rotating separate strips does not show bones growing or ribs curling while connective tissue remains attached.

**Redesign:** vertebral buttresses joined by overlapping rib arches, with tendon and membrane spanning their intervals. Integrate bone emergence and tissue attachments into the anatomy. Vary rib size around a clear structural rhythm; use thorns as secondary growth rather than the entire identity.

**Acceptance:** clay material should reveal a supported skeletal wall. Growth must preserve connected tissue and end in useful continuous cover without detached decorative bone strips or arbitrary enlargement of a finished wall.

### 16. Grasping Thicket — barricade

[Inspected closeup](<../previews/field-deployables/thrown-begotten-barricade/closeup.png>)

**Reject.** Blunt pale posts do not communicate grasping or entanglement.

The low purple root mass is a useful starting scale, but six cut-off-looking rods dominate the front. Living branches lack the hooked tips, joint hierarchy and tendon tension expected of an aggressive obstruction. It resembles a miniature fence rather than limbs reaching to catch an enemy.

The bone groups use the same simple rotation recipe as the wall. That does not create a travelling curl through a flexible appendage or explain attachment at the rooted body. The grasping action should be visible in both silhouette and motion, not inferred from a role label.

**Redesign:** a low radial root knot with a few asymmetrical segmented claw limbs. Give each a broad tendon base, narrowing joint chain and inward hook. Add short cartilage barbs between those larger primary limbs.

**Acceptance:** maintain a distinct low obstacle silhouette, continuous limb bases and a convincing unfolding/grasping cycle. Verify the hostile obstruction footprint and avoid detached tips, rigid post gestures and whole-object shrinkage as deployment.

### 17. Spore Spitter — turret

[Inspected closeup](<../previews/field-deployables/thrown-begotten-turret/closeup.png>)

**Reject.** The anatomy reads as a small mascot rather than a biological weapon.

A round smooth head sits on a tapered stalk, with a lime bead and two sideways pale prongs. There is no mouth aperture, visible pressure membrane or differentiated chitin base. The viewer cannot identify where a projectile would leave the body.

The prongs lack jaw sockets and look attached. The bead supplies the entire eye cue without orbital anatomy. Named eye/jaw groups can move, but there is no actual maw to open or gland to compress. Rigid head movement and generic prong rotation cannot establish the buildup and release of pressure.

**Redesign:** a supported, forward-leaning muscular stalk ending in an open throat with thick lips. Position the tracking eye above or beside that aperture. Connect mandibles through defined sockets and expose a rear pressure sac with longitudinal muscle bands.

**Acceptance:** host aim must align the throat. A confirmed attack must visibly tense the gland, open the maw, expel from the aperture and recover through connected musculature.

### 18. Regeneration Heart — health station

[Inspected closeup](<../previews/field-deployables/thrown-begotten-health/closeup.png>)

**Reject.** The object cannot be identified as a medical organ without its label.

The render shows a smooth upright purple pod, angular tabs and a green patch. Intended ventricles merge into a nearly featureless silhouette. No vessel network, nutrient sacs, treatment appendages or circulation route is legible.

The green patch is a color cue rather than an explanation of restoration. Rotating rigid tabs does not produce a contracting heart, and the vascular body has no meaningful authored chamber deformation. Compared with the other support pods, the role-specific anatomy is almost absent.

**Redesign:** two unequal muscular chambers with a strong connecting cleft, branching vessel arches and smaller nutrient sacs. Anchor them in a low living root organ. Give the ally-facing side soft treatment tendrils with clear tissue bases and a readable nutrient route.

**Acceptance:** an unlit clay silhouette must identify an anatomical pump. Show genuine chamber contraction and circulation/tendril activity during successful restoration, preserving tissue continuity rather than wobbling armor tabs around a rigid pod.

### 19. Brood Magazine — ammunition dispenser

[Inspected closeup](<../previews/field-deployables/thrown-begotten-ammo/closeup.png>)

**Reject.** It lacks readable brood chambers and an actual release path.

A rounded cluster with angular tabs sits on the same rooted pedestal used by the other support organs. Its lobes merge so strongly that separate egg chambers and membranes are difficult to distinguish. There is no clear central nutrient organ or exit lip.

The model does not read as an incubator or dispenser. A viewer cannot identify a selected capsule, distinguish available stock or see how a grown munition separates from its living attachment. The tabs rotate generically; no chamber contracts and no capsule is released. Finite host stock is not represented by distinct chamber states.

**Redesign:** a low brood cradle with several individually readable membrane-wrapped capsules, joined through short vascular stalks to a central nutrient organ. Add one unmistakable release lip and preserve the connected living network.

**Acceptance:** trace a capsule from attached chamber through contraction and separation to delivery. Successful host resupply must drive release, with visibly depleted chambers after supply ends.

### 20. Blight Spore — miasma generator

[Inspected closeup](<../previews/field-deployables/thrown-begotten-miasma/closeup.png>)

**Reject.** The swelling does not explain spore production or expulsion.

The current object is a tall, smooth purple mass with minor side protrusions and no readable emission openings. Its surface and rooted lower body resemble the health and ammunition pods. The intended pustules do not resolve as distinct pressure organs.

No stomata, membrane lips or protective ribs identify where spores emerge or how the organ supports pressure. Rotating tiny carapace elements does not open an organic vent. The primary sac lacks a specific inflation/contraction cycle and the output is not tied to a visible anatomical aperture.

**Redesign:** a squat distended sac restrained by irregular protective ribs. Place secondary pustules around clear sphincter-like stomata; differentiate taut thin membranes from thick structural tissue and vascular feeding routes.

**Acceptance:** show a connected organ swelling, opening local vents and contracting during authoritative emission. The source and three-metre effect must be legible, and its silhouette must remain distinct from the heart and incubator.

### 21. Living Mantle — shield generator

[Inspected closeup](<../previews/field-deployables/thrown-begotten-shield/closeup.png>)

**Reject.** The protective membrane, the defining part of the design, is missing.

The object repeats the upright pod and green patch, with extra outward spikes. Its rib-like pieces do not span a membrane or explain a broad protective envelope. It remains difficult to distinguish from the other support organs.

A crown of spikes cannot demonstrate how a mantle is stretched and supported. Generic rib rotations neither grow membrane nor preserve attached seams. There is no translucent surface to ripple under impact or visibly recover during regeneration, leaving the principal gameplay function without a visual representation.

**Redesign:** a low central organ with radial rib fans and tendon-web attachments. Open these into a broad canopy while a thin continuous membrane stretches between defined seams. Treat the compact generator and the full protective field as related but distinct anatomical systems.

**Acceptance:** preserve membrane attachments throughout motion. Keep the envelope transparent, match the 2.4 m protective radius and drive impact ripples/recovery from real host absorption and regeneration.

## Corrective production approach

Do not extend the current generic generator as the primary design method. Keep these candidates as rejected studies. The loader and host-state changes need their own engineering review; they do not justify the art.

For each redesign, establish a small functional drawing first: packed form, deployed silhouette, named moving parts, material boundaries, ally-facing interaction and output direction. Build both forms around the same actual components. Mechanical and Penitent items need purposeful joints, travel limits and locks; Begotten items need connected anatomy with distinguishable structural tissue and organ actions.

Use one pilot at a time before expanding across the roster. A rebuilt Sentry Mk.I tests mechanical packing, ammunition routing, independent aiming and guided recoil. A Censer of Dread and Regeneration Heart can then prove religious manufacture and living anatomy independently. Do not start with 21 variants of another generic base.

Review pilots at player eye height and gameplay distance, with labels and lights disabled. Watch the actual animation sequence, including packing and transition clearances. Author operation, injury, depletion and death from the device's own machinery/anatomy.

Separately verify authoritative events, inventory consumption, finite supply, flight and landing clocks, ownership, late-join snapshots, animated collision envelopes, exact visual field radii and harmless destruction. Run the real GPU presentation before claiming gameplay integration.

**Disposition: no approved production asset from this batch.** The companion JSON binds the individual findings to the current GLB and four render SHA-256 hashes. No geometry was modified during this review.
