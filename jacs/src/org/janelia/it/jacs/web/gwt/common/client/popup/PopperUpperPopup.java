
package org.janelia.it.jacs.web.gwt.common.client.popup;

import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: May 23, 2007
 * Time: 4:33:04 PM
 */
public class PopperUpperPopup extends PopperUpperHTML {

    private BasePopupPanel popupPanel;

    public PopperUpperPopup(String text, String textStyleName, String hostTextStyleName, String hostHoverName, String hostClickName, boolean needHoverPopup, boolean needClickPopup, BasePopupPanel popupPanel) {
        super(text, textStyleName, DEFAULT_HOVER_STYLE, hostTextStyleName, hostHoverName, hostClickName, null, true, needHoverPopup, needClickPopup);
        this.popupPanel = popupPanel;
    }

    protected void configLauncher() {
        getLauncher().setPopup(popupPanel);
        getLauncher().setDelay(300);
    }

}
