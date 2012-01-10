
package org.janelia.it.jacs.web.gwt.common.client.ui.window;

/**
 * @author Michael Press
 */
public class OpenInNewWindowCommand extends OpenInSameWindowCommand {
    public OpenInNewWindowCommand(String url) {
        super(url);
    }

    protected String getWhichWindow() {
        return "_other";
    }
}
