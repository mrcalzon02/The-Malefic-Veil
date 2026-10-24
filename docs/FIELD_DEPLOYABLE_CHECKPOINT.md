# Thrown field deployables — work-in-progress checkpoint

**No production-complete assets.** This branch preserves the existing work for review and continuation. It does not promote candidates into the game-approved asset folder.

- [21-item asset index](../reports/field-deployable-asset-index.md): 3 refined pilots, 1 additional candidate requiring repair, 17 awaiting independent redesign.
- [Individual review of the rejected original roster](../reports/field-deployables-purpose-review.md).
- [Pilot refinement review](../reports/field-pilot-refinement-review.md): describes the r4 studies; the newer Censer r6 supersedes its Censer budget figures.
- [Resume checkpoint](../reports/field-deployable-resume-checkpoint.md).
- [Current render gallery and retained history](asset-gallery/README.md).
- [Binary/preview upload manifest and SHA-256 hashes](../reports/field-deployable-upload-manifest.json).

## Latest studies

| Item | GLB | Editable project | Animated project | Current status |
|---|---|---|---|---|
| Sentry Mk.I | [r4 GLB](../build/candidates/field-pilots-r4/sentry-mki/sentry-mki.glb) | [Model](../build/candidates/field-pilots-r4/sentry-mki/sentry-mki.blend) | [Animations](../build/candidates/field-pilots-r4/sentry-mki/sentry-mki-animated.blend) | 2,556 triangles; further art, packing and runtime review |
| Censer of Dread | [r6 GLB](../build/candidates/field-pilots-r6/censer-of-dread/censer-of-dread.glb) | [Model](../build/candidates/field-pilots-r6/censer-of-dread/censer-of-dread.blend) | [Animations](../build/candidates/field-pilots-r6/censer-of-dread/censer-of-dread-animated.blend) | 3,496 triangles; selected sampled vessel/frame pairs clear; packed depth about 0.593 m still needs work |
| Regeneration Heart | [r4 GLB](../build/candidates/field-pilots-r4/regeneration-heart/regeneration-heart.glb) | [Model](../build/candidates/field-pilots-r4/regeneration-heart/regeneration-heart.blend) | [Animations](../build/candidates/field-pilots-r4/regeneration-heart/regeneration-heart-animated.blend) | 3,004 triangles; connected closed tissue and morph targets; further art/runtime review |
| Field Medic | [r1 GLB](../build/candidates/field-medic-r1/field-medic/field-medic.glb) | [Model](../build/candidates/field-medic-r1/field-medic/field-medic.blend) | Script-authored tracks in GLB | 2,326 triangles; **rejected pending repairs:** deployment penetrates ground by about 0.0225 m; lid/panel alignment needs repair |

Other files under `build/candidates/field-deployables/` are the rejected original 21-model roster. Earlier `field-pilots-r1` through `r5` folders are retained studies, not approved alternatives. Candidate GLBs, Blender projects and evidence JSONs are deliberately tracked for this checkpoint despite the general `build/` ignore rule. Local logs, caches, compiler output and Blender backup files are excluded.

## Code and reproducibility

The checkpoint includes the proposed GLB TRS/morph animation evaluator, host animation-state snapshots, client pose handling and GPU pose-buffer changes. These are work in progress. The GPU build/live playthrough remains unverified; named clips and passing tests are not proof of production integration. Progressive damage, depletion, collapse and final field effects remain unfinished.

Use Blender 5.0 and Java 17. Relevant tools:

- `tools/build_field_deployables.py`: rejected original roster generator, retained for reproducibility.
- `tools/refine_field_pilots.py`: independently authored Sentry, Censer and Heart studies; `--revision field-pilots-r6 --only censer-of-dread` generates the latest Censer.
- `tools/refine_field_medic.py`: Field Medic repair candidate.
- `tools/render_field_pilots.py -- --revision <candidate-folder>`: reimports GLBs and renders actual exported tracks.
- `tools/inspect_structure_quality.py` and `tools/inspect_field_motion.py`: geometry and scoped motion inspection.
- `tools/save_field_pilot_sources.py -- --revision <candidate-folder>`: editable animation/NLA projects from the exact exported GLBs.

Java validation rerun before this checkpoint: all engine sources compile; `ThrownDeployablesTest`, `FieldAnimationTest`, the r4 `FieldPilotAnimationTest`, and the r6 Censer-specific animation test pass. The new Field Medic's ground-clearance test intentionally remains a recorded failure. No thresholds were changed to pass it.

Private car/character calibration sources and comparison panels containing them are not included. Their identifiers and hashes in inspection reports are calibration evidence only.
