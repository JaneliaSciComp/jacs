
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils

class AddCompressionTypeAttributeScript {
	
	private static final boolean DEBUG = false;
    private String ownerKey = null;
    private final JacsUtils f;
	private int numCorrectedSamples;

    private int raw = 0
    private int ll = 0
    private int llandh5j = 0 
    private int vll = 0
    
	public AddCompressionTypeAttributeScript() {
		f = new JacsUtils(ownerKey, !DEBUG)
	}
	
	public void run() {
        if (ownerKey==null) {
            Set<String> subjectKeys = new TreeSet<String>();
            for(Entity dataSet : f.e.getEntitiesByTypeName(null, "Data Set")) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
                processSamples(subjectKey);
            }
        }
        else {
            processSamples(ownerKey);
        }
	}
    
    private void resetCounts() {
        raw = 0
        ll = 0
        llandh5j = 0
        vll = 0
    }
    
    private void processSamples(String ownerKey) {
        println "Processing samples for "+ownerKey
        for(Entity sample : f.e.getUserEntitiesByTypeName(ownerKey, "Sample")) {
            if (sample.name.contains("~")) continue;
            f.loadChildren(sample)
            processSample(sample)
            resetCounts()
            sample.setEntityData(null)
        }
        println "Completed processing for "+ownerKey
        println "  Corrected status on "+numCorrectedSamples+" samples"
    }
    
	private void processSample(Entity sample) {
        
        if (sample==null) return;
        if (sample.getValueByAttributeName("Compression Type")!=null) return;
        
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
        if (!childSamples.isEmpty()) {
            for(Entity childSample : childSamples) {
                f.loadChildren(childSample)
                processSample()
            }
            return;
        }
        
        for(Entity run : EntityUtils.getChildrenOfType(sample, "Pipeline Run")) {
            f.loadChildren(run)
            for(Entity result : EntityUtils.getChildrenForAttribute(run, "Result")) {
                f.loadChildren(result)
                processResult(result)
            }
        }
        
        String s = " ("+raw+" "+ll+" "+llandh5j+" "+vll+")"
        String ct = null
        
        if (raw>0) {
            println sample.name+" - some raw"+s
            ct = "Uncompressed"
        }
        else if (ll>0) {
            println sample.name+" - some lossless"+s
            ct = "Lossless"
        } 
        else if (llandh5j>0) {
            println sample.name+" - some lossless with h5j"+s
            ct = "Lossless and H5J";
        }
        else if (vll>0) {
            println sample.name+" - visually lossless"+s
            ct = "Visually Lossless"
        }
        else {
            println sample.name+" - no files"+s
            ct = "Lossless"
        }
        
        if (!DEBUG && ct!=null) {
            numCorrectedSamples++;
            f.e.setOrUpdateValue(sample.ownerKey, sample.id, "Compression Type", ct)
        }
        
	}
    
    private void processResult(Entity result) {
        Entity supportingFiles = EntityUtils.getSupportingData(result);
        if (supportingFiles==null) return
        f.loadChildren(supportingFiles)
        for(Entity image : EntityUtils.getChildrenOfType(supportingFiles, "Image 3D")) {
            if (image.name.endsWith("v3dpbd")) {
                f.loadChildren(image)
                Entity h5j = image.getChildByAttributeName("Slightly Lossy Image")
                if (h5j!=null) {
                    llandh5j++
                }
                else {
                    ll++
                }
            }
            else if (image.name.endsWith("v3draw")) {
                raw++
            }
            else if (image.name.endsWith("h5j")) {
                vll++
            }
            else {
                println "[WARN] image has unrecognized format: "+image.name
            }
        }
    }
}

AddCompressionTypeAttributeScript script = new AddCompressionTypeAttributeScript();
script.run();
println "Done"
System.exit(0)