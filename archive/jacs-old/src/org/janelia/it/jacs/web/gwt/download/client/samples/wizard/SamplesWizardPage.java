
package org.janelia.it.jacs.web.gwt.download.client.samples.wizard;

import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardPage;

/**
 * @author Michael Press
 */
abstract public class SamplesWizardPage extends WizardPage {
    private SampleInfo _data;

    public SamplesWizardPage(SampleInfo data, WizardController controller) {
        super(/*show buttons*/ false, controller);
        _data = data;
    }

    public SampleInfo getData() {
        return _data;
    }
}