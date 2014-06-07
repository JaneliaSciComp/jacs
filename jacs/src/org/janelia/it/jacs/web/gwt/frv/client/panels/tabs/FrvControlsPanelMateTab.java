
package org.janelia.it.jacs.web.gwt.frv.client.panels.tabs;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.ClearTitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Michael Press
 */
public class FrvControlsPanelMateTab extends FrvControlsPanelBaseTab {

    private VerticalPanel _verticalPanel;
    public static final String LABEL_NAME = "Mate Pair Filters";
    // Why not put all these in a list and manipulate in a more sophisticated way?
    private CheckBox goodBoxLeft = new CheckBox("");
    private CheckBox goodBoxRight = new CheckBox("");
    private CheckBox tooCloseBoxLeft = new CheckBox("");
    private CheckBox tooCloseBoxRight = new CheckBox("");
    private CheckBox noMateBoxLeft = new CheckBox("");
    private CheckBox noMateBoxRight = new CheckBox("");
    private CheckBox tooFarBoxLeft = new CheckBox("");
    private CheckBox tooFarBoxRight = new CheckBox("");
    private CheckBox antiOrientedBoxLeft = new CheckBox("");
    private CheckBox antiOrientedBoxRight = new CheckBox("");
    private CheckBox normalOrientedBoxLeft = new CheckBox("");
    private CheckBox normalOrientedBoxRight = new CheckBox("");
    private CheckBox outieOrientedBoxLeft = new CheckBox("");
    private CheckBox outieOrientedBoxRight = new CheckBox("");
    private CheckBox missingMateBoxLeft = new CheckBox("");
    private CheckBox missingMateBoxRight = new CheckBox("");
    private CheckBox useSpanPoint = new CheckBox("Show reads whose mates span location:");
    private TextBox spanLocation = new TextBox();
    private HTML lengthLabel = new HTML();


    public FrvControlsPanelMateTab(JobSelectionListener jobSelectionListener) {
        super(jobSelectionListener);
    }

