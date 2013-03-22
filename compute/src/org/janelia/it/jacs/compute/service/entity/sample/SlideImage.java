package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;

/**
 * Image data from SAGE that is used for creating Sample and LSM Stack entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SlideImage {
    
    private Long sageId;
    private String slideCode;
    private String imagePath;
    private String tileType;
    private String line;
    private String channelSpec;
    private String channels;
    private String objective;
    private String opticalRes;
    private String gender;
    private String area;
    private String mountingProtocol;
    private File file;
	
    public Long getSageId() {
        return sageId;
    }
    public void setSageId(Long sageId) {
        this.sageId = sageId;
    }
    public String getSlideCode() {
        return slideCode;
    }
    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        this.file = imagePath!=null?new File(imagePath):null;
    }
    public String getTileType() {
        return tileType;
    }
    public void setTileType(String tileType) {
        this.tileType = tileType;
    }
    public String getLine() {
        return line;
    }
    public void setLine(String line) {
        this.line = line;
    }
    public String getChannelSpec() {
        return channelSpec;
    }
    public void setChannelSpec(String channelSpec) {
        this.channelSpec = channelSpec;
    }
    public String getChannels() {
        return channels;
    }
    public void setChannels(String channels) {
        this.channels = channels;
    }
    public String getObjective() {
        return objective;
    }
    public void setObjective(String objective) {
        this.objective = objective;
    }
    public String getOpticalRes() {
        return opticalRes;
    }
    public void setOpticalRes(String voxelSizeX, String voxelSizeY, String voxelSizeZ) {
        this.opticalRes = voxelSizeX+"x"+voxelSizeY+"x"+voxelSizeZ;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getArea() {
        return area;
    }
    public void setArea(String area) {
        this.area = area;
    }
    public String getMountingProtocol() {
        return mountingProtocol;
    }
    public void setMountingProtocol(String mountingProtocol) {
        this.mountingProtocol = mountingProtocol;
    }
    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
}
