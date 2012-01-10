
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DownloadLinkBuilder;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 8, 2006
 * Time: 1:33:45 PM
 * <p/>
 * Fixed panel (as opposed to a dialog) to allow users to download files.
 */
public class DownloadPanel extends VerticalPanel {

    VerticalPanel contentPanel;

    /**
     * Create a panel for downloading all relevant files.
     *
     * @param subjectDocument      PDF based on data.
     * @param rolledUpDataArchives data cited in the PDF.
     */
    public DownloadPanel(DownloadableDataNode subjectDocument, List rolledUpDataArchives) {

        DataFileFormatter formatter = new DataFileFormatter();

        //TODO consider removing these in favor of a single HTMLpanel with description and links.
        if (subjectDocument != null) {
            String descriptiveText = formatter.getDescriptiveText(subjectDocument);
            HTMLPanel pdfLinkPanel =
                    new HTMLPanel(
                            "<a href='" +
                                    subjectDocument.getLocation() +
                                    "'>" + descriptiveText + "</a>");

            pdfLinkPanel.setStyleName("DownloadDownloadWidgetText");
            add(pdfLinkPanel);
        }

        for (int i = 0; rolledUpDataArchives != null && i < rolledUpDataArchives.size(); i++) {
            DownloadableDataNode dataFile = (DownloadableDataNode) rolledUpDataArchives.get(i);
            String descriptiveText = formatter.getDescriptiveText(dataFile);
            HTMLPanel linkPanel =
                    new HTMLPanel(
                            "<a href='" +
                                    DownloadLinkBuilder.getDownloadLink(dataFile.getLocation()) +
                                    "'>" + descriptiveText + "</a>");

            linkPanel.setStyleName("DownloadDownloadWidgetText");
            add(linkPanel);
        }

    }

}
