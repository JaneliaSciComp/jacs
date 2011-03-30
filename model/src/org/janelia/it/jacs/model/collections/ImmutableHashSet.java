/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
