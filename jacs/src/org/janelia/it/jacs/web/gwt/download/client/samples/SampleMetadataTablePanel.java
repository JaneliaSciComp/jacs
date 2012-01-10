
package org.janelia.it.jacs.web.gwt.download.client.samples;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.SampleItem;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.NotLoggedInPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.security.ClientSecurityUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.DownloadEchoService;
import org.janelia.it.jacs.web.gwt.common.client.service.DownloadEchoServiceAsync;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.AbbreviatedSize;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FulltextPopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.NumericString;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.*;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataService;
import org.janelia.it.jacs.web.gwt.download.client.DownloadMetaDataServiceAsync;
import org.janelia.it.jacs.web.gwt.download.client.DownloadSampleFileClickListener;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.project.ProjectSymbolSelectedListener;
import org.janelia.it.jacs.web.gwt.download.client.samples.wizard.SampleInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SampleMetadataTablePanel extends Composite implements DataRetrievedListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.download.client.samples.ProjectSamplesPage");

    private SampleInfo _data;
    private PagingPanel _pagingPanel;
    private SortableTable _table;
    private ProjectSymbolSelectedListener _projectSymbolSelectedListener;
    private RemotingPaginator _paginator;
    private List<String> _selectedProjects;

    private static final int GEOGRAPHIC_LOCATION_VISIBLE_SIZE = 20;
    private static final int SAMPLE_LOCATION_VISIBLE_SIZE = 20;

    private static final int DATASET_NAME_SIZE = 40;
    private static final String EXPORT_LINK_TEXT = "Export as CSV";

    private static DownloadEchoServiceAsync downloadEchoService = (DownloadEchoServiceAsync) GWT.create(DownloadEchoService.class);

    static {
        ((ServiceDefTarget) downloadEchoService).setServiceEntryPoint("downloadEcho.oas");
    }

    private static DownloadMetaDataServiceAsync metadataService = (DownloadMetaDataServiceAsync) GWT.create(DownloadMetaDataService.class);

    static {
        ((ServiceDefTarget) metadataService).setServiceEntryPoint("download.oas");
    }

    private static final String PROJECT_HEADER = "Project";
    private final static String DATASET_HEADER = "Sample Dataset";
    private final static String FILE_SIZE_HEADER = "File Size";
    private final static String HABITAT_HEADER = "Habitat Type";
    private final static String GEOGRAPHIC_LOCATION_HEADER = "Geographic Location";
    private final static String SAMPLE_LOCATION_HEADER = "Sample Location";
    private final static String COUNTRY_HEADER = "Country";
    private final static String FILTER_SIZE_HEADER = "Filter Size";
    private final static String LAT_HEADER = "Latitude";
    private final static String LONG_HEADER = "Longitude";
    private final static String SAMPLE_DEPTH_HEADER = "Depth";
    private final static String WATER_DEPTH_HEADER = "Wat. Dep.";
    private final static String CHLOROPHYLL_HEADER = "Chlorophyll";
    private final static String OXYGEN_HEADER = "Oxygen";
    private final static String FLOURESCENCE_HEADER = "Fluor.";
    private final static String SALINITY_HEADER = "Salin.";
    private final static String TEMP_HEADER = "Temp";
    private final static String TRANSMISSION_HEADER = "Trans.";
    private final static String BIOMASS_HEADER = "BioMass";
    private final static String INORG_CARBON_HEADER = "Inorg. Carbon";
    private final static String INORG_PHOSPHATE_HEADER = "Inorg. Phospate";
    private final static String ORG_CARBON_HEADER = "Org. Carbon";
    private final static String NITRITE_HEADER = "Nitr.";
    private final static String NUM_POOLED_HEADER = "# Pooled";
    private final static String NUM_SAMPLED_HEADER = "# Sampled";
    private final static String VOLUME_HEADER = "Volume";
    private final static String COLLECTION_DATE_HEADER = "Coll. Date";

    public SampleMetadataTablePanel(ProjectSymbolSelectedListener projectSymbolSelectedListener, SampleInfo data) {
        _projectSymbolSelectedListener = projectSymbolSelectedListener;
        _data = data;
        init();
    }

    public SampleMetadataTablePanel() {
    }

    public void init() {
        // Create the main panel and display the loading label
        initWidget(getContentPanel());
    }

    public VerticalPanel getContentPanel() {
        TitledBox titledBox = new TitledBox("Sample Metadata", true);
        titledBox.setWidth("250px");

        _table = createTable();
        _paginator = new RemotingPaginator(_table, new SampleDataRetriever(), getSortArgs(), "ProjectSampleMetadata");
        _pagingPanel = new RemotePagingPanel(_table, new String[]{"10", "20", "50"}, /*scrolling*/ true, /*loading label*/ true,
                _paginator, /*footer*/ false, PagingPanel.NO_ADVNACED_SORT_LINK_ANYWHERE_EVER_FORGET_IT_DUDE,
                /*default rows*/20, "ProjectSampleMetadata");
        _pagingPanel.setScrollPanelStyleName("projectSamplesPagingPanelScrollPanelIE");
        _pagingPanel.setTopControlsPanelStyleName("projectSamplesPagingPanelControlsPanelIE");
        _pagingPanel.setBottomControlsPanelStyleName("projectSamplesPagingPanelControlsPanelIE");
        _pagingPanel.addCustomContentTop(getExportLink());
        titledBox.add(_pagingPanel);

        //Delay loading the panel until the rest of the page has a chance to load
        new Timer() {
            public void run() {
                // If the URL specfied a particular project, restrict the metadata to just that project.  Have to
                // do it here because the data object may not have been populated yet in the constructor
                if (_data != null && _data.getInitialProjectSymbol() != null) {
                    retrieveProjectSymbols();
                }
                else {
                    // signal the table to fetch the first page of data
                    _paginator.first();
                }
            }
        }.schedule(4000);

        return titledBox;
    }

    public void retrieveProjectSymbols() {
        metadataService.getSymbolToProjectMapping(new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                //TODO: notify failure
            }

            public void onSuccess(Object result) {
                // Convert the project symbol on the command line to a project name found in the samples
                Map<String, Project> projects = (Map<String, Project>) result;
                String selectedProjectName = null;
                for (Map.Entry entry : projects.entrySet()) {
                    if (entry.getKey().equals(_data.getInitialProjectSymbol()))
                        selectedProjectName = ((Project) entry.getValue()).getProjectName();
                }

                ArrayList<String> projectNames = new ArrayList<String>();
                projectNames.add(selectedProjectName);
                _selectedProjects = projectNames;

                // Now trigger the table to retrieve the sample data
                _paginator.first();
            }
        });
    }

    private String[][] getSortArgs() {
        return new String[][]{
                {PROJECT_HEADER, PROJECT_HEADER}
                , {DATASET_HEADER, DATASET_HEADER}
                , {FILE_SIZE_HEADER, FILE_SIZE_HEADER}
                , {HABITAT_HEADER, HABITAT_HEADER}
                , {GEOGRAPHIC_LOCATION_HEADER, GEOGRAPHIC_LOCATION_HEADER}
                , {SAMPLE_LOCATION_HEADER, SAMPLE_LOCATION_HEADER}
                , {COUNTRY_HEADER, COUNTRY_HEADER}
                , {FILTER_SIZE_HEADER, FILTER_SIZE_HEADER}
                , {LAT_HEADER, LAT_HEADER}
                , {LONG_HEADER, LONG_HEADER}
                , {SAMPLE_DEPTH_HEADER, SAMPLE_DEPTH_HEADER}
                , {WATER_DEPTH_HEADER, WATER_DEPTH_HEADER}
                , {CHLOROPHYLL_HEADER, CHLOROPHYLL_HEADER}
                , {OXYGEN_HEADER, OXYGEN_HEADER}
                , {FLOURESCENCE_HEADER, FLOURESCENCE_HEADER}
                , {SALINITY_HEADER, SALINITY_HEADER}
                , {TEMP_HEADER, TEMP_HEADER}
                , {TRANSMISSION_HEADER, TRANSMISSION_HEADER}
                , {BIOMASS_HEADER, BIOMASS_HEADER}
                , {INORG_CARBON_HEADER, INORG_CARBON_HEADER}
                , {INORG_PHOSPHATE_HEADER, INORG_PHOSPHATE_HEADER}
                , {ORG_CARBON_HEADER, ORG_CARBON_HEADER}
                , {NITRITE_HEADER, NITRITE_HEADER}
                , {NUM_POOLED_HEADER, NUM_POOLED_HEADER}
                , {NUM_SAMPLED_HEADER, NUM_SAMPLED_HEADER}
                , {VOLUME_HEADER, VOLUME_HEADER}
                , {COLLECTION_DATE_HEADER, COLLECTION_DATE_HEADER}
        };
    }

    public void setSelectedProjects(List<String> projectNames) {
        _selectedProjects = projectNames;
        _paginator.clear();
        _paginator.first();
    }

    private class SampleDataRetriever implements PagedDataRetriever {
        public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
            metadataService.getNumProjectSampleInfo(_selectedProjects, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    //TODO: something
                }

                public void onSuccess(Object result) {
                    _logger.debug("(((returning " + result + " num rows)))");
                    listener.onSuccess(result);
                }
            });
        }

        public void retrieveDataRows(int startIndex, int numRows, SortArgument[] sortArgs, final DataRetrievedListener listener) {
            List<TableRow> rows = new ArrayList<TableRow>();

            metadataService.getProjectSampleInfo(startIndex, startIndex + numRows, sortArgs, _selectedProjects, new AsyncCallback() {
                public void onFailure(Throwable caught) {
                    //TODO: something
                }

                public void onSuccess(Object result) {
                    List<SampleItem> items = (List<SampleItem>) result;
                    _logger.debug("(((retrieved " + items.size() + " samples)))");
                    List<TableRow> rows = new ArrayList<TableRow>();
                    for (SampleItem item : items)
                        rows.add(createTableRow(item.getProject(), item.getSample(), item.getDataFile(), item.getSite()));
                    listener.onSuccess(rows);
                }
            });
            listener.onSuccess(rows);
        }
    }

    private Widget getExportLink() {
        Image linkImage = ImageBundleFactory.getControlImageBundle().getExportImage().createImage();
        linkImage.setStyleName("exportImage");
        ActionLink exportLink = new ActionLink(EXPORT_LINK_TEXT, linkImage, new DownloadTableClickListener());

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(exportLink);
        panel.add(HtmlUtils.getHtml("&nbsp;", "pagingPanelLoadingLabelSeparator"));

        return panel;
    }

    private SortableTable createTable() {
        SortableTable table = new SortableTable();
        table.setStyleName("projectSamplesTable");
        //TODO: need a loading label.  Why doesn't paging panel show one?
        //table.addSortListener(new SortableTableBusyListener());
        table.setDefaultSortColumns(new SortableColumn[]{new SortableColumn(0, PROJECT_HEADER, SortableColumn.SORT_ASC)});

        table.addColumn(new TextColumn(PROJECT_HEADER));
        table.addColumn(new TextColumn(DATASET_HEADER));
        table.addColumn(new NumericColumn(FILE_SIZE_HEADER));
        table.addColumn(new TextColumn(HABITAT_HEADER));
        table.addColumn(new TextColumn(GEOGRAPHIC_LOCATION_HEADER));
        table.addColumn(new TextColumn(SAMPLE_LOCATION_HEADER));
        table.addColumn(new TextColumn(COUNTRY_HEADER));
        table.addColumn(new TextColumn(FILTER_SIZE_HEADER, "Filter Size (\u00B5m)"));
        table.addColumn(new NumericColumn(LAT_HEADER));
        table.addColumn(new NumericColumn(LONG_HEADER));
        table.addColumn(new NumericColumn(SAMPLE_DEPTH_HEADER, "Sample Depth"));
        table.addColumn(new NumericColumn(WATER_DEPTH_HEADER, "Water Depth"));
        table.addColumn(new NumericColumn(CHLOROPHYLL_HEADER, "Chlorophyll Density"));
        table.addColumn(new NumericColumn(OXYGEN_HEADER, "Dissolved Oxygen"));
        table.addColumn(new NumericColumn(FLOURESCENCE_HEADER, "Flourescence"));
        table.addColumn(new NumericColumn(SALINITY_HEADER, "Salinity"));
        table.addColumn(new NumericColumn(TEMP_HEADER));
        table.addColumn(new TextColumn(TRANSMISSION_HEADER, "Transmission"));
        table.addColumn(new NumericColumn(BIOMASS_HEADER));
        table.addColumn(new NumericColumn(INORG_CARBON_HEADER, "Dissolved Inorganic Carbon"));
        table.addColumn(new NumericColumn(INORG_PHOSPHATE_HEADER, "Dissolved Inorganic Phospate"));
        table.addColumn(new NumericColumn(ORG_CARBON_HEADER, "Dissolved Organic Carbon"));
        table.addColumn(new NumericColumn(NITRITE_HEADER, "Nitrate + Nitrite"));
        table.addColumn(new NumericColumn(NUM_POOLED_HEADER, "# Samples Pooled"));
        table.addColumn(new NumericColumn(NUM_SAMPLED_HEADER, "# Stations Sampled"));
        table.addColumn(new NumericColumn(VOLUME_HEADER, "Volume Filtered"));
        table.addColumn(new TextColumn(COLLECTION_DATE_HEADER, "Collection Date"));

        table.setLoading(); //TODO: doesn't work
        return table;
    }

    //private class SortableTableBusyListener implements TableSortListener {
    //    public void onBusy(Widget widget) {
    //        //_loadingLabel.setText("Sorting...");
    //        //_loadingLabel.setVisible(true);
    //    }
    //
    //    public void onBusyDone(Widget widget) {
    //        //_loadingLabel.setVisible(false);
    //    }
    //}

    private Widget getSampleDownloadWidget(String projectSymbol, DownloadableDataNode file, String displayName) {
        ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
        final Image image;

        if (ClientSecurityUtils.isAuthenticated()) {
            image = imageBundle.getDownloadImage().createImage();
            image.addMouseListener(new HoverImageSetter(image, imageBundle.getDownloadImage(), imageBundle.getDownloadHoverImage()));
            image.addClickListener(new DownloadSampleFileClickListener(projectSymbol, file));
            image.setTitle("Download");
        }
        else {
            image = imageBundle.getDownloadNotLoggedInImage().createImage();
            image.addMouseListener(new HoverImageSetter(image, imageBundle.getDownloadNotLoggedInImage(), imageBundle.getDownloadNotLoggedInHoverImage()));
            image.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    new PopupAboveLauncher(new NotLoggedInPopupPanel("You must be logged in to download this file.")).showPopup(image);
                }
            });
            image.setTitle("Download");
        }


        HorizontalPanel panel = new HorizontalPanel();
        panel.add(image);
        panel.add(new FulltextPopperUpperHTML("&nbsp;" + displayName, DATASET_NAME_SIZE));

        return panel;
    }

    private TableRow createTableRow(String projectName, Sample sample, DownloadableDataNode dataFile, final Site site) {
        TableRow row = new TableRow();

        //TODO: support multiple sites for 1 sample
        if (site == null) {
            for (int col = 0; col < _table.getNumCols(); col++)
                row.setValue(col, new TableCell("&nbsp;"));
            return row;
        }

        // Project column
        int col = -1;
        String projectSymbol = site.getProject();
        setCellValue(row, ++col, projectName, new Link(projectName, new ClickListener() {
            public void onClick(Widget sender) {
                if (_projectSymbolSelectedListener != null)
                    _projectSymbolSelectedListener.onSelect(site.getProject());
            }
        }));

        // Download link column
        if (dataFile != null) {
            String sampleDataset = dataFile.getAttribute("Description");
            int pComma = sampleDataset.indexOf(',');
            if (pComma > 0) sampleDataset = sampleDataset.substring(0, pComma);
            setCellValue(row, ++col, sampleDataset, getSampleDownloadWidget(projectSymbol, dataFile, sampleDataset)); // display name
            setCellValue(row, ++col, new AbbreviatedSize(dataFile.getSize()));
        }
        else { // if no node, null the sample, size and description cells
            setCellValue(row, ++col, sample.getSampleName());
            setCellValue(row, ++col, "n/a");
        }

        // Geography columns
        setStringCellValue(row, ++col, site.getHabitatType());
        setPopperFullTextStringCellValue(row, ++col, site.getGeographicLocation(), GEOGRAPHIC_LOCATION_VISIBLE_SIZE);
        setPopperFullTextStringCellValue(row, ++col, site.getSampleLocation(), SAMPLE_LOCATION_VISIBLE_SIZE);
        setStringCellValue(row, ++col, site.getCountry());

        if (sample.getFilterMin() != null && sample.getFilterMax() != null) {
            setCellValue(row, ++col, new NumericString(sample.getFilterMin() + " - " + sample.getFilterMax(), sample.getFilterMin().floatValue()));
        }
        else {
            setEmptyCellValue(row, ++col);
        }
        setCellValue(row, ++col, new NumericString(site.getFormattedLatitude(), site.getLatitudeDouble().floatValue()));
        setCellValue(row, ++col, new NumericString(site.getFormattedLongitude(), site.getLongitudeDouble().floatValue()));

        setNumStringCellValue(site, row, ++col, site.getSampleDepth());
        setNumStringCellValue(site, row, ++col, site.getWaterDepth());

        // Measurement columns
        setNumStringCellValue(site, row, ++col, site.getChlorophyllDensity());
        setNumStringCellValue(site, row, ++col, site.getDissolvedOxygen());
        setNumStringCellValue(site, row, ++col, site.getFluorescence());
        setNumStringCellValue(site, row, ++col, site.getSalinity());
        setNumStringCellValue(site, row, ++col, site.getTemperature());
        setNumStringCellValue(site, row, ++col, site.getTransmission());
        setNumStringCellValue(site, row, ++col, site.getBiomass());
        setNumStringCellValue(site, row, ++col, site.getDissolvedInorganicCarbon());
        setNumStringCellValue(site, row, ++col, site.getDissolvedInorganicPhospate());
        setNumStringCellValue(site, row, ++col, site.getDissolvedOrganicCarbon());
        setNumStringCellValue(site, row, ++col, site.getNitrate_plus_nitrite());
        setNumStringCellValue(site, row, ++col, site.getNumberOfSamplesPooled());
        setNumStringCellValue(site, row, ++col, site.getNumberOfStationsSampled());
        setNumStringCellValue(site, row, ++col, site.getVolume_filtered());

        if (site.getStartTime() != null) {
            String startTime = (new FormattedDate(site.getStartTime().getTime())).toString();
            String stopTime;
            if (site.getStopTime() != null)
                stopTime = (new FormattedDate(site.getStopTime().getTime())).toString();
            else
                stopTime = startTime;

            if (startTime.equals(stopTime))
                setCellValue(row, ++col, startTime);
            else
                setCellValue(row, ++col, startTime.concat(" to ").concat(stopTime));
        }
        else
            setEmptyCellValue(row, ++col);

        return row;
    }

    private void setCellValue(TableRow row, int col, Comparable value) {
        setCellValue(row, col, value, null);
    }

    private void setCellValue(TableRow row, int col, Comparable value, Widget displayWidget) {
        if (displayWidget == null) {
            row.setValue(col, new TableCell(value));
        }
        else {
            row.setValue(col, new TableCell(value, displayWidget));
        }
    }

    private void setEmptyCellValue(TableRow row, int col) {
        setCellValue(row, col, "&nbsp;");
    }

    private void setNumStringCellValue(Site site, TableRow row, int col, String value) {
        setCellValue(row, col, getNumStringValue(site, col, value));
    }

    private void setPopperFullTextStringCellValue(TableRow row, int col, String value, int visibleSize) {
        if (value != null && !value.trim().equals("")) {
            setCellValue(row, col, value, new FulltextPopperUpperHTML(value, visibleSize));
        }
        else {
            setEmptyCellValue(row, col);
        }
    }

    private void setStringCellValue(TableRow row, int col, String value) {
        setCellValue(row, col, getStringValue(value));
    }

    private Comparable getNumStringValue(Site site, int col, String value) {
        Comparable returnVal = value;
        try {
            returnVal = new NumericString(value);
        }
        catch (NumberFormatException e) {
            _logger.error("ProjectSamplesPage getNumStringValue caught exception processing site:" + site.getSiteId() + " field:\"" + _table.getCol(col + 1).getDisplayName() + "\" value:" + value + "\" :" + e.getMessage());
        }
        return returnVal;
    }

    private String getStringValue(String value) {
        if (value != null && !value.trim().equals("")) {
            return value;
        }
        else {
            return "&nbsp;";
        }
    }

