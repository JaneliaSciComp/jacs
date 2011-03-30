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

package org.janelia.it.jacs.web.gwt.blast.client;

import org.janelia.it.jacs.web.gwt.blast.client.wizard.BlastWizardSubjectSequencePage;
import org.janelia.it.jacs.web.gwt.blast.client.wizard.BlastWizardSubmitJobPage;
import org.janelia.it.jacs.web.gwt.blast.client.wizard.BlastWizardUserSequencePage;
import org.janelia.it.jacs.web.gwt.blast.client.wizard.SubmitJobWaitPage;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.BlastData;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;

public class Blast extends WizardController {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.Blast");
    public static final String TASK_ID_PARAM = "taskId";

    private BlastData _blastData = new BlastData();

    public void onModuleLoad() {
        // Create and fade in the page contents
        try {
            // Setup the pages in the wizard
            //addPage(new PanelReferencePage(_blastData, this));
            addPage(new BlastWizardUserSequencePage(_blastData, this));
            addPage(new BlastWizardSubjectSequencePage(_blastData, this));
            addPage(new BlastWizardSubmitJobPage(_blastData, this));
            addPage(new SubmitJobWaitPage(_blastData, this));
            // when it first loads put the first page's token on history's stack
            //setHistoryEvent(getPageTokenAt(0));
        }
        catch (Throwable e) {
            _logger.error("Error onModuleLoad - Blast. " + e.getMessage());
        }

        // Show the wizard
        start();
    }

    protected void processURLParam(String name, String value) {
        if (TASK_ID_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using taskId=" + value + " from URL");
            _blastData.setTaskIdFromParam(value);
        }
        else
            _logger.error("Got unknown param " + name + "=" + value + " from URL");
    }

    public Breadcrumb getBreadcrumbSection() {
        return new Breadcrumb(Constants.JOBS_NEW_JOB_LABEL, UrlBuilder.getBlastWizardUrl());
    }
}
