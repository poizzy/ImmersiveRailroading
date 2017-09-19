package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.AssemblyStep;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.net.BuildableStockSyncPacket;
import cam72cam.immersiverailroading.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class EntityBuildableRollingStock extends EntityRollingStock {
	private boolean isBuilt = false;
	private List<ItemComponentType> builtItems;
	public EntityBuildableRollingStock(World world, String defID) {
		super(world, defID);
	}
	//TODO PACKET
	@Override
	public void readSpawnData(ByteBuf additionalData) {
		super.readSpawnData(additionalData);
		isBuilt = additionalData.readBoolean();
		builtItems = BufferUtil.readItemComponentTypes(additionalData);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		super.writeSpawnData(buffer);
		buffer.writeBoolean(isBuilt);
		BufferUtil.writeItemComponentTypes(buffer, builtItems);
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setBoolean("isBuilt", this.isBuilt);
		
		int[] items = new int[builtItems.size()];
		for (int i = 0; i < items.length; i ++) {
			items[i] = builtItems.get(i).ordinal();
		}
		
		nbt.setIntArray("builtItems", items);
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		isBuilt = nbt.getBoolean("isBuilt");
		builtItems = new ArrayList<ItemComponentType>();
		
		int[] items = nbt.getIntArray("builtItems");
		
		for (int i = 0; i < items.length; i++) {
			builtItems.add(ItemComponentType.values()[items[i]]);
		}
	}
	
	public void setComponents(List<ItemComponentType> items) {
		this.builtItems = new ArrayList<ItemComponentType>(items);
		this.isBuilt = getMissingItemComponents().isEmpty();
		if (!world.isRemote) {
			this.sendToObserving(new BuildableStockSyncPacket(this));
		}
	}
	
	public void setComponents(boolean isBuilt, List<ItemComponentType> items) {
		this.builtItems = new ArrayList<ItemComponentType>(items);
		this.isBuilt = isBuilt;
	}
	
	public List<ItemComponentType> getItemComponents() {
		return builtItems;
	}
	
	public boolean isBuilt() {
		return this.isBuilt;
	}
	
	public List<ItemComponentType> getMissingItemComponents() {
		List<ItemComponentType> missing = new ArrayList<ItemComponentType>();
		if (this.isBuilt) {
			return missing;
		}
		
		missing.addAll(this.getDefinition().getItemComponents());
		
		for (ItemComponentType item : this.getItemComponents()) {
			// Remove first occurrence
			missing.remove(item);
		}
		
		return missing;
	}

	public boolean hasAllWheels() {
		for (ItemComponentType item : this.getMissingItemComponents()) {
			if (item.isWheelPart()) {
				return false;
			}
		}
		return true;
	}
	
	public void addComponent(ItemComponentType item) {
		System.out.println("Added: " + item);
		this.builtItems.add(item);
		this.isBuilt = getMissingItemComponents().isEmpty();
		this.sendToObserving(new BuildableStockSyncPacket(this));
	}
	
	public void addNextComponent(EntityPlayer player) {
		if (this.isBuilt()) {
			return;
		}
		
		List<ItemComponentType> toAdd = new ArrayList<ItemComponentType>();
		
		for (AssemblyStep step : AssemblyStep.values()) {
			for (ItemComponentType component : this.getMissingItemComponents()) {
				if (component.step == step) {
					toAdd.add(component);
				}
			}
			if (toAdd.size() != 0) {
				break;
			}
		}
		
		boolean addedComponents = false;
		for (int i = 0; i < player.inventory.getSizeInventory(); i ++) {
			ItemStack found = player.inventory.getStackInSlot(i);
			if (ItemRollingStockComponent.defFromStack(found).equals(this.defID)) {
				ItemComponentType type = ItemRollingStockComponent.typeFromStack(found);
				if (toAdd.contains(type)) {
					addComponent(type);
					player.inventory.decrStackSize(i, 1);
					addedComponents = true;
					break;
				}
			}
		}
		
		if (!addedComponents) {
			String comStr = "";
			for (ItemComponentType component : toAdd) {
				comStr += component.prettyString() + ", ";
			}
			player.sendMessage(new TextComponentString("Missing: " + comStr));
		}
	}
	
	public ItemComponentType removeNextComponent(EntityPlayer player) {
		this.isBuilt = false;
		if (this.builtItems.size() <= 1) {
			return null;
		}
		
		ItemComponentType toRemove = null;
		
		for (AssemblyStep step : AssemblyStep.reverse()) {
			for (ItemComponentType component : this.builtItems) {
				if (component == ItemComponentType.FRAME) {
					continue;
				}
				if (component.step == step) {
					toRemove = component;
					break;
				}
			}
			if (toRemove != null) {
				break;
			}
		}
		
		this.builtItems.remove(toRemove);
		System.out.println("Removed: " + toRemove);
		this.sendToObserving(new BuildableStockSyncPacket(this));
		
		
		ItemStack item = new ItemStack(ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT, 1, 0);
		item.setTagCompound(ItemRollingStockComponent.nbtFromDef(this.defID, toRemove));
		world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, item));
		
		return toRemove;
	}
	
	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (world.isRemote) {
			return false;
		}
		if (player.getHeldItemMainhand().getItem() == ImmersiveRailroading.ITEM_LARGE_WRENCH || player.getHeldItemMainhand().getItem() == ImmersiveRailroading.ITEM_ROLLING_STOCK_COMPONENT) {
			if (!player.isSneaking()) {
				if (!this.isBuilt()) {
					//TEMP ASSEMBLE
					addNextComponent(player);
				}
			} else {
				this.removeNextComponent(player);
			}
			return true;
		}
		return false;
	}
}