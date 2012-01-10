
package org.janelia.it.jacs.web.gwt.download.client.samples.wizard;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;

import java.io.Serializable;

/**
 * @author Michael Press
 */
public class SampleInfo implements Serializable, IsSerializable {
    Sample _sample;
    String _projectSymbol;
    String _initialProjectSymbol;

    public SampleInfo() {
    }

    public Sample getCurrentSample() {
        return _sample;
    }

    public void setCurrentSample(Sample sample) {
        _sample = sample;
    }

    public String getCurrentProject() {
        return _projectSymbol;
    }

    public void setCurrentProject(String projectSymbol) {
        _projectSymbol = projectSymbol;
    }

    public void setInitialProjectSymbol(String projectSymbol) {
        _initialProjectSymbol = projectSymbol;
    }

    public String getInitialProjectSymbol() {
        return _initialProjectSymbol;
    }
}
