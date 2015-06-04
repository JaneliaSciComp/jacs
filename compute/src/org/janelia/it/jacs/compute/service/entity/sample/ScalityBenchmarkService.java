package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.scality.ScalityDAO;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Benchmark the copying of all files related to a sample into Scality, and then retrieve them back.
 * Compare to copying the files using a normal copy operation. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScalityBenchmarkService extends AbstractEntityService {

	private static final Logger log = Logger.getLogger(ScalityBenchmarkService.class);

	private static final int NUM_ITERATIONS = 10;
	
	private ScalityDAO scality;
	
	private List<Entity> createdScalityEntities = new ArrayList<>();
	
	public void execute() throws Exception {
		
    	String sampleEntityId = data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
		log.info("Benchmarking sample id: "+sampleEntityId);
		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		log.warn("Sample entity not found with id="+sampleEntityId);
    	}

    	try {
        	this.scality = new ScalityDAO();
			log.info("Upload to Scality:");
        	long elapsed = uploadTreeToScality(sampleEntity);
			log.info(sampleEntity.getName()+"\t"+elapsed+" ms");

			long total = 0;
			for(int i=0; i<NUM_ITERATIONS; i++) {
				log.info("File iteration "+i+":");
				elapsed = downloadTreeToLocalDisk(sampleEntity, false);
				log.info("    "+sampleEntity.getName()+"\t"+elapsed+" ms");
				total += elapsed;
			}
			log.info("File Average: "+Math.round((double)total/(double)NUM_ITERATIONS)+" ms");
			
			total = 0;
			for(int i=0; i<NUM_ITERATIONS; i++) {
				log.info("Scality iteration "+i+":");
				elapsed = downloadTreeToLocalDisk(sampleEntity, true);
				log.info("    "+sampleEntity.getName()+"\t"+elapsed+" ms");
				total += elapsed;
			}
			log.info("Scality Average: "+Math.round((double)total/(double)NUM_ITERATIONS)+" ms");
		
			log.info("Deleting all the Scality objects we created...");
			cleanScality();
    	}
    	finally {
    		if (scality!=null) scality.close();	
    	}	
    }
	
	private long uploadTreeToScality(Entity entity) throws Exception {
		
		long totalElapsed = 0;
		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		if (filepath!=null) {
			long start = System.currentTimeMillis();
			Long scalityId = entity.getId();
			createdScalityEntities.add(entity);
			scality.put(entity);
			long elapsed = System.currentTimeMillis()-start;
			log.info("    "+entity.getName()+"\t"+elapsed+" ms");
			totalElapsed += elapsed;
		}
		
		entityLoader.populateChildren(entity);
		for(Entity child : entity.getOrderedChildren()) {
			totalElapsed += uploadTreeToScality(child);
		}
		return totalElapsed;
	}
	
	private long downloadTreeToLocalDisk(Entity entity, boolean useScality) throws Exception {
		
		long totalElapsed = 0;
		
		String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		if (filepath!=null) {
			File tmpFile = File.createTempFile(entity.getId().toString(), "tmp");
	
			long start = System.currentTimeMillis();
			if (useScality) {
				scality.get(entity, tmpFile.getAbsolutePath());
			}
			else {
				FileUtils.copyFile(new File(filepath), tmpFile);
			}
			long elapsed = System.currentTimeMillis()-start;
			log.info("    "+entity.getName()+"\t"+elapsed+" ms");
			totalElapsed += elapsed;
		}
		
		entityLoader.populateChildren(entity);
		for(Entity child : entity.getOrderedChildren()) {
			totalElapsed += uploadTreeToScality(child);
		}
		return totalElapsed;
	}
	
	private void cleanScality() {
		long start = System.currentTimeMillis();
		for(Entity entity : createdScalityEntities) {
			try {
				scality.delete(entity);
			}
			catch (Exception e) {
				log.error("Failed to delete temporary object "+entity.getId(),e);
			}
		}
		long elapsed = System.currentTimeMillis()-start;
		log.info("Deleting "+createdScalityEntities.size()+" Scality objects took "+elapsed+" ms");
	}
}
