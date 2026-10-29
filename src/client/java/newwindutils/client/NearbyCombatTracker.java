package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NearbyCombatTracker {
    private static final int ATTACK_WINDOW_TICKS = 6;
    private static final double MAX_MELEE_DISTANCE = 4.6;
    private static final double MIN_FACING_DOT = 0.35;

    private final Map<UUID, Integer> lastHurtTimeByPlayer = new HashMap<>();
    private final Map<UUID, Boolean> lastSwingingByPlayer = new HashMap<>();
    private final List<PendingNearbyAttack> pendingNearbyAttacks = new ArrayList<>();

    public void tick(
            Minecraft client,
            WeaponRegistry weaponRegistry,
            EffectTimerManager timerManager,
            NewwindUtilsConfig config
    ) {
        if (client.player == null || client.level == null) {
            lastHurtTimeByPlayer.clear();
            lastSwingingByPlayer.clear();
            pendingNearbyAttacks.clear();
            return;
        }

        long gameTime = client.level.getGameTime();
        List<? extends Player> players = client.level.players();

        collectAttackerCandidates(players, weaponRegistry, client.player, gameTime);
        resolveVictimHurtEvents(players, timerManager, gameTime);
        cleanupExpired(gameTime);
    }

    private void collectAttackerCandidates(
            List<? extends Player> players,
            WeaponRegistry weaponRegistry,
            Player localPlayer,
            long gameTime
    ) {
        for (Player attacker : players) {
            boolean wasSwinging = lastSwingingByPlayer.getOrDefault(attacker.getUUID(), false);
            boolean isSwinging = attacker.isSwinging();

            if (attacker != localPlayer && isSwinging && !wasSwinging) {
                List<TrackedWeapon> sources = weaponRegistry.findMeleeHitSources(attacker);
                if (!sources.isEmpty()) {
                    pendingNearbyAttacks.add(new PendingNearbyAttack(
                            attacker.getUUID(),
                            sources,
                            gameTime + ATTACK_WINDOW_TICKS
                    ));
                }
            }

            lastSwingingByPlayer.put(attacker.getUUID(), isSwinging);
        }
    }

    private void resolveVictimHurtEvents(
            List<? extends Player> players,
            EffectTimerManager timerManager,
            long gameTime
    ) {
        for (Player victim : players) {
            int previousHurtTime = lastHurtTimeByPlayer.getOrDefault(victim.getUUID(), 0);
            int currentHurtTime = victim.hurtTime;

            if (currentHurtTime > previousHurtTime) {
                PendingNearbyAttack best = findBestAttackerForVictim(players, victim, gameTime);

                if (best != null) {
                    for (TrackedWeapon source : best.sources()) {
                        timerManager.startOrRefresh(victim, source, best.attackerUuid(), gameTime);
                    }
                }
            }

            lastHurtTimeByPlayer.put(victim.getUUID(), currentHurtTime);
        }
    }

    private PendingNearbyAttack findBestAttackerForVictim(
            List<? extends Player> players,
            Player victim,
            long gameTime
    ) {
        Player bestAttacker = null;
        PendingNearbyAttack bestAttack = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PendingNearbyAttack pending : pendingNearbyAttacks) {
            if (pending.expiresAtGameTime() < gameTime) {
                continue;
            }

            Player attacker = findPlayerByUuid(players, pending.attackerUuid());
            if (attacker == null || attacker == victim) {
                continue;
            }

            double distance = attacker.distanceTo(victim);
            if (distance > MAX_MELEE_DISTANCE) {
                continue;
            }

            Vec3 attackerEyes = attacker.getEyePosition();
            Vec3 toVictim = victim.getEyePosition().subtract(attackerEyes);
            if (toVictim.lengthSqr() < 0.0001) {
                continue;
            }

            double facingDot = attacker.getViewVector(1.0F).normalize().dot(toVictim.normalize());
            if (facingDot < MIN_FACING_DOT) {
                continue;
            }

            double score = (facingDot * 100.0) - (distance * 10.0);
            if (score > bestScore) {
                bestScore = score;
                bestAttacker = attacker;
                bestAttack = pending;
            }
        }

        return bestAttacker != null ? bestAttack : null;
    }

    private void cleanupExpired(long gameTime) {
        pendingNearbyAttacks.removeIf(pending -> pending.expiresAtGameTime() < gameTime);
    }

    private Player findPlayerByUuid(List<? extends Player> players, UUID uuid) {
        for (Player player : players) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private record PendingNearbyAttack(
            UUID attackerUuid,
            List<TrackedWeapon> sources,
            long expiresAtGameTime
    ) {
    }
}
