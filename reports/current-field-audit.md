# Current field asset audit

Six refined pilots; 15 original candidates still require independent redesign. Zero production approvals.

The current exports contain no normal/tangent attributes, morph normals or normal maps. Offline form shading and AO share the existing 256 px color atlas. The runtime vertex layout remains nine floats and the shader adds no texture samples. Corrected atlas display encoding improves readability; this does not certify the art.

## bastion-panel

3,468 triangles. Current binary SHA-256: `05821fe6925aca29a49cbfe07d2b08e0d4071d389350927ed9d5191b454f466c`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-bastion-r5/bastion-panel/gpu/gpu-audit.json`).
- clearance: PASS_DECLARED_PAIRS (`reports/field-bastion-r5-clearance.json`).
- packing: MEASURED_ENVELOPE (`reports/field-bastion-r5-packing.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and convincing failure-specific destruction poses; Live world gameplay and multiplayer acceptance beyond socket regression; Lateral rail nesting, locks and load path remain unproven.

## razorframe

2,492 triangles. Current binary SHA-256: `4d77001e8b1d496ba1c4277e6963a738fc46f128de3fb6bb79cca5791fd44377`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-razorframe-r3/razorframe/gpu/gpu-audit.json`).
- clearance: PASS_DECLARED_PAIRS (`reports/field-razorframe-r3-clearance.json`).
- packing: MEASURED_ENVELOPE (`reports/field-razorframe-r3-packing.json`).
- linkage: PASS_SCOPED_RIGID_LINKAGE (`reports/field-razorframe-r3-linkage.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and convincing failure-specific destruction poses; Live world gameplay and multiplayer acceptance beyond socket regression; Full part clearance, top-rail support and drive transmission remain unproven.

## sentry-mki

2,556 triangles. Current binary SHA-256: `41926018b56b7e609f9f6b60ba6f1c047763f52e185e45aa3d8c68c83aba6be3`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-pilots-r10/sentry-mki/gpu/gpu-audit.json`).
- packing: MEASURED_ENVELOPE (`reports/field-pilots-r10-packing.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and convincing failure-specific destruction poses; Live world gameplay and multiplayer acceptance beyond socket regression.

## field-medic

2,876 triangles. Current binary SHA-256: `3d151b13b97c29a57c95b2fe689c08ccf0181f85de4a650f8c42cfe30632c48d`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-medic-r7/field-medic/gpu/gpu-audit.json`).
- attachments: PASS_SCOPED_SOCKET_CONTINUITY (`reports/field-medic-r7-attachments.json`).
- clearance: PASS_DECLARED_PAIRS (`reports/field-medic-r7-clearance.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and convincing failure-specific destruction poses; Live world gameplay and multiplayer acceptance beyond socket regression.

## censer-of-dread

3,496 triangles. Current binary SHA-256: `192e57a340887f8927365cd1a12d2fd229520cbbc99bb5568314cacd457f3219`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-pilots-r13/censer-of-dread/gpu/gpu-audit.json`).
- motion-inspection: NO_INTERSECTIONS_IN_SAMPLED_PAIRS (`reports/field-pilots-r13-motion-inspection.json`).
- packing: MEASURED_ENVELOPE (`reports/field-pilots-r13-packing.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and convincing failure-specific destruction poses; Live world gameplay and multiplayer acceptance beyond socket regression; Packing approximately 0.557 x 0.557 x 0.499 m still exceeds the 0.5 m target.

## regeneration-heart

3,004 triangles. Current binary SHA-256: `017b62d5ab410640edb8005e8524c67b6905515e7a4b26b70756969c9b224b09`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-pilots-r9/regeneration-heart/gpu/gpu-audit.json`).
- clearance: PASS_DECLARED_PAIRS (`reports/field-pilots-r9-clearance.json`).
- packing: MEASURED_ENVELOPE (`reports/field-pilots-r9-packing.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and convincing failure-specific destruction poses; Live world gameplay and multiplayer acceptance beyond socket regression; Organic surface/color transitions and sparse dark atlas samples still need inspection.

Java validation on these exports: FieldPilotAnimationTest passed the four earlier pilots and both new studies, including absence of normals/maps, nine unique-channel clips, texture assignment, 31 deployment floor samples and actual TRS/morph changes. ThrownActivationTest passed all 21 craft/throw and activation-boundary cases, including turret target updates between attacks. ThrownDeployablesTest, ArsenalIntegrationTest and ArenaSystemsTest passed. FieldReplicationTest passed real sockets for legacy/current snapshots, tracking/stock/clocks, an active late join, collapse snapshots, wire craft/throw, and two-peer collapse/removal. All 21 role tests cover immediate effect shutdown, 800 ms visual lifetime and one harmless burst/removal, including exhausted shields. The actual field asset loader selects DESTROYED from the host destruction age for the staged fixtures.

The Medic hose stays within 0.34 mm of its sockets over 61 samples in each of nine states; the injector/case clearance contract passes 8,760 pair samples. Heart cradle contacts pass the declared 292 pair samples. Censer r13 expanded checks cover 31 deployment samples of satellites against main vessels, bands/ribs/lid ribs, slides, feet and one another, plus central vessels against slides/feet. The expanded audit found 214 intersections in the superseded r9 study; r10/r11 also failed their narrower tested scope. r13 passes this expanded scope, but its packed width/depth still exceed the target. None of these checks establishes clearance for undeclared part pairs.

## Latest work

Bastion r5: 3,468 triangles; packed approximately 0.460 x 0.500 x 0.495 m; no armor tile intersections in 31,540 declared pair samples. Its operating pressure piston reacts to authoritative damage events; destruction now uses column sag/lean instead of the packed transform. Lateral slide nesting/load paths and other part pairs remain unproven.

Razorframe r3: 2,492 triangles; five rigid scissor bays, bored guide sleeves and nested vertical posts. The exact exported rod endpoints remain within 0.014 mm of declared guide centers across 121 samples per state in all nine states. Packed envelope approximately 0.500 x 0.485 x 0.370 m meets the target. The expanded six-state clearance check passes 89,936 directed pair samples across opposing rod layers, rod/guide housings, header/hardware, sleeve/post and sleeve/sleeve pairs. r1 failed 2,920 rod/solid-guide pair samples; r2 retained four packed sleeve/neighbouring-post contacts, repaired with wider packed post spacing in r3. Full clearance, top-rail supports/nesting, drive transmission, failure motion and art acceptance remain pending.

The GPU audit camera now frames the full active bounds and keeps that camera fixed across states. This fixes clipped wall evidence without hiding packing differences. Both current studies pass nine-state GPU checks. No runtime normal attributes/maps were added.
