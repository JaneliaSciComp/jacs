package org.janelia.it.jacs.model.tasks.mip;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Set;

/**
 * Task for mapping a stack of MIPs to a 3D image.
 */
@XmlRootElement(name = "mipsStack")
@XmlAccessorType(XmlAccessType.NONE)
public class MIPMapTilesTask extends Task {
    transient public static final String TASK_NAME = "mipmaptiles";
    transient public static final String DISPLAY_NAME = "Create Map Tiles";

    // Parameter Keys
    transient private static final String PARAM_imageWidth = "complete image width";
    transient private static final String PARAM_imageHeight = "source image height";
    transient private static final String PARAM_imageDepth = "source image depth";

    transient private static final String PARAM_sourceRootUrl = "root url containing the input stack";
    transient private static final String PARAM_sourceStackFormat = "format of the input stack relative to the root";
    transient private static final String PARAM_sourceMagnificationLevel = "source image magnification level";
    transient private static final String PARAM_sourceTileWidth = "source image tile width";
    transient private static final String PARAM_sourceTileHeight = "source image tile height";
    transient private static final String PARAM_sourceXYResolution = "source image XY resolution";
    transient private static final String PARAM_sourceZResolution = "source image Z resolution";
    transient private static final String PARAM_sourceMinX = "start X of the cropped area in source coordinates";
    transient private static final String PARAM_sourceMinY = "start Y of the cropped area in source coordinates";
    transient private static final String PARAM_sourceMinZ = "start Z of the cropped area in source coordinates";
    transient private static final String PARAM_sourceWidth = "width of the cropped area";
    transient private static final String PARAM_sourceHeight = "height of the cropped area";
    transient private static final String PARAM_sourceDepth = "depth of the cropped area";
    transient private static final String PARAM_bgPixelValue = "background pixel value";

    transient private static final String PARAM_targetRootUrl = "root url containing the target stack";
    transient private static final String PARAM_targetStackFormat = "format of the output stack";
    transient private static final String PARAM_targetTileWidth = "target image tile width";
    transient private static final String PARAM_targetTileHeight = "target image tile height";
    transient private static final String PARAM_targetMinRow = "target min row";
    transient private static final String PARAM_targetMaxRow = "target ax row";
    transient private static final String PARAM_targetMinCol = "target min col";
    transient private static final String PARAM_targetMaxCol = "target max col";
    transient private static final String PARAM_targetMinZ = "target min Z";
    transient private static final String PARAM_targetMaxZ = "target max Z";
    transient private static final String PARAM_orientation = "tile orientation";
    transient private static final String PARAM_targetQuality = "target quality";
    transient private static final String PARAM_targetType = "target type";
    transient private static final String PARAM_targetMediaFormat = "target media format (jpg, png)";
    transient private static final String PARAM_targetSkipEmptyTiles = "target skip empty tiles";
    transient private static final String PARAM_processingAccount = "processing account";

