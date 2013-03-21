package org.janelia.it.jacs.compute.service.entity.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Helper methods for dealing with Samples.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleHelper extends EntityHelper {

    protected Logger logger = Logger.getLogger(SampleHelper.class);
    
	public SampleHelper(String ownerKey) {
		super(ownerKey);
	}
	
    public SampleHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, String ownerKey) {
        super(entityBean, computeBean, ownerKey, Logger.getLogger(SampleHelper.class));
    }
    
    public SampleHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, String ownerKey, Logger logger) {
        super(entityBean, computeBean, ownerKey, logger);
    }

    /**
     * Returns a default channel specification (reference channel last) for the given number of channels.
     * @param numSignals
     * @return
     */
    public String getDefaultChanSpec(int numChannels) {
        int numSignals = numChannels-1;
        StringBuilder buf = new StringBuilder();
        for(int j=0; j<numSignals; j++) {
            buf.append("s");
        }
        buf.append("r");
        return buf.toString();
    }
    
    /**
     * Return the channel specification for the LSM (or create a default one using the number of channels).
     * @param lsmEntity
     * @return
     */
    public String getLSMChannelSpec(Entity lsmEntity) {
        String chanSpec = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (!StringUtils.isEmpty(chanSpec)) return chanSpec;
        String numChannelsStr = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        if (!StringUtils.isEmpty(numChannelsStr)) {
            try {
                return getDefaultChanSpec(Integer.parseInt(numChannelsStr));    
            }
            catch (NumberFormatException e) {
                logger.warn("Could not parse Num Channels ('"+numChannelsStr+"') on LSM entity with id="+lsmEntity.getId());
            }
        }
        throw new IllegalStateException("LSM has no Channel Specification and no Num Channels");
    }
    
    /**
     * Go through a sample area's LSM supporting files and look for an entity attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param sampleEntity
     * @param attrName
     * @param areaName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(Entity sampleEntity, String attrName, String areaName) throws Exception {
        String consensus = null;
        entityLoader.populateChildren(sampleEntity);
        Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
        entityLoader.populateChildren(supportingData);
        for(Entity tile : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {
            entityLoader.populateChildren(tile);
            for(Entity image : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {    
                String lsmArea = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                if (areaName==null || areaName.equals(lsmArea)) {
                    String value = image.getValueByAttributeName(attrName);
                    if (consensus!=null && !consensus.equals(value)) {
                        logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample "+sampleEntity.getId());
                        return null;
                    }
                    else {
                        consensus = value;
                    }
                }
            }
        }
        return consensus;
    }

    /**
     * Returns a space-delimited list of channel indexes containing signal channels.
     * @param channelSpec channel specification (e.g. "rsss")
     * @return zero-indexed signal channels (e.g. "1 2 3")
     */
    public String getSignalChannelIndexes(String channelSpec) {
        return getChannelIndexes(channelSpec, 's');
    }

    /**
     * Returns a space-delimited list of channel indexes containing reference channels.
     * @param channelSpec channel specification (e.g. rsss)
     * @return zero-indexed reference channels (e.g. "0")
     */
    public String getReferenceChannelIndexes(String channelSpec) {
        return getChannelIndexes(channelSpec, 'r');
    }
    
    private String getChannelIndexes(String channelSpec, char channelCode) {
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<channelSpec.length(); i++) {
            if (channelSpec.charAt(i) == channelCode) {
                if (builder.length()>0) builder.append(" ");
                builder.append(""+i);
            }
        }
        return builder.toString();
    }
}
