
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.processors.recruitment.AnnotationTableData;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.ImageColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.NumericString;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.LocalPaginator;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.frv.client.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Press
 */
public class FrvAnnotationTablePopup extends BasePopupPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.frv.client.popups.FrvRegionExportPopup");

    private RecruitableJobInfo _job;
    private FrvBounds _bounds;
    private MapWidget _map;
    private FrvBoundsChangedListener _listener;
    private Grid _grid;
    private TextBox _bpStartRenderer;
    private TextBox _bpEndRenderer;
    private SortableTable _table;
    protected LocalPaginator _paginator;
    protected PagingPanel _pagingPanel;
    protected Label annotLabel = new Label("Annotations:");

    public static final NumberFormat BASE_PAIR_FORMAT = NumberFormat.getDecimalFormat(); // standard is ok
    public static final NumberFormat PCT_ID_FORMAT = NumberFormat.getFormat("##.##");    // only want 2 significant digits
    public static final int MIN = 0;
    public static final int MAX = 1;

    protected static RecruitmentServiceAsync _recruitmentService = (RecruitmentServiceAsync) GWT.create(RecruitmentService.class);

    static {
        ((ServiceDefTarget) _recruitmentService).setServiceEntryPoint("recruitment.srv");
    }

    public FrvAnnotationTablePopup(RecruitableJobInfo job, FrvBoundsChangedListener listener, MapWidget map) {
        super("Select Annotation Range", /*realizeNow*/ false, /*autohide*/ false);
        _job = job;
        _listener = listener;
        _map = map;
        init();
    }

    public void clearDataAndDisplay() {
        if (null != _table) {
            _table.clearDataAndDisplay();
            _pagingPanel.clear();
        }
    }

    /**
     * Reformats Long TextBox text on lost focus (can't reformat on keypress or cursor moves)
     */
    public class LongTextBoxFormatterOnLostFocus extends FocusListenerAdapter {
        private TextBox _textBox;

        public LongTextBoxFormatterOnLostFocus(TextBox textBox) {
            _textBox = textBox;
        }

        public void onLostFocus(Widget sender) {
            _textBox.setText(formatLong(getFormattedTextBoxValueAsLong(_textBox)));
        }
    }

    /**
     * Reformats Double TextBox text on lost focus (can't reformat on keypress or cursor moves)
     */
    public class DoubleTextBoxFormatterOnLostFocus extends FocusListenerAdapter {
        private TextBox _textBox;

        public DoubleTextBoxFormatterOnLostFocus(TextBox textBox) {
            _textBox = textBox;
        }

        // Reformat the text on lost focus (can't reformat on keypress or cursor moves)
        public void onLostFocus(Widget sender) {
            _textBox.setText(formatDouble(getFormattedTextBoxValueAsLong(_textBox)));
        }
    }

    /**
     * On keypress, set the textbox background to indicate valid/invalid value; if valid, notify listener of the change
     */
    public abstract class BaseTextBoxValidator extends KeyboardListenerAdapter {
        private TextBox _textBox;

        public BaseTextBoxValidator(TextBox textBox) {
            _textBox = textBox;
        }

        public void onKeyUp(Widget sender, char keyCode, int modifiers) {
            FrvBounds visibleBounds = FrvMapUtils.convertLatLngToFrvBounds(_job, _map);
            FrvBounds editedBounds = getEditedBounds();
            if (colorTextBox(_textBox, isValid(visibleBounds, editedBounds)))
                _listener.onBoundsChange(getEditedBounds());
        }

        protected abstract boolean isValid(FrvBounds visibleBounds, FrvBounds editedBounds);
    }

    public class BasePairTextBoxValidator extends BaseTextBoxValidator {
        public BasePairTextBoxValidator(TextBox textBox) {
            super(textBox);
        }

        protected boolean isValid(FrvBounds visibleBounds, FrvBounds editedBounds) {
            return isValidBasePair(visibleBounds, editedBounds);
        }
    }

    /**
     * Sets the text box to the min or max base pair number, and notifies the listener of the change
     */
    public abstract class BaseMinMaxActionLinkClickListener implements ClickListener {
        private TextBox _textBox;
        private int _minOrMax;

        public BaseMinMaxActionLinkClickListener(TextBox textBox, int minOrMax) {
            _textBox = textBox;
            _minOrMax = minOrMax;
        }

        public void onClick(Widget sender) {
            FrvBounds visibleBounds = FrvMapUtils.convertLatLngToFrvBounds(_job, _map);

            colorTextBox(_textBox, /*force*/ true);                          // update the color
            _textBox.setText(format(getVisibleMinOrMax(visibleBounds))); // set the text box to the min/max
            _listener.onBoundsChange(getEditedBounds());                     // notify the listener
        }

        public boolean isMin() {
            return _minOrMax == MIN;
        }

        abstract protected String format(double value);

        abstract protected double getVisibleMinOrMax(FrvBounds visibleBounds);
    }

    public class BasePairMinMaxActionLinkClickListener extends BaseMinMaxActionLinkClickListener {
        public BasePairMinMaxActionLinkClickListener(TextBox textBox, int minOrMax) {
            super(textBox, minOrMax);
        }

        protected String format(double value) {
            return formatLong((long) value);
        }

        protected double getVisibleMinOrMax(FrvBounds visibleBounds) {
            return (double) ((isMin()) ? visibleBounds.getStartBasePairCoord() : visibleBounds.getEndBasePairCoord());
        }
    }

    protected void init() {
        annotLabel.setStyleName("frvFilterInfo");

        // Base Pair range controls
        HorizontalPanel bpPanel = new HorizontalPanel();
        bpPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);

        _bpStartRenderer = getTextBox();
        _bpStartRenderer.addFocusListener(new LongTextBoxFormatterOnLostFocus(_bpStartRenderer));
        _bpStartRenderer.addKeyboardListener(new LongTextBoxKeyRestrictor(_bpStartRenderer));
        _bpStartRenderer.addKeyboardListener(new BasePairTextBoxValidator(_bpStartRenderer));

        _bpEndRenderer = getTextBox();
        _bpEndRenderer.addFocusListener(new LongTextBoxFormatterOnLostFocus(_bpEndRenderer));
        _bpEndRenderer.addKeyboardListener(new LongTextBoxKeyRestrictor(_bpEndRenderer));
        _bpEndRenderer.addKeyboardListener(new BasePairTextBoxValidator(_bpEndRenderer));

        ActionLink _minBasePairActionLink = new ActionLink("min", new BasePairMinMaxActionLinkClickListener(_bpStartRenderer, MIN));
        ActionLink _maxBasePairActionLink = new ActionLink("max", new BasePairMinMaxActionLinkClickListener(_bpEndRenderer, MAX));

        bpPanel.add(_minBasePairActionLink);
        bpPanel.add(HtmlUtils.getHtml("&nbsp;", "tinySpacer"));
        bpPanel.add(_bpStartRenderer);
        bpPanel.add(HtmlUtils.getHtml("&nbsp;-&nbsp;", "largeText"));
        bpPanel.add(_bpEndRenderer);
        bpPanel.add(_maxBasePairActionLink);

        // Put the range controls in a grid
        _grid = new Grid(2, 2);
        _grid.setCellSpacing(0);
        _grid.setCellPadding(0);
        TableUtils.addWidgetRow(_grid, new RowIndex(0), "Base Pair range", bpPanel);
    }

    protected Widget getTable() {
        ImageColumn strandColumn = new ImageColumn("Strand", true);
        strandColumn.setStyle("tableColumnCentered");
        _table = new SortableTable();
        _table.setWidth("100%");
        _table.addColumn(new NumericColumn("Begin"));
        _table.addColumn(new NumericColumn("End"));
        _table.addColumn(strandColumn);
        _table.addColumn(new NumericColumn("Length"));
        _table.addColumn(new TextColumn("GI Number", true, true));
        _table.addColumn(new TextColumn("Protein ID", true, true));
        _table.addColumn(new TextColumn("Product", true, true));
        _table.setHighlightSelect(true);
        _table.setDefaultSortColumns(new SortableColumn[]{
                new SortableColumn(0, "Begin", SortableColumn.SORT_ASC)
        });
        _table.setData(new ArrayList());
        _paginator = new LocalPaginator(_table, new LoadingLabel("Loading...", true), "FrvAnnotationTable");
        _pagingPanel = new PagingPanel(_table, "FrvAnnotationTable", 20);
        _pagingPanel.setStyleName("querySequenceChooserPopup");
        _pagingPanel.setNoDataMessage("No query sequences found.");

        return _pagingPanel;
    }

    /**
     * Validate that user-entered base pairs are valid:<ol>
     * <li>start BP is within the visible BP range at this zoom level</li>
     * <li>end   BP is within the visible BP range at this zoom level</li>
     * <li>start BP < end BP</li>
     *
     * @param acceptableBounds - acceptable range
     * @param editedBounds     - bounds the user has edited
     * @return if the base pair is valid
     */
    private boolean isValidBasePair(FrvBounds acceptableBounds, FrvBounds editedBounds) {
        return isBetween(editedBounds.getStartBasePairCoord(), acceptableBounds.getStartBasePairCoord(), acceptableBounds.getEndBasePairCoord()) &&
                isBetween(editedBounds.getEndBasePairCoord(), acceptableBounds.getStartBasePairCoord(), acceptableBounds.getEndBasePairCoord()) &&
                editedBounds.getStartBasePairCoord() < editedBounds.getEndBasePairCoord();
    }

    private boolean isBetween(double value, double start, double end) {
        return (value >= start && value <= end);
    }

    abstract public class BaseTextBoxKeyRestrictor extends KeyboardListenerAdapter {
        TextBox _textBox;

        public BaseTextBoxKeyRestrictor(TextBox textBox) {
            _textBox = textBox;
        }

        public void onKeyPress(Widget widget, char c, int i) {
            if (!isValidKey(c))
                _textBox.cancelKey();
        }

        abstract protected boolean isValidKey(char c);
    }

    public class LongTextBoxKeyRestrictor extends BaseTextBoxKeyRestrictor {
        public LongTextBoxKeyRestrictor(TextBox textBox) {
            super(textBox);
        }

        protected boolean isValidKey(char c) {
            return Character.isDigit(c) || c == KEY_BACKSPACE || c == KEY_DELETE || c == KEY_TAB;
        }
    }

    public class DoubleTextBoxKeyRestrictor extends BaseTextBoxKeyRestrictor {
        public DoubleTextBoxKeyRestrictor(TextBox textBox) {
            super(textBox);
        }

        protected boolean isValidKey(char c) {
            return Character.isDigit(c) || c == KEY_BACKSPACE || c == KEY_DELETE || c == KEY_TAB;
        }
    }

    /**
     * Sets the text box background color to white if the expr indicates it's valid, otherwise to red and disables
     * the export links.
     *
     * @param textBox textbox to color
     * @param expr    boolean to set the color white if true
     * @return the boolean used
     */
    private boolean colorTextBox(TextBox textBox, boolean expr) {
        textBox.setStyleName((expr) ? "textBox" : "textBoxError");
        return expr;
    }

    /**
     * This isn't called until the popup is rendered, at which point the job and bounds are known
     */
    protected void populateContent() {
        //ClearTitledBox controlBox = new ClearTitledBox("Region", /*action links*/false, "whiteTitleLabel");
        SecondaryTitledBox controlBox = new SecondaryTitledBox("Region", /*action links*/false);
        controlBox.add(_grid);
        String tmpFilter = _job.getAnnotationFilterString();
        if (null == tmpFilter || "".equals(tmpFilter)) {
            tmpFilter = "(None)";
        }
        HorizontalPanel panel = new HorizontalPanel();
        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        panel.add(HtmlUtils.getHtml("Filter:&nbsp;&nbsp;", "frvFilterInfo"));
        panel.add(HtmlUtils.getHtml(tmpFilter, "text"));
        controlBox.add(panel);
        controlBox.add(annotLabel);
        controlBox.add(getTable());
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        add(controlBox);
        add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
    }

    private TextBox getTextBox() {
        TextBox textBox = new TextBox();
        textBox.setStyleName("textBox");
        textBox.setVisibleLength(9);
        textBox.setTextAlignment(TextBoxBase.ALIGN_RIGHT);
        return textBox;
    }

    protected ButtonSet createButtons() {
        RoundedButton _submitButton = new RoundedButton("Refresh", new ClickListener() {
            public void onClick(Widget sender) {
                getData();
            }
        });
        RoundedButton cancelButton = new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        });
        return new ButtonSet(new RoundedButton[]{_submitButton, cancelButton});
    }

    public void getData() {
        Frv.trackActivity("FRV.AnnotationRange", _job);
        _table.setLoading();
        _recruitmentService.getAnnotationInfoForRange(_job.getRecruitableNodeId(), getFormattedTextBoxValueAsLong(_bpStartRenderer),
                getFormattedTextBoxValueAsLong(_bpEndRenderer), _job.getAnnotationFilterString(), new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        _logger.error("Failed to retrieve RecruitmentViewer.getAnnotationInfoForRange");
                        _table.setError();
                    }

                    public void onSuccess(Object o) {
                        //Window.alert(o.toString());
                        _logger.info("Got annotation entries for user");
                        List annotationList = (List) o;
                        ArrayList<TableRow> tableRows = new ArrayList<TableRow>();
                        if (annotationList != null && annotationList.size() > 0) {
                            _table.unsetLoading();
                            for (Object anAnnotationList : annotationList) {
                                AnnotationTableData data = (AnnotationTableData) anAnnotationList;
                                TableRow tableRow = new TableRow();
                                String tmpProteinId = data.getProteinId();
                                HorizontalPanel tmpPanel = new HorizontalPanel();
                                if (null != tmpProteinId && !"".equalsIgnoreCase(tmpProteinId)) {
                                    ExternalLink tmpLink = new ExternalLink(tmpProteinId, "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?cmd=Retrieve&id=" + tmpProteinId);
                                    tmpPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
                                    tmpPanel.setWidth("100px");
                                    tmpPanel.add(tmpLink);
                                }
                                tableRow.setValue(0, new TableCell(new NumericString(data.getBegin())));
                                tableRow.setValue(1, new TableCell(new NumericString(data.getEnd())));
                                SimplePanel simplePanel = new SimplePanel();
                                if ("+".equals(data.getStrand())) {
                                    simplePanel.setStyleName("frvForwardStrandBackground");
                                    simplePanel.add(ImageBundleFactory.getControlImageBundle().getArrowForwardImage().createImage());
                                }
                                else if ("-".equals(data.getStrand())) {
                                    simplePanel.setStyleName("frvReverseStrandBackground");
                                    simplePanel.add(ImageBundleFactory.getControlImageBundle().getArrowReverseImage().createImage());
                                }
                                tableRow.setValue(2, new TableCell(data.getStrand(), simplePanel));
                                tableRow.setValue(3, new TableCell(new NumericString(data.getLength())));
                                tableRow.setValue(4, new TableCell(data.getDbXRef()));
                                tableRow.setValue(5, new TableCell(tmpProteinId, tmpPanel));
                                tableRow.setValue(6, new TableCell(data.getProduct()));
                                tableRows.add(tableRow);
                            }
                            _table.setDataRowsVisible(false);
                            _table.setData(tableRows);
                            _table.sort();
                            _table.setDataRowsVisible(true);
                        }
                        else {
                            _table.setNoData("No annotations completely within in the range provided.");
                        }
//                        for (Iterator it = annotationList.iterator(); it.hasNext();) {
//                            Window.alert("Recieved: "+it.next());
//                        }
                    }
                });
    }

    public void setBounds(FrvBounds bounds) {
        _bounds = bounds;
        updatePopupFromBounds();
    }

    public FrvBounds getEditedBounds() {
        updateBoundsFromPopup();
        return _bounds;
    }

    /**
     * Pushes any user changes into the FrvBounds bean
     *
     * @throws NumberFormatException if invalid number;  message is the invalid number as String
     */
    public void updateBoundsFromPopup() throws NumberFormatException {
        _bounds.setStartBasePairCoord(getFormattedTextBoxValueAsLong(_bpStartRenderer));
        _bounds.setEndBasePairCoord(getFormattedTextBoxValueAsLong(_bpEndRenderer));
    }

    /**
     * Formats a long for display (adds commas)
     *
     * @param value long to format
     * @return string representation of the long
     */
    private String formatLong(long value) {
        return BASE_PAIR_FORMAT.format(value);
    }

    /**
     * Formats a double for display (adds commas, restricts to 2 significant digits, etc.)
     *
     * @param value double to format
     * @return string representation of the double
     */
    private String formatDouble(double value) {
        return PCT_ID_FORMAT.format(value);
    }

    /**
     * Convenience Function to convert formatted TextBox value to long or return the invalid number
     *
     * @param textBox textbox to get string from
     * @return long which represents the textbox string
     * @throws NumberFormatException if invalid number;  message is the invalid number as String
     */
    private long getFormattedTextBoxValueAsLong(TextBox textBox) {
        try {
            return (long) NumberFormat.getDecimalFormat().parse(textBox.getText());
        }
        catch (NumberFormatException e) {
            throw new NumberFormatException(textBox.getText());
        }
    }

    /**
     * Pushes any user changes into the FrvBounds bean
     */
    private void updatePopupFromBounds() {
        // Constrain the bounds to the visible area
        FrvBounds visibleBounds = FrvMapUtils.convertLatLngToFrvBounds(_job, _map);
        constrainBounds(visibleBounds);

        // Update the text box values
        _bpStartRenderer.setText(formatLong(_bounds.getStartBasePairCoord()));
        _bpEndRenderer.setText(formatLong(_bounds.getEndBasePairCoord()));

        // Update the min/max popups
        //_minBasePairActionLink.setPopupText(formatLong(visibleBounds.getStartBasePairCoord()));
        //_maxBasePairActionLink.setPopupText(formatLong(visibleBounds.getEndBasePairCoord()));

        // update the colors
        colorTextBox(_bpStartRenderer, true);
        colorTextBox(_bpEndRenderer, true);
    }

    /**
     * Restrict the provided bounds to the currently visible area (protects against rubber-band drags that extend outside the map
     *
     * @param constrainedBounds the bounds attempted
     */
    private void constrainBounds(FrvBounds constrainedBounds) {
        if (_bounds.getStartBasePairCoord() < constrainedBounds.getStartBasePairCoord())
            _bounds.setStartBasePairCoord(constrainedBounds.getStartBasePairCoord());
        if (_bounds.getEndBasePairCoord() > constrainedBounds.getEndBasePairCoord())
            _bounds.setEndBasePairCoord(constrainedBounds.getEndBasePairCoord());
    }
}