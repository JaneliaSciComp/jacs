
import static org.janelia.it.jacs.model.entity.EntityConstants.*
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

import com.google.common.base.Charsets
import com.google.common.io.Files

class MarkSamplesWithBadAlignmentsScript {
    
    private static final boolean DEBUG = true;
    
	private int numMarked = 0 
	private int numTested = 0 
	private int numRecentTested = 0 
    private final JacsUtils f
	private Date cutoff
        
    public MarkSamplesWithBadAlignmentsScript() {
        f = new JacsUtils("user:rokickik", !DEBUG)
    }
    
    public void run() {
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(2016, 4, 8, 0, 0, 0);
		cutoff = cal.getTime();
		
		Set<String> subjectKeys = new HashSet<String>();
		for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
		    subjectKeys.add(dataSet.getOwnerKey());
		}
		
		println "Found users with data sets: "+subjectKeys
		for(String subjectKey : subjectKeys) {
			//println "Processing "+subjectKey;
			processUser(subjectKey)
		}
		
        println "Completed processing"
        println "  Tested "+numTested+" alignments for rerun"
        println "  Found "+numRecentTested+" alignments after cutoff date"
        println "  Marked "+numMarked+" samples for rerun"
    }
	
	private void processUser(String subjectKey) {
		int numMarked = 0
		Set<String> dataSets = new HashSet<>()
		
		List<Entity> files = new ArrayList<>(f.e.getUserEntitiesByName(subjectKey, "Aligned63xScale.h5j"))
		Collections.sort(files, new Comparator<Entity>() {
			public int compare(Entity arg0, Entity arg1) {
				return arg0.creationDate.compareTo(arg1.creationDate)
			};
		});
		for(Entity alignedFile : files) {
			String nccStr = alignedFile.getValueByAttributeName(ATTRIBUTE_ALIGNMENT_NCC_SCORE);
			if (nccStr==null) continue
			numTested++;
			//println "Testing "+subjectKey+" alignment on "+alignedFile.creationDate+" with ncc="+nccStr
			if (alignedFile.creationDate.after(cutoff)) {
				numRecentTested++;
				if (Double.parseDouble(nccStr)<0.8) {
					Entity sample = f.e.getAncestorWithType(alignedFile.ownerKey, alignedFile.id, TYPE_SAMPLE)
					if (sample!=null) {
						if (sample.name.contains("~")) {
							sample = f.e.getAncestorWithType(sample.ownerKey, sample.id, TYPE_SAMPLE)
						}
						//println "--> Reprocess "+sample
						numMarked++
						dataSets.add(sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER))
						if (!DEBUG) {
							f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED)
						}
					}
				}
			}
			alignedFile.setEntityData(null) // free memory
		}
		
		if (numMarked>0) {
			println "Marked "+numMarked+" for "+subjectKey+" with data sets: "+dataSets
			this.numMarked += numMarked
		}
	}
}

MarkSamplesWithBadAlignmentsScript script = new MarkSamplesWithBadAlignmentsScript();
script.run();
System.exit(0);
