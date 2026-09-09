package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.ItemStack;

import java.util.List;

public class ItemTabs {

	public static CreativeTab MAIN_TAB;
	public static CreativeTab LOCOMOTIVE_TAB;
	public static CreativeTab STOCK_TAB;
    public static CreativeTab PASSENGER_TAB;
	public static CreativeTab MAST_TAB;
	public static CreativeTab WIRE_TAB;
	//public static CreativeTab COMPONENT_TAB;

	static {
		MAIN_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".main", () -> new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1));
		LOCOMOTIVE_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".locomotive", () -> {
			List<ItemStack> items = IRItems.ITEM_ROLLING_STOCK.getItemVariants(LOCOMOTIVE_TAB);
			if (items.size() == 0) {
				return new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1);
			}
			return items.get(0);
		});
		STOCK_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".stock", () -> {
			List<ItemStack> items = IRItems.ITEM_ROLLING_STOCK.getItemVariants(STOCK_TAB);
			if (items.size() == 0) {
				return new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1);
			}
			return items.get(0);
		});
		PASSENGER_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".passenger", () -> {
			List<ItemStack> items = IRItems.ITEM_ROLLING_STOCK.getItemVariants(PASSENGER_TAB);
			if (items.size() == 0) {
				return new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1);
			}
			return items.get(0);
		});

		MAST_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".mast", () -> {
			List<ItemStack> items = IRItems.ITEM_MAST.getItemVariants(MAST_TAB);
			if (items.isEmpty()) {
				return new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1);
			}
			return items.getFirst();
		});

		WIRE_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".wire", () -> {
			List<ItemStack> items = IRItems.ITEM_WIRE.getItemVariants(WIRE_TAB);
			if (items.isEmpty()) {
				return new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1);
			}
			return items.getFirst();
		});

		/*COMPONENT_TAB = new CreativeTab(ImmersiveRailroading.MODID + ".components", () -> {
			List<ItemStack> items = IRItems.ITEM_ROLLING_STOCK_COMPONENT.getItemVariants(COMPONENT_TAB);
			if (items.size() == 0) {
				return new ItemStack(IRItems.ITEM_LARGE_WRENCH, 1);
			}
			return items.get(0);
		});*/
	}
}
