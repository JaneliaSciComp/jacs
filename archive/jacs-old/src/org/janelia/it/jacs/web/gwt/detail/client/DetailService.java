
package org.janelia.it.jacs.web.gwt.detail.client;

import com.google.gwt.user.client.rpc.RemoteService;

import java.io.Serializable;

/**
 * GWT RemoteService for retrieving data from database
 * Note:
 * GWT does not like Object as return type so it may be needed to
 * create separate methods for each of the primary objects
 *
 * @author Tareq Nabeel
 */
public interface DetailService extends RemoteService {

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param acc the camera accession
     * @return a serializable entity instance
     */
    public Serializable getEntity(String acc);

}
