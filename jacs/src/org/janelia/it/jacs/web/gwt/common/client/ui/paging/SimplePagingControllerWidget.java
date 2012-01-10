
package org.janelia.it.jacs.web.gwt.common.client.ui.paging;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroup;
import org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroupSelectionModel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * This class implements a panel that contains baisc pagination widgets w/out
 * having any knowledge about the actual data panel
 *
 * @author Cristian Goina
 */
public class SimplePagingControllerWidget extends HorizontalPanel {

    // List of options for changing number of rows on the page
    protected static final String[] DEFAULT_PAGESIZE_OPTIONS = new String[]{"10", "20", "50"};
    private ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
    private Image left_arrow_enabled = imageBundle.getArrowLeftEnabledImage().createImage();
    private Image left_arrow_disabled = imageBundle.getArrowLeftDisabledImage().createImage();
    private Image right_arrow_enabled = imageBundle.getArrowRightEnabledImage().createImage();
    private Image right_arrow_disabled = imageBundle.getArrowRightDisabledImage().createImage();

    public class NextControlWidget extends HorizontalPanel {
        private Image rightArrow;
        private HTML nextText;

        private NextControlWidget() {
            super();
            init();
        }

        public void setEnabled(String controlText) {
            nextText.setText(controlText);
            nextText.setStyleName("smallTextLink");
            ImageBundleFactory.getControlImageBundle().getArrowRightEnabledImage().applyTo(rightArrow);
        }

        public void setDisabled(String controlText) {
            nextText.setText(controlText);
            nextText.setStyleName("disabledSmallTextLink");
            ImageBundleFactory.getControlImageBundle().getArrowRightDisabledImage().applyTo(rightArrow);
        }

        public void init() {
            String controlText = "next " + String.valueOf(paginator.getPageSize());
            if (paginator.hasNext()) {
                nextText = HtmlUtils.getHtml(controlText, "smallTextLink");
                rightArrow = right_arrow_enabled;
            }
            else {
                nextText = HtmlUtils.getHtml(controlText, "disabledTextLink");
                rightArrow = right_arrow_disabled;
            }
            ClickListener eventListener = new ClickListener() {
                public void onClick(Widget sender) {
                    if (paginator.hasNext()) {
                        paginator.next();
                    }
                }
            };
            rightArrow.addClickListener(eventListener);
            nextText.addClickListener(eventListener);
            add(nextText);
            add(HtmlUtils.getHtml("&nbsp;", "smallText"));
            add(rightArrow);
            // this needs to be done for proper alignment
            setCellVerticalAlignment(rightArrow, VerticalPanel.ALIGN_MIDDLE);
            setCellVerticalAlignment(nextText, VerticalPanel.ALIGN_MIDDLE);
        }

    }

    public class PrevControlWidget extends HorizontalPanel {
        private Image leftArrow;
        private HTML prevText;

        private PrevControlWidget() {
            super();
            init();
        }

        public void setEnabled(String controlText) {
            prevText.setText(controlText);
            prevText.setStyleName("smallTextLink");
            ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().applyTo(leftArrow);
        }

        public void setDisabled(String controlText) {
            prevText.setText(controlText);
            prevText.setStyleName("disabledSmallTextLink");
            ImageBundleFactory.getControlImageBundle().getArrowLeftDisabledImage().applyTo(leftArrow);
        }

        public void init() {
            String controlText = "prev " + String.valueOf(paginator.getPageSize());
            boolean enable = paginator.hasPrevious();
            if (enable) {
                prevText = HtmlUtils.getHtml(controlText, "smallTextLink");
                leftArrow = left_arrow_enabled;
            }
            else {
                prevText = HtmlUtils.getHtml(controlText, "disabledTextLink");
                leftArrow = left_arrow_disabled;
            }
            ClickListener eventListener = new ClickListener() {
                public void onClick(Widget sender) {
                    if (paginator.hasPrevious()) {
                        paginator.previous();
                    }
                }
            };
            leftArrow.addClickListener(eventListener);
            prevText.addClickListener(eventListener);
            add(leftArrow);
            add(HtmlUtils.getHtml("&nbsp;", "smallText"));
            add(prevText);
            // this needs to be done for proper alignment
            setCellVerticalAlignment(leftArrow, VerticalPanel.ALIGN_MIDDLE);
            setCellVerticalAlignment(prevText, VerticalPanel.ALIGN_MIDDLE);
        }

    }

