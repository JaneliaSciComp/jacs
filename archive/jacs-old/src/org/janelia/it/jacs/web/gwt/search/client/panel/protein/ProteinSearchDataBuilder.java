
package org.janelia.it.jacs.web.gwt.search.client.panel.protein;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.ProteinSearchExportTask;
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
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.LoginProtectedMenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.detail.client.peptide.PeptideTableBuilder;
import org.janelia.it.jacs.web.gwt.detail.client.util.AnnotationUtil;
import org.janelia.it.jacs.web.gwt.search.client.model.ProteinResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class ProteinSearchDataBuilder extends CategorySearchDataBuilder {
    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.protein.ProteinSearchDataBuilder");

    private static final String DATA_PANEL_TITLE = "All Matching Proteins";

    private static String TAXONOMY_HEADING = "Taxonomy";
    private static String EXTERNAL_ACCESSION_HEADING = "External Accession";
    private static String NCBI_GI_HEADING = "NCBI GI";
    private static String FINAL_CLUSTER_HEADING = "Final Cluster";
    private static String CORE_CLUSTER_HEADING = "Core Cluster";
    private static String RANK_HEADING = "Rank";

    private static Integer TAXONOMY_COLUMN_LENGTH = 25;
    private static Integer BIG_COLUMN_LENGTH = 50;

    private PeptideTableBuilder _tableBuilder;

    public ProteinSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_PROTEIN;
    }

    protected SortableTable createDataTable() {
        // Custom columns for the standard peptide table
        List<TableColumn> customColumns = new ArrayList();
        customColumns.add(new TextColumn(EXTERNAL_ACCESSION_HEADING));
        customColumns.add(new TextColumn(NCBI_GI_HEADING));
        customColumns.add(new TextColumn(TAXONOMY_HEADING));
        customColumns.add(new TextColumn(FINAL_CLUSTER_HEADING));
        customColumns.add(new TextColumn(CORE_CLUSTER_HEADING));

        _tableBuilder = new PeptideTableBuilder(customColumns);
        _tableBuilder.getSortableTable().addColumn(new NumericColumn(RANK_HEADING));
        return _tableBuilder.getSortableTable();
    }

    /**
     * override parent's implementation to do nothing
     */
    protected void addDataTableHeadings(SortableTable dataTable) {
    }

    protected String[][] getSortOptions() {
        String[][] sortOptions = _tableBuilder.getSortOptions(new String[][]{
                {"external_acc", EXTERNAL_ACCESSION_HEADING},
                {"ncbi_gi_number", NCBI_GI_HEADING},
                {"taxon_names", TAXONOMY_HEADING},
                {"final_cluster_acc", FINAL_CLUSTER_HEADING},
                {"core_cluster_acc", CORE_CLUSTER_HEADING},
        });

        // add a rank sort option
        String[][] allSortOptions = new String[sortOptions.length + 1][];
        System.arraycopy(sortOptions, 0, allSortOptions, 0, sortOptions.length);
        allSortOptions[sortOptions.length] = new String[]{"rank", RANK_HEADING};

        return allSortOptions;
    }

    protected TableRow formatDataAsTableRow(Object data) {
        ProteinResult protein = (ProteinResult) data;

        // Stock protein table columns
        List<TableCell> cells = new ArrayList();
        if (ClientSecurityUtils.isAuthenticated())
            cells.add(new TableCell(protein.getAccession(), getAccessionLink(protein.getAccession())));
        else
            cells.add(new TableCell(protein.getAccession(), new NotLoggedInLink(protein.getAccession())));

        cells.add(new TableCell(protein.getProteinFunction(),
                new FulltextPopperUpperHTML(protein.getProteinFunction(), BIG_COLUMN_LENGTH)));

        cells.add(new TableCell(protein.getGeneNames(),
                new FulltextPopperUpperHTML(protein.getGeneNames(), BIG_COLUMN_LENGTH)));

        cells.add(new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(protein.getGoAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(protein.getGoAnnotationDescription())));

        cells.add(new TableCell(
                AnnotationUtil.createAnnotationStringFromAnnotationList(protein.getEcAnnotationDescription()),
                AnnotationUtil.createAnnotationLinksWidget(protein.getEcAnnotationDescription())));

        cells.add(new TableCell(protein.getSequenceLength())); //TODO: format with commas

        if (protein.getRank() != null)
            cells.add(new TableCell(protein.getRank()));
        else
            cells.add(new TableCell(""));

        // Custom column values
        List<TableCell> customCells = new ArrayList();
        customCells.add(new TableCell(protein.getExternalAccession()));

        if (protein.getNcbiGiNumber() != null && !protein.getNcbiGiNumber().equals("0"))
            customCells.add(new TableCell(protein.getNcbiGiNumber()));
        else
            customCells.add(new TableCell(""));

        if (protein.getTaxonomy() != null)
            customCells.add(new TableCell(protein.getTaxonomy(),
                    new FulltextPopperUpperHTML(protein.getTaxonomy(), TAXONOMY_COLUMN_LENGTH)));
        else
            customCells.add(new TableCell(""));

        if (protein.getFinalCluster() != null) {
            if (ClientSecurityUtils.isAuthenticated()) {
                customCells.add(new TableCell(protein.getFinalCluster(), getAccessionLink(protein.getFinalCluster())));
                customCells.add(new TableCell(protein.getCoreCluster(), getAccessionLink(protein.getCoreCluster())));
            }
            else {
                customCells.add(new TableCell(protein.getFinalCluster(), new NotLoggedInLink(protein.getFinalCluster())));
                customCells.add(new TableCell(protein.getCoreCluster(), new NotLoggedInLink(protein.getCoreCluster())));
            }
        }
        else {
            customCells.add(new TableCell(""));
            customCells.add(new TableCell(""));
        }

        return _tableBuilder.createTableRow(cells, customCells);
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

    protected PagedDataRetriever createDataRetriever() {
        return new CategoryResultDataRetriever();
    }

    private Widget getExportMenu() {
        final MenuBarWithRightAlignedDropdowns menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        LoginProtectedMenuBarWithRightAlignedDropdowns dropDown = new LoginProtectedMenuBarWithRightAlignedDropdowns(true, "You must be logged in to export");

        dropDown.addItem("Protein search results as CSV", true, new Command() {
            public void execute() {
                logger.debug("Protein Search Results as CSV");
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ProteinSearchExportTask exportTask = new ProteinSearchExportTask(searchId,
                        ExportWriterConstants.EXPORT_TYPE_CSV,
                        null, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        dropDown.addItem("Protein sequences as FASTA", true, new Command() {
            public void execute() {
                logger.debug("Export protein sequences as FASTA");
                SortableColumn[] sortArgs = dataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ProteinSearchExportTask exportTask = new ProteinSearchExportTask(searchId,
                        ExportWriterConstants.EXPORT_TYPE_FASTA,
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

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

}
