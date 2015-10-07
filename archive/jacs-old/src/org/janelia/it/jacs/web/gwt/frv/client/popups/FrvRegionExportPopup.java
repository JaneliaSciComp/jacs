
package org.janelia.it.jacs.web.gwt.frv.client.popups;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.tasks.export.FrvReadExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.panel.SecondaryTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuBar;
import org.janelia.it.jacs.web.gwt.common.google.user.client.ui.MenuItem;
import org.janelia.it.jacs.web.gwt.frv.client.Frv;
import org.janelia.it.jacs.web.gwt.frv.client.FrvBounds;
import org.janelia.it.jacs.web.gwt.frv.client.FrvBoundsChangedListener;
import org.janelia.it.jacs.web.gwt.frv.client.FrvMapUtils;

import java.util.ArrayList;

/**
 * @author Michael Press
 */
public class FrvRegionExportPopup extends BasePopupPanel {
    private RecruitableJobInfo _job;
    private FrvBounds _bounds;
    private MapWidget _map;
    private FrvBoundsChangedListener _listener;
    private Grid _grid;
    private RoundedButton _exportButton;
    private TextBox _bpStartRenderer;
    private TextBox _bpEndRenderer;
    private TextBox _pctIdStartRenderer;
    private TextBox _pctIdEndRenderer;

    public static final NumberFormat BASE_PAIR_FORMAT = NumberFormat.getDecimalFormat(); // standard is ok
    public static final NumberFormat PCT_ID_FORMAT = NumberFormat.getFormat("##.##");    // only want 2 significant digits
    public static final int MIN = 0;
    public static final int MAX = 1;

