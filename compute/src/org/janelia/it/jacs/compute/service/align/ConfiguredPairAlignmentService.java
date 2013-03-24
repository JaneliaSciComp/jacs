package org.janelia.it.jacs.compute.service.align;

import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.user_data.Subject;

/**
 * A configured aligner which takes additional parameters to align the 63x image to a whole brain 20x image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredPairAlignmentService extends ConfiguredAlignmentService {

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    protected String ownerKey;
    protected SampleHelper sampleHelper;
    protected EntityBeanEntityLoader entityLoader;

    protected String brain20xFilename;
    protected int refChannel20xOneIndexed;
    
    @Override
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        try {
            this.entityBean = EJBFactory.getLocalEntityBean();
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            
            String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
            if (sampleEntityId == null || "".equals(sampleEntityId)) {
                throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
            }
            
            Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
            if (sampleEntity == null) {
                throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
            }
            
            if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityType().getName())) {
                throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
            }

            for(Entity objectiveSample : sampleEntity.getChildren()) {
                String objective = objectiveSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                String filename = objectiveSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                
                if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                    logger.info("Found 20x sub-sample: "+objectiveSample.getName());
                    if (filename!=null) {
                        this.brain20xFilename = filename;
                        logger.info("Found 20x aligned stack: "+brain20xFilename);
                        Entity aligned = objectiveSample.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                        if (aligned!=null) {
                            String channelSpec = aligned.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                            if (channelSpec.contains("r")) {
                                refChannel20xOneIndexed = channelSpec.indexOf('r') + 1;
                                logger.info("Found 20x ref channel (one-indexed): "+refChannel20xOneIndexed);
                            }
                        }
                    }
                }
                else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                    logger.info("Found 63x sub-sample: "+objectiveSample.getName());
                    if (filename!=null) {
                        this.inputFilename = filename;
                        logger.info("Found 63x aligned stack: "+inputFilename);
                        Entity aligned = objectiveSample.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                        if (aligned!=null) {
                            String channelSpec = aligned.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                            if (channelSpec.contains("r")) {
                                refChannel = channelSpec.indexOf('r');
                                refChannelOneIndexed = refChannel + 1;
                                logger.info("Found 63x ref channel (one-indexed): "+refChannelOneIndexed);
                                putOutputVars(channelSpec);
                            }
                        }
                    }

                    this.gender = sampleHelper.getConsensusLsmAttributeValue(objectiveSample, EntityConstants.ATTRIBUTE_GENDER, alignedArea);
                    if (gender!=null) {
                        logger.info("Found gender consensus: "+gender);
                    }
                    
                    this.opticalResolution = sampleHelper.getConsensusLsmAttributeValue(objectiveSample, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, alignedArea);
                    if (opticalResolution!=null) {
                        opticalResolution = opticalResolution.replaceAll("x", " ");
                        logger.info("Found optical resolution consensus: "+opticalResolution);
                    }
                }
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    
    @Override
    protected String getAlignerCommand() {
        StringBuilder builder = new StringBuilder(super.getAlignerCommand());
        builder.append(" -p " + brain20xFilename);
        builder.append(" -q " + refChannel20xOneIndexed);
        return builder.toString();
    }
}
