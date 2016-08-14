package org.janelia.it.jacs.model.domain.tiledMicroscope;

import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

/**
 * Tile microscope workspace for annotating a TmSample.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="tmWorkspace",label="Tiled Microscope Workspace")
public class TmWorkspace extends AbstractDomainObject {

    private Reference sampleRef;
    private Map<Long, TmNeuronStyle> neuronStyles = new HashMap<>();
    private boolean autoTracing;
    private boolean autoPointRefinement;
    private TmColorModel colorModel;
    private TmColorModel colorModel3d;

    public TmWorkspace() {
    }

    public TmWorkspace(Long id, String name, String ownerKey, Long sampleID) {
        setId(id);
        setName(name);
        setOwnerKey(ownerKey);
        this.sampleRef = Reference.createFor("TmSample", sampleID);
    }

    public Reference getSampleRef() {
        return sampleRef;
    }

    public void setSampleRef(Reference sampleRef) {
        this.sampleRef = sampleRef;
    }

    public Map<Long, TmNeuronStyle> getNeuronStyles() {
        return neuronStyles;
    }

    public void setNeuronStyles(Map<Long, TmNeuronStyle> neuronStyles) {
        this.neuronStyles = neuronStyles;
    }

    public boolean isAutoTracing() {
        return autoTracing;
    }

    public void setAutoTracing(boolean autoTracing) {
        this.autoTracing = autoTracing;
    }

    public boolean isAutoPointRefinement() {
        return autoPointRefinement;
    }

    public void setAutoPointRefinement(boolean autoPointRefinement) {
        this.autoPointRefinement = autoPointRefinement;
    }

    public TmColorModel getColorModel() {
        return colorModel;
    }

    public void setColorModel(TmColorModel colorModel) {
        this.colorModel = colorModel;
    }

    public TmColorModel getColorModel3d() {
        return colorModel3d;
    }

    public void setColorModel3d(TmColorModel colorModel3d) {
        this.colorModel3d = colorModel3d;
    }

    @Override
    public String toString() {
        return getName();
    }
}
