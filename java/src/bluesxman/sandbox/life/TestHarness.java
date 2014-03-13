package bluesxman.sandbox.life;

public class TestHarness {
    public static void main(String[] args) throws InterruptedException {
        LifeView lv = LifeView.createInstance();
        lv.setSquare(119, 0, true);
        lv.setSquare(5, 0, true);
        lv.setSquare(0, 59, true);
        lv.render();
    }
}
