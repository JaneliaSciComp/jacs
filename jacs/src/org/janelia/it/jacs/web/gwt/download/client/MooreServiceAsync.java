
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * GWT RemoteService for retrieving data from MF150 database
 *
 * @author Tareq Nabeel
 */
public interface MooreServiceAsync {
    void getOrganisms(AsyncCallback callback);
}