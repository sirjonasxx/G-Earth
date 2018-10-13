package gearth.ui;


import gearth.protocol.HConnection;

public class SubForm {

    protected GEarthController parentController;

    public void setParentController(GEarthController controller) {
        parentController = controller;
        onParentSet();
    }


    //abstract
    protected void onParentSet() {

    }

    protected HConnection getHConnection() {
        return parentController.getHConnection();
    }
    protected void writeToLog(javafx.scene.paint.Color color, String text) {
        parentController.writeToLog(color, text);
    }

}
