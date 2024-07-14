package io.ib67.sfcraft.util;

import java.awt.*;

public class ColorHelper {

    public enum GradientType {
        LINEAR,
        HSV
    }

    public static Color interpolate(GradientType type, Color color1, Color color2, float t) {
        switch (type) {
            case LINEAR:
                return linearInterpolation(color1, color2, t);
            case HSV:
                return hsvInterpolation(color1, color2, t);
            default:
                throw new IllegalArgumentException("Unknown gradient type: " + type);
        }
    }

    private static Color linearInterpolation(Color color1, Color color2, float t) {
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * t);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * t);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * t);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    private static Color hsvInterpolation(Color color1, Color color2, float t) {
        float[] hsv1 = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] hsv2 = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);
        float h = (hsv1[0] + (hsv2[0] - hsv1[0]) * t);
        float s = (hsv1[1] + (hsv2[1] - hsv1[1]) * t);
        float v = (hsv1[2] + (hsv2[2] - hsv1[2]) * t);
        return Color.getHSBColor(h, s, v);
    }
}
