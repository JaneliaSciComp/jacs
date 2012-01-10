
package org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.model.genomics.ScaffoldReadAlignment;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;

/**
 * @author Cristian Goina
 */
public class ScaffoldReadAlignmentPanel extends TitledBox {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.bse.scaffold.ScaffoldReadAlignmentPanel");

    private ScaffoldPanel _parentPanel;
    private VerticalPanel _alignmentPanel;
    private FlexTable _grid;
    private HTML _hint;
    private HTML _errorMsg;

    public ScaffoldReadAlignmentPanel(ScaffoldPanel parentPanel) {
        super("Scaffold Read Alignment");
        _parentPanel = parentPanel;
    }

    protected void popuplateContentPanel() {
        _hint = HtmlUtils.getHtml("Click on a sequence in the table above to view the alignment", "text");
        _errorMsg = HtmlUtils.getHtml("An error occurred retrieving alignment information.", "error");
        _grid = new FlexTable();

        _alignmentPanel = new VerticalPanel();
        _alignmentPanel.add(_grid);
        _alignmentPanel.add(HtmlUtils.getHtml("&nbsp;", "smalSpacer"));

        add(_hint);
        add(_errorMsg);
        add(_alignmentPanel);

        _hint.setVisible(true);
        _errorMsg.setVisible(false);
        _alignmentPanel.setVisible(false);

    }

    /**
     * Updates the panel with a new alignment; fades out the old alignment and fades in the new.
     */
    public void setAlignment(ScaffoldReadAlignment alignment) {
        swapAlignments(alignment);
    }

    /**
     * Sets the panel state to Error (shows the loading error)
     */
    private void setError() {
        _hint.setVisible(false);
        _alignmentPanel.setVisible(false);
        clearAlignment();
        _errorMsg.setVisible(true);
    }

    /**
     * Sets the panel state to Hint (shows the hint to click a table row)
     */
    private void setHint() {
        _errorMsg.setVisible(false);
        _alignmentPanel.setVisible(false);
        clearAlignment();

        _hint.setVisible(true);
    }

    /**
     * Clears all content (but retains internal objects for alignment display)
     */
    public void clearAlignment() {
        _grid.clear();
    }

    private void populateAlignment(ScaffoldReadAlignment alignment) {
        if (alignment == null) {
            _logger.error("populateAlignment: got null alignment");
            setError();
            return;
        }

        // Manually build the "Sequence: ID_GOES_HERE_AS_LINK" spanning the first 2 cells to conserve space
        HTMLPanel sequence = new HTMLPanel("<span class='prompt'>Sequence:&nbsp;&nbsp;</span><span id='sequenceWidget'></span>");
        Widget link = getSequenceIdWidget(alignment);

        sequence.add(link, "sequenceWidget");
        DOM.setStyleAttribute(link.getElement(), "display", "inline");
        DOM.setStyleAttribute(sequence.getElement(), "display", "inline");

        _grid.setWidget(0, 0, sequence);
        _grid.getFlexCellFormatter().setColSpan(0, 0, 2);
        _grid.getCellFormatter().addStyleName(0, 0, "gridCell");
        _grid.getCellFormatter().addStyleName(0, 0, "gridCellTop");
        _grid.getCellFormatter().addStyleName(0, 0, "gridCellFullWidth");

        addPromptValuePair(_grid, 1, 0, "Scaffold Length", getScaffoldLength(alignment));
        addPromptValuePair(_grid, 1, 2, "Scaffold Begin/End", getScaffoldBeginEnd(alignment));

        addPromptValuePair(_grid, 2, 0, "Read Length", getReadLength(alignment));
        addPromptValuePair(_grid, 2, 2, "Read Begin/End", getReadBeginEnd(alignment));

        addPromptValuePair(_grid, 3, 0, "Alignment Length", getAlignmentLength(alignment));
        addPromptValuePair(_grid, 3, 2, "Alignment Ratio", getLengthAlignmentRatio(alignment));

    }

    private Widget getSequenceIdWidget(final ScaffoldReadAlignment alignment) {
        if (alignment.getReadAcc() == null || alignment.getReadAcc().length() == 0)
            return HtmlUtils.getHtml("unknown", "error");
        else {
            final String readAcc = alignment.getReadAcc();
            return _parentPanel.getTargetAccessionWidget(_parentPanel.getAcc(), "Scaffold details", readAcc);
        }
    }

    private String getAlignmentLength(ScaffoldReadAlignment alignment) {
        if (alignment != null && alignment.getRead() != null && alignment.getRead().getSequenceLength() != null) {
            return String.valueOf(alignment.getRead().getSequenceLength());
        }
        else {
            return "n/a";
        }
    }

    private String getReadLength(ScaffoldReadAlignment alignment) {
        if (alignment != null && alignment.getRead() != null) {
            return String.valueOf(alignment.getRead().getSequenceLength());
        }
        else {
            return "n/a";
        }
    }

