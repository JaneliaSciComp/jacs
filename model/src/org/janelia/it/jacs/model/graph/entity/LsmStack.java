package org.janelia.it.jacs.model.graph.entity;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.graph.annotations.GraphAttribute;
import org.janelia.it.jacs.model.graph.annotations.GraphNode;

@GraphNode(type=EntityConstants.TYPE_LSM_STACK)
public class LsmStack extends EntityNode {

    @GraphAttribute(EntityConstants.ATTRIBUTE_AGE)
    private String age;

    @GraphAttribute(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
    private String anatomicalArea;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_COLORS)
    private String channelColors;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES)
    private String channelDyeNames;

    @GraphAttribute(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION)
    private String chanSpec;

    @GraphAttribute(EntityConstants.ATTRIBUTE_EFFECTOR)
    private String effector;

    @GraphAttribute(EntityConstants.ATTRIBUTE_FILE_PATH)
    private String filepath;

    @GraphAttribute(EntityConstants.ATTRIBUTE_GENDER)
    private String gender;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_LINE)
    private String line;

    @GraphAttribute(EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL)
    private String mountingProtocol;

    @GraphAttribute(EntityConstants.ATTRIBUTE_OBJECTIVE)
    private String objective;

    @GraphAttribute(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION)
    private String opticalResolution;

    @GraphAttribute(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION)
    private String pixelResolution;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_SAGE_ID)
    private Integer sageid;
    
    @GraphAttribute(EntityConstants.ATTRIBUTE_SLIDE_CODE)
    private String slideCode;

    /* EVERYTHING BELOW IS AUTO GENERATED */
    
    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
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

    public String getEffector() {
        return effector;
    }

    public void setEffector(String effector) {
        this.effector = effector;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getMountingProtocol() {
        return mountingProtocol;
    }

    public void setMountingProtocol(String mountingProtocol) {
        this.mountingProtocol = mountingProtocol;
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

    public String getPixelResolution() {
        return pixelResolution;
    }

    public void setPixelResolution(String pixelResolution) {
        this.pixelResolution = pixelResolution;
    }

    public Integer getSageid() {
        return sageid;
    }

    public void setSageid(Integer sageid) {
        this.sageid = sageid;
    }

    public String getSlideCode() {
        return slideCode;
    }

    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }

}
