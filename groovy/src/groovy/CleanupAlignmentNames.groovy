import org.janelia.it.jacs.model.domain.sample.*

/**
 * Walk all samples and change the sample alignment names to append the anatomical area to each one.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupAlignmentNamesScript {

    def DEBUG = false
    def dao = DomainDAOManager.instance.dao

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
                        
                        if (result.anatomicalArea != null) {
                            def newName = result.name + " ("+result.anatomicalArea+")"
                            println sample.name+" Updating " + result.name + " -> " + newName
                            result.name = newName
                            dirty = true
                        }
                        else {
                            throw new IllegalStateException(sample.name+" No anatomical area: "+ result.name)
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