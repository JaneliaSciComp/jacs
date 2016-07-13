package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.tasks.Event
import org.janelia.it.jacs.model.tasks.TaskParameter
import org.janelia.it.jacs.model.tasks.utility.GenericTask
import org.janelia.it.jacs.model.user_data.Node
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

class ResubmitIncorrectlyTiledSamplesScript {
    
    private static final boolean DEBUG = false;
    private static final String OWNER_KEY = "user:nerna"
    private static final String OUTPUT_ROOT_NAME = "Resubmitted Samples";
    private final JacsUtils f;
    private String context;
    private String[] dataSetIdentifiers = [ "nerna_optic_lobe_right", "nerna_optic_lobe_left", "nerna_optic_central_border", "nerna_optic_span", "nerna_other" ]
    private int numResubmitted = 0
    private int totalNumResubmitted = 0
    private Entity rootFolder = null
    
    public ResubmitIncorrectlyTiledSamplesScript() {
        f = new JacsUtils(OWNER_KEY, !DEBUG)
    }
    
    public void run() {
        
        if (!DEBUG) {
            rootFolder = f.getRootEntity(OUTPUT_ROOT_NAME)
            if (rootFolder!=null) {
                println "Deleting root folder "+OUTPUT_ROOT_NAME+". This may take a while!"
                f.deleteEntityTree(rootFolder.id)
            }
            rootFolder = f.createRootEntity(OUTPUT_ROOT_NAME)
        }
        
        
        for(String dataSetIdentifier : dataSetIdentifiers) {
            numResubmitted = 0
            int numTotalInDataSet = 0
            println "Processing "+dataSetIdentifier
            for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
                if (entity.entityTypeName.equals("Sample") && !entity.name.endsWith("-Retired")) {
                    if (processSample(entity)) {
                        resubmitSample(entity)
                    }
                    numTotalInDataSet++;
                }
                entity.setEntityData(null)  
            }
            totalNumResubmitted += numResubmitted
            println "Resubmitted "+numResubmitted+"/"+numTotalInDataSet+" samples for "+dataSetIdentifier
        }
        println "Resubmitted "+totalNumResubmitted+" samples"
        println "Done"
        System.exit(0)
    }
    
    public boolean processSample(Entity sample) {
        boolean problem = false
        String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE)
        
        f.loadChildren(sample)
        final Set<String> lsms = new HashSet<>()
        
        Entity supportingData = EntityUtils.getSupportingData(sample)
        f.loadChildren(supportingData)
        if (supportingData != null) {
            List<Entity> tiles = EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY)
            for(Entity imageTile : tiles) {
                f.loadChildren(imageTile)
                for(Entity lsm : EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)) {
                    lsms.add(lsm.name.replaceAll(".bz2",""))
                }
            }
        }
        
        final Set<String> lsmSet = new HashSet<>(lsms)
        final Set<String> metaSet = new HashSet<>()
        
        EntityVistationBuilder.create(f.getEntityLoader()).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).last()
                .childrenOfType(EntityConstants.TYPE_TEXT_FILE)
                .run(new EntityVisitor() {
            public void visit(Entity textFile) throws Exception {
                if (textFile.name.contains(".json") || textFile.name.contains(".metadata")) {
                    String name = textFile.name.replaceAll(".json", "").replaceAll(".metadata", "")
                    lsmSet.remove(name)
                    metaSet.add(name)
                }
            }
        });
        
        metaSet.removeAll(lsms)
    
        // Make sure that every LSM has metadata
        if (!lsmSet.isEmpty()) {
            //println "Missing lsms "+lsmSet+" from meta set "+metaSet
            problem = true
        }
        
        // Make sure that every metadata file is for an LSM
        if (!metaSet.isEmpty()) {
            //println "Missing meta "+metaSet+" from LSMS set"+lsms
            problem = true
        }
        
        return problem
    }
    
    public void resubmitSample(Entity sample) {
        
        String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
        if (status.equals("Error")) return
        
        println "Resubmitting "+sample.name+" ("+status+")"
        numResubmitted++
        
        if (!DEBUG) {
            
            String process = "GSPS_CompleteSamplePipeline"
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", ""+sample.id, null));
            taskParameters.add(new TaskParameter("reuse pipeline runs", "false", null));
            taskParameters.add(new TaskParameter("reuse processing", "false", null));
            taskParameters.add(new TaskParameter("reuse alignment", "false", null));
            String user = sample.getOwnerKey();
            GenericTask task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), taskParameters, process, process);
            task = f.cr.saveOrUpdateTask(task);
            f.cr.submitJob(task.getTaskName(), task.getObjectId());
            
            if (rootFolder!=null) {
                f.addToParent(rootFolder, sample, rootFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
            }
        }
    }
}

ResubmitIncorrectlyTiledSamplesScript script = new ResubmitIncorrectlyTiledSamplesScript();
script.run();