    public FrvRegionExportPopup(RecruitableJobInfo job, FrvBoundsChangedListener listener, MapWidget map) {
        super("Export Selected Sequences", /*realizeNow*/ false, /*autohide*/ false);
        _job = job;
        _listener = listener;
        _map = map;
        init();
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

    public class PctIdTextBoxValidator extends BaseTextBoxValidator {
        public PctIdTextBoxValidator(TextBox textBox) {
            super(textBox);
        }

        protected boolean isValid(FrvBounds visibleBounds, FrvBounds editedBounds) {
            return isValidPctId(visibleBounds, editedBounds);
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

    public class PctIdMinMaxActionLinkClickListener extends BaseMinMaxActionLinkClickListener {
        public PctIdMinMaxActionLinkClickListener(TextBox textBox, int minOrMax) {
            super(textBox, minOrMax);
        }

        protected String format(double value) {
            return formatDouble(value);
        }

        protected double getVisibleMinOrMax(FrvBounds visibleBounds) {
            return (isMin()) ? visibleBounds.getStartPctId() : visibleBounds.getEndPctId();
        }

    }

    protected void init() {
        // Base Pair range controls
        HorizontalPanel bpPanel = new HorizontalPanel();
        bpPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

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

        // Percent ID range controls
        HorizontalPanel pctIdPanel = new HorizontalPanel();
        pctIdPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _pctIdStartRenderer = getTextBox();
        _pctIdStartRenderer.addFocusListener(new DoubleTextBoxFormatterOnLostFocus(_pctIdStartRenderer));
        _pctIdStartRenderer.addKeyboardListener(new DoubleTextBoxKeyRestrictor(_pctIdStartRenderer));
        _pctIdStartRenderer.addKeyboardListener(new PctIdTextBoxValidator(_pctIdStartRenderer));

        _pctIdEndRenderer = getTextBox();
        _pctIdEndRenderer.addFocusListener(new DoubleTextBoxFormatterOnLostFocus(_pctIdEndRenderer));
        _pctIdEndRenderer.addKeyboardListener(new DoubleTextBoxKeyRestrictor(_pctIdEndRenderer));
        _pctIdEndRenderer.addKeyboardListener(new PctIdTextBoxValidator(_pctIdEndRenderer));

        ActionLink _minPctIdActionLink = new ActionLink("min", new PctIdMinMaxActionLinkClickListener(_pctIdStartRenderer, MIN));
        ActionLink _maxPctIdActionLink = new ActionLink("max", new PctIdMinMaxActionLinkClickListener(_pctIdEndRenderer, MAX));

        pctIdPanel.add(_minPctIdActionLink);
        pctIdPanel.add(HtmlUtils.getHtml("&nbsp;", "tinySpacer"));
        pctIdPanel.add(_pctIdStartRenderer);
        pctIdPanel.add(HtmlUtils.getHtml("&nbsp;-&nbsp;", "largeText"));
        pctIdPanel.add(_pctIdEndRenderer);
        pctIdPanel.add(_maxPctIdActionLink);

        // Put the range controls in a grid
        _grid = new Grid(2, 2);
        _grid.setCellSpacing(0);
        _grid.setCellPadding(0);
        TableUtils.addWidgetRow(_grid, new RowIndex(0), "Base Pair range", bpPanel);
        TableUtils.addWidgetRow(_grid, new RowIndex(1), "Percent Identity range", pctIdPanel);
    }

    /**
     * Validate that user-entered base pairs are valid:<ol>
     * <li>start BP is within the visible BP range at this zoom level</li>
     * <li>end   BP is within the visible BP range at this zoom level</li>
     * <li>start BP < end BP</li>
     *
     * @param acceptableBounds
     * @param editedBounds
     * @return
     */
    private boolean isValidBasePair(FrvBounds acceptableBounds, FrvBounds editedBounds) {
        return isBetween(editedBounds.getStartBasePairCoord(), acceptableBounds.getStartBasePairCoord(), acceptableBounds.getEndBasePairCoord()) &&
                isBetween(editedBounds.getEndBasePairCoord(), acceptableBounds.getStartBasePairCoord(), acceptableBounds.getEndBasePairCoord()) &&
                editedBounds.getStartBasePairCoord() < editedBounds.getEndBasePairCoord();
    }

    /**
     * Validate that user-entered pctId's are valid:<ol>
     * <li>start PI is within the visible PI range at this zoom level</li>
     * <li>end   PI is within the visible PI range at this zoom level</li>
     * <li>start PI < end PI</li>
     *
     * @param acceptableBounds
     * @param editedBounds
     * @return
     */
    private boolean isValidPctId(FrvBounds acceptableBounds, FrvBounds editedBounds) {
        return isBetween(editedBounds.getStartPctId(), acceptableBounds.getStartPctId(), acceptableBounds.getEndPctId()) &&
                isBetween(editedBounds.getEndPctId(), acceptableBounds.getStartPctId(), acceptableBounds.getEndPctId()) &&
                editedBounds.getStartPctId() < editedBounds.getEndPctId();
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
     * @param textBox
     * @param expr
     * @return
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

    /**
     * Initialization - instantiates all widgets, and may result in service calls by subclasses to populate content.
     */
    public void realize() {
        if (!isRealized()) {
            if (_panel == null) // Add our panel to the popup (it only accepts 1 panel)
                createPanel();

            if (_title != null)
                _panel.add(createTitlePanel(_title));

            populateContent();

            Widget exportWidget = createFrvRegionExportWidget();

            _panel.add(exportWidget);

            setRealized(true);
        }
    }

    protected ButtonSet createButtons() {
        _exportButton = new RoundedButton("Export", new ClickListener() {
            public void onClick(Widget sender) {
                Frv.trackActivity("FRV.ExportRegion", _job);
                doSeqExport();
                hide();
            }
        });
        RoundedButton cancelButton = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        });
        return new ButtonSet(new RoundedButton[]{_exportButton, cancelButton});
    }

    private void doSeqExport() {
        try {
            updateBoundsFromPopup(); // pull any user changes from popup
            // Accession id list means nothing.  The bounds determine the reads involved
            ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
            attributeList.add(new SortArgument("defline"));
            attributeList.add(new SortArgument("sequence"));
            FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                    _job.getJobId(), _bounds.getStartBasePairCoord(), _bounds.getEndBasePairCoord(),
                    (int) _bounds.getStartPctId(), (int) _bounds.getEndPctId(),
                    ExportWriterConstants.EXPORT_TYPE_FASTA,
                    null, attributeList);
            new AsyncExportTaskController(exportTask).start();
        }
        catch (NumberFormatException e) {
            new PopupAboveLauncher(new ErrorPopupPanel("Invalid number: " + e.getMessage(), false)).showPopup(_exportButton);
        }
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
     * @throws NumberFormatException
     * @throw NumberFormatException if invalid number;  message is the invalid number as String
     */
    public void updateBoundsFromPopup() throws NumberFormatException {
        _bounds.setStartBasePairCoord(getFormattedTextBoxValueAsLong(_bpStartRenderer));
        _bounds.setEndBasePairCoord(getFormattedTextBoxValueAsLong(_bpEndRenderer));
        _bounds.setStartPctId(getFormattedTextBoxValueAsDouble(_pctIdStartRenderer));
        _bounds.setEndPctId(getFormattedTextBoxValueAsDouble(_pctIdEndRenderer));
    }

    /**
     * Formats a long for display (adds commas)
     *
     * @param value
     * @return
     */
    private String formatLong(long value) {
        return BASE_PAIR_FORMAT.format(value);
    }

    /**
     * Formats a double for display (adds commas, restricts to 2 significant digits, etc.)
     *
     * @param value
     * @return
     */
    private String formatDouble(double value) {
        return PCT_ID_FORMAT.format(value);
    }

    /**
     * Convenience Function to convert formatted TextBox value to long or return the invalid number
     *
     * @param textBox
     * @return
     * @throw NumberFormatException if invalid number;  message is the invalid number as String
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
     * Convenience Function to convert formatted TextBox value to double or return the invalid number
     *
     * @param textBox
     * @return
     * @throw NumberFormatException if invalid number;  message is the invalid number as String
     */
    private double getFormattedTextBoxValueAsDouble(TextBox textBox) {
        try {
            return NumberFormat.getDecimalFormat().parse(textBox.getText());
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
        _pctIdStartRenderer.setText(formatDouble(_bounds.getStartPctId()));
        _pctIdEndRenderer.setText(formatDouble(_bounds.getEndPctId()));

        // Update the min/max popups
        //_minBasePairActionLink.setPopupText(formatLong(visibleBounds.getStartBasePairCoord()));
        //_maxBasePairActionLink.setPopupText(formatLong(visibleBounds.getEndBasePairCoord()));
        //_minPctIdActionLink.setPopupText(formatDouble(visibleBounds.getStartPctId()));
        //_maxPctIdActionLink.setPopupText(formatDouble(visibleBounds.getEndPctId()));

        // update the colors
        colorTextBox(_bpStartRenderer, true);
        colorTextBox(_bpEndRenderer, true);
        colorTextBox(_pctIdStartRenderer, true);
        colorTextBox(_pctIdEndRenderer, true);
    }

    /**
     * Restrict the provided bounds to the currently visible area (protects against rubber-band drags that extend outside the map
     *
     * @param constrainedBounds
     */
    private void constrainBounds(FrvBounds constrainedBounds) {
        if (_bounds.getStartBasePairCoord() < constrainedBounds.getStartBasePairCoord())
            _bounds.setStartBasePairCoord(constrainedBounds.getStartBasePairCoord());
        if (_bounds.getEndBasePairCoord() > constrainedBounds.getEndBasePairCoord())
            _bounds.setEndBasePairCoord(constrainedBounds.getEndBasePairCoord());

        if (_bounds.getStartPctId() < constrainedBounds.getStartPctId())
            _bounds.setStartPctId(constrainedBounds.getStartPctId());
        if (_bounds.getEndPctId() > constrainedBounds.getEndPctId())
            _bounds.setEndPctId(constrainedBounds.getEndPctId());
    }


    private Widget createFrvRegionExportWidget() {
        MenuBar menu = new MenuBar();
        menu.setAutoOpen(false);

        MenuBar dropDown = new MenuBar(true);

        dropDown.addItem("Export Reads as FASTA", true, new Command() {
            public void execute() {
                try {
                    updateBoundsFromPopup(); // pull any user changes from popup
                    // Accession id list means nothing.  The bounds determine the reads involved
                    ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
                    attributeList.add(new SortArgument("defline"));
                    attributeList.add(new SortArgument("sequence"));
                    FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                            _job.getJobId(), _bounds.getStartBasePairCoord(), _bounds.getEndBasePairCoord(),
                            (int) _bounds.getStartPctId(), (int) _bounds.getEndPctId(),
                            ExportWriterConstants.EXPORT_TYPE_FASTA,
                            null, attributeList);
                    new AsyncExportTaskController(exportTask).start();
                }
                catch (NumberFormatException e) {
                    new PopupAboveLauncher(new ErrorPopupPanel("Invalid number: " + e.getMessage(), false)).showPopup(_exportButton);
                }
            }
        });

        dropDown.addItem("Export Reads as CSV", true, new Command() {
            public void execute() {
                try {
                    updateBoundsFromPopup(); // pull any user changes from popup
                    // Accession id list means nothing.  The bounds determine the reads involved
                    ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
                    attributeList.add(new SortArgument("defline"));
                    attributeList.add(new SortArgument("sequence"));
                    FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                            _job.getJobId(), _bounds.getStartBasePairCoord(), _bounds.getEndBasePairCoord(),
                            (int) _bounds.getStartPctId(), (int) _bounds.getEndPctId(),
                            ExportWriterConstants.EXPORT_TYPE_CSV,
                            null, attributeList);
                    new AsyncExportTaskController(exportTask).start();
                }
                catch (NumberFormatException e) {
                    new PopupAboveLauncher(new ErrorPopupPanel("Invalid number: " + e.getMessage(), false)).showPopup(_exportButton);
                }
            }
        });

        dropDown.addItem("Export Reads as Excel", true, new Command() {
            public void execute() {
                try {
                    updateBoundsFromPopup(); // pull any user changes from popup
                    // Accession id list means nothing.  The bounds determine the reads involved
                    ArrayList<SortArgument> attributeList = new ArrayList<SortArgument>();
                    attributeList.add(new SortArgument("defline"));
                    attributeList.add(new SortArgument("sequence"));
                    FrvReadExportTask exportTask = new FrvReadExportTask(_job.getRecruitmentResultsFileNodeId(),
                            _job.getJobId(), _bounds.getStartBasePairCoord(), _bounds.getEndBasePairCoord(),
                            (int) _bounds.getStartPctId(), (int) _bounds.getEndPctId(),
                            ExportWriterConstants.EXPORT_TYPE_EXCEL,
                            null, attributeList);
                    new AsyncExportTaskController(exportTask).start();
                }
                catch (NumberFormatException e) {
                    new PopupAboveLauncher(new ErrorPopupPanel("Invalid number: " + e.getMessage(), false)).showPopup(_exportButton);
                }
            }
        });

        MenuItem export = new MenuItem("Export&nbsp;" + ImageBundleFactory.getControlImageBundle().getArrowDownEnabledImage().getHTML(),
                /* asHTML*/ true, dropDown);
        export.setStyleName("topLevelMenuItem");
        menu.addItem(export);

        return menu;
    }

}
