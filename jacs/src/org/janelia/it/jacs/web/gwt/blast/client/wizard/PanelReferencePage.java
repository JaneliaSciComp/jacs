
package org.janelia.it.jacs.web.gwt.blast.client.wizard;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

/**
 * Test page to display the different panel types.  Add to Blast to view
 */
public class PanelReferencePage extends BlastWizardPage {
    private VerticalPanel _mainPanel;
    private static final String HISTORY_TOKEN = "PanelReference";

    public PanelReferencePage(BlastData blastData, WizardController controller) {
        super(blastData, controller, /*buttons*/ true);
        init();
    }

    private void init() {
        _mainPanel = new VerticalPanel();
        _mainPanel.setWidth("100%");

        TitledPanel titledPanel = new TitledPanel("TitledPanel");
        titledPanel.setWidth("auto");
        titledPanel.add(new HTML("This is a Titled Panel with a \"hide\" link and no explicity width setting."));
        _mainPanel.add(titledPanel);

        _mainPanel.add(getSpacer());

        TitledPanel wideTitledPanel = new TitledPanel("TitledPanel", false);
        wideTitledPanel.setWidth("100%");
        wideTitledPanel.add(new HTML("This is a Titled Panel with no hide link and width explicitly set to 100%"));
        _mainPanel.add(wideTitledPanel);

        //wideTitledPanel.add(getSpacer());
        wideTitledPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        TitledBox titledBox = new TitledBox("Titled Box");
        titledBox.add(new HTML("This is the contents of a TitledBox with no explicit width setting."));
        wideTitledPanel.add(titledBox);

        titledBox.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        VerticalPanel inside = new VerticalPanel();
        inside.setStyleName("plainRoundedPanel");
        inside.setWidth("100%");
        inside.add(new HTML("This is a RoundedPanel2"));
        RoundedPanel2 roundedPanel = new RoundedPanel2(inside, RoundedPanel2.ALL);
        roundedPanel.setCornerStyleName("plainRoundedPanelRounding");
        titledBox.add(roundedPanel);

        titledBox.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        VerticalPanel inside2 = new VerticalPanel();
        inside2.add(new HTML("This is a RoundedPanel2 with a #AAAAAA border"));
        inside2.setWidth("100%");
        inside2.setStyleName("plainRoundedPanelWithBorder");
        RoundedPanel2 roundedPanel2 = new RoundedPanel2(inside2, RoundedPanel2.ALL, "#AAAAAA");
        roundedPanel2.setCornerStyleName("plainRoundedPanelWithBorderRounding");
        titledBox.add(roundedPanel2);


        //titledBox.add(getSpacer());
        //
        //DownloadBox downloadBox = new DownloadBox(getDownloadFiles());
        //downloadBox.add(new HTML("This is a DownloadBox.  Contents TBD"));
        //titledBox.add(downloadBox);

        //wideTitledPanel.add(getSpacer());
        wideTitledPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        TitledBox titledBox2 = new TitledBox("Titled Box", false);
        titledBox2.add(new HTML("This is a TitledBox with no width set and no action links"));
        wideTitledPanel.add(titledBox2);

        titledBox2.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        SecondaryTitledBox secondaryBox = new SecondaryTitledBox("Secondary Titled Box", false);
        secondaryBox.add(new HTML("This is a SecondaryTitledBox with no width set"));
        titledBox2.add(secondaryBox);

        wideTitledPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        TitledBox hiddenTitledBox = new TitledBox("Titled Box", true, false);
        hiddenTitledBox.add(new HTML("This is a TitledBox whose contents are initially hidden"));
        wideTitledPanel.add(hiddenTitledBox);

        wideTitledPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        TitledBox wideTitledBox = new TitledBox("Titled Box");
        wideTitledBox.setWidth("100%");
        wideTitledBox.add(new HTML("This is the contents of another TitledBox with width set to 100%."));
        wideTitledPanel.add(wideTitledBox);

        getButtonManager().setBackButtonEnabled(false); // permanent
        getButtonManager().setNextButtonEnabled(false); // until valid selection is made
    }

    private Widget getSpacer() {
        //HTML spacer= HtmlUtils.getHtml("&nbsp;", "spacer");
        //HTML spacer= HtmlUtils.getHtml("--------This is a 10px spacer--------", "spacer");
        //DOM.setStyleAttribute(spacer.getElement(), "color", "#DDDDDD");
        return HtmlUtils.getHtml("&nbsp;", "spacer");
    }

    public Widget getMainPanel() {
        return _mainPanel;
    }

    public String getPageTitle() {
        return "Panel Reference Page";
    }

    public String getPageToken() {
        return HISTORY_TOKEN;
    }

    public void setupButtons() {
        super.setupButtons();
        getButtonManager().setBackButtonEnabled(false);
    }
}
