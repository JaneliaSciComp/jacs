
package org.janelia.it.jacs.web.gwt.download.client.samples;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.download.client.samples.wizard.ProjectDetailsWizardPage;
import org.janelia.it.jacs.web.gwt.download.client.samples.wizard.SampleDetailsWizardPage;
import org.janelia.it.jacs.web.gwt.download.client.samples.wizard.SampleInfo;
import org.janelia.it.jacs.web.gwt.download.client.samples.wizard.SamplesMapWizardPage;

/**
 * @author Michael Press
 */
public class SamplesPage extends WizardController {
    private static Logger _logger = Logger.getLogger("");

    public static final int SAMPLE_MAP_PAGE = 0;
    public static final int SAMPLE_DETAIL_PAGE = 1;
    public static final int PROJECT_DETAIL_PAGE = 2;
    public static final String PROJECT_SYMBOL_PARAM = "projectSymbol";
    private SampleInfo _data;

    public void onModuleLoad() {
        _data = new SampleInfo();

        addPage(new SamplesMapWizardPage(_data, this));
        addPage(new SampleDetailsWizardPage(_data, this));
        addPage(new ProjectDetailsWizardPage(_data, this));

        // Show the wizard
        start();
    }

    public Breadcrumb getBreadcrumbSection() {
        return new Breadcrumb(Constants.DATA_BROWSE_SAMPLES_LABEL, UrlBuilder.getSamplesUrl());
    }

    protected int getStartingPage(String startURL) {
        return 0; // always start on first page TODO: support starting on other wizard pages
    }

    protected void processURLParam(String name, String value) {
        // Determine if a specific project is requested
        if (PROJECT_SYMBOL_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using project symbol " + value + " from URL");
            _data.setInitialProjectSymbol(value);
        }
        else
            _logger.error("Samples: Got unknown param " + name + "=" + value + " from URL");
    }
}
