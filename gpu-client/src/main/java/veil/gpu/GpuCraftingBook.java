package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import veil.CraftingRecipeIndex;
import veil.CraftingRecipeIndex.Record;
import veil.MaleficVeil;
import java.util.*;

/**
 * Source-illustrated item encyclopedia + executable/planned recipe dependency
 * index. All formulas and reverse uses are read from the authoritative
 * registries; visually catalogued concepts cannot be crafted by clicking.
 */
public final class GpuCraftingBook implements AutoCloseable {
    private enum View { RECIPES, ITEMS }
    private static final int PER_PAGE=8;
    private final PsxHorrorMenu skin;
    private final GpuCraftingIcons icons;
    private View view=View.RECIPES;
    private int choice=0,offset=0,filter=0,ingredientOffset=0;
    private String search="";
    private static final String[] FILTERS={"ALL","FIELD","INDUSTRIAL","FACILITIES"};
    private record Line(String title,String output,Record recipe){}
    public GpuCraftingBook(PsxHorrorMenu skin){
        this.skin=Objects.requireNonNull(skin);
        this.icons=new GpuCraftingIcons();
    }
    public void reset(){
        view=View.RECIPES;choice=0;offset=0;filter=0;ingredientOffset=0;search="";
    }
    public boolean type(char c){
        if(c=='\b'||c==127){
            if(!search.isEmpty())search=search.substring(0,search.length()-1);
        }else if(c>=32&&c<=126&&search.length()<28){
            search+=c;
        }else return false;
        choice=0;offset=0;ingredientOffset=0;
        return true;
    }
    /** Real selection follows original recipe keys, never menu row guesses. */
    void inspectItem(String itemId){
        view=View.ITEMS;filter=0;search="";choice=0;offset=0;
        List<Line> known=entries();
        for(int i=0;i<known.size();i++)
            if(known.get(i).output().equals(itemId)){setChoice(i);return;}
        throw new IllegalArgumentException("Unknown indexed item "+itemId);
    }
    void inspectRecipe(String recipeKey){
        view=View.RECIPES;filter=0;search="";offset=0;
        var selected=entries();
        for(int i=0;i<selected.size();i++){
            Record r=selected.get(i).recipe();
            if(r!=null&&r.key().equals(recipeKey)){setChoice(i);return;}
        }
        throw new IllegalArgumentException("Unknown recipe selection "+recipeKey);
    }
    private void followSelected(){
        List<Line> rows=entries();
        if(rows.isEmpty())return;
        Line chosen=rows.get(choice);
        if(chosen.recipe()!=null){
            String target=chosen.output();
            view=View.ITEMS;filter=0;search="";offset=0;
            List<Line> products=entries();
            for(int i=0;i<products.size();i++)
                if(products.get(i).output().equals(target)){setChoice(i);return;}
        }else{
            var producers=CraftingRecipeIndex.makes(chosen.output());
            if(!producers.isEmpty())inspectRecipe(producers.get(0).key());
        }
    }
    public void drawIcon(SpriteBatch batch,String item,float x,float y,float size){
        icons.draw(batch,item,x,y,size);
    }
    private static String clipped(String s,int limit){
        return s.length()<=limit?s:s.substring(0,Math.max(0,limit-1))+"~";
    }
    private List<Line> entries(){
        List<Line> list=new ArrayList<>();
        if(view==View.ITEMS){
            Set<String> ids=new TreeSet<>(icons.ids());
            ids.addAll(CraftingRecipeIndex.items());
            for(String id:ids){
                if(filter==1&&CraftingRecipeIndex.makes(id).isEmpty())continue;
                if(filter==2&&CraftingRecipeIndex.makes(id).stream().noneMatch(
                    r->r.kind()==CraftingRecipeIndex.Kind.INDUSTRIAL))continue;
                if(filter==3&&!CraftingRecipeIndex.makes(id).isEmpty())continue;
                list.add(new Line(icons.label(id),id,null));
            }
        }else for(Record recipe:CraftingRecipeIndex.recipes()){
            if(filter==1&&recipe.kind()!=CraftingRecipeIndex.Kind.FIELD)continue;
            if(filter==2&&recipe.kind()!=CraftingRecipeIndex.Kind.INDUSTRIAL)continue;
            if(filter==3&&recipe.kind()!=CraftingRecipeIndex.Kind.FACILITY)continue;
            list.add(new Line(recipe.name(),recipe.output(),recipe));
        }
        if(!search.isBlank()){
            String term=search.toLowerCase(Locale.ROOT);
            list.removeIf(r->!(r.title()+" "+r.output()+" "+
                (r.recipe()==null?"":r.recipe().station())).toLowerCase(Locale.ROOT)
                .contains(term));
        }
        return list;
    }
    private void setChoice(int n){
        int size=entries().size();
        choice=Math.max(0,Math.min(size-1,n));
        if(choice<offset)offset=choice;
        if(choice>=offset+PER_PAGE)offset=choice-PER_PAGE+1;
        ingredientOffset=0;
    }
    public boolean input(){
        boolean touched=false;
        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
            view=view==View.RECIPES?View.ITEMS:View.RECIPES;
            choice=0;offset=0;filter=0;touched=true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.F4)){
            filter=(filter+1)%FILTERS.length;choice=0;offset=0;
            ingredientOffset=0;touched=true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            followSelected();ingredientOffset=0;touched=true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)){
            ingredientOffset=Math.max(0,ingredientOffset-4);touched=true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)){
            ingredientOffset+=4;touched=true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.UP)){setChoice(choice-1);touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.DOWN)){setChoice(choice+1);touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)){setChoice(choice-PER_PAGE);touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)){setChoice(choice+PER_PAGE);touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT)){setChoice(choice-1);touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)){setChoice(choice+1);touched=true;}
        if(Gdx.input.justTouched()){
            float x=Gdx.input.getX()*640f/Gdx.graphics.getWidth();
            float y=400f-Gdx.input.getY()*400f/Gdx.graphics.getHeight();
            if(x>=26&&x<=270){
                for(int i=0;i<PER_PAGE;i++){
                    float bottom=285-i*31;
                    if(y>=bottom&&y<bottom+29){
                        setChoice(offset+i);touched=true;break;
                    }
                }
            }
        }
        return touched;
    }
    private void line(SpriteBatch batch,String content,float x,float y){
        skin.font().draw(batch,clipped(content,46),x,y);
    }
    private String status(Record record){
        return record==null?"NO MANUFACTURING RECIPE":CraftingRecipeIndex.availabilityText(record);
    }
    private void detailRecipe(SpriteBatch batch,Record record){
        line(batch,"RECIPE  "+clipped(record.name(),29),314,309);
        String icon=record.output().startsWith("FACILITY:")?null:record.output();
        if(icon!=null&&icons.contains(icon))icons.draw(batch,icon,315,253,43);
        else{
            skin.slot(batch,315,253,43,false);
            line(batch,record.kind()==CraftingRecipeIndex.Kind.FACILITY?"SITE":"NO ART",319,279);
        }
        line(batch,"YIELD "+record.quantity()+"  "+clipped(record.output(),25),365,287);
        line(batch,status(record),365,267);
        line(batch,"AT: "+record.station().replace('-',' ').toUpperCase(Locale.ROOT),314,239);
        line(batch,"ERA "+record.era()+"  "+record.faction(),314,223);
        if(record.seconds()>0)
            line(batch,"TIME "+record.seconds()+"S   POWER "+record.power(),314,207);
        line(batch,"INGREDIENTS  [ / ] PAGE",314,191);
        int row=0,seen=0;
        int start=Math.min(ingredientOffset,
            Math.max(0,((record.inputs().size()-1)/4)*4));
        for(var ingredient:record.inputs().entrySet()){
            if(seen++<start)continue;
            if(row==4)break;
            float y=161-row*24;
            skin.slot(batch,315,y,23,false);
            icons.draw(batch,ingredient.getKey(),316,y+1,21);
            line(batch,ingredient.getValue()+"x "+clipped(icons.label(ingredient.getKey()),27),
                344,y+16);
            row++;
        }
        if(record.inputs().size()>start+row)
            line(batch,"+"+(record.inputs().size()-start-row)+" MORE  ] NEXT",314,70);
        else if(start>0)
            line(batch,"[ PREVIOUS COMPONENTS",314,70);
        String related=CraftingRecipeIndex.usesText(record.output(),2);
        line(batch,"USED FOR: "+clipped(related,34),314,53);
        if(!record.prerequisites().isEmpty())
            line(batch,"NEEDS: "+clipped(String.join(", ",record.prerequisites()),34),314,40);
    }
    private void detailItem(SpriteBatch batch,String item){
        line(batch,"ITEM  "+clipped(icons.label(item),32),314,309);
        skin.slot(batch,315,248,52,false);
        icons.draw(batch,item,315,248,52);
        if(!icons.contains(item))line(batch,"UNILLUSTRATED SOURCE ITEM",374,280);
        String status=CraftingRecipeIndex.hasRecipe(item)?"PRODUCTION REGISTERED":
            (CraftingRecipeIndex.items().contains(item)?"HARVEST/LOOT OR INPUT":"ICON-ONLY CONCEPT");
        line(batch,status,314,236);
        List<Record> makers=CraftingRecipeIndex.makes(item);
        line(batch,"MADE AT:",314,218);
        int row=0;
        for(Record r:makers){
            if(row>=3)break;
            line(batch,clipped(r.station()+" ["+r.availability()+"]",42),314,200-row*16);
            row++;
        }
        if(makers.isEmpty())line(batch,"NO VERIFIED PRODUCTION FORMULA",314,201);
        if(makers.size()>3)line(batch,"+"+(makers.size()-3)+" OTHER FORMULAS",314,150);
        line(batch,"REQUIRED BY:",314,135);
        row=0;
        for(Record r:CraftingRecipeIndex.uses(item)){
            if(row==4)break;
            line(batch,clipped(r.name()+" / "+r.station(),43),314,116-row*18);
            row++;
        }
        if(row==0)line(batch,"NO REGISTERED DOWNSTREAM USE",314,116);
        int other=CraftingRecipeIndex.uses(item).size()-row;
        if(other>0)line(batch,"+"+other+" MORE USES",314,35);
    }
    public void draw(SpriteBatch batch,MaleficVeil.GpuFrame snapshot){
        List<Line> all=entries();
        if(choice>=all.size())choice=Math.max(0,all.size()-1);
        if(offset>choice)offset=choice;
        skin.frame(batch,9,11,622,377);
        skin.band(batch,19,348,600,31);
        line(batch,"CRAFT / RECIPE INDEX     "+view+"  ["+FILTERS[filter]+"]",
            29,368);
        line(batch,all.size()+" ENTRIES  TAB VIEW  F4 FILTER",29,339);
        line(batch,"FIND: "+clipped(search,17)+"_",391,339);
        skin.panel(batch,20,34,260,294);
        for(int i=offset;i<Math.min(offset+PER_PAGE,all.size());i++){
            Line entry=all.get(i);
            float y=285-(i-offset)*31;
            skin.choice(batch,"",26,y,249,29,i==choice);
            skin.slot(batch,30,y+2,25,i==choice);
            String target=entry.output().startsWith("FACILITY:")?null:entry.output();
            if(target!=null)icons.draw(batch,target,31,y+3,23);
            line(batch,clipped(entry.title(),25),62,y+20);
            if(entry.recipe()!=null){
                String tag=switch(entry.recipe().availability()){
                    case PLAYABLE_HAND,PLAYABLE_FIRE->"CRAFT NOW";
                    case STATION_UNIMPLEMENTED->"STATION";
                    case INDUSTRIAL_PLANNED->"PLANNED PROCESS";
                    case FACILITY_PLANNED->"PLANNED FACILITY";
                };
                line(batch,tag,62,y+8);
            }else{
                line(batch,clipped(entry.output(),32),62,y+8);
            }
        }
        skin.panel(batch,292,34,327,294);
        if(!all.isEmpty()){
            Line line=all.get(choice);
            if(line.recipe()!=null)detailRecipe(batch,line.recipe());
            else detailItem(batch,line.output());
        }
        skin.band(batch,18,14,603,18);
        line(batch,"ARROWS  PGUP/DN  ENTER FOLLOW  TAB  F4  ESC",29,28);
    }
    @Override public void close(){icons.close();}
}
