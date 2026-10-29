package newwindutils.client;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NametagHud {
    private NametagHud() {}
    public record Style(float scale,int spacing,int background,boolean shadow) {
        public static final Style DEFAULT = new Style(1,3,0x40000000,true);
    }
    public record Frame(List<Component> lines, double height, Style style) {
        public static final Frame EMPTY = new Frame(List.of(), 0, Style.DEFAULT);
        public Frame(List<Component> lines,double height) { this(lines,height,Style.DEFAULT); }
        public Frame { lines = List.copyOf(lines); }
    }
    public interface Access {
        Frame newwindutils$getTimers();
        void newwindutils$setTimers(Frame frame);
    }
    public static Frame frame(List<ActiveEffectTimer> timers,long now,NewwindUtilsConfig config) {
        return new Frame(lines(timers,now,config),config.nametagHeight,
                new Style(config.nametagScale,config.nametagLineSpacing,
                        config.nametagBackground ? config.nametagBackgroundOpacity<<24 : 0,config.nametagShadow));
    }
    public static List<Component> lines(List<ActiveEffectTimer> timers,long now) {
        return lines(timers,now,new NewwindUtilsConfig());
    }
    public static List<Component> lines(List<ActiveEffectTimer> timers,long now,NewwindUtilsConfig config) {
        List<Component> lines=new ArrayList<>();
        for(ActiveEffectTimer timer:timers) {
            if(timer.remainingTicks(now)<=0) continue;
            int color=config.nametagTimeColors?timeColor(timer.progress(now)):config.nametagColor.rgb(config);
            String text=timer.displayName()+": "+String.format(Locale.US,"%."+config.nametagDecimals+"fs",timer.remainingSeconds(now));
            lines.add(Component.literal(text).withStyle(style->style.withColor(color).withBold(config.nametagBold)));
        }
        if(config.nametagLayout==NewwindUtilsConfig.NametagLayout.INLINE && !lines.isEmpty()) {
            var joined=Component.empty();
            for(int i=0;i<lines.size();i++) {
                if(i>0) joined.append(Component.literal("  |  ").withStyle(ChatFormatting.GRAY));
                joined.append(lines.get(i));
            }
            return List.of(joined);
        }
        return List.copyOf(lines);
    }
    private static int timeColor(float progress) {
        return progress>0.66F?0x5FD65F:progress>0.33F?0xE3C34A:0xE05A5A;
    }
}
