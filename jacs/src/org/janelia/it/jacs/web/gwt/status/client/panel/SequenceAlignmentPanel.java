
package org.janelia.it.jacs.web.gwt.status.client.panel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.gwtwidgets.client.wrap.Callback;
import org.gwtwidgets.client.wrap.EffectOption;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.blast.MegablastTask;
import org.janelia.it.jacs.web.gwt.common.client.effect.SafeEffect;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHitWithSample;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.TableUtils;
import org.janelia.it.jacs.web.gwt.common.shared.data.EntityListener;

/**
 * @author Michael Press
 */
public class SequenceAlignmentPanel extends TitledBox {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.panel.SequenceAlignmentPanel");

    private VerticalPanel _alignmentPanel;
    private FlexTable _grid;
    private AlignmentDisplayPanel _alignmentDisplay;
    private HTML _hint;
    private HTML _errorMsg;
    private EntityListener _entityListener;

    public SequenceAlignmentPanel(String title, EntityListener entityListener) {
        super(title);
        setStyleName("sequenceAlignmentTitledBox");
        _entityListener = entityListener;
    }

    protected void popuplateContentPanel() {
        _hint = HtmlUtils.getHtml("Click on a sequence in the table above to view the alignment", "text");
        _errorMsg = HtmlUtils.getHtml("An error occurred retrieving alignment information.", "error");
        _grid = new FlexTable();
        _alignmentDisplay = new AlignmentDisplayPanel();

        _alignmentPanel = new VerticalPanel();
        _alignmentPanel.add(_grid);
        _alignmentPanel.add(HtmlUtils.getHtml("&nbsp;", "smalSpacer"));
        _alignmentPanel.add(_alignmentDisplay);

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
    public void setAlignment(BlastHit hit, String program) {
        swapAlignments(hit, program);
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
        _alignmentDisplay.clear();
    }

    private void populateAlignment(BlastHit hit, String program) {
        if (hit == null) {
            _logger.error("populateAlignment: got null hit");
            setError();
            return;
        }

        if (_logger.isDebugEnabled()) _logger.debug("setting alignment for hit " + hit.getBlastHitId());
        int row = 0;

        // Manually build the "Sequence: ID_GOES_HERE_AS_LINK" spanning the first 2 cells to conserve space
        HTMLPanel sequence = new HTMLPanel("<span class='prompt'>Sequence:&nbsp;&nbsp;</span><span id='sequenceWidget'></span>");
        Widget link = getSequenceIdWidget(hit);

        sequence.add(link, "sequenceWidget");
        DOM.setStyleAttribute(link.getElement(), "display", "inline");
        DOM.setStyleAttribute(sequence.getElement(), "display", "inline");

        _grid.setWidget(row, 0, sequence);
        _grid.getFlexCellFormatter().setColSpan(row, 0, 2);
        _grid.getCellFormatter().addStyleName(row, 0, "gridCell");
        _grid.getCellFormatter().addStyleName(row, 0, "gridCellTop");
        _grid.getCellFormatter().addStyleName(row, 0, "gridCellFullWidth");
        row++;

        addPromptValuePair(_grid, row++, 0, "Sequence Length", getSequenceLength(hit));
        addPromptValuePair(_grid, row++, 0, "Alignment Length", String.valueOf(hit.getLengthAlignment()));
        addPromptValuePair(_grid, row, 0, "Clear Range", getClearRange(hit));

        row = 0;
        addPromptValuePair(_grid, row++, 1, "Score", String.valueOf(hit.getBitScore()));
        addPromptValuePair(_grid, row++, 2, "Expect", String.valueOf(hit.getExpectScore()));
        addPromptValuePair(_grid, row++, 2, "Query Begin/End", getQueryBeginEnd(hit, program));
        addPromptValuePair(_grid, row, 2, "Subject Begin/End", getSubjectBeginEnd(hit, program));

        row = 0;
        addPromptValuePair(_grid, row++, 3, "Identities", getLengthAlignmentRatio(hit.getNumberIdentical(), hit));
        addPromptValuePair(_grid, row++, 4, "Positives", getLengthAlignmentRatio(hit.getNumberSimilar(), hit));
        addPromptValuePair(_grid, row++, 4, "Query Gaps", String.valueOf(hit.getQueryGaps()));
        addPromptValuePair(_grid, row, 4, "Subject Gaps", String.valueOf(hit.getSubjectGaps()));

        _alignmentDisplay.setHit(hit);
    }

    private Widget getSequenceIdWidget(final BlastHit hit) {
        if (hit.getSubjectEntity() == null || hit.getSubjectEntity().getAccession() == null)
            return HtmlUtils.getHtml("unknown", "error");
        else {
            final String accession = hit.getSubjectEntity().getAccession();
            return HtmlUtils.getHtml(accession, "text");
//            return new Link(hit.getSubjectEntity().getAccession(), new ClickListener() {
//                public void onClick(Widget widget) {
//                    _entityListener.onEntitySelected(accession, null);
//                }
//            });
        }
    }

    private String getSequenceLength(BlastHit hit) {
        if (hit.getSubjectEntity() != null && hit.getSubjectEntity().getSeqLength() != null)
            return String.valueOf(hit.getSubjectEntity().getSeqLength());
        else
            return "n/a";
    }

    private String getSampleId(BlastHit hit) {
        String sampleName = "n/a";
        if (hit.getClass().getName().endsWith("BlastHitWithSample") &&
                ((BlastHitWithSample) hit).getSample() != null) {
            sampleName = ((BlastHitWithSample) hit).getSample().getSampleName();
        }
        return sampleName;
    }

    private String getClearRange(BlastHit hit) {
        String range = "n/a";
        if (hit.getClass().getName().endsWith("BlastHitWithSample")) {
            BlastHitWithSample hitWithSample = (BlastHitWithSample) hit;
            range = "unknown";
            if (hitWithSample.getClearRangeBegin() != null && hitWithSample.getClearRangeEnd() != null)
                range = hitWithSample.getClearRangeBegin_oneResCoords().toString() +
                        " - " +
                        hitWithSample.getClearRangeEnd_oneResCoords().toString();
        }
        return range;
    }

    private String getLengthAlignmentRatio(Integer numerator, BlastHit hit) {
        int pct = (int) (100 * (numerator.floatValue() / hit.getLengthAlignment().floatValue()));
        return new StringBuffer()
                .append(numerator).append(" / ").append(hit.getLengthAlignment())
                .append(" (").append(pct).append("%)")
                .toString();
    }

    private String getQueryBeginEnd(BlastHit hit, String program) {
        return getBeginEnd(hit.getQueryBegin_oneResCoords(), hit.getQueryEnd_oneResCoords(), hit.getQueryOrientation(), program);
    }

    private String getSubjectBeginEnd(BlastHit hit, String program) {
        return getBeginEnd(hit.getSubjectBegin_oneResCoords(), hit.getSubjectEnd_oneResCoords(), hit.getSubjectOrientation(), program);
    }

    private String getBeginEnd(Integer begin, Integer end, Integer hitOrientation, String program) {
        String orientationString;
        if (hitOrientation == null)
            orientationString = "unknown";
        else if (BlastNTask.BLASTN_NAME.equals(program) || (MegablastTask.MEGABLAST_NAME.equalsIgnoreCase(program)))
            orientationString = (hitOrientation.intValue() > 0) ? "Plus" : "Minus";
        else
            orientationString = (hitOrientation.intValue() > 0) ? "+" + hitOrientation : hitOrientation.toString();

        //return new StringBuffer()
        String out = new StringBuffer()
                .append(begin).append(" - ").append(end)
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
    protected void swapAlignments(BlastHit newHit, String program) {
        if (GWT.isScript()) // normal mode
            swapAlignmentsFade(newHit, program);
        else {             // hosted mode
            swapAlignmentsImmediate(newHit, program);
        }
    }

    protected void swapAlignmentsFade(final BlastHit newHit, final String program) {
        // After the fade is complete, clear and hide the input container, and notify the caller of completion
        Callback fadeFinished = new Callback() {
            public void execute() {
                // Clear the old data, add the new data, then fade it in
                setupForNewAlignment(newHit, program);
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
    private void swapAlignmentsImmediate(BlastHit newHit, String program) {
        setupForNewAlignment(newHit, program);
        _alignmentPanel.setVisible(true);
    }

    private void setupForNewAlignment(BlastHit newHit, String program) {
        clearAlignment();
        _alignmentPanel.setVisible(true); // is at 1% opacity
        _hint.setVisible(false);
        _errorMsg.setVisible(false);
        populateAlignment(newHit, program);
    }

}
