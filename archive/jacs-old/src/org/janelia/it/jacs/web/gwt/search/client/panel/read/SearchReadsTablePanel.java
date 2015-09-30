
package org.janelia.it.jacs.web.gwt.search.client.panel.read;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.detail.client.sample.SampleReadsTableBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchResultTablePanel;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class SearchReadsTablePanel extends Composite {

    private static final String CANVAS_STYLENAME = "SearchReadsTablePanel";
    private static final String CANVASCORNER_STYLENAME = "SearchRounding";
    private static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    private HorizontalPanel _canvas;
    private PagingPanel readDataPagingPanel;
    private SampleReadsTableBuilder readsTableBuilder;
    private HTML dataPanelTitle;

    public SearchReadsTablePanel() {
        super();
        _canvas = new HorizontalPanel();
        _canvas.setStyleName(CANVAS_STYLENAME);
        RoundedPanel2 canvasWrapper = new RoundedPanel2(_canvas, RoundedPanel2.ALL, BORDER_COLOR);
        canvasWrapper.setCornerStyleName(CANVASCORNER_STYLENAME);
        initWidget(canvasWrapper);
        readsTableBuilder = new SampleReadsTableBuilder();
    }

    /**
     * The method retrieves a listener to be registered with the sample data retriever;
     * if the sample retrieve fails or returns no data the listener will display an appropriate
     * message in the read data panel
     *
     * @return
     */
    public DataRetrievedListener getSamplesRetrievedListener(final CategorySearchResultTablePanel sampleTablePanel) {
        return new DataRetrievedListener() {
            public void onSuccess(Object ignored) {
                // we don't do anything on success since the first
                // row will be selected once the table has been populated
                // also keep in mind that the data received by this
                // listener is not really usable
                sampleTablePanel.selectItem(1);
            }

            public void onFailure(Throwable throwable) {
                // if the sample retrieval failed display an error message in the read data panel
                readDataPagingPanel.displayErrorMessage("Error retrieving data" +
                        (throwable.getMessage() != null ? ": " + throwable.getMessage() : ""));
            }

            public void onNoData() {
                readDataPagingPanel.displayErrorMessage("No sample found");
            }
        };
    }

    //public void setEntityListener(SearchEntityListener entityListener) {
    //    // Have to map the SearchEN
    //    readsTableBuilder.setEntityListener(entityListener);
    //}

    public Panel createReadDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        readDataPagingPanel = readsTableBuilder.createReadDataPanel(defaultNumVisibleRows, pageLengthOptions);

        SimplePanel dataTableWrapper = new SimplePanel();
        dataTableWrapper.setStyleName("SearchDataPanel");
        dataTableWrapper.add(readDataPagingPanel);

        VerticalPanel dataPanel = new VerticalPanel();
        dataPanelTitle = HtmlUtils.getHtml("Reads for Selected Sample", "SearchDataPanelTitle");
        dataPanel.add(dataPanelTitle);
        dataPanel.add(dataTableWrapper);

        _canvas.add(dataPanel);
        return dataPanel;
    }

    public void populateSampleReads(String sampleAcc, String sampleName) {
        dataPanelTitle.setText("Reads for Sample " + sampleName);
        readsTableBuilder.populateSampleReads(sampleAcc);
    }
}
