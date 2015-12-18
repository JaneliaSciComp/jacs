package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import Jama.Matrix;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.List;
import org.janelia.it.jacs.model.util.MatrixUtilities;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 */

public class TmWorkspace implements IsSerializable, Serializable {
    public final static String WS_VERSION_PROP = "workspace_version";
    public enum Version {
        PB_1,             // Version 1 of the Protobuf-Encoded Neuron.
        ENTITY_4          // Latest Workstation version, before Protobuf.
    }

    //todo seed this entirely from without: serializables should not have so much functionality. LLF
    private Long id;
    private String name;
    private String ownerKey;
    private Long sampleID;
    private List<TmNeuron> neuronList;
    private TmPreferences preferences;
    private Matrix micronToVoxMatrix;
    private Matrix voxToMicronMatrix;
    
    private Version version;

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
    
    public void setWorkspaceVersion(Version version) {
        this.version = version;
    }
    
    public Version getVersion() {
        return version;
    }

    public TmWorkspace(Long id, String name, String ownerKey, Long sampleID, List<TmNeuron> neuronList, TmPreferences preferences) {
        this.id=id;
        this.name=name;
        this.ownerKey=ownerKey;
        this.sampleID = sampleID;
        this.neuronList=neuronList;
        this.preferences=preferences;
    }
    
    public TmWorkspace() {        
    }

    public String serializeMatrix(Matrix matrix, String matrixName) {
        return MatrixUtilities.serializeMatrix(matrix, matrixName);
    }

}
