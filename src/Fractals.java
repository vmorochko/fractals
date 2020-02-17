// todo block output https://docs.oracle.com/javafx/2/image_ops/jfxpub-image_ops.htm
// todo scaling controls
// todo resize capability
// todo sigmoid correction + distribution stats https://en.wikipedia.org/wiki/Sigmoid_function


import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class Fractals extends Application{
    public static void main(String[] args) {
        launch(args);
    }

    public static int convergence(double c, double ci, int maxIter) {
        double z = 0;
        double zi = 0;
        double zT = 0;
        double ziT = 0;
        for (int i = 0; i < maxIter; i++) {
            if (z*z + zi*zi > 4.0) return i;

            ziT = 2*(z*zi);
            zT =z*z-(zi*zi);
            z = zT + c;
            zi = ziT + ci;
        }
        return maxIter;
    }

    public static WritableImage fillImage(int width, int height,  double cMin, double cMax, double ciMin, double ciMax, int maxIter)
    {
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        int[][] buf = new int[width][height];
        double cStep = (cMax - cMin) / width;
        double ciStep = (ciMax - ciMin) / height;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                buf[i][j] = (int) (Math.log(convergence(cMin + i * cStep, ciMin + j * ciStep, maxIter)) * 36.9329931);
            }
        }

        int buf_min = buf[0][0];
        int buf_max = buf[0][0];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (buf[i][j] > buf_max) {
                    buf_max = buf[i][j];
                } else
                if (buf[i][j] < buf_min) {
                    buf_min = buf[i][j];
                }
            }
        }
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                buf[i][j] = (int) ((double)(buf[i][j] - buf_min) / (buf_max - buf_min) * 255); // byte color
            }
        }

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixelWriter.setColor(i, j, Color.grayRgb(buf[i][j]));
            }
        }
        return writableImage;
    }

    WritableImage writableImage;
    ImageView imageView;
    double cMin = -2.2;
    double cMax = 1.1;
    double ciMin = -1.2;
    double ciMax = 1.2;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Fractals");

        int width = 800;
        int height = 600;

        GridPane gridPane = new GridPane();
        Scene scene = new Scene(gridPane, width, height, Color.WHITE);

        int maxIter = 1024;
        double scalingRatio = .1; // 10%

        writableImage = fillImage(width, height, cMin, cMax, ciMin, ciMax, maxIter);
        imageView = new ImageView(writableImage);
        gridPane.add(imageView, 0, 0);

        EventHandler<MouseEvent> eventHandler = new EventHandler<>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                new Runnable() {
                    @Override
                    public void run() {
                        double newCenter = cMin + (cMax - cMin) * mouseEvent.getSceneX() / width;
                        double newiCenter = ciMin + (ciMax - ciMin) * mouseEvent.getSceneY() / height;
                        double newcMin = newCenter - (cMax - cMin) * scalingRatio / 2;
                        double newcMax = newCenter + (cMax - cMin) * scalingRatio / 2;
                        double newciMin = newiCenter - (ciMax - ciMin) * scalingRatio / 2;
                        double newciMax = newiCenter + (ciMax - ciMin) * scalingRatio / 2;
                        cMin = newcMin;
                        cMax = newcMax;
                        ciMin = newciMin;
                        ciMax = newciMax;
                        imageView.setImage(fillImage(width, height, cMin, cMax, ciMin, ciMax, maxIter));
                    }
                }.run();
            }
        };
        stage.addEventFilter(MouseEvent.MOUSE_CLICKED, eventHandler);
        stage.setScene(scene);
        stage.show();
    }
}
