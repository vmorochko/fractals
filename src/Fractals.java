// done scaling
// done controls
// done color mapping
// done distribution stats
// done block output
// todo resize capability
// done gamma correction
// todo mouse dragging
// done add julia set


import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.nio.ByteBuffer;


public class Fractals extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    int pictureWidth = 800;
    int pictureHeight = 600;
    double scalingRatio = .1; // 10%
    double cMax = 2.2;
    double cMin = -cMax;
    double ciMax = (cMax - cMin) / pictureWidth * pictureHeight * .5;
    double ciMin = -ciMax;
    final int maxIter = 1024;
    tupleRGB[] colorsRGB = initColors();
    int[] stats = new int[256];
    double gamma = 2.2;
    byte[] imageByteData;
    ImageView imageView;
    ImageView statsImageView;
    Text text;


    private int convergence(double z, double zi, double c, double ci, int maxIter) {
        double zTemp;
        for (int i = 0; i < maxIter; i++) {
            if (z * z + zi * zi > 4.0) return i;

            zTemp = z * z - (zi * zi);
            zi = 2 * (z * zi) + ci;
            z = zTemp + c;
        }
        return maxIter;
    }


    private byte[] createImageByteData(int width, int height, int[][] buffer) {
        byte[] imageData = new byte[width * height * 3];
        int currentPos = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                imageData[currentPos] = (byte) colorsRGB[buffer[i][j]].red;
                imageData[currentPos + 1] = (byte) colorsRGB[buffer[i][j]].green;
                imageData[currentPos + 2] = (byte) colorsRGB[buffer[i][j]].blue;
                currentPos += 3;
            }
        }
        return imageData;
    }


    private WritableImage fillImage(int width, int height, double cMin, double cMax, double ciMin, double ciMax, int maxIter) {
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();

        int[][] buffer = new int[width][height];
        double cStep = (cMax - cMin) / width;
        double ciStep = (ciMax - ciMin) / height;

        // get raw data
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // buffer[i][j] = convergence(0,0,cMin + i * cStep, ciMin + j * ciStep, maxIter); // mandelbrot set
                buffer[i][j] = convergence(cMin + i * cStep, ciMin + j * ciStep, -0.8, 0.156, maxIter); // julia set
                // buffer[i][j] = (int) (Math.log(convergence(0,0,cMin + i * cStep, ciMin + j * ciStep, maxIter)) * 30);
            }
        }

        // contrast adjustment
        int bufMin = buffer[0][0];
        int bufMax = buffer[0][0];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (buffer[i][j] > bufMax && buffer[i][j] != maxIter) {
                    bufMax = buffer[i][j];
                } else if (buffer[i][j] < bufMin) {
                    bufMin = buffer[i][j];
                }
            }
        }
        System.out.println("bufMin: " + bufMin + " bufMax: " + bufMax);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // black point adjustment
                if (buffer[i][j] == maxIter) {
                    buffer[i][j] = bufMin;
                }
                // dynamic range adjustment
                double tmp = ((double) buffer[i][j] - bufMin) / (bufMax - bufMin);
                // gamma correction
                tmp = Math.pow(tmp, 1.0 / gamma);
                // integer output
                buffer[i][j] = (int) Math.round(tmp * 255);
                // get stats
                stats[buffer[i][j]]++;
            }
        }

        // image output
        imageByteData = createImageByteData(width, height, buffer);
        pixelWriter.setPixels(0, 0, width, height, pixelFormat, imageByteData, 0, width * 3);

        return writableImage;
    }

    private WritableImage fillStatsImage(int[] stats) {
        int maxColor = stats[0];
        for (int i = 0; i < stats.length; i++) {
            if (stats[i] > maxColor) {
                maxColor = stats[i];
            }
        }

        WritableImage writableImage = new WritableImage(256, 256);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int i = 0; i < writableImage.getWidth() - 1; i++) {
            for (int j = (int) (255 - 255.0 * stats[i] / maxColor); j < 256; j++) {
                // pixelWriter.setColor(i, j, Color.BLACK);
                pixelWriter.setColor(i, j, Color.rgb(colorsRGB[i].red, colorsRGB[i].green, colorsRGB[i].blue));
            }
        }
        return writableImage;
    }

    private class tupleRGB {
        public int red, green, blue;

        public tupleRGB(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    private tupleRGB spectralColor(double lambda) // RGB <0,1> <- lambda l <400,700> [nm]
    {
        double t;
        double red = 0.0, green = 0.0, blue = 0.0;

        // red convolution
        if ((lambda >= 400.0) && (lambda < 410.0)) {
            t = (lambda - 400.0) / (410.0 - 400.0);
            red = +(0.33 * t) - (0.20 * t * t);
        } else if ((lambda >= 410.0) && (lambda < 475.0)) {
            t = (lambda - 410.0) / (475.0 - 410.0);
            red = 0.14 - (0.13 * t * t);
        } else if ((lambda >= 545.0) && (lambda < 595.0)) {
            t = (lambda - 545.0) / (595.0 - 545.0);
            red = +(1.98 * t) - (t * t);
        } else if ((lambda >= 595.0) && (lambda < 650.0)) {
            t = (lambda - 595.0) / (650.0 - 595.0);
            red = 0.98 + (0.06 * t) - (0.40 * t * t);
        } else if ((lambda >= 650.0) && (lambda < 700.0)) {
            t = (lambda - 650.0) / (700.0 - 650.0);
            red = 0.65 - (0.84 * t) + (0.20 * t * t);
        }
        // green convolution
        if ((lambda >= 415.0) && (lambda < 475.0)) {
            t = (lambda - 415.0) / (475.0 - 415.0);
            green = +(0.80 * t * t);
        } else if ((lambda >= 475.0) && (lambda < 590.0)) {
            t = (lambda - 475.0) / (590.0 - 475.0);
            green = 0.8 + (0.76 * t) - (0.80 * t * t);
        } else if ((lambda >= 585.0) && (lambda < 639.0)) {
            t = (lambda - 585.0) / (639.0 - 585.0);
            green = 0.84 - (0.84 * t);
        }
        // blue convolution
        if ((lambda >= 400.0) && (lambda < 475.0)) {
            t = (lambda - 400.0) / (475.0 - 400.0);
            blue = +(2.20 * t) - (1.50 * t * t);
        } else if ((lambda >= 475.0) && (lambda < 560.0)) {
            t = (lambda - 475.0) / (560.0 - 475.0);
            blue = 0.7 - (t) + (0.30 * t * t);
        }
        return new tupleRGB((int) Math.round(255 * red), (int) Math.round(255 * green), (int) Math.round(255 * blue));
    }

    private tupleRGB[] initColors() {
        colorsRGB = new tupleRGB[256];
        for (int i = 0; i < colorsRGB.length; i++) {
            colorsRGB[i] = spectralColor(400.0 + (700.0 - 400.0) * i / 255.0);
        }
        return colorsRGB;
    }

    private void adjustCoordinates(double centerWidth, double centerHeight, double scalingRatio) {
        double newCenter = cMin + (cMax - cMin) * centerWidth; // 0.5 for center
        double newiCenter = ciMin + (ciMax - ciMin) * centerHeight; // 0.5 for center
        double newcMin = newCenter - (cMax - cMin) * scalingRatio / 2;
        double newcMax = newCenter + (cMax - cMin) * scalingRatio / 2;
        double newciMin = newiCenter - (ciMax - ciMin) * scalingRatio / 2;
        double newciMax = newiCenter + (ciMax - ciMin) * scalingRatio / 2;
        cMin = newcMin;
        cMax = newcMax;
        ciMin = newciMin;
        ciMax = newciMax;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Fractals");
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                Platform.exit();
            }
        });

        Button zoomInButton = new Button("Zoom In (+)");
        zoomInButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                new Runnable() {
                    @Override
                    public void run() {
                        adjustCoordinates(0.5, 0.5, scalingRatio);
                        imageView.setImage(fillImage(pictureWidth, pictureHeight, cMin, cMax, ciMin, ciMax, maxIter));
                        text.setText("cMin: " + cMin + " cMax: " + cMax + " ciMin: " + ciMin + " ciMax: " + ciMax);
                        statsImageView.setImage(fillStatsImage(stats));
                    }
                }.run();
            }
        });

        Button zoomOutButton = new Button("Zoom Out (-)");
        zoomOutButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                new Runnable() {
                    @Override
                    public void run() {
                        adjustCoordinates(0.5, 0.5, 1.0 / scalingRatio);
                        imageView.setImage(fillImage(pictureWidth, pictureHeight, cMin, cMax, ciMin, ciMax, maxIter));
                        text.setText("cMin: " + cMin + " cMax: " + cMax + " ciMin: " + ciMin + " ciMax: " + ciMax);
                        statsImageView.setImage(fillStatsImage(stats));
                    }
                }.run();
            }
        });

        ChangeListener<Number> numberChangeListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                // todo
            }
        };

        EventHandler<MouseEvent> mouseClickOnImageEventHandler = new EventHandler<>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                new Runnable() {
                    @Override
                    public void run() {
                        adjustCoordinates(mouseEvent.getSceneX() / pictureWidth, mouseEvent.getSceneY() / pictureHeight, scalingRatio);
                        imageView.setImage(fillImage(pictureWidth, pictureHeight, cMin, cMax, ciMin, ciMax, maxIter));
                        text.setText("cMin: " + cMin + " cMax: " + cMax + " ciMin: " + ciMin + " ciMax: " + ciMax);
                        statsImageView.setImage(fillStatsImage(stats));
                    }
                }.run();
            }
        };

        WritableImage writableImage = fillImage(pictureWidth, pictureHeight, cMin, cMax, ciMin, ciMax, maxIter);
        imageView = new ImageView(writableImage);

        text = new Text();
        text.setText("cMin: " + cMin + " cMax: " + cMax + " ciMin: " + ciMin + " ciMax: " + ciMax);

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().add(zoomInButton);
        hBox.getChildren().add(zoomOutButton);
        hBox.getChildren().add(text);

        // add listeners
        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseClickOnImageEventHandler);
        stage.widthProperty().addListener(numberChangeListener);
        stage.heightProperty().addListener(numberChangeListener);

        // compose view
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(imageView);
        borderPane.setBottom(hBox);
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();

        // tmp stats
        Stage statsStage = new Stage();
        statsStage.setTitle("stats");
        statsStage.setAlwaysOnTop(true);
        WritableImage statsImage = fillStatsImage(stats);
        statsImageView = new ImageView(statsImage);
        BorderPane statsBorderPane = new BorderPane();
        statsBorderPane.setCenter(statsImageView);
        Scene statsScene = new Scene(statsBorderPane);
        statsStage.setScene(statsScene);
        statsStage.show();
    }
}
