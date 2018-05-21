package sample;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import jxl.Workbook;
import jxl.write.*;
import jxl.write.Number;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Controller {

    @FXML
    private javafx.scene.control.Label statusLabel;
    @FXML
    private ProgressBar progressBar;

    private File chosenDir;
    private final double MAX_HEIGHT = Glue.HEIGHT - Glue.MARGIN_TOP;

    public void onDirChooserButtonClicked() {

        DirectoryChooser dirChooser = new DirectoryChooser();
        this.chosenDir = dirChooser.showDialog(Main.stage);
        statusLabel.setText("Нажмите кнопку \"Старт\"");
        statusLabel.setTextFill(Color.web("#222222"));
    }

    public void onStartButtonClicked() {

        statusLabel.setText("Процесс пошел!");
        Reader reader = new Reader();
        if (chosenDir == null ) {
            statusLabel.setText("Сначала выберите папку");
            statusLabel.setTextFill(Color.web("#aa2222"));
            return;
        } else if (!chosenDir.isDirectory()) {
            statusLabel.setText("Выбранный вами объект не является папкой");
            statusLabel.setTextFill(Color.web("#aa2222"));
            return;
        }

        statusLabel.setText("");
        statusLabel.setTextFill(Color.web("#222222"));

        String msg = null;

        ArrayList<File> images = reader.init(chosenDir);
        HashMap<String, LinkedList<File>> imagesMap = new HashMap();
        String subjectIndex = null;
        int max = 0;

        for (File image : images) {
            String fileName = image.getName();

            System.out.println(fileName);
            if (!fileName.matches("[0-9]{4}_[0-9]{2}_[0-9]{2}_[a-zA-Z]*.(jpg|jpeg)")) {
                msg = "Ошибка: " + fileName;
                break;
            }

            String imageNameArray[] = fileName.split("_");
            String index = null;
            if (imageNameArray.length == 4 ) {
                index = imageNameArray[2];
                if (subjectIndex == null) {
                    subjectIndex = imageNameArray[1];
                }
            }

            if (index != null) {
                LinkedList<File> imageQueue;
                if ((imageQueue = imagesMap.get(index)) == null) {
                    imageQueue = new LinkedList<>();
                }
                imageQueue.add(image);
                imagesMap.put(index, imageQueue);

                if (imageQueue.size() > max) {
                    max = imageQueue.size();
                }
            } else {
                msg = "Ошибка: " + image.getName();
            }
        }

        if (msg != null) {
            statusLabel.setText(msg);
            statusLabel.setTextFill(Color.web("#aa2222"));
            return;
        }

        Glue.setFolderName(subjectIndex);

        SortedSet<String> keys = new TreeSet<>(imagesMap.keySet());
        int keySize = 0;
        for (String key : keys) {
            Collections.shuffle(imagesMap.get(key));
            keySize++;
        }

        Task task = setTask(keys, imagesMap, max, subjectIndex, keySize);

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                statusLabel.setText((String) task.getValue());
                statusLabel.setTextFill(Color.web("#22aa22"));
            }
        });

        task.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                statusLabel.setText((String) task.getValue());
                statusLabel.setTextFill(Color.web("#aa2222"));
            }
        });

        progressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    private String generateImageName(String subjectIndex, int round, int counter) {
        return subjectIndex + "_" + round + "_" + counter;
    }

    private void glueImages(ArrayList<BufferedImage> imageArrayList, String subjectIndex, int round, int counter) {
        String result = Glue.merge(imageArrayList, generateImageName(subjectIndex, round, counter), chosenDir.getAbsolutePath());
    }

    public Task setTask(SortedSet<String> keys, HashMap<String, LinkedList<File>> imagesMap,
                        int max, String subjectIndex, int keySize) {
        return new Task<String>() {

            @Override protected String call() throws Exception {

                String msg = "Готово";

                int keyCount;
                int round = 1;
                HashMap<Integer, ArrayList<String>> excelMap = new HashMap<>();
                double minUnit = 1.0 / max;
                double initUnit = minUnit;

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

                                glueImages(imageArrayList, subjectIndex, round, counter);

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
                            msg = "Ошибка при соединении рисунков";
                            e.printStackTrace();
                        }
                    }

                    if (imageArrayList.size() > 0) {
                        glueImages(imageArrayList, subjectIndex, round, counter);
                    }

                    excelMap.put(round, imageNames);
                    round++;

                    updateProgress(initUnit, 1.0);
                    initUnit += minUnit;

                } while (keyCount == keySize);

                SortedSet<Integer> excelKeys = new TreeSet<>(excelMap.keySet());
                try {

                    WritableWorkbook workbook = Workbook.createWorkbook(new File(chosenDir.getAbsolutePath()
                            + "/" + Glue.folderName()
                            + "//Список.xls"));
                    WritableSheet sheet = workbook.createSheet("Варианты", 0);

                    int i = 0;
                    for (Integer key : excelKeys) {

                        sheet.addCell(new jxl.write.Label(0, i, "Вариант " + key));
                        i++;
                        int counter = 1;

                        for (String name : excelMap.get(key)) {
                            sheet.addCell(new jxl.write.Label(0, i, name));
                            sheet.addCell(new Number(2, i, counter));
                            counter++;
                            i++;
                        }

                        i++;
                    }

                    workbook.write();
                    workbook.close();

                } catch (Exception e) {
                    msg = "Ошибка при записи в Excel";
                    e.printStackTrace();
                }

                return msg;
            }

        };
    }

}
