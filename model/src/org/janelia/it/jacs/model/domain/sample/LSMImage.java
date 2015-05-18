package org.janelia.it.jacs.model.domain.sample;

import java.util.Date;

import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchTraversal;
import org.janelia.it.jacs.model.domain.support.SearchType;

@SearchType(key="lsmImage",label="LSM Image")
public class LSMImage extends Image {

    @SearchTraversal({})
    private Reference sample;

    @SearchAttribute(key="chancolors_txt",label="Channel Colors")
    private String channelColors;

    @SearchAttribute(key="chandyes_txt",label="Channel Dye Names")
    private String channelDyeNames;

    // SAGE Terms

    @SAGEAttribute(cvName="image_query", termName="id")
    @SearchAttribute(key="sage_id_i",label="SAGE Id")
    private Integer sageId;

    @SAGEAttribute(cvName="image_query", termName="line")
    @SearchAttribute(key="line_txt",label="Fly Line")    
    private String line;

    @SAGEAttribute(cvName="light_imagery", termName="representative")
    @SearchAttribute(key="rep_b",label="Representative image?")
    private Boolean representative;
    
    @SAGEAttribute(cvName="light_imagery", termName="age")
    @SearchAttribute(key="age_s",label="Age",facet=true)
    private String age;

    @SAGEAttribute(cvName="light_imagery", termName="annotated_by")
    @SearchAttribute(key="annotatedby_s",label="Annotated By",facet=true)
    private String annotatedBy;

    @SAGEAttribute(cvName="light_imagery", termName="area")
    @SearchAttribute(key="area_s",label="Anatomical Area",facet=true)
    private String anatomicalArea;

    @SAGEAttribute(cvName="light_imagery", termName="bc_correction1")
    @SearchAttribute(key="bcc1_s",label="BC Correction 1")
    private String bcCorrection1;

    @SAGEAttribute(cvName="light_imagery", termName="bc_correction2")
    @SearchAttribute(key="bcc2_s",label="BC Correction 2")
    private String bcCorrection2;

    @SAGEAttribute(cvName="light_imagery", termName="bits_per_sample")
    @SearchAttribute(key="bps_s",label="Bits/Sample")
    private Integer bitsPerSample;

    @SAGEAttribute(cvName="light_imagery", termName="capture_date")
    @SearchAttribute(key="capture_dt",label="Capture Date")
    private Date captureDate;

    @SAGEAttribute(cvName="light_imagery", termName="channel_spec")
    @SearchAttribute(key="chanspec_s",label="Channel Specification",facet=true)
    private String chanSpec;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_detection_channel_1_detector_gain")
    @SearchAttribute(key="dc1_gain_d",label="Detection Channel #1 Detector Gain")
    private String detectionChannel1DetectorGain;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_detection_channel_2_detector_gain")
    @SearchAttribute(key="dc2_gain_d",label="Detection Channel #2 Detector Gain")
    private String detectionChannel2DetectorGain;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_detection_channel_3_detector_gain")
    @SearchAttribute(key="dc3_gain_d",label="Detection Channel #3 Detector Gain")
    private String detectionChannel3DetectorGain;

    @SAGEAttribute(cvName="light_imagery", termName="driver")
    @SearchAttribute(key="driver_txt",label="Driver")
    private String driver;

    @SAGEAttribute(cvName="light_imagery", termName="effector")
    @SearchAttribute(key="effector_txt",label="Effector/Reporter")
    private String effector;

    @SAGEAttribute(cvName="light_imagery", termName="gender")
    @SearchAttribute(key="gender_s",label="Gender",facet=true)
    private String gender;

    @SAGEAttribute(cvName="light_imagery", termName="full_age")
    @SearchAttribute(key="full_age_txt",label="Full Age")
    private String fullAge;

    @SAGEAttribute(cvName="light_imagery", termName="mount_protocol")
    @SearchAttribute(key="mount_protocol_txt",label="Mounting Protocol")
    private String mountingProtocol;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_illumination_channel_1_name")
    @SearchAttribute(key="ic1_name_s",label="Illumination Channel #1 Name")
    private String illuminationChannel1Name;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_illumination_channel_2_name")
    @SearchAttribute(key="ic2_name_s",label="Illumination Channel #2 Name")
    private String illuminationChannel2Name;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_illumination_channel_3_name")
    @SearchAttribute(key="ic3_name_s",label="Illumination Channel #3 Name")
    private String illuminationChannel3Name;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_illumination_channel_1_power_bc_1")
    @SearchAttribute(key="ic1_power_s",label="IlluminationChannel #1 Power B/C 1")
    private String illuminationChannel1PowerBC1;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_illumination_channel_2_power_bc_1")
    @SearchAttribute(key="ic2_power_s",label="IlluminationChannel #2 Power B/C 1")
    private String illuminationChannel2PowerBC1;

