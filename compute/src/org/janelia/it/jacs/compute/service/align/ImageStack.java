package org.janelia.it.jacs.compute.service.align;

import java.io.Serializable;

import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * All the metadata needed for an image stack.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ImageStack implements Serializable {

    protected String filepath;
    protected String opticalResolution;
    protected String pixelResolution;
    protected String channelSpec;
    protected String channelColors;
    protected Integer refChannel;
    protected Integer refChannelOneIndexed;
    protected Integer numChannels;

    public void setPropertiesFromEntity(Entity image) {
        setFilepath(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        setChannelColors(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS));
        setChannelSpec(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION));
        setOpticalResolution(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION));
        setPixelResolution(image.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
    }

    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String inputFilename) {
        this.filepath = inputFilename;
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
        	int r = channelSpec.indexOf(ChanSpecUtils.REFERENCE);
            if (r>=0) {
                setRefChannel(r);
                setRefChannelOneIndexed(r + 1);
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
        return "ImageStack{" +
                "filepath=" + filepath +
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
