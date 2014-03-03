package bluesxman.sandbox.life;

import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/*
 * Have thread updating an image (WritablePixelFormat?).
 * Have render loop draw the latest image at 60 FPS
 * Synchronization without starvation?
 * Possible to pause image updates until the new frame has rendered?
 * JavaFX event for frame complete?
 * 
 * Idea:  Have the thread updating the image do a wait() on a monitor
 * (e.g. the image's).  In the lambda passed to Platform.runLater(), have a 
 * notify() called on the monitor after the show()
 */
public class LifeView extends Application {
    private GraphicsContext gc;
    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Drawing Operations Test");
        Group root = new Group();
        Canvas canvas = new Canvas(300, 250);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root));
        Random rand = new Random();

        Runnable blinkSquare = () -> {
            for(int i = 0; i < 1000; i++){
                boolean black = i % 2 == 0 ; 

                Platform.runLater( () -> {
                    gc.setFill(black ? Color.BLACK : Color.WHITE);
                    gc.fillRect(rand.nextInt(289), rand.nextInt(239), 10, 10); 
                    primaryStage.show();
                });

                try {
                    Thread.sleep(17);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        (new Thread(blinkSquare, "Blink Thread")).start();
    }

    /**
     * Renders the image synchronously.
     * 
     * Enqueues the frame for rendering by the JavaFX application thread
     * and blocks the caller until the frame has rendered.
     * 
     * @param frame The new image to render on the canvas
     */
    public void renderFrame(Image frame){
        // synch the whole section to guarantee the notifyAll()
        // is executed after we start waiting
        synchronized(frame){
            Platform.runLater( () -> {
                synchronized(frame){
                    gc.drawImage(frame, 0, 0);
                    stage.show();
                    frame.notifyAll();
                }
            });

            try {
                frame.wait(); // monitor released, runLater lambda can execute
            } catch (InterruptedException e) {
                System.err.println("Interrupted waiting on frame redraw.");
                e.printStackTrace();
            }
        }
    }
}