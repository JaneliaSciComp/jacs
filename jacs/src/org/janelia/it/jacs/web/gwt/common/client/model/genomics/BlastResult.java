
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
