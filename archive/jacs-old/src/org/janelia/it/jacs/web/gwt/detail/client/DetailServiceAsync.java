
package org.janelia.it.jacs.web.gwt.detail.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * GWT RemoteService for retrieving data from database
 *
 * @author Tareq Nabeel
 */
public interface DetailServiceAsync {
    void getEntity(String acc, AsyncCallback callback);
}
