/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.popup.jobs;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Cristian Goina
 */
public class EditJobNamePopup extends ModalPopupPanel {

    private JobInfo jobStatus;
    private TextBox jobNameText;
    private org.janelia.it.jacs.web.gwt.common.client.popup.jobs.EditJobNameListener jobNameReplacer;

    public EditJobNamePopup(JobInfo jobStatus, org.janelia.it.jacs.web.gwt.common.client.popup.jobs.EditJobNameListener jobNameReplacer, boolean realizeNow) {
        super("Edit Job Name", realizeNow);
        this.jobStatus = jobStatus;
        this.jobNameReplacer = jobNameReplacer;
    }

    protected ButtonSet createButtons() {
        RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Save", new ClickListener() {
            public void onClick(Widget widget) {
                submit();
            }
        });
        tmpButtons[1] = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    /**
     * For subclasses to supply dialog content
     */
    protected void populateContent() {
        HorizontalPanel editPanel = new HorizontalPanel();
        editPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        editPanel.add(HtmlUtils.getHtml("Job Name:&nbsp;&nbsp;", "prompt"));
        jobNameText = new TextBox();
        jobNameText.setVisibleLength(20);
        jobNameText.setMaxLength(60);
        jobNameText.addKeyboardListener(new KeyboardListenerAdapter() {

            public void onKeyPress(Widget sender, char keyCode, int modifiers) {
                // Check for enter
                if ((keyCode == 13) && (modifiers == 0)) {
                    submit();
                }
            }

        });
        String jobName = jobStatus.getJobname();
        if (jobName != null) {
            jobNameText.setText(jobName);
        }
        editPanel.add(jobNameText);
        add(editPanel);
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

    private void
    submit() {
        if (!jobNameText.getText().equals(jobStatus.getJobname())) {
            jobNameReplacer.replaceJobName(jobStatus.getJobId(), jobNameText.getText());
        }
        hide();
    }

}
