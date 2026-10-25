# Current field asset audit

Four refined pilots; 17 original candidates still require independent redesign. Zero production approvals.

The current exports contain no normal/tangent attributes, morph normals or normal maps. Offline form shading and AO share the existing 256 px color atlas. The runtime vertex layout remains nine floats and the shader adds no texture samples. Corrected atlas display encoding improves readability; this does not certify the art.

## sentry-mki

2,556 triangles. Current binary SHA-256: `ab034bdf786a03181b853833b68f16df635b7ecb1c439c9841bb9c5f67392277`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-pilots-r9/sentry-mki/gpu/gpu-audit.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and destruction integration; Live world gameplay and multiplayer acceptance; Packing width 0.511 m slightly exceeds the 0.5 m target.

## field-medic

2,876 triangles. Current binary SHA-256: `3d151b13b97c29a57c95b2fe689c08ccf0181f85de4a650f8c42cfe30632c48d`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-medic-r7/field-medic/gpu/gpu-audit.json`).
- attachments: PASS_SCOPED_SOCKET_CONTINUITY (`reports/field-medic-r7-attachments.json`).
- clearance: PASS_DECLARED_PAIRS (`reports/field-medic-r7-clearance.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and destruction integration; Live world gameplay and multiplayer acceptance.

## censer-of-dread

3,496 triangles. Current binary SHA-256: `9a699e282e7ba212cf417bb8c5b3febee6b60636e8230903096b81bbc9c813ff`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-pilots-r9/censer-of-dread/gpu/gpu-audit.json`).
- motion-inspection: NO_INTERSECTIONS_IN_SAMPLED_PAIRS (`reports/field-pilots-r9-motion-inspection.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and destruction integration; Live world gameplay and multiplayer acceptance; Packing depth 0.593 m exceeds the 0.5 m target.

## regeneration-heart

3,004 triangles. Current binary SHA-256: `017b62d5ab410640edb8005e8524c67b6905515e7a4b26b70756969c9b224b09`.

- gpu_upload_and_pose: PASS_SCOPED_GPU_UPLOAD_AND_POSE_RENDER (`previews/field-pilots-r9/regeneration-heart/gpu/gpu-audit.json`).
- clearance: PASS_DECLARED_PAIRS (`reports/field-pilots-r9-clearance.json`).

Remaining: Purpose/art acceptance and final surface refinement; Full articulation/packing clearance beyond declared pairs; Persistent damage, deterioration and destruction integration; Live world gameplay and multiplayer acceptance; Organic surface/color transitions and sparse dark atlas samples still need inspection.

Java validation on these exports: FieldPilotAnimationTest passed all four, including absence of normals/maps, nine unique-channel clips, texture assignment, 31 deployment floor samples and actual TRS/morph changes. ThrownActivationTest passed all 21 craft/throw and activation-boundary cases, including turret target updates between attacks. ThrownDeployablesTest, ArsenalIntegrationTest and ArenaSystemsTest passed. FIELD tracking packet compatibility and late-join behavior still need a dedicated regression.

The Medic hose stays within 0.34 mm of its sockets over 61 samples in each of nine states; the injector/case clearance contract passes 8,760 pair samples. Heart cradle contacts pass the declared 292 pair samples. Censer checks cover 31 samples of satellite bowls against declared vessels/slides. None of these checks establishes clearance for undeclared part pairs.
