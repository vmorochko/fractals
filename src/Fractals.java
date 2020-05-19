// done scaling
// done controls
// done color mapping
// done distribution stats
// done block output
// todo resize capability
// done gamma correction
// todo mouse dragging
// done add julia set
// done expand controls
// todo optimize recalculation


import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
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
    final int maxIter = 4096;
    TupleRGB[] colorsRGB = initColors();
    int[] stats = new int[256];
    double gamma = 2.2;
    byte[] imageByteData;
    ImageView imageView;
    ImageView statsImageView;
    Text text;
    Stage statsStageGlobal;
    TypeOfSet typeOfSet = TypeOfSet.MANDELBROT;

    boolean isLogCorrectionNeeded = true;
    boolean isGammaCorrectionNeeded = true;
    boolean isDynamicRangeAdjustmentNeeded = true;
    boolean isStatsNeeded = false;


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
        if (typeOfSet == TypeOfSet.MANDELBROT) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    // mandelbrot set
                    buffer[i][j] = convergence(0, 0, cMin + i * cStep, ciMin + j * ciStep, maxIter);
                }
            }
        } else if (typeOfSet == TypeOfSet.JULIA) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    // julia set
                    buffer[i][j] = convergence(cMin + i * cStep, ciMin + j * ciStep, -0.8, 0.156, maxIter);
                }
            }
        }


        // find minimum value for black point adjustment
        int bufMin = buffer[0][0];
        int bufMax = buffer[0][0];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (buffer[i][j] < bufMin) {
                    bufMin = buffer[i][j];
                } else if (buffer[i][j] > bufMax) {
                    bufMax = buffer[i][j]; // for statistics
                }
            }
        }
        System.out.println("bufMin: " + bufMin + " bufMax: " + bufMax);
        // black point adjustment
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (buffer[i][j] == maxIter) {
                    buffer[i][j] = bufMin;
                }
            }
        }

        // tmp transfer to floating point buffer
        double[][] tmp = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                tmp[i][j] = (double) buffer[i][j];
            }
        }

        // log scale
        if (isLogCorrectionNeeded) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (tmp[i][j] != 0.0) {
                        tmp[i][j] = Math.log(tmp[i][j]) * 30; // todo
                    }
                }
            }
        }

        // find black and white points for contrast adjustment
        if (isDynamicRangeAdjustmentNeeded) {
            double blackPoint = tmp[0][0];
            double whitePoint = tmp[0][0];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (tmp[i][j] > whitePoint) {
                        whitePoint = tmp[i][j];
                    } else if (tmp[i][j] < blackPoint) {
                        blackPoint = tmp[i][j];
                    }
                }
            }
            System.out.println("blackPoint: " + blackPoint + " whitePoint: " + whitePoint);
            // dynamic range adjustment
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    tmp[i][j] = (tmp[i][j] - blackPoint) / (whitePoint - blackPoint);
                }
            }
        }

        // gamma correction
        if (isGammaCorrectionNeeded) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    tmp[i][j] = Math.pow(tmp[i][j], 1.0 / gamma);
                }
            }
        }

        // output to buffer
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // integer output
                buffer[i][j] = (int) Math.round(tmp[i][j] * 255);
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

    private class TupleRGB {
        public int red, green, blue;

        public TupleRGB(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    private enum TypeOfSet {
        MANDELBROT,
        JULIA
    }

    private TupleRGB spectralColor(double lambda) // RGB <0,1> <- lambda l <400,700> [nm]
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
        return new TupleRGB((int) Math.round(255 * red), (int) Math.round(255 * green), (int) Math.round(255 * blue));
    }

    private TupleRGB[] initColors() {
        colorsRGB = new TupleRGB[256];
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

    private void refreshStats() {
        if (isStatsNeeded) {
            statsImageView.setImage(fillStatsImage(stats));
        }
    }

    private void refreshView() {
        imageView.setImage(fillImage(pictureWidth, pictureHeight, cMin, cMax, ciMin, ciMax, maxIter));
        text.setText("cMin: " + cMin + " cMax: " + cMax + " ciMin: " + ciMin + " ciMax: " + ciMax);
        refreshStats();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Fractals");
        stage.setResizable(false);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                Platform.exit();
            }
        });

        Button zoomInButton = new Button();
        zoomInButton.setText("Zoom In (+)");
        zoomInButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                new Runnable() {
                    @Override
                    public void run() {
                        adjustCoordinates(0.5, 0.5, scalingRatio);
                        refreshView();
                    }
                }.run();
            }
        });

        Button zoomOutButton = new Button();
        zoomOutButton.setText("Zoom Out (-)");
        zoomOutButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                new Runnable() {
                    @Override
                    public void run() {
                        adjustCoordinates(0.5, 0.5, 1.0 / scalingRatio);
                        refreshView();
                    }
                }.run();
            }
        });

        CheckBox logCorrectionCheckbox = new CheckBox();
        logCorrectionCheckbox.setText("Log Correction");
        logCorrectionCheckbox.setSelected(isLogCorrectionNeeded);
        logCorrectionCheckbox.setIndeterminate(false);
        logCorrectionCheckbox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                isLogCorrectionNeeded = logCorrectionCheckbox.isSelected();
                refreshView();
            }
        });

        CheckBox gammaCorrectionCheckbox = new CheckBox();
        gammaCorrectionCheckbox.setText("Gamma Correction");
        gammaCorrectionCheckbox.setSelected(isGammaCorrectionNeeded);
        gammaCorrectionCheckbox.setIndeterminate(false);
        gammaCorrectionCheckbox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                isGammaCorrectionNeeded = gammaCorrectionCheckbox.isSelected();
                refreshView();
            }
        });

        /*
        CheckBox dynamicRangeAdjustmentCheckbox = new CheckBox();
        dynamicRangeAdjustmentCheckbox.setText("Dynamic Range Adjustment");
        dynamicRangeAdjustmentCheckbox.setSelected(isDynamicRangeAdjustmentNeeded);
        dynamicRangeAdjustmentCheckbox.setIndeterminate(false);
        dynamicRangeAdjustmentCheckbox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                isDynamicRangeAdjustmentNeeded = dynamicRangeAdjustmentCheckbox.isSelected();
                imageView.setImage(fillImage(pictureWidth, pictureHeight, cMin, cMax, ciMin, ciMax, maxIter));
                statsImageView.setImage(fillStatsImage(stats));
            }
        });
        */

        CheckBox statsCheckbox = new CheckBox();
        statsCheckbox.setText("Show stats");
        statsCheckbox.setSelected(isStatsNeeded);
        statsCheckbox.setIndeterminate(false);
        statsCheckbox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                isStatsNeeded = statsCheckbox.isSelected();
                if (isStatsNeeded) {
                    statsStageGlobal.show();
                } else {
                    statsStageGlobal.hide();
                }
                // refreshView();
            }
        });

        ToggleGroup toggleGroup = new ToggleGroup();
        RadioButton radioButtonMandelbrot = new RadioButton();
        radioButtonMandelbrot.setText("Mandelbrot");
        if (typeOfSet == TypeOfSet.MANDELBROT) {
            radioButtonMandelbrot.setSelected(true);
        }
        radioButtonMandelbrot.setToggleGroup(toggleGroup);
        RadioButton radioButtonJulia = new RadioButton();
        radioButtonJulia.setText("Julia");
        radioButtonJulia.setToggleGroup(toggleGroup);
        if (typeOfSet == TypeOfSet.JULIA) {
            radioButtonJulia.setSelected(true);
        }

        radioButtonMandelbrot.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (radioButtonMandelbrot.isSelected()) {
                    typeOfSet = TypeOfSet.MANDELBROT;
                    refreshView();
                }
            }
        });

        radioButtonJulia.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (radioButtonJulia.isSelected()) {
                    typeOfSet = TypeOfSet.JULIA;
                    refreshView();
                }
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
                        refreshView();
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
        statsStageGlobal = statsStage;
        statsStage.setTitle("stats");
        statsStage.setAlwaysOnTop(true);
        statsStage.setResizable(false);
        statsStage.setX(0);
        statsStage.setY(0);
        statsStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                isStatsNeeded = false;
                statsCheckbox.setSelected(false);
            }
        });

        WritableImage statsImage = fillStatsImage(stats);
        statsImageView = new ImageView(statsImage);
        BorderPane statsBorderPane = new BorderPane();
        statsBorderPane.setCenter(statsImageView);
        Scene statsScene = new Scene(statsBorderPane);
        statsStage.setScene(statsScene);
        if (isStatsNeeded) {
            statsStage.show();
        }

        // tmp controls
        Stage controlsStage = new Stage();
        controlsStage.setTitle("controls");
        controlsStage.setAlwaysOnTop(true);
        controlsStage.setX(0);


        VBox contolsBox = new VBox();
        contolsBox.setSpacing(10);
        contolsBox.setAlignment(Pos.CENTER_LEFT);
        // todo add controls here
        contolsBox.getChildren().add(logCorrectionCheckbox);
        contolsBox.getChildren().add(gammaCorrectionCheckbox);
        contolsBox.getChildren().add(statsCheckbox);
        contolsBox.getChildren().add(radioButtonMandelbrot);
        contolsBox.getChildren().add(radioButtonJulia);
        // contolsBox.getChildren().add(dynamicRangeAdjustmentCheckbox);
        // contolsBox.getChildren().add(text);


        // BorderPane controlsBorderPane = new BorderPane();
        // controlsBorderPane.setCenter(null);
        // Scene controlsScene = new Scene(controlsBorderPane);
        Scene controlsScene = new Scene(contolsBox);
        controlsStage.setScene(controlsScene);
        controlsStage.show();

    }
}
