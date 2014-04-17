package org.janelia.it.jacs.model.domain;

import java.util.List;

import org.junit.Test;

public class SampleTest extends MongoDbTest {
    
    @Test
    public void testSampleQuery() {
        
        Iterable<Sample> samples = sampleCollection.find("{dataSet:'asoy_mb_polarity_case_1'}").as(Sample.class);
        for(Sample sample : samples) {
            System.out.println("Sample "+sample.getName()+" Objectives:"+sample.getObjectives().keySet());
            
            ObjectiveSample sample63x = sample.getObjectives().get("63x");
            List<SamplePipelineRun> pipelineRuns = sample63x.getPipelineRuns();
            
            if (sample63x!=null && !pipelineRuns.isEmpty()) {
                SamplePipelineRun lastRun = pipelineRuns.get(pipelineRuns.size()-1);
                
                for(PipelineResult result : lastRun.getResults()) {
                    System.out.println("  Result: "+result.getFilepath());
                }
            }
        }
        
    }
    
    // Counts for all data sets
    // db.sample.aggregate([{$group:{_id:"$dataSet",count:{$sum:1}}}])
 
    
    // Get all children of folder
    
    // ids = db.folder.find({name:"MB Polarity Case 1"},{_id:0,itemIds:1}).map(function (y) { return y.itemIds })[0]
    // db.sample.find({_id:{$in:ids}},{name:1,dataSet:1})
}
