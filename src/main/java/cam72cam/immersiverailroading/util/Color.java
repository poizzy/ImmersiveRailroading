package cam72cam.immersiverailroading.util;

public record Color(double r, double g, double b, double a) {

    public Color(int r, int g, int b, int a) {
        this(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
    }

    public Color(int rgb) {
        this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (rgb >> 24) & 0xFF);
    }

    public Color(String hex) {
        hex = hex.replace("#", "");
        boolean hasAlpha = hex.length() > 6;
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int a = hasAlpha ? Integer.parseInt(hex.substring(6, 8), 16) : 255;
        this(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
    }

    public static Color fromMinecraftCode(char c) {
        return switch (Character.toLowerCase(c)) {
            case '0' -> new Color(0, 0, 0, 255); // black
            case '1' -> new Color(0, 0, 170, 255); // dark_blue
            case '2' -> new Color(0, 170, 0, 255); // dark_green
            case '3' -> new Color(0, 170, 170, 255); // dark_aqua
            case '4' -> new Color(170, 0, 0, 255); // dark_red
            case '5' -> new Color(170, 0, 170, 255); // dark_purple
            case '6' -> new Color(255, 170, 0, 255); // gold
            case '7' -> new Color(170, 170, 170, 255); // gray
            case '8' -> new Color(85, 85, 85, 255); // dark_gray
            case '9' -> new Color(85, 85, 255, 255); // blue
            case 'a' -> new Color(85, 255, 85, 255); // green
            case 'b' -> new Color(85, 255, 255, 255); // aqua
            case 'c' -> new Color(255, 85, 85, 255); // red
            case 'd' -> new Color(255, 85, 255, 255); // light_purple
            case 'e' -> new Color(255, 255, 85, 255); // yellow
            default -> new Color(255, 255, 255, 255); // white
        };
    }

    public static final Color RED = new Color(255, 0, 0, 255);
    public static final Color LIME = new Color(0, 255, 0, 255);
    public static final Color CHARTREUSE = new Color(127, 255, 0, 255);
    public static final Color BLUE = new Color(0, 0, 255, 255);
    public static final Color YELLOW = new Color(255, 255, 0, 255);
    public static final Color GRAY = new Color(128, 128, 128, 255);
    public static final Color WHITE = new Color(255, 255, 255, 255);
    public static final Color MAGENTA = new Color(255, 0, 255, 255);
    public static final Color DARKGREEN = new Color(0, 100, 0, 255);

}