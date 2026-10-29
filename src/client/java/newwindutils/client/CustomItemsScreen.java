package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public class CustomItemsScreen extends Screen {
    private final Screen parent;
    private int page;
    private String error="";
    public CustomItemsScreen(Screen parent) { super(Component.literal("Custom Items")); this.parent=parent; }
    public static void openHeld(Screen parent) {
        Minecraft client=Minecraft.getInstance();
        CustomItemsScreen list=parent instanceof CustomItemsScreen existing ? existing : new CustomItemsScreen(parent);
        if(client.player==null || client.player.getMainHandItem().isEmpty()) {
            list.error="Hold an item in your main hand first."; client.gui.setScreen(list);
        } else client.gui.setScreen(new Editor(list,CustomItemRule.capture(client.player.getMainHandItem()),false));
    }
    private Button button(String text,int x,int y,int w,Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());
    }
    private String fit(String text,int maxWidth) { return font.plainSubstrByWidth(text,maxWidth); }
    @Override protected void init() {
        int w=Math.min(340,width-20),x=(width-w)/2;
        button("Register held item (K)",x,48,w,()->openHeld(this));
        Button incoming=button("Last incoming hit: choose weapon / armor",x,72,w,()->minecraft.gui.setScreen(new Incoming(this)));
        incoming.active=!CooldownObservations.lastHitItems().isEmpty();
        List<CustomItemRule> rules=NewwindUtilsConfigManager.get().customItems;
        int perPage=Math.max(1,(height-168)/24),pages=Math.max(1,(rules.size()+perPage-1)/perPage);
        page=Math.max(0,Math.min(page,pages-1));
        for(int i=page*perPage;i<Math.min(rules.size(),(page+1)*perPage);i++) {
            CustomItemRule rule=rules.get(i);
            button(fit((rule.enabled?"ON ":"OFF ")+rule.label+" / "+rule.weapon().effectDisplayName(),w-12),x,108+(i%perPage)*24,w,
                    ()->minecraft.gui.setScreen(new Editor(this,rule.copy(),true)));
        }
        if(pages>1) {
            button("<",width/2-100,height-54,50,()->{page=(page-1+pages)%pages;rebuild();});
            Button label=button((page+1)+" / "+pages,width/2-45,height-54,90,()->{});label.active=false;
            button(">",width/2+50,height-54,50,()->{page=(page+1)%pages;rebuild();});
        }
        button("Done",width/2-100,height-28,200,this::onClose);
    }
    private void rebuild() { clearWidgets();init(); }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick) {
        super.extractRenderState(g,x,y,tick);
        g.centeredText(font,title,width/2,12,0xFFFFFFFF);
        g.centeredText(font,error.isEmpty()?"L: arm / finish a received-hit stopwatch":error,width/2,30,0xFFCCCCCC);
    }
    private static String sourceName(TrackedWeapon.SourceMode mode) {
        return switch(mode) { case HELD_MAIN_HAND->"Main hand"; case HELD_OFF_HAND->"Off hand";
            case HELD_EITHER->"Either hand"; case CHEST_ARMOR->"Chest armor"; case LEGS_ARMOR->"Leg armor"; };
    }
    private static String effectName(CustomItemRule.Effect effect) {
        return switch(effect) {case BIND->"Bind";case ELYTRA->"Elytra";case FOOD->"Food only";
            case GAPPLE->"Gapple";case ALL_FOOD->"All food (includes gaps)";case CUSTOM->"Custom timer label";};
    }
    private static class Editor extends Screen {
        private final Screen parent;
        private final CustomItemRule draft;
        private final boolean existing;
        private String error="";
        Editor(Screen parent,CustomItemRule draft,boolean existing) {
            super(Component.literal(existing?"Edit tracked item":"Register tracked item"));
            this.parent=parent;this.draft=draft;this.existing=existing;
        }
        private Button button(String text,int x,int y,int w,Runnable action) {
            return addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());
        }
        private void rebuild() {clearWidgets();init();}
        @Override protected void init() {
            int w=Math.min(340,width-20),x=(width-w)/2;
            EditBox label=new EditBox(font,x,56,w,20,Component.literal("Timer name"));
            label.setMaxLength(80);label.setValue(draft.label);label.setResponder(v->draft.label=v);addRenderableWidget(label);
            addRenderableWidget(new NewwindUtilsConfigScreen.SettingSlider(this,x,80,w-62,
                    new NewwindUtilsConfigScreen.NumericSetting("Seconds",()->draft.durationTicks/20.0,
                            v->draft.durationTicks=(int)Math.round(v*20),0.05,300,0.05)));
            button("Recent",x+w-58,80,58,()->minecraft.gui.setScreen(new Samples(this,s->{ if(s.ticks()>6000) error="This sample exceeds the 300-second rule limit."; else draft.durationTicks=s.ticks(); })));
            button("Effect: "+effectName(draft.effect),x,104,w,()->{
                var values=CustomItemRule.Effect.values();draft.effect=values[(draft.effect.ordinal()+1)%values.length];rebuild();});
            button("Source: "+sourceName(draft.source),x,128,w-72,()->{
                var values=new TrackedWeapon.SourceMode[]{TrackedWeapon.SourceMode.HELD_MAIN_HAND,
                        TrackedWeapon.SourceMode.HELD_EITHER,TrackedWeapon.SourceMode.CHEST_ARMOR,TrackedWeapon.SourceMode.LEGS_ARMOR};
                int i=Arrays.asList(values).indexOf(draft.source);draft.source=values[(i+1)%values.length];
                if(draft.source==TrackedWeapon.SourceMode.CHEST_ARMOR || draft.source==TrackedWeapon.SourceMode.LEGS_ARMOR)
                    draft.trigger=TrackedWeapon.TriggerType.MELEE_HIT;
                rebuild();});
            button(draft.enabled?"ON":"OFF",x+w-68,128,68,()->{draft.enabled=!draft.enabled;rebuild();});
            Button trigger=button("Trigger: "+(draft.trigger==TrackedWeapon.TriggerType.MELEE_HIT?"Confirmed melee hit":"Right-click player (estimate)"),x,152,w,()->{
                draft.trigger=draft.trigger==TrackedWeapon.TriggerType.MELEE_HIT?TrackedWeapon.TriggerType.USE_ENTITY:TrackedWeapon.TriggerType.MELEE_HIT;rebuild();});
            trigger.active=draft.source!=TrackedWeapon.SourceMode.CHEST_ARMOR && draft.source!=TrackedWeapon.SourceMode.LEGS_ARMOR;
            EditBox lore=new EditBox(font,x,176,w,20,Component.literal("Optional exact lore line"));
            lore.setMaxLength(200);lore.setHint(Component.literal("Optional: exact lore line for this variant"));
            lore.setValue(draft.requiredLore);lore.setResponder(v->draft.requiredLore=v);addRenderableWidget(lore);
            int bw=(w-8)/3;
            button("Save",x,height-28,bw,()->{
                if(!draft.valid()) {error="Enter a timer name.";return;}
                var rules=NewwindUtilsConfigManager.get().customItems;
                rules.removeIf(r->r.id.equals(draft.id));rules.add(draft.copy());persist();onClose();});
            Button delete=button("Delete",x+bw+4,height-28,bw,()->{
                NewwindUtilsConfigManager.get().customItems.removeIf(r->r.id.equals(draft.id));persist();onClose();});
            delete.active=existing;
            button("Cancel",x+2*(bw+4),height-28,bw,this::onClose);
        }
        @Override public void onClose(){minecraft.gui.setScreen(parent);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){
            super.extractRenderState(g,x,y,tick);
            g.centeredText(font,title,width/2,12,0xFFFFFFFF);
            String identity=draft.cooldownGroup==null?"Matches item type + name":"Matches "+draft.cooldownGroup;
            g.centeredText(font,font.plainSubstrByWidth(error.isEmpty()?identity:error,width-20),width/2,30,0xFFCCCCCC);
            g.centeredText(font,font.plainSubstrByWidth(draft.itemName,width-20),width/2,43,0xFFFFFFFF);
        }
    }
    private static void persist(){NewwindUtilsConfigManager.save();NewwindUtilsClient.refreshWeaponRegistry();}
    private static class Incoming extends Screen {
        private final Screen parent;
        Incoming(Screen parent){super(Component.literal("Last incoming hit: observed items"));this.parent=parent;}
        @Override protected void init(){
            int w=Math.min(360,width-20),x=(width-w)/2,y=58;
            for(var candidate:CooldownObservations.lastHitItems()) {
                addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(candidate.label(),w-12)),b->
                        minecraft.gui.setScreen(new Editor(parent,candidate.rule().copy(),false))).bounds(x,y,w,20).build());y+=24;
            }
            addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(width/2-100,height-28,200,20).build());
        }
        @Override public void onClose(){minecraft.gui.setScreen(parent);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){super.extractRenderState(g,x,y,tick);
            g.centeredText(font,title,width/2,14,0xFFFFFFFF);
            g.centeredText(font,"Choose the item you believe caused the effect.",width/2,34,0xFFCCCCCC);}
    }
    private static class Samples extends Screen {
        private final Screen parent;
        private final Consumer<CooldownObservations.Sample> choose;
        private int page;
        Samples(Screen parent,Consumer<CooldownObservations.Sample> choose){super(Component.literal("Recent durations"));this.parent=parent;this.choose=choose;}
        @Override protected void init(){
            var samples=CooldownObservations.samples();int count=Math.max(1,(height-120)/24);
            int pages=Math.max(1,(samples.size()+count-1)/count);page=Math.min(page,pages-1);
            int w=Math.min(380,width-20),x=(width-w)/2;
            for(int i=page*count;i<Math.min(samples.size(),(page+1)*count);i++) {
                var sample=samples.get(i);
                String text=String.format(Locale.US,"%.2fs / %s / %s",sample.ticks()/20.0,sample.serverReported()?"Server":"Manual",sample.label());
                addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(text,w-12)),b->{choose.accept(sample);onClose();})
                        .bounds(x,58+(i%count)*24,w,20).build());
            }
            if(pages>1){
                addRenderableWidget(Button.builder(Component.literal("<"),b->{page=(page-1+pages)%pages;clearWidgets();init();}).bounds(width/2-100,height-54,60,20).build());
                addRenderableWidget(Button.builder(Component.literal(">"),b->{page=(page+1)%pages;clearWidgets();init();}).bounds(width/2+40,height-54,60,20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(width/2-100,height-28,200,20).build());
        }
        @Override public void onClose(){minecraft.gui.setScreen(parent);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){super.extractRenderState(g,x,y,tick);
            g.centeredText(font,title,width/2,12,0xFFFFFFFF);
            g.centeredText(font,CooldownObservations.samples().isEmpty()?"No samples yet. Use L or receive a server cooldown.":"Choose only a duration that matches this effect.",width/2,32,0xFFCCCCCC);}
    }
}
