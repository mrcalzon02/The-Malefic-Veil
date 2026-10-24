"""Reusable padded UV atlas and baked albedo/ORM; geometry remains untouched."""
import bpy,math
from pathlib import Path

def bake(scene,objects,out,stencil_path):
    # Mechanical geometry is frozen. Author materials, unwrap and bake disjoint UV tiles.
    palette={0:(.070,.095,.115),1:(.28,.34,.37),3:(.020,.029,.033),4:(.39,.22,.075)}
    finishes={0:(.46,.65),1:(.32,.90),3:(.82,.0),4:(.42,.80)}
    stencil=bpy.data.images.load(str(stencil_path))
    opposite_path=Path(stencil_path).with_name(Path(stencil_path).stem+'-opposite.png')
    opposite=bpy.data.images.load(str(opposite_path)) if opposite_path.exists() else stencil
    for o in objects:
        band=int(o.get('trim_band',0));c=palette.get(band,palette[0]);m=bpy.data.materials.new(o.name+' finish source');m.use_nodes=True;n=m.node_tree.nodes;l=m.node_tree.links;bs=n.get('Principled BSDF')
        bs.inputs['Base Color'].default_value=(*c,1);bs.inputs['Metallic'].default_value=.65 if band in(0,1,4) else .02;bs.inputs['Roughness'].default_value=.82 if band==3 else .46
        coord=n.new('ShaderNodeTexCoord');noise=n.new('ShaderNodeTexNoise');noise.inputs['Scale'].default_value=140;noise.inputs['Detail'].default_value=2;l.new(coord.outputs['Generated'],noise.inputs['Vector'])
        ramp=n.new('ShaderNodeValToRGB');ramp.color_ramp.elements[0].color=(*[v*.88 for v in c],1);ramp.color_ramp.elements[1].color=(*[min(1,v*1.10) for v in c],1);l.new(noise.outputs['Fac'],ramp.inputs[0]);l.new(ramp.outputs['Color'],bs.inputs['Base Color'])
        if 'receiver_service_panel' in o.name:
            sep=n.new('ShaderNodeSeparateXYZ');uv=n.new('ShaderNodeCombineXYZ');l.new(coord.outputs['Generated'],sep.inputs[0]);l.new(sep.outputs['X'],uv.inputs['X']);l.new(sep.outputs['Z'],uv.inputs['Y'])
            if o.name.endswith('panel_1'):
                flip=n.new('ShaderNodeMath');flip.operation='SUBTRACT';flip.inputs[0].default_value=1;l.new(sep.outputs['X'],flip.inputs[1]);l.new(flip.outputs[0],uv.inputs['X'])
            t=n.new('ShaderNodeTexImage');t.image=opposite if o.name.endswith('panel_1') else stencil;t.extension='CLIP';l.new(uv.outputs[0],t.inputs['Vector']);mix=n.new('ShaderNodeMixRGB');l.new(t.outputs['Alpha'],mix.inputs[0]);l.new(ramp.outputs['Color'],mix.inputs[1]);l.new(t.outputs['Color'],mix.inputs[2]);l.new(mix.outputs[0],bs.inputs['Base Color'])
        if band==3:
            wave=n.new('ShaderNodeTexWave');wave.wave_type='BANDS';wave.bands_direction='DIAGONAL';wave.inputs['Scale'].default_value=60;l.new(coord.outputs['Generated'],wave.inputs['Vector']);mix=n.new('ShaderNodeMixRGB');mix.inputs[1].default_value=(*[v*.72 for v in c],1);mix.inputs[2].default_value=(*[v*1.3 for v in c],1);l.new(wave.outputs['Fac'],mix.inputs[0]);l.new(mix.outputs[0],bs.inputs['Base Color'])
        o.data.materials.clear();o.data.materials.append(m)
        for f in o.data.polygons:f.material_index=0
    for o in objects:
        bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o;bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT');bpy.ops.uv.smart_project(angle_limit=1.1,island_margin=.025);bpy.ops.object.mode_set(mode='OBJECT')
    # Every physical part has its own padded atlas cell, including hidden magazine surfaces.
    grid=math.ceil(math.sqrt(len(objects)));size=1024;pad=3/size
    for i,o in enumerate(objects):
        x=i%grid/grid;y=i//grid/grid;cell=[x+pad,y+pad,x+1/grid-pad,y+1/grid-pad];o['atlas_uv_cell']=cell
        for u in o.data.uv_layers.active.data:u.uv=(cell[0]+u.uv.x*(cell[2]-cell[0]),cell[1]+u.uv.y*(cell[3]-cell[1]))
    atlas=bpy.data.images.new('Weapon baked albedo',size,size);scene.render.bake.use_pass_direct=False;scene.render.bake.use_pass_indirect=False;scene.render.bake.use_pass_color=True;scene.render.bake.margin=2
    for o in objects:
        n=o.data.materials[0].node_tree.nodes.new('ShaderNodeTexImage');n.image=atlas;o.data.materials[0].node_tree.nodes.active=n
    bpy.ops.object.select_all(action='DESELECT')
    for o in objects:o.select_set(True)
    bpy.context.view_layer.objects.active=objects[0]
    bpy.ops.object.bake(type='DIFFUSE');atlas.filepath_raw=str(out/'albedo.png');atlas.file_format='PNG';atlas.save();atlas.pack()
    # A linear ORM image preserves rubber versus coated receiver versus bare steel.
    orm=bpy.data.images.new('Weapon baked ORM',size,size);orm.colorspace_settings.name='Non-Color'
    for o in objects:
        mat=o.data.materials[0];n=mat.node_tree.nodes;l=mat.node_tree.links;rough,metal=finishes.get(int(o.get('trim_band',0)),finishes[0]);em=n.new('ShaderNodeEmission');em.inputs['Color'].default_value=(1,rough,metal,1);l.new(em.outputs[0],n.get('Material Output').inputs['Surface']);t=n.new('ShaderNodeTexImage');t.image=orm;n.active=t
    bpy.ops.object.bake(type='EMIT');orm.filepath_raw=str(out/'orm.png');orm.file_format='PNG';orm.save();orm.pack()
    shared=bpy.data.materials.new('Weapon shared baked PBR material');shared.use_nodes=True;n=shared.node_tree.nodes;l=shared.node_tree.links;t=n.new('ShaderNodeTexImage');t.image=atlas;bs=n.get('Principled BSDF');l.new(t.outputs['Color'],bs.inputs['Base Color']);pbr=n.new('ShaderNodeTexImage');pbr.image=orm;split=n.new('ShaderNodeSeparateColor');l.new(pbr.outputs['Color'],split.inputs[0]);l.new(split.outputs['Green'],bs.inputs['Roughness']);l.new(split.outputs['Blue'],bs.inputs['Metallic'])
    for o in objects:o.data.materials.clear();o.data.materials.append(shared)
