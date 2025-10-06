package cam72cam.immersiverailroading.render.rail;

import cam72cam.immersiverailroading.render.ExpireableMap;
import cam72cam.immersiverailroading.track.TrackBase;
import cam72cam.immersiverailroading.util.RailInfo;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;

import java.util.List;

public class RailBaseOverlayRender {
	private static final ExpireableMap<String, DirectDraw> cache = new ExpireableMap<>(1, false);

	private static DirectDraw doDraw(RailInfo info, List<TrackBase> tracks, Vec3i pos) {
		DirectDraw draw = new DirectDraw();
		Vec3i placePos = new Vec3i(info.placementInfo.placementPosition).add(pos);

		for (TrackBase base : tracks) {
			boolean canPlace = base.canPlaceTrack();
			if (!canPlace) {
				Vec3i tpos = base.getPos();
				tpos = tpos.subtract(placePos);

				double width = 1.002;
				double height = base.getBedHeight() + 0.2f;
				double x = tpos.x + -0.001;
				double y = tpos.y;
				double z = tpos.z + 0.001 + 1;

				// front
				draw.vertex(x + 0.0f, y + 0.0f, z + 0.0f);
				draw.vertex(x + width, y + 0.0f, z + 0.0f);
				draw.vertex(x + width, y + height, z + 0.0f);
				draw.vertex(x + 0.0f, y + height, z + 0.0f);
				// back
				draw.vertex(x + 0.0f, y + height, z + -width);
				draw.vertex(x + width, y + height, z + -width);
				draw.vertex(x + width, y + 0.0f, z + -width);
				draw.vertex(x + 0.0f, y + 0.0f, z + -width);
				// right
				draw.vertex(x + width, y + 0.0f, z + 0.0f);
				draw.vertex(x + width, y + 0.0f, z + -width);
				draw.vertex(x + width, y + height, z + -width);
				draw.vertex(x + width, y + height, z + 0.0f);
				// left
				draw.vertex(x + 0.0f, y + height, z + 0.0f);
				draw.vertex(x + 0.0f, y + height, z + -width);
				draw.vertex(x + 0.0f, y + 0.0f, z + -width);
				draw.vertex(x + 0.0f, y + 0.0f, z + 0.0f);
				// top
				draw.vertex(x + 0.0f, y + height, z + 0.0f);
				draw.vertex(x + width, y + height, z + 0.0f);
				draw.vertex(x + width, y + height, z + -width);
				draw.vertex(x + 0.0f, y + height, z + -width);
				// bottom
				draw.vertex(x + 0.0f, y + 0.0f, z + 0.0f);
				draw.vertex(x + width, y + 0.0f, z + 0.0f);
				draw.vertex(x + width, y + 0.0f, z + -width);
				draw.vertex(x + 0.0f, y + 0.0f, z + -width);
			}
		}
		return draw;
	}

	public static void draw(RailInfo info, List<TrackBase> tracks, Vec3i pos, RenderState state) {
		String key = info.uniqueID + pos.add(new Vec3i(info.placementInfo.placementPosition));
		DirectDraw draw = cache.get(key);
		if (draw == null) {
			draw = doDraw(info, tracks, pos);
			cache.put(key, draw);
		}
		state.texture(Texture.NO_TEXTURE);
		state.color(1, 0, 0, 1);
		draw.draw(state);
	}
}
