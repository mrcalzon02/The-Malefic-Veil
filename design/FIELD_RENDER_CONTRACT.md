# Field asset rendering cost contract

User direction: **no runtime normals**.

Keep the production vertex layout at nine floats: position (3), color (4), UV (2), plus the existing instance data. Do not add normal attributes, normal maps, runtime normal reconstruction or per-pixel normal lighting.

Form shading, material highlights and ambient occlusion are baked offline into the existing low-resolution color atlas. The renderer continues to use inexpensive ambient/day tint and distance-based light pools. This gives the assets readable form without increasing the GPU vertex format or adding texture samples.

Baked lighting is attached to the asset and does not provide dynamic directional shadows or physically correct changing specular reflections. GPU upload/pose checks remain separate from artistic acceptance. Normal data used internally by Blender for authoring/baking is not uploaded by the game renderer.

Current field exports also omit glTF NORMAL and TANGENT attributes, morph normals and normal textures. FieldPilotAnimationTest verifies their absence on the actual exported files. The offline atlas is encoded for the existing display-color texture path; linear bake values must not be stored directly as display RGB. This correction applies to these authored atlases and does not change the global game shader.
