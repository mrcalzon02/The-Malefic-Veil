# The Malefic Veil — Original UI art intake 01

This directory contains original image-generated, dark PSX-style candidate art, with no changes to the running launcher.

These are actual indexed-color PNG textures packaged by the GPU client's existing Maven resource inclusion. They are deliberately low-resolution compact previews, not the entire full-resolution source artwork pack.

- menu-icons-preview.png: 256x32, 8 adjacent 32x32 icons.
- folder-icons-preview.png: 256x32, 8 adjacent 32x32 icons.
- button-states-preview.png: 240x24, 3 adjacent 80x24 textless button states.

atlas.csv supplies the actual source rectangles. Do not bake menu labels into the buttons: overlay runtime text. These are candidate graphics, not approved item/recipe ID mappings or replacements for the existing licensed PSX UI.

Before wiring these icons into actual crafting/inventory screens, reconcile with the gameplay catalog and keep Unraveled, Penitent and Begotten economies distinct.
