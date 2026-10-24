# Construction test image gallery

471 original PNG renders and contact sheets. All four test views are included where generated: front, rear, closeup and neutral clay.

Private car/character reference renders and comparison panels containing them are excluded. Earlier studies remain visible; gallery inclusion does not imply approval.

Review labels: **ROUGH_STAGE_PASS** = current image and model hashes match a passing rough-stage review/gate; **REJECTED** = retained failed study; **ART_REVIEW_REQUIRED** = refinement pending art/runtime acceptance; **UNREVIEWED** = rendered but no matching pass; **OVERVIEW_OR_HISTORICAL_STUDY** = contact sheet, guide or older study. These assets still require final art and game integration review.

[Machine-readable manifest](manifest.json) Ã‚Â· [Filterable local gallery](index.html)

The HTML gallery runs locally after cloning: `python -m http.server 8000`, then open `http://localhost:8000/docs/asset-gallery/`.

Update and publish from the authoring workspace after each render batch: `python tools/build_asset_gallery.py --publish`. This preserves superseded renders, commits only gallery files and its verification workflow, and pushes the current branch. `--check` verifies source coverage and checksums; `--verify-archive` verifies a GitHub clone without ignored authoring sources. GitHub Actions runs the archive check on gallery changes.

## Field Medic repair study

25 images Ã‚Â· [Full image index](field-medic-r1.md)

[![Field Medic repair study](images/field-medic-r1/field-medic/clay.png)](field-medic-r1.md)

## Deployable pilot studies r1

75 images Ã‚Â· [Full image index](field-pilots-r1.md)

[![Deployable pilot studies r1](images/field-pilots-r1/censer-of-dread/clay.png)](field-pilots-r1.md)

## Deployable pilot studies r2

75 images Ã‚Â· [Full image index](field-pilots-r2.md)

[![Deployable pilot studies r2](images/field-pilots-r2/censer-of-dread/clay.png)](field-pilots-r2.md)

## Deployable pilot studies r3

75 images Ã‚Â· [Full image index](field-pilots-r3.md)

[![Deployable pilot studies r3](images/field-pilots-r3/censer-of-dread/clay.png)](field-pilots-r3.md)

## Deployable pilot studies r4

83 images Ã‚Â· [Full image index](field-pilots-r4.md)

[![Deployable pilot studies r4](images/field-pilots-r4/before-after.png)](field-pilots-r4.md)

## Censer packing study r5

25 images Ã‚Â· [Full image index](field-pilots-r5.md)

[![Censer packing study r5](images/field-pilots-r5/censer-of-dread/clay.png)](field-pilots-r5.md)

## Censer packing study r6

25 images Ã‚Â· [Full image index](field-pilots-r6.md)

[![Censer packing study r6](images/field-pilots-r6/censer-of-dread/clay.png)](field-pilots-r6.md)

## Thrown field equipment: candidate studies

88 images Ã‚Â· [Full image index](field-deployables.md)

[![Thrown field equipment: candidate studies](images/field-deployables/clay-roster.png)](field-deployables.md)

## Superseded renders

88 earlier test images retained. [Browse the history gallery](history.md).
