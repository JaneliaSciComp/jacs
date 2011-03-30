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

package org.janelia.it.jacs.web.gwt.common.client.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side value object based on org.janelia.it.jacs.model.genomics.BlastResultNode.
 *
 * @author Michael Press
 */
public class BlastResult implements IsSerializable {
    private Set<BlastHit> _hits = new HashSet<BlastHit>();
    private String _id;

    public BlastResult() {
    }

    public BlastResult(String objectId, Set<BlastHit> hits) {
        _id = objectId;
        _hits = hits;
    }

    public Set<BlastHit> getHits() {
        return _hits;
    }

    public void setHits(Set<BlastHit> resultSet) {
        this._hits = resultSet;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }
}
