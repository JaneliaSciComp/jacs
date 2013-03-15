package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.align.ParameterizedAlignmentAlgorithm;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.cv.AlignmentAlgorithm;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Decides which types of processing will be run for a Sample after the inital processing is done.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChoosePostSampleProcessingStepsService extends AbstractEntityService {

    public void execute() throws Exception {

    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	String alignAlgorithms = (String)processData.getItem("ALIGNMENT_ALGORITHMS");
    	String alignAlgorithmParams = (String)processData.getItem("ALIGNMENT_ALGORITHM_PARAMS");
    	String analysisAlgorithms = (String)processData.getItem("ANALYSIS_ALGORITHMS");
    	
    	List<String> aas = Task.listOfStringsFromCsvString(alignAlgorithms);
    	List<String> aaps = Task.listOfStringsFromCsvString(alignAlgorithmParams);
    	List<ParameterizedAlignmentAlgorithm> paas = new ArrayList<ParameterizedAlignmentAlgorithm>();
    	
    	for(int i=0; i<aas.size(); i++) {
    		AlignmentAlgorithm aa = AlignmentAlgorithm.valueOf(aas.get(i));
    		String p = null;
    		try {
    			p = aaps.get(i);
    		} catch (IndexOutOfBoundsException e) {
    			logger.info("Alignment algorithm "+aa+" specified with no parameter");
    			// Ignore. Maybe this algorithm doesn't need a parameter.
    		}
    		ParameterizedAlignmentAlgorithm paa = new ParameterizedAlignmentAlgorithm(aa, p);
    		paas.add(paa);
    	}
    	
		processData.putItem("PARAMETERIZED_ALIGNMENT_ALGORITHM",paas);
		processData.putItem("ANALYSIS_ALGORITHM", Task.listOfStringsFromCsvString(analysisAlgorithms));
		
        boolean hasAlignment = !StringUtils.isEmpty(alignAlgorithms);
        boolean hasAnalysis = !StringUtils.isEmpty(analysisAlgorithms);
        
		boolean runAlignment = hasAlignment;
		boolean runAnalysis = hasAnalysis;

		processData.putItem("RUN_ALIGNMENT", new Boolean(runAlignment));
		processData.putItem("RUN_ANALYSIS", new Boolean(runAnalysis));

    	logger.info("Pipeline steps to execute for Sample "+sampleEntity.getName()+":");
    	logger.info("    Alignment = "+runAlignment);
    	logger.info("    Analysis = "+runAnalysis);
    }
}
