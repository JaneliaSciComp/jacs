package org.janelia.it.jacs.model.domain;

import java.util.List;

import org.junit.Test;

public class SampleTest extends MongoDbTest {
    
    @Test
    public void testSampleQuery() {
        
        String dataSet = "wolfft_central_tile";
        
        Iterable<Sample> samples = sampleCollection.find("{dataSet:'"+dataSet+"'}").as(Sample.class);
        int c = 0;
        for(Sample sample : samples) {

            System.out.println("Sample "+sample.getName());
            
            ObjectiveSample sample63x = sample.getObjectives().get("63x");
            if (sample63x!=null) {
                System.out.print("  ");
                for(SampleTile tile : sample63x.getTiles()) {
                    System.out.print(tile.getName()+",");
                }
                System.out.println();
                
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
            }
            c++;
        }
        System.out.println("Processed "+c+" samples");
    }
    
    private void getNeuronFragments(List<Long> neuronFragmentIds) {
        Iterable<NeuronFragment> fragments = neuronFragmentCollection.find("{_id:{$in:#}}", neuronFragmentIds).as(NeuronFragment.class);
        for(NeuronFragment fragment : fragments) {
            System.out.println("  "+fragment.getSignalMipFilepath());
        }
    }

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
    
    @Test
    public void testAlignment() {
        
        Sample sample = sampleCollection.findOne("{name:'GMR_MB042B-20121001_31_A1'}").as(Sample.class);

        if (sample==null) return;
        
        ObjectiveSample sample20x = sample.getObjectives().get("20x");
        if (sample20x!=null) {
            for(SamplePipelineRun run : sample20x.getPipelineRuns()) {
                for(PipelineResult result : run.getResults()) {
                    if (result instanceof SampleAlignmentResult) {
                        SampleAlignmentResult alignment = (SampleAlignmentResult)result;
                        System.out.println(""+alignment.getAlignmentSpace());
                    }
                }
            }
            
        }
    }
    
}
