package org.janelia.it.jacs.model.domain;

public class NeuronFragment {

    private Integer number;
    private String signalMipImage;
    private String maskFile;
    private String chanFile;
    
    public Integer getNumber() {
        return number;
    }
    public void setNumber(Integer number) {
        this.number = number;
    }
    public String getSignalMipImage() {
        return signalMipImage;
    }
    public void setSignalMipImage(String signalMipImage) {
        this.signalMipImage = signalMipImage;
    }
    public String getMaskFile() {
        return maskFile;
    }
    public void setMaskFile(String maskFile) {
        this.maskFile = maskFile;
    }
    public String getChanFile() {
        return chanFile;
    }
    public void setChanFile(String chanFile) {
        this.chanFile = chanFile;
    }
    
    
}
