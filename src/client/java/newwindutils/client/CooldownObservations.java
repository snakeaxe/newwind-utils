package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Authoritative local item cooldowns and explicitly user-confirmed stopwatch measurements. */
public final class CooldownObservations {
    public record Sample(String label, int ticks, boolean serverReported, String group) {}
    public record Candidate(String label, CustomItemRule rule) {}
    private static final Map<String,ActiveEffectTimer> active = new LinkedHashMap<>();
    private static final Map<String,Set<EffectKind>> kinds = new LinkedHashMap<>();
    private static final EnumMap<EffectKind,Long> authority = new EnumMap<>(EffectKind.class);
    private static final List<Sample> samples = new ArrayList<>();
    private static List<Candidate> lastHitItems = List.of();
    private static ClientLevel session;
    private static boolean armed;
    private static long measuringSince = -1;
    private static boolean repeatedHit;
    private CooldownObservations() {}

    private static void ensureWorld(Minecraft client) {
        if (session != client.level) {
            session=client.level; active.clear(); kinds.clear(); authority.clear(); samples.clear(); lastHitItems=List.of();
            armed=false; measuringSince=-1; repeatedHit=false;
        }
    }
    public static void tick(Minecraft client) {
        ensureWorld(client);
        if (client.level==null || client.player==null) return;
        active.entrySet().removeIf(e -> e.getValue().remainingTicks(client.level.getGameTime())==0);
        kinds.keySet().retainAll(active.keySet());
        clearStalePredictions(client);
        if (!client.player.isAlive()) { active.clear(); kinds.clear(); authority.clear(); armed=false; measuringSince=-1; }
    }
    public static void serverCooldown(ClientboundCooldownPacket packet) {
        Minecraft client=Minecraft.getInstance(); ensureWorld(client);
        if (client.level==null || client.player==null) return;
        String group=packet.cooldownGroup().toString();
        if (packet.duration()<=0) { recordServer(group,"",0,client.level.getGameTime());clearStalePredictions(client);return; }
        Set<EffectKind> affected=EnumSet.noneOf(EffectKind.class);
        String label=packet.cooldownGroup().getPath();
        for(int i=0;i<client.player.getInventory().getContainerSize();i++) {
            ItemStack stack=client.player.getInventory().getItem(i);
            if (!stack.isEmpty() && client.player.getCooldowns().getCooldownGroup(stack).equals(packet.cooldownGroup())) {
                label=stack.getHoverName().getString();
                EffectKind kind=EffectKind.ofCooldown(group,label,stack);
                if(kind!=EffectKind.OTHER)affected.add(kind);
            }
        }
        if(affected.isEmpty())affected.add(EffectKind.ofCooldownName(group+" "+label));
        recordServer(group,label,packet.duration(),client.level.getGameTime(),affected);
        clearStalePredictions(client);
    }
    // Kept independent of packet/client objects to make exact duration/reset behavior verifiable.
    static void recordServer(String group, String label, int ticks, long now) {
        recordServer(group,label,ticks,now,Set.of(EffectKind.ofCooldownName(group+" "+label)));
    }
    static void recordServer(String group,String label,int ticks,long now,Set<EffectKind> affected){
        if (ticks<=0) { active.remove(group);kinds.remove(group);return; }
        kinds.put(group,Set.copyOf(affected));
        for(EffectKind kind:affected)if(kind!=EffectKind.OTHER)authority.put(kind,now);
        active.put(group,new ActiveEffectTimer("server:"+group,"Server: "+label,ticks,now+ticks,null));
        remember(new Sample(label+" ["+group+"]",ticks,true,group));
    }
    private static void remember(Sample sample) {
        samples.add(0,sample);
        if(samples.size()>24) samples.remove(samples.size()-1);
    }
    public static List<Sample> samples() { return List.copyOf(samples); }
    public static List<Candidate> lastHitItems() { return List.copyOf(lastHitItems); }
    public static List<ActiveEffectTimer> activeTimers(long now) {
        return active.values().stream().filter(t->t.remainingTicks(now)>0).toList();
    }
    public static List<ActiveEffectTimer> selfTimers(List<ActiveEffectTimer> predicted,NewwindUtilsConfig config,long now){
        List<SelfEffectSummary.Evidence> evidence=new ArrayList<>();
        if(config.showServerCooldowns) for(var entry:active.entrySet()) for(EffectKind kind:kinds.getOrDefault(entry.getKey(),Set.of()))
            evidence.add(new SelfEffectSummary.Evidence(kind,entry.getValue()));
        List<ActiveEffectTimer> result=SelfEffectSummary.summarize(predicted,evidence,
                config.showServerCooldowns?authority:Map.of(),config,now);
        if(config.showRawSelfCooldowns && config.showServerCooldowns)result.addAll(activeTimers(now));
        return result;
    }
    private static void clearStalePredictions(Minecraft client){
        if(client.player==null || client.level==null)return;
        long now=client.level.getGameTime();
        for(var entry:authority.entrySet()){
            EffectKind kind=entry.getKey();
            boolean stillActive=active.entrySet().stream().anyMatch(e->e.getValue().remainingTicks(now)>0 && kinds.getOrDefault(e.getKey(),Set.of()).contains(kind));
            if(!stillActive)NewwindUtilsClient.getTimerManager().clearPredictedBefore(client.player,kind,entry.getValue()+8);
        }
    }
    public static void incomingHit(ClientboundDamageEventPacket packet) {
        Minecraft client=Minecraft.getInstance(); ensureWorld(client);
        if(client.level==null || client.player==null || packet.entityId()!=client.player.getId()) return;
        Entity source=client.level.getEntity(packet.sourceCauseId());
        if(!(source instanceof Player)) source=client.level.getEntity(packet.sourceDirectId());
        if(!(source instanceof Player attacker)) return;
        if(measuringSince>=0) repeatedHit=true;
        // These are candidates, not proof that a particular piece of armor caused the effect.
        if(measuringSince<0) {
            List<Candidate> captured=new ArrayList<>();
            capture(captured,attacker,"Main hand",attacker.getMainHandItem(),TrackedWeapon.SourceMode.HELD_MAIN_HAND);
            capture(captured,attacker,"Chest",attacker.getItemBySlot(EquipmentSlot.CHEST),TrackedWeapon.SourceMode.CHEST_ARMOR);
            capture(captured,attacker,"Legs",attacker.getItemBySlot(EquipmentSlot.LEGS),TrackedWeapon.SourceMode.LEGS_ARMOR);
            lastHitItems=List.copyOf(captured);
        }
        if(armed) {
            armed=false; measuringSince=client.level.getGameTime(); repeatedHit=false;
            message(client,"Measurement started. Press L when the effect ends. Another hit can refresh it.");
        }
    }
    private static void capture(List<Candidate> result, Player attacker,String slot,ItemStack stack,TrackedWeapon.SourceMode mode) {
        if(stack.isEmpty()) return;
        CustomItemRule rule=CustomItemRule.capture(stack.copy()); rule.source=mode;
        result.add(new Candidate(attacker.getName().getString()+" / "+slot+": "+rule.itemName,rule));
    }
    public static void measureKey(Minecraft client) {
        ensureWorld(client);
        if(client.level==null || client.player==null) return;
        if(measuringSince>=0) {
            int ticks=(int)Math.min(Integer.MAX_VALUE,Math.max(1,client.level.getGameTime()-measuringSince));
            remember(new Sample("Stopwatch"+(repeatedHit?" (extra hits; may include refreshes)":" (user timed)"),ticks,false,null));
            measuringSince=-1;
            message(client,String.format(Locale.US,"Measured %.2fs. Open Custom Items > Last incoming hit to save a rule.",ticks/20.0));
        } else if(armed) { armed=false; message(client,"Measurement cancelled."); }
        else { armed=true; message(client,"Armed: next player hit starts timing. Press L again to cancel."); }
    }
    private static void message(Minecraft client,String text) {
        client.gui.hud.setOverlayMessage(Component.literal(text),false);
    }
}
