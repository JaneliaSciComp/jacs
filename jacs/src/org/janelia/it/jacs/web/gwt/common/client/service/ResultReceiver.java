
package org.janelia.it.jacs.web.gwt.common.client.service;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 25, 2006
 * Time: 4:28:27 PM
 * <p/>
 * Simplified callback so a single helper can be used to carry out the communication with
 * server.
 */
public interface ResultReceiver {
    void setResult(Object obj);
}
