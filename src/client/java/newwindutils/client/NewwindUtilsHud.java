package newwindutils.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.Locale;

public class NewwindUtilsHud {
    private static final int PADDING = 6;
    private static final int HEADER_GAP = 6;
    private static final int TEXT_LINE_HEIGHT = 12;
    private static final int BAR_TEXT_GAP = 3;
    private static final int EFFECT_GAP = 4;
    private static final int SECTION_GAP = 6;
    private static final String INVISIBLE_NAME_MASK = "\u00A7k-------------\u00A7r";
    private static final Identifier NEWWINDSERVER_SKIN =
            Identifier.fromNamespaceAndPath("newwindutils", "textures/gui/newwindserver_skin.png");

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        if (HudPreview.isEditing(client.gui.screen())) return;
        NewwindUtilsConfig config = NewwindUtilsConfigManager.get();
        if (config.targetHudMode == NewwindUtilsConfig.TargetHudMode.NAMETAG) return;
        long currentGameTime = client.level.getGameTime();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        List<EffectTimerManager.FocusedTarget> focusedTargets =
                NewwindUtilsClient.getTimerManager().getFocusedTargets(
                        client,
                        currentGameTime,
                        partialTick,
                        config
                );

        if (focusedTargets.isEmpty()) {
            return;
        }

        float hudScale = config.hudScale;
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.scale(hudScale, hudScale);

        int currentY = Math.round(config.hudY / hudScale);
        int scaledHudX = Math.round(config.hudX / hudScale);

        for (EffectTimerManager.FocusedTarget target : focusedTargets) {
            int cardHeight = renderTargetCard(graphics, client, target, scaledHudX, currentY, currentGameTime, config);
            currentY += cardHeight + config.targetCardSpacing;
        }

