package main.extensions;

/**
 * Created by Jonas on 24/06/18.
 */
public class SimpleTestExtension extends Extension {

    public static void main(String[] args) {
        new SimpleTestExtension(args);
    }

    private SimpleTestExtension(String[] args) {
        super(args);
    }

    @Override
    protected void init() {
        System.out.println("init");
    }

    @Override
    protected void onDoubleClick() {
        System.out.println("doubleclick");
    }

    @Override
    protected void onStartConnection() {
        System.out.println("connection started");
    }

    @Override
    protected void onEndConnection() {
        System.out.println("connection ended");
    }

    @Override
    protected String getTitle() {
        return "Simple Test!";
    }

    @Override
    protected String getDescription() {
        return "But just for testing purpose";
    }

    @Override
    protected String getVersion() {
        return "0.1";
    }

    @Override
    protected String getAuthor() {
        return "sirjonasxx";
    }
}
