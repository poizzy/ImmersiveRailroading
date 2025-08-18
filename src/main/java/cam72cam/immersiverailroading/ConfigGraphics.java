package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.library.PressureDisplayType;
import cam72cam.immersiverailroading.library.SpeedDisplayType;
import cam72cam.immersiverailroading.library.TemperatureDisplayType;
import cam72cam.mod.config.ConfigFile.Comment;
import cam72cam.mod.config.ConfigFile.Name;
import cam72cam.mod.render.OptiFine;

import java.util.HashMap;
import java.util.Map;

import static cam72cam.mod.config.ConfigFile.*;

@Comment("Configuration File")
@Name("general")
@File("immersiverailroading_graphics.cfg")
public class ConfigGraphics {
	@Comment("Enable Particles")
	public static boolean particlesEnabled = true;

	@Comment( "Self explanatory" )
	public static boolean trainsOnTheBrain = true;
	
	@Comment( "What unit to use for speedometer. (kmh, mph or ms)" )
	public static SpeedDisplayType speedUnit = SpeedDisplayType.kmh;

	@Comment("What units to display pressure in (psi, bar)")
	public static PressureDisplayType pressureUnit = PressureDisplayType.psi;

	@Comment("What units to display pressure in (psi, bar)")
	public static TemperatureDisplayType temperatureUnit = TemperatureDisplayType.celcius;

	@Comment( "How long to keep textures in memory after they have left the screen (higher numbers = smoother game play, lower numbers = less GPU memory used)")
	@Range(min = 0, max = 100)
    public static int textureCacheSeconds = 30;

	@Comment( "Show text tooltips over interactable components" )
	public static boolean interactiveComponentsOverlay = true;

	@Comment("Show stock variants in JEI/NEI/Creative search")
	public static boolean stockItemVariants = false;

	@Comment("Override OptiFine Shaders for entities")
	public static OptiFine.Shaders OptiFineEntityShader = OptiFine.Shaders.Entities;
	@Comment("Override Optifine Shader for all entities (not just ones that have specular/normal maps)")
	public static boolean OptifineEntityShaderOverrideAll = false;

	@Comment("How far away stock needs to be to switch to a smaller LOD texture")
	@Range(min = 0, max = 500)
	public static double StockLODDistance = 64;

	@Comment("0.0 is no sway, 1.0 is default sway")
	@Range(min = 0, max = 1)
	public static double StockSwayMultiplier = 1;

	@Comment("How likely a piece of stock is to sway (1 == always, 10 == infrequent)")
	@Range(min = 1, max = 10)
	public static int StockSwayChance = 1;

	@Comment("Settings used in the stock user interfaces")
	public static Map<String, Float> settings = new HashMap<>();

	@Comment("Mouse Scroll Speed (negative values invert it)")
	@Range(min = -10, max = 10)
	public static float ScrollSpeed = 1;

	@Comment("Try to fake interior lighting for locomotives/passenger cars that are being ridden")
	public static boolean FakeInteriorLighting = true;

	@Comment("The track's maximum visibility range")
	@Range(min = 256, max = 4096)
	public static double TrackRenderDistance = 256;
}
