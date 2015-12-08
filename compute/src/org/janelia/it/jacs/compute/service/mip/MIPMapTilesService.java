package org.janelia.it.jacs.compute.service.mip;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Retile the original data set.
 *
 * @author cgoina
 */
public class MIPMapTilesService extends SubmitDrmaaJobService {

    private static final Logger LOG = LoggerFactory.getLogger(MIPMapTilesService.class);

    private static final int DESIRED_PROCESSED_X_TILES = 4; // 4 horizontal tiles
    private static final int DESIRED_PROCESSED_Y_TILES = 4; // 4 vertical tiles
    private static final int DESIRED_PROCESSED_Z_LAYERS = 10; // 10 layers

    private Long imageWidth;
    private Long imageHeight;
    private Long imageDepth;
    private String sourceRootUrl;
    private String sourceStackFormat;
    private String targetRootUrl;
    private String targetStackFormat;

    private Long sourceMinX;
    private Long sourceMinY;
    private Long sourceMinZ;
    private Long sourceWidth;
    private Long sourceHeight;
    private Long sourceDepth;
    private Integer sourceTileWidth;
    private Integer sourceTileHeight;
    private Integer targetTileWidth;
    private Integer targetTileHeight;
    private Integer sourceScaleLevel;
    private Double sourceXYResolution;
    private Double sourceZResolution;
    private String orientation;
    private Double targetQuality;
    private String targetType;
    private String targetMediaFormat;
    private Boolean targetSkipEmptyTiles;
    private Integer bgPixelValue;
    private String processingAccount;

    @Override
    protected String getGridServicePrefixName() {
        return "mipmaps-retiler";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        sourceRootUrl = processData.getString("SOURCE_ROOT_URL");
        if (!processData.getBoolean("RETILE_IMAGE")) {
            LOG.info("No RETILE requested for {}", sourceRootUrl);
            cancel();
        }
        sourceStackFormat = processData.getString("SOURCE_STACK_FORMAT");
        targetRootUrl = processData.getString("TARGET_ROOT_URL");
        targetStackFormat = processData.getString("TARGET_STACK_FORMAT");
        if (targetStackFormat == null || targetStackFormat.trim().length() == 0) {
            targetStackFormat = sourceStackFormat;
        }
        if (targetRootUrl == null) {
            targetRootUrl = resultFileNode.getDirectoryPath() + "/" + "mipmaptiles";
        }
        extractImageParameters(processData);
        orientation = processData.getString("ORIENTATION");
        targetQuality = processData.getDouble("TARGET_QUALITY");
        targetType = processData.getString("TARGET_TYPE");
        targetMediaFormat = processData.getString("TARGET_MEDIA_FORMAT");
        targetSkipEmptyTiles = processData.getBoolean("TARGET_SKIP_EMPTY_TILES");
        bgPixelValue = processData.getInt("BG_PIXEL_VALUE");
        processingAccount = processData.getString("PROCESSING_ACCOUNT");
    }

    private void extractImageParameters(IProcessData processData) throws MissingDataException {
        imageWidth = getValidDimension(processData, "IMAGE_WIDTH").longValue();
        imageHeight = getValidDimension(processData, "IMAGE_HEIGHT").longValue();
        imageDepth = getDimensionWithDefault(processData, "IMAGE_DEPTH", 1L).longValue();

        sourceScaleLevel = processData.getInt("SOURCE_MAGNIFICATION_LEVEL");
        sourceXYResolution = processData.getDouble("SOURCE_XY_RESOLUTION");
        sourceZResolution = processData.getDouble("SOURCE_Z_RESOLUTION");

        sourceMinX = getDimensionWithDefault(processData,"SOURCE_MIN_X", 0L).longValue();
        sourceMinY = getDimensionWithDefault(processData, "SOURCE_MIN_Y", 0L).longValue();
        sourceMinZ = getDimensionWithDefault(processData, "SOURCE_MIN_Z", 0L).longValue();

        sourceWidth = getDimensionWithDefault(processData, "SOURCE_WIDTH", imageWidth).longValue();
        sourceHeight = getDimensionWithDefault(processData, "SOURCE_HEIGHT", imageHeight).longValue();
        sourceDepth = getDimensionWithDefault(processData, "SOURCE_DEPTH", imageDepth).longValue();

        sourceTileWidth = getDimensionWithDefault(processData, "SOURCE_TILE_WIDTH", 256).intValue();
        sourceTileHeight = getDimensionWithDefault(processData, "SOURCE_TILE_HEIGHT", 256).intValue();

        targetTileWidth = getValidDimension(processData, "TARGET_TILE_WIDTH").intValue();
        targetTileHeight = getValidDimension(processData, "TARGET_TILE_HEIGHT").intValue();
    }

