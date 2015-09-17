package org.janelia.it.jacs.compute.service.image;

import java.io.Serializable;

/**
 * Metadata and options for an input file to a Fiji macro. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InputImage implements Serializable {

    private String filepath;
    private String outputPrefix;
    private String chanspec;
    private String colorspec;
    private String divspec;
    private Integer laser;
    private Integer gain;
    private String area;
    
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public String getOutputPrefix() {
        return outputPrefix;
    }
    public void setOutputPrefix(String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }
    public String getChanspec() {
        return chanspec;
    }
    public void setChanspec(String chanspec) {
        this.chanspec = chanspec;
    }
    public String getColorspec() {
        return colorspec;
    }
    public void setColorspec(String colorspec) {
        this.colorspec = colorspec;
    }
    public String getDivspec() {
        return divspec;
    }
    public void setDivspec(String divspec) {
        this.divspec = divspec;
    }
    public Integer getLaser() {
        return laser;
    }
    public void setLaser(Integer laser) {
        this.laser = laser;
    }
    public Integer getGain() {
        return gain;
    }
    public void setGain(Integer gain) {
        this.gain = gain;
    }
    public String getArea() {
        return area;
    }
    public void setArea(String area) {
        this.area = area;
    }
    @Override
    public String toString() {
        return "InputImage [filepath=" + filepath + "]";
    }
}
