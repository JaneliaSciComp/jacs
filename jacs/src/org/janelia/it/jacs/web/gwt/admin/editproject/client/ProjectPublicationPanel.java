
package org.janelia.it.jacs.web.gwt.admin.editproject.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl;

import java.util.*;


public class ProjectPublicationPanel extends VerticalPanel {

    // Publication data
    protected Map<String, PublicationImpl> _accessionToPublicationMap;
    protected ArrayList<PublicationImpl> _publicationList;
    private static final String PublicationSelectorPrompt = "-- Please select a publication --";


    private SortableTable publicationTable;
    private HashSet<String> tableData = null; // data structure to hold Strings (pub titles)
    private ListBox publicationSelector = null;

    private static final int COLUMN_PUB_TITLE = 0;
    private static final int MAX_PUB_COLUMN_WIDTH = 130;
    private static final int NO_ROW_SELECTED = Integer.MIN_VALUE; // so TableSelectionListener can play nice

    private PubTableSelectionListener tableListener = null;


    // Connect to Database //
    private static DownloadMetaDataServiceAsync downloadService =
            (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) downloadService).setServiceEntryPoint("download.oas");
    }


    public ProjectPublicationPanel(ProjectTabPanel parent) {

        super();

        createPublicationPanel();

        parent.add(this, "Publications");

        populatePublicationList();

        //parent.addTabListener(new PublicationTabListener());

        this.setWidth("100%");

    }


    private void createPublicationPanel() {

        // Publication Table and Remove button (top part)
        publicationTable = createTable();

        // Selector ListBox and Add button (bottom part)
        Label publicationTitle = new Label("Publication Title:          ");
        publicationTitle.setStyleName("prompt");
        publicationSelector = new ListBox();
        publicationSelector.setStyleName("EPPublicationListBox");
        HorizontalPanel availablePublicationsPanel = new HorizontalPanel();
        availablePublicationsPanel.add(publicationTitle);
        availablePublicationsPanel.add(publicationSelector);

        // Put it all together
        FlexTable grid = new FlexTable();

        EPTableUtilities.addWidgetWidgetPair(grid, 0, 0, publicationTable, createRemoveButton());
        EPTableUtilities.addWidgetWidgetPair(grid, 1, 0, availablePublicationsPanel, createAddButton());

        grid.setCellPadding(12);
        grid.setCellSpacing(4);

        this.add(grid);

    }

    private void populatePublicationList() {

        System.out.println("populating publication list");

        populatePubsDemo();  // todo: for demo purposes only

        // Retrieve Projects from Database
//        downloadService.getAccessionToPublicationMapping(new PopulatePublicationsCallback(this));

    }

    protected class PopulatePublicationsCallback implements AsyncCallback {

        LoadingLabel _loadingLabel = null;
        ProjectPublicationPanel parentPanel = null;

        public PopulatePublicationsCallback(ProjectPublicationPanel parentPanel) {

            this.parentPanel = parentPanel;

            _loadingLabel = new LoadingLabel("Loading publications...", true);

            this.parentPanel.add(_loadingLabel);

        }

        public void onFailure(Throwable throwable) {
            System.out.println("ProjectPublicationPanel: Failed to retrieve publications: " + throwable.getMessage());
        }

        public void onSuccess(Object result) {
            System.out.println("Publications received successfully");

            // invisify "loading publications..."
            _loadingLabel.setVisible(false);
            this.parentPanel.remove(_loadingLabel);


            _accessionToPublicationMap = (Map<String, PublicationImpl>) result;
            _publicationList = new ArrayList<PublicationImpl>(_accessionToPublicationMap.values());

            populatePublicationNames();

        }
    }


    protected void populatePublicationNames() {

        // if projects retrieved correctly from database
        if (_publicationList != null && _publicationList.size() != 0) {

            publicationSelector.clear();

            // always add "select project" prompt
            publicationSelector.addItem(PublicationSelectorPrompt);

            /*
            for (int i=0; i<_publicationList.size(); i++) {
                PublicationImpl currPublication = (PublicationImpl)_publicationList.get(i);

                // *note: possible cause of stack overflow in GWT testing shell
                publicationSelector.addItem(currPublication.getTitle());
            }
            */

            publicationSelector.setEnabled(true);

        }
        else {
            publicationSelector.setEnabled(false);
        }

    }

    protected void populatePubsDemo() {
        publicationSelector.addItem(PublicationSelectorPrompt);
        publicationSelector.addItem("The Sorcerer II Global Ocean Sampling Expedition: Northwest Atlantic through Eastern Tropical Pacific");
        publicationSelector.addItem("Structural and functional diversity of the microbial kinome");
        publicationSelector.addItem("Environmental Genome Shotgun Sequencing of the Sargasso Sea");
        publicationSelector.addItem("Community Genomics Among Stratified Microbial Assemblages in the Ocean's Interior");
        publicationSelector.addItem("The Marine Viromes of Four Oceanic Regions");
    }


    private SortableTable createTable() {

        // initiate table once
        SortableTable sortableTable = new SortableTable();
        sortableTable.setStyleName("EPPublicationTable");
        TextColumn publication = new TextColumn("Publication Title");
        sortableTable.addColumn(publication);  // set column

        sortableTable.setHighlightSelect(true);
        sortableTable.setNoData("This project has no papers.");

        tableListener = new PubTableSelectionListener();
        sortableTable.addSelectionListener(tableListener);

        // to store and maintain contents of table
        tableData = new HashSet<String>();

        return sortableTable;
    }

    private RoundedButton createRemoveButton() {

        RoundedButton removeButton = new RoundedButton("Remove from Project", new ClickListener() {
            public void onClick(Widget sender) {
                removePublication();
            }
        });
        removeButton.setStyleName("EPPublicationButton");

        return removeButton;
    }

    private RoundedButton createAddButton() {

        RoundedButton addButton = new RoundedButton("Add to Project", new ClickListener() {
            public void onClick(Widget sender) {
                addPublication();
            }
        });

        addButton.setStyleName("EPPublicationButton");
        return addButton;
    }


    private void addPublication() {

        String currPublication = publicationSelector.getItemText(publicationSelector.getSelectedIndex());

        tableData.clear();

        String currTableRowValue;

        int numRows = publicationTable.getTableRows().size();

        if (numRows > 0) {
            for (int i = 0; i < numRows; i++) {

                currTableRowValue = (String) ((TableRow) publicationTable.getTableRows().get(i)).getColumnValue(0);
                //addDebugMessage(" >> adding: " + currTableRowValue);

                tableData.add(currTableRowValue);
                //ddDebugMessage(" >> just added new row: " + tableData.toArray()[0] );
            }
        }

        if (tableData.size() != 0) {
            System.out.println(" >before add new row: " + tableData.toArray()[0]);
        }

        // adds new publcation into table (overwriting if repeat)
        tableData.add(currPublication);

        if (tableData.size() != 0) {
            System.out.println(" >after add new row: " + tableData.toArray()[0]);
        }


        populateTable();

    }


    private void removePublication() {

        if (tableListener.getRowSelected() != NO_ROW_SELECTED) {
            // update table
            publicationTable.removeRow(tableListener.getRowSelected());

        }
        else {
            System.out.println("no row selected");
        }
    }


    private void populateTable() {

        publicationTable.clear(); // clear old table

        System.out.println("   table Data size: " + tableData.size());

        Iterator iterator = tableData.iterator();
        String currPublication;

        int row = 1;
        while (iterator.hasNext()) {

            currPublication = (String) iterator.next();
            publicationTable.setValue(row, COLUMN_PUB_TITLE, currPublication);
            row++;

            if (row == 3)
                break;
        }

        publicationTable.refresh(); // refresh to display table

    }

    public class PubTableSelectionListener implements SelectionListener {


        int rowSelected = NO_ROW_SELECTED;

        public void onSelect(String rowString) {
            //System.out.println("selected!!!!!");
            rowSelected = Integer.parseInt(rowString);
        }

        public void onUnSelect(String rowString) {
            //System.out.println("UN - selected!!!!!");
            rowSelected = NO_ROW_SELECTED;
        }

        public int getRowSelected() {
            return rowSelected;
        }

    }

    public void updateProjectView(ProjectImpl project) {

        // UPDATE PUBLICATION TABLE //

        // add list to table
        publicationTable.clear();

        // get list of publications
        List publications = project.getPublications();
        for (int i = 0; i < publications.size(); i++) {
            String pubTitle = ((PublicationImpl) publications.get(i)).getTitle();
            publicationTable.setValue(i + 1, COLUMN_PUB_TITLE, pubTitle, new FulltextPopperUpperHTML(pubTitle, MAX_PUB_COLUMN_WIDTH));
        }

        publicationTable.refreshColumn(COLUMN_PUB_TITLE);
        publicationTable.refresh();

        /*
        if (publicationSelector != null) {

            publicationSelector.clear();

            List publications = project.getPublications();

            for (int i=0; i<publications.size(); i++) {

                PublicationImpl publication = (PublicationImpl) publications.get(i);
                publicationSelector.addItem(publication.getTitle());

            }
        }

        if (tableData != null) {
            tableData.clear();
        }
        */

    }

    public void setSampleNewProject() {
        if (publicationSelector != null) {
            System.out.println("trying to set sample publications");

            publicationSelector.clear();
            publicationSelector.addItem("Structure and Functional Diversity");
            publicationSelector.addItem("The Whale Scat Genome: Genomes");

            publicationTable.clear();

        }
        else {
            System.out.println(" publicationSelector is null");
        }
    }

}
