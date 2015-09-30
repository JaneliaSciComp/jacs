
package org.janelia.it.jacs.web.gwt.download.server;

import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

public interface PublicationSource {
    Publication readPublication(String location) throws Exception;
}
