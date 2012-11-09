
package org.janelia.it.jacs.web.gwt.search.client.panel;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.RoundedPanel2;
import org.janelia.it.jacs.web.gwt.common.client.ui.ImageMap;
import org.janelia.it.jacs.web.gwt.common.client.ui.ImageMapArea;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.MappedImage;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageAreaModel;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
abstract public class CategorySummarySearchPanel extends Composite {

    protected static final String CANVAS_STYLENAME = "CategorySummarySearchPanel";
    protected static final String CANVASCORNER_STYLENAME = "SearchRounding";
    protected static final String BORDER_COLOR = "#CCCCCC"; // temp until can be set via CSS

    private static int uniqueChartId = 0;
    private CategorySearchDataBuilder dataBuilder;
    protected HorizontalPanel _canvas;
    private SearchIconPanel categoryIcon;
    private boolean detailLinkFlag;
    private ClickListener viewDetailListener;
    private HTML _loadingMessage;

    protected CategorySummarySearchPanel(String searchId, String searchQuery) {
        super();
        dataBuilder = createDataBuilder(searchId, searchQuery);
        dataBuilder.setParentPanel(this);
        init();
    }

    public void setCategoryIcon(SearchIconPanel categoryIcon, boolean detailLinkFlag, ClickListener viewDetailListener) {
        this.categoryIcon = categoryIcon;
        this.detailLinkFlag = detailLinkFlag;
        this.viewDetailListener = viewDetailListener;
    }

    public CategorySearchDataBuilder getDataBuilder() {
        return dataBuilder;
    }

    public void populatePanel() {
        addSearchIcon();
        addLoadingMessage();
        addSearchResultTable();
    }

    protected void deactivateLoadingMessage() {
        if (_loadingMessage != null) {
            _canvas.remove(_loadingMessage);
            _loadingMessage = null;
        }
    }

    public void populateResultCharts(List<ImageModel> resultChartImages) {
        deactivateLoadingMessage();
        if (resultChartImages != null) {
            for (Object resultChartImage : resultChartImages) {
                ImageModel resultChartModel = (ImageModel) resultChartImage;
                if (resultChartModel == null) {
                    continue;
                }

                MappedImage mappedImage = new MappedImage();
                mappedImage.setStyleName("SearchChartImage"); //  turns off blue border
                mappedImage.setUrl(resultChartModel.getURL());

                SimplePanel chartPanel = new SimplePanel();
                chartPanel.setStyleName("SearchChart");
                chartPanel.add(mappedImage);

                VerticalPanel chartAndTitlePanel = new VerticalPanel();
                chartAndTitlePanel.add(HtmlUtils.getHtml(
                        (resultChartModel.getTitle() == null) ? "&nbsp;" : resultChartModel.getTitle(),
                        "SearchChartTitle"));
                chartAndTitlePanel.add(chartPanel);

                addItem(chartAndTitlePanel);

                if (resultChartModel.getImageAreas() != null && resultChartModel.getImageAreas().size() > 0) {
                    String chartMapName = resultChartModel.getName();
                    if (chartMapName == null) {
                        chartMapName = "chart" + dataBuilder.getSearchId() + "_" + String.valueOf(++uniqueChartId);
                    }
                    ImageMap chartImageMap = new ImageMap(chartMapName);
                    chartAndTitlePanel.add(chartImageMap);
                    for (Object o : resultChartModel.getImageAreas()) {
                        ImageAreaModel areaModel = (ImageAreaModel) o;
                        ImageMapArea chartArea =
                                chartImageMap.addArea(areaModel.getShape(), areaModel.getCoordinates());
                        chartArea.setTitle(areaModel.getTips());
                    }
                    mappedImage.setMap(chartImageMap);
                }
            }
        }
    }

    abstract protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery);

    protected void addItem(Widget item) {
        _canvas.add(item);
    }

    protected void addSearchIcon() {
        Panel _leftIconPanel = createSearchIconPanel();
        _canvas.add(_leftIconPanel);
    }

    protected void addLoadingMessage() {
        if (_loadingMessage != null)
            _canvas.remove(_loadingMessage);
        _loadingMessage = new LoadingLabel(true);
        _loadingMessage.addStyleName("SearchChartLoadingMessage");
        _loadingMessage.setVisible(true);
        _canvas.add(_loadingMessage);
    }

    protected void addSearchResultTable() {
        deactivateLoadingMessage();
        Panel dataPanel = dataBuilder.createDataPanel(3, new String[]{"2", "3", "5"});
        _canvas.add(dataPanel);
        dataBuilder.populateDataPanel();
    }

    protected DockPanel createSearchIconPanel() {
        DockPanel panel = new DockPanel();
        panel.setStyleName("SearchResultsOverviewPanel");
        panel.setHeight("100%"); // doesn't work on IE

        panel.add(categoryIcon, DockPanel.NORTH);

        if (detailLinkFlag) {
            ActionLink detailLink = new ActionLink("View Details");
            detailLink.setShowBrackets(false);
            detailLink.addStyleName("SearchResultsDetailLink");

            Image image = ImageBundleFactory.getControlImageBundle().getDetailViewImage().createImage();
            image.setStyleName("SearchResultsDetailLinkImage");
            detailLink.setImage(image);

            if (viewDetailListener != null) {
                detailLink.addClickListener(viewDetailListener);
                categoryIcon.addClickListener(viewDetailListener);
            }
            panel.add(detailLink, DockPanel.SOUTH);
        }

        return panel;
    }

    protected void init() {
        _canvas = new HorizontalPanel();
        _canvas.setStyleName(CANVAS_STYLENAME);
        RoundedPanel2 _canvasWrapper = new RoundedPanel2(_canvas, RoundedPanel2.ALL, BORDER_COLOR);
        _canvasWrapper.setCornerStyleName(CANVASCORNER_STYLENAME);
        initWidget(_canvasWrapper);
    }

}
