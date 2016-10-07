package gui;

import javafx.scene.control.TextField;

/**
 * Created by tkaczenko on 07.10.16.
 */
public class NumberTextField extends TextField {
    @Override
    public void replaceText(int start, int end, String text) {
        if (text.matches("[0-9]") || text.isEmpty()) {
            super.replaceText(start, end, text);
        }
    }

    @Override
    public void replaceSelection(String replacement) {
        super.replaceSelection(replacement);
    }
}
