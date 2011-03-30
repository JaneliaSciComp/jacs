/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.DoubleParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * @author Michael Press
 */
public class DoubleParameterRenderer extends IncrementableParameterRenderer {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.renderers.DoubleParameterRenderer");

    TextBox _textBox;

    public DoubleParameterRenderer(DoubleParameterVO param, String key, Task task) {
        super(param, key, task);
    }

    protected DoubleParameterVO getDoubleParam() {
        return (DoubleParameterVO) getValueObject();
    }

    protected Widget createPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        // Create the text entry area
        _textBox = new TextBox();
        //TODO: dynamically expand visible length if value exceeds 4 digits
        _textBox.setVisibleLength(2);
        _textBox.setTextAlignment(TextBoxBase.ALIGN_RIGHT);
        _textBox.addKeyboardListener(new ValueChangedListener());

        // Force the value to contain a decimal
        _textBox.setText(getNormalizedDoubleString(getDoubleParam()));
        panel.add(_textBox);

        panel.add(getIncrementButtons(new DoubleIncrementCallback(), new DoubleDecrementCallback()));
        _textBox.setStyleName("textBox");

        // Create the hint label
        //panel.add(getHintLabel());
        return panel;
    }

    /**
     * Hack to append ".0" to any float that doesn't have one after being converted to a String
     */
    protected String getNormalizedDoubleString(DoubleParameterVO param) {
        String val = param.getActualValue().toString();
        if (!(val.indexOf('.') >= 0))
            val += ".0";
        return val;
    }

    private Widget getHintLabel() {
        boolean hasMin = false;
        boolean hasMax = false;
        if (getDoubleParam().getMinValue().doubleValue() != Double.MIN_VALUE)
            hasMin = true;
        if (getDoubleParam().getMaxValue().doubleValue() != Double.MAX_VALUE)
            hasMax = true;

        Label hint = new Label();
        hint.setStyleName("hint");
        if (hasMin && hasMax)
            hint.setText("[" + getDoubleParam().getMinValue() + ".." + getDoubleParam().getMaxValue() + "]");
        else if (hasMin && !hasMax)
            hint.setText(">=" + getDoubleParam().getMinValue());
        else if (!hasMin && hasMax)
            hint.setText("<=" + getDoubleParam().getMaxValue());
        else // no min or max
            hint.setText("Enter a floating point number");

        return hint;
    }

    public void setValue(double newVal) {
        try {
            getDoubleParam().setActualValue(new Double(newVal));
            _textBox.setText(getNormalizedDoubleString(getDoubleParam()));
            setValueObject(getDoubleParam()); // updates task
        }
        catch (ParameterException e) {
            //TODO: put error next to widget on screen, or popup dialog
            _logger.debug("Error setting Double value " + newVal);
        }
    }

    public class DoubleIncrementCallback implements IncrementListener {
        public void onClick() {
            Double val = getDoubleParam().getActualValue();
            if (val != null)
                setValue(val.doubleValue() + 1.0);
        }
    }

    public class DoubleDecrementCallback implements IncrementListener {
        public void onClick() {
            Double val = getDoubleParam().getActualValue();
            if (val != null)
                setValue(val.doubleValue() - 1.0);
        }
    }

    public class ValueChangedListener implements KeyboardListener {
        // todo this is a hack because the TextBox screams out a different char on key pressed than key up.
        // For example, pressing 1 on the Number Pad registers 1 for pressed but a on key up.
        char lastPressed = '@';

        public boolean isValidDoubleKey(char c) {
//            _logger.debug("Is Character: "+c);
            return Character.isDigit(c) || c == KEY_BACKSPACE || c == KEY_DELETE || c == KEY_TAB || c == '.' || c == '-';
        }

        /**
         * Can only cancel keys onKeyPress, but can only get mouse-based events onKeyUp
         */
        public void onKeyPress(Widget widget, char c, int i) {
//            _logger.debug("Key Pressed: "+c+", modifier: "+i);
            if (!isValidDoubleKey(c)) {
                lastPressed = '@';
                _textBox.cancelKey();
            }
            else {
                lastPressed = c;
            }
        }

        public void onKeyDown(Widget widget, char c, int i) {
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
            if (isValidDoubleKey(targetChar) && (targetChar != KEY_TAB)) {
                onChange(widget);
            }
            lastPressed = '@';
        }

        /**
         * When the user changes the value in the text box, try to create a Double and set the value.  If invalid,
         * change the background color to red
         */
        public void onChange(Widget widget) {
//            _logger.debug("On Change...");
            boolean valid = true;
            try {
                Double newVal = new Double(((TextBox) widget).getText());
                getDoubleParam().setActualValue(newVal);
                setValueObject(getDoubleParam()); // updates task
                valid = getDoubleParam().isValid();
                _logger.debug("DoubleParameterVO value  " + getDoubleParam().getActualValue() + " is valid.");
            }
            catch (NumberFormatException e) {
                _logger.debug("NumberFormatException for value " + getDoubleParam().getActualValue() + ": " + e.getMessage());
                valid = false;
            }
            catch (ParameterException e) {
                _logger.debug("ParameterException for value " + getDoubleParam().getActualValue() + ": " + e.getMessage());
                valid = false;
            }
            catch (Throwable e) {
                _logger.debug("Caught exception onChange of DoubleParameterRenderer: " + e.getMessage());
                valid = false;
            }

            // Update the style
            if (valid)
                _textBox.setStyleName("textBox");
            if (!valid) {
                _logger.debug("DoubleParameterVO value  " + getDoubleParam().getActualValue() + " is not valid.");
                _textBox.setStyleName("textBoxError");
            }
        }
    }
}
