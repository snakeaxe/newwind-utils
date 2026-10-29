package newwindutils.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Mechanical material counts; no auction-price assumptions. Rates supplied by the player. */
public final class UpgradeCosts {
    public enum Operation { UPGRADE, SHARPEN, REINFORCE }
    public record Plan(Map<String,Long> materials,boolean complete,int missingSteps,String note){
        public Plan{materials=Collections.unmodifiableMap(new LinkedHashMap<>(materials));}
    }
    private UpgradeCosts(){}
    public static Plan levels(Operation operation,int from,int target,NewwindUtilsConfig c){
        Map<String,Long> costs=new LinkedHashMap<>();int missing=0;
        if(from<0 || target<from || target>1000)return new Plan(costs,false,0,"Target must be at least the current level (0-1000).");
        if(operation==Operation.UPGRADE)return new Plan(costs,false,0,"Choose an item upgrade.");
        for(int level=from;level<target;level++){
            String item=null;long amount=0;
            if(operation==Operation.SHARPEN){
                if(level>=10 && level<20){item="Ember";amount=c.sharpenEmbersPerLevel;}
                else if(level>=20 && level<30){item="Dragon Egg";amount=c.sharpenEggsPerLevel;}
            }else{
                if(level>=0 && level<10){item="Gold Coin";amount=c.reinforceCoinsPerLevel;}
                else if(level>=10 && level<20){item="Star Remnant";amount=c.reinforceRemsPerLevel;}
            }
            if(item==null)missing++;else costs.merge(item,amount,Long::sum);
        }
        String note=missing>0?"Unpriced levels: "+missing+". Displayed materials are a subtotal, not the total.":from==target?"Already at the selected level.":"Rates supplied by player; edit in Price settings.";
        return new Plan(costs,missing==0,missing,note);
    }
    public static Plan ability(ItemCatalog.Entry entry,int selected,int current){
        if(selected<0 || selected>=entry.upgrades().size())return new Plan(Map.of(),false,0,"No upgrade selected / no published recipe.");
        if(selected==current)return new Plan(Map.of(),true,0,"Selected upgrade is already applied; no additional recipe required.");
        var upgrade=entry.upgrades().get(selected);Map<String,Long> costs=new LinkedHashMap<>();
        for(var ingredient:upgrade.ingredients())if(ingredient.amount()>0)costs.merge(ingredient.item(),ingredient.amount(),Long::sum);
        return new Plan(costs,!costs.isEmpty(),0,costs.isEmpty()?"Recipe unavailable; check wiki.":
                current>=0?"Full new recipe shown; removal refunds are not deducted.":"Wiki recipe. Sacrifices are additional to the base item.");
    }
    public static Plan withBase(Plan plan,ItemCatalog.Entry entry,boolean includeBase){
        if(!includeBase)return plan;
        Map<String,Long> result=new LinkedHashMap<>(plan.materials());result.merge(entry.name(),1L,Long::sum);
        return new Plan(result,plan.complete(),plan.missingSteps(),plan.note()+" Includes one base item.");
    }
    public static String eggEquivalent(Map<String,Long> materials,NewwindUtilsConfig c){
        long embers=materials.getOrDefault("Ember",0L),eggs=materials.getOrDefault("Dragon Egg",0L);
        if(embers==0 && eggs==0)return "";
        long units=embers+eggs*c.embersPerEgg,perStack=(long)c.embersPerEgg*c.eggsPerStack;
        long stacks=units/perStack,remainder=units%perStack;
        String fraction=BigDecimal.valueOf(remainder).divide(BigDecimal.valueOf(c.embersPerEgg),4,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return "Currency equivalent: "+stacks+" stegg + "+fraction+" degg";
    }
}
