
package org.janelia.it.jacs.web.gwt.status.client.panel;

import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;

/**
 * @author Michael Press
 */
public interface AlignmentListener {
    public void onAlignmentSelected(BlastHit hit, String program);
}
