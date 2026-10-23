package veil.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import veil.CraftingRecipeIndex;
import veil.CraftingEraRegistry;
import java.util.function.Supplier;
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
    private final Supplier<CraftingEraRegistry.Faction> hostFaction;
    private CraftingEraRegistry.Faction faction(){
        return Objects.requireNonNull(hostFaction.get(),"Missing host faction");
    }
    private View view=View.RECIPES;
    private int choice=0,offset=0,filter=0,ingredientOffset=0,browse=0;
    private record Place(View view,int choice,int offset,int filter,int ingredientOffset,int browse,String search) {}
    private final Deque<Place> history=new ArrayDeque<>();
    private String search="";
    private static final String[] FILTERS={"ALL","FIELD","INDUSTRIAL","FACILITIES"};
    private static final String[] BROWSE={"OWN","ALL","UNRAVELED","PENITENT","BEGOTTEN"};
    private CraftingEraRegistry.Faction listingFaction(){
        return switch(browse){
            case 0 -> faction();
            case 1 -> CraftingEraRegistry.Faction.ALL;
            case 2 -> CraftingEraRegistry.Faction.UNRAVELED;
            case 3 -> CraftingEraRegistry.Faction.PENITENT;
            case 4 -> CraftingEraRegistry.Faction.BEGOTTEN;
            default -> throw new IllegalStateException("Invalid browse filter");
        };
    }
    private List<Record> makers(String item){return browse==1?
        CraftingRecipeIndex.makes(item):CraftingRecipeIndex.makes(item,listingFaction());}
    private List<Record> uses(String item){return browse==1?
        CraftingRecipeIndex.uses(item):CraftingRecipeIndex.uses(item,listingFaction());}
    private void remember(){
        if(history.size()==32)history.removeFirst();
        history.addLast(new Place(view,choice,offset,filter,ingredientOffset,browse,search));
    }
    private void back(){
        if(history.isEmpty())return;
        Place p=history.removeLast();
        view=p.view();choice=p.choice();offset=p.offset();filter=p.filter();
        ingredientOffset=p.ingredientOffset();browse=p.browse();search=p.search();
    }
    private record Line(String title,String output,Record recipe){}
    public GpuCraftingBook(PsxHorrorMenu skin,
            Supplier<CraftingEraRegistry.Faction> hostFaction){
        this.skin=Objects.requireNonNull(skin);
        this.hostFaction=Objects.requireNonNull(hostFaction);
        this.icons=new GpuCraftingIcons();
    }
    public void reset(){
        view=View.RECIPES;choice=0;offset=0;filter=0;ingredientOffset=0;browse=0;search="";history.clear();
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
        if(browse!=1){
            browse=1;
            known=entries();
            for(int i=0;i<known.size();i++)
                if(known.get(i).output().equals(itemId)){setChoice(i);return;}
        }
        throw new IllegalArgumentException("Unknown indexed item "+itemId);
    }
    void inspectRecipe(String recipeKey){
        view=View.RECIPES;filter=0;search="";offset=0;
        var selected=entries();
        for(int i=0;i<selected.size();i++){
            Record r=selected.get(i).recipe();
            if(r!=null&&r.key().equals(recipeKey)){setChoice(i);return;}
        }
        if(browse!=1){
            browse=1;
            selected=entries();
            for(int i=0;i<selected.size();i++)
                if(selected.get(i).recipe()!=null&&
                   selected.get(i).recipe().key().equals(recipeKey)){setChoice(i);return;}
        }
        throw new IllegalArgumentException("Unknown recipe selection "+recipeKey);
    }
    /** Real OpenGL audit selection exercises read-only foreign-faction listings. */
    void auditFactionRecipe(String key,CraftingEraRegistry.Faction owner){
        browse=switch(owner){
            case ALL->1;
            case UNRAVELED->2;
            case PENITENT->3;
            case BEGOTTEN->4;
        };
        inspectRecipe(key);
    }
    private void followIngredient(int row){
        List<Line> rows=entries();
        if(rows.isEmpty()||rows.get(choice).recipe()==null)return;
        List<String> ids=new ArrayList<>(rows.get(choice).recipe().inputs().keySet());
        int index=ingredientOffset+row;
        if(index>=0&&index<ids.size()){
            remember();inspectItem(ids.get(index));
        }
    }
    private void followDownstream(){
        List<Line> rows=entries();
        if(rows.isEmpty())return;
        List<Record> dependents=uses(rows.get(choice).output());
        if(!dependents.isEmpty()){
            remember();inspectRecipe(dependents.get(0).key());
        }
    }
    private void followSelected(){
        List<Line> rows=entries();
        if(rows.isEmpty())return;
        Line chosen=rows.get(choice);
        if(chosen.recipe()!=null){
            if(chosen.recipe().kind()==CraftingRecipeIndex.Kind.FACILITY){followDownstream();return;}
            String target=chosen.output();
            remember();view=View.ITEMS;filter=0;search="";offset=0;
            List<Line> products=entries();
            for(int i=0;i<products.size();i++)
                if(products.get(i).output().equals(target)){setChoice(i);return;}
        }else{
            var producers=makers(chosen.output());
            if(!producers.isEmpty()){remember();inspectRecipe(producers.get(0).key());}
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
                String pack=icons.pack(id);
                if(browse!=1&&pack.equals("unraveled_industry")&&
                   listingFaction()!=CraftingEraRegistry.Faction.UNRAVELED)continue;
                if(browse!=1&&pack.equals("penitent_ritual")&&
                   listingFaction()!=CraftingEraRegistry.Faction.PENITENT)continue;
                if(browse!=1&&pack.equals("begotten_biology")&&
                   listingFaction()!=CraftingEraRegistry.Faction.BEGOTTEN)continue;
                if(browse!=1&&!CraftingRecipeIndex.visibleItem(id,listingFaction()))continue;
                List<Record> producers=makers(id);
                if(filter==1&&producers.isEmpty())continue;
                if(filter==2&&producers.stream().noneMatch(
                    r->r.kind()==CraftingRecipeIndex.Kind.INDUSTRIAL))continue;
                if(filter==3&&!producers.isEmpty())continue;
                list.add(new Line(icons.label(id),id,null));
            }
        }else for(Record recipe:(browse==1?CraftingRecipeIndex.recipes():CraftingRecipeIndex.recipesFor(listingFaction()))){
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
        if(Gdx.input.isKeyJustPressed(Input.Keys.F5)){
            browse=(browse+1)%BROWSE.length;choice=0;offset=0;ingredientOffset=0;touched=true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.F6)){followIngredient(0);touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.F7)){followDownstream();touched=true;}
        if(Gdx.input.isKeyJustPressed(Input.Keys.F8)){back();touched=true;}
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
                        if(offset+i<entries().size())setChoice(offset+i);touched=true;break;
                    }
                }
            }
        }
        return touched;
    }
    private void line(SpriteBatch batch,String content,float x,float y){
        skin.font().draw(batch,clipped(content,46),x,y);
    }
    private String status(Record record,MaleficVeil.GpuFrame snapshot){
        if(record==null)return "NO MANUFACTURING RECIPE";
        if(!CraftingRecipeIndex.belongsTo(record,faction()))
            return "FACTION LOCK: "+record.faction()+" / VIEW ONLY";
        if(record.kind()==CraftingRecipeIndex.Kind.FIELD){
            if(snapshot==null)return "HOST CRAFT STATE UNAVAILABLE";
            for(MaleficVeil.GpuCraftRecipe craft:snapshot.crafts())
                if(record.key().equals("hand:"+craft.id()))
                    return craft.affordable()?"READY: HOST VALIDATES":
                        "BLOCKED: "+clipped(craft.description(),27);
            return "HOST RECIPE UNAVAILABLE";
        }
        return CraftingRecipeIndex.availabilityText(record);
    }
    private void detailRecipe(SpriteBatch batch,Record record,MaleficVeil.GpuFrame snapshot){
        line(batch,"RECIPE  "+clipped(record.name(),29),314,309);
        String icon=record.output().startsWith("FACILITY:")?null:record.output();
        if(icon!=null&&icons.contains(icon))icons.draw(batch,icon,315,253,43);
        else{
            skin.slot(batch,315,253,43,false);
            line(batch,record.kind()==CraftingRecipeIndex.Kind.FACILITY?"SITE":"NO ART",319,279);
        }
        line(batch,"YIELD "+record.quantity()+"  "+clipped(record.output(),25),365,287);
        line(batch,status(record,snapshot),365,267);
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
            line(batch,"+"+(record.inputs().size()-start-row)+" MORE  ] NEXT",314,79);
        else if(start>0)
            line(batch,"[ PREVIOUS COMPONENTS",314,79);
        String related=CraftingRecipeIndex.usesText(record.output(),2,listingFaction());
        line(batch,"USED FOR: "+clipped(related,34),314,48);
        if(record.kind()==CraftingRecipeIndex.Kind.FACILITY &&
           !record.prerequisites().isEmpty())
            line(batch,"NEEDS: "+clipped(String.join(", ",record.prerequisites()),34),314,63);
    }
    private void detailItem(SpriteBatch batch,String item){
        line(batch,"ITEM  "+clipped(icons.label(item),32),314,309);
        skin.slot(batch,315,248,52,false);
        icons.draw(batch,item,315,248,52);
        if(!icons.contains(item))line(batch,"UNILLUSTRATED SOURCE ITEM",374,280);
        String status=!makers(item).isEmpty()?
            "YOUR FACTION HAS PRODUCTION":
            (CraftingRecipeIndex.items().contains(item)?"HARVEST/LOOT OR INPUT":"ICON-ONLY CONCEPT");
        line(batch,status,314,236);
        List<Record> makers=makers(item);
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
        for(Record r:uses(item)){
            if(row==4)break;
            line(batch,clipped(r.name()+" / "+r.station(),43),314,116-row*18);
            row++;
        }
        if(row==0)line(batch,"NO REGISTERED DOWNSTREAM USE",314,116);
        int other=uses(item).size()-row;
        if(other>0)line(batch,"+"+other+" MORE USES",314,44);
    }
    public void draw(SpriteBatch batch,MaleficVeil.GpuFrame snapshot){
        List<Line> all=entries();
        if(choice>=all.size())choice=Math.max(0,all.size()-1);
        if(offset>choice)offset=choice;
        skin.frame(batch,9,11,622,377);
        skin.band(batch,19,348,600,31);
        line(batch,""+faction().name()+" / "+view+" ["+FILTERS[filter]+"] BROWSE "+BROWSE[browse],
            29,368);
        line(batch,all.size()+" ENTRIES  TAB VIEW  F4 FILTER  F5 FACTION",29,339);
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
                    case PLAYABLE_HAND,PLAYABLE_FIRE,PLAYABLE_WORKBENCH->"CRAFT NOW";
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
            if(line.recipe()!=null)detailRecipe(batch,line.recipe(),snapshot);
            else detailItem(batch,line.output());
        }
        skin.band(batch,18,14,603,18);
        line(batch,"ARROWS  ENTER FOLLOW  F6 INGREDIENT  F7 USED BY  F8 BACK",29,28);
    }
    @Override public void close(){icons.close();}
}
