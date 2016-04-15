package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

import javax.ejb.Remote;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws ComputeException;

    public void importSWCFolder(String swcFolderLoc, String ownerKey, Long sampleId, String workspaceName) throws ComputeException;
    
    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws ComputeException;

    public void removeWorkspacePreference(Long workspaceId, String key) throws ComputeException;

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws ComputeException;

    public TmWorkspace loadWorkspace(Long workspaceId) throws ComputeException;

    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws ComputeException;

	public RawFileInfo getNearestChannelFiles(String basePath, int[] viewerCoord) throws ComputeException;
	
    public CoordinateToRawTransform getTransform( String basePath ) throws ComputeException;

    public Set<TmNeuron> getNeuronsFromProtobufDataByWorkspaceId(Long workspaceId) throws ComputeException;

    public void saveProtobufNeuronBytesJDBC(Long entityDataId, byte[] serializedBytes) throws ComputeException;

}
