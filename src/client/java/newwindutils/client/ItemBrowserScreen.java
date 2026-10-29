package newwindutils.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.net.URI;
import java.util.*;

/** Compact, category-first wiki browser. */
public class ItemBrowserScreen extends Screen {
    private final Screen parent;
    private final ItemCatalog.Entry dropKey;
    private String query="",category="All",notice="";
    private boolean home=true;
    private int page;
    private final List<Button> resultButtons=new ArrayList<>();
    private final Map<String,ItemStack> icons=new HashMap<>();
    private List<ItemCatalog.Entry> matches=List.of();
    private Button pageLabel;
    private Panel panel;
    record Panel(int x,int y,int width,int height){}
    static Panel layout(int width,int height){int w=Math.min(460,Math.max(240,width-40)),h=Math.min(310,Math.max(190,height-32));return new Panel((width-w)/2,(height-h)/2,w,h);}
    public ItemBrowserScreen(Screen parent){this(parent,null);}
    private ItemBrowserScreen(Screen parent,ItemCatalog.Entry key){
        super(Component.literal(key==null?"Newwind item browser":"Drops: "+key.name()));this.parent=parent;dropKey=key;
        if(key!=null){home=false;category="All";}
    }
    private Button button(String text,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(text,w-8)),b->action.run()).bounds(x,y,w,18).build());}
    private int columns(){return panel.width()>=340?2:1;}
    static List<String> categories(){List<String> result=new ArrayList<>();result.add("All items");ItemCatalog.all().stream().map(ItemCatalog.Entry::category).distinct().sorted().forEach(result::add);return List.copyOf(result);}
    private int rows(){return Math.max(1,(panel.height()-139)/21);}
    @Override protected void init(){
        panel=layout(width,height);resultButtons.clear();icons.clear();int x=panel.x()+10,y=panel.y(),w=panel.width()-20,half=(w-6)/2;
        EditBox search=new EditBox(font,x,y+28,w,18,Component.literal("Search items"));search.setMaxLength(100);search.setHint(Component.literal("Search items or effects..."));search.setValue(query);search.setResponder(v->{query=v;page=0;home=dropKey==null && v.isBlank() && category.equals("All");refresh();});addRenderableWidget(search);
        button(dropKey==null?"Categories":"< Back to key",x,y+50,half,()->{if(dropKey!=null){onClose();return;}query="";home=true;category="All";page=0;clearWidgets();init();});
        button("Look up held item",x+half+6,y+50,half,()->{
            var entry=minecraft.player==null?null:ItemCatalog.find(minecraft.player.getMainHandItem());
            if(entry==null)notice="Not recognized: search its item page > Identify held item.";else minecraft.gui.setScreen(new Detail(this,entry));
        });
        int bottom=y+panel.height();
        button("<",x,bottom-49,42,()->{page--;refresh();});
        pageLabel=button("",x+46,bottom-49,w-92,()->{});pageLabel.active=false;
        button(">",x+w-42,bottom-49,42,()->{page++;refresh();});
        int bw=(w-12)/3;
        button("Prices",x,bottom-25,bw,()->CostCalculatorScreen.openPrices(this));
        button("Radius guides",x+bw+6,bottom-25,bw,()->RadiusSettings.openGeneral(this));
        button(dropKey==null?"Done":"Back",x+2*(bw+6),bottom-25,bw,this::onClose);
        refresh();
    }
    private void refresh(){
        for(Button b:resultButtons)removeWidget(b);resultButtons.clear();
        int perPage=rows()*columns(),w=(panel.width()-20-(columns()-1)*6)/columns();
        if(home){
            List<String> categories=categories();
            int pages=Math.max(1,(categories.size()+perPage-1)/perPage);page=Math.floorMod(page,pages);if(pageLabel!=null)pageLabel.setMessage(Component.literal("Categories "+(page+1)+" / "+pages));
            for(int i=page*perPage;i<Math.min(categories.size(),(page+1)*perPage);i++){
                String name=categories.get(i);long count=name.equals("All items")?ItemCatalog.all().size():ItemCatalog.all().stream().filter(e->e.category().equals(name)).count();
                int local=i%perPage;
                resultButtons.add(button(name+" ("+count+")",panel.x()+10+(local%columns())*(w+6),panel.y()+75+(local/columns())*21,w,()->{category=name.equals("All items")?"All":name;home=false;page=0;refresh();}));
            }
            matches=List.of();return;
        }
        matches=ItemCatalog.search(query,category);
        if(dropKey!=null){var ids=KeyDrops.items(dropKey.id()).stream().map(ItemCatalog.Entry::id).collect(java.util.stream.Collectors.toSet());matches=matches.stream().filter(e->ids.contains(e.id())).toList();}
        int pages=Math.max(1,(matches.size()+perPage-1)/perPage);page=Math.floorMod(page,pages);
        if(pageLabel!=null)pageLabel.setMessage(Component.literal((dropKey==null?category:"Drops")+" "+(page+1)+" / "+pages));
        for(int i=page*perPage;i<Math.min(matches.size(),(page+1)*perPage);i++){
            var entry=matches.get(i);int local=i%perPage,x=panel.x()+10+(local%columns())*(w+6),y=panel.y()+75+(local/columns())*21;
            resultButtons.add(button("    "+font.plainSubstrByWidth(entry.name(),w-32),x,y,w,()->minecraft.gui.setScreen(new Detail(this,entry))));
        }
    }
    private ItemStack icon(ItemCatalog.Entry entry){return icons.computeIfAbsent(entry.id(),id->{
        // Real inventory stacks retain their exact armor trim, colors, model and enchantments.
        if(minecraft.player!=null)for(int i=0;i<minecraft.player.getInventory().getContainerSize();i++){
            ItemStack stack=minecraft.player.getInventory().getItem(i);var match=ItemCatalog.find(stack);
            if(match!=null && match.id().equals(entry.id()))return stack.copyWithCount(1);
        }
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(entry.materialId())).orElse(Items.BOOK));
    });}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){
        g.fill(panel.x(),panel.y(),panel.x()+panel.width(),panel.y()+panel.height(),0xF00A111B);
        g.outline(panel.x(),panel.y(),panel.width(),panel.height(),0xFF506780);
        super.extractRenderState(g,mouseX,mouseY,tick);g.centeredText(font,title,width/2,panel.y()+9,0xFFFFFFFF);
        if(!home)for(int i=0;i<resultButtons.size();i++){
            var entry=matches.get(page*rows()*columns()+i);Button b=resultButtons.get(i);g.fakeItem(icon(entry),b.getX()+2,b.getY()+1);
            if(b.isMouseOver(mouseX,mouseY))g.setComponentTooltipForNextFrame(font,List.of(Component.literal(entry.name()),Component.literal(entry.category()+" / "+entry.material())),mouseX,mouseY);
        }
        String text=!ItemCatalog.error.isEmpty()?"Catalog error: "+ItemCatalog.error:!notice.isEmpty()?notice:home?"Choose a category or search above":dropKey!=null?(KeyDrops.items(dropKey.id()).isEmpty()?"No drops documented in this wiki snapshot":matches.size()+" wiki-listed drops / odds not published"):matches.size()+" items / "+category;
        g.centeredText(font,font.plainSubstrByWidth(text,panel.width()-20),width/2,panel.y()+panel.height()-62,0xFFBBBBBB);
    }
    private static class Detail extends Screen {
        private final Screen parent;private final ItemCatalog.Entry entry;private int page;private List<FormattedCharSequence> lines;private String notice="";
        Detail(Screen parent,ItemCatalog.Entry entry){super(Component.literal(entry.name()));this.parent=parent;this.entry=entry;}
        private int count(){return Math.max(1,(height-140)/12);}
        @Override protected void init(){
            List<String> paragraphs=new ArrayList<>();
            paragraphs.add(entry.summary());paragraphs.add("");
            paragraphs.add("Category: "+entry.category()+" / "+entry.type());paragraphs.add("Material: "+entry.material());
            if(!entry.years().isBlank())paragraphs.add("Years: "+entry.years());
            paragraphs.add("Base enchantments: Sharpness "+entry.baseSharpness()+" / Unbreaking "+entry.baseUnbreaking());
            for(String effect:entry.effects())paragraphs.add("Effect: "+effect);
            paragraphs.add("");paragraphs.add("UPGRADE RECIPES");
            if(entry.upgrades().isEmpty())paragraphs.add(entry.detailsAvailable()?"No upgrade recipe listed on this wiki page.":"Detailed wiki page is unavailable.");
            for(var upgrade:entry.upgrades()){
                paragraphs.add(upgrade.name());paragraphs.add("Materials: "+upgrade.costText());
                if(!upgrade.details().isBlank())paragraphs.add(upgrade.details());
            }
            if(entry.areaEffect()){paragraphs.add("");paragraphs.add(entry.id().equals("heliast")?"Radius outlines follow your landed potion clouds using the server radius. No feet guide while holding the bow.":"Area effect: numeric radius is not published in the inspected wiki. Use Radius to enter tested base/upgraded values.");}
            paragraphs.add("");paragraphs.add("Source: "+entry.source());
            lines=new ArrayList<>();for(String p:paragraphs)if(p.isEmpty())lines.add(FormattedCharSequence.EMPTY);else lines.addAll(font.split(Component.literal(p),width-24));
            int pages=Math.max(1,(lines.size()+count()-1)/count());page=Math.max(0,Math.min(page,pages-1));
            if(pages>1){
                addRenderableWidget(Button.builder(Component.literal("< Prev"),b->{page=(page-1+pages)%pages;clearWidgets();init();}).bounds(width/2-120,height-54,72,20).build());
                Button label=addRenderableWidget(Button.builder(Component.literal((page+1)+" / "+pages),b->{}).bounds(width/2-44,height-54,88,20).build());label.active=false;
                addRenderableWidget(Button.builder(Component.literal("Next >"),b->{page=(page+1)%pages;clearWidgets();init();}).bounds(width/2+48,height-54,72,20).build());
            }
            int identifyWidth=KeyDrops.isKey(entry)?(width-28)/2:width-24;
            addRenderableWidget(Button.builder(Component.literal("Identify held item as "+entry.name()),b->{
                if(minecraft.player!=null){notice=CatalogIdentity.register(minecraft.player.getMainHandItem(),entry,NewwindUtilsConfigManager.get());NewwindUtilsConfigManager.save();clearWidgets();init();}
            }).bounds(12,height-80,identifyWidth,20).build());
            if(KeyDrops.isKey(entry))addRenderableWidget(Button.builder(Component.literal("View drops ("+KeyDrops.items(entry.id()).size()+")"),b->minecraft.gui.setScreen(new ItemBrowserScreen(this,entry))).bounds(16+identifyWidth,height-80,identifyWidth,20).build());
            int bw=(width-36)/4;
            addRenderableWidget(Button.builder(Component.literal("Costs"),b->minecraft.gui.setScreen(new CostCalculatorScreen(this,entry))).bounds(12,height-28,bw,20).build());
            addRenderableWidget(Button.builder(Component.literal("Radius"),b->RadiusSettings.open(this,entry)).bounds(16+bw,height-28,bw,20).build());
            addRenderableWidget(Button.builder(Component.literal("Wiki"),ConfirmLinkScreen.confirmLink(this,URI.create(entry.source()))).bounds(20+2*bw,height-28,bw,20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(24+3*bw,height-28,bw,20).build());
        }
        @Override public void onClose(){minecraft.gui.setScreen(parent);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){
            super.extractRenderState(g,x,y,tick);g.centeredText(font,font.plainSubstrByWidth(entry.name(),width-24),width/2,12,0xFFFFFFFF);
            g.centeredText(font,font.plainSubstrByWidth(notice.isEmpty()?"Wiki snapshot "+ItemCatalog.retrieved:notice,width-24),width/2,30,0xFFBBBBBB);
            for(int i=page*count();i<Math.min(lines.size(),(page+1)*count());i++)g.text(font,lines.get(i),12,48+(i%count())*12,0xFFFFFFFF,false);
        }
    }
}
