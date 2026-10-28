# Razorframe feasibility review: r3 remains a candidate

Expanded inspection distinguished correct rod endpoints from collision-free parts. r1 passed the linkage test but failed 2,920 rod/solid-guide pair samples in its 49,640-sample clearance scope. r2 replaced solid blocks with bored sleeves and shortened rail segments; its expanded 70,664-sample scope still found four packed sleeve/neighbouring-post contacts. These failed studies are retained.

r3 increases packed bay spacing from 40 to 45 mm, uses 2 mm wall lower sleeves and smaller upper sleeves, and leaves clearance above the shortened lower post. Top rail segments are 320 mm long, leaving deliberate gaps appropriate to this porous barricade; continuous solid cover is not claimed. Packed envelope: approximately 0.500 x 0.485 x 0.370 m. Triangle count: 2,492.

Current scoped results: no intersections in 89,936 directed pair samples over PACKED, DEPLOYING, ACTIVE, OPERATING, DAMAGED and EXPIRING. Tested groups cover opposing scissor rod layers, rods versus guide housings, top rails versus scissor hardware, bored sleeves versus posts, and sleeves versus other sleeves. Self pairs are excluded; sleeve/sleeve reverse directions are counted separately. This is not every possible part pair or continuous-time collision proof.

All nine states pass rod endpoint-to-guide checks at 121 samples each, including half-key interpolation times. Maximum endpoint discrepancy is about 0.014 mm; rigid bar length remains 420 mm within the declared tolerance. Runtime checks verify absent normals/maps, nine unique-channel clips, texture assignment, motion/floor samples and host-clock destruction selection. Actual nine-state GPU upload/pose checks pass on the current exported binary.

Still unproven: top-rail bearing/support construction, drive transmission, whole mechanism load path, axle/bore construction beyond tested groups, distinct destruction and persistent wear, full clearance during thrown/impact/destruction, exact collider match, final faction/material quality, and live-world playthrough. No production certification or promotion.

Current exact hash and measurements are in field-razorframe-r3-clearance.json, field-razorframe-r3-linkage.json, field-razorframe-r3-packing.json and previews/field-razorframe-r3/razorframe/gpu/gpu-audit.json.
