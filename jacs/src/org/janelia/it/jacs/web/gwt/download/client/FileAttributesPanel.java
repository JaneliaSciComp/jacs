
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNodeImpl;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 13, 2006
 * Time: 10:27:29 AM
 */
public class FileAttributesPanel extends VerticalPanel {
    private final String DEFAULT_ATTRIBUTE_STYLE = "DownloadAttribute";
    private final String DESCRIPTIVE_TEXT_NAME = "Description";
    private final String DESCRIPTIVE_TEXT_ZIP = "Download file in .zip format (Windows)";
    private final String DESCRIPTIVE_TEXT_GZ = "Download file in .zip format (Windows)";
    private final Map _attributeNameVsStyle = new HashMap();

    /**
     * Constructor takes data file which is source of all information shown.
     *
     * @param dataFile what to display
     */
    public FileAttributesPanel(DownloadableDataNode dataFile) {
        _attributeNameVsStyle.put("Status", "DownloadAttribute-Status");

        createUI(dataFile);
    }

    /**
     * Here is where the user interface is setup.
     *
     * @param dataFile where to get the information to show.
     */
    private void createUI(DownloadableDataNode dataFile) {
        DataFileFormatter formatter = new DataFileFormatter();

        Panel filePanel = formatter.getFile(dataFile);
        add(filePanel);

        String[] attributeNames = dataFile.getAttributeNames();
        for (int i = 0; i < attributeNames.length; i++) {
            String nextAttribName = attributeNames[i];
            Panel attribPanel = formatter.getAttribute(dataFile, nextAttribName);
            String attributeStyle = (String) _attributeNameVsStyle.get(nextAttribName);
            if (attributeStyle == null)
                attribPanel.setStyleName(DEFAULT_ATTRIBUTE_STYLE);
            else
                attribPanel.setStyleName(attributeStyle);
            add(attribPanel);
        }

        DownloadPanel dnldWidget = new DownloadPanel(null, repackageDataFile(dataFile));
        dnldWidget.setStyleName("DownloadContentWidget");
        RoundedPanel2 container = new RoundedPanel2(dnldWidget, RoundedPanel2.ALL, "#FFFFFF");
        container.setCornerStyleName("DownloadContentWidgetCorner");

        add(container);

    }

    /**
     * It is assumed that every data file accessible, has two versions, available for download:
     * both a .gz and a .zip archive.  Further, it is assumed that the location given for a data
     * file is minus this suffix of either .gz or .zip.
     *
     * @param dataFile
     * @return list of archives assumed to represent this file, and with standard descriptions.
     */
    private List repackageDataFile(DownloadableDataNode dataFile) {
        List archivesList = new ArrayList();
        String[] attributeNames = new String[]{DESCRIPTIVE_TEXT_NAME};
        String[] attributeValues = new String[]{DESCRIPTIVE_TEXT_ZIP};
        DownloadableDataNode zipArchiveFile = new DownloadableDataNodeImpl(
                null, dataFile.getText(), attributeNames, attributeValues, dataFile.getLocation() + ".zip", 0L
        );
        attributeNames = new String[]{DESCRIPTIVE_TEXT_NAME};
        attributeValues = new String[]{DESCRIPTIVE_TEXT_GZ};
        String gzExtension = dataFile.isMultifileArchive() ? ".tar.gz" : ".gz";
        DownloadableDataNode gzArchiveFile = new DownloadableDataNodeImpl(
                null, dataFile.getText(), attributeNames, attributeValues, dataFile.getLocation() + gzExtension, 0L
        );
        archivesList.add(zipArchiveFile);
        archivesList.add(gzArchiveFile);
        return archivesList;
    }

}
