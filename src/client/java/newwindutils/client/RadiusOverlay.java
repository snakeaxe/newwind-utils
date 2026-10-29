package newwindutils.client;

import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import java.util.*;

public final class RadiusOverlay {
    public record Point(double x,double y,double z,boolean valid){}
    public record Ring(Vec3 origin,List<Point> points,int color,float width,boolean followsPlayer){public Ring{points=List.copyOf(points);}public Ring(Vec3 origin,List<Point> points,int color,float width){this(origin,points,color,width,true);}}
    private static final RenderStateDataKey<List<Ring>> KEY=RenderStateDataKey.create(()->"newwindutils:radius-guides");
    private static ClientLevel session;
    private static List<Ring> snapshot=List.of();
    private RadiusOverlay(){}
    public static void register(){
        LevelExtractionEvents.END_EXTRACTION.register(ctx->{
            Minecraft client=Minecraft.getInstance();List<Ring> rings=List.of();
            if(client.player!=null && ctx.level()==session && !client.gui.hud.isHidden() && NewwindUtilsConfigManager.get().showRadiusGuides){
                Vec3 position=client.player.getPosition(ctx.deltaTracker().getGameTimeDeltaPartialTick(false));
                List<Ring> interpolated=new ArrayList<>();
                for(Ring ring:snapshot){if(!ring.followsPlayer()){interpolated.add(ring);continue;}double dx=position.x-ring.origin().x,dz=position.z-ring.origin().z;
                    interpolated.add(new Ring(position,ring.points().stream().map(p->new Point(p.x()+dx,p.y(),p.z()+dz,p.valid())).toList(),ring.color(),ring.width()));}
                rings=List.copyOf(interpolated);
            }
            ((FabricRenderState)(Object)ctx.levelState()).setData(KEY,rings);
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx->{
            var rings=((FabricRenderState)(Object)ctx.levelState()).getDataOrDefault(KEY,List.of());
            Vec3 camera=ctx.levelState().cameraRenderState.pos;
            for(Ring ring:rings)ctx.submitNodeCollector().submitCustomGeometry(ctx.poseStack(),RenderTypes.linesTranslucentNoDepthWrite(),(pose,buffer)->{
                for(int i=0;i<ring.points().size()-1;i++){
                    Point a=ring.points().get(i),b=ring.points().get(i+1);
                    if(!a.valid() || !b.valid() || Math.abs(a.y()-b.y())>2)continue;
                    float nx=(float)(b.x()-a.x()),ny=(float)(b.y()-a.y()),nz=(float)(b.z()-a.z());
                    float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(length==0)continue;nx/=length;ny/=length;nz/=length;
                    buffer.addVertex(pose,(float)(a.x()-camera.x),(float)(a.y()-camera.y),(float)(a.z()-camera.z)).setColor(ring.color()).setNormal(pose,nx,ny,nz).setLineWidth(ring.width());
                    buffer.addVertex(pose,(float)(b.x()-camera.x),(float)(b.y()-camera.y),(float)(b.z()-camera.z)).setColor(ring.color()).setNormal(pose,nx,ny,nz).setLineWidth(ring.width());
                }
            });
        });
    }
    public static List<Point> flatCircle(Vec3 center,double radius,int segments){
        List<Point> points=new ArrayList<>();
        for(int i=0;i<=segments;i++){double angle=i*Math.PI*2/segments;points.add(new Point(center.x+Math.cos(angle)*radius,center.y,center.z+Math.sin(angle)*radius,true));}
        return List.copyOf(points);
    }
    private static Ring groundRing(Minecraft client,Vec3 center,double radius,int color,float width,boolean follows){
        List<Point> points=new ArrayList<>();
        for(Point p:flatCircle(center,radius,64)){
            var hit=client.level.clip(new ClipContext(new Vec3(p.x(),center.y+1.5,p.z()),new Vec3(p.x(),center.y-4,p.z()),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,client.player));
            points.add(new Point(p.x(),hit.getLocation().y+0.035,p.z(),hit.getType()!=HitResult.Type.MISS));
        }
        return new Ring(center,points,color,width,follows);
    }
    public static void tick(Minecraft client){
        HeliastCloudTracker.tick(client);
        if(session!=client.level){session=client.level;snapshot=List.of();}
        if(client.player==null || client.level==null){snapshot=List.of();return;}
        var config=NewwindUtilsConfigManager.get();
        if(!config.showRadiusGuides || !client.player.isAlive()){snapshot=List.of();return;}
        if(client.level.getGameTime()%2!=0)return;
        List<Ring> rings=new ArrayList<>();Vec3 center=client.player.position();
        int color=(config.radiusOpacity<<24)|(config.radiusRed<<16)|(config.radiusGreen<<8)|config.radiusBlue;
        Map<RadiusRule.Source,List<ItemStack>> stacks=new EnumMap<>(RadiusRule.Source.class);
        stacks.put(RadiusRule.Source.HELD,List.of(client.player.getMainHandItem(),client.player.getOffhandItem()));
        stacks.put(RadiusRule.Source.WORN,List.of(client.player.getItemBySlot(EquipmentSlot.HEAD),client.player.getItemBySlot(EquipmentSlot.CHEST),client.player.getItemBySlot(EquipmentSlot.LEGS),client.player.getItemBySlot(EquipmentSlot.FEET)));
        for(RadiusRule rule:config.radiusRules){
            if(!rule.enabled || !HeliastCloudTracker.usesFeetGuide(rule.itemId))continue;
            if(rule.source==RadiusRule.Source.INVENTORY && !stacks.containsKey(rule.source)){
                List<ItemStack> inventory=new ArrayList<>();for(int i=0;i<client.player.getInventory().getContainerSize();i++)inventory.add(client.player.getInventory().getItem(i));stacks.put(rule.source,inventory);
            }
            ItemCatalog.Entry entry=ItemCatalog.byId(rule.itemId);if(entry==null)continue;
            double radius=rule.source==RadiusRule.Source.ALWAYS?rule.radius(entry,-1):0;
            for(ItemStack stack:stacks.getOrDefault(rule.source,List.of())){
                var match=ItemCatalog.find(stack);
                if(match!=null && match.id().equals(rule.itemId))radius=Math.max(radius,rule.radius(entry,ItemCatalog.heldState(stack,entry).upgradeIndex()));
            }
            // Zero means unknown, never a guessed radius.
            if(radius<=0)continue;
            rings.add(groundRing(client,center,radius,color,config.radiusLineWidth,true));
        }
        if(config.showDragonBreathClouds){
            int count=0;
            for(var entity:client.level.entitiesForRendering()){
                if(entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud && !cloud.isRemoved() && !cloud.isWaiting()
                        && cloud.distanceToSqr(client.player)<4096 && HeliastCloudTracker.eligible(cloud)){
                    double radius=cloud.getRadius();
                    if(Double.isFinite(radius) && radius>0 && radius<=64){
                        rings.add(groundRing(client,cloud.position(),radius,color,config.radiusLineWidth,false));
                        if(++count>=32)break;
                    }
                }
            }
        }
        snapshot=List.copyOf(rings);
    }
}
