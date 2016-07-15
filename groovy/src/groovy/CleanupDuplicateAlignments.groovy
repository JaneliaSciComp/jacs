import org.janelia.it.jacs.model.domain.DomainConstants
import org.janelia.it.jacs.model.domain.enums.FileType
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO
import org.janelia.it.jacs.model.domain.support.DomainUtils

/**
 * Walk all samples and clean up any duplicate CMTK alignments that contain "Broken" stacks. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupDuplicateAlignmentsScript {

    boolean DEBUG = false
    DomainDAO dao = DomainDAOManager.instance.dao

    public void run() {

        for(DataSet dataSet : dao.getDataSets(null)) {
            for(Sample sample : dao.getSamplesForDataSet(dataSet.ownerKey, dataSet.identifier)) {
                cleanup(sample)
            }
        }
    }

    def cleanup(Sample sample) {

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

        if (dirty) {
            if (!DEBUG) {
                dao.save(sample.ownerKey, sample)
            }
        }

    }

}

new CleanupDuplicateAlignmentsScript().run()