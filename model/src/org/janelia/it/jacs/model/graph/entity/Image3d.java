package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;
import org.janelia.it.jacs.model.graph.annotations.RelatedTo;

@GraphNode(type=EntityConstants.TYPE_IMAGE_3D)
public class Image3d extends Renderable {

	private static final long serialVersionUID = 1L;
	
    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_ALIGNED_CONSOLIDATED_LABEL)
    private Image3d alignedConsolidatedLabel;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE)
    private String alignmentInconsistencyScore;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_NCC_SCORE)
    private String alignmentNormalizedCrossCorrelationScore;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE)
    private String alignmentSpace;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_ALIGNMENT_VERIFY_MOVIE)
    private Movie alignmentVerificationMovie;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ARTIFACT_SOURCE_ID)
    private Long artifactSourceEntityId;

    @GraphAttribute(EntityConstants.ATTRIBUTE_BOUNDING_BOX)
    private String boundingBox;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_CHAN_IMAGE)
    private Image3d chanImage;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_COLORS)
    private String channelColors;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES)
    private String channelDyeNames;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)
    private String chanSpec;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;

    @GraphAttribute(EntityConstants.ATTRIBUTE_IMAGE_FORMAT)
    private String imageFormat;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_IS_ZIPPED)
    private Boolean isZipped;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_MASK_IMAGE)
    private Image3d maskImage;

    @GraphAttribute(EntityConstants.ATTRIBUTE_OBJECTIVE)
    private String objective;

    @GraphAttribute(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)
    private String opticalResolution;

    @RelatedTo(relationType=EntityConstants.ATTRIBUTE_PERFORMANCE_PROXY_IMAGE)
    private Image3d performanceProxyImage;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)
    private String pixelResolution;

    /* EVERYTHING BELOW IS AUTO GENERATED */

    public Image3d getAlignedConsolidatedLabel() {
        return alignedConsolidatedLabel;
    }

    public void setAlignedConsolidatedLabel(Image3d alignedConsolidatedLabel) {
        this.alignedConsolidatedLabel = alignedConsolidatedLabel;
    }

    public String getAlignmentInconsistencyScore() {
        return alignmentInconsistencyScore;
    }

    public void setAlignmentInconsistencyScore(String alignmentInconsistencyScore) {
        this.alignmentInconsistencyScore = alignmentInconsistencyScore;
    }

    public String getAlignmentNormalizedCrossCorrelationScore() {
        return alignmentNormalizedCrossCorrelationScore;
    }

    public void setAlignmentNormalizedCrossCorrelationScore(String alignmentNormalizedCrossCorrelationScore) {
        this.alignmentNormalizedCrossCorrelationScore = alignmentNormalizedCrossCorrelationScore;
    }

    public String getAlignmentSpace() {
        return alignmentSpace;
    }

    public void setAlignmentSpace(String alignmentSpace) {
        this.alignmentSpace = alignmentSpace;
    }

    public Movie getAlignmentVerificationMovie() {
        return alignmentVerificationMovie;
    }

    public void setAlignmentVerificationMovie(Movie alignmentVerificationMovie) {
        this.alignmentVerificationMovie = alignmentVerificationMovie;
    }

    public Long getArtifactSourceEntityId() {
        return artifactSourceEntityId;
    }

    public void setArtifactSourceEntityId(Long artifactSourceEntityId) {
        this.artifactSourceEntityId = artifactSourceEntityId;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Image3d getChanImage() {
        return chanImage;
    }

    public void setChanImage(Image3d chanImage) {
        this.chanImage = chanImage;
    }

    public String getChannelColors() {
        return channelColors;
    }

    public void setChannelColors(String channelColors) {
        this.channelColors = channelColors;
    }

    public String getChannelDyeNames() {
        return channelDyeNames;
    }

    public void setChannelDyeNames(String channelDyeNames) {
        this.channelDyeNames = channelDyeNames;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    public Boolean getIsZipped() {
        return isZipped;
    }

    public void setIsZipped(Boolean isZipped) {
        this.isZipped = isZipped;
    }

    public Image3d getMaskImage() {
        return maskImage;
    }

    public void setMaskImage(Image3d maskImage) {
        this.maskImage = maskImage;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getOpticalResolution() {
        return opticalResolution;
    }

    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }

    public Image3d getPerformanceProxyImage() {
        return performanceProxyImage;
    }

    public void setPerformanceProxyImage(Image3d performanceProxyImage) {
        this.performanceProxyImage = performanceProxyImage;
    }

    public String getPixelResolution() {
        return pixelResolution;
    }

    public void setPixelResolution(String pixelResolution) {
        this.pixelResolution = pixelResolution;
    }
}
