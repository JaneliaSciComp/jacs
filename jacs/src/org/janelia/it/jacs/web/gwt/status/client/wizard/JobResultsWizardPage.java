
package org.janelia.it.jacs.web.gwt.status.client.wizard;

import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardPage;
import org.janelia.it.jacs.web.gwt.status.client.JobResultsData;

/**
 * @author Michael Press
 */
abstract public class JobResultsWizardPage extends WizardPage {
    private JobResultsData _data;

    public JobResultsWizardPage(JobResultsData data, WizardController controller) {
        super(controller);
        _data = data;
    }

    public JobResultsWizardPage(JobResultsData data, WizardController controller, boolean showButtons) {
        super(showButtons, controller);
        _data = data;
    }

    public JobResultsData getData() {
        return _data;
    }
}