    public void setJob(JobInfo job) {
        super.setJob(job);
        RecruitableJobInfo recruitJob = (RecruitableJobInfo) job;
        String mateInfo = recruitJob.getMateInfo();
        goodBoxLeft.setValue(mateInfo.charAt(0) == '1');
        goodBoxRight.setValue(mateInfo.charAt(1) == '1');
        tooCloseBoxLeft.setValue(mateInfo.charAt(2) == '1');
        tooCloseBoxRight.setValue(mateInfo.charAt(3) == '1');
        noMateBoxLeft.setValue(mateInfo.charAt(4) == '1');
        noMateBoxRight.setValue(mateInfo.charAt(5) == '1');
        tooFarBoxLeft.setValue(mateInfo.charAt(6) == '1');
        tooFarBoxRight.setValue(mateInfo.charAt(7) == '1');
        antiOrientedBoxLeft.setValue(mateInfo.charAt(8) == '1');
        antiOrientedBoxRight.setValue(mateInfo.charAt(9) == '1');
        normalOrientedBoxLeft.setValue(mateInfo.charAt(10) == '1');
        normalOrientedBoxRight.setValue(mateInfo.charAt(11) == '1');
        outieOrientedBoxLeft.setValue(mateInfo.charAt(12) == '1');
        outieOrientedBoxRight.setValue(mateInfo.charAt(13) == '1');
        missingMateBoxLeft.setValue(mateInfo.charAt(14) == '1');
        missingMateBoxRight.setValue(mateInfo.charAt(15) == '1');

        ClearTitledBox categoryPanel = new ClearTitledBox("Select Mates To Display", true);
        categoryPanel.setLabelStyleName("clearTitledBoxLabel");
        categoryPanel.setLabelPanelStyleName("tabColorBackground");
        categoryPanel.setActionLinkBackgroundStyleName("tabColorBackground");
        categoryPanel.setWidth("100%");
        categoryPanel.removeActionLinks();

        categoryPanel.addActionLink(new ActionLink("select all",
                ImageBundleFactory.getControlImageBundle().getSelectAllImage().createImage(), new ClickListener() {
                    public void onClick(Widget sender) {
                        goodBoxLeft.setValue(true);
                        goodBoxRight.setValue(true);
                        tooCloseBoxLeft.setValue(true);
                        tooCloseBoxRight.setValue(true);
                        noMateBoxLeft.setValue(true);
                        noMateBoxRight.setValue(true);
                        tooFarBoxLeft.setValue(true);
                        tooFarBoxRight.setValue(true);
                        antiOrientedBoxLeft.setValue(true);
                        antiOrientedBoxRight.setValue(true);
                        normalOrientedBoxLeft.setValue(true);
                        normalOrientedBoxRight.setValue(true);
                        outieOrientedBoxLeft.setValue(true);
                        outieOrientedBoxRight.setValue(true);
                        missingMateBoxLeft.setValue(true);
                        missingMateBoxRight.setValue(true);
                    }
                }));
        categoryPanel.addActionLink(new ActionLink("unselect all",
                ImageBundleFactory.getControlImageBundle().getSelectNoneImage().createImage(), new ClickListener() {
                    public void onClick(Widget sender) {
                        goodBoxLeft.setValue(false);
                        goodBoxRight.setValue(false);
                        tooCloseBoxLeft.setValue(false);
                        tooCloseBoxRight.setValue(false);
                        noMateBoxLeft.setValue(false);
                        noMateBoxRight.setValue(false);
                        tooFarBoxLeft.setValue(false);
                        tooFarBoxRight.setValue(false);
                        antiOrientedBoxLeft.setValue(false);
                        antiOrientedBoxRight.setValue(false);
                        normalOrientedBoxLeft.setValue(false);
                        normalOrientedBoxRight.setValue(false);
                        outieOrientedBoxLeft.setValue(false);
                        outieOrientedBoxRight.setValue(false);
                        missingMateBoxLeft.setValue(false);
                        missingMateBoxRight.setValue(false);
                    }
                }));

        FlexTable _grid = new FlexTable();
        _grid.setCellSpacing(0);
        _grid.setCellPadding(0);
        _grid.setWidget(0, 0, HtmlUtils.getHtml("Mate Pair Category&nbsp;&nbsp;&nbsp;", "frvFilterTableHeader"));
        _grid.setWidget(0, 1, HtmlUtils.getHtml("Left&nbsp;", "frvFilterTableHeader"));
        _grid.setWidget(0, 2, HtmlUtils.getHtml("Right", "frvFilterTableHeader"));

        _grid.setWidget(1, 0, new Label("Good"));
        _grid.setWidget(1, 1, goodBoxLeft);
        _grid.setWidget(1, 2, goodBoxRight);

        _grid.setWidget(2, 0, new Label("Too Close"));
        _grid.setWidget(2, 1, tooCloseBoxLeft);
        _grid.setWidget(2, 2, tooCloseBoxRight);

        _grid.setWidget(3, 0, new Label("Too Far"));
        _grid.setWidget(3, 1, tooFarBoxLeft);
        _grid.setWidget(3, 2, tooFarBoxRight);

        _grid.setWidget(4, 0, new Label("No Mate"));
        _grid.setWidget(4, 1, noMateBoxLeft);
        _grid.setWidget(4, 2, noMateBoxRight);

        _grid.setWidget(0, 3, HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", "text"));

        _grid.setWidget(0, 4, HtmlUtils.getHtml("Mate Pair Category&nbsp;&nbsp;&nbsp;", "frvFilterTableHeader"));
        _grid.setWidget(0, 5, HtmlUtils.getHtml("Left&nbsp;", "frvFilterTableHeader"));
        _grid.setWidget(0, 6, HtmlUtils.getHtml("Right", "frvFilterTableHeader"));

        _grid.setWidget(1, 4, new Label("Missing Mate"));
        _grid.setWidget(1, 5, missingMateBoxLeft);
        _grid.setWidget(1, 6, missingMateBoxRight);

        _grid.setWidget(2, 4, new Label("Anti-Oriented"));
        _grid.setWidget(2, 5, antiOrientedBoxLeft);
        _grid.setWidget(2, 6, antiOrientedBoxRight);

        _grid.setWidget(3, 4, new Label("Normal Oriented"));
        _grid.setWidget(3, 5, normalOrientedBoxLeft);
        _grid.setWidget(3, 6, normalOrientedBoxRight);

        _grid.setWidget(4, 4, new Label("Outie-Oriented"));
        _grid.setWidget(4, 5, outieOrientedBoxLeft);
        _grid.setWidget(4, 6, outieOrientedBoxRight);

        categoryPanel.add(_grid);

        ClearTitledBox mateDropLineBox = new ClearTitledBox("Mate Span Point", false);
        mateDropLineBox.setLabelStyleName("clearTitledBoxLabel");
        mateDropLineBox.setLabelPanelStyleName("tabColorBackground");
        mateDropLineBox.setActionLinkBackgroundStyleName("tabColorBackground");
        useSpanPoint.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                spanLocation.setEnabled(useSpanPoint.getValue());
                if (useSpanPoint.getValue()) {
                    lengthLabel.setStyleName("text");
                }
                else {
                    lengthLabel.setStyleName("disabledText");
                    spanLocation.setText("");
                }
            }
        });
        spanLocation.setVisibleLength(9);
        if (null != recruitJob.getMateSpanPoint()) {
            spanLocation.setText(recruitJob.getMateSpanPoint());
            spanLocation.setEnabled(true);
        }
        else {
            spanLocation.setEnabled(false);
        }
        lengthLabel = HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(Length of Query sequence: " + recruitJob.getRefAxisEndCoord() + ")", "infoTextDisabled");

        FlexTable spanGrid = new FlexTable();
        spanGrid.setWidget(0, 0, useSpanPoint);
        spanGrid.setWidget(0, 1, spanLocation);
        spanGrid.setWidget(1, 0, lengthLabel);
        mateDropLineBox.add(spanGrid);
        _verticalPanel.setWidth("400px");
        _verticalPanel.add(HtmlUtils.getHtml("<br>", "text"));
        _verticalPanel.add(categoryPanel);
