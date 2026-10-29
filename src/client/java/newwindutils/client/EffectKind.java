package newwindutils.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;

public enum EffectKind {
    BIND("bind","General bind","Binded"), GAPPLE("gapple_bind","Gap bind","Gap binded"),
    ELYTRA("elytra_bind","Elytra bind","Elytra binded"), FOOD("food_bind","Food bind","Food binded"),
    OTHER("other","Other effects","Other");
    public final String id,label,selfLabel;
    EffectKind(String id,String label,String selfLabel){this.id=id;this.label=label;this.selfLabel=selfLabel;}
    public static EffectKind ofEffect(String id){
        if(id.startsWith("self:")) id=id.substring(5);
        return switch(id){case "bind"->BIND;case "gapple_bind"->GAPPLE;case "elytra_bind","clip","acropsis"->ELYTRA;
            case "food_bind","all_food"->FOOD;default->OTHER;};
    }
    public boolean visible(NewwindUtilsConfig config,boolean self){
        return !(self?config.hiddenSelfEffects:config.hiddenTargetEffects).contains(id);
    }
    public static EffectKind ofCooldown(String group,String name,ItemStack stack){
        if(stack!=null && !stack.isEmpty()){
            if(stack.is(Items.GOLDEN_APPLE)||stack.is(Items.ENCHANTED_GOLDEN_APPLE))return GAPPLE;
            if(stack.is(Items.ELYTRA))return ELYTRA;
            if(stack.is(Items.ENDER_PEARL)||stack.is(Items.WIND_CHARGE)||stack.is(Items.FIREWORK_ROCKET)||stack.is(Items.CHORUS_FRUIT))return BIND;
        }
        EffectKind byName=ofCooldownName(group+" "+name);
        if(byName!=OTHER)return byName;
        if(stack!=null && stack.has(DataComponents.FOOD))return FOOD;
        return OTHER;
    }
    static EffectKind ofCooldownName(String text){
        String key=text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        if(key.contains("goldenapple")||key.contains("gapple")||key.contains("egap")||key.contains("candiedapple")||key.contains("easterapple"))return GAPPLE;
        if(key.contains("elytra"))return ELYTRA;
        if(key.contains("enderpearl")||key.contains("pausepearl")||key.contains("searchpearl")||key.contains("windcharge")||key.contains("fireworkrocket")||key.contains("chorusfruit"))return BIND;
        return OTHER;
    }
}
