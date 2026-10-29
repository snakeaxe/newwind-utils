package newwindutils.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;
import java.util.UUID;

/** A user-authored rule. Captured identity is separate from the estimated effect duration. */
public class CustomItemRule {
    public String id = "user_" + UUID.randomUUID();
    public String itemId = "";
    public String itemName = "";
    public String cooldownGroup;
    public String requiredLore = "";
    public String label = "Cooldown";
    public int durationTicks = 200;
    public Effect effect = Effect.CUSTOM;
    public TrackedWeapon.SourceMode source = TrackedWeapon.SourceMode.HELD_MAIN_HAND;
    public TrackedWeapon.TriggerType trigger = TrackedWeapon.TriggerType.MELEE_HIT;
    public boolean enabled = true;

    public enum Effect { BIND, ELYTRA, FOOD, GAPPLE, ALL_FOOD, CUSTOM }
    public static CustomItemRule capture(ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Hold an item first");
        CustomItemRule rule = new CustomItemRule();
        rule.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        rule.itemName = stack.getHoverName().getString();
        rule.label = rule.itemName;
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            if (equippable.slot() == net.minecraft.world.entity.EquipmentSlot.LEGS) rule.source=TrackedWeapon.SourceMode.LEGS_ARMOR;
            if (equippable.slot() == net.minecraft.world.entity.EquipmentSlot.CHEST) rule.source=TrackedWeapon.SourceMode.CHEST_ARMOR;
        }
        UseCooldown cooldown = stack.get(DataComponents.USE_COOLDOWN);
        rule.cooldownGroup = cooldown == null ? null : cooldown.cooldownGroup().map(Object::toString).orElse(null);
        return rule;
    }
    public CustomItemRule copy() {
        CustomItemRule r = new CustomItemRule();
        r.id=id; r.itemId=itemId; r.itemName=itemName; r.cooldownGroup=cooldownGroup;
        r.requiredLore=requiredLore; r.label=label; r.durationTicks=durationTicks;
        r.effect=effect; r.source=source; r.trigger=trigger; r.enabled=enabled;
        return r;
    }
    public boolean valid() {
        if (id==null || !id.startsWith("user_") || itemId==null || itemId.isBlank()
                || itemName==null || label==null || label.isBlank() || effect==null || source==null || trigger==null) return false;
        durationTicks=Math.max(1,Math.min(6000,durationTicks));
        label=label.trim(); if (label.length()>80) label=label.substring(0,80);
        if (requiredLore==null) requiredLore="";
        if (source==TrackedWeapon.SourceMode.CHEST_ARMOR || source==TrackedWeapon.SourceMode.LEGS_ARMOR)
            trigger=TrackedWeapon.TriggerType.MELEE_HIT;
        return true;
    }
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) return false;
        if (cooldownGroup != null && !cooldownGroup.isBlank()) {
            UseCooldown cooldown=stack.get(DataComponents.USE_COOLDOWN);
            if (cooldown==null || !cooldown.cooldownGroup().map(Object::toString).orElse("").equals(cooldownGroup)) return false;
        } else if (!stack.getHoverName().getString().equals(itemName)) return false;
        return WeaponRegistry.matchesLoreRequirement(stack, requiredLore);
    }
    public TrackedWeapon weapon() {
        String effectId=switch(effect) {
            case BIND -> "bind"; case ELYTRA -> "elytra_bind"; case FOOD -> "food_bind";
            case GAPPLE -> "gapple_bind"; case ALL_FOOD -> "all_food"; case CUSTOM -> "custom_"+id;
        };
        String display=switch(effect) {
            case BIND -> "Bind"; case ELYTRA -> "Elytra"; case FOOD, ALL_FOOD -> "Food";
            case GAPPLE -> "Gapple"; case CUSTOM -> label;
        };
        return new TrackedWeapon(id,label,effectId,display,cooldownGroup,requiredLore,durationTicks,enabled,trigger,source);
    }
}
