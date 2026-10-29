# The Malefic Veil — Desktop & File Icons (Pack 02)

User-approved, original PSX-style gothic-industrial icon artwork. Source: the generated October 28, 2026 desktop/file-management icon sheet.

**Production resources**

- `malefic-veil-64.png`: cropped red-sigil application identity. The GPU desktop window uses this classpath image. `tools/make_windows_icon.py` creates a valid Windows ICO from the **same shipped JAR resource** for the native desktop shortcut and setup package.
- `file-ui.png`: 288×96, 8-color indexed original icon atlas, 24×24 cells; 12 folders, 12 file types, 10 actions, 8 status markers. The live `PsxHorrorMenu` renders cached `TextureRegion` cells; `GpuFrontEnd` puts them on existing clickable world/save controls.
- `file-ui.csv`: exact source rectangles and semantic names.

The full artwork source pack (48 individual high-resolution icons, six alternate launcher designs, PNGs sized for desktop use, multi-resolution native `.ico`, original generated sheet) was delivered separately for authoring; this repository commit contains compact **runtime-optimized originals** rather than the source archive. Menu text is always drawn at runtime.

Do **not** register Windows file-type extensions until the game has a published portable save/project interchange format. The desktop application/shortcut icon **is** wired through the real Windows build; extension-based associations are not yet implemented.
