# Current field asset audit

Four refined pilots; 17 original candidates still require independent redesign. Zero production approvals.

The current exports contain no normal/tangent attributes, morph normals or normal maps. Offline form shading and AO share the existing 256 px color atlas. The runtime vertex layout remains nine floats and the shader adds no texture samples. Corrected atlas display encoding improves readability; this does not certify the art.

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

Java validation on these exports: FieldPilotAnimationTest passed all four, including absence of normals/maps, nine unique-channel clips, texture assignment, 31 deployment floor samples and actual TRS/morph changes. ThrownActivationTest passed all 21 craft/throw and activation-boundary cases, including turret target updates between attacks. ThrownDeployablesTest, ArsenalIntegrationTest and ArenaSystemsTest passed. FieldReplicationTest passed real sockets for legacy/current snapshots, tracking/stock/clocks, an active late join, collapse snapshots, wire craft/throw, and two-peer collapse/removal. All 21 role tests cover immediate effect shutdown, 800 ms visual lifetime and one harmless burst/removal, including exhausted shields. The actual field asset loader selects DESTROYED from the host destruction age for all four staged fixtures.

The Medic hose stays within 0.34 mm of its sockets over 61 samples in each of nine states; the injector/case clearance contract passes 8,760 pair samples. Heart cradle contacts pass the declared 292 pair samples. Censer r13 expanded checks cover 31 deployment samples of satellites against main vessels, bands/ribs/lid ribs, slides, feet and one another, plus central vessels against slides/feet. The expanded audit found 214 intersections in the superseded r9 study; r10/r11 also failed their narrower tested scope. r13 passes this expanded scope, but its packed width/depth still exceed the target. None of these checks establishes clearance for undeclared part pairs.
