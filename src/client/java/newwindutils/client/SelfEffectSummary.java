package newwindutils.client;

import java.util.*;

public final class SelfEffectSummary {
    public record Evidence(EffectKind kind,ActiveEffectTimer timer) {}
    private SelfEffectSummary(){}
    public static List<ActiveEffectTimer> summarize(List<ActiveEffectTimer> predicted,List<Evidence> received,
                                                   Map<EffectKind,Long> authority,NewwindUtilsConfig config,long now){
        EnumMap<EffectKind,ActiveEffectTimer> exact=new EnumMap<>(EffectKind.class),estimated=new EnumMap<>(EffectKind.class);
        List<ActiveEffectTimer> other=new ArrayList<>();
        for(Evidence evidence:received){
            if(evidence.kind()!=EffectKind.OTHER && evidence.timer().remainingTicks(now)>0)
                keepLongest(exact,evidence.kind(),evidence.timer());
        }
        for(ActiveEffectTimer timer:predicted){
            if(timer.remainingTicks(now)<=0)continue;
            EffectKind kind=EffectKind.ofEffect(timer.effectId());
            if(kind==EffectKind.OTHER){if(kind.visible(config,true))other.add(timer);continue;}
            // A server clear/expiry must not reveal a stale estimate for the same effect.
            Long started=authority.get(kind);
            if(started!=null && timer.expiresAtGameTime()-timer.durationTicks()<=started+8)continue;
            keepLongest(estimated,kind,timer);
        }
        List<ActiveEffectTimer> result=new ArrayList<>();
        for(EffectKind kind:EffectKind.values()){
            if(kind==EffectKind.OTHER || !kind.visible(config,true))continue;
            ActiveEffectTimer timer=exact.getOrDefault(kind,estimated.get(kind));
            if(timer!=null)result.add(new ActiveEffectTimer("self:"+kind.id,kind.selfLabel,timer.durationTicks(),timer.expiresAtGameTime(),timer.appliedByUuid()));
        }
        result.addAll(other);return result;
    }
    private static void keepLongest(Map<EffectKind,ActiveEffectTimer> map,EffectKind kind,ActiveEffectTimer timer){
        ActiveEffectTimer old=map.get(kind);
        if(old==null || timer.expiresAtGameTime()>old.expiresAtGameTime())map.put(kind,timer);
    }
    public static List<ActiveEffectTimer> preview(List<ActiveEffectTimer> timers,NewwindUtilsConfig config){
        return summarize(timers,List.of(),Map.of(),config,0);
    }
}
