package org.janelia.it.jacs.compute.service.align;

import java.io.Serializable;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * All the metadata needed to use a file as an input into an alignment pipeline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentInputFile implements Serializable {

    protected String inputFilename;
    protected String inputSeparationFilename;
    protected String opticalResolution;
    protected String pixelResolution;
    protected String channelSpec;
    protected String channelColors;
    protected Integer refChannel;
    protected Integer refChannelOneIndexed;
    protected Integer numChannels;

    public void setPropertiesFromEntity(Entity image) {
        setInputFilename(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        setChannelColors(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
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
    public String getChannelColors() {
        return channelColors;
    }
    public void setChannelColors(String channelColors) {
        this.channelColors = channelColors;
    }
    public String getChannelSpec() {
        return channelSpec;
    }
    public void setChannelSpec(String channelSpec) {
        this.channelSpec = channelSpec;
        if (channelSpec!=null) {
            if (channelSpec.contains("r")) {
                setRefChannel(channelSpec.indexOf('r'));
                setRefChannelOneIndexed(refChannel + 1);
            }
            setNumChannels(channelSpec.length());
        }
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

    @Override
    public String toString() {
        return "AlignmentInputFile{" +
                "inputFilename='" + inputFilename + '\'' +
                ", inputSeparationFilename='" + inputSeparationFilename + '\'' +
                ", opticalResolution='" + opticalResolution + '\'' +
                ", pixelResolution='" + pixelResolution + '\'' +
                ", channelSpec='" + channelSpec + '\'' +
                ", channelColors='" + channelColors + '\'' +
                ", refChannel=" + refChannel +
                ", refChannelOneIndexed=" + refChannelOneIndexed +
                ", numChannels=" + numChannels +
                '}';
    }
}
