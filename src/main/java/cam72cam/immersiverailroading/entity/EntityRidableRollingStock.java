package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Door;
import cam72cam.immersiverailroading.model.part.Seat;
import cam72cam.immersiverailroading.registry.WalkableSpaceDefinition;
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
import dan200.computercraft.shared.util.Palette;
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

    @TagField(value = "lastPassengerPosition", mapper = PassengerMapper.class)
    @TagSync
    private Map<UUID, Vec3d> lastPassengerPosition = new HashMap<>();

	// Hack to remount players if they were seated
	private Map<UUID, Vec3d> remount = new HashMap<>();

	private final WalkableSpaceDefinition walkableSpaceDefinition = getDefinition().walkableSpaceDefinition;

	private double lastHeight = -1;


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
		Map<String, Pair<Double, Double>> level = getDefinition().yMap;

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

	public Vec3d restrictMovement(Vec3d playerPosition, Vec3d playerVelocity, List<Vec3d> triangleVertices, Map<int[], List<Vec3d>> walkableTriangles) {
		// Calculate the normal of the triangle
		Vec3d normal = calculateNormal(triangleVertices);

		// Project player's velocity onto the plane of the triangle
		Vec3d projectedVelocity = projectOntoPlane(playerVelocity, normal);
		playerPosition = new Vec3d(playerPosition.z * -1, playerPosition.y, playerPosition.x);

		// Define triangle edges
		Vec3d[] edge1 = {triangleVertices.get(0), triangleVertices.get(1)};
		Vec3d[] edge2 = {triangleVertices.get(1), triangleVertices.get(2)};
		Vec3d[] edge3 = {triangleVertices.get(2), triangleVertices.get(0)};

		// Process each edge individually and restrict movement if necessary
		projectedVelocity = restrictOnEdge(projectedVelocity, playerPosition, edge1, walkableTriangles);
		projectedVelocity = restrictOnEdge(projectedVelocity, playerPosition, edge2, walkableTriangles);
		projectedVelocity = restrictOnEdge(projectedVelocity, playerPosition, edge3, walkableTriangles);

		return projectedVelocity;
	}

	private Vec3d restrictOnEdge(Vec3d velocity, Vec3d playerPos, Vec3d[] edge, Map<int[], List<Vec3d>> walkableTriangles) {
		if (isBoundaryEdge(edge, walkableTriangles) && isCollidingWithEdge(playerPos, velocity, edge[0], edge[1])) {
			// Restrict velocity along the direction of the edge
			return WalkableSpaceDefinition.projectOnto(velocity, edge[1].subtract(edge[0]));
		}
		return velocity;
	}

	// Check if the player's movement is within the edge boundaries
	private boolean isCollidingWithEdge(Vec3d playerPos, Vec3d velocity, Vec3d edgeStart, Vec3d edgeEnd) {
		// Edge vector and length squared
		Vec3d edgeVector = edgeEnd.subtract(edgeStart);
		double edgeLengthSquared = edgeVector.lengthSquared();

		// Vector from edge start to the player's position
		Vec3d playerToEdgeStart = playerPos.subtract(edgeStart);

		// Project player position onto the edge
		double projection = dotProduct(playerToEdgeStart, edgeVector) / edgeLengthSquared;

		// Check if the projected point is within the bounds of the edge
		if (projection < 0 || projection > 1) {
			return false;  // Player position projection is outside edge bounds
		}

		// Closest point on the edge to the player's position
		Vec3d closestPointOnEdge = edgeStart.add(edgeVector.scale(projection));

		// Perpendicular distance from player position to the edge
		double distanceToEdge = playerPos.distanceTo(closestPointOnEdge);

		// Check if the player is close enough to the edge (within precision) to be considered "colliding"
		if (distanceToEdge > 1) {
			return false;
		}

		// Check if the player's velocity moves it over the edge
		Vec3d playerEndPos = playerPos.add(velocity);
		Vec3d playerToEdgeEnd = playerEndPos.subtract(edgeStart);

		// Project end position onto the edge to check if it overshoots
		double endProjection = dotProduct(playerToEdgeEnd, edgeVector) / edgeLengthSquared;
		if (endProjection < 0 || endProjection > 1) {
			return false;
		}

		// Ensure velocity is directed towards the edge
		Vec3d playerToClosestPoint = closestPointOnEdge.subtract(playerPos);
		return dotProduct(playerToClosestPoint, velocity) > 0;
	}



	// Method to calculate the normal vector of a triangle
	private Vec3d calculateNormal(List<Vec3d> triangleVertices) {
		Vec3d v0 = triangleVertices.get(1).subtract(triangleVertices.get(0));
		Vec3d v1 = triangleVertices.get(2).subtract(triangleVertices.get(0));
		return crossProduct(v0, v1).normalize();
	}

	// Project a vector onto a plane defined by its normal
	private Vec3d projectOntoPlane(Vec3d vector, Vec3d normal) {
		double dotProduct = dotProduct(vector, normal);
		return vector.subtract(normal.scale(dotProduct)); // Removes component along the normal
	}

	public double dotProduct(Vec3d v1, Vec3d v2) {
		return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	}

	private boolean isBoundaryEdge(Vec3d[] edge, Map<int[], List<Vec3d>> walkableTriangles) {
		int sharedCount = 0;

		// Iterate through triangles to check if this edge is shared with any other triangle
		for (List<Vec3d> triangle : walkableTriangles.values()) {
			if (triangleContainsEdge(triangle, edge)) {
				sharedCount++;
				if (sharedCount > 1) return false; // More than one triangle shares this edge
			}
		}

		return true; // Edge is a boundary if it is not shared with another triangle
	}

	private boolean triangleContainsEdge(List<Vec3d> triangle, Vec3d[] edge) {
		return (triangle.contains(edge[0]) && triangle.contains(edge[1]));
	}

	// Method to determine if the player is currently within a triangle
	public List<Vec3d> getCurrentTriangle(Vec3d playerPosition, Map<int[], List<Vec3d>> walkableTriangles) {
		for (List<Vec3d> vertices : walkableTriangles.values()) {
			if (walkableSpaceDefinition.isPlayerInsideTriangle(playerPosition, vertices.get(0), vertices.get(1), vertices.get(2))) {
				return vertices; // Return the triangle the player is in
			}
		}
		return new ArrayList<>(); // Return null if the player is not in any walkable triangle
	}

	// Check if player is colliding with an edge of a triangle
	private boolean isColliding(Vec3d playerPosition, Vec3d playerVelocity, Vec3d edgeStart, Vec3d edgeEnd) {
		Vec3d edgeVector = edgeEnd.subtract(edgeStart);
		Vec3d playerToEdgeStart = playerPosition.subtract(edgeStart);

		// Calculate perpendicular distance of player's path to the edge
		Vec3d crossProduct = crossProduct(edgeVector, playerVelocity);
		double area = crossProduct.length();
		double edgeLength = edgeVector.length();
		double distance = area / edgeLength; // perpendicular distance

		// Check if the distance is small enough to consider a collision
		return distance < 0.1; // Adjust threshold based on desired precision
	}

	// Vec3d helper method
	private Vec3d crossProduct(Vec3d vec1, Vec3d vec2) {
		double crossX = vec1.y * vec2.z - vec1.z * vec2.y;
		double crossY = vec1.z * vec2.x - vec1.x * vec2.z;
		double crossZ = vec1.x * vec2.y - vec1.y * vec2.x;
		return new Vec3d(crossX, crossY, crossZ);
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
				if (walkableSpaceDefinition.isPlayerInsideTriangle(playerPosition, v1, v2, v3)) {
					lastHeight = walkableSpaceDefinition.calculateHeightInTriangle(playerPosition, v1, v2, v3);
					return walkableSpaceDefinition.calculateHeightInTriangle(playerPosition, v1, v2, v3);
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

		int wiggle = passenger.isVillager() ? 10 : 0;
		off = off.add((Math.random()-0.5) * wiggle, 0, (Math.random()-0.5) * wiggle);
		off = this.getDefinition().correctPassengerBounds(gauge, off, shouldRiderSit(passenger));

		double yOffset = getDefinition().yMap.isEmpty() ? off.y : getHeightAtPlayerPosition(off);
		if (!getDefinition().yMap.isEmpty()) {
			off = walkableSpaceDefinition.getNearestWalkablePoint(new Vec3d(off.z * -1, off.y, off.x));
			yOffset = getHeightAtPlayerPosition(off);
			off = new Vec3d(off.z, yOffset, off.x * -1);
			return off;
		}

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
		if (passenger.getWorld().isClient && !getDefinition().yMap.isEmpty()) {
			yOffset = getHeightAtPlayerPosition(offset);
		}

		Vec3d seat = getSeatPosition(passenger.getUUID());
		if (seat != null) {
			offset = seat;
		} else {
			offset = this.getDefinition().correctPassengerBounds(gauge, offset, shouldRiderSit(passenger));
		}
		offset = offset.add(0, Math.sin(Math.toRadians(this.getRotationPitch())) * offset.z, 0);

		if (seat == null && !getDefinition().yMap.isEmpty()) {
			offset = new Vec3d(offset.x, yOffset, offset.z);
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

		Vec3d offsetM = offset.add(movement);

		if (!getDefinition().yMap.isEmpty()) {
			Map<int[], List<Vec3d>> faces = getDefinition().floorMap.get(getCurrentFloor(offset));
			List<Vec3d> edges = getCurrentTriangle(new Vec3d(offset.z * -1, offset.y, offset.x), faces);
			Vec3d velocity = offsetM.subtract(offset);
			if (!edges.isEmpty()) {
				offset = offset.add(restrictMovement(offset, velocity, edges, faces));
//				offset = new Vec3d(offset.z, offset.y, offset.x * -1);
			} else {offset = offsetM;}
		}else {
			offset = offsetM;
		}
        if (this instanceof EntityCoupleableRollingStock) {
			EntityCoupleableRollingStock couplable = (EntityCoupleableRollingStock) this;

			boolean atFront = this.getDefinition().isAtFront(gauge, offset);
			boolean atBack = this.getDefinition().isAtRear(gauge, offset);
			if (!getDefinition().yMap.isEmpty()) {
				atFront = this.walkableSpaceDefinition.isAtRearNew(gauge, offset);
				atBack = this.walkableSpaceDefinition.isAtFrontNew(gauge, offset);
			}
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
		lastPassengerPosition.put(player.getUUID(), player.getPosition().subtract(this.getPosition()).rotateYaw(this.getRotationYaw()));
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
