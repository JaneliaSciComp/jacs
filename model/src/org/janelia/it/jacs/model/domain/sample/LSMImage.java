package org.janelia.it.jacs.model.domain.sample;

public class LSMImage extends Image {

    private Long sampleId;
    private String age;
    private String anatomicalArea;
    private String channelColors;
    private String channelDyeNames;
    private String chanSpec;
    private String effector;
    private String gender;
    private String line;
    private String mountingProtocol;
    private String tissueOrientation;
    private Integer sageId;
    private String slideCode;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

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

    public String getTissueOrientation() {
        return tissueOrientation;
    }

    public void setTissueOrientation(String tissueOrientation) {
        this.tissueOrientation = tissueOrientation;
    }

    public Integer getSageId() {
        return sageId;
    }

    public void setSageId(Integer sageId) {
        this.sageId = sageId;
    }

    public String getSlideCode() {
        return slideCode;
    }

    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }
}
