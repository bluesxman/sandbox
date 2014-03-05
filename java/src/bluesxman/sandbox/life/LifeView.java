package bluesxman.sandbox.life;

import java.nio.IntBuffer;
import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
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
    private static final int CANVAS_X = 300;
    private static final int CANVAS_Y = 250;
    private static final long FRAME_SIZE_NANO = 16666666;
    private static final int SQUARE_SIZE = 10;
    private static final PixelFormat<IntBuffer> PIXEL_FORMAT =  PixelFormat.getIntArgbInstance();
    private static final int[] BLACK_SQUARE_BUF = createSquareBuffer(Color.BLACK);
    private static final int[] WHITE_SQUARE_BUF = createSquareBuffer(Color.WHITE);    
  
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Canvas canvas = new Canvas(CANVAS_X, CANVAS_Y);
        GraphicsContext canvasGC = canvas.getGraphicsContext2D();
        Random rand = new Random();
        WritableImage image = new WritableImage(CANVAS_X, CANVAS_Y);
        
        primaryStage.setTitle("Life View Test");
        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root));

        Runnable blinkSquares = () -> {
            int x, y;
            Color color;
            long nextFrame = System.nanoTime() + FRAME_SIZE_NANO;

            for(int i = 0; i < 1e6; i++){               
                color = i % 2 == 0 ? Color.BLACK : Color.WHITE;
                x = rand.nextInt(CANVAS_X - SQUARE_SIZE);
                y = rand.nextInt(CANVAS_Y - SQUARE_SIZE);
                drawSquareToBuffer(image, color, x, y);

                if(System.nanoTime() >= nextFrame){
                    try {
                        invokeAndWaitFX( () -> {
                            canvasGC.drawImage(image, 0, 0);
//                            canvasGC.setFill(Color.BLACK);
//                            canvasGC.fillRect(rand.nextInt(CANVAS_X - SQUARE_SIZE), rand.nextInt(CANVAS_X - SQUARE_SIZE), 10, 10);
                            primaryStage.show();
                        });
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    nextFrame += FRAME_SIZE_NANO;
                }
            }
        };

        (new Thread(blinkSquares, "Blink Thread")).start();
        
        
    }
    
    public static int[] createSquareBuffer(Color color){
        int[] buf = new int[SQUARE_SIZE * SQUARE_SIZE];
        int c = color == Color.BLACK ? 0xFF000000 : 0xFFFFFFFF;
        
        for(int i = 0; i < buf.length; i++){
            buf[i] = c;
        }
        
        return buf;
    }
    
    public static WritableImage drawSquareToBuffer(WritableImage buffer, Color color, int x, int y){
        int[] buf = color == Color.BLACK ? BLACK_SQUARE_BUF : WHITE_SQUARE_BUF;
        buffer.getPixelWriter().setPixels(x, y, SQUARE_SIZE, SQUARE_SIZE, PIXEL_FORMAT, buf, 0, 0);
        return buffer;
    }

    /**
     * Run a function on the JavaFX application thread and block until it completes.
     * 
     * Puts the function on the event queue to be handled by the JavaFX application thread
     * via Platform.runLater() and blocks the caller until the execution of the function completes.
     * 
     * @param lambda The function to invoke on the JavaFX app thread
     */
    public static void invokeAndWaitFX(Runnable lambda) throws InterruptedException{
        // synch the whole section to guarantee that notifyAll()
        // is executed after the call to wait()
        synchronized(lambda){
            Platform.runLater( () -> {
                synchronized(lambda){
                    lambda.run();
                    lambda.notifyAll();
                }
            });
     
            lambda.wait(); // monitor released, runLater lambda can execute
        }
    }
}