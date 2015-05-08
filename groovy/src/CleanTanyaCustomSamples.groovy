import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanTanyaCustomSamplesScript {
	
	private static final boolean DEBUG = false;
	private static final subjectKey = "user:wolfft";
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "" ]
    private int numUpdated;
    private int numLsmUpdated;
	
	private String objective = "63x";
	
	public CleanTanyaCustomSamplesScript() {
		f = new JacsUtils(subjectKey, !DEBUG)
	}
	
	public void run() {
				
//		long sampleId = 1747354274502803545L;
//		Entity sample = f.e.getEntityById(subjectKey, sampleId)
//		processSample(sample)
		
		for(Entity sample : f.e.getEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE)) {
			if (sample.name.matches(".*?_L\\d{1,2}-L\\d{1,2}")) {
				// There should be 1476 such samples
				processSample(sample)
			}	
		}
		
        println "Updated "+numLsmUpdated+" LSMs"
        println "Updated "+numUpdated+" samples"
		println "Done"
	}
	
	public void processSample(Entity sample) {
				
		println "Fixing "+sample.name
				
		f.loadChildren(sample)
		Entity sd = EntityUtils.getSupportingData(sample)
        f.loadChildren(sd)
        
		for(Entity tile : EntityUtils.getChildrenOfType(sd, EntityConstants.TYPE_IMAGE_TILE)) {
			f.loadChildren(tile)
			
			String chanSpec = "rss";
			for(Entity lsm : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {
				lsm.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, chanSpec)
				lsm.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, chanSpec.length()+"")
				lsm.setValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE, objective)
				lsm.setValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, "Brain")
				// This is just a guess and probably incorrect, but it doesn't matter. We just need something in there for the pipeline to run.
				lsm.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, "1024x1024x300") 
				lsm.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, "0.16x0.16x0.38") 
				f.save(lsm)
				chanSpec = "rs";
				numLsmUpdated++;
			}
		}
		
		sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE, objective)
		f.save(sample)
		
        numUpdated++;
	}
}

CleanTanyaCustomSamplesScript script = new CleanTanyaCustomSamplesScript();
script.run();