    @SAGEAttribute(cvName="light_imagery", termName="lsm_illumination_channel_3_power_bc_1")
    @SearchAttribute(key="ic3_power_s",label="IlluminationChannel #3 Power B/C 1")
    private String illuminationChannel3PowerBC1;

    @SAGEAttribute(cvName="light_imagery", termName="family")
    @SearchAttribute(key="family_txt",label="Image Family")
    private String imageFamily;

    @SAGEAttribute(cvName="light_imagery", termName="created_by")
    @SearchAttribute(key="created_by_txt",label="Imager")
    private String createdBy;

    @SAGEAttribute(cvName="light_imagery", termName="data_set")
    @SearchAttribute(key="data_set_txt",label="Imagery Data Set")
    private String dataSet;

    @SAGEAttribute(cvName="light_imagery", termName="imaging_project")
    @SearchAttribute(key="img_proj_txt",label="Imaging Project")
    private String imagingProject;

    @SAGEAttribute(cvName="light_imagery", termName="interpolation_elapsed")
    @SearchAttribute(key="intp_elapsed_s",label="Interpolation Elapsed Time")
    private String interpolationElapsed;

    @SAGEAttribute(cvName="light_imagery", termName="interpolation_start")
    @SearchAttribute(key="intp_start_i",label="Interpolation Start")
    private Integer interpolationStart;

    @SAGEAttribute(cvName="light_imagery", termName="interpolation_stop")
    @SearchAttribute(key="intp_stop_i",label="Interpolation Stop")
    private Integer interpolationStop;

    @SAGEAttribute(cvName="light_imagery", termName="microscope")
    @SearchAttribute(key="ms_txt",label="Microscope")
    private String microscope;

    @SAGEAttribute(cvName="light_imagery", termName="microscope_filename")
    @SearchAttribute(key="ms_filename_txt",label="Microscope Filename")
    private String microscopeFilename;

    @SAGEAttribute(cvName="light_imagery", termName="mac_address")
    @SearchAttribute(key="mac_address_s",label="Microscope MAC Address")
    private String macAddress;

    @SAGEAttribute(cvName="light_imagery", termName="objective")
    @SearchAttribute(key="objective_name_txt",label="Objective Name")
    private String objectiveName;

    @SAGEAttribute(cvName="light_imagery", termName="sample_0time")
    @SearchAttribute(key="sample_0time_d",label="Sample 0 Time")
    private String sampleZeroTime;

    @SAGEAttribute(cvName="light_imagery", termName="sample_0z")
    @SearchAttribute(key="sample_0z_d",label="Sample 0 Z")
    private String sampleZeroZ;

    @SAGEAttribute(cvName="light_imagery", termName="scan_start")
    @SearchAttribute(key="scan_start_i",label="Scan Start")
    private Integer scanStart;

    @SAGEAttribute(cvName="light_imagery", termName="scan_stop")
    @SearchAttribute(key="scan_stop_i",label="Scan Stop")
    private Integer scanStop;

    @SAGEAttribute(cvName="light_imagery", termName="scan_type")
    @SearchAttribute(key="scan_type_txt",label="Scan Type")
    private String scanType;

    @SAGEAttribute(cvName="light_imagery", termName="screen_state")
    @SearchAttribute(key="screen_state_txt",label="Screen State")
    private String screenState;

    @SAGEAttribute(cvName="light_imagery", termName="slide_code")
    @SearchAttribute(key="slide_code_txt",label="Slide Code")
    private String slideCode;

    @SAGEAttribute(cvName="light_imagery", termName="tile")
    @SearchAttribute(key="tile_txt",label="Tile")
    private String tile;

    @SAGEAttribute(cvName="light_imagery", termName="tissue_orientation")
    @SearchAttribute(key="orientation_txt",label="Tissue Orientation")
    private String tissueOrientation;

