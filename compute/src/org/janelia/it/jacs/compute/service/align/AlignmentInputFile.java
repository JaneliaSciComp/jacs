package org.janelia.it.jacs.compute.service.align;

/**
 * All the metadata needed to use a file as an input into an alignment pipeline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentInputFile extends ImageStack {

    protected Long sampleId;
    protected String inputSeparationFilename;
    
    public AlignmentInputFile() {
    }
    
    public Long getSampleId() {
        return sampleId;
    }
    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }
    public String getInputSeparationFilename() {
        return inputSeparationFilename;
    }
    public void setInputSeparationFilename(String inputSeparationFilename) {
        this.inputSeparationFilename = inputSeparationFilename;
    }
    
    @Override
    public String toString() {
        return "AlignmentInputFile{" +
                "filepath=" + filepath +
                ", objective=" + objective +
                ", opticalResolution=" + opticalResolution +
                ", pixelResolution=" + pixelResolution + 
                ", channelSpec=" + channelSpec +
                ", channelColors=" + channelColors +
                ", sampleId=" + sampleId +
                ", inputSeparationFilename=" + inputSeparationFilename +
                '}';
    }
}
