package newwindutils.client;

public class NewwindUtilsConfig {
    private static final int DEFAULT_STASIS_DURATION_TICKS = 10 * 20;
    private static final int DEFAULT_STASIS_UPGRADED_DURATION_TICKS = 15 * 20;
    private static final int DEFAULT_INSIPID_DURATION_TICKS = 10 * 20;
    private static final int DEFAULT_INSIPID_UPGRADED_DURATION_TICKS = 15 * 20;
    private static final int DEFAULT_INSIPID_BRIEF_FOOD_DURATION_TICKS = 2 * 20;
    private static final int DEFAULT_ACROPSIS_DURATION_TICKS = 10 * 20;
    private static final int DEFAULT_ACROPSIS_UPGRADED_DURATION_TICKS = 15 * 20;
    private static final int DEFAULT_VOID_LEGGINGS_DURATION_TICKS = 7 * 20;
    private static final int DEFAULT_VOID_LEGGINGS_UPGRADED_DURATION_TICKS = 14 * 20;
    private static final int DEFAULT_BRAXLION_DURATION_TICKS = 5 * 20;
    private static final int DEFAULT_EASTER_LEGGINGS_BIND_DURATION_TICKS = 10 * 20;
    private static final int DEFAULT_EASTER_LEGGINGS_CLIP_DURATION_TICKS = 10 * 20;

    public double nametagHeight = 0.65;
    public float nametagScale = 1.0F;
    public int nametagLineSpacing = 3;
    public int nametagDecimals = 1;
    public boolean nametagShadow = true;
    public boolean nametagBold = false;
    public boolean nametagBackground = true;
    public int nametagBackgroundOpacity = 64;
    public boolean nametagTimeColors = false;
    public NametagLayout nametagLayout = NametagLayout.STACKED;
    public NametagColor nametagColor = NametagColor.WHITE;
    public int nametagColorR = 255;
    public int nametagColorG = 255;
    public int nametagColorB = 255;
    public boolean showServerCooldowns = true;
    public boolean showRawSelfCooldowns = false;
    public int sharpenEmbersPerLevel=90;
    public int sharpenEggsPerLevel=90;
    public int reinforceCoinsPerLevel=90;
    public int reinforceRemsPerLevel=90;
    public int embersPerEgg=4;
    public int eggsPerStack=64;
    public boolean showRadiusGuides=true;
    public int radiusRed=255,radiusGreen=55,radiusBlue=55,radiusOpacity=210;
    public float radiusLineWidth=2.0F;
    public boolean showDragonBreathClouds=true;
    public java.util.List<CatalogIdentity> catalogIdentities=new java.util.ArrayList<>();
    public java.util.List<RadiusRule> radiusRules=new java.util.ArrayList<>();
    public java.util.Set<String> hiddenSelfEffects = new java.util.HashSet<>(java.util.List.of("other"));
    public java.util.Set<String> hiddenTargetEffects = new java.util.HashSet<>();
    public java.util.List<CustomItemRule> customItems = new java.util.ArrayList<>();

    public int hudX = 10;
    public int hudY = 10;
    public int maxTargetsShown = 3;
    public int targetCardSpacing = 6;

    // -1 anchors the panel to the right edge, independent of the hotbar.
    public int selfHudX = -1;
    public int selfHudY = 10;
    public boolean showSelfHud = true;

    public int headSize = 20;
    public boolean showPlayerHeads = true;
    public float hudScale = 1.0F;
    public float nameScale = 1.35F;

    public TimerDisplayMode timerDisplayMode = TimerDisplayMode.BAR_AND_SECONDS;
    public TargetHudMode targetHudMode = TargetHudMode.SCREEN;
    public int barWidth = 110;
    public int barHeight = 6;

    public boolean colorBarsByRemainingTime = true;
    public boolean showOutline = true;
    public boolean shadowText = true;
    public boolean showOnlyLocalInflictedOnTargets = false;
    public boolean showOnlySelfTimers = false;

    public double focusMaxDistance = 48.0;
    public double focusMinDot = 0.55;
    public double focusDistanceWeight = 0.35;

