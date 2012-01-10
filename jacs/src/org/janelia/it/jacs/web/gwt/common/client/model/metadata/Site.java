
package org.janelia.it.jacs.web.gwt.common.client.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

public class Site implements IsSerializable {
    // Global metadata
    private String siteId;
    private String project;
    private String sampleLocation;
    private String longitude;
    private String latitude;
    private Double latitudeDouble;
    private Double longitudeDouble;
    private Date dataTimestamp;  // obsolete??
    private Date startTime;
    private Date stopTime;

    // Host Organism metadata
    private String hostOrganism;
    private String hostDetails;

    // GOS metadata
    private String chlorophyllDensity;
    private String country;
    private String geographicLocation;
    private String habitatType;
    private String leg;
    private String salinity;
    private String sampleDepth;
    private String temperature;
    private String waterDepth;

    // HOT-specific metadata
    private String biomass;
    private String dissolvedOxygen;
    private String dissolvedInorganicCarbon;
    private String dissolvedInorganicPhospate;
    private String dissolvedOrganicCarbon;
    private String nitrate_plus_nitrite;

    // MOVE-specific metadata
    private String fluorescence;
    private String transmission;

    // Viral-specific metadata
    private String geographicLocationDetail;
    private String numberOfSamplesPooled;
    private String numberOfStationsSampled;

    private String volume_filtered;

    /**
     * no-arg constructor, required for GWT
     */
    public Site() {
        super();
    }

