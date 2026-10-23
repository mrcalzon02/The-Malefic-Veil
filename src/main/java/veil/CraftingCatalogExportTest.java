package veil;
import java.nio.file.*;
import java.util.*;
public final class CraftingCatalogExportTest {
    public static void main(String[] args)throws Exception{
        Path export=Files.createTempFile("malefic-recipe-unified-", ".csv");
        try{
            var counts=CraftingCatalogExport.write(Path.of("."),export);
            List<String> lines=Files.readAllLines(export);
            if(lines.size()!=counts.totalRows()+1||counts.field()<22||
                counts.processes()<47||counts.facilities()<53)
                throw new AssertionError("Missing canonical recipe/facility/art rows");
            if(counts.unillustrated()<5||counts.conceptOnly()<20)
                throw new AssertionError("Missing-art and concept-only entries are hidden");
            Set<String> keys=new HashSet<>();
            for(String line:lines.subList(1,lines.size())){
                int start=line.indexOf("\"")+1,end=line.indexOf("\"",start);
                if(start<1||end<start)throw new AssertionError("Invalid CSV export");
                if(!keys.add(line))throw new AssertionError("Duplicated complete catalog row");
            }
            for(String id:List.of("facility:purification-works",
                "facility:wall-gate-system","process:fortified-rations",
                "hand:hand-woven-cloth","item:TIMBER","item:CHITIN_SHARDS"))
                if(lines.stream().noneMatch(l->l.contains("\""+id+"\"")))
                    throw new AssertionError("Unfinished/source catalog entry missing "+id);
            if(lines.stream().noneMatch(s->s.contains("\"process:fortified-rations\"")&&
                s.contains("\"INDUSTRIAL_PLANNED\"")&&
                s.contains("\"ICON_REQUIRED\"")))
                throw new AssertionError("Inaccessible/unfinished manufacture labeled craftable");
            System.out.println("PASS CraftingCatalogExportTest recipes/facilities "+
                counts.totalRows()+"  source art 150  remaining art "+counts.unillustrated());
        }finally{Files.deleteIfExists(export);}
    }
}
