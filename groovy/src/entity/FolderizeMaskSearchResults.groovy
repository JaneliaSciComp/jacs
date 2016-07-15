package entity

import org.janelia.it.workstation.api.entity_model.management.ModelMgr
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.jacs.model.entity.Entity
// Globals
subject = "user:wum10"
f = new JacsUtils(subject, false)
e = f.e
c = f.c

//Scanner scanner = new Scanner(new File("/Users/saffordt/Desktop/LC6_mask1_Combined_SearchResults.txt"))
Scanner scanner = new Scanner(new File("/Users/saffordt/Desktop/LC6_mask2_Combined.218.labels_SearchResults.txt"))
ArrayList<String> sampleNames = new ArrayList<String>();
while(scanner.hasNextLine()){
    String tmpLine = scanner.nextLine();
    tmpLine = tmpLine.substring(tmpLine.lastIndexOf("/")+1);
    tmpLine = tmpLine.substring(0,tmpLine.indexOf("."));
    sampleNames.add(tmpLine);
}

//id = 1805889121739079778L
ArrayList<Long> searchResultSampleIds = new ArrayList<Long>()
for (String tmpName : sampleNames) {
    Set<Entity> tmpSet = e.getEntitiesByNameAndTypeName(subject, tmpName, "Screen Sample");
    // Assume one hit and take it.
    if (null!=tmpSet && tmpSet.size()>0) {
        searchResultSampleIds.add(tmpSet.iterator().next().getId());
    };
    println tmpName+":"+tmpSet.size();
}

SessionMgr.getSessionMgr().loginSubject();
Entity newFolder = ModelMgr.getModelMgr().createCommonRoot("LC6_mask2 Search Results (Best Score first)");
ModelMgr.getModelMgr().addChildren(newFolder.getId(), searchResultSampleIds,"Entity");
println "Added "+searchResultSampleIds.size()+" samples to folder LC6_mask2 Search Results (Best Score first)";
SessionMgr.getSessionMgr().logoutUser();

//sampleFolder =
//f.loadChildren(sampleFolder)
//e.createEntity()

//int numSamples = 0;
//int numSamplesReprocessed = 0;
//
//for (Entity sample : sampleFolder.children) {
//	f.loadChildren(sample)
//	sc = EntityUtils.getOrderedEntityDataForAttribute(sample,ATTRIBUTE_ENTITY)
//
//    int numDefaultImages = 0;
//    for(EntityData pipelineRunEd : sc) {
//        if (pipelineRunEd.childEntity.getValueByAttributeName(ATTRIBUTE_DEFAULT_2D_IMAGE)!=null) {
//            numDefaultImages++;
//        }
//    }
//
//    boolean reprocess = false;
//    if (sc.size()>=2) {
//        // skip
//        if (numDefaultImages<2) {
//            reprocess = true;
//        }
//    }
//    else if (sc.size()==1) {
//        reprocess = true;
//    }
//    else {
//        println "UNKNOWN sample: "+sample.name
//        for(EntityData child : sc) {
//            println "    "+child.childEntity.name
//        }
//        reprocess = true;
//    }
//
//    if (reprocess) {
//        println "Reprocess "+sample.name+" for "+sample.ownerKey
//        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
//        taskParameters.add(new TaskParameter("sample entity id", sample.id.toString(), null));
//        taskParameters.add(new TaskParameter("reuse processing", "false", null));
//        Task task = new GenericTask(new HashSet<Node>(), "leetlab", new ArrayList<Event>(),
//                taskParameters, "flylightSampleAllPipelines", "Flylight Sample All Pipelines");
//        task.setJobName("Flylight Sample All Pipelines Task");
//        task = c.saveOrUpdateTask(task);
//        c.submitJob("GSPS_CompleteSamplePipeline", task.getObjectId());
//        numSamplesReprocessed++;
//    }
//    numSamples++;
//}
//
//println "folder id: "+sampleFolder
//println "numSamplesReprocessed: "+numSamplesReprocessed
//
//
