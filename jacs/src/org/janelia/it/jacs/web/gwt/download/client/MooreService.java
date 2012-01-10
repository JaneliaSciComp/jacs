
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.rpc.RemoteService;

import java.util.List;

/**
 * GWT RemoteService for retrieving data from MF150 database
 * Note:
 * GWT does not like Object as return type so it may be needed to
 * create separate methods for each of the primary objects
 *
 * @author Tareq Nabeel
 */
public interface MooreService extends RemoteService {

    /**
     */
    public List getOrganisms();

}