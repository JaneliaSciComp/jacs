import org.janelia.it.jacs.model.domain.DomainConstants
import org.janelia.it.jacs.model.domain.enums.FileType
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO
import org.janelia.it.jacs.model.domain.support.DomainUtils
import org.janelia.it.jacs.shared.utils.StringUtils

/**
 * Walk all samples without data sets and clean them up.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupSamplesMissingDatasetsScript {

    boolean DEBUG = true
    DomainDAO dao = DomainDAOManager.instance.dao

    public void run() {
        for(Sample sample : dao.getCollectionByClass(Sample.class).find("{dataSet:{\$exists:false}}").as(Sample.class)) {
            println "Clean up "+sample.name
            cleanup(sample)
        }
    }

    def boolean cleanup(Sample sample) {

        sample.sageSynced = false
                
        if (!DEBUG) {
            dao.save(sample.ownerKey, sample)
        }
    }
    
}

new CleanupSamplesMissingDatasetsScript().run()