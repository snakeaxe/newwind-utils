package newwindutils.client.mixin;

import newwindutils.client.CooldownObservations;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketObservationMixin {
    // RETURN runs after vanilla's main-thread handoff and cooldown application.
    @Inject(method="handleItemCooldown",at=@At("RETURN"))
    private void newwindutils$cooldown(ClientboundCooldownPacket packet, CallbackInfo ci) {
        CooldownObservations.serverCooldown(packet);
    }
    @Inject(method="handleDamageEvent",at=@At("RETURN"))
    private void newwindutils$receivedHit(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        CooldownObservations.incomingHit(packet);
    }
}
