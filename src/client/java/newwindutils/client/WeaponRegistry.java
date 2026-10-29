package newwindutils.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WeaponRegistry {
    private static final String INCREASED_BIND_TIME_LORE = "- Increased Bind Time";
    private static final String BRIEFLY_BINDS_ALL_FOOD_LORE = "- Briefly Binds All Food";

    private static final String EFFECT_BIND = "bind";
    private static final String EFFECT_SUCC = "succ";
    private static final String EFFECT_ACROPSIS = "acropsis";
    private static final String EFFECT_CLIP = "clip";

    // Items are recognised by their "use cooldown group", which NewWind sets on every custom item
    // (seen in the game log as e.g. cooldownGroup=Optional[nw:insipid]).
    private static final String STASIS_CMD = "nw:stasis";
    private static final String INSIPID_CMD = "nw:insipid";
    private static final String ACROPSIS_CMD = "nw:acropsis"; // GUESS - not checked against a real item yet
    private static final String VOID_LEGGINGS_CMD = "nw:voidlegs";
    private static final String BRAXLION_CMD = "nw:braxlion";
    private static final String EASTER_LEGGINGS_CMD = "nw:easter2020legs";

    private final Map<String, TrackedWeapon> weaponsById = new LinkedHashMap<>();
    private final Map<String, CustomItemRule> customRules = new LinkedHashMap<>();

    public static WeaponRegistry createDefault(NewwindUtilsConfig config) {
        WeaponRegistry registry = new WeaponRegistry();

        registry.register(new TrackedWeapon(
                "stasis_upgraded",
                "Stasis+",
                EFFECT_BIND,
                "Bind",
                STASIS_CMD,
                INCREASED_BIND_TIME_LORE,
                config.stasisUpgradedDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.HELD_MAIN_HAND
        ));

        registry.register(new TrackedWeapon(
                "stasis",
                "Stasis",
                EFFECT_BIND,
                "Bind",
                STASIS_CMD,
                null,
                config.stasisDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.HELD_MAIN_HAND
        ));

        registry.register(new TrackedWeapon(
                "insipid_brief_food",
                "Insipid Brief Food",
                "all_food",
                "Food",
                INSIPID_CMD,
                BRIEFLY_BINDS_ALL_FOOD_LORE,
                config.insipidBriefFoodDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.HELD_MAIN_HAND
        ));

        registry.register(new TrackedWeapon(
                "insipid_upgraded",
                "Insipid+",
                "gapple_bind",
                "Gapple",
                INSIPID_CMD,
                INCREASED_BIND_TIME_LORE,
                config.insipidUpgradedDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.HELD_MAIN_HAND
        ));

        registry.register(new TrackedWeapon(
                "insipid",
                "Insipid",
                "gapple_bind",
                "Gapple",
                INSIPID_CMD,
                null,
                config.insipidDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.HELD_MAIN_HAND
        ));

        registry.register(new TrackedWeapon(
                "acropsis_upgraded",
                "Acropsis+",
                EFFECT_ACROPSIS,
                "Acropsis",
                ACROPSIS_CMD,
                INCREASED_BIND_TIME_LORE,
                config.acropsisUpgradedDurationTicks,
                true,
                TrackedWeapon.TriggerType.USE_ENTITY,
                TrackedWeapon.SourceMode.HELD_EITHER
        ));

        registry.register(new TrackedWeapon(
                "acropsis",
                "Acropsis",
                EFFECT_ACROPSIS,
                "Acropsis",
                ACROPSIS_CMD,
                null,
                config.acropsisDurationTicks,
                true,
                TrackedWeapon.TriggerType.USE_ENTITY,
                TrackedWeapon.SourceMode.HELD_EITHER
        ));

        registry.register(new TrackedWeapon(
                "void_leggings_upgraded",
                "Void Leggings+",
                EFFECT_BIND,
                "Bind",
                VOID_LEGGINGS_CMD,
                INCREASED_BIND_TIME_LORE,
                config.voidLeggingsUpgradedDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.LEGS_ARMOR
        ));

        registry.register(new TrackedWeapon(
                "void_leggings",
                "Void Leggings",
                EFFECT_BIND,
                "Bind",
                VOID_LEGGINGS_CMD,
                null,
                config.voidLeggingsDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.LEGS_ARMOR
        ));

        registry.register(new TrackedWeapon(
                "easter_leggings_bind",
                "Easter Leggings",
                EFFECT_BIND,
                "Bind",
                EASTER_LEGGINGS_CMD,
                null,
                config.easterLeggingsBindDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.LEGS_ARMOR
        ));

        registry.register(new TrackedWeapon(
                "easter_leggings_clip",
                "Easter Leggings",
                "elytra_bind",
                "Elytra",
                EASTER_LEGGINGS_CMD,
                null,
                config.easterLeggingsClipDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.LEGS_ARMOR
        ));

        registry.register(new TrackedWeapon(
                "braxlion",
                "Braxlion",
                "gapple_bind",
                "Gapple",
                BRAXLION_CMD,
                null,
                config.braxlionDurationTicks,
                true,
                TrackedWeapon.TriggerType.MELEE_HIT,
                TrackedWeapon.SourceMode.CHEST_ARMOR
        ));

        for (CustomItemRule rule : config.customItems) {
            if (rule.valid()) {
                registry.register(rule.weapon());
                registry.customRules.put(rule.id, rule);
            }
        }
        return registry;
    }

    public void register(TrackedWeapon weapon) {
        weaponsById.put(weapon.id(), weapon);
    }

    public TrackedWeapon findByItem(ItemStack stack) {
        List<TrackedWeapon> matches = findMatchesByItem(stack);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public List<TrackedWeapon> findHandSources(Player player, InteractionHand hand, TrackedWeapon.TriggerType trigger) {
        List<TrackedWeapon> result = new ArrayList<>();
        boolean foundBuiltin = false;
        for (TrackedWeapon weapon : findMatchesByItem(player.getItemInHand(hand))) {
            if (weapon.triggerType()!=trigger || !weapon.isHeldSource() || !weapon.matchesHand(hand)) continue;
            if (customRules.containsKey(weapon.id())) result.add(weapon);
            else if (!foundBuiltin) { result.add(0,weapon); foundBuiltin=true; }
        }
        return result;
    }
    public TrackedWeapon findByHand(Player player, InteractionHand hand, TrackedWeapon.TriggerType trigger) {
        List<TrackedWeapon> sources=findHandSources(player,hand,trigger);
        return sources.isEmpty()?null:sources.get(0);
    }
    private List<TrackedWeapon> findArmorSources(Player player, EquipmentSlot slot, TrackedWeapon.TriggerType trigger) {
        List<TrackedWeapon> result = new ArrayList<>();
        boolean foundBuiltin = false;
        for (TrackedWeapon weapon : findMatchesByItem(player.getItemBySlot(slot))) {
            if (weapon.triggerType()!=trigger || !weapon.isArmorSource() || !weapon.matchesArmorSlot(slot)) continue;
            if (customRules.containsKey(weapon.id())) result.add(weapon);
            else if (!foundBuiltin) {
                result.add(0,weapon); foundBuiltin=true;
                // The same Easter hit applies both effects, even when another rule also matches Bind.
                if (weapon.id().equals("easter_leggings_bind")) result.add(weaponsById.get("easter_leggings_clip"));
            }
        }
        return result;
    }
    public TrackedWeapon findByArmorSlot(Player player, EquipmentSlot slot, TrackedWeapon.TriggerType trigger) {
        List<TrackedWeapon> sources=findArmorSources(player,slot,trigger);
        return sources.isEmpty()?null:sources.get(0);
    }

    public TrackedWeapon findAnyMatchingUseEntityWeapon(Player player) {
        TrackedWeapon main = findByHand(player, InteractionHand.MAIN_HAND, TrackedWeapon.TriggerType.USE_ENTITY);
        if (main != null) {
            return main;
        }

        return findByHand(player, InteractionHand.OFF_HAND, TrackedWeapon.TriggerType.USE_ENTITY);
    }

    public List<TrackedWeapon> findMeleeHitSources(Player player) {
        Map<String, TrackedWeapon> chosenByEffect = new LinkedHashMap<>();

        for (TrackedWeapon w : findHandSources(player, InteractionHand.MAIN_HAND, TrackedWeapon.TriggerType.MELEE_HIT)) considerMeleeSource(chosenByEffect,w);
        for (TrackedWeapon w : findHandSources(player, InteractionHand.OFF_HAND, TrackedWeapon.TriggerType.MELEE_HIT)) considerMeleeSource(chosenByEffect,w);
        for (TrackedWeapon w : findArmorSources(player, EquipmentSlot.CHEST, TrackedWeapon.TriggerType.MELEE_HIT)) considerMeleeSource(chosenByEffect,w);
        for (TrackedWeapon w : findArmorSources(player, EquipmentSlot.LEGS, TrackedWeapon.TriggerType.MELEE_HIT)) considerMeleeSource(chosenByEffect,w);

        return new ArrayList<>(chosenByEffect.values());
    }

    public Collection<TrackedWeapon> getWeapons() {
        return weaponsById.values();
    }

    private List<TrackedWeapon> findMatchesByItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        List<TrackedWeapon> matches = new ArrayList<>();

        for (TrackedWeapon weapon : weaponsById.values()) {
            if (!weapon.enabled()) {
                continue;
            }

            CustomItemRule custom=customRules.get(weapon.id());
            if (custom!=null) {
                if (custom.matches(stack)) matches.add(weapon);
                continue;
            }
            if (!matchesCustomModelData(stack, weapon.requiredCustomModelData())) {
                continue;
            }

            if (!matchesLoreRequirement(stack, weapon.requiredLoreLine())) {
                continue;
            }

            matches.add(weapon);
        }

        matches.sort(Comparator.comparingInt(TrackedWeapon::specificityScore).reversed());
        return matches;
    }

    private static boolean matchesCustomModelData(ItemStack stack, String expectedGroup) {
        if (expectedGroup == null) {
            return true;
        }

        UseCooldown cooldown = stack.get(DataComponents.USE_COOLDOWN);
        if (cooldown == null) {
            return false;
        }

        return cooldown.cooldownGroup()
                .map(id -> id.toString().equals(expectedGroup))
                .orElse(false);
    }

    static boolean matchesLoreRequirement(ItemStack stack, String requiredLoreLine) {
        if (requiredLoreLine == null || requiredLoreLine.isBlank()) {
            return true;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }

        String normalizedRequired = normalizeLoreText(requiredLoreLine);
        for (Component line : lore.lines()) {
            if (line != null && normalizedRequired.equals(normalizeLoreText(line.getString()))) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeLoreText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text
                .replaceAll("(?i)&[0-9A-FK-OR]", "")
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .trim();

        return normalized;
    }

    private static void considerMeleeSource(Map<String, TrackedWeapon> chosenByEffect, TrackedWeapon candidate) {
        if (candidate == null) {
            return;
        }

        TrackedWeapon existing = chosenByEffect.get(candidate.effectId());
        if (existing == null || isPreferredForSameEffect(candidate, existing)) {
            chosenByEffect.put(candidate.effectId(), candidate);
        }
    }

    private static boolean isPreferredForSameEffect(TrackedWeapon candidate, TrackedWeapon existing) {
        if (candidate.sourcePriority() != existing.sourcePriority()) {
            return candidate.sourcePriority() > existing.sourcePriority();
        }

        if (candidate.specificityScore() != existing.specificityScore()) {
            return candidate.specificityScore() > existing.specificityScore();
        }

        return candidate.durationTicks() > existing.durationTicks();
    }
}
