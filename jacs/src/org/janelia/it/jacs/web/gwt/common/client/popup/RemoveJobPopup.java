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

package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.shared.tasks.JobInfo;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RemoveJobListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Cristian Goina
 */
public class RemoveJobPopup extends ModalPopupPanel {

    private org.janelia.it.jacs.shared.tasks.JobInfo jobStatus;
    private RemoveJobListener jobRemover;

    public RemoveJobPopup(JobInfo jobStatus, RemoveJobListener jobRemover, boolean realizeNow) {
        super("Confirm Delete", realizeNow);
        this.jobRemover = jobRemover;
        this.jobStatus = jobStatus;
    }

    protected ButtonSet createButtons() {
        final RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Delete", new ClickListener() {
            public void onClick(Widget widget) {
                jobRemover.removeJob(jobStatus.getJobId());
                tmpButtons[0].setEnabled(false);
                tmpButtons[1].setEnabled(false);
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
        String tmpJobname = (null == jobStatus.getJobname() || "".equals(jobStatus.getJobname())) ? "" : (" \"" + jobStatus.getJobname() + "\"");
        add(HtmlUtils.getHtml("Delete job" + tmpJobname + "?", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

}
