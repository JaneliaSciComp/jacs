package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.TiledMicroscopeDAO;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:54 PM
 */


@Stateless(name = "TiledMicroscopeEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class TiledMicroscopeBeanImpl implements TiledMicroscopeBeanLocal, TiledMicroscopeBeanRemote {

    private static final Logger _logger = Logger.getLogger(TiledMicroscopeBeanImpl.class);

    private final TiledMicroscopeDAO _tiledMicroscopeDAO = new TiledMicroscopeDAO(_logger);

    @Override
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

    @Override
    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.createTiledMicroscopeWorkspace(parentId, brainSampleId, name, ownerKey);
        } catch (Exception e) {
            String errorString="Error calling createTiledMicroscopeWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.createTiledMicroscopeNeuron(workspaceId, name);
        } catch (Exception e) {
            String errorString="Error calling createTiledMicroscopeNeuron in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.createTiledMicroscopeSample(user, sampleName, pathToRenderFolder);
        }
        catch (Exception e) {
            String errorString="Error calling createTiledMicroscopeSample in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
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

    @Override
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

    @Override
    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws ComputeException {
        try {
            _tiledMicroscopeDAO.rerootNeurite(neuron, newRoot);
        } catch (Exception e) {
            String errorString="Error calling rerootNeurite in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void splitNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws ComputeException {
        try {
            _tiledMicroscopeDAO.splitNeurite(neuron, newRoot);
        } catch (Exception e) {
            String errorString="Error calling splitNeurite in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron newNeuron) throws ComputeException {
        try {
            _tiledMicroscopeDAO.moveNeurite(annotation, newNeuron);
        } catch (Exception e) {
            String errorString="Error calling moveNeurite in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
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

    @Override
    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.getWorkspacesForBrainSample(brainSampleId, ownerKey);
        } catch (Exception e) {
            String errorString="Error calling getWorkspacesForBrainSample in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws ComputeException {
        try {
            return getNeuronsForWorkspace(workspaceId, ownerKey);
        } catch (Exception e) {
            String errorString="Error calling getNeuronsForWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void removeWorkspacePreference(Long workspaceId, String key) throws ComputeException {
        try {
            _tiledMicroscopeDAO.removeWorkspacePreference(workspaceId, key);
        } catch (Exception e) {
            String errorString="Error calling removeWorkspacePreference in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws ComputeException {
        try {
            _tiledMicroscopeDAO.createOrUpdateWorkspacePreference(workspaceId, key, value);
        } catch (Exception e) {
            String errorString="Error calling createOrUpdateWorkspacePreference in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void deleteNeuron(String ownerKey, Long neuronId) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteNeuron(ownerKey, neuronId);
        } catch (Exception e) {
            String errorString="Error calling deleteNeuron in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void deleteWorkspace(String ownerKey, Long workspaceId) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteWorkspace(ownerKey, workspaceId);
        } catch (Exception e) {
            String errorString="Error calling deleteWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void deleteGeometricAnnotation(Long geoId) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteGeometricAnnotation(geoId);
        } catch (Exception e) {
            String errorString="Error calling deleteGeometricAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public TmWorkspace loadWorkspace(Long workspaceId) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.loadWorkspace(workspaceId);
        } catch (Exception e) {
            String errorString="Error calling loadWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public TmNeuron loadNeuron(Long neuronId) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.loadNeuron(neuronId);
        } catch (Exception e) {
            String errorString="Error calling loadNeuron in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
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

    @Override
    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
        List<List<Integer>> pointlist) throws ComputeException {
        try {
            _tiledMicroscopeDAO.updateAnchoredPath(anchoredPath, annotationID1, annotationID2, pointlist);
        } catch (Exception e) {
            String errorString="Error calling updateAnchoredPath in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void deleteAnchoredPath(Long pathID) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteAnchoredPath(pathID);
        } catch (Exception e) {
            String errorString="Error calling deleteAnchoredPath in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public TmStructuredTextAnnotation addStructuredTextAnnotation(Long neuronID, Long parentID,
        int parentType, int formatVersion, String data) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.addStructuredTextAnnotation(neuronID,
                    parentID, parentType, formatVersion, data);
        } catch (Exception e) {
            String errorString="Error calling addStructuredTextAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void updateStructuredTextAnnotation(TmStructuredTextAnnotation textAnnotation, String data)
            throws ComputeException {
        try {
            _tiledMicroscopeDAO.updateStructuredTextAnnotation(textAnnotation, data);
        } catch (Exception e) {
            String errorString="Error calling udpateStructuredTextAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public void deleteStructuredTextAnnotation(Long annID) throws ComputeException {
        try {
            _tiledMicroscopeDAO.deleteStructuredText(annID);
        } catch (Exception e) {
            String errorString="Error calling deleteStructuredTextAnnotation in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public RawFileInfo getNearestFileInfo(String basePath, int[] viewerCoord) throws ComputeException {
        RawFileInfo rtnVal = null;
        try {
            rtnVal = _tiledMicroscopeDAO.getNearestFileInfo(basePath, viewerCoord);
        } catch (Exception e) {
            String errorString="Error calling getNearestFileInfo DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(e);
        }
        return rtnVal;
    }

    @Override
    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int cubicDim ) throws ComputeException {
        Map<Integer,byte[]> rtnVal = null;
        try {
            rtnVal = _tiledMicroscopeDAO.getTextureBytes(basePath, viewerCoord, cubicDim);

        } catch (Exception e) {
            String errorString="Error calling getTextureByteArray DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(e);
        }
        return rtnVal;
    }
    
    @Override
    public CoordinateToRawTransform getTransform( String basePath ) throws ComputeException {
        CoordinateToRawTransform transform = null;
        try {
            transform = _tiledMicroscopeDAO.getTransform(basePath);
        } catch (Exception e) {
            String errorString="Error calling getTransform DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(e);
        }
        return transform;
    }

}