    private String getScaffoldLength(ScaffoldReadAlignment alignment) {
        if (alignment != null && alignment.getScaffoldLength() != null) {
            return String.valueOf(alignment.getScaffoldLength());
        }
        else {
            return "n/a";
        }
    }

    private String getLengthAlignmentRatio(ScaffoldReadAlignment alignment) {
        float numerator = 0;
        float denominator = 0;
        if (alignment.getRead() != null) {
            numerator = alignment.getRead().getClearRangeEnd().floatValue() -
                    alignment.getRead().getClearRangeBegin().floatValue();
            denominator = alignment.getScaffoldLength().floatValue();
            int pct = (int) (100 * (numerator / denominator));
            return new StringBuffer()
                    .append(numerator).append(" / ").append(alignment.getScaffoldLength())
                    .append(" (").append(pct).append("%)")
                    .toString();
        }
        else {
            return "unknown";
        }
    }

    private String getReadBeginEnd(ScaffoldReadAlignment alignment) {
        Integer begin = null;
        Integer end = null;
        Integer orientation = null;
        if (alignment.getRead() != null) {
            begin = alignment.getRead().getClearRangeBegin_oneResCoords();
            end = alignment.getRead().getClearRangeEnd_oneResCoords();
            String sequencingDir = alignment.getRead().getSequencingDirection();
            if (sequencingDir != null) {
                if (sequencingDir.equalsIgnoreCase("forward")) {
                    orientation = new Integer(1);
                }
                else if (sequencingDir.equalsIgnoreCase("reverse")) {
                    orientation = new Integer(-1);
                }
            }
        }
        return getBeginEnd(begin, end, orientation);
    }

    private String getScaffoldBeginEnd(ScaffoldReadAlignment alignment) {
        return getBeginEnd(alignment.getAlignmentBegin_oneResCoords(), alignment.getAlignmentEnd_oneResCoords(), alignment.getScaffoldOrientation());
    }

    private String getBeginEnd(Integer begin, Integer end, Integer orientation) {
        String orientationString;
        if (orientation == null) {
            orientationString = "unknown";
        }
        else {
            orientationString = (orientation.intValue() > 0) ? "Direct" : "Reverse";
        }
        String out = new StringBuffer()
                .append(begin != null ? begin.toString() : "unknown")
                .append(" - ")
                .append(end != null ? end.toString() : "unknown")
                .append(" (").append(orientationString).append(")")
                .toString();
        _logger.debug("  string = " + out);
        return out;
    }

    /**
     * Returns panel state to "hint" state
     */
    public void reset() {
        setHint();
    }

    private void addPromptValuePair(HTMLTable table, int row, int col, String prompt, String itemValue) {
        TableUtils.addTextRow(table, new RowIndex(row), col, prompt, itemValue);
        TableUtils.setLabelCellStyle(table, row, col);
        TableUtils.addCellStyle(table, row, col + 1, "text");
        TableUtils.addCellStyle(table, row, col + 1, "nowrap");
    }

    //private void addPromptValuePair(HTMLTable table, int row, int col, String prompt, Widget widget) {
    //    TableUtils.addTextRow(table, new RowIndex(row), col, prompt, widget);
    //    TableUtils.setLabelCellStyle(table, row, col);
    //}

    /**
     * Fade out the old alignemnt and fade in the now one to give a clear visual indication of a change in data.
     */
    protected void swapAlignments(ScaffoldReadAlignment newAlignment) {
        if (GWT.isScript()) // normal mode
            swapAlignmentsFade(newAlignment);
        else {             // hosted mode
            swapAlignmentsImmediate(newAlignment);
        }
    }

    protected void swapAlignmentsFade(final ScaffoldReadAlignment newAlignment) {
        // After the fade is complete, clear and hide the input container, and notify the caller of completion
        Callback fadeFinished = new Callback() {
            public void execute() {
                // Clear the old data, add the new data, then fade it in
                setupForNewAlignment(newAlignment);
                SafeEffect.fade(_alignmentPanel, new EffectOption[]{
                        new EffectOption("to", "1.0")
                        , new EffectOption("duration", "0.4")
                });
            }
        };

        // Fade out the current contents
        SafeEffect.fade(_alignmentPanel, new EffectOption[]{
                new EffectOption("to", "0.01")
                , new EffectOption("duration", "0.2")
                , new EffectOption("afterFinish", fadeFinished)
        });
    }

    /**
     * Support for GWT hosted mode where we can't cascade effects
     */
    private void swapAlignmentsImmediate(final ScaffoldReadAlignment newAlignment) {
        setupForNewAlignment(newAlignment);
        _alignmentPanel.setVisible(true);
    }

    private void setupForNewAlignment(ScaffoldReadAlignment newAlignment) {
        clearAlignment();
        _alignmentPanel.setVisible(true); // is at 1% opacity
        _hint.setVisible(false);
        _errorMsg.setVisible(false);
        populateAlignment(newAlignment);
    }

}
