package org.janelia.it.jacs.model.domain;

public class Image3d {
    
    private String file;
    private String movieFile;
    private String signalMipFilepath;
    private String referenceMipFilepath;
    private String imageSize;
    private String opticalResolution;
    
    public String getFile() {
        return file;
    }
    public void setFile(String file) {
        this.file = file;
    }
    public String getMovieFile() {
        return movieFile;
    }
    public void setMovieFile(String movieFile) {
        this.movieFile = movieFile;
    }
    public String getSignalMipFilepath() {
        return signalMipFilepath;
    }
    public void setSignalMipFilepath(String signalMipFilepath) {
        this.signalMipFilepath = signalMipFilepath;
    }
    public String getReferenceMipFilepath() {
        return referenceMipFilepath;
    }
    public void setReferenceMipFilepath(String referenceMipFilepath) {
        this.referenceMipFilepath = referenceMipFilepath;
    }
    public String getImageSize() {
        return imageSize;
    }
    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
    public String getOpticalResolution() {
        return opticalResolution;
    }
    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }
    
    
    
    
}