//        _verticalPanel.add(HtmlUtils.getHtml("<br>&nbsp;&nbsp;<br>", "text"));
//        _verticalPanel.add(mateDropLineBox);
    }

    public Widget getPanel() {
        _verticalPanel = new VerticalPanel();
        return _verticalPanel;
    }

    public String getTabLabel() {
        return LABEL_NAME;
    }

    /**
     * This method is intended to alert the panels that someone is going to submit a job and they should update any
     * changes they care about.
     *
     * @return boolean to state whether changes occurred
     */
    public boolean updateJobChanges() {
        StringBuffer tmpMateInfo = new StringBuffer();
        tmpMateInfo.append(goodBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(goodBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(tooCloseBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(tooCloseBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(noMateBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(noMateBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(tooFarBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(tooFarBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(antiOrientedBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(antiOrientedBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(normalOrientedBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(normalOrientedBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(outieOrientedBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(outieOrientedBoxRight.getValue() ? 1 : 0);
        tmpMateInfo.append(missingMateBoxLeft.getValue() ? 1 : 0);
        tmpMateInfo.append(missingMateBoxRight.getValue() ? 1 : 0);

        boolean tmpChanges = false;
        String tmpSpanText = spanLocation.getText().trim();
        if (!_job.getMateInfo().equals(tmpMateInfo.toString())) {
//            Window.alert(_job.getMateInfo()+"\n NOT equal to "+tmpMateInfo.toString());
            tmpChanges = true;
        }
        // if both are useless, return false (no changes)
        if ((null == _job.getMateSpanPoint() || "".equals(_job.getMateSpanPoint()))
                &&
                (null == tmpSpanText || "".equals(tmpSpanText))) {
            // do nothing
        }
        else {
            if (!tmpSpanText.equals(_job.getMateSpanPoint())) {
//                Window.alert(tmpSpanText+"\n NOT equal to "+_job.getMateSpanPoint());
                tmpChanges = true;
            }
        }
        _job.setMateSpanPoint(tmpSpanText);
        _job.setMateInfo(tmpMateInfo.toString());
        return tmpChanges;
    }

}