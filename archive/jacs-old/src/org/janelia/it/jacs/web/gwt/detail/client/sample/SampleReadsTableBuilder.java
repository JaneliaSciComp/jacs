
package org.janelia.it.jacs.web.gwt.detail.client.sample;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.tasks.export.SampleExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.NotLoggedInLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.LoginProtectedMenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBarWithRightAlignedDropdowns;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;
import org.janelia.it.jacs.web.gwt.detail.client.service.sample.SampleService;
import org.janelia.it.jacs.web.gwt.detail.client.service.sample.SampleServiceAsync;
import org.janelia.it.jacs.web.gwt.detail.client.util.AccessionLinkBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SampleReadsTableBuilder {

    // constants for column positions
    private static int ACCESSION_COLUMN = 0;
    private static int LENGTH_COLUMN = 1;
    private static int CLEARBEGIN_COLUMN = 2;
    private static int CLEAREND_COLUMN = 3;
    private static int DIRECTION_COLUMN = 4;

    // constants for column headings
    private static String ACCESSION_HEADING = "Accession";
    private static String LENGTH_HEADING = "Length";
    private static String CLEARBEGIN_HEADING = "Clear Range Begin";
    private static String CLEAREND_HEADING = "Clear Range End";
    private static String DIRECTION_HEADING = "Sequencing Direction";

    private static final boolean SORTABLE = true;

    private static SampleServiceAsync _sampleService = (SampleServiceAsync) GWT.create(SampleService.class);

    static {
        ((ServiceDefTarget) _sampleService).setServiceEntryPoint("sampledetail.oas");
    }

    private class SampleReadsDataRetriever implements PagedDataRetriever {

        SampleReadsDataRetriever() {
        }

        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            if (sampleAcc == null) {
                listener.onNoData();
            }
            else {
                _sampleService.getNumSampleReads(sampleAcc, new AsyncCallback() {
                    public void onFailure(Throwable caught) {
                        listener.onFailure(caught);
                    }

                    public void onSuccess(Object result) {
                        listener.onSuccess(result); // Integer
                    }
                });
            }
        }

        public void retrieveDataRows(int startIndex,
                                     int numRows,
                                     SortArgument[] sortArgs,
                                     DataRetrievedListener listener) {
            if (sampleAcc == null) {
                listener.onNoData();
            }
            else {
                _sampleService.getPagedSampleReads(sampleAcc, startIndex, numRows, sortArgs,
                        createSampleReadsCallback(listener));
            }
        }
    }

    private String sampleAcc;
    private PagingPanel readDataPagingPanel;
    private SortableTable readDataTable;
    private EntityListener entityListener;
    private AccessionLinkBuilder readAccessionBuilder;

    public SampleReadsTableBuilder() {
        readAccessionBuilder = new AccessionLinkBuilder() {
            public Widget createAccessionLink(final String displayName, final String accession) {
                return new Link(displayName, new ClickListener() {
                    public void onClick(Widget widget) {
                        if (readDataTable != null) {
                            readDataTable.clearHover(); // Have to remove the highlight style since the table will never get a mouse out event
                        }
                        if (entityListener != null) {
                            entityListener.onEntitySelected(accession, null);
                        }
                    }
                });
            }
        };
    }

    public void setEntityListener(EntityListener entityListener) {
        this.entityListener = entityListener;
    }

    public PagingPanel createReadDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        readDataTable = new SortableTable();
        addReadDataTableHeadings(readDataTable);
        RemotingPaginator dataPaginator = new RemotingPaginator(readDataTable, getReadSortOptions(), "SampleReadsTable");
        readDataPagingPanel = new RemotePagingPanel(readDataTable,
                pageLengthOptions,
                true,
                true,
                dataPaginator,
                false,
                PagingPanel.ADVANCEDSORTLINK_IN_THE_HEADER,
                defaultNumVisibleRows,
                "SampleReadsData");
        readDataPagingPanel.addAdvancedSortClickListener(new AdvancedSortableRemotePaginatorClickListener
                (readDataTable, dataPaginator, (RemotePagingPanel) readDataPagingPanel));
        DockPanel footer = readDataPagingPanel.getTableFooterPanel();
        Widget exportMenu = createExportMenu();
        footer.add(exportMenu, DockPanel.EAST);
        footer.setCellHorizontalAlignment(exportMenu, DockPanel.ALIGN_RIGHT);
        readDataPagingPanel.showTableFooter();
        readDataPagingPanel.setStyleName("sampleReadsTablePanel");
        return readDataPagingPanel;
    }

    public void populateSampleReads(String sampleAcc) {
        this.sampleAcc = sampleAcc;
        readDataPagingPanel.clear();
        ((RemotingPaginator) readDataPagingPanel.getPaginator()).setDataRetriever(
                new SampleReadsDataRetriever());
        readDataPagingPanel.first();
    }

    private void addReadDataTableHeadings(SortableTable dataTable) {
        dataTable.addColumn(new TextColumn(ACCESSION_HEADING, SORTABLE));
        dataTable.addColumn(new NumericColumn(LENGTH_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new NumericColumn(CLEARBEGIN_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new NumericColumn(CLEAREND_HEADING, null, SORTABLE, true));
        dataTable.addColumn(new TextColumn(DIRECTION_HEADING, SORTABLE));
    }

    private String[][] getReadSortOptions() {
        return new String[][]{
                {"accession", ACCESSION_HEADING},
                {"sequenceLength", LENGTH_HEADING},
                {"clearRangeBegin", CLEARBEGIN_HEADING},
                {"clearRangeEnd", CLEAREND_HEADING},
                {"sequencingDirection", DIRECTION_HEADING}
        };
    }

    private Widget createExportMenu() {
        final MenuBarWithRightAlignedDropdowns menu = new MenuBarWithRightAlignedDropdowns();
        menu.setAutoOpen(false);

        LoginProtectedMenuBarWithRightAlignedDropdowns dropDown =
                new LoginProtectedMenuBarWithRightAlignedDropdowns(true, "You must be logged in to export");

        dropDown.addItem("Sample reads as CSV", true, new Command() {
            public void execute() {
                SortableColumn[] sortArgs = readDataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ArrayList<String> accessionList = new ArrayList<String>();
                accessionList.add(sampleAcc);
                SampleExportTask exportTask = new SampleExportTask(ExportWriterConstants.EXPORT_TYPE_CSV,
                        accessionList, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        dropDown.addItem("Sample reads as FASTA", true, new Command() {
            public void execute() {
                SortableColumn[] sortArgs = readDataPagingPanel.getSortableTable().getSortColumns();
                ArrayList<SortArgument> sortList = new ArrayList<SortArgument>();
                if (null != sortArgs && sortArgs.length > 0) {
                    sortList.addAll(Arrays.asList(sortArgs));
                }
                ArrayList<String> accessionList = new ArrayList<String>();
                accessionList.add(sampleAcc);
                SampleExportTask exportTask = new SampleExportTask(ExportWriterConstants.EXPORT_TYPE_FASTA,
                        accessionList, sortList);
                new AsyncExportTaskController(exportTask).start();
            }
        });

        MenuItem export = new MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

    private AsyncCallback createSampleReadsCallback(final DataRetrievedListener listener) {
        return new AsyncCallback() {
            public void onFailure(Throwable caught) {
                listener.onFailure(caught);
            }

            public void onSuccess(Object result) {
                List resultList = (List) result;
                if (resultList == null || resultList.size() == 0) {
                    listener.onNoData();
                }
                else {
                    listener.onSuccess(formatReadListAsTableRowList(resultList));
                }
            }
        };
    }

    private List formatReadListAsTableRowList(List dataList) {
        List tableRows = new ArrayList();
        for (Iterator dataItr = dataList.iterator(); dataItr.hasNext();) {
            TableRow row = formatReadAsTableRow((Read) dataItr.next());
            tableRows.add(row);
        }
        return tableRows;
    }

    private TableRow formatReadAsTableRow(Read r) {
        TableRow row = new TableRow();
        if (ClientSecurityUtils.isAuthenticated()) {
            row.setValue(ACCESSION_COLUMN,
                    new TableCell(r.getAccession(),
                            readAccessionBuilder.createAccessionLink(r.getAccession(), r.getAccession())));
        }
        else {
            row.setValue(ACCESSION_COLUMN,
                    new TableCell(r.getAccession(), new NotLoggedInLink(r.getAccession())));
        }
        row.setValue(LENGTH_COLUMN, new TableCell(r.getSequenceLength()));
        row.setValue(CLEARBEGIN_COLUMN, new TableCell(r.getClearRangeBegin()));
        row.setValue(CLEAREND_COLUMN, new TableCell(r.getClearRangeEnd()));
        Widget orientationWidget = null;
        if (r.getSequencingDirection() != null) {
            if (r.getSequencingDirection().equalsIgnoreCase("forward")) {
                orientationWidget = ImageBundleFactory.getControlImageBundle().getArrowForwardImage().createImage();
            }
            else if (r.getSequencingDirection().equalsIgnoreCase("reverse")) {
                orientationWidget = ImageBundleFactory.getControlImageBundle().getArrowReverseImage().createImage();
            }
        }
        if (orientationWidget != null) {
            row.setValue(DIRECTION_COLUMN, new TableCell(r.getSequencingDirection(), orientationWidget));
        }
        else {
            row.setValue(DIRECTION_COLUMN, new TableCell("n/a"));
        }
        return row;
    }

}
