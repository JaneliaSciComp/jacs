import org.janelia.it.jacs.model.domain.gui.search.Filter
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO
import org.janelia.it.jacs.model.domain.support.DomainUtils;

/**
 * Walk all data sets filters and add the data set owner to each one that is not owned by the data set owner.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupDataSetFilterNamesScript {

    boolean DEBUG = false
    DomainDAO dao = DomainDAOManager.instance.dao
    Set dataSetNames = new HashSet()
    
    public void run() {

        for(DataSet dataSet : dao.getDataSets(null)) {
            dataSetNames.add(dataSet.name)
        }
        
        for(Filter filter : dao.getDomainObjects(null, Filter.class)) {
            if (dataSetNames.contains(filter.name)) {
                cleanup(filter)   
            }
        }
    }
    
    def cleanup(Filter filter) {
        
        println ""+filter.name+" ("+filter.ownerKey+")"
        
        boolean dirty = false

        for(Criteria criteria : filter.getCriteriaList()) {
            if (criteria instanceof FacetCriteria) {
                FacetCriteria ac = (FacetCriteria)criteria
                if (ac.attributeName=="dataSet") {
                    String value = ac.values.getAt(0)
                    
                    String filterUser = DomainUtils.getNameFromSubjectKey(filter.ownerKey)
                   
                    if (!value.startsWith(filterUser)) {
                        String newName = filter.name+" ("+value.split("_")[0]+")"
                        println "  Fixing data set filter name -> "+newName
                        filter.setName(newName)
                        dirty = true
                        break
                    }
                }
            }
        }
        
        if (dirty) {
            if (!DEBUG) {
                dao.save(filter.ownerKey, filter)
            }
        }

    }
    
}

new CleanupDataSetFilterNamesScript().run()