package newwindutils.client.mixin;

import newwindutils.client.NametagHud;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class NametagTimerStateMixin implements NametagHud.Access {
    @Unique private NametagHud.Frame newwindutils$timers = NametagHud.Frame.EMPTY;
    public NametagHud.Frame newwindutils$getTimers() { return newwindutils$timers; }
    public void newwindutils$setTimers(NametagHud.Frame frame) { newwindutils$timers = frame; }
}
