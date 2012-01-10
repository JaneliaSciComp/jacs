
package org.janelia.it.jacs.web.gwt.search.client.panel.sample;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NotLoggedInLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.search.client.model.SampleResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SampleSearchDataBuilder extends CategorySearchDataBuilder {
    private static Logger logger =
            Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.sample.SampleSearchDataBuilder");

    // constants for column positions
    private static int ACCESSION_COLUMN = 0; // hidden
    private static int NAME_COLUMN = 1;
    private static int PROJECT_COLUMN = 2;
    private static int REGION_COLUMN = 3;
    private static int LOCATION_COLUMN = 4;
    private static int COUNTRY_COLUMN = 5;
    private static int HABITAT_COLUMN = 6;
    private static int DEPTH_COLUMN = 7;
    private static int HOST_COLUMN = 8;
    private static int FILTER_COLUMN = 9;
    private static int READ_COUNT_COLUMN = 10;
    private static int RANK_COLUMN = 11;

    protected static int MEDIUM_COLUMN_LENGTH = 20;
    protected static int LONG_COLUMN_LENGTH = 35;

    // constants for column headings
    private static String ACCESSION_HEADING = "Accession"; // hidden
    private static String NAME_HEADING = "Sample";
    private static String PROJECT_HEADING = "Project";
    private static String REGION_HEADING = "Geographic Region";
    private static String LOCATION_HEADING = "Location";
    private static String COUNTRY_HEADING = "Country";
    private static String HABITAT_HEADING = "Habitat Type";
    private static String DEPTH_HEADING = "Sample Depth";
    private static String HOST_HEADING = "Host Organism";
    private static String FILTER_HEADING = "Filter Size";
    private static String READ_COUNT_HEADING = "Number of Reads";
    private static String RANK_HEADING = "Rank";

    private static final String DATA_PANEL_TITLE = "All Matching Samples";

    public SampleSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_SAMPLE;
    }

    protected void addDataTableHeadings(SortableTable dataTable) {
        dataTable.addColumn(new TextColumn(ACCESSION_HEADING, /*sortable*/ false, /*visible*/ false));
        dataTable.addColumn(new TextColumn(NAME_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(PROJECT_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(REGION_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(LOCATION_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(COUNTRY_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(HABITAT_HEADING, SORTABLE));
        dataTable.addColumn(new NumericColumn(DEPTH_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new TextColumn(HOST_HEADING, SORTABLE));
        dataTable.addColumn(new NumericColumn(FILTER_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new NumericColumn(READ_COUNT_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new NumericColumn(RANK_HEADING, null, SORTABLE, true));
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"sample_acc", ACCESSION_HEADING},
                {"sample_name", NAME_HEADING},
                {"project", PROJECT_HEADING},
                {"sample_region", LOCATION_HEADING},
                {"sample_location", LOCATION_HEADING},
                {"sample_country", COUNTRY_HEADING},
                {"sample_habitat", HABITAT_HEADING},
                {"sample_depth", DEPTH_HEADING},
                {"sample_host_organism", HOST_HEADING},
                {"filter_size", FILTER_HEADING},
                {"read_count", READ_COUNT_HEADING},
                {"rank", RANK_HEADING}
        };
    }

    /* Returns the accession and display name of the selected sample */
    protected String[] getSelectedValues(String rowStr) {
        int row = Integer.valueOf(rowStr).intValue();
        String accession = (String) dataTable.getValue(row, ACCESSION_COLUMN).getValue();
        String name = (String) dataTable.getValue(row, NAME_COLUMN).getValue();
        return new String[]{accession, name};
    }

    protected TableRow formatDataAsTableRow(Object data) {
        SampleResult sampleResult = (SampleResult) data;

        TableRow row = new TableRow();

        row.setValue(ACCESSION_COLUMN, new TableCell(sampleResult.getAccession()));
        if (ClientSecurityUtils.isAuthenticated()) {
            row.setValue(NAME_COLUMN, new TableCell(sampleResult.getName(),
                    getAccessionLink(sampleResult.getName(), sampleResult.getAccession())));
        }
        else {
            row.setValue(NAME_COLUMN, new TableCell(sampleResult.getName(),
                    new NotLoggedInLink(sampleResult.getName())));
        }
        row.setValue(PROJECT_COLUMN,
                new TableCell(sampleResult.getProject()));
        row.setValue(REGION_COLUMN,
                new TableCell(sampleResult.getRegion(),
                        new FulltextPopperUpperHTML(sampleResult.getRegion(), MEDIUM_COLUMN_LENGTH)));
        row.setValue(LOCATION_COLUMN,
                new TableCell(sampleResult.getLocation(),
                        new FulltextPopperUpperHTML(sampleResult.getLocation(), LONG_COLUMN_LENGTH)));
        row.setValue(COUNTRY_COLUMN,
                new TableCell(sampleResult.getCountry(),
                        new FulltextPopperUpperHTML(sampleResult.getCountry(), MEDIUM_COLUMN_LENGTH)));
        row.setValue(HABITAT_COLUMN,
                new TableCell(sampleResult.getHabitat(),
                        new FulltextPopperUpperHTML(sampleResult.getHabitat(), MEDIUM_COLUMN_LENGTH)));
        row.setValue(DEPTH_COLUMN,
                new TableCell(sampleResult.getDepth()));
        row.setValue(HOST_COLUMN, new TableCell(sampleResult.getHostOrganism(),
                new FulltextPopperUpperHTML(sampleResult.getHostOrganism(), LONG_COLUMN_LENGTH)));
        row.setValue(FILTER_COLUMN,
                new TableCell(sampleResult.getFilterSize()));
        row.setValue(READ_COUNT_COLUMN,
                new TableCell(sampleResult.getReadCount()));
        row.setValue(RANK_COLUMN,
                new TableCell(sampleResult.getRank()));
        return row;
    }

    protected PagedDataRetriever createDataRetriever() {
        return new CategoryResultDataRetriever();
    }

    void retrieveMapInfo() {
        _searchService.getMapInfoForSearchResultsBySearchId(searchId, getPanelSearchCategory(), new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                // only log the error for now
                logger.error(throwable);
            }

            public void onSuccess(Object o) {
                Set sites = (Set) o;
                ((SampleSummarySearchPanel) parentPanel).populateMap(sites);
            }
        });
    }

    protected Panel createDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        Panel dataPanel = super.createDataPanel(defaultNumVisibleRows, pageLengthOptions);

        SimplePanel hintPanel = new SimplePanel();
        hintPanel.setStyleName("SearchDetailsTableHintPanel");
        hintPanel.add(HtmlUtils.getHtml(
                "&bull;&nbsp;" +
                        "Click a Sample name for more details." +
                        "&nbsp;&bull;&nbsp;&nbsp;" +
                        "Select a Sample row to view all reads for that Sample below.",
                "hint"));

        dataPanel.add(hintPanel);
        return dataPanel;
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }
}
