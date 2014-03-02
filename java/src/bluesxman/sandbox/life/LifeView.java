package bluesxman.sandbox.life;

import java.util.Random;
import java.util.function.IntSupplier;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;

public class LifeView extends Application {
	 
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

}