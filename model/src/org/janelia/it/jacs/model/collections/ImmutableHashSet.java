
package org.janelia.it.jacs.model.collections;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2006
 * Time: 9:59:34 AM
 */
public class ImmutableHashSet extends HashSet implements IsSerializable {
    /**
     * For GWT Serialization support
     */
    public ImmutableHashSet() {
    }

    public ImmutableHashSet(Set set) {
        this.addAll(set);
    }

    public boolean add(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public boolean addAll(Collection c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public boolean remove(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public boolean removeAll(Collection c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }

    public boolean retainAll(Collection c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Immutable");
    }
}
