
package org.janelia.it.jacs.web.gwt.detail.client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.MapPanel;
import org.janelia.it.jacs.web.gwt.detail.client.bse.metadata.SiteManager;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 29, 2007
 * Time: 4:24:18 PM
 */
public class SampleAndSiteInfoPopup extends BasePopupPanel {
    private String sampleId;

    public SampleAndSiteInfoPopup(String sampleId) {
        super("Sample Details", /*realizeNow*/ false, /*autohide*/ false, /*modal*/ false);
        this.sampleId = sampleId;
    }

    protected void populateContent() {
        // Build the content - sample and site MD vertically on left, map on right
        SiteManager siteManager = new SiteManager();

        VerticalPanel col1 = new VerticalPanel();
        col1.add(siteManager.getSampleDataPanel().getSampleMetaDataTitleBox());
        col1.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        col1.add(siteManager.getSiteDataPanel().getSiteMetaDataTitleBox());

        // Override read detail page default sizes for the map box
        VerticalPanel col2 = new VerticalPanel();

        MapPanel mapPanel = siteManager.getMapPanel();
        mapPanel.setGoogleMapHeight(330);
        mapPanel.setGoogleMapWidth(330);
        mapPanel.setGoogleMapStyleName("frvLegendSampleDetailMapBox");
        mapPanel.getGoogleMapBox().setTitle("Sample Geography");

        col2.add(siteManager.getSampleDataDownloadsPanel());
        col2.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        col2.add(mapPanel.getGoogleMapBox());

        HorizontalPanel row = new HorizontalPanel();
        row.add(col1);
        row.add(col2);

        add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        add(row);

        // Now populate the data
        siteManager.retrieveAndDisplayDataBySampleName(sampleId);
    }

    protected ButtonSet createButtons() {
        RoundedButton closeButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(new RoundedButton[]{closeButton});
    }
}
