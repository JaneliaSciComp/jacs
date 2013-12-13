package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.TiledMicroscopeDAO;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:54 PM
 */


@Stateless(name = "TiledMicroscopeEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class TiledMicroscopeBeanImpl implements TiledMicroscopeBeanLocal, TiledMicroscopeBeanRemote {

    private static final Logger _logger = Logger.getLogger(AnnotationBeanImpl.class);

    private final TiledMicroscopeDAO _tiledMicroscopeDAO = new TiledMicroscopeDAO(_logger);

    public void createTiledMicroscopeEntityTypes() throws ComputeException {
        try {
            _tiledMicroscopeDAO.createTiledMicroscopeEntityTypes();
        }
        catch (Exception e) {
            String errorString="Error calling createdTiledMicroscopeEntityTypes in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.createTiledMicroscopeWorkspace(parentId, brainSampleId, name, ownerKey);
        } catch (Exception e) {
            String errorString="Error calling createTiledMicroscopeWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.createTiledMicroscopeNeuron(workspaceId, name);
        } catch (Exception e) {
            String errorString="Error calling createTiledMicroscopeNeuron in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmSample createTiledMicroscopeSample(Long sampleId, String name) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.createTiledMicroscopeSample(sampleId, name);
        }
        catch (Exception e) {
            String errorString="Error calling createTiledMicroscopeSample in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
                                                  double x, double y, double z, String comment) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.addGeometricAnnotation(neuronId, parentAnnotationId, index, x, y, z, comment);
        } catch (Exception e) {
            String errorString="Error calling addGeometricAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void reparentGeometricAnnotation(TmGeoAnnotation annotation, Long newParentAnnotationID,
                                            TmNeuron neuron) throws ComputeException {
        try {
            _tiledMicroscopeDAO.reparentGeometricAnnotation(annotation, newParentAnnotationID, neuron);
        } catch (Exception e) {
            String errorString="Error calling reparentGeometricAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
                                          int index, double x, double y, double z, String comment) throws ComputeException {
        try {
            _tiledMicroscopeDAO.updateGeometricAnnotation(geoAnnotation, index, x, y, z, comment);
        } catch (Exception e) {
            String errorString="Error calling updateGeometricAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.getWorkspacesForBrainSample(brainSampleId, ownerKey);
        } catch (Exception e) {
            String errorString="Error calling getWorkspacesForBrainSample in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws ComputeException {
        try {
            return getNeuronsForWorkspace(workspaceId, ownerKey);
        } catch (Exception e) {
            String errorString="Error calling getNeuronsForWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void removeWorkspacePreference(Long workspaceId, String key) throws ComputeException {
        try {
            _tiledMicroscopeDAO.removeWorkspacePreference(workspaceId, key);
        } catch (Exception e) {
            String errorString="Error calling removeWorkspacePreference in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws ComputeException {
        try {
            _tiledMicroscopeDAO.createOrUpdateWorkspacePreference(workspaceId, key, value);
        } catch (Exception e) {
            String errorString="Error calling createOrUpdateWorkspacePreference in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void deleteNeuron(String ownerKey, Long neuronId) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteNeuron(ownerKey, neuronId);
        } catch (Exception e) {
            String errorString="Error calling deleteNeuron in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void deleteWorkspace(String ownerKey, Long workspaceId) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteWorkspace(ownerKey, workspaceId);
        } catch (Exception e) {
            String errorString="Error calling deleteWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void deleteGeometricAnnotation(Long geoId) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteGeometricAnnotation(geoId);
        } catch (Exception e) {
            String errorString="Error calling deleteGeometricAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmWorkspace loadWorkspace(Long workspaceId) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.loadWorkspace(workspaceId);
        } catch (Exception e) {
            String errorString="Error calling loadWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmNeuron loadNeuron(Long neuronId) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.loadNeuron(neuronId);
        } catch (Exception e) {
            String errorString="Error calling loadNeuron in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public TmAnchoredPath addAnchoredPath(Long neuronID, Long annotationID1, Long annotationID2,
        List<List<Integer>> pointlist) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.addAnchoredPath(neuronID,
                    annotationID1, annotationID2, pointlist);
        } catch (Exception e) {
            String errorString="Error calling addAnchoredPath in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
        List<List<Integer>> pointlist) throws ComputeException {
        try {
            _tiledMicroscopeDAO.updateAnchoredPath(anchoredPath, annotationID1, annotationID2, pointlist);
        } catch (Exception e) {
            String errorString="Error calling updateAnchoredPathin DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    public void deleteAnchoredPath(Long pathID) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteAnchoredPath(pathID);
        } catch (Exception e) {
            String errorString="Error calling deleteAnchoredPathin DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

}
