
package org.janelia.it.jacs.web.gwt.download.client.project;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * @author Michael Press
 */
public interface ProjectSymbolSelectedListener extends Serializable, IsSerializable {
    public void onSelect(String projectSymbol);

    public void onUnSelect(String projectSymbol);
}