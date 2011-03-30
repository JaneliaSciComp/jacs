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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.Timer;

import java.io.Serializable;

/**
 * A standard "Loading..." label for panels to use while sub-panels or contenst are being loaded.
 * Use setVisible(true|false) at appropriate times.
 *
 * @author Michael Press
 */
public class LoadingLabel extends HTML implements Serializable, IsSerializable {
    private static final String SUBMITTING_MESSAGE = "&nbsp;Submitting your job...";
    private static final String SUCCESS_MESSAGE = "&nbsp;Your job has been submitted successfully.";
    private static final String FAILURE_MESSAGE = "&nbsp;An error occurred submitting your job.";
    private static final int SHOW_MESSAGE_DURATION = 5000;

    /**
     * Creates new label with "Loading..." text
     */
    public LoadingLabel() {
        this("Loading...", true);
    }

    public LoadingLabel(boolean visible) {
        this("Loading...", visible);
    }

    public LoadingLabel(String text, boolean visible) {
        super();
        init(text, visible);
    }

    private void init(String text, boolean visible) {
        setStyleName("loadingMsgText");
        setText(text);
        setVisible(visible);
    }

    public void showSubmittingMessage() {
        setStyleName("text");
        addStyleName("nowrap");
        addStyleName("AdvancedBlastStatusLabel");
        setHTML(SUBMITTING_MESSAGE);
    }

    public void showFailureMessage() {
        setHTML(FAILURE_MESSAGE);
        setStyleName("jobError");
        addStyleName("nowrap");
        addStyleName("AdvancedBlastStatusLabel");
    }

    public void showSuccessMessage() {
        setHTML(SUCCESS_MESSAGE);
        setStyleName("jobCompletedOKText");
        addStyleName("nowrap");
        addStyleName("AdvancedBlastStatusLabel");
        new HideMessageTimer().schedule(SHOW_MESSAGE_DURATION);
    }

    private class HideMessageTimer extends Timer {
        public void run() {
            setHTML("&nbsp;");
        }
    }

}
