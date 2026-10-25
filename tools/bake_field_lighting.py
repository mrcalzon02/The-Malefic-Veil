"""Bake form lighting/AO into the existing color atlas. Zero runtime normal attributes/maps.
The production GPU vertex format remains position/color/UV (nine floats).
"""
import bpy

def bake(meshes,materials,label):
    atlas=next(node.image for node in materials[0].node_tree.nodes if node.type=='TEX_IMAGE' and node.image and node.image.name.endswith('_baked'))
    base=list(atlas.pixels[:]);w,h=atlas.size;scene=bpy.context.scene
    saved={obj:list(obj.data.materials) for obj in meshes};temporary=[];images=[]
    scene.render.engine='CYCLES';scene.cycles.samples=24
    scene.render.bake.use_clear=False;scene.render.bake.margin=2
    scene.render.bake.use_pass_direct=True;scene.render.bake.use_pass_indirect=True;scene.render.bake.use_pass_color=True
    scene.world=scene.world or bpy.data.worlds.new('offline_bake_world');scene.world.use_nodes=True
    scene.world.node_tree.nodes['Background'].inputs['Color'].default_value=(.75,.75,.75,1)
    scene.world.node_tree.nodes['Background'].inputs['Strength'].default_value=.35
    lights=[]
    for loc,power,size in [((-2,-4,5),650,4),((3,1,3),180,4)]:
        bpy.ops.object.light_add(type='AREA',location=loc);light=bpy.context.object;lights.append(light);light.data.energy=power;light.data.size=size
        from mathutils import Vector
        light.rotation_euler=(Vector((0,0,.5))-light.location).to_track_quat('-Z','Y').to_euler()
    def image(name):
        result=bpy.data.images.new(label+name,width=w,height=h);images.append(result);return result
    def target(mat,img):
        node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=img;mat.node_tree.nodes.active=node
    def select():
        bpy.ops.object.select_all(action='DESELECT')
        for obj in meshes:obj.select_set(True)
        bpy.context.view_layer.objects.active=meshes[0]
    shade=image('_offline_shade');ao=image('_offline_ao');emission=image('_offline_emission_mask')
    shade_materials={}
    for original in set(mat for mats in saved.values() for mat in mats):
        mat=bpy.data.materials.new('offline_white_'+original.name);mat.use_nodes=True;temporary.append(mat)
        shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Base Color'].default_value=(.8,.8,.8,1)
        source=original.node_tree.nodes.get('Principled BSDF') if original.use_nodes else None
        shader.inputs['Metallic'].default_value=source.inputs['Metallic'].default_value if source else 0
        shader.inputs['Roughness'].default_value=source.inputs['Roughness'].default_value if source else .8
        target(mat,shade);shade_materials[original]=mat
    for obj,mats in saved.items():
        obj.data.materials.clear()
        for mat in mats:obj.data.materials.append(shade_materials[mat])
    select();bpy.ops.object.bake(type='COMBINED')
    ao_mat=bpy.data.materials.new('offline_ambient_occlusion');ao_mat.use_nodes=True;temporary.append(ao_mat);nodes=ao_mat.node_tree.nodes;nodes.clear()
    output=nodes.new('ShaderNodeOutputMaterial');emit=nodes.new('ShaderNodeEmission');ambient=nodes.new('ShaderNodeAmbientOcclusion');ambient.samples=24;ambient.inputs['Distance'].default_value=.14
    ao_mat.node_tree.links.new(ambient.outputs['Color'],emit.inputs['Color']);ao_mat.node_tree.links.new(emit.outputs[0],output.inputs['Surface']);target(ao_mat,ao)
    for obj,mats in saved.items():
        obj.data.materials.clear()
        for _ in mats:obj.data.materials.append(ao_mat)
    select();bpy.ops.object.bake(type='EMIT')
    mask_mats=[]
    for on in (False,True):
        mat=bpy.data.materials.new('offline_emission_'+str(on));mat.use_nodes=True;temporary.append(mat);nodes=mat.node_tree.nodes;nodes.clear()
        output=nodes.new('ShaderNodeOutputMaterial');emit=nodes.new('ShaderNodeEmission');emit.inputs['Color'].default_value=((1,1,1,1) if on else (0,0,0,1));mat.node_tree.links.new(emit.outputs[0],output.inputs['Surface']);target(mat,emission);mask_mats.append(mat)
    for obj,mats in saved.items():
        obj.data.materials.clear()
        for mat in mats:obj.data.materials.append(mask_mats[int(mat==materials[4])])
    select();bpy.ops.object.bake(type='EMIT')
    shaded=list(shade.pixels[:]);occlusion=list(ao.pixels[:]);mask=list(emission.pixels[:])
    for pixel in range(w*h):
        offset=pixel*4;light=sum(shaded[offset:offset+3])/3
        factor=max(.30,min(1.70,.30+1.65*light))*(.55+.45*max(0,min(1,occlusion[offset])))
        if mask[offset]>.5:factor=1 # Instrument colors are preserved; no new runtime emission pass.
        for channel in range(3):
            linear=max(0,min(1,base[offset+channel]*factor))
            # The lightweight renderer samples ordinary display-encoded textures
            # without a framebuffer transfer function. Encode this offline atlas
            # here, rather than uploading linear bake values as dark display RGB.
            base[offset+channel]=12.92*linear if linear<=.0031308 else 1.055*linear**(1/2.4)-.055
    atlas.pixels=base;atlas.pack()
    for obj,mats in saved.items():
        obj.data.materials.clear()
        for mat in mats:obj.data.materials.append(mat)
    for mat in temporary:bpy.data.materials.remove(mat)
    for img in images:bpy.data.images.remove(img)
    for light in lights:bpy.data.objects.remove(light,do_unlink=True)
    scene['baked_form_lighting']=True
    print('OFFLINE_FORM_AO_BAKED',label,'runtime normal attributes=0',flush=True)
