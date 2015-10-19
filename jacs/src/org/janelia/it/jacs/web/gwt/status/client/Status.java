
package org.janelia.it.jacs.web.gwt.status.client;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
//import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
//import org.janelia.it.jacs.web.gwt.status.client.wizard.DetailsWizardPage;
import org.janelia.it.jacs.web.gwt.status.client.wizard.JobDetailsPage;
import org.janelia.it.jacs.web.gwt.status.client.wizard.JobResultsPage;

/**
 * Wizard to walk job status info.
 */
public class Status extends WizardController {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.status.client.Status");

    private JobResultsData _jobResultsData = new JobResultsData();

    public static final String JOB_ID_PARAM = "jobId";
//    public static final String ENTITY_ACC_PARAM = DetailPanel.ACC_PARAM;

    public void onModuleLoad() {
        // Create and fade in the page contents
        try {
            // Setup the pages in the wizard
            addPage(new JobResultsPage(_jobResultsData, this));
            addPage(new JobDetailsPage(_jobResultsData, this));
//            addPage(new DetailsWizardPage(_jobResultsData, this));
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
//        if (ENTITY_ACC_PARAM.equalsIgnoreCase(name)) {
//            _logger.debug("Using acc=" + value + " from URL");
//            _jobResultsData.setDetailAcc(value);
//        }
//        else
//            _logger.error("Status: Got unknown param " + name + "=" + value + " from URL");
    }

    public Breadcrumb getBreadcrumbSection() {
        return new Breadcrumb(Constants.JOBS_JOB_RESULTS_LABEL, UrlBuilder.getStatusUrl());
    }
}
