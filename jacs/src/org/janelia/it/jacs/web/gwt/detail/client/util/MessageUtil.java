
package org.janelia.it.jacs.web.gwt.detail.client.util;

import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * This class is used to post messages to the title boxes on ReadDetail page
 *
 * @author Tareq Nabeel
 */
public class MessageUtil {

    /**
     * This method posts a "Not Found" message to a panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void addNotFoundErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.add(HtmlUtils.getHtml(entityName + " unavailable for " + searchString,
                "detailMessageError"));
    }

    /**
     * This method posts a message to a panel after clearing the panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void setNotFoundErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.clearContent();
        addNotFoundErrorMessage(panel, entityName, searchString);
    }

    /**
     * This method posts a data retrieval error message to a panel after clearing the panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void setServiceErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.clearContent();
        addServiceErrorMessage(panel, entityName, searchString);
    }

    /**
     * This method posts a data retrieval error message to a panel
     *
     * @param panel        The panel to post the message to
     * @param entityName   The entity name that the message is for e.g. "Sample"
     * @param searchString e.g. Read Id: 12122 or LibraryId: 324234
     */
    public static void addServiceErrorMessage(TitledBox panel, String entityName, String searchString) {
        panel.add(HtmlUtils.getHtml("Error retrieving " + entityName + " information for " + searchString,
                "detailMessageError"));
    }
}
