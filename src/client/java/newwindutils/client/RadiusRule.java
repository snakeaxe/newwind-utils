package newwindutils.client;

public class RadiusRule {
    public enum Source { HELD, WORN, INVENTORY, ALWAYS }
    public enum Variant { AUTO, BASE, UPGRADED }
    public String itemId="";
    public double baseRadius=0,upgradedRadius=0;
    public boolean enabled=true;
    public Source source=Source.HELD;
    public Variant variant=Variant.AUTO;
    public boolean valid(){
        if(itemId==null || !itemId.matches("[a-z0-9_-]+"))return false;
        if(!Double.isFinite(baseRadius))baseRadius=0;if(!Double.isFinite(upgradedRadius))upgradedRadius=0;
        baseRadius=Math.max(0,Math.min(64,baseRadius));upgradedRadius=Math.max(0,Math.min(64,upgradedRadius));
        if(source==null)source=Source.HELD;if(variant==null)variant=Variant.AUTO;return true;
    }
    public double radius(ItemCatalog.Entry entry,int upgradeIndex){
        boolean upgraded=variant==Variant.UPGRADED;
        if(variant==Variant.AUTO && upgradeIndex>=0 && upgradeIndex<entry.upgrades().size())
            upgraded=ItemCatalog.radiusUpgrade(entry.upgrades().get(upgradeIndex).name());
        return upgraded?upgradedRadius:baseRadius;
    }
}
