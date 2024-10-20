package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.model.part.Seat;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IRidable;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityRidableRollingStock extends EntityBuildableRollingStock implements IRidable {
	public float getRidingSoundModifier() {
		return getDefinition().dampeningAmount;
	}

	@TagField(value = "payingPassengerPositions", mapper = PassengerMapper.class)
	private Map<UUID, Vec3d> payingPassengerPositions = new HashMap<>();

	@TagField(value = "seatedPassengers", mapper = SeatedMapper.class)
	@TagSync
	private Map<String, UUID> seatedPassengers = new HashMap<>();

	// Hack to remount players if they were seated
	private Map<UUID, Vec3d> remount = new HashMap<>();

	private String currentFloor = null; // Stores the current floor
	private double transitionFactor = 2; // Factor for smoothing the transition



	@Override
	public ClickResult onClick(Player player, Player.Hand hand) {
		ClickResult clickRes = super.onClick(player, hand);
		if (clickRes != ClickResult.PASS) {
			return clickRes;
		}

		if (player.isCrouching()) {
			return ClickResult.PASS;
		} else if (isPassenger(player)) {
			return ClickResult.PASS;
		} else {
			if (getWorld().isServer) {
				player.startRiding(this);
			}
			return ClickResult.ACCEPTED;
		}
	}

	private Vec3d getSeatPosition(UUID passenger) {
		String seat = seatedPassengers.entrySet().stream()
				.filter(x -> x.getValue().equals(passenger))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		return this.getDefinition().getModel().getSeats().stream()
				.filter(s -> s.part.key.equals(seat))
				.map(s -> new Vec3d(s.part.center.z, s.part.min.y, -s.part.center.x).scale(gauge.scale()).subtract(0, 0.6, 0))
				.findFirst().orElse(null);
	}

	/*
	 * Get the current floor the player is standing on.
	 */

	private String getCurrentFloor(Vec3d playerPos) {
		double tolerance = 0.05;
		Map<String, Pair<Double, Double>> level = getDefinition().yLevel;

		String closestFloor = null;
		double closestDistance = Double.MAX_VALUE;

		for (String floor : level.keySet()) {
			Pair<Double, Double> heightRange = level.get(floor);
			double minHeight = heightRange.getLeft();
			double maxHeight = heightRange.getRight();

			double adjustedMin = minHeight - tolerance;
			double adjustedMax = maxHeight + tolerance;

			if (playerPos.y >= adjustedMin && playerPos.y <= adjustedMax) {
				double distanceToMin = Math.abs(playerPos.y - minHeight);
				double distanceToMax = Math.abs(playerPos.y - maxHeight);
				double preferredDistance = Math.min(distanceToMin, distanceToMax);

				if (preferredDistance <= closestDistance) {
					closestDistance = preferredDistance;
					closestFloor = floor;
				}
			}
		}

		return closestFloor != null ? closestFloor : level.keySet().iterator().next();
	}

	// Atm not implemented TODO implement!
	private Vec3d restrictPlayerMovement(Vec3d playerPos, Vec3d offset, List<Vec3d> faces) {
		Vec3d newPosition = playerPos.add(offset);
		if (getDefinition().isPlayerInsideTriangle(newPosition, faces.get(0), faces.get(1), faces.get(2))) {
			return offset;
		} else {
			return offset;
		}
	}

	/*
	 * get the y-offset for the player Position
	 */

	public double getHeightAtPlayerPosition(Vec3d playerPosition) {
		playerPosition = new Vec3d(playerPosition.z * - 1, playerPosition.y, playerPosition.x);
		String floor = getCurrentFloor(playerPosition);
		Map<int[], List<Vec3d>> faces = getDefinition().floorMap.get(floor);
		for (int[] faceIndices : faces.keySet()) {
			List<Vec3d> vertices = faces.get(faceIndices);
			if (vertices.size() >= 3) {
				Vec3d v1 = vertices.get(0);
				Vec3d v2 = vertices.get(1);
				Vec3d v3 = vertices.get(2);
				if (getDefinition().isPlayerInsideTriangle(playerPosition, v1, v2, v3)) {
					return getDefinition().calculateHeightInTriangle(playerPosition, v1, v2, v3);
				}
			}
		}
		return -1;
	}



	@Override
	public Vec3d getMountOffset(Entity passenger, Vec3d off) {
		if (passenger.isVillager() && !payingPassengerPositions.containsKey(passenger.getUUID())) {
			payingPassengerPositions.put(passenger.getUUID(), passenger.getPosition());
		}

		if (passenger.isVillager() && !seatedPassengers.containsValue(passenger.getUUID())) {
			for (Seat<?> seat : getDefinition().getModel().getSeats()) {
				if (!seatedPassengers.containsKey(seat.part.key)) {
					seatedPassengers.put(seat.part.key, passenger.getUUID());
					break;
				}
			}
		}

		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			return seat;
		}

		double yOffset = getHeightAtPlayerPosition(off);

		int wiggle = passenger.isVillager() ? 10 : 0;
		off = off.add((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);
		off = this.getDefinition().correctPassengerBounds(gauge, off, shouldRiderSit(passenger));
		off = new Vec3d(off.x, yOffset, off.z);

		return off;
	}

	@Override
	public boolean canFitPassenger(Entity passenger) {
		if (passenger instanceof Player && !((Player) passenger).hasPermission(Permissions.BOARD_STOCK)) {
			return false;
		}
		return getPassengerCount() < this.getDefinition().getMaxPassengers();
	}
	
	@Override
	public boolean shouldRiderSit(Entity passenger) {
		boolean nonSeated = this.getDefinition().shouldSit != null ? this.getDefinition().shouldSit : this.gauge.shouldSit();
		return nonSeated || this.seatedPassengers.containsValue(passenger.getUUID());
	}

	@Override
	public Vec3d onPassengerUpdate(Entity passenger, Vec3d offset) {
		if (passenger.isPlayer()) {
			offset = playerMovement(passenger.asPlayer(), offset);
		}

		double yOffset = 1;
		if (passenger.getWorld().isClient) {
			yOffset = getHeightAtPlayerPosition(offset);
		}

		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			offset = seat;
		} else {
			offset = this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(passenger));
		}
		offset = offset.add(0, Math.sin(Math.toRadians(this.getRotationPitch())) * offset.z, 0);

		if (seat == null) {
			offset = new Vec3d(offset.x, yOffset, offset.z); // TODO search better way to implement this
		}
		return offset;
	}

	private boolean isNearestDoorOpen(Player source) {
		// Find any doors that are close enough that are closed (and then negate)
		return !this.getDefinition().getModel().getDoors().stream()
				.filter(d -> d.type == Door.Types.CONNECTING)
				.filter(d -> d.center(this).distanceTo(source.getPosition()) < getDefinition().getLength(this.gauge)/3)
				.min(Comparator.comparingDouble(d -> d.center(this).distanceTo(source.getPosition())))
				.filter(x -> !x.isOpen(this))
				.isPresent();
	}

	private Vec3d playerMovement(Player source, Vec3d offset) {
		Vec3d movement = source.getMovementInput();
        /*
        if (sprinting) {
            movement = movement.scale(3);
        }
        */

        if (movement.length() < 0.1) {
            return offset;
        }

        movement = new Vec3d(movement.x, 0, movement.z).rotateYaw(this.getRotationYaw() - source.getRotationYawHead());

		offset = offset.add(movement);

		Vec3d playerPos = new Vec3d(offset.z * - 1, offset.y, offset.x);
		String floor = getCurrentFloor(playerPos);
		if (floor != null) {
			Map<int[], List<Vec3d>> faces = getDefinition().floorMap.get(floor);
			for (Map.Entry<int[], List<Vec3d>> entry : faces.entrySet()) {
				List<Vec3d> faceVertecies = entry.getValue();
				offset = restrictPlayerMovement(playerPos, offset, faceVertecies);
			}
		}

        if (this instanceof EntityCoupleableRollingStock) {
			EntityCoupleableRollingStock couplable = (EntityCoupleableRollingStock) this;

			boolean atFront = this.getDefinition().isAtFront(gauge, offset);
			boolean atBack = this.getDefinition().isAtRear(gauge, offset);
			// TODO config for strict doors
			boolean atDoor = isNearestDoorOpen(source);

			atFront &= atDoor;
			atBack &= atDoor;

			for (CouplerType coupler : CouplerType.values()) {
				boolean atCoupler = coupler == CouplerType.FRONT ? atFront : atBack;
				if (atCoupler && couplable.isCoupled(coupler)) {
					EntityCoupleableRollingStock coupled = ((EntityCoupleableRollingStock) this).getCoupled(coupler);
					if (coupled != null) {
						if (((EntityRidableRollingStock)coupled).isNearestDoorOpen(source)) {
							coupled.addPassenger(source);
						}
					} else if (this.getTickCount() > 20) {
						ImmersiveRailroading.info(
								"Tried to move between cars (%s, %s), but %s was not found",
								this.getUUID(),
								couplable.getCoupledUUID(coupler),
								couplable.getCoupledUUID(coupler)
						);
					}
					return offset;
				}
			}
        }

        if (getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL)) &&
				getWorld().isServer &&
				!this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(source)).equals(offset)
		) {
        	this.removePassenger(source);
		}

		return offset;
	}

	@Override
	public void onTick() {
		super.onTick();

		if (getWorld().isServer) {
			remount.forEach((uuid, pos) -> {
				Player player = getWorld().getEntity(uuid, Player.class);
				if (player != null) {
					player.setPosition(pos);
					player.startRiding(this);
				}
			});
			remount.clear();
			for (Player source : getWorld().getEntities(Player.class)) {
				if (source.getRiding() == null && getDefinition().getModel().getDoors().stream().anyMatch(x -> x.isAtOpenDoor(source, this, Door.Types.EXTERNAL))) {
					this.addPassenger(source);
				}
			}
		}
	}

	public Vec3d onDismountPassenger(Entity passenger, Vec3d offset) {
		List<String> seats = seatedPassengers.entrySet().stream().filter(x -> x.getValue().equals(passenger.getUUID()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!seats.isEmpty()) {
			seats.forEach(seatedPassengers::remove);
			if (getWorld().isServer && passenger.isPlayer()) {
				remount.put(passenger.getUUID(), passenger.getPosition());
			}
		}

		//TODO calculate better dismount offset
		offset = new Vec3d(Math.copySign(getDefinition().getWidth(gauge)/2 + 1, offset.x), 0, offset.z);

		if (getWorld().isServer && passenger.isVillager() && payingPassengerPositions.containsKey(passenger.getUUID())) {
			double distanceMoved = passenger.getPosition().distanceTo(payingPassengerPositions.get(passenger.getUUID()));

			int payout = (int) Math.floor(distanceMoved * Config.ConfigBalance.villagerPayoutPerMeter);

			List<ItemStack> payouts = Config.ConfigBalance.getVillagerPayout();
			if (payouts.size() != 0) {
				int type = (int)(Math.random() * 100) % payouts.size();
				ItemStack stack = payouts.get(type).copy();
				stack.setCount(payout);
				getWorld().dropItem(stack, getBlockPosition());
				// TODO drop by player or new pos?
			}
			payingPassengerPositions.remove(passenger.getUUID());
		}

		return offset;
	}

	public void onSeatClick(String seat, Player player) {
		List<String> seats = seatedPassengers.entrySet().stream().filter(x -> x.getValue().equals(player.getUUID()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
		if (!seats.isEmpty()) {
			seats.forEach(seatedPassengers::remove);
			return;
		}

		seatedPassengers.put(seat, player.getUUID());
	}

	private static class PassengerMapper implements TagMapper<Map<UUID, Vec3d>> {
		@Override
		public TagAccessor<Map<UUID, Vec3d>> apply(Class<Map<UUID, Vec3d>> type, String fieldName, TagField tag) {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, UUID::toString, (Vec3d pos) -> new TagCompound().setVec3d("pos", pos)),
					d -> d.getMap(fieldName, UUID::fromString, t -> t.getVec3d("pos"))
			);
		}
	}

	private static class SeatedMapper implements TagMapper<Map<String, UUID>> {
		@Override
		public TagAccessor<Map<String, UUID>> apply(Class<Map<String, UUID>> type, String fieldName, TagField tag) throws SerializationException {
			return new TagAccessor<>(
					(d, o) -> d.setMap(fieldName, o, i -> i, u -> new TagCompound().setUUID("uuid", u)),
					d -> d.getMap(fieldName, i -> i, t -> t.getUUID("uuid"))
			);
		}
	}
}
