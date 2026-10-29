package newwindutils.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TargetEffectState {
    private final UUID targetUuid;
    private String lastKnownName;
    private final Map<String, ActiveEffectTimer> activeTimers = new LinkedHashMap<>();

    public TargetEffectState(UUID targetUuid, String lastKnownName) {
        this.targetUuid = targetUuid;
        this.lastKnownName = lastKnownName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public void startOrRefresh(TrackedWeapon weapon, UUID appliedByUuid, long currentGameTime) {
        String effectId = weapon.effectId();
        long newExpiresAt = currentGameTime + weapon.durationTicks();

        ActiveEffectTimer existing = activeTimers.get(effectId);
        if (existing != null && existing.expiresAtGameTime() > newExpiresAt) {
            return;
        }

        activeTimers.put(
                effectId,
                new ActiveEffectTimer(
                        effectId,
                        weapon.effectDisplayName(),
                        weapon.durationTicks(),
                        newExpiresAt,
                        appliedByUuid
                )
        );
    }

    public void tick(long currentGameTime) {
        Iterator<ActiveEffectTimer> iterator = activeTimers.values().iterator();
        while (iterator.hasNext()) {
            ActiveEffectTimer timer = iterator.next();
            if (timer.remainingTicks(currentGameTime) <= 0) {
                iterator.remove();
            }
        }
    }

    public boolean isEmpty() {
        return activeTimers.isEmpty();
    }

    public List<ActiveEffectTimer> getActiveTimers(long currentGameTime) {
        List<ActiveEffectTimer> result = new ArrayList<>();

        for (ActiveEffectTimer timer : activeTimers.values()) {
            if (timer.remainingTicks(currentGameTime) > 0) {
                result.add(timer);
            }
        }

        result.sort(Comparator.comparingInt(timer -> timer.remainingTicks(currentGameTime)));
        return result;
    }
}
