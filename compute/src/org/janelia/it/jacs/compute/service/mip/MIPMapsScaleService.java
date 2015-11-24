package org.janelia.it.jacs.compute.service.mip;

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
 * Generate map tiles at different magnification levels from a retiled set.
 *
 * @author cgoina
 */
public class MIPMapsScaleService extends SubmitDrmaaJobService {

    private static final Logger LOG = LoggerFactory.getLogger(MIPMapsScaleService.class);

    private static final int DESIRED_PROCESSED_Z_LAYERS = 4; // 4 sections

    private Long imageWidth;
    private Long imageHeight;
    private Long imageDepth;
    private String rootUrl;
    private String tileStackFormat;

    private Long sourceMinX;
    private Long sourceMinY;
    private Long sourceMinZ;
    private Long sourceWidth;
    private Long sourceHeight;
    private Long sourceDepth;
    private Integer targetTileWidth;
    private Integer targetTileHeight;
    private Double targetQuality;
    private String targetType;
    private String targetMediaFormat;
    private Boolean targetSkipEmptyTiles;
    private String processingAccount;

    @Override
    protected String getGridServicePrefixName() {
        return "mipmaps-scaler";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        rootUrl = processData.getString("ROOT_URL");
        tileStackFormat = processData.getString("TILE_STACK_FORMAT");

        if (rootUrl == null) {
            rootUrl = resultFileNode.getDirectoryPath() + "/" + "mipmaptiles";
        }
        if (!processData.getBoolean("SCALE_IMAGE")) {
            LOG.info("No SCALE requested for {}", rootUrl);
            cancel();
        }
        extractImageParameters(processData);
        targetQuality = processData.getDouble("TARGET_QUALITY");
        targetType = processData.getString("TARGET_TYPE");
        targetMediaFormat = processData.getString("TARGET_MEDIA_FORMAT");
        targetSkipEmptyTiles = processData.getBoolean("TARGET_SKIP_EMPTY_TILES");
        processingAccount = processData.getString("PROCESSING_ACCOUNT");
    }

    private void extractImageParameters(IProcessData processData) throws MissingDataException {
        imageWidth = getValidDimension(processData, "IMAGE_WIDTH").longValue();
        imageHeight = getValidDimension(processData, "IMAGE_HEIGHT").longValue();
        imageDepth = getDimensionWithDefault(processData, "IMAGE_DEPTH", 1L).longValue();

        sourceMinX = getDimensionWithDefault(processData,"SOURCE_MIN_X", 0L).longValue();
        sourceMinY = getDimensionWithDefault(processData, "SOURCE_MIN_Y", 0L).longValue();
        sourceMinZ = getDimensionWithDefault(processData, "SOURCE_MIN_Z", 0L).longValue();

        sourceWidth = getDimensionWithDefault(processData, "SOURCE_WIDTH", imageWidth).longValue();
        sourceHeight = getDimensionWithDefault(processData, "SOURCE_HEIGHT", imageHeight).longValue();
        sourceDepth = getDimensionWithDefault(processData, "SOURCE_DEPTH", imageDepth).longValue();

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
        long processedDepth = DESIRED_PROCESSED_Z_LAYERS;
        int zSplits = (int) Math.ceil(sourceDepth.doubleValue() / processedDepth);

        for (int z = 0; z < zSplits; z++) {
            long startZ = sourceMinZ + z * processedDepth;
            long depth = Math.min(processedDepth, sourceMinZ + sourceDepth - startZ);
            createConfigurationFile(++nJobs, startZ, depth);
        }
        setJobIncrementStop(nJobs);
    }

    private void createConfigurationFile(int configIndex, long startZ, long depth) throws ServiceException {
        File configFile = new File(
                getSGEConfigurationDirectory(),
                getGridServicePrefixName() + "Configuration." + configIndex);
        LOG.debug("Write configFile: {} for region ({}, {}) ", configFile, startZ, depth);
        try(FileWriter fw = new FileWriter(configFile)) {
            fw.write(rootUrl + "\n");
            fw.write(tileStackFormat + "\n");
            fw.write(imageWidth + "\n");
            fw.write(imageHeight + "\n");
            fw.write(imageDepth + "\n");
            fw.write(sourceMinX + "\n");
            fw.write(sourceMinY + "\n");
            fw.write(startZ + "\n");
            fw.write(sourceWidth + "\n");
            fw.write(sourceHeight + "\n");
            fw.write((startZ + depth - 1) + "\n");
            fw.write(targetTileWidth + "\n");
            fw.write(targetTileHeight + "\n");
            writeValueOrNone(targetQuality, fw);
            writeValueOrNone(targetType, fw);
            writeValueOrNone(targetMediaFormat, fw);
            writeValueOrNone(targetSkipEmptyTiles, fw);
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
        return 15;
    }

    @Override
    protected int getRequiredSlots() {
        return 2;
    }

    /**
     * Write the shell script used for all instances in the job array.
     */
    private void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        // read the vars from stdin
        script.append("read ROOT_URL\n");
        script.append("read TILE_STACK_FORMAT\n");
        script.append("read IMAGE_WIDTH\n");
        script.append("read IMAGE_HEIGHT\n");
        script.append("read IMAGE_DEPTH\n");
        script.append("read SOURCE_MIN_X\n");
        script.append("read SOURCE_MIN_Y\n");
        script.append("read SOURCE_MIN_Z\n");
        script.append("read SOURCE_WIDTH\n");
        script.append("read SOURCE_HEIGHT\n");
        script.append("read SOURCE_MAX_Z\n");
        script.append("read TARGET_TILE_WIDTH\n");
        script.append("read TARGET_TILE_HEIGHT\n");
        if (targetQuality != null) script.append("read TARGET_QUALITY\n");
        if (targetType != null) script.append("read TARGET_TYPE\n");
        if (targetMediaFormat != null) script.append("read TARGET_MEDIA_FORMAT\n");
        if (targetSkipEmptyTiles != null) script.append("read TARGET_SKIP_EMPTY_TILES\n");

        // pass them to the script as environment variables
        script
            .append("ROOT_URL=$ROOT_URL ")
            .append("TILE_STACK_FORMAT=$TILE_STACK_FORMAT ")
            .append("IMAGE_WIDTH=$IMAGE_WIDTH ")
            .append("IMAGE_HEIGHT=$IMAGE_HEIGHT ")
            .append("IMAGE_DEPTH=$IMAGE_DEPTH ")
            .append("SOURCE_MIN_X=$SOURCE_MIN_X ")
            .append("SOURCE_MIN_Y=$SOURCE_MIN_Y ")
            .append("SOURCE_MIN_Z=$SOURCE_MIN_Z ")
            .append("SOURCE_WIDTH=$SOURCE_WIDTH ")
            .append("SOURCE_HEIGHT=$SOURCE_HEIGHT ")
            .append("SOURCE_MAX_Z=$SOURCE_MAX_Z ")
            .append("TARGET_TILE_WIDTH=$TARGET_TILE_WIDTH ")
            .append("TARGET_TILE_HEIGHT=$TARGET_TILE_HEIGHT ")
            .append("TARGET_QUALITY=$TARGET_QUALITY ")
            .append("TARGET_TYPE=$TARGET_TYPE ")
            .append("TARGET_MEDIA_FORMAT=$TARGET_MEDIA_FORMAT ")
            .append("TARGET_SKIP_EMPTY_TILES=$TARGET_SKIP_EMPTY_TILES ")
            .append(MIPMapTilesHelper.getMipMapsScalerCommands()).append('\n');
        writer.write(script.toString());
    }

}
