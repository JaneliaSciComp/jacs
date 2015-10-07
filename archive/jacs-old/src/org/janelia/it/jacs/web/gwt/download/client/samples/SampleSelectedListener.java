
package org.janelia.it.jacs.web.gwt.download.client.samples;

import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;

/**
 * @author Michael Press
 */
public interface SampleSelectedListener {
    public void onSelect(Sample project);

    public void onUnSelect(Sample project);
}