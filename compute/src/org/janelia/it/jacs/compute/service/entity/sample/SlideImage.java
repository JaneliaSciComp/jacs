package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.model.entity.EntityConstants;

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
    private String age;
    private String mountingProtocol;
    private File file;
	
    public Map<String,String> getProperties() {
        Map<String,String> properties = new HashMap<String,String>();
        properties.put(EntityConstants.ATTRIBUTE_SAGE_ID, sageId.toString());
        properties.put(EntityConstants.ATTRIBUTE_SLIDE_CODE, slideCode);
        properties.put(EntityConstants.ATTRIBUTE_FILE_PATH, imagePath);
        properties.put(EntityConstants.ATTRIBUTE_LINE, line);
        
        if (channelSpec!=null) {
            properties.put(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);
        }
        
        if (channels!=null) {
            properties.put(EntityConstants.ATTRIBUTE_NUM_CHANNELS, channels);
        }
        
        if (objective!=null) {
            properties.put(EntityConstants.ATTRIBUTE_OBJECTIVE, objective);
        }
        
        if (opticalRes!=null) {
            properties.put(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalRes);
        }

        if (gender!=null) {
            properties.put(EntityConstants.ATTRIBUTE_GENDER, gender);
        }

        if (area!=null) {
            properties.put(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, area);
        }
        
        if (age!=null) {
            properties.put(EntityConstants.ATTRIBUTE_AGE, age);
        }
        
        if (mountingProtocol!=null) {
            properties.put(EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL, mountingProtocol);
        }
        
        return properties;
    }
    
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
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
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