//    private void createMF150Link(TableRow row, int col, Sample sample) {
//        String speciesTag = sample.getSampleName().substring(3).trim();
//        // Can't externalize this to jacs.properties because GWT can't compile SystemConfigurationProperties because of java.io.
//        String link = "https://research.venterinstitute.org/moore/SingleOrganism.do?speciesTag=" + speciesTag + "&pageAttr=pageMain";
////        String displayLink = row.getSampleName();
//        String title = sample.getSampleTitle();
//        String displayLink = title.substring(title.indexOf('-') + 1).trim();
//        setCellValue(row, col, displayLink, new ExternalLink(displayLink, link)); // display name
//    }

    private class DownloadTableClickListener implements ClickListener {
        /**
         * Gets the data from the table and post to the download servlet via XmlHttpRequest
         */
        public void onClick(Widget widget) {
            _logger.debug("posting table data...");

            SortableTable table = (SortableTable) _table.clone();
            table.setData(_pagingPanel.getPaginator().getData());
            downloadEchoService.postData(table.toCommaDelimitedValues(), new DataPostCallback());
        }

        private class DataPostCallback implements AsyncCallback {
            public void onFailure(Throwable throwable) {
                //TODO: notify user
                _logger.error("failed to post table data");
            }

            /**
             * Callback when the table data post is complete.  Calls a servlet to stream the data back to the
             * browser so it will prompt the user to save/open it
             */
            public void onSuccess(Object object) {
                _logger.debug("Successfully posted table data, now retrieving for download");

                String url = "/jacs/downloadEcho.htm" +
                        "?suggestedFilename=SampleMetadata.csv" +
                        "&suggestedContentType=text/csv";
                Window.open(url, "_self", "");
            }
        }
    }

    /**
     * Implementation of DataRetrievedListener
     */
    //TODO: don't need this anymore since data retrieved internally
    public void onSuccess(Object data) {
    }

    public void onFailure(Throwable throwable) {
        ////_loadingLabel.setVisible(false);
        //_table.setError();
        //_logger.debug("onFailure()");
        //_logger.error(throwable.getMessage());
    }

    public void onNoData() {
        //_table.setNoData();
        //_logger.debug("onNoData()");
    }

    public void clearHover() {
        _table.clearHover();
    }
}
