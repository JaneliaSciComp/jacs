package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;
import org.janelia.it.jacs.model.domain.support.SearchType;

@SearchType(key="lsm_image",label="LSM Image")
public class LSMImage extends Image {

    @SearchTraversal({})
    private Reference sample;
    
    @SearchAttribute(key="age_txt",label="Age",facet=true)
    private String age;

    @SearchAttribute(key="anatomical_area_txt",label="Anatomical Area",facet=true)
    private String anatomicalArea;

    @SearchAttribute(key="channel_colors_txt",label="Channel Colors")
    private String channelColors;

    @SearchAttribute(key="channel_dyes_txt",label="Channel Dye Names")
    private String channelDyeNames;

    @SearchAttribute(key="channel_spec_txt",label="Channel Specification",facet=true)
    private String chanSpec;

    @SearchAttribute(key="effector_txt",label="Effector")
    private String effector;

    @SearchAttribute(key="gender_txt",label="Gender",facet=true)
    private String gender;

    @SearchAttribute(key="line_txt",label="Fly Line")    
    private String line;

    @SearchAttribute(key="mount_protocol_txt",label="Mounting Protocol",facet=true)
    private String mountingProtocol;

    @SearchAttribute(key="orientation_txt",label="Tissue Orientation")
    private String tissueOrientation;

    @SearchAttribute(key="vt_line_txt",label="VT Line")
    private String vtLine;

    @SearchAttribute(key="sage_id_i",label="SAGE Id")
    private Integer sageId;

    @SearchAttribute(key="slide_code_txt",label="Slide Code")
    private String slideCode;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getAge() {
        return age;
    }

    public Reference getSample() {
        return sample;
    }

    public void setSample(Reference sample) {
        this.sample = sample;
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

    public String getVtLine() {
        return vtLine;
    }

    public void setVtLine(String vtLine) {
        this.vtLine = vtLine;
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
