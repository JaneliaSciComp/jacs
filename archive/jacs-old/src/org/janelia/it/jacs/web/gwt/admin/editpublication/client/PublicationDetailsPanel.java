
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.admin.editproject.client.RequiredPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;

public class PublicationDetailsPanel extends VerticalPanel {

    VerticalPanel detailsInnerPanel = null;

    protected TextBox titleBox = null;
    protected TextBox accessionBox = null;
    protected TextBox urlBox = null;
    protected TextBox dateBox = null;
    protected ListBox projectSelector = null;

    protected TextBox journalNameBox = null;
    protected TextBox journalVolumeBox = null;
    protected TextBox journalIssueBox = null;
    protected TextBox lowerRange = null;
    protected TextBox upperRange = null;

    protected TextBox authorTextBoxValue = null;
    protected AuthorTable authorTable = null;


    public PublicationDetailsPanel(PublicationTabPanel parent) {

        super();

        createDetailsPanel();

        parent.add(this, "Publication Details");

        setSampleNewPublication();

    }


    protected void createDetailsPanel() {

        detailsInnerPanel = new VerticalPanel();

        createInputFields();
        detailsInnerPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        createJournalBox();
        detailsInnerPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        createAuthorsBox();

        detailsInnerPanel.setWidth("100%");

        HorizontalPanel detailsOutterPanel = new HorizontalPanel();

        detailsOutterPanel.add(detailsInnerPanel);
        detailsOutterPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        detailsOutterPanel.add(new RequiredPanel());
        detailsOutterPanel.setWidth("100%");
        //detailsOutterPanel.setBorderWidth(2);

        this.add(detailsOutterPanel);

    }


    protected void createInputFields() {

        FlexTable detailsInputFields = new FlexTable();
        //detailsInputFields.setBorderWidth(1);
        detailsInputFields.setWidth("700");

        // PUBLICATION TITLE //
        titleBox = new TextBox();
        titleBox.setStyleName("EPFullTextBox");
        addWidgetWidgetPair(detailsInputFields, 0, 0,
                new HTMLPanel("<span class='prompt'>Publication Title:</span>&nbsp;<span class='requiredInformation'>*</span>"),
                titleBox);

        // PUBLICATION ACCESSION //
        accessionBox = new TextBox();
        accessionBox.setStyleName("EPsymbolTextBox");
        DockPanel accessPanel = new DockPanel();
        accessPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        Label accessLabel = new Label("CAM_PUB_");
        accessLabel.setStyleName("EPSymbolLabel");
        accessPanel.add(accessLabel, DockPanel.WEST);
        accessPanel.add(accessionBox, DockPanel.EAST);
        addWidgetWidgetPair(detailsInputFields, 1, 0,
                new HTMLPanel("<span class='prompt'>Publication Accession:</span>&nbsp;<span class='requiredInformation'>*</span>"),
                accessPanel);

        // PUBLICATION URL //
        urlBox = new TextBox();
        urlBox.setStyleName("EPURLTextBox");
        urlBox.setWidth("247");
        HorizontalPanel urlPanel = new HorizontalPanel();
        urlPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        Label webLabel = new Label("http://");
        webLabel.setStyleName("EPWebLabel");
        urlPanel.add(webLabel);
        urlPanel.add(urlBox);
        //urlPanel.setStyleName("EPFullTextBox");
        urlPanel.setWidth("95%");
        addPromptWidgetPair(detailsInputFields, 2, 0, "Publication URL", urlPanel);

        // PUBLICATION DATE //
        dateBox = new TextBox();
        dateBox.setStyleName("EPDateBox");
        HorizontalPanel datePanel = new HorizontalPanel();
        datePanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        datePanel.add(dateBox);
        datePanel.add(new HTMLPanel("<span class='commaList'>&nbsp;&nbsp;mm/dd/yyyy</span>"));
        addPromptWidgetPair(detailsInputFields, 3, 0, "Publication Date", datePanel);


        // PUBLICATION PROJECT //
        projectSelector = new ListBox();
        projectSelector.setStyleName("EPTextBox");
        addPromptWidgetPair(detailsInputFields, 4, 0, "Project", projectSelector);

        detailsInnerPanel.add(detailsInputFields);

    }


