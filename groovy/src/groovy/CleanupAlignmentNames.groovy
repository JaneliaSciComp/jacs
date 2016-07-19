import org.janelia.it.jacs.model.domain.DomainConstants
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO

/**
 * Walk all samples and change the sample alignment names to append the anatomical area to each one.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupAlignmentNamesScript {

    boolean DEBUG = true
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

        if (dirty) {
            if (!DEBUG) {
                dao.save(sample.ownerKey, sample)
            }
        }

    }

}

new CleanupAlignmentNamesScript().run()