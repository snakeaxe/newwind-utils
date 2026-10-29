package newwindutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NewwindUtilsClient implements ClientModInitializer {
    public static final String MOD_ID = "newwindutils";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int CONFIRM_WINDOW_TICKS = 8;

    private static WeaponRegistry WEAPON_REGISTRY = new WeaponRegistry();
    private static final EffectTimerManager TIMER_MANAGER = new EffectTimerManager();
    private static final NearbyCombatTracker NEARBY_COMBAT_TRACKER = new NearbyCombatTracker();
    private static final NearbyInteractionTracker NEARBY_INTERACTION_TRACKER = new NearbyInteractionTracker();

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "general"));

    private static KeyMapping inspectCmdKey;
    private static KeyMapping openConfigKey;
    private static KeyMapping registerItemKey;
    private static KeyMapping measureEffectKey;

    private final List<PendingHit> pendingHits = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        LOGGER.info("[NewWindUtils] Client mod initialized");

        NewwindUtilsConfigManager.load();
        refreshWeaponRegistry();

        registerKeybinds();
        registerHud();
        registerTickHandler();
        registerAttackHandler();
        registerUseEntityHandler();
    }

    public static EffectTimerManager getTimerManager() {
        return TIMER_MANAGER;
    }

    public static void refreshWeaponRegistry() {
        WEAPON_REGISTRY = WeaponRegistry.createDefault(NewwindUtilsConfigManager.get());
        logRegisteredSources();
    }

    private static void logRegisteredSources() {
        for (TrackedWeapon weapon : WEAPON_REGISTRY.getWeapons()) {
            LOGGER.info(
                    "[NewWindUtils] Registered source '{}' cmd={} lore={} duration={} ticks trigger={} sourceMode={} enabled={}",
                    weapon.displayName(),
                    weapon.requiredCustomModelData(),
                    weapon.requiredLoreLine(),
                    weapon.durationTicks(),
                    weapon.triggerType(),
                    weapon.sourceMode(),
                    weapon.enabled()
            );
        }
    }

    private void registerUseEntityHandler() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (Minecraft.getInstance().player != player) {
                return InteractionResult.PASS;
            }

            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }

            if (!(entity instanceof Player targetPlayer)) {
                return InteractionResult.PASS;
            }

            for (TrackedWeapon weapon : WEAPON_REGISTRY.findHandSources(player, hand, TrackedWeapon.TriggerType.USE_ENTITY)) {
                TIMER_MANAGER.startOrRefresh(targetPlayer, weapon, player.getUUID(), world.getGameTime());
            }

            return InteractionResult.PASS;
        });
    }

    private void registerKeybinds() {
        inspectCmdKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.newwindutils.inspect_custom_model_data",
                
                InputConstants.KEY_J,
                CATEGORY
        ));

        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.newwindutils.open_config",
                
                InputConstants.KEY_U,
                CATEGORY
        ));
        registerItemKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.newwindutils.register_item", InputConstants.KEY_K, CATEGORY));
        measureEffectKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.newwindutils.measure_effect", InputConstants.KEY_L, CATEGORY));
    }

    private void registerHud() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "effect_timers"),
                NewwindUtilsHud::render
        );

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "self_effect_timers"),
                NewwindUtilsSelfHud::render
        );
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CooldownObservations.tick(client);
            NEARBY_INTERACTION_TRACKER.tick(
                    client,
                    WEAPON_REGISTRY,
                    TIMER_MANAGER
            );

            if (client.level == null) {
                pendingHits.clear();
                TIMER_MANAGER.clear();
                return;
            }

            if (client.player == null) {
                pendingHits.clear();
                return;
            }

            long currentGameTime = client.level.getGameTime();

            TIMER_MANAGER.tick(currentGameTime);
            TIMER_MANAGER.clearDeadPlayers(client.level.players());

            if (!client.player.isAlive()) {
                pendingHits.clear();
            }

            NEARBY_COMBAT_TRACKER.tick(
                    client,
                    WEAPON_REGISTRY,
                    TIMER_MANAGER,
                    NewwindUtilsConfigManager.get()
            );

            while (inspectCmdKey.consumeClick()) {
                dumpMainHandCustomModelData(client);
            }

            while (openConfigKey.consumeClick()) {
                client.gui.setScreen(new NewwindUtilsConfigScreen(client.gui.screen()));
            }

            while (registerItemKey.consumeClick()) {
                CustomItemsScreen.openHeld(client.gui.screen());
            }
            while (measureEffectKey.consumeClick()) CooldownObservations.measureKey(client);

            Iterator<PendingHit> iterator = pendingHits.iterator();
            while (iterator.hasNext()) {
                PendingHit pending = iterator.next();

                if (client.player.tickCount > pending.expiresAtTick()) {
                    iterator.remove();
                    continue;
                }

                Entity entity = client.level.getEntity(pending.targetEntityId());
                if (!(entity instanceof LivingEntity living) || entity.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                if (living.hurtTime > pending.startingHurtTime()) {
                    if (entity instanceof Player targetPlayer) {
                        for (TrackedWeapon source : pending.sources()) {
                            LOGGER.info(
                                    "[NewWindUtils] Confirmed hit with '{}' on {}",
                                    source.displayName(),
                                    entity.getName().getString()
                            );
                            TIMER_MANAGER.startOrRefresh(targetPlayer, source, client.player.getUUID(), currentGameTime);
                        }

                        showMessage(client, 
                                Component.literal("Started " + pending.sources().size() + " timer(s) -> " + targetPlayer.getName().getString()),
                                true
                        );
                    }

                    iterator.remove();
                }
            }
        });
    }

    private void registerAttackHandler() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (Minecraft.getInstance().player != player) {
                return InteractionResult.PASS;
            }

            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }

            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            if (!(entity instanceof Player targetPlayer)) {
                return InteractionResult.PASS;
            }

            List<TrackedWeapon> sources = WEAPON_REGISTRY.findMeleeHitSources(player);
            if (sources.isEmpty()) {
                return InteractionResult.PASS;
            }

            pendingHits.removeIf(hit -> hit.targetEntityId() == targetPlayer.getId());
            pendingHits.add(new PendingHit(
                    targetPlayer.getId(),
                    targetPlayer.hurtTime,
                    player.tickCount + CONFIRM_WINDOW_TICKS,
                    List.copyOf(sources)
            ));

            LOGGER.info(
                    "[NewWindUtils] Queued pending hit with {} source(s) on {}",
                    sources.size(),
                    targetPlayer.getName().getString()
            );

            return InteractionResult.PASS;
        });
    }

    private static void dumpMainHandCustomModelData(Minecraft client) {
        if (client.player == null) {
            return;
        }

        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            LOGGER.info("[NewWindUtils] Main hand is empty");
            showMessage(client, Component.literal("Main hand is empty"), true);
            return;
        }

        LOGGER.info("[NewWindUtils] Inspecting item: {}", stack.getHoverName().getString());
        LOGGER.info("[NewWindUtils] ALL components: {}", stack.getComponents());
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd == null) {
            LOGGER.info("[NewWindUtils] No custom model data component found");
            showMessage(client, Component.literal("No custom model data found"), true);
            return;
        }

        LOGGER.info("[NewWindUtils] CMD floats={}", cmd.floats());
        LOGGER.info("[NewWindUtils] CMD flags={}", cmd.flags());
        LOGGER.info("[NewWindUtils] CMD strings={}", cmd.strings());
        LOGGER.info("[NewWindUtils] CMD colors={}", cmd.colors());

        Float firstFloat = cmd.getFloat(0);
        if (firstFloat != null) {
            showMessage(client, 
                    Component.literal("CMD float[0] = " + firstFloat),
                    true
            );
        } else {
            showMessage(client, 
                    Component.literal("CMD found; check console for full values"),
                    true
            );
        }

        TrackedWeapon matchedWeapon = WEAPON_REGISTRY.findByItem(stack);
        if (matchedWeapon != null) {
            showMessage(client, 
                    Component.literal("Tracked source match: " + matchedWeapon.displayName()),
                    false
            );
        }
    }

    private record PendingHit(
            int targetEntityId,
            int startingHurtTime,
            int expiresAtTick,
            List<TrackedWeapon> sources
    ) {
    }
    private static void showMessage(Minecraft client, Component message, boolean overlay) {
        if (overlay) {
            client.gui.hud.setOverlayMessage(message, false);
        } else {
            client.player.sendSystemMessage(message);
        }
    }}