    protected void createJournalBox() {

        SecondaryTitledBox journalBox = new SecondaryTitledBox("Journal Entry", true);

        journalBox.setWidth("250");

        FlexTable journalInfo = new FlexTable();

        // journal name //
        journalNameBox = new TextBox();
        journalNameBox.setStyleName("EPJournalTextBox");
        addPromptWidgetPair(journalInfo, 0, 0, "Journal Name", journalNameBox);

        // journal volume //
        journalVolumeBox = new TextBox();
        journalVolumeBox.setStyleName("EPJournalNumber");
        addPromptWidgetPair(journalInfo, 1, 0, "Volume", journalVolumeBox);

        // journal issue //
        journalIssueBox = new TextBox();
        journalIssueBox.setStyleName("EPJournalNumber");
        addPromptWidgetPair(journalInfo, 2, 0, "Issue", journalIssueBox);

        // journal pages //
        HorizontalPanel pageRangePanel = new HorizontalPanel();
        lowerRange = new TextBox();
        lowerRange.setStyleName("EPJournalNumber");
        upperRange = new TextBox();
        upperRange.setStyleName("EPJournalNumber");
        pageRangePanel.add(lowerRange);
        pageRangePanel.add(new Label("   -    "));
        pageRangePanel.add(upperRange);
        addPromptWidgetPair(journalInfo, 3, 0, "Pages(s)", pageRangePanel);

        journalBox.add(journalInfo);

        detailsInnerPanel.add(journalBox);

    }


    protected void createAuthorsBox() {

        SecondaryTitledBox authorBox = new SecondaryTitledBox(
                "Authors", true);
        authorBox.setWidth("550");

        // author input panel //
        HTMLPanel authorName = new HTMLPanel("<span class='prompt'>Author Name:</span>&nbsp;" +
                "<span class='requiredInformation'>*</span>&nbsp;&nbsp;");
        authorTextBoxValue = new TextBox();
        authorTextBoxValue.setStyleName("EPAuthorTextBox");
        HorizontalPanel authorInputPanel = new HorizontalPanel();
        authorInputPanel.add(authorName);
        authorInputPanel.add(authorTextBoxValue);

        // author table //
        authorTable = new AuthorTable();

        // author order panel //
        VerticalPanel authorOrderPanel = new VerticalPanel();
        RoundedButton upButton = new RoundedButton("Move Up", new UpButtonListener());
        RoundedButton downButton = new RoundedButton("Move Down", new DownButtonListener());
        authorOrderPanel.add(upButton);
        authorOrderPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        authorOrderPanel.add(downButton);

        // put it all together //
        FlexTable grid = new FlexTable();
        //grid.setBorderWidth(2);
        grid.setWidth("100%");

        addWidgetWidgetPair(grid, 0, 0, authorInputPanel, createAddButton());
        addWidgetWidgetPair(grid, 1, 0, authorTable, authorOrderPanel);

        grid.setCellPadding(7);
        grid.setCellSpacing(2);

        authorBox.add(grid);

        detailsInnerPanel.add(authorBox);
    }


    protected class UpButtonListener implements ClickListener {

        public void onClick(Widget widget) {

            authorTable.upButtonPressed();

        }
    }

    protected class DownButtonListener implements ClickListener {

        public void onClick(Widget widget) {
            authorTable.downButtonPressed();
        }
    }

    protected RoundedButton createAddButton() {
        return new RoundedButton("Add", new ClickListener() {
            public void onClick(Widget sender) {

                if (authorTextBoxValue != null && !authorTextBoxValue.getText().equals("")) {

                    authorTable.addNewAuthor(authorTextBoxValue.getText());

                    authorTextBoxValue.setText("");

                }
            }
        });
    }


    public String getTitle() {
        return titleBox.getText();
    }

    // sample
    public void setSampleNewPublication() {

        System.out.println("setting sample new publication");

        // primary information
        titleBox.setText("Whale Scat Genome");
        accessionBox.setText("WhaleScat");
        urlBox.setText("biology.plosjournals.org/189034823/whalescat.pdf");
        dateBox.setText("05/12/2005");
        projectSelector.addItem("None");

        // journal information
        journalNameBox.setText("PLoS");
        journalVolumeBox.setText("54");
        journalIssueBox.setText("3");
        lowerRange.setText("125");
        upperRange.setText("127");

        // set sample authors
        authorTable.setSampleAuthors();
    }


    ///////////////////////////////////////////////////////////////////////////////////////

    public static void addWidgetWidgetPair(HTMLTable table, int row, int col, Widget widget1, Widget widget2) {

        table.setWidget(row, col, widget1);
        table.setWidget(row, col + 1, widget2);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }


    public static void addPromptWidgetPair(HTMLTable table, int row, int col, String prompt, Widget tableWidget) {
        TableUtils.addHTMLRow(table, new RowIndex(row), col, prompt, tableWidget);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }


}
