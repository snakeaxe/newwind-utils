package newwindutils.client;

import com.google.gson.Gson;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

public final class ItemCatalog {
    public record Ingredient(String item,long amount) {}
    public record Upgrade(String name,String costText,List<Ingredient> ingredients,String details) {
        public Upgrade { ingredients=ingredients==null?List.of():List.copyOf(ingredients); }
    }
    public record Entry(String id,String name,String category,String type,String material,String materialId,
                        String summary,List<String> effects,String years,int baseSharpness,int baseUnbreaking,
                        List<Upgrade> upgrades,boolean areaEffect,String source,boolean detailsAvailable) {
        public Entry {effects=effects==null?List.of():List.copyOf(effects);upgrades=upgrades==null?List.of():List.copyOf(upgrades);}
        public String searchText(){return normalize(name+" "+category+" "+type+" "+material+" "+summary+" "+String.join(" ",effects)+" "+upgrades);}
    }
    public record HeldState(int sharpness,int unbreaking,int upgradeIndex) {}
    private static class Data {String retrieved;List<Entry> items;}
    private static final List<Entry> entries;
    private static final Map<String,String> names=new HashMap<>(),searchable=new HashMap<>();
    private static final Map<String,Entry> ids=new HashMap<>();
    public static final String retrieved;
    public static final String error;
    static {
        List<Entry> loaded=List.of();String date="Unknown",problem="";
        try(var stream=ItemCatalog.class.getResourceAsStream("/assets/newwindutils/data/item_catalog.json")) {
            if(stream==null)throw new IllegalStateException("Catalog resource missing");
            Data data=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Data.class);
            loaded=data.items.stream().filter(e->e.id()!=null && e.id().matches("[a-z0-9_-]+") && e.name()!=null)
                    .sorted(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER)).toList();date=data.retrieved;
        }catch(Exception e){problem=e.getMessage();}
        entries=List.copyOf(loaded);retrieved=date;error=problem;
        for(Entry entry:entries){names.put(entry.id(),normalize(entry.name()));searchable.put(entry.id(),entry.searchText());ids.put(entry.id(),entry);}
    }
    private ItemCatalog(){}
    public static List<Entry> all(){return entries;}
    public static Entry byId(String id){return ids.get(id);}
    public static String normalize(String text){
        if(text==null)return "";
        return Normalizer.normalize(text,Normalizer.Form.NFKD).replaceAll("(?i)\u00a7[0-9a-fk-or]","")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
    }
    public static List<Entry> search(String query,String category){
        if(normalize(query).equals("helist"))query="heliast";
        if(normalize(query).equals("cscythe"))query="cupid scythe";
        String[] tokens=query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return entries.stream().filter(e->category.equals("All") || category.equals(e.category()))
                .filter(e->{String hay=searchable.get(e.id());for(String token:tokens)if(!hay.contains(normalize(token)))return false;return true;}).toList();
    }
    public static Entry match(String itemId,String displayName){
        String name=normalize(displayName);Entry best=null;
        for(Entry entry:entries){
            String candidate=names.get(entry.id());
            if(candidate.isEmpty() || !name.contains(candidate))continue;
            boolean exact=name.equals(candidate);
            if(!exact && !entry.materialId().equals(itemId))continue;
            if(best==null || candidate.length()>names.get(best.id()).length())best=entry;
        }
        return best;
    }
    public static Entry find(ItemStack stack){return find(stack,NewwindUtilsConfigManager.get().catalogIdentities);}
    static Entry find(ItemStack stack,List<CatalogIdentity> identities){
        if(stack.isEmpty())return null;
        String material=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var original=stack.get(DataComponents.ITEM_NAME);
        Entry entry=original==null?null:match(material,original.getString());
        if(entry!=null)return entry;
        var cooldown=stack.get(DataComponents.USE_COOLDOWN);
        if(cooldown!=null && cooldown.cooldownGroup().isPresent()){
            var id=cooldown.cooldownGroup().get();Entry byGroup=ids.get(id.getPath());
            if(id.getNamespace().equals("nw") && byGroup!=null && byGroup.materialId().equals(material))return byGroup;
        }
        entry=CatalogIdentity.resolve(stack,identities);if(entry!=null)return entry;
        entry=match(material,stack.getHoverName().getString());if(entry!=null)return entry;
        var lore=stack.get(DataComponents.LORE);
        return lore==null?null:matchLore(material,lore.lines().stream().map(line->line.getString()).toList());
    }
    public static Entry matchLore(String material,List<String> lines){
        Set<String> normalized=new HashSet<>();for(String line:lines)normalized.add(normalize(line));
        List<Entry> matches=new ArrayList<>();
        for(Entry entry:entries){
            if(!entry.materialId().equals(material) || entry.effects().isEmpty())continue;
            if(entry.effects().stream().filter(e->normalize(e).length()>=8).allMatch(e->normalized.contains(normalize(e)))
                    && entry.effects().stream().anyMatch(e->normalize(e).length()>=8))matches.add(entry);
        }
        return matches.size()==1?matches.get(0):null;
    }
    public static boolean radiusUpgrade(String name){String value=normalize(name);return value.contains("radius") || value.contains("largerpools") || value.contains("increasedrange");}
    public static HeldState heldState(ItemStack stack,Entry entry){
        int sharp=0,unbreaking=0;
        var enchants=stack.get(DataComponents.ENCHANTMENTS);
        if(enchants!=null)for(var enchant:enchants.entrySet()){
            if(enchant.getKey().is(Enchantments.SHARPNESS))sharp=enchant.getIntValue();
            if(enchant.getKey().is(Enchantments.UNBREAKING))unbreaking=enchant.getIntValue();
        }
        StringBuilder lore=new StringBuilder(stack.getHoverName().getString());
        var lines=stack.get(DataComponents.LORE);if(lines!=null)for(var line:lines.lines())lore.append(' ').append(line.getString());
        int upgrade=-1;String normalized=normalize(lore.toString());
        for(int i=0;i<entry.upgrades().size();i++)if(normalized.contains(normalize(entry.upgrades().get(i).name())))upgrade=i;
        return new HeldState(sharp,unbreaking,upgrade);
    }
}
