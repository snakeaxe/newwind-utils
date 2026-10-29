package newwindutils.client;

import java.util.UUID;

public record ActiveEffectTimer(
        String effectId,
        String displayName,
        int durationTicks,
        long expiresAtGameTime,
        UUID appliedByUuid
) {
    public int remainingTicks(long currentGameTime) {
        long remaining = expiresAtGameTime - currentGameTime;
        return (int) Math.max(0L, remaining);
    }

    public float remainingSeconds(long currentGameTime) {
        return remainingTicks(currentGameTime) / 20.0F;
    }

    public float progress(long currentGameTime) {
        if (durationTicks <= 0) {
            return 0.0F;
        }

        return Math.max(0.0F, Math.min(1.0F, remainingTicks(currentGameTime) / (float) durationTicks));
    }

    public boolean wasAppliedBy(UUID playerUuid) {
        return playerUuid != null && playerUuid.equals(appliedByUuid);
    }
}
