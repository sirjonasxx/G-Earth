package main.ui.info;

import javafx.scene.control.TextArea;
import main.ui.SubForm;

/**
 * Created by Jonas on 06/04/18.
 */
public class Info extends SubForm {
    public TextArea text;


    public void initialize() {
        String[] lines = {
                "G-Earth 0.1.1",
                "Linux Habbo Packet Manipulator",
                "",
                "Made by:",
                "sirjonasxx",
                "",
                "Special thanks to:",
                "LittleJ",
                "ArachisH",
                "",
                "Check out:",
                "sngforum.info",
                "darkbox.nl"
        };

        String all = lines[0];
        for (int i = 1; i < lines.length; i++) {
            all += (System.lineSeparator() + lines[i]);
        }


        text.setText(
              all
        );
    }
}
