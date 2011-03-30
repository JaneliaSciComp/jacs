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

package org.janelia.it.jacs.web.gwt.status.client;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.status.client.wizard.DetailsWizardPage;
import org.janelia.it.jacs.web.gwt.status.client.wizard.JobDetailsPage;
import org.janelia.it.jacs.web.gwt.status.client.wizard.JobResultsPage;

/**
 * Wizard to walk job status info.
 */
public class Status extends WizardController {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.Status");

    private JobResultsData _jobResultsData = new JobResultsData();

    public static final String JOB_ID_PARAM = "jobId";
    public static final String ENTITY_ACC_PARAM = DetailPanel.ACC_PARAM;

    public void onModuleLoad() {
        // Create and fade in the page contents
        try {
            // Setup the pages in the wizard
            addPage(new JobResultsPage(_jobResultsData, this));
            addPage(new JobDetailsPage(_jobResultsData, this));
            addPage(new DetailsWizardPage(_jobResultsData, this));
        }
        catch (Throwable t) {
            _logger.error("Error onModuleLoad - Wizard. ", t);
        }

        // Show the wizard
        start();
    }

    protected void processURLParam(String name, String value) {
        if (JOB_ID_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using jobId=" + value + " from URL");
            _jobResultsData.setJobId(value);
        }
        if (ENTITY_ACC_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using acc=" + value + " from URL");
            _jobResultsData.setDetailAcc(value);
        }
        else
            _logger.error("Got unknown param " + name + "=" + value + " from URL");
    }

    public Breadcrumb getBreadcrumbSection() {
        return new Breadcrumb(Constants.JOBS_JOB_RESULTS_LABEL, UrlBuilder.getStatusUrl());
    }
}
