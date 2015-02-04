package org.janelia.it.jacs.compute.service.align;

/**
 * All the metadata needed to use a file as an input into an alignment pipeline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentInputFile extends ImageStack {

    protected String inputSeparationFilename;

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
                ", inputSeparationFilename=" + inputSeparationFilename +
                ", opticalResolution=" + opticalResolution +
                ", pixelResolution=" + pixelResolution + 
                ", channelSpec=" + channelSpec +
                ", channelColors=" + channelColors + 
                ", refChannel=" + refChannel +
                ", refChannelOneIndexed=" + refChannelOneIndexed +
                ", numChannels=" + numChannels +
                '}';
    }
}
