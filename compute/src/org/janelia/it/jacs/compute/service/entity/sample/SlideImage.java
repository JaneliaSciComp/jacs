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
    private String crossBarcode;
    private String channelSpec;
    private String channels;
    private String objective;
    private String opticalRes;
    private String pixelRes;
    private String gender;
    private String area;
    private String age;
    private String effector;
    private String mountingProtocol;
    private String tissueOrientation;
    private String vtLine;
    private String tmogDate;
    private File file;
	
    public Map<String,String> getProperties() {
        Map<String,String> properties = new HashMap<>();
        properties.put(EntityConstants.ATTRIBUTE_SAGE_ID, sageId.toString());
        properties.put(EntityConstants.ATTRIBUTE_SLIDE_CODE, slideCode);
        properties.put(EntityConstants.ATTRIBUTE_FILE_PATH, imagePath);
        properties.put(EntityConstants.ATTRIBUTE_LINE, line);

        if (crossBarcode != null) {
            properties.put(EntityConstants.ATTRIBUTE_CROSS_BARCODE, crossBarcode);
        }

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

        if (pixelRes!=null) {
            properties.put(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, pixelRes);
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

        if (effector!=null) {
            properties.put(EntityConstants.ATTRIBUTE_EFFECTOR, effector);
        }
        
        if (mountingProtocol!=null) {
            properties.put(EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL, mountingProtocol);
        }

        if (tissueOrientation!=null) {
            properties.put(EntityConstants.ATTRIBUTE_TISSUE_ORIENTATION, tissueOrientation);
        }

        if (vtLine!=null) {
            properties.put(EntityConstants.ATTRIBUTE_VT_LINE, vtLine);
        }

        if (tmogDate!=null) {
            properties.put(EntityConstants.ATTRIBUTE_TMOG_DATE, tmogDate);
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
    public String getCrossBarcode() {
        return crossBarcode;
    }
    public void setCrossBarcode(String crossBarcode) {
        this.crossBarcode = crossBarcode;
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
    public String getPixelRes() {
        return pixelRes;
    }
    public void setPixelRes(String imageSizeX, String imageSizeY, String imageSizeZ) {
        this.pixelRes = imageSizeX+"x"+imageSizeY+"x"+imageSizeZ;
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
    public String getEffector() {
        return effector;
    }
    public void setEffector(String effector) {
        this.effector = effector;
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
    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
    public String getVtLine() {
        return vtLine;
    }
    public void setVtLine(String vtLine) {
        this.vtLine = vtLine;
    }
    public String getTmogDate() {
        return tmogDate;
    }
    public void setTmogDate(String tmogDate) {
        this.tmogDate = tmogDate;
    }
    
}
