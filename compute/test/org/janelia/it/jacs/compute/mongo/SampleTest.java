package org.janelia.it.jacs.compute.mongo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.junit.Assert;
import org.junit.Test;

public class SampleTest extends MongoDbTest {
    
//    @Test
//    public void testSampleQuery() {
//        
//        String dataSet = "wolfft_central_tile";
//        
//        Iterable<Sample> samples = sampleCollection.find("{dataSet:'"+dataSet+"'}").as(Sample.class);
//        int c = 0;
//        for(Sample sample : samples) {
//
//            System.out.println("Sample "+sample.getName());
//            
//            ObjectiveSample sample63x = sample.getObjectives().get(Objective.OBJECTIVE_63X);
//            if (sample63x!=null) {
//                System.out.print("  ");
//                for(SampleTile tile : sample63x.getTiles()) {
//                    System.out.print(tile.getName()+",");
//                }
//                System.out.println();
//                
//                SamplePipelineRun latestRun = sample63x.getLatestRun();
//                if (latestRun!=null) {
//                    PipelineResult result = latestRun.getLatestProcessingResult();
//                    if (result!=null) {
//                        NeuronSeparation separation = result.getLatestSeparationResult();
//                        if (separation!=null) {
//                            System.out.println("    "+separation.getFragmentsReference().getCount());
//                            //getNeuronFragments(separation.getNeuronFragmentIds());
//                            
//                        }
//                        
//                    }
//                }
//            }
//            c++;
//        }
//        System.out.println("Processed "+c+" samples");
//    }
    
//    private void getNeuronFragments(List<Long> neuronFragmentIds) {
//        Iterable<NeuronFragment> fragments = fragmentCollection.find("{_id:{$in:#}}", neuronFragmentIds).as(NeuronFragment.class);
//        for(NeuronFragment fragment : fragments) {
//            System.out.println("  "+fragment.getSignalMipFilepath());
//        }
//    }

    // Counts for all data sets
    // db.sample.aggregate([{$group:{_id:"$dataSet",count:{$sum:1}}}, {$sort:{_id:1}}])
    // db.sample.find({dataSet:""}).limit(1).pretty()
    
    // Get all children of folder
    
    // ids = db.folder.find({name:"MB Polarity Case 1"},{_id:0,references:1}).map(function (x) { return x.references })[0].map(function (x) { return x.id })
    // db.sample.find({_id:{$in:ids}},{name:1,dataSet:1}).pretty()
    
    // ids = db.folder.find({_id:1803559164052504674},{_id:0,references:1}).map(function (x) { return x.references })[0].map(function (x) { return x.id })
    // db.folder.find({_id:{$in:ids}},{references:0}).pretty()
    
    // Share data set "VT MCRO Case 1":
    // db.sample.update({dataSet:"nerna_vt_mcfo_case_1"},{$addToSet:{writers:"user:rokickik"}},{multi:true})
    // db.sample.find({dataSet:"nerna_vt_mcfo_case_1"},{readers:1,writers:1})
    // Unshare:
    // db.sample.update({dataSet:"nerna_vt_mcfo_case_1"},{$pull:{writers:"user:rokickik"}},{multi:true})
    
    // All entities annotated with something:
    // ids = db.annotation.find({text:"Single_cell"},{_id:0,targetId:1}).map(function (x) {return x.targetId})
    // db.sample.find({_id:{$in:ids}},{name:1,dataSet:1}).pretty()
    
    // Get sample by name
    // db.sample.find({name:"B4_T1_20120615_2CC_26B12FlpL_5Days_V7.0_R1_L07-L08"}).pretty()
    // Get all fragments
    // ids = db.fragment.find({separationId:1755020997624332377},{_id:1}).map(function (x) {return x._id})
    // All fragment annotations:
    // db.annotation.find({targetId:{$in:ids}})
    
//    @Test
//    public void testAlignment() {
//        
//        Sample sample = sampleCollection.findOne("{name:'GMR_MB042B-20121001_31_A1'}").as(Sample.class);
//
//        if (sample==null) return;
//        
//        ObjectiveSample sample20x = sample.getObjectives().get("20x");
//        if (sample20x!=null) {
//            for(SamplePipelineRun run : sample20x.getPipelineRuns()) {
//                for(PipelineResult result : run.getResults()) {
//                    if (result instanceof SampleAlignmentResult) {
//                        SampleAlignmentResult alignment = (SampleAlignmentResult)result;
//                        System.out.println(""+alignment.getAlignmentSpace());
//                    }
//                }
//            }
//            
//        }
//    }
    

