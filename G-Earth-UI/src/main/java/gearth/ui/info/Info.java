package gearth.ui.info;

import javafx.scene.control.TextArea;
import gearth.ui.SubForm;

/**
 * Created by Jonas on 06/04/18.
 */
public class Info extends SubForm {
    public TextArea text;

    // this is a TEMPORARY info tab

    public void initialize() {
        String[] lines = {
                "G-Earth 0.1.1",
                "Linux Habbo Packet Manipulator",
                "",
                "Made by:",
                "sirjonasxx",
                "",
                "Contributors:",
                "XePeleato (Windows & Mac support)",
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
