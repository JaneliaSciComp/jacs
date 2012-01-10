
package org.janelia.it.jacs.web.gwt.detail.client.bse.metadata;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedTabPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.sample.SampleReadsTableBuilder;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;

/**
 * This class is responsible for managing the Sample Detail Panel
 *
 * @author Cristian Goina
 */
public class SampleDetailPanel extends TitledBox {
    private SiteManager _siteManager;
    private SampleReadsTableBuilder readsTableBuilder;

    private static DownloadMetaDataServiceAsync downloadService =
            (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    public SampleDetailPanel(String title,
                             String sampleAcc,
                             ActionLink actionLink,
                             boolean showActionLinks) {
        super(title,
                false, /* showActionLinks */
                true /* showContent */);
        readsTableBuilder = new SampleReadsTableBuilder();
        init(sampleAcc, actionLink, showActionLinks);
    }

    public void setEntityListener(EntityListener entityListener) {
        readsTableBuilder.setEntityListener(entityListener);
    }

    private void init(String sampleAcc, ActionLink actionLink, boolean showActionLinks) {
        if (showActionLinks) {
            setShowActionLinks(true);
            showActionLinks();
            if (actionLink != null) {
                addActionLink(actionLink);
            }
        }
        createPanelWidgets();
        populatePanelWithData(sampleAcc);
    }

    private void createPanelWidgets() {
        _siteManager = new SiteManager();
        Panel sampleDataAndMapPanel = _siteManager.createFullSampleDataPanel();
        Panel readDataPanel = readsTableBuilder.createReadDataPanel(10, new String[]{"10", "20", "50"});

        RoundedTabPanel tabPanel = new RoundedTabPanel();
        tabPanel.add(sampleDataAndMapPanel, "Sample data");
        tabPanel.add(readDataPanel, "Sample reads");
        tabPanel.selectTab(0);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(tabPanel);
    }

    private void populatePanelWithData(String sampleAcc) {
        _siteManager.retrieveAndDisplayDataBySampleAcc(sampleAcc);
        readsTableBuilder.populateSampleReads(sampleAcc);
    }

}