    @SAGEAttribute(cvName="light_imagery", termName="total_pixels")
    @SearchAttribute(key="total_pixels_i",label="Total Pixels")
    private Integer totalPixels;

    @SAGEAttribute(cvName="light_imagery", termName="tracks")
    @SearchAttribute(key="tracks_i",label="Tracks")
    private Integer tracks;

    @SAGEAttribute(cvName="light_imagery", termName="voxel_size_x")
    @SearchAttribute(key="vs_x_s",label="Voxel Size X")
    private String voxelSizeX;

    @SAGEAttribute(cvName="light_imagery", termName="voxel_size_y")
    @SearchAttribute(key="vs_y_s",label="Voxel Size Y")
    private String voxelSizeY;

    @SAGEAttribute(cvName="light_imagery", termName="voxel_size_z")
    @SearchAttribute(key="vs_z_s",label="Voxel Size Z")
    private String voxelSizeZ;

    @SAGEAttribute(cvName="light_imagery", termName="dimension_x")
    @SearchAttribute(key="dim_x_s",label="X Dimension")
    private String dimensionX;

    @SAGEAttribute(cvName="light_imagery", termName="dimension_y")
    @SearchAttribute(key="dim_y_s",label="Y Dimension")
    private String dimensionY;

    @SAGEAttribute(cvName="light_imagery", termName="dimension_z")
    @SearchAttribute(key="dim_z_s",label="Z Dimension")
    private String dimensionZ;

    @SAGEAttribute(cvName="light_imagery", termName="zoom_x")
    @SearchAttribute(key="zoom_x_s",label="Zoom X")
    private String zoomX;

    @SAGEAttribute(cvName="light_imagery", termName="zoom_y")
    @SearchAttribute(key="zoom_y_s",label="Zoom Y")
    private String zoomY;

    @SAGEAttribute(cvName="light_imagery", termName="zoom_z")
    @SearchAttribute(key="zoom_z_s",label="Zoom Z")
    private String zoomZ;

    @SAGEAttribute(cvName="light_imagery", termName="vt_line")
    @SearchAttribute(key="vtline_txt",label="VT Line")
    private String vtLine;

    @SAGEAttribute(cvName="line_query", termName="organism")
    @SearchAttribute(key="organism_txt",label="Organism")
    private String organism;
    
    @SAGEAttribute(cvName="line", termName="genotype")
    @SearchAttribute(key="genotype_txt",label="Genotype")
    private String genotype;

    @SAGEAttribute(cvName="line", termName="flycore_id")
    @SearchAttribute(key="flycore_id_i",label="Fly Core Id")
    private Integer flycoreId;

    @SAGEAttribute(cvName="line", termName="flycore_lab")
    @SearchAttribute(key="fclab_s",label="Fly Core Lab Id")
    private String flycoreLabId;
    
    @SAGEAttribute(cvName="line", termName="flycore_landing_site")
    @SearchAttribute(key="fclanding_txt",label="Fly Core Landing Site")
    private String flycoreLandingSite;

    @SAGEAttribute(cvName="line", termName="flycore_permission")
    @SearchAttribute(key="fcpermn_txt",label="Fly Core Permission")
    private String flycorePermission;
    
    @SAGEAttribute(cvName="line", termName="flycore_project")
    @SearchAttribute(key="fcproj_txt",label="Fly Core Project")
    private String flycoreProject;
    
    @SAGEAttribute(cvName="line", termName="hide")
    @SearchAttribute(key="linehide_txt",label="Hide Line?",display=false)
    private String lineHide;

    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    
    public Reference getSample() {
        return sample;
    }

