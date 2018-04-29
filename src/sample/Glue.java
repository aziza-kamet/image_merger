package sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by aziza on 28.04.18.
 */
public class Glue {

    public static final int HEIGHT = 2480;
    public static final int WIDTH = 1752;
    public static final int MARGIN_TOP = 74;

    public static String merge(ArrayList<BufferedImage> images, String name, String path) {

        int heightCurr = MARGIN_TOP;
        BufferedImage concatImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = concatImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        for (BufferedImage image : images) {
            g2d.drawImage(image, 0, heightCurr, null);
            heightCurr += image.getHeight();
        }
        g2d.dispose();

        try {
            File dir = new File(path + "/Набор");
            if (!dir.exists()){
                dir.mkdir();
            }
            ImageIO.write(concatImage, "jpg", new File(dir.getAbsolutePath() + "/" + name + ".jpg"));
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }
}
