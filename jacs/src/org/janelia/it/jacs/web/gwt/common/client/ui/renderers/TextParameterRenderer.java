
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * @author Michael Press
 */
public class TextParameterRenderer extends ParameterRenderer {
    private TextBox _textBox;

    public TextParameterRenderer(TextParameterVO param, String key, Task task) {
        super(param, key, task);
        addKeyboardListener(new MyKeyboardListener());
    }

    protected TextParameterVO getTextParam() {
        return (TextParameterVO) getValueObject();
    }

    protected Widget createPanel() {
        _textBox = new TextBox();
        _textBox.setVisibleLength(10);
        _textBox.setText(getTextParam().getTextValue());

        return _textBox;
    }

    private class MyKeyboardListener implements KeyboardListener {
        public void onKeyDown(Widget sender, char keyCode, int modifiers) {
        }

        public void onKeyPress(Widget sender, char keyCode, int modifiers) {
        }

        public void onKeyUp(Widget sender, char keyCode, int modifiers) {
            try {
                String tmpValue = _textBox.getText();
                // todo Parameter validation probably needed here
                ((TextParameterVO) _param).setTextValue(tmpValue);
                setValueObject(_param);
            }
            catch (ParameterException e) {
                System.out.println("Error setting the text value.");
            }
        }
    }
}