    public MIPMapTilesTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public MIPMapTilesTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_imageWidth, "");
        setTaskName(TASK_NAME);
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;

        switch (key) {
            case PARAM_imageWidth:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_imageHeight:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_imageDepth:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_sourceRootUrl:
                return new TextParameterVO(value, 400);
            case PARAM_sourceStackFormat:
                return new TextParameterVO(value, 400);
            case PARAM_sourceMagnificationLevel:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_sourceTileWidth:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_sourceTileHeight:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_sourceXYResolution:
                return new DoubleParameterVO(Double.valueOf(value));
            case PARAM_sourceZResolution:
                return new DoubleParameterVO(Double.valueOf(value));
            case PARAM_sourceMinX:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_sourceMinY:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_sourceMinZ:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_sourceWidth:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_sourceHeight:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_sourceDepth:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_bgPixelValue:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetRootUrl:
                return new TextParameterVO(value, 400);
            case PARAM_targetStackFormat:
                return new TextParameterVO(value, 400);
            case PARAM_targetTileWidth:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetTileHeight:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetMinRow:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetMaxRow:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetMinCol:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetMaxCol:
                return new IntegerParameterVO(Integer.valueOf(value));
            case PARAM_targetMinZ:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_targetMaxZ:
                return new LongParameterVO(Long.valueOf(value));
            case PARAM_orientation:
                return new TextParameterVO(value, 10);
            case PARAM_targetQuality:
                return new DoubleParameterVO(Double.valueOf(value));
            case PARAM_targetType:
                return new TextParameterVO(value, 400);
            case PARAM_targetMediaFormat:
                return new TextParameterVO(value, 400);
            case PARAM_targetSkipEmptyTiles:
                return new BooleanParameterVO(Boolean.valueOf(value));
            case PARAM_processingAccount:
                return new TextParameterVO(value, 400);
            default:
                return null;
        }
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @XmlElement(name = "imageWidth")
    public Long getImageWidth() {
        return getParameterAsLong(PARAM_imageWidth);
    }

    public void setImageWidth(Long imageWidth) {
        setParameterAsLong(PARAM_imageWidth, imageWidth);
    }

    @XmlElement(name = "imageHeight")
    public Long getImageHeight() {
        return getParameterAsLong(PARAM_imageHeight);
    }

    public void setImageHeight(Long imageHeight) {
        setParameterAsLong(PARAM_imageHeight, imageHeight);
    }

    @XmlElement(name = "imageDepth")
    public Long getImageDepth() {
        return getParameterAsLong(PARAM_imageDepth);
    }

    public void setImageDepth(Long imageDepth) {
        setParameterAsLong(PARAM_imageDepth, imageDepth);
    }

    @XmlElement(name = "sourceRootUrl")
    public String getSourceRootUrl() {
        return getParameter(PARAM_sourceRootUrl);
    }

    public void setSourceRootUrl(String sourceRootUrl) {
        setParameter(PARAM_sourceRootUrl, sourceRootUrl);
    }

    @XmlElement(name = "sourceStackFormat")
    public String getSourceStackFormat() {
        return getParameter(PARAM_sourceStackFormat);
    }

    public void setSourceStackFormat(String sourceStackFormat) {
        setParameter(PARAM_sourceStackFormat, sourceStackFormat);
    }

    @XmlElement(name = "sourceMagnificationLevel")
    public Integer getSourceMagnificationLevel() {
        return getParameterAsInteger(PARAM_sourceMagnificationLevel);
    }

    public void setSourceMagnificationLevel(Integer sourceMagnificationLevel) {
        setParameterAsInteger(PARAM_sourceMagnificationLevel, sourceMagnificationLevel);
    }

    @XmlElement(name = "sourceTileWidth")
    public Integer getSourceTileWidth() {
        return getParameterAsInteger(PARAM_sourceTileWidth);
    }

    public void setSourceTileWidth(Integer sourceTileWidth) {
        setParameterAsInteger(PARAM_sourceTileWidth, sourceTileWidth);
    }

    @XmlElement(name = "sourceTileHeight")
    public Integer getSourceTileHeight() {
        return getParameterAsInteger(PARAM_sourceTileHeight);
    }

    public void setSourceTileHeight(Integer sourceTileHeight) {
        setParameterAsInteger(PARAM_sourceTileHeight, sourceTileHeight);
    }

    @XmlElement(name = "sourceXYResolution")
    public Double getSourceXYResolution() {
        return getParameterAsDouble(PARAM_sourceXYResolution);
    }

    public void setSourceXYResolution(Double sourceXYResolution) {
        setParameterAsDouble(PARAM_sourceXYResolution, sourceXYResolution);
    }

    @XmlElement(name = "sourceZResolution")
    public Double getSourceZResolution() {
        return getParameterAsDouble(PARAM_sourceZResolution);
    }

    public void setSourceZResolution(Double sourceZResolution) {
        setParameterAsDouble(PARAM_sourceZResolution, sourceZResolution);
    }

    @XmlElement(name = "sourceMinX")
    public Long getSourceMinX() {
        return getParameterAsLong(PARAM_sourceMinX);
    }

    public void setSourceMinX(Long sourceMinX) {
        setParameterAsLong(PARAM_sourceMinX, sourceMinX);
    }

    @XmlElement(name = "sourceMinY")
    public Long getSourceMinY() {
        return getParameterAsLong(PARAM_sourceMinY);
    }

    public void setSourceMinY(Long sourceMinY) {
        setParameterAsLong(PARAM_sourceMinY, sourceMinY);
    }

    @XmlElement(name = "sourceMinZ")
    public Long getSourceMinZ() {
        return getParameterAsLong(PARAM_sourceMinZ);
    }

    public void setSourceMinZ(Long sourceMinZ) {
        setParameterAsLong(PARAM_sourceMinZ, sourceMinZ);
    }

    @XmlElement(name = "sourceWidth")
    public Long getSourceWidth() {
        return getParameterAsLong(PARAM_sourceWidth);
    }

    public void setSourceWidth(Long sourceWidth) {
        setParameterAsLong(PARAM_sourceWidth, sourceWidth);
    }

    @XmlElement(name = "sourceHeight")
    public Long getSourceHeight() {
        return getParameterAsLong(PARAM_sourceHeight);
    }

    public void setSourceHeight(Long sourceHeight) {
        setParameterAsLong(PARAM_sourceHeight, sourceHeight);
    }

    @XmlElement(name = "sourceDepth")
    public Long getSourceDepth() {
        return getParameterAsLong(PARAM_sourceDepth);
    }

    public void setSourceDepth(Long sourceDepth) {
        setParameterAsLong(PARAM_sourceDepth, sourceDepth);
    }

    @XmlElement(name = "bgPixelValue")
    public Integer getBgPixelValue() {
        return getParameterAsInteger(PARAM_bgPixelValue);
    }

    public void setBgPixelValue(Integer bgPixelValue) {
        setParameterAsInteger(PARAM_bgPixelValue, bgPixelValue);
    }

    @XmlElement(name = "targetRootUrl")
    public String getTargetRootUrl() {
        return getParameter(PARAM_targetRootUrl);
    }

    public void setTargetRootUrl(String targetRootUrl) {
        setParameter(PARAM_targetRootUrl, targetRootUrl);
    }

    @XmlElement(name = "targetStackFormat")
    public String getTargetStackFormat() {
        return getParameter(PARAM_targetStackFormat);
    }

    public void setTargetStackFormat(String targetStackFormat) {
        setParameter(PARAM_targetStackFormat, targetStackFormat);
    }

    @XmlElement(name = "targetTileWidth")
    public Integer getTargetTileWidth() {
        return getParameterAsInteger(PARAM_targetTileWidth);
    }

    public void setTargetTileWidth(Integer targetTileWidth) {
        setParameterAsInteger(PARAM_targetTileWidth, targetTileWidth);
    }

    @XmlElement(name = "targetTileHeight")
    public Integer getTargetTileHeight() {
        return getParameterAsInteger(PARAM_targetTileHeight);
    }

    public void setTargetTileHeight(Integer targetTileHeight) {
        setParameterAsInteger(PARAM_targetTileHeight, targetTileHeight);
    }

    @XmlElement(name = "targetMinRow")
    public Integer getTargetMinRow() {
        return getParameterAsInteger(PARAM_targetMinRow);
    }

    public void setTargetMinRow(Integer targetMinRow) {
        setParameterAsInteger(PARAM_targetMinRow, targetMinRow);
    }

    @XmlElement(name = "targetMaxRow")
    public Integer getTargetMaxRow() {
        return getParameterAsInteger(PARAM_targetMaxRow);
    }

    public void setTargetMaxRow(Integer targetMinRow) {
        setParameterAsInteger(PARAM_targetMaxRow, targetMinRow);
    }

    @XmlElement(name = "targetMinCol")
    public Integer getTargetMinCol() {
        return getParameterAsInteger(PARAM_targetMinCol);
    }

    public void setTargetMinCol(Integer targetMinCol) {
        setParameterAsInteger(PARAM_targetMinCol, targetMinCol);
    }

    @XmlElement(name = "targetMaxCol")
    public Integer getTargetMaxCol() {
        return getParameterAsInteger(PARAM_targetMaxCol);
    }

    public void setTargetMaxCol(Integer targetMaxCol) {
        setParameterAsInteger(PARAM_targetMaxCol, targetMaxCol);
    }

    @XmlElement(name = "targetMinZ")
    public Long getTargetMinZ() {
        return getParameterAsLong(PARAM_targetMinZ);
    }

    public void setTargetMinZ(Long targetMinZ) {
        setParameterAsLong(PARAM_targetMinZ, targetMinZ);
    }

    @XmlElement(name = "targetMaxZ")
    public Long getTargetMaxZ() {
        return getParameterAsLong(PARAM_targetMaxZ);
    }

    public void setTargetMaxZ(Long targetMaxZ) {
        setParameterAsLong(PARAM_targetMaxZ, targetMaxZ);
    }

    @XmlElement(name = "orientation")
    public String getOrientation() {
        return getParameter(PARAM_orientation);
    }

    public void setOrientation(String orientation) {
        setParameter(PARAM_orientation, orientation);
    }

    @XmlElement(name = "targetQuality")
    public Double getTargetQuality() {
        return getParameterAsDouble(PARAM_targetQuality);
    }

    public void setTargetQuality(Double targetQuality) {
        setParameterAsDouble(PARAM_targetQuality, targetQuality);
    }

    @XmlElement(name = "targetType")
    public String getTargetType() {
        return getParameter(PARAM_targetType);
    }

    public void setTargetType(String targetType) {
        setParameter(PARAM_targetType, targetType);
    }

    @XmlElement(name = "targetMediaFormat")
    public String getTargetMediaFormat() {
        return getParameter(PARAM_targetMediaFormat);
    }

    public void setTargetMediaFormat(String targetMediaFormat) {
        setParameter(PARAM_targetMediaFormat, targetMediaFormat);
    }

    @XmlElement(name = "skipEmptyTiles")
    public Boolean getSkipEmptyTiles() {
        return getParameterAsBoolean(PARAM_targetSkipEmptyTiles);
    }

    public void setSkipEmptyTiles(Boolean skipEmptyTiles) {
        setParameterAsBoolean(PARAM_targetSkipEmptyTiles, skipEmptyTiles);
    }

    @XmlElement(name = "processingAccount")
    public String getProcessingAccount() {
        return getParameter(PARAM_processingAccount);
    }

    public void setProcessingAccount(String processingAccount) {
        setParameter(PARAM_processingAccount, processingAccount);
    }
}
