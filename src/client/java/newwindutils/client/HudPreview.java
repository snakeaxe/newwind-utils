package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Sample-only drawing. This class never writes to cooldown tracking state. */
public final class HudPreview {
    private HudPreview() {}
    public interface Editing {}
    public static boolean isEditing(Screen screen) { return screen instanceof Editing; }
    static final class State {
        boolean animated;
        private long start=System.nanoTime();
        void toggle() { animated=!animated;start=System.nanoTime(); }
        List<ActiveEffectTimer> timers() {
            long elapsed=animated?(System.nanoTime()-start)/50_000_000L:0;
            return sampleTimers(elapsed,animated);
        }
    }
    static List<ActiveEffectTimer> sampleTimers(long elapsed,boolean animated) {
        String[] ids={"bind","elytra_bind","gapple_bind"};
        String[] names={"Bind","Elytra","Gapple"};
        int[] durations={200,160,300},frozen={170,98,244};
        var result=new java.util.ArrayList<ActiveEffectTimer>();
        for(int i=0;i<ids.length;i++) {
            int remaining=animated?durations[i]-(int)Math.floorMod(elapsed,durations[i]):frozen[i];
            result.add(new ActiveEffectTimer(ids[i],names[i],durations[i],remaining,null));
        }
        return List.copyOf(result);
    }
    static void drawPanel(GuiGraphicsExtractor g,Minecraft client,int x,int y,int width,int height,
                          NewwindUtilsConfig config,boolean self,State state) {
        g.fill(x,y,x+width,y+height,0xDF0A111B);
        g.outline(x,y,width,height,0xFF40526A);
        String heading=self?"Self HUD preview":"Live preview - sample timers";
        g.centeredText(client.font,client.font.plainSubstrByWidth(heading,width-10),x+width/2,y+7,0xFFE4EEFF);
        boolean compact=height<150;
        if(!compact) g.centeredText(client.font,"Fitted to panel",x+width/2,y+21,0xFF9FAFC6);
        int top=y+(compact?21:36),available=Math.max(5,height-(compact?54:69));
        g.enableScissor(x+3,top,x+width-3,top+available);
        List<ActiveEffectTimer> timers=previewTimers(state.timers(),config,self);
        if(self) {
            var size=NewwindUtilsSelfHud.measure(client,timers,0,config);
            float scale=fit(config.hudScale,size.width(),size.height(),width-14,available-4);
            var pose=g.pose();pose.pushMatrix();
            pose.translate(x+(width-size.width()*scale)/2,top+(available-size.height()*scale)/2);
            pose.scale(scale,scale);
            NewwindUtilsSelfHud.drawPanel(g,client,timers,0,config,0,0);
            pose.popMatrix();
        } else if(config.targetHudMode==NewwindUtilsConfig.TargetHudMode.SCREEN) {
            var size=NewwindUtilsHud.measureCard(client,"ExamplePlayer",timers,0,config);
            int count=Math.min(2,config.maxTargetsShown);
            int totalHeight=size.height()*count+config.targetCardSpacing*(count-1);
            float scale=fit(config.hudScale,size.width(),totalHeight,width-14,available-4);
            var pose=g.pose();pose.pushMatrix();
            pose.translate(x+(width-size.width()*scale)/2,top+(available-totalHeight*scale)/2);
            pose.scale(scale,scale);
            for(int i=0;i<count;i++) NewwindUtilsHud.renderSample(g,client,0,i*(size.height()+config.targetCardSpacing),0,config,timers);
            pose.popMatrix();
        } else drawNametagScene(g,client,x+7,top,width-14,available,config,timers);
        g.disableScissor();
    }
    static List<ActiveEffectTimer> previewTimers(List<ActiveEffectTimer> timers,NewwindUtilsConfig config,boolean self){
        return self?SelfEffectSummary.preview(timers,config):timers.stream().filter(t->EffectKind.ofEffect(t.effectId()).visible(config,false)).toList();
    }
    static float fit(float desired,float contentWidth,float contentHeight,float width,float height) {
        return Math.max(0.01F,Math.min(desired,Math.min(Math.max(1,width)/Math.max(1,contentWidth),
                Math.max(1,height)/Math.max(1,contentHeight))));
    }
    private static void drawNametagScene(GuiGraphicsExtractor g,Minecraft client,int x,int y,int width,int height,
                                         NewwindUtilsConfig config,List<ActiveEffectTimer> timers) {
        var frame=NametagHud.frame(timers,0,config);
        var font=client.font;
        float textScale=frame.style().scale();
        int lineStep=font.lineHeight+frame.style().spacing();
        float maxWidth=Math.max(font.width("ExamplePlayer"),font.width("Cosmetic Title"));
        for(var line:frame.lines()) maxWidth=Math.max(maxWidth,font.width(line)*textScale);
        float sceneWidth=maxWidth+16;
        float topDistance=(float)frame.height()*40+Math.max(0,frame.lines().size()-1)*lineStep*textScale;
        float nameY=topDistance+font.lineHeight*textScale+8;
        float sceneHeight=nameY+58;
        float zoom=fit(1.5F,sceneWidth,sceneHeight,width,height);
        var pose=g.pose();pose.pushMatrix();
        pose.translate(x+(width-sceneWidth*zoom)/2,y+(height-sceneHeight*zoom)/2);
        pose.scale(zoom,zoom);
        int center=Math.round(sceneWidth/2);
        g.centeredText(font,"Cosmetic Title",center,Math.round(nameY)-12,0xFFFFD447);
        g.centeredText(font,"ExamplePlayer",center,Math.round(nameY),0xFFFFFFFF);
        int headY=Math.round(nameY)+14;
        if(client.player!=null) NewwindUtilsHud.drawPlayerHead(g,client.player,center-9,headY,18);
        else g.fill(center-9,headY,center+9,headY+18,0xFFE6B68A);
        g.fill(center-9,headY+19,center+9,headY+40,0xFF377AA5);
        g.fill(center-15,headY+19,center-10,headY+35,0xFF65A4C7);
        g.fill(center+10,headY+19,center+15,headY+35,0xFF65A4C7);
        pose.pushMatrix();
        pose.translate(center,nameY-(float)frame.height()*40);
        pose.scale(textScale,textScale);
        for(int i=0;i<frame.lines().size();i++) {
            var line=frame.lines().get(i);
            int half=font.width(line)/2;
            int lineY=-i*lineStep;
            if(frame.style().background()!=0) g.fill(-half-2,lineY-1,half+3,lineY+font.lineHeight+1,frame.style().background());
            g.text(font,line,-half,lineY,0xFFFFFFFF,frame.style().shadow());
        }
        pose.popMatrix();pose.popMatrix();
    }
    static final class FullScreen extends Screen implements Editing {
        private final Screen parent;
        private final boolean self;
        private final State state;
        FullScreen(Screen parent,boolean self,State state) {
            super(Component.literal("Sample HUD preview"));this.parent=parent;this.self=self;this.state=state;
        }
        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.literal("Back to editor"),b->onClose())
                    .bounds(width/2-154,height-28,150,20).build());
            addRenderableWidget(Button.builder(Component.literal("Animate: "+(state.animated?"ON":"OFF")),b->{
                state.toggle();clearWidgets();init();}).bounds(width/2+4,height-28,150,20).build());
        }
        @Override public void onClose(){minecraft.gui.setScreen(parent);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partialTick) {
            var config=NewwindUtilsConfigManager.get();
            var timers=previewTimers(state.timers(),config,self);
            if(self) NewwindUtilsSelfHud.renderSampleOnScreen(g,minecraft,timers,0,config);
            else if(config.targetHudMode==NewwindUtilsConfig.TargetHudMode.SCREEN) {
                var pose=g.pose();pose.pushMatrix();pose.scale(config.hudScale,config.hudScale);
                int cardY=Math.round(config.hudY/config.hudScale);
                for(int i=0;i<Math.min(2,config.maxTargetsShown);i++) {
                    var target=new EffectTimerManager.FocusedTarget(null,"ExamplePlayer",timers);
                    cardY+=NewwindUtilsHud.renderTargetCard(g,minecraft,target,Math.round(config.hudX/config.hudScale),cardY,0,config)+config.targetCardSpacing;
                }
                pose.popMatrix();
            } else drawNametagScene(g,minecraft,12,42,width-24,height-80,config,timers);
            g.fill(0,0,width,35,0xBD08111C);
            g.centeredText(font,"Sample timers only - tracking is unchanged",width/2,6,0xFFFFFFFF);
            String caption=!self && config.targetHudMode==NewwindUtilsConfig.TargetHudMode.NAMETAG
                    ?"Illustrative player; world size varies with distance."
                    :"Sample panels at your configured screen positions.";
            g.centeredText(font,caption,width/2,21,0xFFBBBBBB);
            if(!self && config.targetHudMode==NewwindUtilsConfig.TargetHudMode.SCREEN
                    && (config.hudX>=width || config.hudY>=height))
                g.centeredText(font,"HUD X/Y is off screen. Lower it in the editor.",width/2,height/2,0xFFFF9999);
            super.extractRenderState(g,x,y,partialTick);
        }
    }
}
