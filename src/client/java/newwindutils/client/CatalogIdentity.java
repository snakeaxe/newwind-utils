package newwindutils.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Stable server metadata, never enchantment level, damage or the player's custom name. */
public class CatalogIdentity {
    public String catalogId="",material="",group="",model="";
    public List<String> lore=new ArrayList<>();
    public static CatalogIdentity capture(ItemStack stack,String id){
        CatalogIdentity value=new CatalogIdentity();value.catalogId=id;value.material=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var cooldown=stack.get(DataComponents.USE_COOLDOWN);if(cooldown!=null)value.group=cooldown.cooldownGroup().map(Object::toString).orElse("");
        var data=stack.get(DataComponents.CUSTOM_MODEL_DATA);if(data!=null)value.model=data.toString();
        var lines=stack.get(DataComponents.LORE);if(lines!=null)for(var line:lines.lines()){
            String text=line.getString().trim();
            // Mechanics/upgrade lines survive renames, repair, reinforcement and kill counters.
            if(text.startsWith("-") && text.length()>5)value.lore.add(ItemCatalog.normalize(text));
        }
        return value;
    }
    public boolean valid(){return catalogId!=null && material!=null && group!=null && model!=null && lore!=null && (!group.isEmpty() || !model.isEmpty() || !lore.isEmpty());}
    public boolean matches(CatalogIdentity value){
        if(!valid() || !material.equals(value.material))return false;
        if(!group.isEmpty() && !group.equals(value.group))return false;
        if(!model.isEmpty() && !model.equals(value.model))return false;
        return lore.isEmpty() || value.lore.containsAll(lore);
    }
    public boolean sameSignature(CatalogIdentity other){return material.equals(other.material) && group.equals(other.group) && model.equals(other.model) && lore.equals(other.lore);}
    public static ItemCatalog.Entry resolve(ItemStack stack,List<CatalogIdentity> saved){
        CatalogIdentity actual=capture(stack,"");Set<String> matches=new HashSet<>();
        for(var identity:saved)if(identity.matches(actual))matches.add(identity.catalogId);
        return matches.size()==1?ItemCatalog.byId(matches.iterator().next()):null;
    }
    public static String register(ItemStack stack,ItemCatalog.Entry entry,NewwindUtilsConfig config){
        if(stack.isEmpty())return "Hold the item first.";
        if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(entry.materialId()))return "Wrong base material for "+entry.name()+".";
        CatalogIdentity identity=capture(stack,entry.id());
        if(!identity.valid())return "No stable server identity or mechanic lore found. Nothing was saved.";
        config.catalogIdentities.removeIf(old->old.sameSignature(identity));config.catalogIdentities.add(identity);
        return "Saved renamed-item identity for "+entry.name()+".";
    }
}
