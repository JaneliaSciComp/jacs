
package org.janelia.it.jacs.web.gwt.common.client.model.download;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;

import java.io.Serializable;
import java.util.List;

/**
 * User: Lfoster
 * Date: Aug 21, 2006
 * Time: 2:20:10 PM
 * <p/>
 * All the stuff it takes to make a data file for model.  Java Bean by getter convention.
 */
public interface DownloadableDataNode extends Serializable, IsSerializable {
    List getChildren();

    String getText();

    String getAttribute(String attributeName);

    String[] getAttributeNames();

    long getSize();

    String getLocation();

    String getInfoLocation();

    boolean isMultifileArchive();

    Site getSite();
}
