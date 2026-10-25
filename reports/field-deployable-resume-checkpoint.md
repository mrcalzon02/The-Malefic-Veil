# Active field development checkpoint

The earlier 50% pause ended when the user resumed work. The current goal remains active; the last five-hour allowance check showed 32% used / 68% remaining. Stop at 50% used, as requested.

Current candidates: Field Medic r7; Sentry, Censer and Regeneration Heart r9. All four use canonical coordinates, omit normals/tangents and normal maps, and contain offline form/AO shading in the existing 256 px color atlas. Actual GPU evidence uses the production upload path and shader. No production approval is claimed.

Medic floor, lid, nested support, injector stowage and hose continuity repairs pass their declared checks. Turret aim now follows authoritative targets between attacks, with a replicated tracking flag. All role effects and collision begin only after unfolding. Current evidence: reports/current-field-audit.md and .json. The authoritative 21-item index records four pilots and 17 redesigns outstanding.

Next: dedicated FIELD packet/late-join regression; resolve remaining material/organic surface defects and packing exceptions; continue independent deployable redesigns. Persistent damage/destruction and live-world acceptance remain outstanding. The broader product inventory is reports/product-model-audit-roadmap.md; 406 stored GLBs include variants and do not constitute 406 certified assets.

Workspace: build/field-deployables-main beneath the authoring checkout. Branch codex/field-deployables. Keep this checkout separate from arsenal-prototypes authoring work and private references.
