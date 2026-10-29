package veil;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Offline release ordering, prerelease inclusion and malformed JSON gates. */
public final class ReleaseCatalogTest {
    private static void yes(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static String item(String tag,String date,boolean draft){
        String base="https://github.com/mrcalzon02/The-Malefic-Veil/releases/download/"+tag+"/";
        return "{\"tag_name\":\""+tag+"\",\"draft\":"+draft+
            ",\"prerelease\":true,\"published_at\":\""+date+"\",\"body\":\"test {nested} \\\", text\", "+
            "\"assets\":[{\"name\":\"update.properties\",\"browser_download_url\":\""+
            base+"update.properties\"},{\"name\":\"MaleficVeil-"+tag.substring(4)+
            ".zip\",\"browser_download_url\":\""+base+
            "MaleficVeil-"+tag.substring(4)+".zip\"}]}";
    }
    public static void main(String[] args)throws Exception{
        String early="dev-aaaaaaaaaaaa",later="dev-bbbbbbbbbbbb";
        String json="["+item(later,"2026-10-22T14:00:00Z",false)+","+
            item(early,"2026-10-22T13:00:00Z",false)+","+
            item("dev-cccccccccccc","2026-10-23T13:00:00Z",true)+"]";
        var releases=ReleaseCatalog.parse(json);
        yes(releases.size()==2,"Published prereleases not selected properly");
        yes(ReleaseCatalog.newest(releases).tag().equals(later),
            "Newer published prerelease must win, regardless of API order");
        yes(ReleaseCatalog.newest(releases).published().equals(
            Instant.parse("2026-10-22T14:00:00Z")),"Publication timestamp wrong");
        yes(ReleaseCatalog.parse("[]").isEmpty(),"Empty release list");
        boolean malformed=false;
        try{ReleaseCatalog.parse("{invalid");}catch(IOException expected){malformed=true;}
        yes(malformed,"Malformed JSON accepted");
        yes(!DesktopUpdater.isNewer(ReleaseCatalog.newest(releases),
            "bbbbbbbbbbbb"+"b".repeat(28),releases),"Same installed release selected again");
        yes(DesktopUpdater.isNewer(ReleaseCatalog.newest(releases),
            "aaaaaaaaaaaa"+"a".repeat(28),releases),"Newer release not offered");
        System.out.println("PASS ReleaseCatalogTest");
    }
}
