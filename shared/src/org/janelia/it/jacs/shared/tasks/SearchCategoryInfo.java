
package org.janelia.it.jacs.shared.tasks;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * @author Michael Press
 */
public class SearchCategoryInfo implements IsSerializable, Serializable {
    private String _name;
    private Integer _numHits;

    /**
     * No-arg constructor required for GWT
     */
    public SearchCategoryInfo() {
    }

    public SearchCategoryInfo(String name) {
        _name = name;
    }

    public SearchCategoryInfo(String name, Integer numHits) {
        _name = name;
        _numHits = numHits;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public int getNumHits() {
        return _numHits;
    }

    public void setNumHits(int numHits) {
        _numHits = numHits;
    }
}
