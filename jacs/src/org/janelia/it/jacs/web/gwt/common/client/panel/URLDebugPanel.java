
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.ui.Grid;
import org.gwtwidgets.client.util.Location;
import org.gwtwidgets.client.util.WindowUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.Map;

/**
 * @author Michael Press
 */
public class URLDebugPanel extends TitledPanel {
    Grid _grid = null;
    TitledPanel me = this;

    public URLDebugPanel(String title) {
        super(title);
    }

    protected void popuplateContentPanel() {
//        JavaScriptObject obj = jsTest();
//        System.out.println("obj=" + obj.toString());

        Location location = WindowUtils.getLocation();
        _grid = new Grid(0, 2); // rows to be added dynamically

        addGridRow(_grid, "URL", location.getHref());
        addGridRow(_grid, "Host", location.getHost());
        addGridRow(_grid, "Path", location.getPath());
        addGridRow(_grid, "QueryString", location.getQueryString());

        Map params = location.getParameterMap();
        for (Object o : params.keySet()) {
            String name = (String) o;
            addGridRow(_grid, name, (String) params.get(name));
        }

        add(_grid);
    }

//TODO: get css property
//    private native JavaScriptObject jsTest() /*-{
//        var obj = new Object(document.styleSheets[0]);
//        obj.setAttribute("param", "123");
//        return obj;
//       alert("document.styleSheets[0]="+ document.styleSheets[0]);
//       alert("document.styleSheets[0].cssRules[0].style.textDecoration = StrValue; ");
//       alert("document.styleSheets[0].cssRules[0].style.textDecoration = StrValue; ");
//       alert("document.styleSheets[0].cssRules[0].style.textDecoration = StrValue; ");
//    }-*/;

    private void addGridRow(Grid grid, String promptString, String valueString) {
        // Add a row to the grid
        int row = grid.getRowCount();
        grid.resizeRows(row + 1);

        grid.setWidget(row, 0, HtmlUtils.getHtml(promptString + ": ", "prompt"));
        grid.setWidget(row, 1, HtmlUtils.getHtml(valueString, "text"));
        grid.getCellFormatter().setStyleName(row, 0, "gridCell");
        grid.getCellFormatter().setStyleName(row, 1, "gridCell");
    }
}
