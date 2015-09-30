
package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SearchPanel extends Composite {
    Panel _canvas;
    RoundedPanel2 _canvasWrapper;

    public static final String SEARCH_STYLE = "SearchPanel";
    public static final String ROUNDING_STYLE = "SearchRounding";
    public static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    public SearchPanel() {
        super();
        _canvas = new HorizontalPanel();
        _canvas.setStyleName(SEARCH_STYLE);
        _canvasWrapper = new RoundedPanel2(_canvas, RoundedPanel2.ALL, BORDER_COLOR);
        _canvasWrapper.setCornerStyleName(ROUNDING_STYLE);
        initWidget(_canvasWrapper);
    }

    public void add(Widget w) {
        _canvas.add(w);
    }

}

