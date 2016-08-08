package org.janelia.it.jacs.compute.service.domain.model;

import java.io.File;
import java.util.Map;

import org.janelia.it.jacs.model.entity.cv.Objective;

/**
 * Image data from SAGE that is used for creating Sample and LSM Stack entities. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SlideImage {
    
    private Map<String,Object> properties;

    private String datasetName;
    private String crossBarCode;
    private String channelSpec;
    private String age;
    private String gender;
    private String channels;
    private String mountingProtocol;
    private String tissueOrientation;
    private String vtLine;
    private String effector;
    private String objective;
    private String[] opticalRes;
    private String[] pixelRes;
    
    public SlideImage(Map<String,Object> properties) {
        this.properties = properties;

        String gender = (String)properties.get("light_imagery_gender");
        if (gender!=null) {
        	properties.put("light_imagery_gender", sanitizeGender(gender));
        }
    }
    
    public Map<String,Object> getProperties() {
        return properties;
    }
    
    public String getFilepath() {

        // Use JFS path if available
        String jfsPath = (String)properties.get("image_query_jfs_path");
        if (jfsPath!=null) {
            return jfsPath;
        }
    
        // Or use the normal path
        String path = (String)properties.get("image_query_path");
        if (path!=null) {
            return path;
        }
     
        return null;
    }
    
    public File getFile() {
    	String filepath = getFilepath();
    	if (filepath==null) return null;
    	return new File(filepath);
    }
    
    public String getName() {
    	File file = getFile();
    	if (file==null) return null;
    	return file.getName();
    }
        
    public String getObjective() {
        String objectiveStr = (String)properties.get("light_imagery_objective");
        if (objectiveStr!=null) {
            if (objectiveStr.contains(Objective.OBJECTIVE_10X.getName())) {
                return Objective.OBJECTIVE_10X.getName();
            }
            else if (objectiveStr.contains(Objective.OBJECTIVE_20X.getName())) {
                return Objective.OBJECTIVE_20X.getName();
            }
            else if (objectiveStr.contains(Objective.OBJECTIVE_25X.getName())) {
                return Objective.OBJECTIVE_25X.getName();
            }
            else if (objectiveStr.contains(Objective.OBJECTIVE_40X.getName())) {
                return Objective.OBJECTIVE_40X.getName();
            }
            else if (objectiveStr.contains(Objective.OBJECTIVE_63X.getName())) {
                return Objective.OBJECTIVE_63X.getName();
            }
        }
        return "";
    }
    
    public Integer getSageId() {
        Long id = (Long)properties.get("image_query_id");
        if (id==null) return null;
        return id.intValue();
    }

    public void setSageId(Integer sageId) {
        properties.put("image_query_id", new Long(sageId));
    }

    
    public String getSlideCode() {
        return (String)properties.get("light_imagery_slide_code");
    }

    public void setSlideCode(String slideCode) {
        properties.put("light_imagery_slide_code", slideCode);
    }

    public String getImageName() {
        return (String)properties.get("image_query_name");
    }

    public void setImageName(String imageName) {
        properties.put("image_query_name", imageName);
    }

    public String getImagePath() {
        return (String)properties.get("image_query_path");
    }

    public void setImagePath(String imagePath) {
        properties.put("image_query_path", imagePath);
    }

    public String getJfsPath() {
        return (String)properties.get("image_query_jfs_path");
    }

    public void setJfsPath(String jfsPath) {
        properties.put("image_query_jfs_path", jfsPath);
    }

    public String getTileType() {
        return (String)properties.get("light_imagery_tile");
    }

    public void setTileType(String tileType) {
        properties.put("light_imagery_tile", tileType);
    }

    public String getLine() {
        return (String)properties.get("image_query_line");
    }

    public void setLine(String line) {
        properties.put("image_query_line", line);
    }

    public String getArea() {
        return (String)properties.get("light_imagery_area");
    }

    public void setArea(String area) {
        properties.put("light_imagery_area", area);
    }

    public String getTmogDate() {
        return (String)properties.get("tmog_date");
    }

    public void setTmogDate(String tmogDate) {
        properties.put("tmog_date", tmogDate);
    }

    public String getLab() {
        return (String)properties.get("image_lab_name");
    }

    public void setLab(String lab) {
        properties.put("image_lab_name", lab);
    }
    
    /**
     * Convert non-standard gender values like "Female" into standardized codes like "f". The
     * four standardized codes are "m", "f", "x", and "NO_CONSENSUS" in the case of samples.
     */
    private String sanitizeGender(String gender) {
        if (gender==null) {
            return null;
        }
        String genderLc = gender.toLowerCase();
        if (genderLc.startsWith("f")) {
            return "f";
        }
        else if (genderLc.startsWith("m")) {
            return "m";
        }
        else if (genderLc.startsWith("x")) {
            return "x";
        } 
        else {
            return null;
        }
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String dataSetName) {
        this.datasetName = dataSetName;
    }

    public String getCrossBarcode() {
        return crossBarCode;
    }

    public void setCrossBarcode(String crossBarCode) {
        this.crossBarCode = crossBarCode;
    }

    public String getChannelSpec() {
        return channelSpec;
    }

    public void setChannelSpec(String channelSpec) {
        this.channelSpec = channelSpec;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
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

    public String getEffector() {
        return effector;
    }

    public void setEffector(String effector) {
        this.effector = effector;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String[] getOpticalRes() {
        return opticalRes;
    }

    public void setOpticalRes(String opticalResX, String opticalResY, String opticalResZ) {
        this.opticalRes = new String[] {opticalResX, opticalResY, opticalResZ};
    }

    public void setOpticalRes(String[] opticalRes) {
        this.opticalRes = opticalRes;
    }

    public String[] getPixelRes() {
        return pixelRes;
    }

    public void setPixelRes(String pixelResX, String pixelResY, String pixelResZ) {
        this.pixelRes = new String[] {pixelResX, pixelResY, pixelResZ};
    }

    public void setPixelRes(String[] pixelRes) {
        this.pixelRes = pixelRes;
    }
}
