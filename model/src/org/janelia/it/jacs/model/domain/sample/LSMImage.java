package org.janelia.it.jacs.model.domain.sample;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.ImageType;
import org.janelia.it.jacs.model.domain.interfaces.HasImages;
import org.jongo.marshall.jackson.oid.Id;

public class LSMImage extends AbstractDomainObject implements HasImages {

    private Long sampleId;
    private String age;
    private String anatomicalArea;
    private String channelColors;
    private String channelDyeNames;
    private String chanSpec;
    private String lsmFilepath;
    private String effector;
    private String gender;
    private String line;
    private String mountingProtocol;
    private String tissueOrientation;
    private Integer numChannels;
    private String objective;
    private String opticalResolution;
    private String pixelResolution;
    private Integer sageId;
    private String slideCode;
    private Map<ImageType,String> images;

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
    public String getLsmFilepath() {
        return lsmFilepath;
    }
    public void setLsmFilepath(String lsmFilepath) {
        this.lsmFilepath = lsmFilepath;
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
    public Integer getNumChannels() {
        return numChannels;
    }
    public void setNumChannels(Integer numChannels) {
        this.numChannels = numChannels;
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
    public Map<ImageType, String> getImages() {
        return images;
    }
    public void setImages(Map<ImageType, String> images) {
        this.images = images;
    }
    
    
}
