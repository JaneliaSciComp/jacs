package org.janelia.it.jacs.compute.service.entity.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

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
    
    public String getConsensusLsmAttributeValue(Entity sampleEntity, String attrName) throws Exception {
        String consensus = null;
        entityLoader.populateChildren(sampleEntity);
        Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
        entityLoader.populateChildren(supportingData);
        for(Entity tile : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {
            entityLoader.populateChildren(tile);
            for(Entity image : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {    
                String imageChanSpec = image.getValueByAttributeName(attrName);
                if (consensus!=null && !consensus.equals(imageChanSpec)) {
                    logger.info("No consensus for attribute '"+attrName+"' can be reached for sample "+sampleEntity.getId());
                    return null;
                }
                else {
                    consensus = imageChanSpec;
                }
            }
        }
        return consensus;
    }
    
}
