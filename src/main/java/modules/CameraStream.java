package modules;

import databases.JSONHelper;
import helpers.Constants;
import helpers.LoggingUtil;
import helpers.Utils;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.logging.Level;

/***
 * @author Dalton Smith
 * CameraStream
 * Manages grabbing frames from the camera, and reading the bar codes.
 */

public class CameraStream {
    private VideoCapture capture = new VideoCapture(0);
    private boolean stopCamera = false;
    private UserProcess process = new UserProcess();
    private JSONHelper parser = new JSONHelper();
    private Utils utils = new Utils();

    public void displayImage(GridPane root) {
        startWebCamStream(root);

    }

    //grab frames from camera
    private void startWebCamStream(GridPane root) {
        GridPane subRoot = new GridPane();
        Size sz = new Size(Constants.cameraWidth, Constants.cameraHeight);
        Mat frame = new Mat();
        ImageView currentFrame = new ImageView();

        File errorImage = new File(Utils.getCurrentDir() + "\\images\\error.png");

        LoggingUtil.log(Level.INFO, "Path to fallback: " + Utils.getCurrentDir() + "\\images\\error.png");
        //open the camera
        capture.retrieve(frame);

        //check if camera opened successfully, or is disabled
        if (!capture.isOpened() || parser.getKey("enableCamera").equals("false")) {
          LoggingUtil.log(Level.WARNING, "Camera disabled or unable to be opened.");

            Image image;

            LoggingUtil.log(Level.INFO, "Does custom fallback exist? " +  errorImage.exists());
            //custom error image
            if (errorImage.exists()){
                image = new Image(errorImage.toURI().toString());
            } else {
                image = new Image("images/error.png");
            }

            //set image properties
            currentFrame.setImage(image);
            currentFrame.setFitHeight(Constants.cameraHeight);
            currentFrame.setFitWidth(Constants.cameraWidth);

            subRoot.add(currentFrame, 0, 0);
            root.add(subRoot, 0, 0);

            //close the camera if successfully opened
            capture.release();

            return;

        }

        //grab frames from the camera
        Runnable frameGrabber = () -> {
            int prevID = 0;

            while (!stopCamera) {

                try {
                    //read frames from teh camera and display them
                    capture.read(frame);
                    Imgproc.resize(frame, frame, sz);
                    Core.flip(frame, frame, 1);
                    MatOfByte buffer = new MatOfByte();
                    Imgcodecs.imencode(".png", frame, buffer);
                    Image imageToShow = new Image(new ByteArrayInputStream(buffer.toArray()));

                    Platform.runLater(() -> currentFrame.setImage(imageToShow));
                } catch (Exception e) {
                    LoggingUtil.log(Level.SEVERE, e);
                    utils.createAlert("Camera disabled", "Camera disabled", "The camera has been disabled, please restart the application to re-enable", Alert.AlertType.ERROR);
                    stopCamera = true;

                }

            }

        };

        //start camera thread
        Thread t = new Thread(frameGrabber);
        t.setDaemon(true);
        t.start();

        subRoot.add(currentFrame, 0, 0);
        root.add(subRoot, 0, 0);
    }

}
