package veil;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

/** Parse and rank *published* GitHub Releases, including prereleases. */
final class ReleaseCatalog {
    private ReleaseCatalog() {}
    record Release(String tag, Instant published, Map<String,URI> assets) {
        URI asset(String name) { return assets.get(name); }
    }
    private static final class Json {
        final String input; int at;
        Json(String input) { this.input=input; }
        void ws() { while(at<input.length()&&Character.isWhitespace(input.charAt(at)))at++; }
        char next() throws IOException {
            if(at==input.length()) throw new IOException("Truncated JSON");
            return input.charAt(at++);
        }
        void expect(char expected) throws IOException {
            ws(); if(next()!=expected)throw new IOException("Malformed JSON");
        }
        String string() throws IOException {
            expect('"');StringBuilder out=new StringBuilder();
            while(true) {
                char c=next();
                if(c=='"')return out.toString();
                if(c<32)throw new IOException("Invalid JSON string");
                if(c!='\\') {out.append(c);continue;}
                switch(next()) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if(at+4>input.length())throw new IOException("Invalid Unicode escape");
                        try {out.append((char)Integer.parseInt(input.substring(at,at+4),16));}
                        catch(NumberFormatException bad) {throw new IOException("Invalid Unicode escape",bad);}
                        at+=4;
                    }
                    default -> throw new IOException("Invalid JSON escape");
                }
            }
        }
        Object value() throws IOException {
            ws();if(at==input.length())throw new IOException("Missing JSON value");
            char c=input.charAt(at);
            if(c=='"')return string();
            if(c=='{') {
                at++;Map<String,Object> result=new LinkedHashMap<>();ws();
                if(at<input.length()&&input.charAt(at)=='}'){at++;return result;}
                while(true) {
                    String key=string();expect(':');result.put(key,value());ws();
                    char sep=next();if(sep=='}')return result;
                    if(sep!=',')throw new IOException("Invalid JSON object");
                }
            }
            if(c=='[') {
                at++;List<Object> result=new ArrayList<>();ws();
                if(at<input.length()&&input.charAt(at)==']'){at++;return result;}
                while(true) {
                    result.add(value());ws();char sep=next();
                    if(sep==']')return result;
                    if(sep!=',')throw new IOException("Invalid JSON array");
                }
            }
            for(String literal:List.of("true","false","null"))if(input.startsWith(literal,at)) {
                at+=literal.length();
                return literal.equals("null")?null:Boolean.valueOf(literal);
            }
            int start=at;
            while(at<input.length()&&"-0123456789.eE+".indexOf(input.charAt(at))>=0)at++;
            if(at==start)throw new IOException("Invalid JSON literal");
            return input.substring(start,at);
        }
        Object parse()throws IOException {
            Object result=value();ws();
            if(at!=input.length())throw new IOException("Trailing JSON");
            return result;
        }
    }
    static int resultCount(String json)throws IOException {
        Object data=new Json(json).parse();
        if(!(data instanceof List<?> list))throw new IOException("GitHub releases response not an array");
        return list.size();
    }
    static List<Release> parse(String json)throws IOException {
        Object data=new Json(json).parse();
        if(!(data instanceof List<?> list))throw new IOException("GitHub releases response not an array");
        List<Release> releases=new ArrayList<>();
        for(Object object:list) {
            if(!(object instanceof Map<?,?> r)||Boolean.TRUE.equals(r.get("draft")))continue;
            if(!(r.get("tag_name") instanceof String tag)||
                !tag.matches("dev-[a-f0-9]{12}"))continue;
            if(!(r.get("published_at") instanceof String date))continue;
            Instant published;
            try {published=Instant.parse(date);}catch(RuntimeException invalid){continue;}
            Map<String,URI> assets=new HashMap<>();
            if(r.get("assets") instanceof List<?> entries)for(Object entry:entries){
                if(!(entry instanceof Map<?,?> a))continue;
                if(!(a.get("name") instanceof String name)||
                    !(a.get("browser_download_url") instanceof String url))continue;
                String prefix="https://github.com/mrcalzon02/The-Malefic-Veil/releases/download/"+tag+"/";
                if(!url.equals(prefix+name))continue;
                if(!name.matches("[A-Za-z0-9._-]{1,120}"))continue;
                assets.put(name,URI.create(url));
            }
            if(assets.containsKey("update.properties"))releases.add(new Release(tag,published,assets));
        }
        return releases;
    }
    static Release newest(Collection<Release> releases) {
        return releases.stream().max(Comparator.comparing(Release::published)
            .thenComparing(Release::tag)).orElse(null);
    }
}
