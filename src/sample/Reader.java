package sample;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by aziza on 28.04.18.
 */
public class Reader {

    private String[] extensions;
    private FilenameFilter imageFilter;

    public Reader() {

        this.extensions = new String[]{
                "jpg", "jpeg", // and other formats you need
        };

        this.imageFilter  = (dir, name) -> {
            for (final String ext : extensions) {
                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }
            return (false);
        };

    }

    public ArrayList<File> init(File dir) {
        ArrayList<File> images = new ArrayList<File>();
        for (final File file : dir.listFiles(imageFilter)) {
            images.add(file);
        }
        return images;
//            return Glue.merge(images, maxWidth);
    }

}
