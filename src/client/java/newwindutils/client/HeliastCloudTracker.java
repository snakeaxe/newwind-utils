package newwindutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Associates locally fired Heliast projectiles with newly spawned, landed cloud entities. */
public final class HeliastCloudTracker {
    private record Shot(Projectile entity,long started){}
    public record Impact(Vec3 position,long tick){}
    private static ClientLevel session;
    private static boolean wasUsing;
    private static long released=-1000;
    private static final Map<Integer,Shot> shots=new HashMap<>();
    private static final Map<Integer,Impact> impacts=new HashMap<>();
    private static final Set<Integer> pools=new HashSet<>();
    private HeliastCloudTracker(){}
    public static boolean nearImpact(Vec3 cloud,int age,long now,List<Impact> recent){
        return age<=10 && recent.stream().anyMatch(impact->now>=impact.tick() && now-impact.tick()<=10 && cloud.distanceToSqr(impact.position())<=64);
    }
    public static boolean launchWindow(long now,long release,boolean own,boolean unknown,double distanceSquared){
        return now>=release && now-release<=8 && (own || unknown && distanceSquared<=25);
    }
    public static boolean usesFeetGuide(String id){return !id.equals("heliast");}
    public static boolean eligible(AreaEffectCloud cloud){return pools.contains(cloud.getId());}
    public static void tick(Minecraft client){
        if(session!=client.level || client.player==null || !client.player.isAlive()){
            session=client.level;shots.clear();impacts.clear();pools.clear();wasUsing=false;released=-1000;
        }
        if(client.level==null || client.player==null)return;
        long now=client.level.getGameTime();
        boolean using=false;
        if(client.player.isUsingItem()){
            var entry=ItemCatalog.find(client.player.getUseItem());using=entry!=null && entry.id().equals("heliast");
        }
        if(wasUsing && !using)released=now;wasUsing=using;
        // A real projectile is required: holding the bow, or cancelling a draw, creates no pool.
        for(var entity:client.level.entitiesForRendering())if(entity instanceof Projectile projectile && projectile.tickCount<=5 && !projectile.isRemoved()){
            var owner=projectile.getOwner();
            if(launchWindow(now,released,owner==client.player,owner==null,projectile.distanceToSqr(client.player)))
                shots.putIfAbsent(projectile.getId(),new Shot(projectile,now));
        }
        var iterator=shots.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var shot=entry.getValue();
            if(shot.entity().isRemoved()){
                impacts.put(entry.getKey(),new Impact(shot.entity().position(),now));iterator.remove();
            }else if(now-shot.started()>600)iterator.remove();
        }
        impacts.values().removeIf(impact->now-impact.tick()>10);
        Set<Integer> live=new HashSet<>();
        for(var entity:client.level.entitiesForRendering())if(entity instanceof AreaEffectCloud cloud && !cloud.isRemoved()){
            live.add(cloud.getId());
            // Ordinary lingering-potion particles also qualify. No particle setting is read.
            boolean landed=nearImpact(cloud.position(),cloud.tickCount,now,new ArrayList<>(impacts.values()));
            if(!landed && cloud.tickCount<=10)landed=shots.values().stream().anyMatch(shot->shot.entity().position().distanceToSqr(cloud.position())<=16);
            if(landed)pools.add(cloud.getId());
        }
        pools.retainAll(live);
    }
}