    public int stasisDurationTicks = DEFAULT_STASIS_DURATION_TICKS;
    public int stasisUpgradedDurationTicks = DEFAULT_STASIS_UPGRADED_DURATION_TICKS;
    public int insipidDurationTicks = DEFAULT_INSIPID_DURATION_TICKS;
    public int insipidUpgradedDurationTicks = DEFAULT_INSIPID_UPGRADED_DURATION_TICKS;
    public int insipidBriefFoodDurationTicks = DEFAULT_INSIPID_BRIEF_FOOD_DURATION_TICKS;
    public int acropsisDurationTicks = DEFAULT_ACROPSIS_DURATION_TICKS;
    public int acropsisUpgradedDurationTicks = DEFAULT_ACROPSIS_UPGRADED_DURATION_TICKS;
    public int voidLeggingsDurationTicks = DEFAULT_VOID_LEGGINGS_DURATION_TICKS;
    public int voidLeggingsUpgradedDurationTicks = DEFAULT_VOID_LEGGINGS_UPGRADED_DURATION_TICKS;
    public int braxlionDurationTicks = DEFAULT_BRAXLION_DURATION_TICKS;
    public int easterLeggingsBindDurationTicks = DEFAULT_EASTER_LEGGINGS_BIND_DURATION_TICKS;
    public int easterLeggingsClipDurationTicks = DEFAULT_EASTER_LEGGINGS_CLIP_DURATION_TICKS;

    // Used when the colour preset is set to "Custom" (each value 0-255)
    public int customColorR = 74;
    public int customColorG = 163;
    public int customColorB = 255;

    public HudColorPreset colorPreset = HudColorPreset.OCEAN;

