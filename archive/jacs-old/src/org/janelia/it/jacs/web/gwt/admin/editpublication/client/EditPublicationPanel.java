
package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Guy Cao
 */
public class EditPublicationPanel extends TitledBox {

    // Connect to Database //
    private static DownloadMetaDataServiceAsync downloadService =
            (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }

    // data structure
    protected Map<String, PublicationImpl> _accessionToPublicationMap = null;
    protected ArrayList<PublicationImpl> _publicationList = null;


    // GUI components //
    private PublicationTabPanel tabs = null;

    private ListBox existingPubSelector = null;
    private static final String ExistingPubSelectorPrompt = "-- Please select a publication --";

    public EditPublicationPanel() {
        super("Edit Publication");
    }

    protected void popuplateContentPanel() {

//        downloadService.getAccessionToPublicationMapping(new PopulatePublicationsCallback(this, /*create GUI*/ true));

        createPublicationSelection();

        // add Project names to List BOX
        populatePublicationNames();

        // add publication information (tabs)
        tabs = new PublicationTabPanel();
        add(tabs);

        // saving publications
        createSaveButtonPanel();

    }

    protected class PopulatePublicationsCallback implements AsyncCallback {

        LoadingLabel _loadingLabel = null;
        EditPublicationPanel parentPanel = null;

        // flag to only reload publications (as opposed to create new tabs and save buttons again)
        // added after calling PopulatePublicationsCallback everytime after hitting save button
        boolean createAndLoadGUI = false;


        public PopulatePublicationsCallback(EditPublicationPanel parentPanel, boolean createAndLoadGUI) {

            this.parentPanel = parentPanel;
            this.createAndLoadGUI = createAndLoadGUI;

            if (createAndLoadGUI) {
                _loadingLabel = new LoadingLabel("Loading publications...", true);
            }
            else {
                _loadingLabel = new LoadingLabel("Refreshing publications...", true);
            }

            this.parentPanel.add(_loadingLabel);
        }


        // TODO: notify user
        public void onFailure(Throwable throwable) {
            System.out.println("---EditPublicationPanel: Failed to retrieve publications: " + throwable.getMessage());
            System.out.println("---stack trace: " + throwable.getStackTrace().toString());
            System.out.println("---throwable: " + throwable);
            _loadingLabel.setText("Failed to retrieve publications");
        }

        public void onSuccess(Object result) {
            System.out.println("Publications received successfully");

            // invisify "loading projects..."
            _loadingLabel.setVisible(false);
            this.parentPanel.remove(_loadingLabel);

            _accessionToPublicationMap = (Map) result;
            _publicationList = new ArrayList<PublicationImpl>(_accessionToPublicationMap.values());

            // create name-to-symbol mapping
            //createNameSymbolMap();

            if (createAndLoadGUI) {
                // create publications selector
                createPublicationSelection();
            }

            // add Project names to List BOX
            populatePublicationNames();

            // populate tabs panel and create save button
            if (createAndLoadGUI) { // only do this once at the beginning of each session

                // add publication information (tabs)
                tabs = new PublicationTabPanel();
                add(tabs);

                // saving publications
                createSaveButtonPanel();

            }

        }
    }

    protected void populatePublicationNames() {

        if (_publicationList != null && _publicationList.size() != 0) {

            for (PublicationImpl currPublication : _publicationList) {
                existingPubSelector.addItem(currPublication.getTitle());
            }

            existingPubSelector.setEnabled(true);
        }
        else {
            existingPubSelector.setEnabled(false);
        }
    }

    protected void createPublicationSelection() {

        // Publication Selection
        RadioButton existingPubButton = new RadioButton("publicationGroup", "Edit Existing Publication");
        RadioButton newPubButton = new RadioButton("publicationGroup", "Create New Publication");
        existingPubButton.setValue(true);

        existingPubSelector = new ListBox();
        existingPubSelector.addItem(ExistingPubSelectorPrompt);
        existingPubSelector.setStyleName("EPTextBox");

        // add listeners
        //existingPubButton.addClickListener(new ExistingPubListener());
        newPubButton.addClickListener(new NewPubHandler());
        //existingPubSelector.addChangeListener(new PublicationelectionChangeListener());

        // Attach to GUI
        FlexTable pubSelectorGrid = new FlexTable();
        addWidgetWidgetPair(pubSelectorGrid, 0, 0,
                existingPubButton, existingPubSelector);
        addWidgetWidgetPair(pubSelectorGrid, 1, 0,
                newPubButton, null);


        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(pubSelectorGrid);
        add(HtmlUtils.getHtml("&nbsp;", "spacer"));

    }

    protected void createSaveButtonPanel() {

        RoundedButton saveButton = new RoundedButton("Save", new ClickListener() {
            public void onClick(Widget sender) {
                savePublication();
            }
        });

        saveButton.setEnabled(false);
        saveButton.setStyleName("EPSaveButton");

        HorizontalPanel savePanel = new HorizontalPanel();

        savePanel.setStyleName("EPFullPanel");
        savePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        savePanel.add(saveButton);

        this.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        this.add(savePanel);
    }


    private class NewPubHandler implements ClickListener {

        public void onClick(Widget widget) {

            System.out.println(" new publication clicked");

            if (existingPubSelector == null) {
                System.out.println(" exsiting publication selector is NULL");
            }
            else {
                System.out.println(" exsiting publication is NOT NULL");
                existingPubSelector.setSelectedIndex(0);
                existingPubSelector.setEnabled(false);
                System.out.println(" exsiting publication selector DISABLED");
            }

            tabs.getDetailPanel().setSampleNewPublication();
            tabs.getDescriptionPanel().setSampleNewPublication();
            tabs.getPreviewPanel().updatePublicationView();
        }

    }

    protected void savePublication() {

        System.out.println("   Trying to save publication!!!!");


    }

    public static void addWidgetWidgetPair(HTMLTable table, int row, int col, Widget widget1, Widget widget2) {

        table.setWidget(row, col, widget1);
        table.setWidget(row, col + 1, widget2);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }


}
