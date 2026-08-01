package io.ib67.sfcraft.module.game.mapart;

import net.minecraft.world.level.material.MapColor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 把任意尺寸的图片来源伸缩为指定尺寸画布(单张地图 128x128,多格墙画画布为 128 的整数倍)的像素缓冲(MapColor 打包 id)。
 * 每像素取 RGB 距离最近的 MapColor x Brightness 组合;alpha < 128 视为透明(打包 id 0)。
 */
public final class MapArtRenderer {
    public static final int MAP_SIZE = 128;

    private static final int[] PALETTE_RGB;
    private static final byte[] PALETTE_PACKED;

    static {
        var rgbs = new int[63 * 4];
        var packed = new byte[63 * 4];
        int size = 0;
        for (int id = 1; id <= 63; id++) {
            MapColor color = MapColor.byId(id);
            if (color == MapColor.NONE) {
                continue;
            }
            for (MapColor.Brightness brightness : MapColor.Brightness.values()) {
                rgbs[size] = color.calculateARGBColor(brightness);
                packed[size] = color.getPackedId(brightness);
                size++;
            }
        }
        PALETTE_RGB = new int[size];
        PALETTE_PACKED = new byte[size];
        System.arraycopy(rgbs, 0, PALETTE_RGB, 0, size);
        System.arraycopy(packed, 0, PALETTE_PACKED, 0, size);
    }

    private MapArtRenderer() {
    }

    /** 拉伸至 {@value #MAP_SIZE}x{@value #MAP_SIZE} 并映射为地图画像素。 */
    public static byte[] render(BufferedImage source) {
        return render(source, MAP_SIZE, MAP_SIZE);
    }

    /** 拉伸至 width x height(行优先铺平,第 0 行为图像顶部)并映射为地图画像素。 */
    public static byte[] render(BufferedImage source, int width, int height) {
        var canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();

        var pixels = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = canvas.getRGB(x, y);
                pixels[x + y * width] = (argb >>> 24) < 128 ? 0 : closestColor(argb);
            }
        }
        return pixels;
    }

    /**
     * 从整幅画布(cols x rows 格,行优先铺平)中切出单格 128x128 像素;
     * 格坐标 (tx, ty) 以<b>左下角</b>为原点,与墙画铺设方向一致。
     */
    public static byte[] sliceTile(byte[] canvas, int cols, int rows, int tx, int ty) {
        int canvasWidth = cols * MAP_SIZE;
        int rowFromTop = rows - 1 - ty; // 画布像素第 0 行在图像顶部,墙上第 0 格在最底部
        var tile = new byte[MAP_SIZE * MAP_SIZE];
        for (int y = 0; y < MAP_SIZE; y++) {
            System.arraycopy(canvas, ((rowFromTop * MAP_SIZE + y) * canvasWidth) + tx * MAP_SIZE,
                    tile, y * MAP_SIZE, MAP_SIZE);
        }
        return tile;
    }

    private static byte closestColor(int argb) {
        int red = argb >> 16 & 0xFF;
        int green = argb >> 8 & 0xFF;
        int blue = argb & 0xFF;
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < PALETTE_RGB.length; i++) {
            int paletteColor = PALETTE_RGB[i];
            long dr = red - (paletteColor >> 16 & 0xFF);
            long dg = green - (paletteColor >> 8 & 0xFF);
            long db = blue - (paletteColor & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return PALETTE_PACKED[best];
    }
}
