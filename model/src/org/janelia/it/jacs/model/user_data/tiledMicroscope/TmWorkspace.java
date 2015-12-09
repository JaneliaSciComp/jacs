package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import Jama.Matrix;
import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.entity.Entity;

import java.io.Serializable;
import java.util.List;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmFromEntityPopulator;
import org.janelia.it.jacs.model.util.MatrixUtilities;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 */

public class TmWorkspace implements IsSerializable, Serializable {

    //todo seed this entirely from without: serializables should not have so much functionality. LLF
    private Long id;
    private String name;
    private String ownerKey;
    private Long sampleID;
    private transient Entity workspace;
    private List<TmNeuron> neuronList;
    private TmPreferences preferences;
    private Matrix micronToVoxMatrix;
    private Matrix voxToMicronMatrix;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSampleID() {
        return sampleID;
    }

    public void setSampleID(Long id) {
        this.sampleID = id;
    }

    public String getName() {
        return name;
    }
    
    public Entity getEntity() {
        return workspace;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public List<TmNeuron> getNeuronList() {
        return neuronList;
    }

    public void setNeuronList(List<TmNeuron> neuronList) {
        this.neuronList = neuronList;
    }

    public TmPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(TmPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * @return the micronToVoxMatrix
     */
    public Matrix getMicronToVoxMatrix() {
        return micronToVoxMatrix;
    }

    /**
     * @param micronToVoxMatrix the micronToVoxMatrix to set
     */
    public void setMicronToVoxMatrix(Matrix micronToVoxMatrix) {
        this.micronToVoxMatrix = micronToVoxMatrix;
    }

    /**
     * @return the voxToMicronMatrix
     */
    public Matrix getVoxToMicronMatrix() {
        return voxToMicronMatrix;
    }
    
    public void setFromEntity(Entity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.ownerKey = entity.getOwnerKey();
        this.workspace = entity;
    }

    /**
     * @param voxToMicronMatrix the voxToMicronMatrix to set
     */
    public void setVoxToMicronMatrix(Matrix voxToMicronMatrix) {
        this.voxToMicronMatrix = voxToMicronMatrix;
    }

    public TmWorkspace(Long id, String name, String ownerKey, Long sampleID, List<TmNeuron> neuronList, TmPreferences preferences) {
        this.id=id;
        this.name=name;
        this.ownerKey=ownerKey;
        this.sampleID = sampleID;
        this.neuronList=neuronList;
        this.preferences=preferences;
    }

    // This method will typically be used to generate a TmWorkspace object after the Workspace entity has been initially
    // retrieved from the database.

    public TmWorkspace(Entity entity, Entity sampleEntity) throws Exception {
        new TmFromEntityPopulator().populateWorkspace(entity, sampleEntity, this);
    }

    public String serializeMatrix(Matrix matrix, String matrixName) {
        return MatrixUtilities.serializeMatrix(matrix, matrixName);
    }

}
