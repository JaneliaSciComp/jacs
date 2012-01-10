
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
abstract public class IncrementableParameterRenderer extends ParameterRenderer {
    protected IncrementableParameterRenderer(ParameterVO param, String key, Task task) {
        super(param, key, task);
    }

    protected Panel getIncrementButtons(IncrementListener incrCallback, IncrementListener decrCallback) {
        VerticalPanel buttons = new VerticalPanel();

        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();

        Image up = imageBundle.getArrowUpButtonImage().createImage();
        up.addMouseListener(new HoverImageSetter(up, imageBundle.getArrowUpButtonImage(), imageBundle.getArrowUpButtonHoverImage(),
                new IncrementClickListenerConverterListener(incrCallback)));

        Image down = imageBundle.getArrowDownButtonImage().createImage();
        down.addMouseListener(new HoverImageSetter(down, imageBundle.getArrowDownButtonImage(), imageBundle.getArrowDownButtonHoverImage(),
                new IncrementClickListenerConverterListener(decrCallback)));

        buttons.add(up);
        buttons.add(down);

        return buttons;
    }

    // Converts a ClickListener's onClick(Widget) callback to an IncrementListener's onClick() callback
    public class IncrementClickListenerConverterListener implements ClickListener {
        private IncrementListener _incrListener;

        public IncrementClickListenerConverterListener(IncrementListener incrListener) {
            _incrListener = incrListener;
        }

        public void onClick(Widget widget) {
            if (_incrListener != null)
                _incrListener.onClick();
        }
    }

}
