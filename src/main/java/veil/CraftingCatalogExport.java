package veil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Generated single catalog: live recipes, industrial plans, facilities and
 * independently checked original art concepts. Never writes gameplay truth.
 */
public final class CraftingCatalogExport {
    public record Report(int field,int processes,int facilities,int illustrated,
                         int unillustrated,int conceptOnly,int totalRows) {}
    private CraftingCatalogExport(){}
    private static String csv(Object value){
        return "\"" + String.valueOf(value).replace("\"","\"\"") + "\"";
    }
    private static String field(String id){
        return id.startsWith("FACILITY:")?id.substring(9).replace('-',' '):id.replace('_',' ');
    }
    private static String requirements(Map<String,Integer> cost){
        StringJoiner list=new StringJoiner("; ");
        new TreeMap<>(cost).forEach((id,n)->list.add(id+" x"+n));
        return list.toString();
    }
    private static String dependency(String output){
        var dependents=CraftingRecipeIndex.uses(output);
        StringJoiner list=new StringJoiner("; ");
        for(var record:dependents)list.add(record.key());
        return list.toString();
    }
    private static String row(Object... cells){
        StringJoiner j=new StringJoiner(",");
        for(Object cell:cells)j.add(csv(cell));
        return j.toString();
    }
    public static Report write(Path root,Path dest)throws IOException{
        CraftingIconPacks art=CraftingIconPacks.load(root);
        StringBuilder out=new StringBuilder(32_768);
        out.append("entry_type,key,label,output,quantity,station,era,faction,seconds,power,ingredients,prerequisites,status,icon_pack,icon_entry,icon_sha256,used_for\n");
        int field=0,industrial=0,facilities=0,illustrated=0,unillustrated=0,concepts=0;
        for(var recipe:CraftingRecipeIndex.recipes()){
            switch(recipe.kind()){
                case FIELD->field++;
                case INDUSTRIAL->industrial++;
                case FACILITY->facilities++;
            }
            var entry=art.entries().get(recipe.output());
            if(entry==null)unillustrated++;else illustrated++;
            out.append(row(recipe.kind(),recipe.key(),recipe.name(),
                recipe.output(),recipe.quantity(),recipe.station(),recipe.era(),
                recipe.faction(),recipe.seconds(),recipe.power(),
                requirements(recipe.inputs()),String.join("; ",recipe.prerequisites()),
                recipe.availability(),entry==null?"ICON_REQUIRED":entry.group(),
                entry==null?"":entry.zipEntry(),entry==null?"":entry.sha256(),
                dependency(recipe.output()))).append('\n');
        }
        // Include the full 150-image asset catalog. A sprite is never
        // misrepresented as proof of a manufacturing process.
        for(var item:art.entries().values()){
            String status=CraftingRecipeIndex.hasRecipe(item.id())?"PRODUCED_BY_RECIPE":
                CraftingRecipeIndex.items().contains(item.id())?
                    "EXTERNAL_INPUT_OR_LOOT":"ICON_ONLY_CONCEPT";
            if(status.equals("ICON_ONLY_CONCEPT"))concepts++;
            out.append(row("ITEM","item:"+item.id(),item.label(),item.id(),0,
                "","","",0,0,"","",status,item.group(),item.zipEntry(),
                item.sha256(),dependency(item.id()))).append('\n');
        }
        int total=field+industrial+facilities+art.entries().size();
        Files.createDirectories(dest.toAbsolutePath().getParent());
        Files.writeString(dest,out.toString(),StandardCharsets.UTF_8);
        return new Report(field,industrial,facilities,illustrated,unillustrated,
            concepts,total);
    }
    public static void main(String[] args)throws Exception{
        Path root=Path.of(".");
        Path destination=Path.of(args.length>0?args[0]:
            "build/test-visuals/crafting-completion-catalog.csv");
        Report report=write(root,destination);
        if(report.field()!=SurvivalCrafting.recipes().size()||
            report.processes()!=CraftingEraRegistry.processes().size()||
            report.facilities()!=CraftingEraRegistry.facilities().size()||
            report.totalRows()!=report.field()+report.processes()+report.facilities()+150)
            throw new IllegalStateException("Catalog left out registered gameplay data");
        System.out.println("PASS CraftingCatalogExport "+report+
            " output="+destination.toAbsolutePath());
    }
}
