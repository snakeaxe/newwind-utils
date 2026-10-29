package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NearbyInteractionTracker {
    private static final int USE_WINDOW_TICKS = 4;
    private static final double MAX_USE_DISTANCE = 4.5;
    private static final double MIN_FACING_DOT = 0.65;

    private final Map<UUID, Boolean> lastSwingingByPlayer = new HashMap<>();
    private final List<PendingNearbyUse> pendingUses = new ArrayList<>();

    public void tick(
            Minecraft client,
            WeaponRegistry weaponRegistry,
            EffectTimerManager timerManager
    ) {
        if (client.player == null || client.level == null) {
            lastSwingingByPlayer.clear();
            pendingUses.clear();
            return;
        }

        long gameTime = client.level.getGameTime();
        List<? extends Player> players = client.level.players();

        collectUseCandidates(players, weaponRegistry, client.player, gameTime);
        resolveUses(players, timerManager, gameTime);
        cleanupExpired(gameTime);
    }

    private void collectUseCandidates(
            List<? extends Player> players,
            WeaponRegistry weaponRegistry,
            Player localPlayer,
            long gameTime
    ) {
        for (Player attacker : players) {
            boolean wasSwinging = lastSwingingByPlayer.getOrDefault(attacker.getUUID(), false);
            boolean isSwinging = attacker.isSwinging();

            if (attacker != localPlayer && isSwinging && !wasSwinging) {
                List<TrackedWeapon> sources = new ArrayList<>(weaponRegistry.findHandSources(attacker,
                        net.minecraft.world.InteractionHand.MAIN_HAND, TrackedWeapon.TriggerType.USE_ENTITY));
                sources.addAll(weaponRegistry.findHandSources(attacker,
                        net.minecraft.world.InteractionHand.OFF_HAND, TrackedWeapon.TriggerType.USE_ENTITY));
                for (TrackedWeapon weapon : sources) {
                    pendingUses.add(new PendingNearbyUse(
                            attacker.getUUID(),
                            weapon,
                            gameTime + USE_WINDOW_TICKS
                    ));
                }
            }

            lastSwingingByPlayer.put(attacker.getUUID(), isSwinging);
        }
    }

    private void resolveUses(
            List<? extends Player> players,
            EffectTimerManager timerManager,
            long gameTime
    ) {
        Iterator<PendingNearbyUse> iterator = pendingUses.iterator();

        while (iterator.hasNext()) {
            PendingNearbyUse pending = iterator.next();

            Player attacker = findPlayerByUuid(players, pending.attackerUuid());
            if (attacker == null) {
                iterator.remove();
                continue;
            }

            Player bestTarget = findBestTarget(players, attacker);
            if (bestTarget != null) {
                timerManager.startOrRefresh(bestTarget, pending.weapon(), pending.attackerUuid(), gameTime);
                iterator.remove();
            }
        }
    }

    private Player findBestTarget(List<? extends Player> players, Player attacker) {
        Player bestTarget = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        Vec3 attackerEyes = attacker.getEyePosition();
        Vec3 look = attacker.getViewVector(1.0F).normalize();

        for (Player target : players) {
            if (target == attacker || target.isRemoved()) {
                continue;
            }

            double distance = attacker.distanceTo(target);
            if (distance > MAX_USE_DISTANCE) {
                continue;
            }

            Vec3 toTarget = target.getEyePosition().subtract(attackerEyes);
            if (toTarget.lengthSqr() < 0.0001) {
                continue;
            }

            double facingDot = look.dot(toTarget.normalize());
            if (facingDot < MIN_FACING_DOT) {
                continue;
            }

            double score = (facingDot * 100.0) - (distance * 10.0);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private void cleanupExpired(long gameTime) {
        pendingUses.removeIf(use -> use.expiresAtGameTime() < gameTime);
    }

    private Player findPlayerByUuid(List<? extends Player> players, UUID uuid) {
        for (Player player : players) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private record PendingNearbyUse(
            UUID attackerUuid,
            TrackedWeapon weapon,
            long expiresAtGameTime
    ) {
    }
}