    //@Test
    public void test() {
    
    	
//    	for(Subject subject : dao.getCollection("subject").find().as(Subject.class)) {
//    		System.out.println(subject.getId());
//    	}
        //return toList(getCollection(type).find("{_id:{$in:#},readers:{$in:#}}", ids, subjects).as(getObjectClass(type)), ids);
        
    	
    	List<Long> ids = new ArrayList<Long>();
    	ids.add(1813399562556014681L);

//    	String subjectKey = "user:rokickik";
//        Set<String> subjects = dao.getSubjectSet(subjectKey);
//    	for(TreeNode treeNode : dao.getCollection("treeNode").find("{_id:{$in:#}}",ids).as(TreeNode.class)) {
//    		System.out.println(treeNode.getId()+" "+treeNode.getReaders());
//    	}

    	for(DomainObject treeNode : dao.getDomainObjects("user:rokickik","treeNode",ids)) {
    		System.out.println(treeNode.getId()+" "+treeNode);
    	}
    	
    	
    }
    
    //@Test
    public void test3() {

//        for(Workspace workspace : dao.getCollection("treeNode").find("{class:#,ownerKey:#}",Workspace.class.getName(),"user:saffordt").projection("{class:1,name:1}").as(Workspace.class)) {
//            
//            System.out.println(workspace.getName());
//            System.out.println(workspace.getOwnerKey());
//        }

        int roots = 0;
        String subjectKey = "group:leetlab";
        for(Workspace workspace : dao.getWorkspaces(subjectKey)) {
            System.out.println("Got workspace: "+workspace.getName()+" for "+workspace.getOwnerKey());
            
            for(DomainObject obj : dao.getDomainObjects(subjectKey, workspace.getChildren())) {
                TreeNode node = (TreeNode)obj;
                System.out.println(node.getId()+" "+node.getName()+" ("+node.getOwnerKey()+") - "+node.getNumChildren());
                
            }
        }
    }
    
    public void test2() {
        String subjectKey = "user:riddifordl";
        for(Workspace workspace : dao.getWorkspaces(subjectKey)) {
            System.out.println("Found workspace "+workspace.getName()+" for "+workspace.getOwnerKey());
            for(DomainObject obj : dao.getDomainObjects(subjectKey, workspace.getChildren())) {
                TreeNode node = (TreeNode)obj;
                System.out.println("    "+node.getClass().getSimpleName()+"#"+node.getName());
                for(DomainObject child : dao.getDomainObjects(subjectKey, node.getChildren())) {
                    System.out.println("        "+child.getClass().getSimpleName()+"#"+child.getId());
                }
            }
        }
    }
    
    public void testSubjects() {
        for(Subject subject : dao.getCollectionByName("subject").find().as(Subject.class)) {
            Assert.assertNotNull(subject.getId());
            Assert.assertNotNull(subject.getKey());
        }
    }
    
