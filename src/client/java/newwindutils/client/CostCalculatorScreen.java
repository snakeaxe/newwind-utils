package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

public class CostCalculatorScreen extends Screen {
    private final Screen parent;private final ItemCatalog.Entry entry;private final ItemCatalog.HeldState held;
    private UpgradeCosts.Operation operation;
    private boolean fromHeld,includeBase;
    private int currentSharp,currentUnbreaking,targetSharp,targetUnbreaking,currentUpgrade,targetUpgrade,resultPage;
    public CostCalculatorScreen(Screen parent,ItemCatalog.Entry entry){
        super(Component.literal("Costs: "+entry.name()));this.parent=parent;this.entry=entry;
        var client=Minecraft.getInstance();var match=client.player==null?null:ItemCatalog.find(client.player.getMainHandItem());
        held=match!=null && match.id().equals(entry.id())?ItemCatalog.heldState(client.player.getMainHandItem(),entry):null;
        fromHeld=held!=null;includeBase=!fromHeld;operation=entry.upgrades().isEmpty()?UpgradeCosts.Operation.SHARPEN:UpgradeCosts.Operation.UPGRADE;
        setStart();targetSharp=Math.max(currentSharp,30);targetUnbreaking=Math.max(currentUnbreaking,20);targetUpgrade=entry.upgrades().isEmpty()?-1:0;
    }
    private void setStart(){currentSharp=fromHeld?held.sharpness():entry.baseSharpness();currentUnbreaking=fromHeld?held.unbreaking():entry.baseUnbreaking();currentUpgrade=fromHeld?held.upgradeIndex():-1;}
    private Button button(String text,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(text,w-10)),b->action.run()).bounds(x,y,w,20).build());}
    private void rebuild(){clearWidgets();init();}
    @Override protected void init(){
        int half=(width-28)/2;
        button("Mode: "+operation,12,48,half,()->{operation=UpgradeCosts.Operation.values()[(operation.ordinal()+1)%3];resultPage=0;rebuild();});
        Button source=button("Start: "+(fromHeld?"Held item":"Base item"),16+half,48,half,()->{fromHeld=!fromHeld;includeBase=!fromHeld;setStart();resultPage=0;rebuild();});source.active=held!=null;
        if(operation==UpgradeCosts.Operation.UPGRADE){
            button("Current: "+upgradeName(currentUpgrade),12,72,half,()->{currentUpgrade=(currentUpgrade+2)%(entry.upgrades().size()+1)-1;resultPage=0;rebuild();});
            Button selected=button("Upgrade: "+upgradeName(targetUpgrade),16+half,72,half,()->{targetUpgrade=(targetUpgrade+1)%entry.upgrades().size();resultPage=0;rebuild();});selected.active=!entry.upgrades().isEmpty();
        }else{
            int max=operation==UpgradeCosts.Operation.SHARPEN?Math.max(30,Math.max(currentSharp,targetSharp)):Math.max(30,Math.max(currentUnbreaking,targetUnbreaking));
            addRenderableWidget(new NewwindUtilsConfigScreen.SettingSlider(this,12,72,half,new NewwindUtilsConfigScreen.NumericSetting("Current",()->operation==UpgradeCosts.Operation.SHARPEN?currentSharp:currentUnbreaking,v->{if(operation==UpgradeCosts.Operation.SHARPEN)currentSharp=(int)Math.round(v);else currentUnbreaking=(int)Math.round(v);resultPage=0;},0,Math.min(1000,max),1)));
            addRenderableWidget(new NewwindUtilsConfigScreen.SettingSlider(this,16+half,72,half,new NewwindUtilsConfigScreen.NumericSetting("Target",()->operation==UpgradeCosts.Operation.SHARPEN?targetSharp:targetUnbreaking,v->{if(operation==UpgradeCosts.Operation.SHARPEN)targetSharp=(int)Math.round(v);else targetUnbreaking=(int)Math.round(v);resultPage=0;},0,Math.min(1000,max),1)));
        }
        button("Include base item: "+(includeBase?"ON":"OFF"),12,96,half,()->{includeBase=!includeBase;resultPage=0;rebuild();});
        button("Price settings",16+half,96,half,()->openPrices(this));
        button("<",width/2-120,height-54,60,()->resultPage--);
        button(">",width/2+60,height-54,60,()->resultPage++);
        button("Back",width/2-100,height-28,200,this::onClose);
    }
    private String upgradeName(int index){return index<0 || index>=entry.upgrades().size()?"None":entry.upgrades().get(index).name();}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){
        super.extractRenderState(g,x,y,tick);
        var c=NewwindUtilsConfigManager.get();
        UpgradeCosts.Plan plan=operation==UpgradeCosts.Operation.UPGRADE?UpgradeCosts.ability(entry,targetUpgrade,currentUpgrade):UpgradeCosts.levels(operation,operation==UpgradeCosts.Operation.SHARPEN?currentSharp:currentUnbreaking,operation==UpgradeCosts.Operation.SHARPEN?targetSharp:targetUnbreaking,c);
        plan=UpgradeCosts.withBase(plan,entry,includeBase);
        List<String> text=new ArrayList<>();text.add(plan.complete()?"Material requirements":"INCOMPLETE QUOTE - known subtotal only");
        text.add(operation==UpgradeCosts.Operation.UPGRADE?"Selected upgrade: "+upgradeName(targetUpgrade):(operation==UpgradeCosts.Operation.SHARPEN?"Sharpness "+currentSharp+" -> "+targetSharp:"Unbreaking "+currentUnbreaking+" -> "+targetUnbreaking));
        if(plan.materials().isEmpty() && plan.complete())text.add("No additional materials.");
        plan.materials().forEach((item,amount)->text.add(amount+" x "+item));
        String equivalent=UpgradeCosts.eggEquivalent(plan.materials(),c);if(!equivalent.isEmpty())text.add(equivalent);
        text.add(plan.note());
        if(operation==UpgradeCosts.Operation.SHARPEN)text.add("Known rates: 10-20 = "+c.sharpenEmbersPerLevel+" embers/level; 20-30 = "+c.sharpenEggsPerLevel+" degg/level.");
        if(operation==UpgradeCosts.Operation.REINFORCE)text.add("Known rates: 0-10 = "+c.reinforceCoinsPerLevel+" coins/level; 10-20 = "+c.reinforceRemsPerLevel+" rems/level.");
        List<FormattedCharSequence> lines=new ArrayList<>();for(String value:text)lines.addAll(font.split(Component.literal(value),width-32));
        int count=Math.max(1,(height-182)/12),pages=Math.max(1,(lines.size()+count-1)/count);resultPage=Math.floorMod(resultPage,pages);
        g.fill(12,120,width-12,height-62,0xC008111A);g.outline(12,120,width-24,height-182,0xFF4A637B);
        for(int i=resultPage*count;i<Math.min(lines.size(),(resultPage+1)*count);i++)g.text(font,lines.get(i),16,124+(i%count)*12,plan.complete()?0xFFE6F1FF:0xFFFFCD88,false);
        g.centeredText(font,font.plainSubstrByWidth(title.getString(),width-24),width/2,12,0xFFFFFFFF);
        g.centeredText(font,"Wiki recipes / player-supplied level prices",width/2,30,0xFFBBBBBB);
        g.centeredText(font,(resultPage+1)+" / "+pages,width/2,height-48,0xFFBBBBBB);
    }
    public static void openPrices(Screen parent){
        var c=NewwindUtilsConfigManager.get();
        Minecraft.getInstance().gui.setScreen(new SimpleSettingsScreen(parent,"Improvement price settings","Player-supplied rates. Not an auction-price calculator.",()->List.of(
            SimpleSettingsScreen.number("Sharp 10-20: embers/level",()->c.sharpenEmbersPerLevel,v->c.sharpenEmbersPerLevel=(int)Math.round(v),0,1000000,1),
            SimpleSettingsScreen.number("Sharp 20-30: degg/level",()->c.sharpenEggsPerLevel,v->c.sharpenEggsPerLevel=(int)Math.round(v),0,1000000,1),
            SimpleSettingsScreen.number("Unbreaking 0-10: coins/level",()->c.reinforceCoinsPerLevel,v->c.reinforceCoinsPerLevel=(int)Math.round(v),0,1000000,1),
            SimpleSettingsScreen.number("Unbreaking 10-20: rems/level",()->c.reinforceRemsPerLevel,v->c.reinforceRemsPerLevel=(int)Math.round(v),0,1000000,1),
            SimpleSettingsScreen.number("Embers per dragon egg",()->c.embersPerEgg,v->c.embersPerEgg=(int)Math.round(v),1,1000000,1),
            SimpleSettingsScreen.number("Dragon eggs per stack",()->c.eggsPerStack,v->c.eggsPerStack=(int)Math.round(v),1,1000000,1)
        )));
    }
}
