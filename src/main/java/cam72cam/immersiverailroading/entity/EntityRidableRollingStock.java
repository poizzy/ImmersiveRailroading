package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.floor.NavMesh;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.model.part.Seat;
import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.util.MathUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.custom.IRidable;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.OBJFace;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;

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

	protected Vec3d getSeatPosition(UUID passenger) {
		String seat = seatedPassengers.entrySet().stream()
				.filter(x -> x.getValue().equals(passenger))
				.map(Map.Entry::getKey).findFirst().orElse(null);
		return this.getDefinition().getModel().getSeats().stream()
				.filter(s -> s.part.key.equals(seat))
				.map(s -> new Vec3d(s.part.center.z, s.part.min.y, -s.part.center.x).scale(gauge.scale()).subtract(0, 0.6, 0))
				.findFirst().orElse(null);
	}

	@Override
	public Vec3d getMountOffset(Entity passenger, Vec3d off) {
		NavMesh navMesh = getDefinition().navMesh;

		off = off.scale(gauge.scale());
		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			return seat;
		}

		Vec3d realOffset = off.rotateYaw(-90);
		IBoundingBox queryBox = IBoundingBox.from(
				realOffset.subtract(4f, 4f, 4f),
				realOffset.add(4f, 4f, 4f)
		);

		List<OBJFace> nearby = new ArrayList<>();
		navMesh.queryBVH(navMesh.root, queryBox, nearby, this.gauge.scale());

		Vec3d closestPoint = null;
		double closestDistanceSq = Double.MAX_VALUE;

		for (OBJFace tri : nearby) {
			Vec3d p0 = tri.vertex0.pos;
			Vec3d p1 = tri.vertex1.pos;
			Vec3d p2 = tri.vertex2.pos;

			Vec3d pointOnTri = MathUtil.closestPointOnTriangle(realOffset, p0, p1, p2);
			double distSq = realOffset.subtract(pointOnTri).lengthSquared();

			if (distSq < closestDistanceSq) {
				closestDistanceSq = distSq;
				closestPoint = pointOnTri;
			}
		}

		if (closestPoint != null) {
			return closestPoint.rotateYaw(90);
		} else {
			return new Vec3d(0, 0, 0);
		}
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
		Vec3d movement = new Vec3d(0, 0, 0);
		if (passenger.isPlayer()) {
			movement = playerMovement(passenger.asPlayer(), offset);
		}
		Vec3d targetXZ = VecUtil.rotatePitch(movement, -this.getRotationPitch());

		Vec3d rayStart = targetXZ.rotateYaw(-90).add(0, 1, 0);
		Vec3d rayDir = new Vec3d(0, -1, 0);

		Vec3d localTarget = targetXZ.rotateYaw(-90);

		IBoundingBox rayBox = IBoundingBox.from(
				localTarget.subtract(0.5f, 0.5f, 0.5f),
				localTarget.add(0.5f, 0.5f, 0.5f)
		);
		List<OBJFace> nearby = new ArrayList<>();
		NavMesh navMesh = getDefinition().navMesh;
		navMesh.queryBVH(navMesh.root, rayBox, nearby, this.gauge.scale());

		double closestY = Float.NEGATIVE_INFINITY;
		boolean hit = false;

		for(OBJFace tri : nearby) {
			Double t = MathUtil.intersectRayTriangle(rayStart, rayDir, tri);
			if (t != null && t >= 0) {
				Vec3d hitPoint = rayStart.add(rayDir.scale(t));
				if (!hit || hitPoint.y > closestY) {
					closestY = hitPoint.y;
					hit = true;
				}
			}

		}

		if (hit) {
			offset = VecUtil.rotatePitch(new Vec3d(targetXZ.x, closestY, targetXZ.z), this.getRotationPitch());
		}

		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			offset = seat;
		}

		return offset;
	}

	protected boolean isNearestDoorOpen(Player source) {
		// Find any doors that are close enough that are closed (and then negate)
		return !this.getDefinition().getModel().getDoors().stream()
				.filter(d -> d.type == Door.Types.CONNECTING)
				.filter(d -> d.center(this).distanceTo(source.getPosition()) < getDefinition().getLength(this.gauge)/3)
				.min(Comparator.comparingDouble(d -> d.center(this).distanceTo(source.getPosition())))
				.filter(x -> !x.isOpen(this))
				.isPresent();
	}

	protected Vec3d playerMovement(Player source, Vec3d offset) {
		Vec3d movement = source.getMovementInput();
		if (movement.length() <= 0.1) {
			return offset;
		}

		movement = new Vec3d(movement.x, 0, movement.z).rotateYaw(this.getRotationYaw() - source.getRotationYawHead());
		Vec3d localOffset = offset.rotateYaw(-90).add(movement.rotateYaw(-90));

		IBoundingBox rayBox = IBoundingBox.from(
				localOffset.subtract(0.2f, 0.2f, 0.2f),
				localOffset.add(0.2f, 0.2f, 0.2f)
		);
		List<OBJFace> nearby = new ArrayList<>();
		NavMesh navMesh = getDefinition().navMesh;
		navMesh.queryBVH(navMesh.collisionRoot, rayBox, nearby, this.gauge.scale());

		Vec3d rayStart = localOffset.add(0, 1, 0);
		Vec3d rayDir = movement.rotateYaw(-90).normalize();

		for (OBJFace tri : nearby) {
			Double t = MathUtil.intersectRayTriangle(rayStart, rayDir, tri);
			if (t != null && t >= 0) {
				return offset;
			}
		}

		if (isDoorOpen(offset, movement)) {
			return offset;
		}

		offset = offset.add(movement);

		if (getWorld().isServer) {
			for (Door<?> door : getDefinition().getModel().getDoors()) {
				if (door.isAtOpenDoor(source, this, Door.Types.EXTERNAL)) {
					Vec3d doorCenter = door.center(this);
					Vec3d toDoor = doorCenter.subtract(offset).normalize();
					double dot = toDoor.dotProduct(movement.normalize());
					if (dot > 0.5) {
						this.removePassenger(source);
						break;
					}
				}
			}
		}

		if (this instanceof EntityCoupleableRollingStock) {
			EntityCoupleableRollingStock coupleable = (EntityCoupleableRollingStock) this;

			boolean isAtFront = isAtCoupler(offset, movement, EntityCoupleableRollingStock.CouplerType.FRONT);
			boolean isAtBack =  isAtCoupler(offset, movement, EntityCoupleableRollingStock.CouplerType.BACK);
			boolean atDoor = isNearestDoorOpen(source);

			isAtFront &= atDoor;
			isAtBack &= atDoor;

			for (EntityCoupleableRollingStock.CouplerType coupler : EntityCoupleableRollingStock.CouplerType.values()) {
				boolean atCoupler = coupler == EntityCoupleableRollingStock.CouplerType.FRONT ? isAtFront : isAtBack;
				if (atCoupler && coupleable.isCoupled(coupler)) {
					EntityCoupleableRollingStock coupled = ((EntityCoupleableRollingStock) this).getCoupled(coupler);
					if (coupled != null) {
						if (coupled.isNearestDoorOpen(source)) {
							coupled.addPassenger(source);
						}
					} else if (this.getTickCount() > 20) {
						ImmersiveRailroading.info(
								"Tried to move between cars (%s, %s), but %s was not found",
								this.getUUID(),
								coupleable.getCoupledUUID(coupler),
								coupleable.getCoupledUUID(coupler)
						);
					}
				}
			}
		}

		return offset;
	}

	private boolean isDoorOpen(Vec3d start, Vec3d end) {
		start = VecUtil.rotatePitch(start, -this.getRotationPitch());
		end = VecUtil.rotatePitch(end, -this.getRotationPitch());

		start = start.rotateYaw(-90);
		end = start.add(end.rotateYaw(-90));

		List<Door<?>> doors = getDefinition().getModel().getDoors().stream()
				.filter(d -> d.type == Door.Types.INTERNAL || d.type == Door.Types.CONNECTING)
				.filter(d -> !d.isOpen(this)).collect(Collectors.toList());
		boolean intersects = false;
		for (Door<?> door : doors) {
			IBoundingBox box = IBoundingBox.from(
					door.part.min,
					door.part.max
			);
			intersects = box.intersectsSegment(start, end);
			if (intersects) {
				break;
			}
		}
		return intersects;
	}

	private boolean isAtCoupler(Vec3d offset, Vec3d movement, EntityCoupleableRollingStock.CouplerType type) {
		offset = offset.rotateYaw(-90);
		double coupler = getDefinition().getCouplerPosition(type, this.gauge);
		Vec3d couplerPos = new Vec3d(type == EntityCoupleableRollingStock.CouplerType.FRONT ? -coupler : coupler, offset.y, offset.z);

		IBoundingBox queryBox = IBoundingBox.from(
				couplerPos.subtract(0.2, 0.2, 0.2),
				couplerPos.add(0.2, 0.2, 0.2)
		);

		List<OBJFace> nearby = new ArrayList<>();
		NavMesh navMesh = getDefinition().navMesh;
		navMesh.queryBVH(navMesh.root, queryBox, nearby, this.gauge.scale());

		for (OBJFace tri : nearby) {
			Vec3d p0 = tri.vertex0.pos;
			Vec3d p1 = tri.vertex1.pos;
			Vec3d p2 = tri.vertex2.pos;

			Vec3d closestPoint = MathUtil.closestPointOnTriangle(offset, p0, p1, p2);
			double distance = offset.subtract(closestPoint).length();
			if (distance < 0.5) {
				Vec3d toCoupler = couplerPos.subtract(offset).normalize();
				double dot = toCoupler.dotProduct(movement.rotateYaw(-90).normalize());
				if (dot > 0.5) return true;
			}
		}
		return false;
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
