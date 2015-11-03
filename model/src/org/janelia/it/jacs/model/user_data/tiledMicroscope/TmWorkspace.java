package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import Jama.Matrix;
import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.util.MatrixUtilities;
import static org.janelia.it.jacs.model.util.MatrixUtilities.*;

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
        if (entity.getEntityTypeName()==null || !entity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            throw new Exception("Entity type must be="+EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
        }
        this.id=entity.getId();
        this.name=entity.getName();
        this.ownerKey=entity.getOwnerKey();
        this.neuronList = new ArrayList<TmNeuron>();
        for (Entity child : entity.getChildren()) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                TmNeuron neuron=new TmNeuron(child);
                neuronList.add(neuron);
            } else if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                preferences=new TmPreferences(child);
            }
        }

        if (sampleEntity != null) {
            this.sampleID = sampleEntity.getId();
            String matrixStr = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX);
            if (matrixStr != null) {
                Matrix matrix = deserializeMatrix(matrixStr, EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX);
                setVoxToMicronMatrix(matrix);
            }
            matrixStr = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX);
            if (matrixStr != null) {
                Matrix matrix = deserializeMatrix(matrixStr, EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX);
                setMicronToVoxMatrix(matrix);
            }
        }
    }
    
    public String serializeMatrix(Matrix matrix, String matrixName) {
        return MatrixUtilities.serializeMatrix(matrix, matrixName);
    }

    private Matrix deserializeMatrix(String matrixString, String matrixName) {
        return MatrixUtilities.deserializeMatrix(matrixString, matrixName);
    }
    
}
