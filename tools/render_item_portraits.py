#!/usr/bin/env python3
"""Bake transparent, angled thumbnails from the ACTUAL source GLBs using Blender.

Invoke: blender --background --factory-startup --python tools/render_item_portraits.py
Never invent an image or substitute a name-matched placeholder for missing geometry.
Outputs are deterministic build artifacts, not extra recipes or gameplay items.
"""
import csv
import hashlib
import math
from pathlib import Path

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/generated/item-portraits/portraits"
WEAPONS = ROOT / "assets/game/arsenal"
AIRCRAFT = ROOT / "assets/game/aircraft"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def bounds_of(objects):
    corners = [obj.matrix_world @ Vector(corner)
               for obj in objects for corner in obj.bound_box]
    if not corners:
        raise ValueError("Imported GLB contains no mesh geometry")
    low = Vector(tuple(min(v[axis] for v in corners) for axis in range(3)))
    high = Vector(tuple(max(v[axis] for v in corners) for axis in range(3)))
    if (high - low).length < 1e-7:
        raise ValueError("Zero-sized geometry")
    return (low + high) * 0.5, high - low


def light(name, location, energy, extent):
    data = bpy.data.lights.new(name, type="AREA")
    data.energy = energy
    data.shape = "DISK"
    data.size = extent
    obj = bpy.data.objects.new(name, data)
    bpy.context.collection.objects.link(obj)
    obj.location = location
    obj.rotation_euler = (-obj.location).to_track_quat("-Z", "Y").to_euler()
    return obj


def portrait(source, destination):
    reset()
    bpy.ops.import_scene.gltf(filepath=str(source))
    bpy.context.view_layer.update()
    objects = [o for o in bpy.context.scene.objects
               if o.type == "MESH" and not o.hide_render]
    if not objects or not any(len(o.data.polygons) for o in objects):
        raise ValueError("No drawable mesh triangles: " + str(source))
    center, extent = bounds_of(objects)
    span = max(extent.x, extent.y, extent.z, 0.01)
    direction = Vector((0.9, -1.4, 0.95)).normalized()

    camera_data = bpy.data.cameras.new("Inventory portrait orthographic")
    camera = bpy.data.objects.new("Inventory portrait camera", camera_data)
    bpy.context.collection.objects.link(camera)
    camera.location = center + direction * (span * 3)
    camera.rotation_euler = (center - camera.location).to_track_quat("-Z", "Y").to_euler()
    camera_data.type = "ORTHO"
    camera_data.ortho_scale = span * 1.72
    bpy.context.scene.camera = camera

    light("Key", center + Vector((-span * 1.4, -span * 1.9, span * 2.9)),
          460, span * 2)
    light("Fill", center + Vector((span * 2, span * 0.6, span * 1.5)),
          320, span * 2.5)
    world = bpy.context.scene.world
    world.use_nodes = True
    world.node_tree.nodes.get("Background").inputs["Color"].default_value = (0.24, 0.27, 0.33, 1)
    world.node_tree.nodes.get("Background").inputs["Strength"].default_value = 0.85

    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = 12
    scene.render.resolution_x = 256
    scene.render.resolution_y = 256
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    scene.render.filepath = str(destination)
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "Medium High Contrast"
    scene.render.film_transparent = True
    destination.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.render.render(write_still=True)
    if not destination.is_file() or destination.stat().st_size < 1000:
        raise RuntimeError("Portrait render failed: " + str(source))
    # Catch fully transparent images, including silently unrenderable imports.
    image = bpy.data.images.load(str(destination), check_existing=False)
    try:
        if image.size[0] != 256 or image.size[1] != 256:
            raise RuntimeError("Unexpected portrait resolution: " + str(destination))
        alphas = image.pixels[3::4]
        if max(alphas) < 0.2:
            raise RuntimeError("Blank portrait: " + str(source))
    finally:
        bpy.data.images.remove(image)


def main():
    if not WEAPONS.is_dir() or not AIRCRAFT.is_dir():
        raise RuntimeError("The checked-in GLB source directories are required")
    weapons = sorted(WEAPONS.glob("w[0-9][0-9]-*.glb"))
    aircraft = sorted(AIRCRAFT.glob("*.glb"))
    if len(weapons) != 32 or len(aircraft) != 9:
        raise RuntimeError("Expected 32 real arsenal GLBs and 9 real aircraft GLBs; got "
                           + str((len(weapons), len(aircraft))))
    records = []
    for category, sources in (("weapons", weapons), ("aircraft", aircraft)):
        for source in sources:
            destination = OUT / category / (source.stem + ".png")
            portrait(source, destination)
            records.append((category, source.stem,
                            str(source.relative_to(ROOT)), sha256(source),
                            str(destination.relative_to(OUT.parent)), sha256(destination)))
            print("PORTRAIT_FROM_SOURCE", category, source.stem, flush=True)
    OUT.mkdir(parents=True, exist_ok=True)
    with (OUT / "index.csv").open("w", newline="", encoding="utf-8") as output:
        writer = csv.writer(output)
        writer.writerow(("category", "id", "source", "source_sha256", "render", "render_sha256"))
        writer.writerows(records)
    print("PASS REAL_3D_ITEM_PORTRAITS total=" + str(len(records)), flush=True)


if __name__ == "__main__":
    main()