        matrices.popMatrix();
    }

    static int renderTargetCard(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            EffectTimerManager.FocusedTarget target,
            int x,
            int y,
            long currentGameTime,
            NewwindUtilsConfig config
    ) {
        NewwindUtilsConfig.HudColorPreset preset = config.colorPreset;

        String playerName = getDisplayedPlayerName(target.player(), target.displayName());
        List<ActiveEffectTimer> timers = target.timers();

        CardLayout layout=measureCard(client,playerName,timers,currentGameTime,config);
        int headWidth=layout.headWidth();
        int headerHeight=layout.headerHeight();
        int contentWidth=layout.contentWidth();
        int contentHeight=layout.contentHeight();

        int left = x;
        int top = y;
        int right = left + (PADDING * 2) + contentWidth;
        int bottom = top + (PADDING * 2) + contentHeight;

        graphics.fill(left, top, right, bottom, preset.backgroundColor());

        if (config.showOutline) {
            graphics.outline(left, top, right - left, bottom - top, preset.outlineColor());
        }

        int headerX = left + PADDING;
        int headerY = top + PADDING;

        if (config.showPlayerHeads) {
            boolean invisible = target.player() != null && target.player().isInvisible();
            if (invisible) {
                drawNewwindserverHead(graphics, headerX, headerY, config.headSize);
            } else if(target.player()!=null) {
                drawPlayerHead(graphics,target.player(),headerX,headerY,config.headSize);
            } else if(client.player!=null) {
                drawPlayerHead(graphics,client.player,headerX,headerY,config.headSize);
            } else {
                drawNewwindserverHead(graphics,headerX,headerY,config.headSize);
            }
        }

        int nameX = headerX + headWidth;
        int nameY = headerY + 2;

        drawScaledString(
                graphics,
                client,
                playerName,
                nameX,
                nameY,
                preset.headerColor(),
                config.nameScale,
                config.shadowText
        );

        int currentY = headerY + headerHeight + SECTION_GAP;
        int effectX = left + PADDING;

        for (int i = 0; i < timers.size(); i++) {
            ActiveEffectTimer timer = timers.get(i);

            String line = formatTimerLine(timer, currentGameTime, config);
            graphics.text(client.font, line, effectX, currentY, preset.textColor(), config.shadowText);
            currentY += TEXT_LINE_HEIGHT;

            if (config.timerDisplayMode.showsBar()) {
                int barY = currentY + 1;
                renderTimerBar(graphics, effectX, barY, timer, currentGameTime, config, preset);
                currentY += BAR_TEXT_GAP + config.barHeight;
            }

            if (i < timers.size() - 1) {
                currentY += EFFECT_GAP;
            }
        }

        return bottom - top;
    }

    record CardLayout(int headWidth,int headerHeight,int contentWidth,int contentHeight) {
        int width() { return contentWidth+PADDING*2; }
        int height() { return contentHeight+PADDING*2; }
    }
    static CardLayout measureCard(Minecraft client,String playerName,List<ActiveEffectTimer> timers,
                                  long currentGameTime,NewwindUtilsConfig config) {
        int headWidth = config.showPlayerHeads ? config.headSize + HEADER_GAP : 0;
        int scaledNameWidth = Math.round(client.font.width(playerName) * config.nameScale);
        int scaledNameHeight = Math.round(client.font.lineHeight * config.nameScale);

        int headerWidth = headWidth + scaledNameWidth;
        int headerHeight = Math.max(config.showPlayerHeads ? config.headSize : 0, scaledNameHeight);

        int effectsWidth = 0;
        for (ActiveEffectTimer timer : timers) {
            String line = formatTimerLine(timer, currentGameTime, config);
            effectsWidth = Math.max(effectsWidth, client.font.width(line));
            if (config.timerDisplayMode.showsBar()) {
                effectsWidth = Math.max(effectsWidth, config.barWidth);
            }
        }

        int contentWidth = Math.max(headerWidth, effectsWidth);
        int effectsHeight = 0;

        for (int i = 0; i < timers.size(); i++) {
            effectsHeight += getEffectBlockHeight(config);
            if (i < timers.size() - 1) {
                effectsHeight += EFFECT_GAP;
            }
        }

        int contentHeight = headerHeight + SECTION_GAP + effectsHeight;

        return new CardLayout(headWidth,headerHeight,contentWidth,contentHeight);
    }
    static void renderSample(GuiGraphicsExtractor graphics,Minecraft client,int x,int y,long now,
                             NewwindUtilsConfig config,List<ActiveEffectTimer> timers) {
        renderTargetCard(graphics,client,new EffectTimerManager.FocusedTarget(null,"ExamplePlayer",timers),x,y,now,config);
    }

    private static String getDisplayedPlayerName(AbstractClientPlayer player, String fallbackName) {
        if (player != null && player.isInvisible()) {
            return INVISIBLE_NAME_MASK;
        }
        return fallbackName;
    }

    private static int getEffectBlockHeight(NewwindUtilsConfig config) {
        int height = TEXT_LINE_HEIGHT;
        if (config.timerDisplayMode.showsBar()) {
            height += BAR_TEXT_GAP + config.barHeight;
        }
        return height;
    }

    private static void renderTimerBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ActiveEffectTimer timer,
            long currentGameTime,
            NewwindUtilsConfig config,
            NewwindUtilsConfig.HudColorPreset preset
    ) {
        graphics.fill(x, y, x + config.barWidth, y + config.barHeight, preset.barBackgroundColor());

        float progress = timer.progress(currentGameTime);
        int fillWidth = Math.max(0, Math.round(config.barWidth * progress));

        if (fillWidth > 0) {
            int fillColor = config.colorBarsByRemainingTime
                    ? getTimeColor(progress)
                    : preset.barColor();

            graphics.fill(x, y, x + fillWidth, y + config.barHeight, fillColor);
        }

        if (config.showOutline) {
            graphics.outline(x, y, config.barWidth, config.barHeight, preset.outlineColor());
        }
    }

    private static int getTimeColor(float progress) {
        if (progress > 0.66F) {
            return 0xFF5FD65F;
        }
        if (progress > 0.33F) {
            return 0xFFE3C34A;
        }
        return 0xFFE05A5A;
    }

    private static String formatTimerLine(
            ActiveEffectTimer timer,
            long currentGameTime,
            NewwindUtilsConfig config
    ) {
        if (config.timerDisplayMode.showsSeconds()) {
            String secondsText = String.format(Locale.US, "%.1fs", timer.remainingSeconds(currentGameTime));
            return timer.displayName() + ": " + secondsText;
        }

        return timer.displayName();
    }

    private static void drawScaledString(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            String text,
            int x,
            int y,
            int color,
            float scale,
            boolean shadow
    ) {
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.scale(scale, scale);

        graphics.text(
                client.font,
                text,
                Math.round(x / scale),
                Math.round(y / scale),
                color,
                shadow
        );

        matrices.popMatrix();
    }

    static void drawPlayerHead(GuiGraphicsExtractor graphics, AbstractClientPlayer player, int x, int y, int size) {
        Identifier skinTexture = player.getSkin().body().texturePath();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                skinTexture,
                x,
                y,
                8,
                8,
                size,
                size,
                8,
                8,
                64,
                64
        );

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                skinTexture,
                x,
                y,
                40,
                8,
                size,
                size,
                8,
                8,
                64,
                64
        );
    }

    private static void drawNewwindserverHead(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                NEWWINDSERVER_SKIN,
                x,
                y,
                8,
                8,
                size,
                size,
                8,
                8,
                64,
                64
        );

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                NEWWINDSERVER_SKIN,
                x,
                y,
                40,
                8,
                size,
                size,
                8,
                8,
                64,
                64
        );
    }
}
