package newwindutils.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

public record TrackedWeapon(
        String id,
        String displayName,
        String effectId,
        String effectDisplayName,
        String requiredCustomModelData,
        String requiredLoreLine,
        int durationTicks,
        boolean enabled,
        TriggerType triggerType,
        SourceMode sourceMode
) {
    public float durationSeconds() {
        return durationTicks / 20.0F;
    }

    public boolean hasCustomModelDataRequirement() {
        return requiredCustomModelData != null;
    }

    public boolean hasLoreRequirement() {
        return requiredLoreLine != null && !requiredLoreLine.isBlank();
    }

    public int specificityScore() {
        int score = 0;
        if (hasCustomModelDataRequirement()) {
            score++;
        }
        if (hasLoreRequirement()) {
            score++;
        }
        return score;
    }

    public boolean isHeldSource() {
        return sourceMode == SourceMode.HELD_MAIN_HAND
                || sourceMode == SourceMode.HELD_OFF_HAND
                || sourceMode == SourceMode.HELD_EITHER;
    }

    public boolean isArmorSource() {
        return sourceMode == SourceMode.CHEST_ARMOR || sourceMode == SourceMode.LEGS_ARMOR;
    }

    public int sourcePriority() {
        return isArmorSource() ? 2 : 1;
    }

    public boolean matchesHand(InteractionHand hand) {
        return sourceMode == SourceMode.HELD_EITHER
                || (sourceMode == SourceMode.HELD_MAIN_HAND && hand == InteractionHand.MAIN_HAND)
                || (sourceMode == SourceMode.HELD_OFF_HAND && hand == InteractionHand.OFF_HAND);
    }

    public boolean matchesArmorSlot(EquipmentSlot slot) {
        return (sourceMode == SourceMode.CHEST_ARMOR && slot == EquipmentSlot.CHEST)
                || (sourceMode == SourceMode.LEGS_ARMOR && slot == EquipmentSlot.LEGS);
    }

    public enum TriggerType {
        MELEE_HIT,
        USE_ENTITY
    }

    public enum SourceMode {
        HELD_MAIN_HAND,
        HELD_OFF_HAND,
        HELD_EITHER,
        CHEST_ARMOR,
        LEGS_ARMOR
    }
}
