import org.janelia.it.jacs.model.domain.DomainConstants
import org.janelia.it.jacs.model.domain.enums.FileType
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO
import org.janelia.it.jacs.model.domain.support.DomainUtils
import org.janelia.it.jacs.shared.utils.StringUtils

/**
 * Walk all samples and run clean up procedures.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupSamplesScript {

    boolean DEBUG = true
    DomainDAO dao = DomainDAOManager.instance.dao

    public void run() {
        Set<String> users = new HashSet<>()
        for(DataSet dataSet : dao.getDataSets(null)) {
            users.add(dataSet.ownerKey)
//            println "Processing "+dataSet.identifier
//            for(Sample sample : dao.getSamplesForDataSet(dataSet.ownerKey, dataSet.identifier)) {
//                cleanup(sample)
//            }
        }
        // Samples without data sets
        for(Sample sample : dao.getCollectionByClass(Sample.class).find("{dataSet:{\$exists:false}}").as(Sample.class)) {
            println sample.name
            cleanup(sample)
        }
    }

    def cleanup(Sample sample) { 
        if (cleanupAll(sample)) {
            if (!DEBUG) {
                dao.save(sample.ownerKey, sample)
            }
        }
    }

    def boolean cleanupAll(Sample sample) {
        boolean dirty = false
//        dirty |= cleanupAlignmentNames(sample)
//        dirty |= cleanupDuplicateAlignments(sample)
        dirty |= cleanupNullObjective(sample)
        return dirty
    }
    
    def boolean cleanupAlignmentNames(Sample sample) {
        
        boolean dirty = false
        
        for(ObjectiveSample objectiveSample : sample.objectiveSamples) {

            for(SamplePipelineRun run : objectiveSample.pipelineRuns) {

                for(PipelineResult result : run.results) {
                    if (result in SampleAlignmentResult) {
                        
                        if (result.anatomicalArea == null) {
                            throw new IllegalStateException(sample.name+" No anatomical area: "+ result.name)
                        }
                        
                        String suffix = " ("+result.anatomicalArea+")"
                        if (!result.name.endsWith(suffix)) {
                            def newName = result.name + suffix
                            println sample.name+" Updating " + result.name + " -> " + newName
                            result.name = newName
                            dirty = true
                            if (sample.status.equals(DomainConstants.VALUE_PROCESSING)) {
                                throw new Exception("Sample is processing, cannot proceed");
                            }
                        }
                        
                    }
                }
            }
        }
        
        return dirty
    }
    
    def cleanupDuplicateAlignments(Sample sample) {

        boolean dirty = false

        for(ObjectiveSample objectiveSample : sample.objectiveSamples) {

            for(SamplePipelineRun run : objectiveSample.pipelineRuns) {

                Set<Long> alignmentIds = new HashSet<>()
                
                for(Iterator<PipelineResult> i = run.results.iterator(); i.hasNext(); ) {
                    PipelineResult result = i.next();
                    if (result in SampleAlignmentResult) {
                        
                        if (alignmentIds.contains(result.id)) {
                            String filepath = DomainUtils.getFilepath(result, FileType.FirstAvailable3d)
                            if (filepath.indexOf("Broken")>0) {
                                println sample.name+" (id="+sample.id+") -- Deleting broken alignment "+result.name+" (id="+result.id+")"
                                i.remove()
                                dirty = true
                                if (sample.status.equals(DomainConstants.VALUE_PROCESSING)) {
                                    throw new Exception("Sample is processing, cannot proceed");
                                }
                            }
                        }
                        else {
                            alignmentIds.add(result.id)
                        }
                        
                    }
                }
            }
        }

        return dirty
    }
    
    def boolean cleanupNullObjective(Sample sample) {
        
        
        boolean dirty = false

        for(ObjectiveSample objectiveSample : sample.objectiveSamples) {
            println "   objective: "+objectiveSample.objective
            if (objectiveSample.objective.equals("")) {
                println "Missing objective: "+sample.name
                
                objectiveSample.objective=""
                dirty = true
                                
                if (sample.status.equals(DomainConstants.VALUE_PROCESSING)) {
                    throw new Exception("Sample is processing, cannot proceed");
                }
            }
        }

        return dirty
    }
    
}

new CleanupSamplesScript().run()