    private SimplePaginator paginator;
    private HTML recordRangeIndicator;

    private PrevControlWidget prevControlWidget;
    private NextControlWidget nextControlWidget;
    private HorizontalPanel pageSizeSelectionWidget;
    private String[] pageSizeOptions;

    public SimplePagingControllerWidget(SimplePaginator paginator) {
        this(paginator, DEFAULT_PAGESIZE_OPTIONS);
    }

    public SimplePagingControllerWidget(SimplePaginator paginator,
                                        String[] pageSizeOptions) {
        this.paginator = paginator;
        this.pageSizeOptions = pageSizeOptions;
        setDataChangeListener();
        initializePanel();
    }

    protected HorizontalPanel createPageSizeSelectionWidget() {
        setWidth("100%");
        HorizontalPanel pageSizeSelectionWidget = new HorizontalPanel();
        LinkGroupSelectionModel pageSizeSelectionModel = new LinkGroupSelectionModel();
        // set the default selection
        pageSizeSelectionModel.setSelectedValue(String.valueOf(paginator.getPageSize()));
        pageSizeSelectionModel.addSelectionListener(new SelectionListener() {
            public void onSelect(String value) {
                paginator.setPageSize(Integer.parseInt(value));
            }

            public void onUnSelect(String value) {
            }
        });
        LinkGroup pageSizeSelectionLinks = new LinkGroup(pageSizeOptions, pageSizeSelectionModel);
        pageSizeSelectionModel.addSelectionListener(pageSizeSelectionLinks);

        HTML[] pageSizeHTMLElements = pageSizeSelectionLinks.getGroupMembers();
        HTML pageSizeSelectionText = new HTML("&nbsp;Show:&nbsp;&nbsp;");
        pageSizeSelectionText.setStyleName("infoPrompt");
        pageSizeSelectionWidget.add(pageSizeSelectionText);
        for (HTML pageSizeHTMLElement : pageSizeHTMLElements) {
            pageSizeSelectionWidget.add(pageSizeHTMLElement);
            pageSizeSelectionWidget.add(HtmlUtils.getHtml("&nbsp;", "infoText")); // have to make the blanks space same font size
        }
        pageSizeSelectionWidget.addStyleName("pagingControlSizer");
        if (paginator.hasData()) {
            pageSizeSelectionWidget.setVisible(true);
        }
        else {
            pageSizeSelectionWidget.setVisible(false);
        }

        return pageSizeSelectionWidget;
    }

    protected NextControlWidget createNextControl() {
        return new NextControlWidget();
    }

    protected PrevControlWidget createPrevControl() {
        return new PrevControlWidget();
    }

    protected HorizontalPanel createPrevNextControl(PrevControlWidget prevControl, NextControlWidget nextControl) {
        HorizontalPanel prevNextControl = new HorizontalPanel();
        HTML separator = createVerticalBarSeparator();
        prevNextControl.add(prevControl);
        prevNextControl.add(separator);
        prevNextControl.add(nextControl);
        // this needs to be done for proper alignment
        prevNextControl.setCellVerticalAlignment(prevControl, VerticalPanel.ALIGN_MIDDLE);
        prevNextControl.setCellVerticalAlignment(separator, VerticalPanel.ALIGN_MIDDLE);
        prevNextControl.setCellVerticalAlignment(nextControl, VerticalPanel.ALIGN_MIDDLE);
        return prevNextControl;
    }

    protected HTML createRangeIndicator() {
        HTML rangeIndicator = new HTML();
        if (paginator.hasData()) {
            rangeIndicator.setText(paginator.getCurrentOffset() + " - " + paginator.getEndPageOffset() +
                    " of " + paginator.getTotalCount());
            rangeIndicator.setStyleName("infoText");
        }
        else {
            rangeIndicator.setText("0 - 0 of 0");
            rangeIndicator.setStyleName("infoTextDisabled");
        }
        rangeIndicator.addStyleName("pagingControlRange");
        // set the range indicator text
        return rangeIndicator;
    }

