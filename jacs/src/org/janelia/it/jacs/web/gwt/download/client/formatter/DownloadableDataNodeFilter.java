
package org.janelia.it.jacs.web.gwt.download.client.formatter;

import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;

/**
 * Filter: include or exclude a Downloadable Data Node, based on its return.
 * <p/>
 * User: Lfoster
 * Date: Nov 6, 2006
 * Time: 3:34:18 PM
 */
public interface DownloadableDataNodeFilter {
    boolean isAcceptable(DownloadableDataNode node);
}
