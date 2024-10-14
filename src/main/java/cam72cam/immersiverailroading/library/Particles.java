package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.render.CustomParticle.CustomParticleData;
import cam72cam.immersiverailroading.render.SmokeParticle.SmokeParticleData;

import java.util.function.Consumer;

public class Particles {
    public static Consumer<SmokeParticleData> SMOKE;
    public static Consumer<CustomParticleData> CUSTOM;
}
