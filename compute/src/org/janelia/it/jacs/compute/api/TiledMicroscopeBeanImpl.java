package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.TiledMicroscopeDAO;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.jboss.ejb3.StrictMaxPool;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void importSWCFolder(String swcFolderLoc, String ownerKey, Long sampleId, String workspaceName) throws ComputeException {
        try {
            _tiledMicroscopeDAO.importSWCFolder(swcFolderLoc, ownerKey, sampleId, workspaceName);
        } catch (Exception e) {
            String errorString = "Error calling importSWCFolder in DAO layer: " + e.getMessage();
            _logger.error(errorString, e);
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
    public TmWorkspace loadWorkspace(Long workspaceId) throws ComputeException {
        try {
            return _tiledMicroscopeDAO.loadWorkspace(workspaceId);
        } catch (Exception e) {
            String errorString="Workspace " + workspaceId + ".  Error calling loadWorkspace in DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(errorString);
        }
    }

    @Override
    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws ComputeException {
        Map<Integer,byte[]> rtnVal = null;
        try {
            rtnVal = _tiledMicroscopeDAO.getTextureBytes(basePath, viewerCoord, dimensions);

        } catch (Exception e) {
            String errorString="Error calling getTextureByteArray DAO layer: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(e);
        }
        return rtnVal;
    }
    
	/**
	 * Return all channel files whose centroids are nearest the coords given.
	 */
	@Override
	public RawFileInfo getNearestChannelFiles(String basePath, int[] viewerCoord) throws ComputeException {
		RawFileInfo rtnVal = null;
		try {
			RawFileInfo rfi = _tiledMicroscopeDAO.getNearestFileInfo(basePath, viewerCoord);
			if (rfi != null) {
				rtnVal = rfi;
			}
		} catch (Exception e) {
			String errorString = "Error calling getNearestChannelFiles DAO layer: " + e.getMessage();
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

    @Override
    public Set<TmNeuron> getNeuronsFromProtobufDataByWorkspaceId(Long workspaceId) throws ComputeException {
        Set<TmNeuron> neuronSet=null;
        try {
            neuronSet=_tiledMicroscopeDAO.getNeuronsFromProtobufDataByWorkspaceId(workspaceId);
        } catch (Exception e) {
            String errorString="Error calling getNeuronsFromProtobufDataByWorkspaceId: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(e);
        }
        return neuronSet;
    }

    @Override
    public void saveProtobufNeuronBytesJDBC(Long entityDataId, byte[] serializedBytes) throws ComputeException {
        try {
            _tiledMicroscopeDAO.saveProtobufNeuronBytesJDBC(entityDataId, serializedBytes);
        } catch (Exception e) {
            String errorString="Error calling saveProtobufNeuronBytesJDBC: " + e.getMessage();
            _logger.error(errorString);
            throw new ComputeException(e);
        }
    }


}
