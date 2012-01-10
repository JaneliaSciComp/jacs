
package org.janelia.it.jacs.web.gwt.common.client.ui.window;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Michael Press
 */
public class OpenInSameWindowCommand implements Command, ClickListener {
    String _url;

    public OpenInSameWindowCommand(String url) {
        setUrl(url);
    }

    /**
     * From Command
     */
    public void execute() {
        open();
    }

    /**
     * From ClickListener
     */
    public void onClick(Widget sender) {
        open();
    }

    protected void open() {
        Window.open(_url, getWhichWindow(), "");
    }

    protected String getWhichWindow() {
        return "_self";
    }

    public void setUrl(String url) {
        _url = url;
    }

}