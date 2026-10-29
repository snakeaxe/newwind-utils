package newwindutils.client;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Published Obtaining links, without guessing drop odds or adding whole categories. */
public final class KeyDrops {
    private static class Data {Map<String,List<String>> keys;}
    private static final Map<String,List<String>> drops;
    public static final String error;
    static {
        Map<String,List<String>> loaded=new HashMap<>();String problem="";
        try(var stream=KeyDrops.class.getResourceAsStream("/assets/newwindutils/data/key_drops.json")){
            if(stream==null)throw new IllegalStateException("Drop catalog missing");
            var data=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Data.class);
            data.keys.forEach((key,ids)->loaded.put(key,List.copyOf(new LinkedHashSet<>(ids))));
        }catch(Exception e){problem=e.getMessage();}
        drops=Map.copyOf(loaded);error=problem;
    }
    private KeyDrops(){}
    public static boolean isKey(ItemCatalog.Entry entry){return entry.type().equals("Key") || entry.category().equals("Keys");}
    public static List<ItemCatalog.Entry> items(String key){return drops.getOrDefault(key,List.of()).stream().map(ItemCatalog::byId).filter(Objects::nonNull).sorted(Comparator.comparing(ItemCatalog.Entry::name,String.CASE_INSENSITIVE_ORDER)).toList();}
}
