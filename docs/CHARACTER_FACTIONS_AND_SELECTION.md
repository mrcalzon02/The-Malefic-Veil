# Character selection and faction implementation — The Malefic Veil

## Three factions

The world has been driven mad by an eldritch invasion. The living are not clean heroic archetypes: everyone has adapted to horrors beyond reason. Faction identity must be readable at PSX resolution and across varied LOD distances, distinct from combat class or character ownership.

1. **The Unraveled** — starving, hallucinating survivors clinging to fractured memories of ordinary life. Improvised armor, repurposed streetwear, scavenger medicine, weathered coats, desperate technology. The four currently shipped **project-authored** heroes (Warden, Medic, Shade, Occultist) provide functional stand-ins and remain separate mechanical classes.
2. **The Penitent** — deranged worshippers and opportunistic prophets who interpret the breach as divine revelation. Masked confessors, zealots, votaries and ritual executioners. Human but unmistakably ceremonial; a playable faction only after explicit authority, spawn, PVP readability and character model gates.
3. **The Begotten** — warped monsters and eldritch monstrosities, from almost-human infected to grotesque alien predators. These should have strongly contrasting silhouettes rather than mere human models tinted red. Monster candidates require appropriate original/custom monster geometry if the PSX archive's `Killer` category does not suit each archetype.

Faction is a persistent **character affiliation**; ability/class remains an independent validated gameplay choice. Appearance ID is a third independent choice. No faction or skin is allowed to secretly alter collision or combat statistics. If future modes allow a monster faction to have unique mechanics, define and test those rules explicitly on the authoritative host.

## Source archive accounting and binding policy

The audited `Characters_psx_1.2.zip` contains 138 rigged and 138 unrigged FBX models organized by source as 59 male, 58 female and 21 killer model families, with 136 PNG textures. Those category numbers are **not** a license to invent precise filename IDs or to assume every killer model is a monster. Until the archive's exact entries, thumbnails, rights and skeleton viability are reviewed, all source-dependent entries in `CharacterRoster` intentionally have null runtime mesh IDs.

Do not redistribute the raw archive in GitHub. Import selectively: record exact archive path, sha256, license evidence, visual contact-sheet inspection, candidate faction/role, rig and animation availability; then convert through an approved Blender FBX-to-glTF/GLB pipeline. Generate reduced triangle LOD0/LOD1/LOD2 representations (e.g., preserve silhouette at 100%/50%/20% source face budgets, validated visually, not blind face dropping), compact compatible textures, and a simple impostor for extreme distance. Verify source UV, animation skeleton/weights, pivot height, material transparency and left/right orientation. Preserve original unmodified sources outside public distributables. Compare rigging and static alternatives and choose the least expensive accurate display path.

The 4 existing runtime hero IDs are source-owned and verified present in `GameplayMeshes`. They are usable now without claiming the FBX archive is integrated. Pending names in the registry are **fictional casting targets**, not confirmed archive file titles.

## Selection interface contract

Compact faction columns/tabs with a scrollable filtered appearance list, rotating preview panel, name, identity description, locked/pending reason, and explicit confirm. No confirmation for a character whose model or authoritative roster ID is unavailable. Preserve independent dropdowns for mechanical class and owned character save slot; changing the visual appearance must not create a new persistent character or grant other faction abilities.

Server validates faction, appearance ID, ownership, unlocked state and class association; sends immutable stable faction + appearance identifiers in the roster handshake. Existing LAN protocol only exposes class ID today. Do **not** misrepresent a menu-only client choice as replicated faction identity. Add stable versioned roster manifests so mismatched clients fail with clear information instead of rendering another player's wrong model. Character selection precedes spawn selection and binds to the authenticated character/save, not to the current machine.

## Engineering gates

- **C1**: Registry has unique, source-traceable appearance IDs; fallback models explicitly identified; no unlicensed asset packaged. (First registry implemented.)
- **C2**: Enumerate actual archive entries, inspect thumbnails/contact sheet and license; nominate exact 4–8 suitable humans per human faction and review killer catalog for Begotten candidates.
- **C3**: Convert and render first three distinct candidates across the three factions; verify silhouette at 320×200 and with LOD.
- **C4**: Implement faction-aware selection screen with preview, scroll list, unavailable states and controller accessibility; no misleading selectable placeholders.
- **C5**: Versioned server-authoritative character selection, unique ownership, persistent affiliation, reconnection and independent class selection.
- **C6**: Animation, hide/reveal, occlusion, multi-client identity consistency and asset provenance release testing.

Treat characters, class and faction as three different fields. Reuse existing player-account and session authority specifications in `SERVER_WORLD_IDENTITY_ARCHITECTURE.md`.
