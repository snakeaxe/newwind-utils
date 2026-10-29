package newwindutils.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.Locale;

public class NewwindUtilsSelfHud {
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 12;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        if(HudPreview.isEditing(client.gui.screen())) return;
        NewwindUtilsConfig config = NewwindUtilsConfigManager.get();
        long gameTime = client.level.getGameTime();

        List<ActiveEffectTimer> selfTimers = NewwindUtilsClient
                .getTimerManager()
                .getTimersForPlayer(client.player, gameTime);

        if (selfTimers.isEmpty()) {
            return;
        }

        if (!config.showSelfHud && !config.showOnlySelfTimers) {
            return;
        }

        renderSampleOnScreen(graphics,client,selfTimers,gameTime,config);
    }
    record Size(int width,int height) {}
    static Size measure(Minecraft client,List<ActiveEffectTimer> selfTimers,long gameTime,NewwindUtilsConfig config) {
        int maxWidth = client.font.width("On You");
        for (ActiveEffectTimer timer : selfTimers) {
            String line = formatTimer(timer, gameTime, config);
            maxWidth = Math.max(maxWidth, client.font.width(line));
        }

        int width = maxWidth + (PADDING * 2);
        int height = (selfTimers.size() + 1) * LINE_HEIGHT + (PADDING * 2);
        return new Size(width,height);
    }
    static void renderSampleOnScreen(GuiGraphicsExtractor graphics,Minecraft client,List<ActiveEffectTimer> selfTimers,
                                     long gameTime,NewwindUtilsConfig config) {
        Size size=measure(client,selfTimers,gameTime,config);
        int width=size.width(),height=size.height();
        float hudScale = Math.min(config.hudScale, Math.min(
                (client.getWindow().getGuiScaledWidth() - 12F) / width,
                (client.getWindow().getGuiScaledHeight() - 12F) / height));
        hudScale = Math.max(0.01F, hudScale);
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.scale(hudScale, hudScale);
        int screenWidth = Math.round(client.getWindow().getGuiScaledWidth() / hudScale);
        int screenHeight = Math.round(client.getWindow().getGuiScaledHeight() / hudScale);
        int x = config.selfHudX < 0
                ? Math.max(0, screenWidth - width - PADDING)
                : Math.min(Math.round(config.selfHudX / hudScale), Math.max(0, screenWidth - width));
        int y = Math.min(Math.round(config.selfHudY / hudScale), Math.max(0, screenHeight - height));

        drawPanel(graphics,client,selfTimers,gameTime,config,x,y);
        matrices.popMatrix();
    }
    static void drawPanel(GuiGraphicsExtractor graphics,Minecraft client,List<ActiveEffectTimer> selfTimers,
                          long gameTime,NewwindUtilsConfig config,int x,int y) {
        Size size=measure(client,selfTimers,gameTime,config);
        int width=size.width(),height=size.height();
        NewwindUtilsConfig.HudColorPreset preset = config.colorPreset;

        graphics.fill(x, y, x + width, y + height, preset.backgroundColor());
        if (config.showOutline) {
            graphics.outline(x, y, width, height, preset.outlineColor());
        }

        int lineY = y + PADDING;
        graphics.text(client.font, "On You", x + PADDING, lineY, preset.headerColor(), config.shadowText);
        lineY += LINE_HEIGHT;

        for (ActiveEffectTimer timer : selfTimers) {
            graphics.text(
                    client.font,
                    formatTimer(timer, gameTime, config),
                    x + PADDING,
                    lineY,
                    preset.textColor(),
                    config.shadowText
            );
            lineY += LINE_HEIGHT;
        }

    }

    private static String formatTimer(
            ActiveEffectTimer timer,
            long currentGameTime,
            NewwindUtilsConfig config
    ) {
        if (config.timerDisplayMode.showsSeconds()) {
            return timer.displayName() + ": " +
                    String.format(Locale.US, "%.1fs", timer.remainingSeconds(currentGameTime));
        }

        return timer.displayName();
    }
}
