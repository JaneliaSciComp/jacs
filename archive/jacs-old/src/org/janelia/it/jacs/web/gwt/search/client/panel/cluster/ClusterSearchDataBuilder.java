
package org.janelia.it.jacs.web.gwt.search.client.panel.cluster;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.ClusterSearchExportTask;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NotLoggedInLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.LoginProtectedMenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;
import org.janelia.it.jacs.web.gwt.search.client.model.ClusterResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class ClusterSearchDataBuilder extends CategorySearchDataBuilder {
    private static Logger logger =
            Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.cluster.ClusterSearchDataBuilder");

    // constants for column positions
    //private static int HEADLINE_COLUMN = 1;
    private static int FINAL_ACCESSION_COLUMN = 0;
    private static int NUM_CORE_CLUSTERS_COLUMN = 1;
    private static int NUM_PROTEINS_COLUMN = 2;
    private static int NUM_NR_PROTEINS_COLUMN = 3;
    private static int GENE_SYMBOLS_COLUMN = 4;
    private static int PROTEIN_FUNCTIONS_COLUMN = 5;
    private static int EC_COLUMN = 6;
    private static int GO_COLUMN = 7;
    private static int RANK_COLUMN = 8;

    // constants for column headings
    private static String FINAL_ACCESSION_HEADING = "Final Cluster";
    private static String NUM_CORE_CLUSTERS_HEADING = "#<br>Core<br>Clusters";
    private static String NUM_PROTEINS_HEADING = "#<br>Proteins";
    private static String NUM_NR_PROTEINS_HEADING = "# Non-<br>Redundant";
    private static String GENE_SYMBOLS_HEADING = "Gene Symbols";
    private static String PROTEIN_FUNCTIONS_HEADING = "Protein Functions";
    private static String EC_HEADING = "EC #";
    private static String GO_HEADING = "Gene Ontology";
    private static String RANK_HEADING = "Rank";

    private static final String DATA_PANEL_TITLE = "All Matching Clusters";

    // maximum displayed length for some of the columns
    private static final int ANNOTATION_COLUMN_LENGTH = 35;
    private static final int GENE_SYMBOL_COLUMN_LENGTH = 15;

    public ClusterSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_CLUSTER;
    }

    protected void addDataTableHeadings(SortableTable dataTable) {
        dataTable.addColumn(new TextColumn(FINAL_ACCESSION_HEADING, SORTABLE));
        dataTable.addColumn(new NumericColumn(NUM_CORE_CLUSTERS_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new NumericColumn(NUM_PROTEINS_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new NumericColumn(NUM_NR_PROTEINS_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new TextColumn(GENE_SYMBOLS_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(PROTEIN_FUNCTIONS_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(EC_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(GO_HEADING, SORTABLE));
        dataTable.addColumn(new TextColumn(RANK_HEADING, SORTABLE));
    }

    protected String[][] getSortOptions() {
        return new String[][]{
                {"final_cluster_acc", FINAL_ACCESSION_HEADING},
                {"num_core_cluster", NUM_CORE_CLUSTERS_HEADING},
                {"num_protein", NUM_PROTEINS_HEADING},
                {"num_nonredundant", NUM_NR_PROTEINS_HEADING},
                {"gn_symbols", GENE_SYMBOLS_HEADING},
                {"protein_functions", PROTEIN_FUNCTIONS_HEADING},
                {"ec_list", EC_HEADING},
                {"go_list", GO_HEADING},
                {"rank", RANK_HEADING}
        };
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ClusterResult clusterResult = (ClusterResult) data;
        TableRow row = new TableRow();
        if (ClientSecurityUtils.isAuthenticated()) {
            row.setValue(FINAL_ACCESSION_COLUMN,
                    new TableCell(clusterResult.getFinalAccession(),
                            getAccessionLink(clusterResult.getFinalAccession())));
        }
        else {
            row.setValue(FINAL_ACCESSION_COLUMN,
                    new TableCell(clusterResult.getFinalAccession(),
                            new NotLoggedInLink(clusterResult.getFinalAccession())));
        }
        row.setValue(NUM_CORE_CLUSTERS_COLUMN,
                new TableCell(clusterResult.getNumCoreClusters()));
        row.setValue(NUM_PROTEINS_COLUMN,
                new TableCell(clusterResult.getNumProteins()));
        row.setValue(NUM_NR_PROTEINS_COLUMN,
                new TableCell(clusterResult.getNumNRProteins()));
        String annoString = clusterResult.getGeneSymbols();
        if (annoString == null || annoString.length() == 0) {
            row.setValue(GENE_SYMBOLS_COLUMN, new TableCell(annoString));
        }
        else {
            row.setValue(GENE_SYMBOLS_COLUMN,
                    new TableCell(annoString, new FulltextPopperUpperHTML(annoString, GENE_SYMBOL_COLUMN_LENGTH)));
        }
        annoString = clusterResult.getProteinFunctions();
        if (annoString == null || annoString.length() == 0) {
            row.setValue(PROTEIN_FUNCTIONS_COLUMN, new TableCell(annoString));
        }
        else {
            row.setValue(PROTEIN_FUNCTIONS_COLUMN,
                    new TableCell(annoString, new FulltextPopperUpperHTML(annoString, ANNOTATION_COLUMN_LENGTH)));
        }
        row.setValue(EC_COLUMN, new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(clusterResult.getEcAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(clusterResult.getEcAnnotationDescription())));
        row.setValue(GO_COLUMN, new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(clusterResult.getGoAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(clusterResult.getGoAnnotationDescription())));
        row.setValue(RANK_COLUMN, new TableCell(clusterResult.getRank()));
        return row;
    }

    protected Panel createDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        Panel panel = super.createDataPanel(defaultNumVisibleRows, pageLengthOptions);

        // Add a footer to the table
        DockPanel footer = dataPagingPanel.getTableFooterPanel();
        Widget exportMenu = getExportMenu();
        footer.add(exportMenu, DockPanel.EAST);
        footer.setCellHorizontalAlignment(exportMenu, DockPanel.ALIGN_RIGHT);
        dataPagingPanel.showTableFooter();

        return panel;
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

    protected PagedDataRetriever createDataRetriever() {
        return new CategoryResultDataRetriever();
    }

    private Widget getExportMenu() {
        final MenuBar menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        LoginProtectedMenuBarWithRightAlignedDropdowns dropDown = new LoginProtectedMenuBarWithRightAlignedDropdowns(true,
                "You must be logged in to export");

        dropDown.addItem("Cluster search results as CSV", true, new Command() {
            public void execute() {
                logger.debug("Cluster Search Results as CSV");
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ClusterSearchExportTask exportTask = new ClusterSearchExportTask(searchId,
                        ExportWriterConstants.EXPORT_TYPE_CSV,
                        null, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        MenuItem export = new MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

}
