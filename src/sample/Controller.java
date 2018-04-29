package sample;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Controller {

    @FXML
    private javafx.scene.control.Label statusLabel;

    private File chosenDir;
    private final double MAX_HEIGHT = Glue.HEIGHT - Glue.MARGIN_TOP;

    public void onDirChooserButtonClicked() {

        DirectoryChooser dirChooser = new DirectoryChooser();
        this.chosenDir = dirChooser.showDialog(Main.stage);
        statusLabel.setText("Нажмите на кнопку \"Старт\"");
        statusLabel.setTextFill(Color.web("#555555"));
    }

    public void onStartButtonClicked() {

        statusLabel.setText("Процесс пошел!");
        Reader reader = new Reader();
        if (chosenDir == null ) {
            statusLabel.setText("Сначала выберите папку");
            statusLabel.setTextFill(Color.web("#cc5555"));
            return;
        } else if (!chosenDir.isDirectory()) {
            statusLabel.setText("Выбранный вами объект не является папкой");
            statusLabel.setTextFill(Color.web("#cc5555"));
            return;
        }

        statusLabel.setText("");
        statusLabel.setTextFill(Color.web("#555555"));

        ArrayList<File> images = reader.init(chosenDir);

        HashMap<String, LinkedList<File>> imagesMap = new HashMap();
        for (File image : images) {
            String fileName = image.getName();
            String imageNameArray[] = fileName.split("_");
            String index = null;
            if (imageNameArray.length == 4 ) {
                index = imageNameArray[2];
            }

            if (index != null) {
                LinkedList<File> imageQueue;
                if ((imageQueue = imagesMap.get(index)) == null) {
                    imageQueue = new LinkedList<>();
                }
                imageQueue.add(image);
                imagesMap.put(index, imageQueue);
            }
        }

        SortedSet<String> keys = new TreeSet<>(imagesMap.keySet());
        int keySize = 0;
        for (String key : keys) {
            Collections.shuffle(imagesMap.get(key));
            keySize++;
        }

        int keyCount;
        int round = 1;
        HashMap<Integer, ArrayList<String>> excelMap = new HashMap<>();

        do {
            keyCount = 0;

            ArrayList<BufferedImage> imageArrayList = new ArrayList<>();
            ArrayList<String> imageNames = new ArrayList<String>();
            double height = 0;
            int counter = 1;

            for (String key : keys) {
                try {
                    File imageFile = imagesMap.get(key).pop();
                    imageNames.add(FilenameUtils.removeExtension(imageFile.getName()));

                    BufferedImage image = ImageIO.read(imageFile);
                    height += image.getHeight();
                    if (height > MAX_HEIGHT) {

                        glueImages(imageArrayList, round, counter);

                        imageArrayList = new ArrayList<>();
                        imageArrayList.add(image);
                        height = image.getHeight();
                        counter++;
                    } else {
                        imageArrayList.add(image);
                    }

                    if (imagesMap.get(key).size() > 0) {
                        keyCount++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (imageArrayList.size() > 0) {
                glueImages(imageArrayList, round, counter);
            }

            excelMap.put(round, imageNames);
            round++;


        } while (keyCount == keySize);

        SortedSet<Integer> excelKeys = new TreeSet<>(excelMap.keySet());
        try {

            WritableWorkbook workbook = Workbook.createWorkbook(new File(chosenDir.getAbsolutePath() + "/Набор/Список.xls"));
            WritableSheet sheet = workbook.createSheet("Варианты", 0);

            int i = 0;
            for (Integer key : excelKeys) {

                sheet.addCell(new Label(0, i, "Вариант " + key));
                i++;

                for (String name : excelMap.get(key)) {
                    sheet.addCell(new Label(0, i, name));
                    i++;
                }

                i++;
            }

            workbook.write();
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        statusLabel.setText("Процесс завершился успешно!");
        statusLabel.setTextFill(Color.web("#55cc55"));
    }

    private String generateImageName(int round, int counter) {
        return "Вариант" + round + "_" + counter;
    }

    private void glueImages(ArrayList<BufferedImage> imageArrayList, int round, int counter) {
        String result = Glue.merge(imageArrayList, generateImageName(round, counter), chosenDir.getAbsolutePath());

        if (result.equals("error")) {
            statusLabel.setText("Ошибка при соединении изоброжении");
            statusLabel.setTextFill(Color.web("#cc5555"));
        } else {
            statusLabel.setText("Вариант " + round + " Страница " + counter + " готов");
        }

    }

}
