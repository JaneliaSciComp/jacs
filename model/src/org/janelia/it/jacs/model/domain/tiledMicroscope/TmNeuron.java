package org.janelia.it.jacs.model.domain.tiledMicroscope;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

/**
 * Tiled microscope neuron in a TmWorkspace.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="tmNeuron",label="Tiled Microscope Neuron")
public class TmNeuron extends AbstractDomainObject {

    private Reference workspaceRef;
    private String protoBuf;

    public Reference getWorkspaceRef() {
        return workspaceRef;
    }

    public void setWorkspaceRef(Reference workspaceRef) {
        this.workspaceRef = workspaceRef;
    }

    public String getProtoBuf() {
        return protoBuf;
    }

    public void setProtoBuf(String protoBuf) {
        this.protoBuf = protoBuf;
    }
}