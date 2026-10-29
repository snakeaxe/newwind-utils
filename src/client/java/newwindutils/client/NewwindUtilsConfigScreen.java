package newwindutils.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class NewwindUtilsConfigScreen extends Screen implements HudPreview.Editing {
    private static final int ROW_HEIGHT = 24;
    private static final int TOP = 46;
    private static final int BOTTOM_RESERVED = 62;

    private final Screen parent;
    private int page = 0;
    private final boolean selfPage;
    private final HudPreview.State preview = new HudPreview.State();
    private EditorLayout editorLayout;

    public NewwindUtilsConfigScreen(Screen parent) { this(parent,false); }
    private NewwindUtilsConfigScreen(Screen parent,boolean selfPage) {
        super(Component.literal(selfPage?"NewwindUTILS - Self HUD":"NewwindUTILS"));
        this.parent=parent;
        this.selfPage=selfPage;
    }
    record EditorLayout(int controlsX,int controlsWidth,int controlsTop,int rows,
                        int previewX,int previewY,int previewWidth,int previewHeight) {}
    static EditorLayout layoutFor(int width,int height) {
        boolean side=width>=440 || height<220;
        int cw=side?Math.min(240,(width-36)/2):width-24;
        int px=side?12+cw+12:12;
        int py=side?42:68;
        int pw=side?width-px-12:width-24;
        int ph=side?Math.max(64,height-108):Math.max(64,Math.min(90,height/3));
        int top=side?70:py+ph+8;
        int rows=Math.max(1,(height-64-top)/24);
        return new EditorLayout(12,cw,top,rows,px,py,pw,ph);
    }

    @Override
    protected void init() {
        super.init();
        NewwindUtilsConfig config=NewwindUtilsConfigManager.get();
        editorLayout=layoutFor(width,height);
        EditorLayout l=editorLayout;
        // The mode selector is always the first control, outside pagination.
        if(selfPage) addPlainButton("< Target HUD settings",l.controlsX(),42,l.controlsWidth(),this::onClose);
        else addPlainButton("Target HUD: "+config.targetHudMode.label(),l.controlsX(),42,l.controlsWidth(),()->{
            config.targetHudMode=config.targetHudMode.next();page=0;persistAndRefresh();rebuild();
        });
        List<Entry> entries=buildEntries(config);
        int pages=Math.max(1,(entries.size()+l.rows()-1)/l.rows());
        page=Math.max(0,Math.min(page,pages-1));
        for(int i=page*l.rows();i<Math.min(entries.size(),(page+1)*l.rows());i++) {
            Entry entry=entries.get(i);
            int y=l.controlsTop()+(i%l.rows())*24;
            if(entry.number()!=null) addRenderableWidget(new SettingSlider(this,l.controlsX(),y,l.controlsWidth(),entry.number()));
            else addPlainButton(entry.label().get(),l.controlsX(),y,l.controlsWidth(),()->{
                entry.forward().run();persistAndRefresh();rebuild();
            });
        }
        if(pages>1) {
            int bw=(l.controlsWidth()-8)/3;
            addPlainButton("< Prev",l.controlsX(),height-54,bw,()->{page=(page-1+pages)%pages;rebuild();});
            Button label=addPlainButton((page+1)+" / "+pages,l.controlsX()+bw+4,height-54,bw,()->{});label.active=false;
            addPlainButton("Next >",l.controlsX()+2*(bw+4),height-54,bw,()->{page=(page+1)%pages;rebuild();});
        }
        int pw=(l.previewWidth()-18)/2;
        addPlainButton("Full screen",l.previewX()+6,l.previewY()+l.previewHeight()-26,pw,
                ()->minecraft.gui.setScreen(new HudPreview.FullScreen(this,selfPage,preview)));
        addPlainButton("Animate: "+onOff(preview.animated),l.previewX()+12+pw,l.previewY()+l.previewHeight()-26,pw,
                ()->{preview.toggle();rebuild();});
        int bw=(width-32)/3;
        addPlainButton("Durations",12,height-28,bw,()->minecraft.gui.setScreen(new DurationSettingsScreen(this)));
        addPlainButton("Reset look",16+bw,height-28,bw,()->{resetAppearance(config,selfPage);persistAndRefresh();rebuild();});
        addPlainButton("Done",20+2*bw,height-28,bw,this::onClose);
    }

    private List<Entry> buildEntries(NewwindUtilsConfig c) {
        return buildEntries(c,selfPage,
                ()->minecraft.gui.setScreen(new NewwindUtilsConfigScreen(this,true)),
                ()->minecraft.gui.setScreen(new CustomItemsScreen(this)));
    }
    static List<Entry> buildEntries(NewwindUtilsConfig c,boolean selfPage,Runnable openSelf,Runnable openItems) {
        List<Entry> list=new ArrayList<>();
        if(selfPage) {
            list.add(toggle(()->"Show Self HUD: "+onOff(c.showSelfHud),()->c.showSelfHud=!c.showSelfHud));
            list.add(number("Self HUD X",()->c.selfHudX,v->c.selfHudX=(int)Math.round(v),-1,5000,1));
            list.add(number("Self HUD Y",()->c.selfHudY,v->c.selfHudY=(int)Math.round(v),0,5000,1));
            list.add(number("HUD Scale",()->c.hudScale,v->c.hudScale=(float)v,0.5,3,0.05));
            addEffectToggles(list,c,true);
            addPanelColors(list,c);
            list.add(toggle(()->"Raw cooldown details: "+onOff(c.showRawSelfCooldowns),()->c.showRawSelfCooldowns=!c.showRawSelfCooldowns));
            list.add(toggle(()->"Server Cooldowns: "+onOff(c.showServerCooldowns),()->c.showServerCooldowns=!c.showServerCooldowns));
            return list;
        }
        if(c.targetHudMode==NewwindUtilsConfig.TargetHudMode.SCREEN) {
            list.add(number("HUD X",()->c.hudX,v->c.hudX=(int)Math.round(v),0,5000,1));
            list.add(number("HUD Y",()->c.hudY,v->c.hudY=(int)Math.round(v),0,5000,1));
            list.add(number("HUD Scale",()->c.hudScale,v->c.hudScale=(float)v,0.5,3,0.05));
            list.add(number("Name Scale",()->c.nameScale,v->c.nameScale=(float)v,0.75,3,0.05));
            list.add(toggle(()->"Show Heads: "+onOff(c.showPlayerHeads),()->c.showPlayerHeads=!c.showPlayerHeads));
            if(c.showPlayerHeads) list.add(number("Head Size",()->c.headSize,v->c.headSize=(int)Math.round(v),12,48,1));
            list.add(number("Card Spacing",()->c.targetCardSpacing,v->c.targetCardSpacing=(int)Math.round(v),0,30,1));
            list.add(toggle(()->"Display Mode: "+c.timerDisplayMode.label(),()->c.timerDisplayMode=c.timerDisplayMode.next()));
            if(c.timerDisplayMode.showsBar()) {
                list.add(number("Bar Width",()->c.barWidth,v->c.barWidth=(int)Math.round(v),40,300,1));
                list.add(number("Bar Height",()->c.barHeight,v->c.barHeight=(int)Math.round(v),2,20,1));
                list.add(toggle(()->"Time-Based Bar Colors: "+onOff(c.colorBarsByRemainingTime),()->c.colorBarsByRemainingTime=!c.colorBarsByRemainingTime));
            }
            addPanelColors(list,c);
        } else {
            list.add(number("Nametag Height",()->c.nametagHeight,v->c.nametagHeight=v,0.05,8,0.05));
            list.add(number("Nametag Size",()->c.nametagScale,v->c.nametagScale=(float)v,0.5,3,0.05));
            list.add(toggle(()->"Layout: "+c.nametagLayout.label(),()->c.nametagLayout=c.nametagLayout.next()));
            if(c.nametagLayout==NewwindUtilsConfig.NametagLayout.STACKED)
                list.add(number("Line Spacing",()->c.nametagLineSpacing,v->c.nametagLineSpacing=(int)Math.round(v),0,12,1));
            list.add(number("Decimal Places",()->c.nametagDecimals,v->c.nametagDecimals=(int)Math.round(v),0,2,1));
            list.add(toggle(()->"Time-Based Text Colors: "+onOff(c.nametagTimeColors),()->c.nametagTimeColors=!c.nametagTimeColors));
            if(!c.nametagTimeColors) {
                list.add(toggle(()->"Text Color: "+c.nametagColor.label(),()->c.nametagColor=c.nametagColor.next()));
                if(c.nametagColor==NewwindUtilsConfig.NametagColor.CUSTOM) {
                    list.add(number("Text Red",()->c.nametagColorR,v->c.nametagColorR=(int)Math.round(v),0,255,1));
                    list.add(number("Text Green",()->c.nametagColorG,v->c.nametagColorG=(int)Math.round(v),0,255,1));
                    list.add(number("Text Blue",()->c.nametagColorB,v->c.nametagColorB=(int)Math.round(v),0,255,1));
                }
            }
            list.add(toggle(()->"Bold Text: "+onOff(c.nametagBold),()->c.nametagBold=!c.nametagBold));
            list.add(toggle(()->"Text Shadow: "+onOff(c.nametagShadow),()->c.nametagShadow=!c.nametagShadow));
            list.add(toggle(()->"Background: "+onOff(c.nametagBackground),()->c.nametagBackground=!c.nametagBackground));
            if(c.nametagBackground) list.add(number("Background Opacity",()->c.nametagBackgroundOpacity,v->c.nametagBackgroundOpacity=(int)Math.round(v),0,255,1));
        }
        addEffectToggles(list,c,false);
        list.add(toggle(()->"Self HUD settings",openSelf));
        list.add(toggle(()->"Custom Items / Measurements",openItems));
        list.add(toggle(()->"God Item Browser / Costs (B)",()->net.minecraft.client.Minecraft.getInstance().gui.setScreen(new ItemBrowserScreen(net.minecraft.client.Minecraft.getInstance().gui.screen()))));
        list.add(toggle(()->"Effect Radius Guides",()->RadiusSettings.openGeneral(net.minecraft.client.Minecraft.getInstance().gui.screen())));
        list.add(number("Max Players Shown",()->c.maxTargetsShown,v->c.maxTargetsShown=(int)Math.round(v),1,8,1));
        list.add(toggle(()->"Only Yours On Others: "+onOff(c.showOnlyLocalInflictedOnTargets),()->c.showOnlyLocalInflictedOnTargets=!c.showOnlyLocalInflictedOnTargets));
        list.add(toggle(()->"Only Self Effects: "+onOff(c.showOnlySelfTimers),()->c.showOnlySelfTimers=!c.showOnlySelfTimers));
        list.add(number("Focus Distance",()->c.focusMaxDistance,v->c.focusMaxDistance=v,8,128,1));
        list.add(number("Focus Cone",()->c.focusMinDot,v->c.focusMinDot=v,0.15,0.95,0.01));
        list.add(number("Distance Weight",()->c.focusDistanceWeight,v->c.focusDistanceWeight=v,0,2,0.01));
        return list;
    }
    private static void addEffectToggles(List<Entry> list,NewwindUtilsConfig c,boolean self){
        for(EffectKind kind:EffectKind.values())list.add(toggle(()->"Show "+kind.label+": "+onOff(kind.visible(c,self)),()->{
            var hidden=self?c.hiddenSelfEffects:c.hiddenTargetEffects;
            if(!hidden.remove(kind.id))hidden.add(kind.id);
        }));
    }
    private static Entry toggle(LabelSupplier label,Runnable action) { return new Entry(label,action,null); }
    private static void addPanelColors(List<Entry> list,NewwindUtilsConfig c) {
        list.add(toggle(()->"Color Preset: "+c.colorPreset.label(),()->c.colorPreset=c.colorPreset.next()));
        if(c.colorPreset==NewwindUtilsConfig.HudColorPreset.CUSTOM) {
            list.add(number("Custom Red",()->c.customColorR,v->c.customColorR=(int)Math.round(v),0,255,1));
            list.add(number("Custom Green",()->c.customColorG,v->c.customColorG=(int)Math.round(v),0,255,1));
            list.add(number("Custom Blue",()->c.customColorB,v->c.customColorB=(int)Math.round(v),0,255,1));
        }
        list.add(toggle(()->"Outline: "+onOff(c.showOutline),()->c.showOutline=!c.showOutline));
        list.add(toggle(()->"Text Shadow: "+onOff(c.shadowText),()->c.shadowText=!c.shadowText));
    }
    static void resetAppearance(NewwindUtilsConfig c,boolean selfPage) {
        NewwindUtilsConfig d=new NewwindUtilsConfig();
        if(!selfPage && c.targetHudMode==NewwindUtilsConfig.TargetHudMode.NAMETAG) {
            c.nametagHeight=d.nametagHeight;c.nametagScale=d.nametagScale;c.nametagLineSpacing=d.nametagLineSpacing;
            c.nametagDecimals=d.nametagDecimals;c.nametagShadow=d.nametagShadow;c.nametagBold=d.nametagBold;
            c.nametagBackground=d.nametagBackground;c.nametagBackgroundOpacity=d.nametagBackgroundOpacity;
            c.nametagTimeColors=d.nametagTimeColors;c.nametagLayout=d.nametagLayout;c.nametagColor=d.nametagColor;
            c.nametagColorR=d.nametagColorR;c.nametagColorG=d.nametagColorG;c.nametagColorB=d.nametagColorB;
        } else {
            c.hudScale=d.hudScale;c.timerDisplayMode=d.timerDisplayMode;c.colorPreset=d.colorPreset;
            c.customColorR=d.customColorR;c.customColorG=d.customColorG;c.customColorB=d.customColorB;
            c.showOutline=d.showOutline;c.shadowText=d.shadowText;
            if(selfPage) {c.selfHudX=d.selfHudX;c.selfHudY=d.selfHudY;c.showSelfHud=d.showSelfHud;}
            else {c.hudX=d.hudX;c.hudY=d.hudY;c.nameScale=d.nameScale;c.headSize=d.headSize;
                c.showPlayerHeads=d.showPlayerHeads;c.targetCardSpacing=d.targetCardSpacing;c.barWidth=d.barWidth;
                c.barHeight=d.barHeight;c.colorBarsByRemainingTime=d.colorBarsByRemainingTime;}
        }
    }

    private Button addPlainButton(String text, int x, int y, int width, Runnable action) {
        Button button = Button.builder(
                Component.literal(text),
                b -> action.run()
        ).bounds(x, y, width, 20).build();
        this.addRenderableWidget(button);
        return button;
    }

    private static void persistAndRefresh() {
        NewwindUtilsConfigManager.get().clamp();
        NewwindUtilsConfigManager.save();
        NewwindUtilsClient.refreshWeaponRegistry();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        if(editorLayout!=null) {
            var l=editorLayout;
            HudPreview.drawPanel(graphics,minecraft,l.previewX(),l.previewY(),l.previewWidth(),l.previewHeight(),
                    NewwindUtilsConfigManager.get(),selfPage,preview);
        }
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
        graphics.centeredText(font,title,width/2,12,0xFFFFFFFF);
        graphics.centeredText(font,"Drag sliders; double-click for an exact value",width/2,27,0xFFBBBBBB);
    }

    @FunctionalInterface
    interface LabelSupplier {
        String get();
    }

    private static Entry number(String label, DoubleSupplier getter, DoubleConsumer setter,
                                double min, double max, double step) {
        return new Entry(() -> label, null, new NumericSetting(label, getter, setter, min, max, step));
    }

    record NumericSetting(String label, DoubleSupplier getter, DoubleConsumer setter,
                                  double min, double max, double step) {
        double normalize(double value) {
            return Math.max(min, Math.min(max, Math.round(value / step) * step));
        }
        String formatted() {
            double number = getter.getAsDouble();
            if (label.equals("Self HUD X") && number == -1) return "Right edge";
            return step >= 1 ? Long.toString(Math.round(number)) : String.format(Locale.US, "%.2f", number);
        }
    }

    static class SettingSlider extends AbstractSliderButton {
        private final Screen parent;
        private final NumericSetting setting;
        private double beforeClick;
        SettingSlider(Screen parent, int x, int y, int width, NumericSetting setting) {
            super(x, y, width, 20, Component.empty(),
                    (setting.getter().getAsDouble() - setting.min()) / (setting.max() - setting.min()));
            this.parent = parent;
            this.setting = setting;
            updateMessage();
        }
        @Override protected void updateMessage() {
            if (setting != null) setMessage(Component.literal(setting.label() + ": " + setting.formatted()));
        }
        @Override protected void applyValue() {
            setting.setter().accept(setting.normalize(setting.min() + value * (setting.max() - setting.min())));
        }
        @Override public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (doubleClick) {
                setting.setter().accept(beforeClick);
                persistAndRefresh();
                net.minecraft.client.Minecraft.getInstance().gui.setScreen(new NumberInputScreen(parent, setting));
                return;
            }
            beforeClick = setting.getter().getAsDouble();
            super.onClick(event, false);
        }
        @Override public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            persistAndRefresh();
        }
        @Override public boolean keyPressed(KeyEvent event) {
            boolean handled = super.keyPressed(event);
            if (handled) persistAndRefresh();
            return handled;
        }
    }

    private static class NumberInputScreen extends Screen implements HudPreview.Editing {
        private final Screen parent;
        private final NumericSetting setting;
        private EditBox input;
        private String error = "";
        NumberInputScreen(Screen parent, NumericSetting setting) {
            super(Component.literal(setting.label()));
            this.parent = parent;
            this.setting = setting;
        }
        @Override protected void init() {
            input = new EditBox(font, width / 2 - 100, height / 2 - 25, 200, 20, title);
            input.setMaxLength(32);
            input.setValue(Double.toString(setting.getter().getAsDouble()));
            addRenderableWidget(input);
            setInitialFocus(input);
            addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
                try {
                    double number = Double.parseDouble(input.getValue().trim());
                    if (!Double.isFinite(number) || number < setting.min() || number > setting.max()) {
                        error = "Enter a number from " + setting.min() + " to " + setting.max();
                        return;
                    }
                    setting.setter().accept(setting.normalize(number));
                    persistAndRefresh();
                    onClose();
                } catch (NumberFormatException e) { error = "Enter a valid number"; }
            }).bounds(width / 2 - 100, height / 2 + 5, 96, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds(width / 2 + 4, height / 2 + 5, 96, 20).build());
        }
        @Override public void onClose() { minecraft.gui.setScreen(parent); }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
            super.extractRenderState(graphics, x, y, partialTick);
            graphics.centeredText(font, title, width / 2, height / 2 - 55, 0xFFFFFFFF);
            graphics.centeredText(font, error, width / 2, height / 2 + 35, 0xFFFF7777);
        }
    }

    record Entry(LabelSupplier label, Runnable forward, NumericSetting number) {
    }

    private record DurationEntry(String name, IntSupplier getter, IntConsumer setter) {
    }

    private static class DurationSettingsScreen extends Screen {
        private static final int ROW_HEIGHT = 24;
        private static final int TOP = 46;
        private static final int BOTTOM_RESERVED = 62;

        private final Screen parent;
        private int page = 0;

        protected DurationSettingsScreen(Screen parent) {
            super(Component.literal("Effect Durations"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();

            NewwindUtilsConfig config = NewwindUtilsConfigManager.get();

            List<DurationEntry> entries = new ArrayList<>();
            entries.add(new DurationEntry("Stasis", () -> config.stasisDurationTicks, value -> config.stasisDurationTicks = value));
            entries.add(new DurationEntry("Stasis+", () -> config.stasisUpgradedDurationTicks, value -> config.stasisUpgradedDurationTicks = value));
            entries.add(new DurationEntry("Insipid", () -> config.insipidDurationTicks, value -> config.insipidDurationTicks = value));
            entries.add(new DurationEntry("Insipid+", () -> config.insipidUpgradedDurationTicks, value -> config.insipidUpgradedDurationTicks = value));
            entries.add(new DurationEntry("Insipid All Food", () -> config.insipidBriefFoodDurationTicks, value -> config.insipidBriefFoodDurationTicks = value));
            entries.add(new DurationEntry("Braxlion", () -> config.braxlionDurationTicks, value -> config.braxlionDurationTicks = value));
            entries.add(new DurationEntry("Void Leggings", () -> config.voidLeggingsDurationTicks, value -> config.voidLeggingsDurationTicks = value));
            entries.add(new DurationEntry("Void Leggings+", () -> config.voidLeggingsUpgradedDurationTicks, value -> config.voidLeggingsUpgradedDurationTicks = value));
            entries.add(new DurationEntry("Easter Leggings Bind", () -> config.easterLeggingsBindDurationTicks, value -> config.easterLeggingsBindDurationTicks = value));
            entries.add(new DurationEntry("Easter Leggings Elytra", () -> config.easterLeggingsClipDurationTicks, value -> config.easterLeggingsClipDurationTicks = value));
            entries.add(new DurationEntry("Acropsis", () -> config.acropsisDurationTicks, value -> config.acropsisDurationTicks = value));
            entries.add(new DurationEntry("Acropsis+", () -> config.acropsisUpgradedDurationTicks, value -> config.acropsisUpgradedDurationTicks = value));

            int rowsPerPage = Math.max(1, (this.height - TOP - BOTTOM_RESERVED) / ROW_HEIGHT);
            int pageCount = Math.max(1, (entries.size() + rowsPerPage - 1) / rowsPerPage);
            page = Math.max(0, Math.min(page, pageCount - 1));

            int sliderWidth = Math.min(380, this.width - 20);
            int x = (this.width - sliderWidth) / 2;
            int start = page * rowsPerPage;
            int end = Math.min(entries.size(), start + rowsPerPage);

            for (int i = start; i < end; i++) {
                DurationEntry entry = entries.get(i);
                addDurationRow(x, TOP + (i - start) * ROW_HEIGHT, sliderWidth, entry.name(), entry.getter(), entry.setter());
            }

            if (pageCount > 1) {
                int pagerY = this.height - 54;

                addPlainButton("< Prev", this.width / 2 - 100, pagerY, 60, () -> {
                    page = (page - 1 + pageCount) % pageCount;
                    rebuild();
                });

                Button pageLabel = addPlainButton("Page " + (page + 1) + "/" + pageCount, this.width / 2 - 38, pagerY, 76, () -> {
                });
                pageLabel.active = false;

                addPlainButton("Next >", this.width / 2 + 40, pagerY, 60, () -> {
                    page = (page + 1) % pageCount;
                    rebuild();
                });
            }

            addPlainButton("Back", this.width / 2 - 100, this.height - 28, 200, this::onClose);
        }

        private Button addPlainButton(String text, int x, int y, int width, Runnable action) {
            Button button = Button.builder(
                    Component.literal(text),
                    b -> action.run()
            ).bounds(x, y, width, 20).build();
            this.addRenderableWidget(button);
            return button;
        }

        private void addDurationRow(int x, int y, int sliderWidth, String name, IntSupplier getter, IntConsumer setter) {
            addRenderableWidget(new SettingSlider(this, x, y, sliderWidth,
                    new NumericSetting(name + " (seconds)", () -> getter.getAsInt() / 20.0,
                            value -> setter.accept((int) Math.round(value * 20)), 0.05, 300, 0.05)));
        }

        private static void persistAndRefresh() {
            NewwindUtilsConfigManager.get().clamp();
            NewwindUtilsConfigManager.save();
            NewwindUtilsClient.refreshWeaponRegistry();
        }

        private void rebuild() {
            clearWidgets();
            init();
        }

        private static String durationLabel(String name, int ticks) {
            return name + ": " + String.format("%.2fs (%dt)", ticks / 20.0F, ticks);
        }

        @Override
        public void onClose() {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(parent);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);

            graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
            graphics.centeredText(
                    this.font,
                    "Drag sliders; double-click for exact seconds",
                    this.width / 2,
                    28,
                    0xFFAAAAAA
            );
        }
    }
}
