package org.janelia.it.jacs.compute.service.mip;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.align.ImageStack;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparatorHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate MIPs for any number of 3d volumes in parallel. 
 * 
 * This is similar to the MIPGenerationService in the vaa3d package, except it uses the Neuron Separator pipeline
 * tools in order to generate MIPs for specific sets of channels.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MIPMapTilesService extends SubmitDrmaaJobService {

    private Long imageWidth;
    private Long imageHeight;
    private String sourceRootUrl;
    private String sourceStackFormat;
    private Integer sourceMagnificationLevel;
    private Integer sourceTileWidth;
    private Integer sourceTileHeight;
    private Double sourceXYResolution;
    private Double sourceZResolution;
    private Long sourceMinX;
    private Long sourceMinY;
    private Long sourceMinZ;
    private Long sourceWidth;
    private Long sourceHeight;
    private Long sourceDepth;
    private String targetRootUrl;
    private String targetStackFormat;
    private Integer targetTileWidth;
    private Integer targetTileHeight;
    private Integer targetMinRow;
    private Integer targetMinCol;
    private Long targetMinZ;
    
    @Override
    protected String getGridServicePrefixName() {
        return "mipmaptiles";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        imageWidth = processData.getLong("IMAGE_WIDTH");
        imageHeight = processData.getLong("IMAGE_HEIGHT");
        sourceRootUrl = processData.getString("SOURCE_ROOT_URL");
        sourceStackFormat = processData.getString("SOURCE_STACK_FORMAT");
        sourceMagnificationLevel = processData.getInt("SOURCE_MAGNIFICATION_LEVEL");
        sourceTileWidth = processData.getInt("SOURCE_TILE_WIDTH");
        sourceTileHeight = processData.getInt("SOURCE_TILE_HEIGHT");
        sourceXYResolution = processData.getDouble("SOURCE_XY_RESOLUTION");
        sourceZResolution = processData.getDouble("SOURCE_Z_RESOLUTION");
        sourceMinX = processData.getLong("SOURCE_MIN_X");
        sourceMinY = processData.getLong("SOURCE_MIN_Y");
        sourceMinZ = processData.getLong("SOURCE_MIN_Z");
        sourceWidth = processData.getLong("SOURCE_WIDTH");
        sourceHeight = processData.getLong("SOURCE_HEIGHT");
        sourceDepth = processData.getLong("SOURCE_DEPTH");
        targetRootUrl = processData.getString("TARGET_ROOT_URL");
        targetStackFormat = processData.getString("TARGET_STACK_FORMAT");
        targetTileWidth = processData.getInt("TARGET_TILE_WIDTH");
        targetTileHeight = processData.getInt("TARGET_TILE_HEIGHT");
        targetMinRow = processData.getInt("TARGET_MIN_ROW");
        targetMinCol = processData.getInt("TARGET_MIN_COL");
        targetMinZ = processData.getLong("TARGET_MIN_Z");
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        // TODO
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 6;
    }
}