    @Test
    public void runBenchmarks() throws Exception {

        long start = System.currentTimeMillis();

        int roots = 0;
        String subjectKey = "user:nerna";
        for(Workspace workspace : dao.getWorkspaces(subjectKey)) {
            System.out.println("Got workspace: "+workspace.getName()+" for "+workspace.getOwnerKey());
            roots += dao.getDomainObjects(subjectKey, workspace.getChildren()).size();
        }
        System.out.println("getCommonRootEntities('user:nerna') took "+(System.currentTimeMillis()-start)+" ms and returned "+roots);
        
//        start = System.currentTimeMillis();
//        dao.changePermissions("user:nerna","sample",1977172557152911458L,"user:rokickik","r",true);
//        System.out.println("grantPermissions('GMR_MB122B-20140131_19_A1') took "+(System.currentTimeMillis()-start)+" ms");
//
//        start = System.currentTimeMillis();
//        dao.changePermissions("user:nerna","sample",1977172557152911458L,"user:rokickik","r",false);
//        System.out.println("revokePermissions('GMR_MB122B-20140131_19_A1') took "+(System.currentTimeMillis()-start)+" ms");

        // Count the number of items in the "Pan Lineage 40x" tree
        start = System.currentTimeMillis();
        
        int c = count("user:nerna", 2047918390982475874L);
        System.out.println("countTree('OL Split MCFO Case 1') took "+(System.currentTimeMillis()-start)+" ms and returned "+c);
        
        System.out.println("getProjectedResults(Sample->LSM Stack) ...");
        
        Long folderId = 1988022805484011618L;
        subjectKey = "user:asoy";

        start = System.currentTimeMillis();
        TreeNode polarityCase1Folder = dao.getTreeNodeById(subjectKey, folderId);
        Map<Long,Sample> sampleMap = new HashMap<Long,Sample>();
        
        for(DomainObject obj : dao.getDomainObjects(subjectKey, polarityCase1Folder.getChildren())) {
            Sample sample = (Sample)obj;
            sampleMap.put(sample.getId(), sample);
        }
        
        System.out.println("1) getting original entity set ("+sampleMap.size()+" ids) took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        int mappedLsms = 0;
        for(Long sampleId : sampleMap.keySet()) {
            Sample sample = sampleMap.get(sampleId);
            
            Map<Long,LSMImage> lsmMap = new HashMap<Long,LSMImage>();
            for(LSMImage lsm : dao.getActiveLsmsBySampleId(subjectKey, sample.getId())) {
                lsmMap.put(lsm.getId(), lsm);
            }
            
            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                for(SampleTile tile : objectiveSample.getTiles()) {
                    for(Reference lsmRef : tile.getLsmReferences()) {
                        LSMImage image = lsmMap.get(lsmRef.getTargetId());
                        if (image==null) {
                            throw new IllegalStateException("Missing LSM: "+lsmRef.getTargetId());
                        }
                        //System.out.println(sample.getName()+" -> "+image.getFiles().get(FileType.Stack));
                        mappedLsms++;
                    }
                }
            }
        }
        
        System.out.println("2+3) mapping and retrieval of "+mappedLsms+" LSMs took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        int count = count(subjectKey, folderId);
        System.out.println("4) count entity tree returned "+count+" and took "+(System.currentTimeMillis()-start)+" ms");
        
//        start = System.currentTimeMillis();
//        dao.changePermissions("user:nerna","treeNode",1889491952735354978L,"user:rokickik","r",true);
//        System.out.println("Grant on nerna's Polarity Case 1 and took "+(System.currentTimeMillis()-start)+" ms");
//
//        start = System.currentTimeMillis();
//        dao.changePermissions("user:nerna","treeNode",1889491952735354978L,"user:rokickik","r",false);
//        System.out.println("Revoke on nerna's Polarity Case 1 and took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    private int count(String subjectKey, Long nodeId) {
        int c = 1;
        TreeNode treeNode = dao.getTreeNodeById(subjectKey, nodeId);
        List<Long> sampleIds = new ArrayList<Long>();
        if (treeNode.getChildren()==null) return c;
        for(Reference ref : treeNode.getChildren()) {
            sampleIds.add(ref.getTargetId());
        }
        for(DomainObject obj : dao.getDomainObjects(subjectKey, treeNode.getChildren())) {
            Sample sample = (Sample)obj;
            for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                for(SampleTile tile : objectiveSample.getTiles()) {
                    for(Reference lsm : tile.getLsmReferences()) {
                    }
                }
                for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                    if (run.getResults()==null) continue;
                    for(PipelineResult result : run.getResults()) {
                        if (result.getResults()==null) continue;
                        for(PipelineResult secResult : result.getResults()) {
                            if (secResult instanceof NeuronSeparation) {
                                ((NeuronSeparation)secResult).getFragmentsReference().getCount();
                            }  
                        }
                    }
                }
            }
            c++;
        }
        return c;
    }
}