    public void clamp() {
        sharpenEmbersPerLevel=clampInt(sharpenEmbersPerLevel,0,1000000);
        sharpenEggsPerLevel=clampInt(sharpenEggsPerLevel,0,1000000);
        reinforceCoinsPerLevel=clampInt(reinforceCoinsPerLevel,0,1000000);
        reinforceRemsPerLevel=clampInt(reinforceRemsPerLevel,0,1000000);
        embersPerEgg=clampInt(embersPerEgg,1,1000000);eggsPerStack=clampInt(eggsPerStack,1,1000000);
        radiusRed=clampInt(radiusRed,0,255);radiusGreen=clampInt(radiusGreen,0,255);radiusBlue=clampInt(radiusBlue,0,255);radiusOpacity=clampInt(radiusOpacity,0,255);
        if(!Float.isFinite(radiusLineWidth))radiusLineWidth=2;radiusLineWidth=clampFloat(radiusLineWidth,1,6);
        if(catalogIdentities==null)catalogIdentities=new java.util.ArrayList<>();
        catalogIdentities.removeIf(v->v==null || !v.valid());
        if(radiusRules==null)radiusRules=new java.util.ArrayList<>();
        radiusRules.removeIf(r->r==null || !r.valid());
        if(hiddenSelfEffects==null)hiddenSelfEffects=new java.util.HashSet<>(java.util.List.of("other"));
        if(hiddenTargetEffects==null)hiddenTargetEffects=new java.util.HashSet<>();
        if (!Float.isFinite(nametagScale)) nametagScale = 1.0F;
        nametagScale = clampFloat(nametagScale, 0.5F, 3.0F);
        nametagLineSpacing = clampInt(nametagLineSpacing, 0, 12);
        nametagDecimals = clampInt(nametagDecimals, 0, 2);
        nametagBackgroundOpacity = clampInt(nametagBackgroundOpacity, 0, 255);
        nametagColorR = clampInt(nametagColorR, 0, 255);
        nametagColorG = clampInt(nametagColorG, 0, 255);
        nametagColorB = clampInt(nametagColorB, 0, 255);
        if (nametagColor == null) nametagColor = NametagColor.WHITE;
        if (nametagLayout == null) nametagLayout = NametagLayout.STACKED;
        if (!Double.isFinite(nametagHeight)) nametagHeight = 0.65;
        nametagHeight = clampDouble(nametagHeight, 0.05, 8.0);
        if (customItems == null) customItems = new java.util.ArrayList<>();
        customItems.removeIf(rule -> rule == null || !rule.valid());
        hudX = clampInt(hudX, 0, 5000);
        hudY = clampInt(hudY, 0, 5000);
        selfHudX = clampInt(selfHudX, -1, 5000);
        selfHudY = clampInt(selfHudY, 0, 5000);
        maxTargetsShown = clampInt(maxTargetsShown, 1, 8);
        targetCardSpacing = clampInt(targetCardSpacing, 0, 30);

        headSize = clampInt(headSize, 12, 48);
        hudScale = clampFloat(hudScale, 0.50F, 3.0F);
        nameScale = clampFloat(nameScale, 0.75F, 3.0F);

        barWidth = clampInt(barWidth, 40, 300);
        barHeight = clampInt(barHeight, 2, 20);

        focusMaxDistance = clampDouble(focusMaxDistance, 8.0, 128.0);
        focusMinDot = clampDouble(focusMinDot, 0.15, 0.95);
        focusDistanceWeight = clampDouble(focusDistanceWeight, 0.0, 2.0);

        if (stasisDurationTicks <= 0) {
            stasisDurationTicks = DEFAULT_STASIS_DURATION_TICKS;
        }
        if (stasisUpgradedDurationTicks <= 0) {
            stasisUpgradedDurationTicks = DEFAULT_STASIS_UPGRADED_DURATION_TICKS;
        }
        if (insipidDurationTicks <= 0) {
            insipidDurationTicks = DEFAULT_INSIPID_DURATION_TICKS;
        }
        if (insipidUpgradedDurationTicks <= 0) {
            insipidUpgradedDurationTicks = DEFAULT_INSIPID_UPGRADED_DURATION_TICKS;
        }
        if (insipidBriefFoodDurationTicks <= 0) {
            insipidBriefFoodDurationTicks = DEFAULT_INSIPID_BRIEF_FOOD_DURATION_TICKS;
        }
        if (acropsisDurationTicks <= 0) {
            acropsisDurationTicks = DEFAULT_ACROPSIS_DURATION_TICKS;
        }
        if (acropsisUpgradedDurationTicks <= 0) {
            acropsisUpgradedDurationTicks = DEFAULT_ACROPSIS_UPGRADED_DURATION_TICKS;
        }
        if (voidLeggingsDurationTicks <= 0) {
            voidLeggingsDurationTicks = DEFAULT_VOID_LEGGINGS_DURATION_TICKS;
        }
        if (voidLeggingsUpgradedDurationTicks <= 0) {
            voidLeggingsUpgradedDurationTicks = DEFAULT_VOID_LEGGINGS_UPGRADED_DURATION_TICKS;
        }
        if (braxlionDurationTicks <= 0) {
            braxlionDurationTicks = DEFAULT_BRAXLION_DURATION_TICKS;
        }
        if (easterLeggingsBindDurationTicks <= 0) {
            easterLeggingsBindDurationTicks = DEFAULT_EASTER_LEGGINGS_BIND_DURATION_TICKS;
        }
        if (easterLeggingsClipDurationTicks <= 0) {
            easterLeggingsClipDurationTicks = DEFAULT_EASTER_LEGGINGS_CLIP_DURATION_TICKS;
        }

        stasisDurationTicks = clampInt(stasisDurationTicks, 1, 20 * 300);
        stasisUpgradedDurationTicks = clampInt(stasisUpgradedDurationTicks, 1, 20 * 300);
        insipidDurationTicks = clampInt(insipidDurationTicks, 1, 20 * 300);
        insipidUpgradedDurationTicks = clampInt(insipidUpgradedDurationTicks, 1, 20 * 300);
        insipidBriefFoodDurationTicks = clampInt(insipidBriefFoodDurationTicks, 1, 20 * 300);
        acropsisDurationTicks = clampInt(acropsisDurationTicks, 1, 20 * 300);
        acropsisUpgradedDurationTicks = clampInt(acropsisUpgradedDurationTicks, 1, 20 * 300);
        voidLeggingsDurationTicks = clampInt(voidLeggingsDurationTicks, 1, 20 * 300);
        voidLeggingsUpgradedDurationTicks = clampInt(voidLeggingsUpgradedDurationTicks, 1, 20 * 300);
        braxlionDurationTicks = clampInt(braxlionDurationTicks, 1, 20 * 300);
        easterLeggingsBindDurationTicks = clampInt(easterLeggingsBindDurationTicks, 1, 20 * 300);
        easterLeggingsClipDurationTicks = clampInt(easterLeggingsClipDurationTicks, 1, 20 * 300);
        customColorR = clampInt(customColorR, 0, 255);
        customColorG = clampInt(customColorG, 0, 255);
        customColorB = clampInt(customColorB, 0, 255);

        if (timerDisplayMode == null) {
            timerDisplayMode = TimerDisplayMode.BAR_AND_SECONDS;
        }
        if (targetHudMode == null) targetHudMode = TargetHudMode.SCREEN;

        if (colorPreset == null) {
            colorPreset = HudColorPreset.OCEAN;
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum NametagLayout {
        STACKED("Stacked"), INLINE("One line");
        private final String label;
        NametagLayout(String label) { this.label = label; }
        public String label() { return label; }
        public NametagLayout next() { return values()[(ordinal()+1)%values().length]; }
    }
    public enum NametagColor {
        WHITE("White",0xFFFFFF), AQUA("Aqua",0x66DDFF), GOLD("Gold",0xFFD447),
        GREEN("Green",0x88EE88), PINK("Pink",0xFF99DD), RED("Red",0xFF7777), CUSTOM("Custom",0xFFFFFF);
        private final String label;
        private final int rgb;
        NametagColor(String label,int rgb) { this.label=label; this.rgb=rgb; }
        public String label() { return label; }
        public NametagColor next() { return values()[(ordinal()+1)%values().length]; }
        public int rgb(NewwindUtilsConfig config) {
            return this==CUSTOM ? (config.nametagColorR<<16)|(config.nametagColorG<<8)|config.nametagColorB : rgb;
        }
    }

    public enum TimerDisplayMode {
        SECONDS_ONLY("Seconds only"),
        BAR_ONLY("Bar only"),
        BAR_AND_SECONDS("Bar + seconds");

        private final String label;

        TimerDisplayMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public boolean showsSeconds() {
            return this == SECONDS_ONLY || this == BAR_AND_SECONDS;
        }

        public boolean showsBar() {
            return this == BAR_ONLY || this == BAR_AND_SECONDS;
        }

        public TimerDisplayMode next() {
            TimerDisplayMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public TimerDisplayMode previous() {
            TimerDisplayMode[] values = values();
            return values[(ordinal() - 1 + values.length) % values.length];
        }
    }

    public enum TargetHudMode {
        SCREEN("Screen HUD"), NAMETAG("Above nametag");
        private final String label;
        TargetHudMode(String label) { this.label = label; }
        public String label() { return label; }
        public TargetHudMode next() { return values()[(ordinal() + 1) % values().length]; }
        public TargetHudMode previous() { return values()[(ordinal() + values().length - 1) % values().length]; }
    }

    public enum HudColorPreset {
        OCEAN("Ocean", 0x900B1220, 0xFF4AA3FF, 0xFF9FD3FF, 0xFFFFFFFF, 0xFF2F7FD6, 0xFF14263A),
        FIRE("Fire", 0x901C0C08, 0xFFFF8A3D, 0xFFFFC266, 0xFFFFFFFF, 0xFFE25A1C, 0xFF3A180D),
        POISON("Poison", 0x9010180C, 0xFF76D64A, 0xFFB9FF8E, 0xFFFFFFFF, 0xFF55B83E, 0xFF162614),
        GOLD("Gold", 0x90201808, 0xFFFFD447, 0xFFFFF0A5, 0xFFFFFFFF, 0xFFE0B63A, 0xFF33260F),
        MONO("Mono", 0x90111111, 0xFFB8B8B8, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF909090, 0xFF2A2A2A),
        CUSTOM("Custom", 0x90111111, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF2A2A2A);

        private final String label;
        private final int backgroundColor;
        private final int outlineColor;
        private final int headerColor;
        private final int textColor;
        private final int barColor;
        private final int barBackgroundColor;

        HudColorPreset(
                String label,
                int backgroundColor,
                int outlineColor,
                int headerColor,
                int textColor,
                int barColor,
                int barBackgroundColor
        ) {
            this.label = label;
            this.backgroundColor = backgroundColor;
            this.outlineColor = outlineColor;
            this.headerColor = headerColor;
            this.textColor = textColor;
            this.barColor = barColor;
            this.barBackgroundColor = barBackgroundColor;
        }

        public String label() {
            return label;
        }

        public int backgroundColor() {
            return this == CUSTOM ? (0x90000000 | scaleRgb(customRgb(), 0.10F)) : backgroundColor;
        }

        public int outlineColor() {
            return this == CUSTOM ? (0xFF000000 | customRgb()) : outlineColor;
        }

        public int headerColor() {
            return this == CUSTOM ? (0xFF000000 | lightenRgb(customRgb(), 0.45F)) : headerColor;
        }

        public int textColor() {
            return textColor;
        }

        public int barColor() {
            return this == CUSTOM ? (0xFF000000 | scaleRgb(customRgb(), 0.85F)) : barColor;
        }

        public int barBackgroundColor() {
            return this == CUSTOM ? (0xFF000000 | scaleRgb(customRgb(), 0.18F)) : barBackgroundColor;
        }

        private static int customRgb() {
            NewwindUtilsConfig config = NewwindUtilsConfigManager.get();
            return (config.customColorR << 16) | (config.customColorG << 8) | config.customColorB;
        }

        private static int scaleRgb(int rgb, float factor) {
            int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
            int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
            int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
            return (r << 16) | (g << 8) | b;
        }

        private static int lightenRgb(int rgb, float amount) {
            int r = Math.round(((rgb >> 16) & 0xFF) + (255 - ((rgb >> 16) & 0xFF)) * amount);
            int g = Math.round(((rgb >> 8) & 0xFF) + (255 - ((rgb >> 8) & 0xFF)) * amount);
            int b = Math.round((rgb & 0xFF) + (255 - (rgb & 0xFF)) * amount);
            return (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
        }

        public HudColorPreset next() {
            HudColorPreset[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public HudColorPreset previous() {
            HudColorPreset[] values = values();
            return values[(ordinal() - 1 + values.length) % values.length];
        }
    }
}
