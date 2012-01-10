
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.download.MooreOrganism;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableSortListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortableTableClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.DateColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;

import java.util.Iterator;
import java.util.List;


/**
 * This class is responsible for displaying data from the MF150 database
 *
 * @author Tareq Nabeel
 */
public class MooreDataPanel extends VerticalPanel {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client.MooreDataPanel");

    private static MooreServiceAsync mfService = (MooreServiceAsync) GWT.create(MooreService.class);

    static {
        ((ServiceDefTarget) mfService).setServiceEntryPoint("mooreFoundationData.oas");
    }

    private LoadingLabel loadingLabel;
    private Project project;
    private SortableTable organismsTable;
    private SecondaryTitledBox organismsBox;

    /**
     * Preconstructs the panels and retrieves and displays data from the MF150 database
     *
     * @param project
     */
    public MooreDataPanel(Project project) {
        this.project = project;

        constructSubPanels();

        retrieveAndDisplayData();
    }

    /**
     * Preconstructs the panel skeletons
     */
    private void constructSubPanels() {
        loadingLabel = new LoadingLabel("Loading organisms...", true);
        add(HtmlUtils.getHtml("This project has no papers.", "text", "DownloadByPubsNoPapersMessage"));
        add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        HorizontalPanel horizontalPanel = new HorizontalPanel();
        add(horizontalPanel);

        organismsBox = new SecondaryTitledBox("Marine Organisms", false);
        organismsBox.setWidth("100%");
        organismsBox.add(loadingLabel);
        horizontalPanel.add(organismsBox);
        addSpacer(horizontalPanel);
        horizontalPanel.add(getPublicationLinksPanel());

        createOrganismsTable();
        PagingPanel pagingPanel = new PagingPanel(organismsTable, true, PagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER,
                "MooreData");
        pagingPanel.addAdvancedSortClickListener(new AdvancedSortableTableClickListener(organismsTable,
                organismsTable.getAllSortableColumns()));
//        if (BrowserDetector.isIE()) {
        pagingPanel.setScrollPanelStyleName("mooreDataPagingScrollPanelIE");
        pagingPanel.setTopControlsPanelStyleName("mooreDataPagingPanelControlsPanelIE");
        pagingPanel.setBottomControlsPanelStyleName("mooreDataPagingPanelControlsPanelIE");
//        }
        organismsBox.add(pagingPanel);
    }

    /**
     * Retrieves data from the MF150 database using MooreService
     */
    private void retrieveAndDisplayData() {
        mfService.getOrganisms(new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                organismsBox.clear();
                loadingLabel.setText("Data unavailable");
                organismsBox.add(loadingLabel);
            }

            public void onSuccess(Object object) {
                logger.info("MooreDataPanel mfService.getOrganisms() returned successfully");
                try {
                    List organismList = (List) object;
                    logger.info("organismList.size()=" + organismList.size());
                    populateOrganismsTable(organismList);
                }
                catch (Exception e) {
                    logger.error("Exception raised while processing the returned organism list in retrieveAndDisplayData", e);
                }
            }
        });
    }

    /**
     * Preconstructs the organism table with the headers
     */
    private void createOrganismsTable() {
        organismsTable = new SortableTable();
        organismsTable.addSortListener(new SortableTableBusyListener());
        organismsTable.addColumn(new TextColumn("Organism"));
        organismsTable.addColumn(new TextColumn("Investigator"));
        organismsTable.addColumn(new DateColumn("Release Date"));
        organismsTable.addColumn(new TextColumn("Status"));
//        if (BrowserDetector.isIE()) {
        organismsTable.addStyleName("mooreOrganismTable");
//        }
    }

    /**
     * Populates the preconstructed organism table with data retrieved from MF150 database
     *
     * @param organismList
     */
    private void populateOrganismsTable(List organismList) {
        RowIndex rowIndex = new RowIndex(1);
        for (Iterator iterator = organismList.iterator(); iterator.hasNext();) {
            MooreOrganism organism = (MooreOrganism) iterator.next();
            addTableRow(organism, rowIndex);
        }
        organismsTable.sort();
    }

    /**
     * Adds a single row to the organismsTable
     *
     * @param organism
     * @param rowIndex
     */
    private void addTableRow(MooreOrganism organism, RowIndex rowIndex) {
        try {
            int col = 0;
            // Can't externalize this to jacs.properties because GWT can't compile SystemConfigurationProperties because of java.io.
            String link = "https://research.venterinstitute.org/moore/SingleOrganism.do?speciesTag=" + organism.getSpeciesTag() + "&pageAttr=pageMain";
            organismsTable.setValue(rowIndex.getCurrentRow(), col++, organism.getOrganismName(), new ExternalLink(organism.getOrganismName(), link));
            organismsTable.setValue(rowIndex.getCurrentRow(), col++, organism.getInvestigatorName());
            if (organism.getReleaseDate() == null) {
                organismsTable.setValue(rowIndex.getCurrentRow(), col++, "&nbsp;");
            }
            else {
                organismsTable.setValue(rowIndex.getCurrentRow(), col++, new FormattedDate(organism.getReleaseDate().getTime()).toString());
            }
            organismsTable.setValue(rowIndex.getCurrentRow(), col++, organism.getStatus());
            rowIndex.increment();
        }
        catch (Exception e) {
            logger.error("Caught exception processing row " + rowIndex.getCurrentRow() + " organism=" + organism.toString() + " " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates the panel with links to Browse project and Project website for the More Information panel
     *
     * @return
     */
    private Panel getPublicationLinksPanel() {
        SecondaryTitledBox infoBox = new SecondaryTitledBox("More Information", false);

        Grid grid = new Grid(2, 1);
        if (project != null) {
            grid.setWidget(0, 0, TableUtils.getLinkCell(
                    new Link("Browse the project", getProjectUrlBySymbol(project.getProjectSymbol()))));
            if (project.getWebsite() != null)
                grid.setWidget(1, 0, TableUtils.getLinkCell(new ExternalLink("View the project website", project.getWebsite())));
        }
        infoBox.add(grid);

        return infoBox;
    }

    private String getProjectUrlBySymbol(String projectSymbol) {
        return "/jacs/gwt/BrowseProjectsPage/BrowseProjectsPage.oa?projectSymbol=" + projectSymbol;
    }

    private class SortableTableBusyListener implements TableSortListener {
        public void onBusy(Widget widget) {
            loadingLabel.setText("Sorting...");
            loadingLabel.setVisible(true);
        }

        public void onBusyDone(Widget widget) {
            loadingLabel.setVisible(false);
        }
    }

    /**
     * Adds space between the organism panel and the more information panel
     *
     * @param horizontalPanel
     */
    private void addSpacer(HorizontalPanel horizontalPanel) {
        horizontalPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        horizontalPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        horizontalPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        horizontalPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
    }
}
