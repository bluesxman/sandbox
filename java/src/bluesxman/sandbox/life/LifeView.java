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
    private static final int CANVAS_X = 1200;
    private static final int CANVAS_Y = 600;
    private static final int SQUARE_SIZE = 5;
    private static final PixelFormat<IntBuffer> PIXEL_FORMAT =  PixelFormat.getIntArgbInstance();
    private static final int[] BLACK_SQUARE_BUF = createSquareBuffer(Color.BLACK);
    private static final int[] WHITE_SQUARE_BUF = createSquareBuffer(Color.WHITE);    
  
    private static LifeView instance;
    
    private WritableImage buffer;
    private GraphicsContext canvasGC;
    private Stage primaryStage;
    
    public static LifeView createInstance() throws InterruptedException {
        synchronized(LifeView.class){
            if(instance == null){
                (new Thread(() -> launch(new String[]{}), "LifeView")).start();
                while(instance == null){
                    System.out.println("Waiting on app to launch");
                    LifeView.class.wait();
                }
            }
            System.out.println("Launched!");
            return instance;
        }
    }
    
    public void setSquare(int x, int y, boolean alive){
        drawSquareToBuffer(
                buffer, 
                alive ? BLACK_SQUARE_BUF : WHITE_SQUARE_BUF, 
                x * SQUARE_SIZE, 
                y * SQUARE_SIZE);
    }
    
    public void render() throws InterruptedException{
        invokeAndWaitFX( () -> {
            canvasGC.drawImage(buffer, 0, 0);
            primaryStage.show();
        });
    }
    
    private static void setInstance(LifeView app){
        synchronized(LifeView.class){
            if(instance == null){
                instance = app;
            }
            LifeView.class.notifyAll();
        }
    }
    
    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Canvas canvas = new Canvas(CANVAS_X, CANVAS_Y);
        canvasGC = canvas.getGraphicsContext2D();
        buffer = new WritableImage(CANVAS_X, CANVAS_Y);
        this.primaryStage = primaryStage;
        
        primaryStage.setTitle("Life");
        root.getChildren().add(canvas);
        primaryStage.setScene(new Scene(root));
        
        setInstance(this);
    }
    
    public static int[] createSquareBuffer(Color color){
        int[] buf = new int[SQUARE_SIZE * SQUARE_SIZE];
        int c = color == Color.BLACK ? 0xFF000000 : 0xFFFFFFFF;
        
        for(int i = 0; i < buf.length; i++){
            buf[i] = c;
        }
        
        return buf;
    }
    
    public static void drawSquareToBuffer(WritableImage buffer, int[] square, int x, int y){
        buffer.getPixelWriter().setPixels(x, y, SQUARE_SIZE, SQUARE_SIZE, PIXEL_FORMAT, square, 0, 0);
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