package gearth.app.ui;


import gearth.app.protocol.HConnection;

public class SubForm {

    protected GEarthController parentController;

    //gets called when all UI elements are initialized, set parentController available & stage field is initialized
    public void setParentController(GEarthController controller) {
        parentController = controller;
        onParentSet();
    }


    //abstract but non required
    protected void onParentSet() {

    }

    public void exit() {
        onExit();
    }

    //abstract but non required
    protected void onExit() {

    }

    protected void onTabOpened() {

    }

    protected HConnection getHConnection() {
        if (parentController == null) {
            return null;
        }
        return parentController.getHConnection();
    }
    protected void writeToLog(javafx.scene.paint.Color color, String text) {
        parentController.writeToLog(color, text);
    }

}
