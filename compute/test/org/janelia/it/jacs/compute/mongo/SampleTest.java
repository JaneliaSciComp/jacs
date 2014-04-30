package org.janelia.it.jacs.compute.mongo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.ImageType;
import org.janelia.it.jacs.model.domain.LSMImage;
import org.janelia.it.jacs.model.domain.NeuronSeparation;
import org.janelia.it.jacs.model.domain.ObjectiveSample;
import org.janelia.it.jacs.model.domain.PipelineResult;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.SampleTile;
import org.janelia.it.jacs.model.domain.Subject;
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
    
    
    @Test
    public void testSubjects() {
        for(Subject subject : dao.getCollection("subject").find().as(Subject.class)) {
            System.out.println(subject.getKey());
        }
        
    }
    public void runBenchmarks() throws Exception {

        long start = System.currentTimeMillis();

        List<Folder> roots = dao.getRootFolders("group:leetlab");
        System.out.println("getCommonRootEntities('group:leetlab') took "+(System.currentTimeMillis()-start)+" ms and returned "+roots.size());
        
        start = System.currentTimeMillis();
        dao.changePermissions("group:leetlab","sample",1759767174932594786L,"user:rokickik","r",true);
        //sampleCollection.update("{_id:1759767174932594786,writers:'group:leetlab'}").with("{$addToSet:{readers:#}}","user:rokickik");
        System.out.println("grantPermissions('TZL_stg14-Hey01328_Y1') took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        dao.changePermissions("group:leetlab","sample",1759767174932594786L,"user:rokickik","r",false);
        System.out.println("revokePermissions('TZL_stg14-Hey01328_Y1') took "+(System.currentTimeMillis()-start)+" ms");

        // Count the number of items in the "Pan Lineage 40x" tree
        start = System.currentTimeMillis();
        
        int c = count("group:leetlab", 1803555221738094690L);
        System.out.println("countTree('Pan Lineage 40x') took "+(System.currentTimeMillis()-start)+" ms and returned "+c);
        
        System.out.println("getProjectedResults(Sample->LSM Stack) ...");
        
        Long retiredDataId = 1870629090470396002L;
        String subjectKey = "group:heberleinlab";

        start = System.currentTimeMillis();
        Folder retiredDataFolder = dao.getFolderById(subjectKey, retiredDataId);
        Map<Long,Sample> sampleMap = new HashMap<Long,Sample>();
        List<Long> sampleIds = new ArrayList<Long>();
        for(Reference ref : retiredDataFolder.getReferences()) {
            sampleIds.add(ref.getTargetId());
        }

        for(Sample sample : dao.getSamplesByIds(subjectKey, sampleIds)) {
            sampleMap.put(sample.getId(), sample);
        }
        
        System.out.println("1) getting original entity set ("+sampleIds.size()+" ids) took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        int mappedLsms = 0;
        for(Long sampleId : sampleIds) {
            Sample sample = sampleMap.get(sampleId);
            
            Map<Long,LSMImage> lsmMap = new HashMap<Long,LSMImage>();
            for(LSMImage lsm : dao.getLsmsBySampleId(subjectKey, sample.getId())) {
                lsmMap.put(lsm.getId(), lsm);
            }
            
            for(String objective : sample.getObjectives().keySet()) {
                ObjectiveSample osample = sample.getObjectives().get(objective);
                for(SampleTile tile : osample.getTiles()) {
                    for(Reference lsmRef : tile.getLsmReferences()) {
                        LSMImage image = lsmMap.get(lsmRef.getTargetId());
                        if (image==null) {
                            throw new IllegalStateException("Missing LSM: "+lsmRef.getTargetId());
                        }
                        System.out.println(sample.getName()+" -> "+image.getImages().get(ImageType.Stack));
                        mappedLsms++;
                    }
                }
            }
        }
        
        System.out.println("2+3) mapping and retrieval of "+mappedLsms+" LSMs took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        int count = count(subjectKey, retiredDataId);
        System.out.println("4) count entity tree returned "+count+" and took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        dao.changePermissions("user:nerna","folder",1938712577584398434L,"user:rokickik","r",true);
        System.out.println("Grant on VT MCFO Case 1 and took "+(System.currentTimeMillis()-start)+" ms");
        
        start = System.currentTimeMillis();
        dao.changePermissions("user:nerna","folder",1938712577584398434L,"user:rokickik","r",false);
        System.out.println("Revoke on VT MCFO Case 1 and took "+(System.currentTimeMillis()-start)+" ms");
    }
    
    private int count(String subjectKey, Long folderId) {

        Folder folder = dao.getFolderById(subjectKey, folderId);
        List<Long> sampleIds = new ArrayList<Long>();
        for(Reference ref : folder.getReferences()) {
            sampleIds.add(ref.getTargetId());
        }
        int c = 1;
        for(Sample sample : dao.getSamplesByIds(subjectKey, sampleIds)) {
            for(String objective : sample.getObjectives().keySet()) {
                ObjectiveSample osample = sample.getObjectives().get(objective);
                for(SampleTile tile : osample.getTiles()) {
                    for(Reference lsm : tile.getLsmReferences()) {
                    }
                }
                for(SamplePipelineRun run : osample.getPipelineRuns()) {
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
