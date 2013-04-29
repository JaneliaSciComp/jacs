package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * All the metadata needed to use a file as an input into an alignment pipeline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentInputFile {

    protected String inputFilename;
    protected String inputSeparationFilename;
    protected String opticalResolution;
    protected String pixelResolution;
    protected String channelSpec;
    protected Integer refChannel;
    protected Integer refChannelOneIndexed;
    protected Integer numChannels;
    
    public void setPropertiesFromEntity(Entity image) {
        setInputFilename(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        setChannelSpec(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        setOpticalResolution(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
        setPixelResolution(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
    }
    
    public String getInputFilename() {
        return inputFilename;
    }
    public void setInputFilename(String inputFilename) {
        this.inputFilename = inputFilename;
    }
    public String getInputSeparationFilename() {
        return inputSeparationFilename;
    }
    public void setInputSeparationFilename(String inputSeparationFilename) {
        this.inputSeparationFilename = inputSeparationFilename;
    }
    public String getOpticalResolution() {
        return opticalResolution;
    }
    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution==null?null:opticalResolution.replaceAll(" ", "x");
    }
    public String getPixelResolution() {
        return pixelResolution;
    }
    public void setPixelResolution(String pixelResolution) {
        this.pixelResolution = pixelResolution==null?null:pixelResolution.replaceAll(" ", "x");
    }
    public String getChannelSpec() {
        return channelSpec;
    }
    public void setChannelSpec(String channelSpec) {
        this.channelSpec = channelSpec;
        if (channelSpec.contains("r")) {
            this.refChannel = channelSpec.indexOf('r');
            this.refChannelOneIndexed = refChannel + 1;
        }
        this.numChannels = channelSpec.length();
    }
    public Integer getRefChannel() {
        return refChannel;
    }
    public void setRefChannel(Integer refChannel) {
        this.refChannel = refChannel;
    }
    public Integer getRefChannelOneIndexed() {
        return refChannelOneIndexed;
    }
    public void setRefChannelOneIndexed(Integer refChannelOneIndexed) {
        this.refChannelOneIndexed = refChannelOneIndexed;
    }
    public Integer getNumChannels() {
        return numChannels;
    }
    public void setNumChannels(Integer numChannels) {
        this.numChannels = numChannels;
    }
}
