package newwindutils.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import newwindutils.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class PlayerNametagHudMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void newwindutils$extractTimers(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        NametagHud.Access access = (NametagHud.Access)state;
        access.newwindutils$setTimers(NametagHud.Frame.EMPTY);
        Minecraft client = Minecraft.getInstance();
        if(HudPreview.isEditing(client.gui.screen())) return;
        NewwindUtilsConfig config = NewwindUtilsConfigManager.get();
        if (config.targetHudMode != NewwindUtilsConfig.TargetHudMode.NAMETAG
                || client.player == null || client.level == null || !(entity instanceof Player player)
                || entity == client.player || entity.isInvisibleTo(client.player)) return;
        if (NewwindUtilsClient.getTimerManager().getTimersForPlayer(player, client.level.getGameTime()).isEmpty()) return;
        // Use the same focus, distance, ownership, and self-only rules as the screen HUD.
        for (EffectTimerManager.FocusedTarget target : NewwindUtilsClient.getTimerManager()
                .getFocusedTargets(client, client.level.getGameTime(), partialTick, config)) {
            if (target.player().getUUID().equals(player.getUUID())) {
                access.newwindutils$setTimers(NametagHud.frame(target.timers(),client.level.getGameTime(),config));
                break;
            }
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void newwindutils$submitTimers(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                          CameraRenderState camera, CallbackInfo ci) {
        NametagHud.Frame frame = ((NametagHud.Access)state).newwindutils$getTimers();
        if (frame.lines().isEmpty()) return;
        Vec3 anchor = state.nameTagAttachment != null ? state.nameTagAttachment
                : new Vec3(0, state.boundingBoxHeight + 0.5, 0);
        // Follow the vanilla nametag's camera-facing transform, with independent styling.
        double scoreSpace=state.scoreText==null?0:0.26;
        var font=Minecraft.getInstance().font;
        NametagHud.Style style=frame.style();
        pose.pushPose();
        pose.translate(anchor.x,anchor.y+0.5+frame.height()+scoreSpace,anchor.z);
        pose.rotate(camera.orientation);
        float scale=0.025F*style.scale();
        pose.scale(scale,-scale,scale);
        int light=net.minecraft.util.LightCoordsUtil.lightCoordsWithEmission(state.lightCoords,2);
        for(int i=0;i<frame.lines().size();i++) {
            var line=frame.lines().get(i);
            float x=-font.width(line)/2F;
            float y=-i*(font.lineHeight+style.spacing());
            if(style.background()!=0) collector.submitTextBackground(pose,x-2,y-1,-x+2,y+font.lineHeight+1,
                    style.background(),net.minecraft.client.gui.Font.DisplayMode.NORMAL,light);
            collector.submitText(pose,x,y,line.getVisualOrderText(),style.shadow(),
                    net.minecraft.client.gui.Font.DisplayMode.NORMAL,light,0xFFFFFFFF,0,0);
        }
        pose.popPose();
    }
}