    public void setSample(Reference sample) {
        this.sample = sample;
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

    public Integer getSageId() {
        return sageId;
    }

    public void setSageId(Integer sageId) {
        this.sageId = sageId;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public Boolean getRepresentative() {
        return representative;
    }

    public void setRepresentative(Boolean representative) {
        this.representative = representative;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAnnotatedBy() {
        return annotatedBy;
    }

    public void setAnnotatedBy(String annotatedBy) {
        this.annotatedBy = annotatedBy;
    }

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public String getBcCorrection1() {
        return bcCorrection1;
    }

    public void setBcCorrection1(String bcCorrection1) {
        this.bcCorrection1 = bcCorrection1;
    }

    public String getBcCorrection2() {
        return bcCorrection2;
    }

    public void setBcCorrection2(String bcCorrection2) {
        this.bcCorrection2 = bcCorrection2;
    }

    public Integer getBitsPerSample() {
        return bitsPerSample;
    }

    public void setBitsPerSample(Integer bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public Date getCaptureDate() {
        return captureDate;
    }

    public void setCaptureDate(Date captureDate) {
        this.captureDate = captureDate;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public String getDetectionChannel1DetectorGain() {
        return detectionChannel1DetectorGain;
    }

    public void setDetectionChannel1DetectorGain(String detectionChannel1DetectorGain) {
        this.detectionChannel1DetectorGain = detectionChannel1DetectorGain;
    }

    public String getDetectionChannel2DetectorGain() {
        return detectionChannel2DetectorGain;
    }

    public void setDetectionChannel2DetectorGain(String detectionChannel2DetectorGain) {
        this.detectionChannel2DetectorGain = detectionChannel2DetectorGain;
    }

    public String getDetectionChannel3DetectorGain() {
        return detectionChannel3DetectorGain;
    }

    public void setDetectionChannel3DetectorGain(String detectionChannel3DetectorGain) {
        this.detectionChannel3DetectorGain = detectionChannel3DetectorGain;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
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

    public String getFullAge() {
        return fullAge;
    }

    public void setFullAge(String fullAge) {
        this.fullAge = fullAge;
    }

    public String getMountingProtocol() {
        return mountingProtocol;
    }

    public void setMountingProtocol(String mountingProtocol) {
        this.mountingProtocol = mountingProtocol;
    }

    public String getIlluminationChannel1Name() {
        return illuminationChannel1Name;
    }

    public void setIlluminationChannel1Name(String illuminationChannel1Name) {
        this.illuminationChannel1Name = illuminationChannel1Name;
    }

    public String getIlluminationChannel2Name() {
        return illuminationChannel2Name;
    }

    public void setIlluminationChannel2Name(String illuminationChannel2Name) {
        this.illuminationChannel2Name = illuminationChannel2Name;
    }

    public String getIlluminationChannel3Name() {
        return illuminationChannel3Name;
    }

    public void setIlluminationChannel3Name(String illuminationChannel3Name) {
        this.illuminationChannel3Name = illuminationChannel3Name;
    }

    public String getIlluminationChannel1PowerBC1() {
        return illuminationChannel1PowerBC1;
    }

    public void setIlluminationChannel1PowerBC1(String illuminationChannel1PowerBC1) {
        this.illuminationChannel1PowerBC1 = illuminationChannel1PowerBC1;
    }

    public String getIlluminationChannel2PowerBC1() {
        return illuminationChannel2PowerBC1;
    }

    public void setIlluminationChannel2PowerBC1(String illuminationChannel2PowerBC1) {
        this.illuminationChannel2PowerBC1 = illuminationChannel2PowerBC1;
    }

    public String getIlluminationChannel3PowerBC1() {
        return illuminationChannel3PowerBC1;
    }

    public void setIlluminationChannel3PowerBC1(String illuminationChannel3PowerBC1) {
        this.illuminationChannel3PowerBC1 = illuminationChannel3PowerBC1;
    }

    public String getImageFamily() {
        return imageFamily;
    }

    public void setImageFamily(String imageFamily) {
        this.imageFamily = imageFamily;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getImagingProject() {
        return imagingProject;
    }

    public void setImagingProject(String imagingProject) {
        this.imagingProject = imagingProject;
    }

    public String getInterpolationElapsed() {
        return interpolationElapsed;
    }

    public void setInterpolationElapsed(String interpolationElapsed) {
        this.interpolationElapsed = interpolationElapsed;
    }

    public Integer getInterpolationStart() {
        return interpolationStart;
    }

    public void setInterpolationStart(Integer interpolationStart) {
        this.interpolationStart = interpolationStart;
    }

    public Integer getInterpolationStop() {
        return interpolationStop;
    }

    public void setInterpolationStop(Integer interpolationStop) {
        this.interpolationStop = interpolationStop;
    }

    public String getMicroscope() {
        return microscope;
    }

    public void setMicroscope(String microscope) {
        this.microscope = microscope;
    }

    public String getMicroscopeFilename() {
        return microscopeFilename;
    }

    public void setMicroscopeFilename(String microscopeFilename) {
        this.microscopeFilename = microscopeFilename;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public void setObjectiveName(String objectiveName) {
        this.objectiveName = objectiveName;
    }

    public String getSampleZeroTime() {
        return sampleZeroTime;
    }

    public void setSampleZeroTime(String sampleZeroTime) {
        this.sampleZeroTime = sampleZeroTime;
    }

    public String getSampleZeroZ() {
        return sampleZeroZ;
    }

    public void setSampleZeroZ(String sampleZeroZ) {
        this.sampleZeroZ = sampleZeroZ;
    }

    public Integer getScanStart() {
        return scanStart;
    }

    public void setScanStart(Integer scanStart) {
        this.scanStart = scanStart;
    }

    public Integer getScanStop() {
        return scanStop;
    }

    public void setScanStop(Integer scanStop) {
        this.scanStop = scanStop;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getScreenState() {
        return screenState;
    }

    public void setScreenState(String screenState) {
        this.screenState = screenState;
    }

    public String getSlideCode() {
        return slideCode;
    }

    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }

    public String getTile() {
        return tile;
    }

    public void setTile(String tile) {
        this.tile = tile;
    }

    public String getTissueOrientation() {
        return tissueOrientation;
    }

    public void setTissueOrientation(String tissueOrientation) {
        this.tissueOrientation = tissueOrientation;
    }

    public Integer getTotalPixels() {
        return totalPixels;
    }

    public void setTotalPixels(Integer totalPixels) {
        this.totalPixels = totalPixels;
    }

    public Integer getTracks() {
        return tracks;
    }

    public void setTracks(Integer tracks) {
        this.tracks = tracks;
    }

    public String getVoxelSizeX() {
        return voxelSizeX;
    }

    public void setVoxelSizeX(String voxelSizeX) {
        this.voxelSizeX = voxelSizeX;
    }

    public String getVoxelSizeY() {
        return voxelSizeY;
    }

    public void setVoxelSizeY(String voxelSizeY) {
        this.voxelSizeY = voxelSizeY;
    }

    public String getVoxelSizeZ() {
        return voxelSizeZ;
    }

    public void setVoxelSizeZ(String voxelSizeZ) {
        this.voxelSizeZ = voxelSizeZ;
    }

    public String getDimensionX() {
        return dimensionX;
    }

    public void setDimensionX(String dimensionX) {
        this.dimensionX = dimensionX;
    }

    public String getDimensionY() {
        return dimensionY;
    }

    public void setDimensionY(String dimensionY) {
        this.dimensionY = dimensionY;
    }

    public String getDimensionZ() {
        return dimensionZ;
    }

    public void setDimensionZ(String dimensionZ) {
        this.dimensionZ = dimensionZ;
    }

    public String getZoomX() {
        return zoomX;
    }

    public void setZoomX(String zoomX) {
        this.zoomX = zoomX;
    }

    public String getZoomY() {
        return zoomY;
    }

    public void setZoomY(String zoomY) {
        this.zoomY = zoomY;
    }

    public String getZoomZ() {
        return zoomZ;
    }

    public void setZoomZ(String zoomZ) {
        this.zoomZ = zoomZ;
    }

    public String getVtLine() {
        return vtLine;
    }

    public void setVtLine(String vtLine) {
        this.vtLine = vtLine;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getGenotype() {
        return genotype;
    }

    public void setGenotype(String genotype) {
        this.genotype = genotype;
    }

    public Integer getFlycoreId() {
        return flycoreId;
    }

    public void setFlycoreId(Integer flycoreId) {
        this.flycoreId = flycoreId;
    }

    public String getFlycoreLabId() {
        return flycoreLabId;
    }

    public void setFlycoreLabId(String flycoreLabId) {
        this.flycoreLabId = flycoreLabId;
    }

    public String getFlycoreLandingSite() {
        return flycoreLandingSite;
    }

    public void setFlycoreLandingSite(String flycoreLandingSite) {
        this.flycoreLandingSite = flycoreLandingSite;
    }

    public String getFlycorePermission() {
        return flycorePermission;
    }

    public void setFlycorePermission(String flycorePermission) {
        this.flycorePermission = flycorePermission;
    }

    public String getFlycoreProject() {
        return flycoreProject;
    }

    public void setFlycoreProject(String flycoreProject) {
        this.flycoreProject = flycoreProject;
    }

    public String getLineHide() {
        return lineHide;
    }

    public void setLineHide(String lineHide) {
        this.lineHide = lineHide;
    }
}
