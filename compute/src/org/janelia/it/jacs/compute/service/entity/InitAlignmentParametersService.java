package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Decides what analysis pipeline to run, based on the enumerated value.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitAlignmentParametersService extends AbstractEntityService {

	@Override 
    public void execute() throws Exception {

        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, ownerKey, logger);
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>)processData.getItem("SAMPLE_AREAS");
        
        // For legacy alignment use, just take any area and align it
        String inputFilename = null;
        for(AnatomicalArea anatomicalArea : sampleAreas) {
            Entity result = entityBean.getEntityById(anatomicalArea.getSampleProcessingResultId());
            if (result!=null) {
                String filename = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                if (filename!=null)  {
                    anatomicalArea.setSampleProcessingResultFilename(filename);
                    inputFilename = filename;
                }
            }
        }
        
        if (inputFilename!=null) {
            logger.info("Putting '"+inputFilename+"' in INPUT_FILENAME");
            processData.putItem("INPUT_FILENAME", inputFilename);
        }
        
		ParameterizedAlignmentAlgorithm paa = (ParameterizedAlignmentAlgorithm)processData.getItem("PARAMETERIZED_ALIGNMENT_ALGORITHM");
		AlignmentAlgorithm aa = paa.getAlgorithm();
		
		if (AlignmentAlgorithm.WHOLE_40X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
		}
		else if (AlignmentAlgorithm.WHOLE_40X_IMPROVED == aa) {
            processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain40xImprovedAlignmentService");
            processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 40x Alignment");
        }
		else if (AlignmentAlgorithm.WHOLE_63X == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.WholeBrain63xAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Whole Brain 63x Alignment");
		}
		else if (AlignmentAlgorithm.OPTIC == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.OpticLobeAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Optic Lobe Alignment");
        	// TODO: there should be a better way to get this...
        	String[] parts = sampleEntity.getName().split("-");
        	String tileName = parts[parts.length-1].replaceAll("_", " ");
        	processData.putItem("ALIGNMENT_TILE_NAME", tileName);
		}
		else if (AlignmentAlgorithm.CENTRAL == aa) {
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.CentralBrainAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Central Brain Alignment");
		}
		else if (AlignmentAlgorithm.CONFIGURED == aa) {
			String scriptName = paa.getParameter();
			if (scriptName==null) {
				throw new IllegalArgumentException("Tried to use Configured alignment algorithm without a parameter");
			}
        	processData.putItem("ALIGNMENT_SERVICE_CLASS", "org.janelia.it.jacs.compute.service.align.ConfiguredAlignmentService");
        	processData.putItem("ALIGNMENT_RESULT_NAME", "Brain Alignment");
        	processData.putItem("ALIGNMENT_SCRIPT_NAME", scriptName);
		}
		else {
			throw new IllegalArgumentException("No such alignment algorithm: "+aa);
		}

        logger.info("Putting '"+processData.getItem("ALIGNMENT_SERVICE_CLASS")+"' in ALIGNMENT_SERVICE_CLASS");
        logger.info("Putting '"+processData.getItem("ALIGNMENT_RESULT_NAME")+"' in ALIGNMENT_RESULT_NAME");
        logger.info("Putting '"+processData.getItem("ALIGNMENT_TILE_NAME")+"' in ALIGNMENT_TILE_NAME");
        logger.info("Putting '"+processData.getItem("ALIGNMENT_SCRIPT_NAME")+"' in ALIGNMENT_SCRIPT_NAME");

        String opticalRes = getConsensusOpticalResolution(sampleEntity);
        logger.info("Putting '"+opticalRes+"' in OPTICAL_RESOLUTION");
        processData.putItem("OPTICAL_RESOLUTION", opticalRes);
        
        String mountingProtocol = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL);
		if (mountingProtocol!=null) {
		    logger.info("Putting '"+mountingProtocol+"' in MOUNTING_PROTOCOL");
		    processData.putItem("MOUNTING_PROTOCOL", mountingProtocol);
		}

		String gender = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_GENDER);
        if (gender!=null) {
            logger.info("Putting '"+gender+"' in GENDER");
            processData.putItem("GENDER", gender);
        }
    }
    
    private String getConsensusOpticalResolution(Entity sampleEntity) throws Exception {
    	
    	String consensus = null;
    	populateChildren(sampleEntity);

    	Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
    	if (supportingFiles==null) return null; 
        populateChildren(supportingFiles);

		for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE)) {		
            populateChildren(imageTile);

    		for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
    			String opticalRes = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
    			if (consensus==null) {
    				consensus = opticalRes;
    			}
    			else {
    				if (!consensus.equals(opticalRes)) {
    					logger.warn("At least two different optical resolutions in sample with id="+
    							sampleEntity.getId()+": (1) "+consensus+" and (2) "+opticalRes+". Using (1)");
    					return consensus;
    				}
    			}
    		}
		}
		
		return consensus;
    }
}
