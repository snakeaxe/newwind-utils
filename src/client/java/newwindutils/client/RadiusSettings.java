package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.*;

public final class RadiusSettings {
    private RadiusSettings(){}
    public static void open(Screen parent,ItemCatalog.Entry entry){
        var config=NewwindUtilsConfigManager.get();
        if(entry.id().equals("heliast")){
            Minecraft.getInstance().gui.setScreen(new SimpleSettingsScreen(parent,"Heliast landed pools","After impact only; uses the actual cloud location and radius.",()->List.of(
                SimpleSettingsScreen.action(()->"Landed pool outlines: "+(config.showDragonBreathClouds?"ON":"OFF"),()->config.showDragonBreathClouds=!config.showDragonBreathClouds)
            )));return;
        }
        RadiusRule rule=config.radiusRules.stream().filter(r->r.itemId.equals(entry.id())).findFirst().orElse(null);
        if(rule==null){rule=new RadiusRule();rule.itemId=entry.id();
            if(entry.materialId().matches(".*(helmet|chestplate|leggings|boots)$"))rule.source=RadiusRule.Source.WORN;
            config.radiusRules.add(rule);
        }
        final RadiusRule r=rule;
        Minecraft.getInstance().gui.setScreen(new SimpleSettingsScreen(parent,entry.name()+" radius","0 = unknown. HELD = while held; ALWAYS = persistent feet guide.",()->List.of(
                SimpleSettingsScreen.action(()->"Enabled: "+(r.enabled?"ON":"OFF"),()->r.enabled=!r.enabled),
                SimpleSettingsScreen.number("Base radius (blocks)",()->r.baseRadius,v->r.baseRadius=v,0,64,0.25),
                SimpleSettingsScreen.number("Upgraded radius (blocks)",()->r.upgradedRadius,v->r.upgradedRadius=v,0,64,0.25),
                SimpleSettingsScreen.action(()->"Variant: "+r.variant,()->r.variant=RadiusRule.Variant.values()[(r.variant.ordinal()+1)%RadiusRule.Variant.values().length]),
                SimpleSettingsScreen.action(()->"Source: "+r.source,()->r.source=RadiusRule.Source.values()[(r.source.ordinal()+1)%RadiusRule.Source.values().length])
        )));
    }
    public static void openGeneral(Screen parent){
        var c=NewwindUtilsConfigManager.get();
        Minecraft.getInstance().gui.setScreen(new SimpleSettingsScreen(parent,"Effect radius guides","Set each item's tested radius in the item browser.",()->{
            List<SimpleSettingsScreen.Row> rows=new ArrayList<>();
            rows.add(SimpleSettingsScreen.action(()->"Radius guides: "+(c.showRadiusGuides?"ON":"OFF"),()->c.showRadiusGuides=!c.showRadiusGuides));
            rows.add(SimpleSettingsScreen.action(()->"Heliast landed pool outlines: "+(c.showDragonBreathClouds?"ON":"OFF"),()->c.showDragonBreathClouds=!c.showDragonBreathClouds));
            rows.add(SimpleSettingsScreen.number("Line red",()->c.radiusRed,v->c.radiusRed=(int)Math.round(v),0,255,1));
            rows.add(SimpleSettingsScreen.number("Line green",()->c.radiusGreen,v->c.radiusGreen=(int)Math.round(v),0,255,1));
            rows.add(SimpleSettingsScreen.number("Line blue",()->c.radiusBlue,v->c.radiusBlue=(int)Math.round(v),0,255,1));
            rows.add(SimpleSettingsScreen.number("Line opacity",()->c.radiusOpacity,v->c.radiusOpacity=(int)Math.round(v),0,255,1));
            rows.add(SimpleSettingsScreen.number("Line width",()->c.radiusLineWidth,v->c.radiusLineWidth=(float)v,1,6,0.25));
            rows.add(SimpleSettingsScreen.action(()->"Open item browser / configure radius",()->Minecraft.getInstance().gui.setScreen(new ItemBrowserScreen(Minecraft.getInstance().gui.screen()))));
            for(RadiusRule r:c.radiusRules){var entry=ItemCatalog.byId(r.itemId);if(entry!=null)rows.add(SimpleSettingsScreen.action(()->entry.name()+": "+(r.enabled?"ON":"OFF"),()->open(Minecraft.getInstance().gui.screen(),entry)));}
            return rows;
        }));
    }
}