    private Number getValidDimension(IProcessData processData, String key) throws MissingDataException {
        Number value = (Number) processData.getMandatoryItem(key);
        if (value.longValue() <= 0L) {
            throw new IllegalArgumentException("Invalid value for " + key + ": " + value);
        }
        return value;
    }

    private Number getDimensionWithDefault(IProcessData processData, String key, Number defaultValue) {
        Number value = (Number) processData.getItem(key);
        if (value == null) {
            value = defaultValue;
        } else if (value.longValue() < 0) {
            throw new IllegalArgumentException("Invalid value for " + key + ": " + value);
        }
        return value;
    }

    @Override
    protected String getAccount() {
        if (processingAccount != null && processingAccount.trim().length() > 0) {
            return processingAccount.trim();
        } else {
            return super.getAccount();
        }
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeShellScript(writer);
        int nJobs = 0;
        long processedWidth = sourceTileWidth * DESIRED_PROCESSED_X_TILES;
        long processedHeight = sourceTileHeight * DESIRED_PROCESSED_Y_TILES;
        long processedDepth = DESIRED_PROCESSED_Z_LAYERS;
        int xSplits = (int) Math.ceil(sourceWidth.doubleValue() / processedWidth);
        int ySplits = (int) Math.ceil(sourceHeight.doubleValue() / processedHeight);
        int zSplits = (int) Math.ceil(sourceDepth.doubleValue() / processedDepth);

        for (int z = 0; z < zSplits; z++) {
            for (int y = 0; y < ySplits; y++) {
                for (int x = 0; x < xSplits; x++) {
                    long startX = sourceMinX + x * processedWidth;
                    long startY = sourceMinY + y * processedHeight;
                    long startZ = sourceMinZ + z * processedDepth;
                    long width = Math.min(processedWidth, sourceMinX + sourceWidth - startX);
                    long height = Math.min(processedHeight, sourceMinY + sourceHeight - startY);
                    long depth = Math.min(processedDepth, sourceMinZ + sourceDepth - startZ);
                    createConfigurationFile(++nJobs,
                            startX, startY, startZ,
                            width, height, depth);
                }
            }
        }
        setJobIncrementStop(nJobs);
    }

