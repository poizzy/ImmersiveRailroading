package cam72cam.immersiverailroading.render;

import cam72cam.immersiverailroading.model.part.CustomParticleConfig.rgba;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.Particle;
import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import util.Matrix4;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomParticle extends Particle {
    public static class CustomParticleData extends ParticleData {
        private final float darken;
        private final float thickness;
        private final double diameter;
        private final Identifier texture;
        private final double expansionRate;
        private final rgba color;

        public CustomParticleData(World world, Vec3d pos, Vec3d motion, int lifespan, float darken, float thickness, double diameter, Identifier texture, double expansionRate, rgba rgba) {
            super(world, pos, motion, lifespan);
            this.darken = darken;
            this.thickness = thickness;
            this.diameter = diameter;
            this.texture = texture;
            this.expansionRate = expansionRate;
            this.color = rgba;
        }
    }

    private final double rot;
    private final CustomParticleData data;

    public CustomParticle(CustomParticleData data) {
        this.data = data;
        this.rot = Math.random() * 360;
    }

    @Override
    protected boolean depthTestEnabled() {
        return false;
    }

    @Override
    protected void render(RenderState state, float partialTicks) {
    }

    public static void renderAll(List<CustomParticle> particles, RenderState state, float partialTicks) {
        state.cull_face(false);

        Map<Identifier, List<CustomParticle>> partitioned = particles.stream().collect(Collectors.groupingBy(p -> p.data.texture));
        for (Identifier texture : partitioned.keySet()) {
            state.texture(Texture.wrap(texture));

            DirectDraw buffer = new DirectDraw();
            for (CustomParticle particle : partitioned.get(texture)) {
                double life = particle.ticks / (float) particle.data.lifespan;

                double expansionRate = particle.data.expansionRate;

                double radius = particle.data.diameter * (Math.sqrt(life) * expansionRate + 1) * 0.5;

                float alpha = (particle.data.thickness + 0.25f) * (1 - (float) Math.sqrt(life));
                Matrix4 matrix = new Matrix4();
//                float darken = 0.9f - particle.data.darken * 0.9f;

                matrix.translate(particle.renderX, particle.renderY, particle.renderZ);

                // Rotate to look at internal
                particle.lookAtPlayer(matrix);

                // Apply size
                matrix.scale(radius, radius, radius);

                // Noise Factor
                matrix.rotate(Math.toRadians(particle.rot), 0, 0, 1);
                matrix.translate(0.5, 0, 0);
                matrix.rotate(Math.toRadians(-particle.rot), 0, 0, 1);

                // Spin
                double angle = particle.ticks + partialTicks;// + 45;
                matrix.rotate(Math.toRadians(angle), 0, 0, 1);

                rgba color = particle.data.color;

                if (color.a < 1f) {
                    alpha = (float) color.a;
                }

                //Draw
                buffer.vertex(matrix.apply(new Vec3d(-1, -1, 0))).uv(0, 0).color(color.r, color.g, color.b, alpha);
                buffer.vertex(matrix.apply(new Vec3d(-1, 1, 0))).uv(0, 1).color(color.r, color.g, color.b, alpha);
                buffer.vertex(matrix.apply(new Vec3d(1, 1, 0))).uv(1, 1).color(color.r, color.g, color.b, alpha);
                buffer.vertex(matrix.apply(new Vec3d(1, -1, 0))).uv(1, 0).color(color.r, color.g, color.b, alpha);
            }
            buffer.draw(state);
        }
    }
}
