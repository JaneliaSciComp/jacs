

import org.janelia.it.jacs.model.entity.Entity

/**
 * This script takes the output from the Informatics export mechanism and parses the sample name to clean up
 * re-TMOG'd samples that lost Status.
 * Assumes the sample called from the database returns one and only one result.
 * Assumes no sub-samples; samples with an objective ~ in the name
 */
class AddStatusAttributeScript {
	
	private static final boolean DEBUG = false;
    private String ownerKey = null;
    private final JacsUtils f;

	public AddStatusAttributeScript() {
		f = new JacsUtils(ownerKey, !DEBUG)
	}
	
	public void run() {
        String filePath = "/Users/saffordt/Desktop/20150923_17_33_35_Tasking.csv";
        File tmpFile = new File(filePath);
        Scanner scanner = new Scanner(tmpFile);
        while (scanner.hasNextLine()) {
            String tmpLine = scanner.nextLine().trim();
            String[] pieces = tmpLine.split(",");
            String tmpOwner = "user:"+pieces[1].substring(0,pieces[1].indexOf("_"));
            HashSet<Entity> results = f.e.getUserEntitiesByName(tmpOwner, pieces[0])
            if (!DEBUG && null!=results && results.size()==1) {
                Entity tmpSample = results.iterator().next();
                if (!tmpSample.name.contains("~")) {
                    String tmpStatus = tmpSample.getValueByAttributeName("Status");
                    if (null==tmpStatus || "".equals(tmpStatus)) {
                        f.e.setOrUpdateValue(tmpSample.ownerKey, tmpSample.id, "Status", "Complete")
                    }
                    else {
                        println "Status already exists for "+pieces[0];
                    }
                }
            }
            else if (null!=results && results.size()>1) {
                println "Too many results for "+pieces[0];
            }
        }
    }
    
}

AddStatusAttributeScript script = new AddStatusAttributeScript();
script.run();
println "Done"
System.exit(0)