    private void createConfigurationFile(int configIndex,
                                         long startX, long startY, long startZ,
                                         long width, long height, long depth) throws ServiceException {
        File configFile = new File(
                getSGEConfigurationDirectory(),
                getGridServicePrefixName() + "Configuration." + configIndex);
        LOG.debug("Write configFile: {} for region ({}, {}, {}) ({}, {}, {}) ", configFile,
                startX, startY, startZ, width, height, depth);
        try(FileWriter fw = new FileWriter(configFile)) {
            fw.write(sourceRootUrl + "\n");
            writeValueOrNone(sourceStackFormat, fw);
            fw.write(targetRootUrl + "\n");
            writeValueOrNone(targetStackFormat, fw);
            fw.write(imageWidth + "\n");
            fw.write(imageHeight + "\n");
            fw.write(imageDepth + "\n");
            fw.write(sourceScaleLevel + "\n");
            fw.write(sourceTileWidth + "\n");
            fw.write(sourceTileHeight + "\n");
            writeValueOrNone(sourceXYResolution, fw);
            writeValueOrNone(sourceZResolution, fw);
            fw.write(startX + "\n");
            fw.write(startY + "\n");
            fw.write(startZ + "\n");
            fw.write(width + "\n");
            fw.write(height + "\n");
            fw.write(depth + "\n");
            fw.write(targetTileWidth + "\n");
            fw.write(targetTileHeight + "\n");
            fw.write((startY / targetTileHeight) + "\n");
            fw.write(((startY + height) / targetTileHeight) + "\n");
            fw.write((startX / targetTileWidth) + "\n");
            fw.write(((startX + width) / targetTileWidth) + "\n");
            fw.write(startZ + "\n");
            fw.write((startZ + depth - 1) + "\n");
            writeValueOrNone(orientation, fw);
            writeValueOrNone(targetQuality, fw);
            writeValueOrNone(targetType, fw);
            writeValueOrNone(targetMediaFormat, fw);
            writeValueOrNone(targetSkipEmptyTiles, fw);
            writeValueOrNone(bgPixelValue, fw);
        } catch (IOException e) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e);
        }
    }

    private void writeValueOrNone(Object val, FileWriter configFileWriter) throws IOException {
        if (val != null) {
            configFileWriter.write(val.toString() + "\n");
        }
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 21;
    }

    @Override
    protected int getRequiredSlots() {
        return 8;
    }

    /**
     * Write the shell script used for all instances in the job array.
     */
    private void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        // read the vars from stdin
        script.append("read SOURCE_URL_ROOT\n");
        if (sourceStackFormat != null) script.append("read SOURCE_STACK_FORMAT\n");
        script.append("read TARGET_ROOT_URL\n");
        if (targetStackFormat != null) script.append("read TARGET_STACK_FORMAT\n");
        script.append("read IMAGE_WIDTH\n");
        script.append("read IMAGE_HEIGHT\n");
        script.append("read IMAGE_DEPTH\n");
        script.append("read SOURCE_MAGNIFICATION_LEVEL\n");
        script.append("read SOURCE_TILE_WIDTH\n");
        script.append("read SOURCE_TILE_HEIGHT\n");
        if (sourceXYResolution != null) script.append("read SOURCE_XY_RESOLUTION\n");
        if (sourceZResolution != null) script.append("read SOURCE_Z_RESOLUTION\n");
        script.append("read SOURCE_MIN_X\n");
        script.append("read SOURCE_MIN_Y\n");
        script.append("read SOURCE_MIN_Z\n");
        script.append("read SOURCE_WIDTH\n");
        script.append("read SOURCE_HEIGHT\n");
        script.append("read SOURCE_DEPTH\n");
        script.append("read TARGET_TILE_WIDTH\n");
        script.append("read TARGET_TILE_HEIGHT\n");
        script.append("read TARGET_MIN_ROW\n");
        script.append("read TARGET_MAX_ROW\n");
        script.append("read TARGET_MIN_COL\n");
        script.append("read TARGET_MAX_COL\n");
        script.append("read TARGET_MIN_Z\n");
        script.append("read TARGET_MAX_Z\n");
        if (orientation != null) script.append("read ORIENTATION\n");
        if (targetQuality != null) script.append("read TARGET_QUALITY\n");
        if (targetType != null) script.append("read TARGET_TYPE\n");
        if (targetMediaFormat != null) script.append("read TARGET_MEDIA_FORMAT\n");
        if (targetSkipEmptyTiles != null) script.append("read TARGET_SKIP_EMPTY_TILES\n");
        if (bgPixelValue != null) script.append("read BG_PIXEL_VALUE\n");

        // pass them to the script as environment variables
        script
            .append("SOURCE_URL_ROOT=$SOURCE_URL_ROOT ")
            .append("SOURCE_STACK_FORMAT=$SOURCE_STACK_FORMAT ")
            .append("TARGET_ROOT_URL=$TARGET_ROOT_URL ")
            .append("TARGET_STACK_FORMAT=$TARGET_STACK_FORMAT ")
            .append("IMAGE_WIDTH=$IMAGE_WIDTH ")
            .append("IMAGE_HEIGHT=$IMAGE_HEIGHT ")
            .append("IMAGE_DEPTH=$IMAGE_DEPTH ")
            .append("SOURCE_MAGNIFICATION_LEVEL=$SOURCE_MAGNIFICATION_LEVEL ")
            .append("SOURCE_TILE_WIDTH=$SOURCE_TILE_WIDTH ")
            .append("SOURCE_TILE_HEIGHT=$SOURCE_TILE_HEIGHT ")
            .append("SOURCE_XY_RESOLUTION=$SOURCE_XY_RESOLUTION ")
            .append("SOURCE_Z_RESOLUTION=$SOURCE_Z_RESOLUTION ")
            .append("SOURCE_MIN_X=$SOURCE_MIN_X ")
            .append("SOURCE_MIN_Y=$SOURCE_MIN_Y ")
            .append("SOURCE_MIN_Z=$SOURCE_MIN_Z ")
            .append("SOURCE_WIDTH=$SOURCE_WIDTH ")
            .append("SOURCE_HEIGHT=$SOURCE_HEIGHT ")
            .append("SOURCE_DEPTH=$SOURCE_DEPTH ")
            .append("TARGET_TILE_WIDTH=$TARGET_TILE_WIDTH ")
            .append("TARGET_TILE_HEIGHT=$TARGET_TILE_HEIGHT ")
            .append("TARGET_MIN_ROW=$TARGET_MIN_ROW ")
            .append("TARGET_MAX_ROW=$TARGET_MAX_ROW ")
            .append("TARGET_MIN_COL=$TARGET_MIN_COL ")
            .append("TARGET_MAX_COL=$TARGET_MAX_COL ")
            .append("TARGET_MIN_Z=$TARGET_MIN_Z ")
            .append("TARGET_MAX_Z=$TARGET_MAX_Z ")
            .append("ORIENTATION=$ORIENTATION ")
            .append("TARGET_QUALITY=$TARGET_QUALITY ")
            .append("TARGET_TYPE=$TARGET_TYPE ")
            .append("TARGET_MEDIA_FORMAT=$TARGET_MEDIA_FORMAT ")
            .append("TARGET_SKIP_EMPTY_TILES=$TARGET_SKIP_EMPTY_TILES ")
            .append("BG_PIXEL_VALUE=$BG_PIXEL_VALUE ")
            .append(MIPMapTilesHelper.getMipMapsRetilerCommands()).append('\n');
        writer.write(script.toString());
    }

}