    protected HTML createVerticalBarSeparator() {
        return HtmlUtils.getHtml("&nbsp;|&nbsp;", "hint");
    }

    protected void updateControls() {
        updateRangeIndicator(recordRangeIndicator);
        updateNextControl(nextControlWidget);
        updatePrevControl(prevControlWidget);
        updatePageSizeSelectionWidget(pageSizeSelectionWidget);
    }

    protected void updateNextControl(NextControlWidget nextControl) {
        String controlText = "next " + String.valueOf(paginator.getPageSize());
        if (paginator.hasNext())
            nextControl.setEnabled(controlText);
        else
            nextControl.setDisabled(controlText);
    }

    protected void updatePageSizeSelectionWidget(HorizontalPanel pageSizeSelectionWidget) {
        if (paginator.hasData()) {
            pageSizeSelectionWidget.setVisible(true);
        }
        else {
            pageSizeSelectionWidget.setVisible(false);
        }
    }

    protected void updatePrevControl(PrevControlWidget prevControl) {
        String controlText = "prev " + String.valueOf(paginator.getPageSize());
        prevControl.prevText.setText(controlText);
        if (paginator.hasPrevious())
            prevControl.setEnabled(controlText);
        else
            prevControl.setDisabled(controlText);
    }

    /**
     * Updates the range indicator text in the rangeIndicator widget e.g. 11-20 of 56
     *
     * @param rangeIndicator range indicator widget to update
     */
    protected void updateRangeIndicator(HTML rangeIndicator) {
        if (paginator.hasData()) {
            rangeIndicator.setText(paginator.getCurrentOffset() + " - " + paginator.getEndPageOffset() +
                    " of " + paginator.getTotalCount());
            rangeIndicator.setStyleName("infoText");
        }
        else {
            rangeIndicator.setText("0 - 0 of 0");
            rangeIndicator.setStyleName("infoTextDisabled");
        }
        rangeIndicator.addStyleName("pagingControlRange");
    }

    private void initializePanel() {
        recordRangeIndicator = createRangeIndicator();
        prevControlWidget = createPrevControl();
        nextControlWidget = createNextControl();
        pageSizeSelectionWidget = createPageSizeSelectionWidget();
        Panel prevNextControl = createPrevNextControl(prevControlWidget, nextControlWidget);

        // DockPanel for all of the paging controls and info
        DockPanel pagingControlsPanel = new DockPanel();
        pagingControlsPanel.setWidth("100%");
        pagingControlsPanel.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);

        // "Show" widget left-aligned on the left
        pagingControlsPanel.add(pageSizeSelectionWidget, DockPanel.WEST);
        pagingControlsPanel.setCellHorizontalAlignment(pageSizeSelectionWidget, DockPanel.ALIGN_LEFT);
        pagingControlsPanel.setCellVerticalAlignment(pageSizeSelectionWidget, DockPanel.ALIGN_TOP);

        // Prev/Next controls centered in the center
        pagingControlsPanel.add(prevNextControl, DockPanel.CENTER);
        //pagingControlsPanel.setCellHorizontalAlignment(prevNextControl, DockPanel.ALIGN_CENTER); // strangely, aligned too far right
        pagingControlsPanel.setCellVerticalAlignment(prevNextControl, DockPanel.ALIGN_TOP);

        // Range info right-aligned on the right
        pagingControlsPanel.add(recordRangeIndicator, DockPanel.EAST);
        pagingControlsPanel.setCellHorizontalAlignment(recordRangeIndicator, DockPanel.ALIGN_RIGHT);
        pagingControlsPanel.setCellVerticalAlignment(recordRangeIndicator, DockPanel.ALIGN_TOP);

        add(pagingControlsPanel);
    }

    private void setDataChangeListener() {
        // for now we don't bother unregistering the dataChangeListener
        DataRetrievedListener dataChangeListener = new DataRetrievedListener() {
            public void onSuccess(Object data) {
                updateControls();
            }

            public void onFailure(Throwable throwable) {
                updateControls();
            }

            public void onNoData() {
                updateControls();
            }
        };
        paginator.addDataChangedListener(dataChangeListener);
    }

}
