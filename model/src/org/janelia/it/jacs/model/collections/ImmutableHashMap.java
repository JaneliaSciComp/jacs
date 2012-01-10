
package org.janelia.it.jacs.model.collections;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2006
 * Time: 9:56:06 AM
 */
public class ImmutableHashMap extends HashMap implements IsSerializable {
    /**
     * For GWT Serialization support
     */
    public ImmutableHashMap() {
    }

    public ImmutableHashMap(Map map) {
        this.putAll(map);
    }

    public void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public Object put(Object key, Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public void putAll(Map m) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public Object remove(Object key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }
}
