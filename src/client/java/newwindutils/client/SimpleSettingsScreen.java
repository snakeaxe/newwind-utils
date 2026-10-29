package newwindutils.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.*;

/** Shared paged settings controls for price and radius tools. */
class SimpleSettingsScreen extends Screen {
    record Row(Supplier<String> label,Runnable action,NewwindUtilsConfigScreen.NumericSetting number){}
    static Row number(String label,DoubleSupplier getter,DoubleConsumer setter,double min,double max,double step){
        return new Row(()->label,null,new NewwindUtilsConfigScreen.NumericSetting(label,getter,setter,min,max,step));
    }
    static Row action(Supplier<String> label,Runnable action){return new Row(label,action,null);}
    private final Screen parent;
    private final String subtitle;
    private final Supplier<List<Row>> rows;
    private int page;
    SimpleSettingsScreen(Screen parent,String title,String subtitle,Supplier<List<Row>> rows){
        super(Component.literal(title));this.parent=parent;this.subtitle=subtitle;this.rows=rows;
    }
    @Override protected void init(){
        List<Row> values=rows.get();int count=Math.max(1,(height-112)/24),pages=Math.max(1,(values.size()+count-1)/count);
        page=Math.max(0,Math.min(page,pages-1));int w=Math.min(360,width-24),x=(width-w)/2;
        for(int i=page*count;i<Math.min(values.size(),(page+1)*count);i++){
            Row row=values.get(i);int y=50+(i%count)*24;
            if(row.number()!=null)addRenderableWidget(new NewwindUtilsConfigScreen.SettingSlider(this,x,y,w,row.number()));
            else addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(row.label().get(),w-10)),b->{row.action().run();persist();clearWidgets();init();}).bounds(x,y,w,20).build());
        }
        if(pages>1){
            addRenderableWidget(Button.builder(Component.literal("<"),b->{page=(page-1+pages)%pages;clearWidgets();init();}).bounds(width/2-100,height-54,60,20).build());
            Button label=addRenderableWidget(Button.builder(Component.literal((page+1)+" / "+pages),b->{}).bounds(width/2-36,height-54,72,20).build());label.active=false;
            addRenderableWidget(Button.builder(Component.literal(">"),b->{page=(page+1)%pages;clearWidgets();init();}).bounds(width/2+40,height-54,60,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-100,height-28,200,20).build());
    }
    static void persist(){NewwindUtilsConfigManager.save();NewwindUtilsClient.refreshWeaponRegistry();}
    @Override public void onClose(){persist();minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partialTick){
        super.extractRenderState(g,x,y,partialTick);g.centeredText(font,title,width/2,12,0xFFFFFFFF);
        g.centeredText(font,font.plainSubstrByWidth(subtitle,width-20),width/2,31,0xFFBBBBBB);
    }
}
