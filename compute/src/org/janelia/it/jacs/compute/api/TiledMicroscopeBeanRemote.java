package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;

import javax.ejb.Remote;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:52 PM
 */
@Remote
public interface TiledMicroscopeBeanRemote {

    public void createTiledMicroscopeEntityTypes() throws ComputeException;

    public TmWorkspace createTiledMicroscopeWorkspace(Long brainSampleId, String name, String ownerKey) throws ComputeException;

    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws ComputeException;

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws ComputeException;

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

}