    public Site(String biomass, String chlorophyllDensity, String country, Date dataTimestamp, String dissolved_inorganic_carbon,
                String dissolved_inorganic_phospate, String dissolved_organic_carbon, String dissolved_oxygen, String dissolvedOxygen,
                String fluorescence, String geographic_location_detail, String geographicLocation, String habitatType, String latitude,
                Double latitudeDouble, String leg, String longitude, Double longitudeDouble, String nitrate_plus_nitrite,
                String number_of_samples_pooled, String number_of_stations_sampled, String project, String salinity, String sampleDepth, String sampleLocation, String siteId, Date start_time, Date stop_time,
                String temperature, String transmission, String volume_filtered, String waterDepth) {
        this.biomass = biomass;
        this.chlorophyllDensity = chlorophyllDensity;
        this.country = country;
        this.dataTimestamp = dataTimestamp;
        this.dissolvedInorganicCarbon = dissolved_inorganic_carbon;
        this.dissolvedInorganicPhospate = dissolved_inorganic_phospate;
        this.dissolvedOrganicCarbon = dissolved_organic_carbon;
        this.dissolvedOxygen = dissolved_oxygen;
        this.dissolvedOxygen = dissolvedOxygen;
        this.fluorescence = fluorescence;
        this.geographicLocationDetail = geographic_location_detail;
        this.geographicLocation = geographicLocation;
        this.habitatType = habitatType;
        this.latitude = latitude;
        this.latitudeDouble = latitudeDouble;
        this.leg = leg;
        this.longitude = longitude;
        this.longitudeDouble = longitudeDouble;
        this.nitrate_plus_nitrite = nitrate_plus_nitrite;
        this.numberOfSamplesPooled = number_of_samples_pooled;
        this.numberOfStationsSampled = number_of_stations_sampled;
        this.project = project;
        this.salinity = salinity;
        this.sampleDepth = sampleDepth;
        this.sampleLocation = sampleLocation;
        this.siteId = siteId;
        this.startTime = start_time;
        this.stopTime = stop_time;
        this.temperature = temperature;
        this.transmission = transmission;
        this.volume_filtered = volume_filtered;
        this.waterDepth = waterDepth;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSampleLocation() {
        return sampleLocation;
    }

    public void setSampleLocation(String sampleLocation) {
        this.sampleLocation = sampleLocation;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGeographicLocation() {
        return geographicLocation;
    }

    public void setGeographicLocation(String geographicLocation) {
        this.geographicLocation = geographicLocation;
    }

    public String getLeg() {
        return leg;
    }

    public void setLeg(String leg) {
        this.leg = leg;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getFormattedLatitude() {
        return latitude.replace('d', '\u00B0');
    }

    public String getFormattedLongitude() {
        return longitude.replace('d', '\u00B0');
    }

    public Date getDataTimestamp() {
        return dataTimestamp;
    }

    public void setDataTimestamp(Date dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }

    public String getSampleDepth() {
        return sampleDepth;
    }

    public void setSampleDepth(String sampleDepth) {
        this.sampleDepth = sampleDepth;
    }

    public String getWaterDepth() {
        return waterDepth;
    }

    public void setWaterDepth(String waterDepth) {
        this.waterDepth = waterDepth;
    }

    public String getHabitatType() {
        return habitatType;
    }

    public void setHabitatType(String habitatType) {
        this.habitatType = habitatType;
    }

    public String getChlorophyllDensity() {
        return chlorophyllDensity;
    }

    public void setChlorophyllDensity(String chlorophyllDensity) {
        this.chlorophyllDensity = chlorophyllDensity;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getSalinity() {
        return salinity;
    }

    public void setSalinity(String salinity) {
        this.salinity = salinity;
    }

    public String getFluorescence() {
        return fluorescence;
    }

    public void setFluorescence(String fluorescence) {
        this.fluorescence = fluorescence;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getDissolvedOxygen() {
        return dissolvedOxygen;
    }

    public void setDissolvedOxygen(String dissolvedOxygen) {
        this.dissolvedOxygen = dissolvedOxygen;
    }

    public String getBiomass() {
        return biomass;
    }

    public void setBiomass(String biomass) {
        this.biomass = biomass;
    }

    public String getDissolvedInorganicCarbon() {
        return dissolvedInorganicCarbon;
    }

    public void setDissolvedInorganicCarbon(String dissolvedInorganicCarbon) {
        this.dissolvedInorganicCarbon = dissolvedInorganicCarbon;
    }

    public String getDissolvedInorganicPhospate() {
        return dissolvedInorganicPhospate;
    }

    public void setDissolvedInorganicPhospate(String dissolvedInorganicPhospate) {
        this.dissolvedInorganicPhospate = dissolvedInorganicPhospate;
    }

    public String getDissolvedOrganicCarbon() {
        return dissolvedOrganicCarbon;
    }

    public void setDissolvedOrganicCarbon(String dissolvedOrganicCarbon) {
        this.dissolvedOrganicCarbon = dissolvedOrganicCarbon;
    }

    public String getGeographicLocationDetail() {
        return geographicLocationDetail;
    }

    public void setGeographicLocationDetail(String geographicLocationDetail) {
        this.geographicLocationDetail = geographicLocationDetail;
    }

    public String getNitrate_plus_nitrite() {
        return nitrate_plus_nitrite;
    }

    public void setNitrate_plus_nitrite(String nitrate_plus_nitrite) {
        this.nitrate_plus_nitrite = nitrate_plus_nitrite;
    }

    public String getNumberOfSamplesPooled() {
        return numberOfSamplesPooled;
    }

    public void setNumberOfSamplesPooled(String numberOfSamplesPooled) {
        this.numberOfSamplesPooled = numberOfSamplesPooled;
    }

    public String getNumberOfStationsSampled() {
        return numberOfStationsSampled;
    }

    public void setNumberOfStationsSampled(String numberOfStationsSampled) {
        this.numberOfStationsSampled = numberOfStationsSampled;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public void setStopTime(Date stopTime) {
        this.stopTime = stopTime;
    }

    public String getVolume_filtered() {
        return volume_filtered;
    }

    public void setVolume_filtered(String volume_filtered) {
        this.volume_filtered = volume_filtered;
    }

    public Double getLongitudeDouble() {
        return longitudeDouble;
    }

    public void setLongitudeDouble(Double longitudeDouble) {
        this.longitudeDouble = longitudeDouble;
    }

    public Double getLatitudeDouble() {
        return latitudeDouble;
    }

    public void setLatitudeDouble(Double latitudeDouble) {
        this.latitudeDouble = latitudeDouble;
    }

    public String getHostOrganism() {
        return hostOrganism;
    }

    public void setHostOrganism(String hostOrganism) {
        this.hostOrganism = hostOrganism;
    }

    public String getHostDetails() {
        return hostDetails;
    }

    public void setHostDetails(String hostDetails) {
        this.hostDetails = hostDetails;
    }
}
