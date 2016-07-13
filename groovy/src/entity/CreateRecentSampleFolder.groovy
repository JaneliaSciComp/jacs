package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.tasks.Task

import static org.janelia.it.jacs.model.entity.EntityConstants.*

int days = 5;

String ownerKey = "group:admin";
f = new JacsUtils(ownerKey, true);

Set<String> subjectKeys = new HashSet<String>();
for(Entity sample : f.e.getEntitiesByTypeName(null, TYPE_DATA_SET)) {
    subjectKeys.add(sample.ownerKey);
}

now = new Date()

println "Checking recent sample tasks for these users: "+subjectKeys

Entity topLevelFolder = f.getRootEntity("Recent Samples");
if (topLevelFolder==null) {
    topLevelFolder = f.createRootEntity("Recent Samples");
}

println "Got top level folder: "+topLevelFolder.id

List<Long> sampleIds = [];

for (String subjectKey : subjectKeys) {

    List<Long> userSampleIds = [];
    String taskOwner = subjectKey.contains(":") ? subjectKey.split(":")[1] : subjectKey;

    for(Task task : f.c.getUserParentTasks(taskOwner)) {
        
        if (task.taskName!="GSPS_UserDataSetPipelines") continue;

        Date creationDate = task.getEvents().iterator().next().timestamp;
        long elapsedSecs = (now.time-creationDate.time)/1000;

        if ((((elapsedSecs)/60)/60) < (days*24)) {
            for(Task childTask : f.c.getChildTasksByParentTaskId(task.objectId)) {
                String sampleIdStr = childTask.getParameter("sample entity id");
                if (sampleIdStr!=null) {
                    userSampleIds.add(Long.parseLong(sampleIdStr));
                }
            }
        }
    }

    println taskOwner+" : "+userSampleIds.size()+" samples";
    sampleIds.addAll(userSampleIds);
}

Collections.sort(sampleIds);

println "Adding "+sampleIds.size()+" samples to Recent Samples folder";

if (!sampleIds.isEmpty()) {
    f.e.addChildren(ownerKey, topLevelFolder.id, sampleIds, ATTRIBUTE_ENTITY);
}
