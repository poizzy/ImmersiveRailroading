package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.inventory.FilteredStackHandler;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.FreightDefinition;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Living;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public abstract class Freight extends EntityCoupleableRollingStock {
	@TagField("items")
	public FilteredStackHandler cargoItems = new FilteredStackHandler(0);

	@TagSync
	@TagField("CARGO_ITEMS")
	private int itemCount = 0;

	@TagSync
	@TagField("PERCENT_FULL")
	private int percentFull = 0;

	public abstract int getInventorySize();
	public abstract int getInventoryWidth();

	private Globals globals;
	private LuaValue controlPositionEvent;
	private boolean isLuaLoaded = false;

	@Override
	public FreightDefinition getDefinition() {
		return this.getDefinition(FreightDefinition.class);
	}

	/**
	 *
	 * Lua Implementation
	 *
	 */

	@Override
	public void handleControlPositionEvent(Control<?> control, float val, Map<String, Pair<Boolean, Float>> controlPositions, boolean pressed)
	{
		try {
			ModCore.info(String.format("Control %s changed to %f while %b", control.controlGroup, val, pressed));
			controlPositions.put(control.controlGroup, Pair.of(pressed, val));

			ModCore.info(controlPositions.toString());

			if (!isLuaLoaded) {
				globals = JsePlatform.standardGlobals();

				// Get Lua file from Json
				Identifier script = getDefinition().script;
				Identifier identifier = new Identifier(script.getDomain(), script.getPath());
				InputStream inputStream = identifier.getResourceStream();

				if (inputStream == null) {
					ModCore.error(String.format("File %s does not exist", script.getDomain() + ":" + script.getPath()));
					return;
				}

				String luaScript = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
				if (luaScript == null || luaScript.isEmpty()) {
					ModCore.error("Lua script content is empty | file not found");
					return;
				}

				LuaValue chunk = globals.load(luaScript);
				chunk.call();

				controlPositionEvent = globals.get("controlPositionEvent");
				if (controlPositionEvent.isnil()) {
					ModCore.error("Lua function 'controlPositionEvent' is not defined");
					return;
				}

				isLuaLoaded = true;
				ModCore.info("Lua environment initialized and script loaded successfully");
			}

			LuaValue result = controlPositionEvent.call(LuaValue.valueOf(control.controlGroup), LuaValue.valueOf(val));
			String result_debug = String.valueOf(controlPositionEvent.call(LuaValue.valueOf(control.controlGroup), LuaValue.valueOf(val)));
			ModCore.info("results: " + result_debug);
			if (result.istable()) {
				LuaTable table = result.checktable();
				ModCore.info("Lua return is a table");

				for (LuaValue key : table.keys()) {
					LuaValue value = table.get(key);

					String controlName = key.toString();
					Float newVal = value.tofloat();


					ModCore.info("Key: " + controlName + ", Value: " + newVal);

					// Add to the Java map
					controlPositions.put(controlName, Pair.of(false, newVal));

					ModCore.info(controlPositions.toString());
				}
			} else {
				ModCore.error("Result is not a table. Type: " + result.typename());
			}

		} catch (LuaError e) {
			ModCore.error("LuaError: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			ModCore.error("An unexpected error occurred: " + e.getMessage());
			e.printStackTrace();
			ModCore.info(String.format("Control %s changed to %f", control.controlGroup, val));
			controlPositions.put(control.controlGroup, Pair.of(pressed, val));
		}
	}


	/*
	 * 
	 * EntityRollingStock Overrides
	 */
	
	@Override
	public void onAssemble() {
		super.onAssemble();
		List<ItemStack> extras = cargoItems.setSize(this.getInventorySize());
		if (getWorld().isServer) {
			extras.forEach(stack -> getWorld().dropItem(stack, getPosition()));
			cargoItems.onChanged(slot -> handleMass());
			handleMass();
		}
		initContainerFilter();
	}
	
	@Override
	public void onDissassemble() {
		super.onDissassemble();

		if (getWorld().isServer) {
			for (int i = 0; i < cargoItems.getSlotCount(); i++) {
				ItemStack stack = cargoItems.get(i);
				if (!stack.isEmpty()) {
					getWorld().dropItem(stack.copy(), getPosition());
					stack.setCount(0);
					cargoItems.set(i, stack);
				}
			}
		}
	}

	@Override
	public ClickResult onClick(Player player, Player.Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (!this.isBuilt()) {
			return ClickResult.PASS;
		}

		// See ItemLead.attachToFence
		if (this.getDefinition().acceptsLivestock()) {
			List<Living> leashed = getWorld().getEntities((Living e) -> e.getPosition().distanceTo(player.getPosition()) < 16 && e.isLeashedTo(player), Living.class);
			if (getWorld().isClient && !leashed.isEmpty()) {
				return ClickResult.ACCEPTED;
			}
			if (player.hasPermission(Permissions.BOARD_WITH_LEAD)) {
				for (Living entity : leashed) {
					if (canFitPassenger(entity)) {
						entity.unleash(player);
						this.addPassenger(entity);
						return ClickResult.ACCEPTED;
					}
				}
			}

			if (player.getHeldItem(hand).is(Fuzzy.LEAD)) {
				for (Entity passenger : this.getPassengers()) {
					if (passenger instanceof Living && !passenger.isVillager()) {
						if (getWorld().isServer) {
							Living living = (Living) passenger;
							if (living.canBeLeashedTo(player)) {
								this.removePassenger(living);
								living.setLeashHolder(player);
								player.getHeldItem(hand).shrink(1);
							}
						}
						return ClickResult.ACCEPTED;
					}
				}
			}
		}

		if (player.getHeldItem(hand).isEmpty() || player.getRiding() != this) {
			if (getWorld().isClient || openGui(player)) {
				return ClickResult.ACCEPTED;
			}
		}
		return ClickResult.PASS;
	}

	protected boolean openGui(Player player) {
		if (getInventorySize() == 0) {
			return false;
		}
		if (player.hasPermission(Permissions.FREIGHT_INVENTORY)) {
			GuiTypes.FREIGHT.open(player, this);
		}
		return true;
	}

	/**
	 * Handle mass depending on item count
	 */
	protected void handleMass() {
		int itemInsideCount = 0;
		int stacksWithStuff = 0;
		for (int slot = 0; slot < cargoItems.getSlotCount(); slot++) {
			itemInsideCount += cargoItems.get(slot).getCount();
			if (cargoItems.get(slot).getCount() != 0) {
				stacksWithStuff += 1;
			}
		}
		itemCount = itemInsideCount;
		percentFull = this.getInventorySize() > 0 ? stacksWithStuff * 100 / this.getInventorySize() : 100;
	}
	
	public int getPercentCargoFull() {
		return percentFull;
	}

	protected void initContainerFilter() {
		
	}

	@Override
	public double getWeight() {
		double fLoad = ConfigBalance.blockWeight * itemCount;
		/*
		for (int i = 0; i < cargoItems.getSlotCount(); i++) {
			ItemStack item = Fuzzy.WOOD_PLANK.example();
			item.setCount(64);
			cargoItems.set(i, item);
		}*/

		fLoad = fLoad + super.getWeight();
		return fLoad;
	}

	@Override
	public double getMaxWeight() {
		return super.getMaxWeight() + ConfigBalance.blockWeight * getInventorySize() * 64;
	}
}