package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import javax.ejb.Remote;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:52 PM
 */
@Remote
public interface TiledMicroscopeBeanRemote {

    public void createTiledMicroscopeEntityTypes() throws ComputeException;

    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws ComputeException;

    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws ComputeException;
    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws ComputeException;

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws ComputeException;
    
    public void addLinkedGeometricAnnotations (Map<Integer, Integer> nodeParentLinkage, Map<Integer, TmGeoAnnotation> annotations) throws ComputeException;
    
    public void reparentGeometricAnnotation(TmGeoAnnotation annotation,
                                            Long newParentAnnotationID, TmNeuron neuron) throws ComputeException;

    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws ComputeException;

    public void splitNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws ComputeException;

    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron newNeuron) throws ComputeException;

    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
                                          int index, double x, double y, double z, String comment) throws ComputeException;

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws ComputeException;

    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws ComputeException;

    public void removeWorkspacePreference(Long workspaceId, String key) throws ComputeException;

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws ComputeException;

    public void deleteNeuron(String ownerKey, Long neuronId) throws ComputeException;

    public void deleteWorkspace(String ownerKey, Long workspaceId) throws ComputeException;

    public void deleteGeometricAnnotation(Long geoId) throws ComputeException;

    public TmWorkspace loadWorkspace(Long workspaceId) throws ComputeException;

    public TmNeuron loadNeuron(Long neuronId) throws ComputeException;

    public TmAnchoredPath addAnchoredPath(Long neuronID, Long annotationID1, Long annotationID2,
                                          List<List<Integer>> pointlist) throws ComputeException;

    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
                List<List<Integer>> pointList) throws ComputeException;

    public void deleteAnchoredPath(Long pathID) throws ComputeException;

    public TmStructuredTextAnnotation addStructuredTextAnnotation(Long neuronID, Long parentID,
        int parentType, int formatVersion, String data) throws ComputeException;

    public void updateStructuredTextAnnotation(TmStructuredTextAnnotation textAnnotation, String data)
        throws ComputeException;

    public void deleteStructuredTextAnnotation(Long annID) throws ComputeException;

    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws ComputeException;

    public CoordinateToRawTransform getTransform( String basePath ) throws ComputeException;
}
