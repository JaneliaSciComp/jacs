
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * @author Michael Press
 */
public class LongParameterRenderer extends IncrementableParameterRenderer {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.renderers.LongParameterRenderer");

    TextBox _textBox;

    public LongParameterRenderer(LongParameterVO param, String key, Task task) {
        super(param, key, task);
    }

    protected LongParameterVO getLongParam() {
        return (LongParameterVO) getValueObject();
    }

    protected Widget createPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        // Create the text entry area
        _textBox = new TextBox();
        _textBox.setTextAlignment(TextBoxBase.ALIGN_RIGHT);
        //TODO: dynamically expand visible length if value exceeds 4 digits
        _textBox.setVisibleLength(2);
        _textBox.setText(String.valueOf(getLongParam().getActualValue()));
        //TODO: use mouse listener to catch deletes via mouse select and use of the delete item on the popup menu
        _textBox.addKeyboardListener(new ValueChangedListener());
        panel.add(_textBox);

        panel.add(getIncrementButtons(new LongIncrementCallback(), new LongDecrementCallback()));
        _textBox.setStyleName("textBox");

        // Create the hint label
        //panel.add(getHintLabel());
        return panel;
    }

    private Widget getHintLabel() {
        boolean hasMin = false;
        boolean hasMax = false;
        if (getLongParam().getMinValue().longValue() != Long.MIN_VALUE)
            hasMin = true;
        if (getLongParam().getMaxValue().longValue() != Long.MAX_VALUE)
            hasMax = true;

        Label hint = new Label();
        hint.setStyleName("hint");
        if (hasMin && hasMax)
            hint.setText("[" + getLongParam().getMinValue() + ".." + getLongParam().getMaxValue() + "]");
        else if (hasMin && !hasMax)
            hint.setText(">=" + getLongParam().getMinValue());
        else if (!hasMin && hasMax)
            hint.setText("<=" + getLongParam().getMaxValue());
        else // no min or max
            hint.setText("Enter an integer");

        return hint;
    }

    public void setValue(long newVal) {
        try {
            getLongParam().setActualValue(new Long(newVal));
            setValueObject(getLongParam()); // updates task
            _textBox.setText(String.valueOf(getLongParam().getActualValue()));
        }
        catch (ParameterException e) {
            //TODO: put error next to widget on screen, or popup dialog
            _logger.debug("Error setting Long value " + newVal);
        }
    }

    public class LongIncrementCallback implements IncrementListener {
        public void onClick() {
            Long actualVal = new Long(_textBox.getText());
            _logger.debug("LongIncrementCallback: LongParameterVO text value  " + actualVal);

            // New value after the increment
            Long newVal = actualVal + 1;

            boolean valid = false;
            try {
                getLongParam().setActualValue(newVal);
                setValueObject(getLongParam()); // updates task
                valid = getLongParam().isValid();

                // If the current value is the maxValue, reset the actual value to the maxValue.
                // Above code would have incremented the actual value by 1
                if (newVal.longValue() == getLongParam().getMaxValue()) {
                    getLongParam().setActualValue(actualVal);
                    setValueObject(getLongParam()); // updates task
                }
            }
            catch (NumberFormatException e) {
                valid = false;
            }
            catch (ParameterException e) {
                valid = false;
            }

            // Do nothing if null
            if (null == actualVal) return;

            //if ((val.longValue()+1)<=getLongParam().getMaxValue().longValue()) {
            if (valid) {
                _logger.debug("LongIncrementCallback: LongParameterVO value  " + newVal + " is valid.");
                setValue(newVal);
                _textBox.setStyleName("textBox");
            }
            else {
                _logger.debug("LongIncrementCallback: LongParameterVO value  " + newVal + " is not valid.");
                setValue(newVal);
                _textBox.setStyleName("textBoxError");
                Window.alert("Valid value range is " + getLongParam().getMinValue() + " to " +
                        getLongParam().getMaxValue());
            }
        }
    }

    public class LongDecrementCallback implements IncrementListener {
        public void onClick() {
            Long actualVal = new Long(_textBox.getText());
            _logger.debug("LongDecrementCallback: LongParameterVO text value  " + actualVal);

            // New value after the decrement
            Long newVal = actualVal - 1;

            boolean valid = false;
            try {
                getLongParam().setActualValue(newVal);
                setValueObject(getLongParam()); // updates task
                valid = getLongParam().isValid();

                // If the current value is the minValue, reset the actual value to the minValue.
                // Above code would have decremented the actual value by 1
                if (newVal.longValue() == getLongParam().getMinValue()) {
                    getLongParam().setActualValue(actualVal);
                    setValueObject(getLongParam()); // updates task
                }
            }
            catch (NumberFormatException e) {
                valid = false;
            }
            catch (ParameterException e) {
                valid = false;
            }


            // Do nothing if null
            if (null == actualVal) return;

            //if ( (val.longValue()-1) <= getLongParam().getMaxValue().longValue()) {
            if (valid) {
                _logger.debug("LongDecrementCallback: LongParameterVO value  " + newVal + " is valid.");
                setValue(newVal);
                _textBox.setStyleName("textBox");
            }
            else {
                _logger.debug("LongDecrementCallback: LongParameterVO value  " + newVal + " is not valid.");
                _textBox.setStyleName("textBoxError");

                // Decrement the value if the new value is not the minumum value. This enables
                if (newVal != getLongParam().getMinValue()) {
                    setValue(newVal);
                }
                Window.alert("Valid value range is " + getLongParam().getMinValue() + " to " +
                        getLongParam().getMaxValue());
            }
        }
    }

    // TODO: move this into a ParameterValidator
    public class ValueChangedListener implements KeyboardListener {
        // todo this is a hack because the TextBox screams out a different char on key pressed than key up.
        // For example, pressing 1 on the Number Pad registers 1 for pressed but a on key up.
        char lastPressed = '@';

        public boolean isValidLongKey(char c) {
//            _logger.debug("Is Character: "+c);
            return Character.isDigit(c) || c == KEY_BACKSPACE || c == KEY_DELETE || c == KEY_TAB || c == '-';
        }

        /**
         * Can only cancel keys onKeyPress, but can only get mouse-based events onKeyUp
         */
        public void onKeyPress(Widget widget, char c, int i) {
//            _logger.debug("Key Pressed: "+c+", modifier: "+i);
            if (!isValidLongKey(c)) {
                lastPressed = '@';
                _textBox.cancelKey();
            }
            else {
                lastPressed = c;
            }
        }

        public void onKeyDown(Widget widget, char c, int i) {

            //_logger.debug("Key Down: "+c+", modifier: "+i);
            char targetChar = c;
            if (c != lastPressed) {
                targetChar = lastPressed;
            }
//            _logger.debug("TargetChar is "+targetChar);
            if (isValidLongKey(targetChar) && (targetChar != KEY_TAB)) {
                onChange(widget);
            }
            lastPressed = '@';


        }

        /**
         * Catch new chars. Non-digits are cancelled in onKeyPress, and test new value for validity
         */
        public void onKeyUp(Widget widget, char c, int i) {
//            _logger.debug("Key Up: "+c+", modifier: "+i);
            char targetChar = c;
            if (c != lastPressed) {
                targetChar = lastPressed;
            }
//            _logger.debug("TargetChar is "+targetChar);
            if (isValidLongKey(targetChar) && (targetChar != KEY_TAB)) {
                onChange(widget);
            }
            lastPressed = '@';
        }

        /**
         * When the user changes the value in the text box, try to create a Long and set the value.  If invalid,
         * change the background color to red
         */
        public void onChange(Widget widget) {
//            _logger.debug("On Change...");
            boolean valid = true;
            try {
                Long newVal = new Long(((TextBox) widget).getText());
                getLongParam().setActualValue(newVal);
                setValueObject(getLongParam()); // updates task
                valid = getLongParam().isValid();
            }
            catch (NumberFormatException e) {
                valid = false;
            }
            catch (ParameterException e) {
                valid = false;
            }

            if (valid)
                _textBox.setStyleName("textBox");
            _logger.debug("ValueChangedListener: onChange : LongParameterVO value  " + getLongParam().getActualValue() + " is valid.");
            if (!valid) {
                _logger.debug("ValueChangedListener: onChange: text is " + ((TextBox) widget).getText());
                String text = ((TextBox) widget).getText().trim();
                if (text.equals("-") || text.equals("")) {
                    // Do not pop up the error message.
                    _textBox.setStyleName("textBox");
                }
                else {
                    _logger.debug("ValueChangedListener: onChange : LongParameterVO value  " + getLongParam().getActualValue() + " is not valid.");
                    _textBox.setStyleName("textBoxError");
                    Window.alert("Valid value range is " + getLongParam().getMinValue() + " to " + getLongParam().getMaxValue());
                }

            }
        }
    }
}
