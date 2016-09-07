import org.janelia.it.jacs.model.domain.gui.search.Filter
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria
import org.janelia.it.jacs.model.domain.gui.search.criteria.FacetCriteria
import org.janelia.it.jacs.model.domain.sample.*
import org.janelia.it.jacs.model.domain.support.DomainDAO
import org.janelia.it.jacs.model.domain.support.DomainUtils
import org.janelia.it.jacs.model.domain.workspace.TreeNode

/**
 * Walk all tree nodes and make sure that:
 * 1) there are no duplicate children
 * 2) all children actually exist
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
class CleanupTreeNodesScript {

    boolean DEBUG = false
    DomainDAO dao = DomainDAOManager.instance.dao
    
    public void run() {

        for(TreeNode treeNode : dao.getDomainObjects(null, TreeNode.class)) {
            
            boolean dirty = false
            
            List<Reference> oldChildren = treeNode.getChildren()
            List<Reference> newChildren = new ArrayList<>(new LinkedHashSet<>(treeNode.getChildren()))
            
            if (oldChildren.size()!=newChildren.size()) {
                println treeNode.name+" "+treeNode.ownerKey+" changed as a result of duplicate deletion: "+oldChildren.size()+" -> "+newChildren.size()
                dirty = true
            }
            
            int oldSize = newChildren.size()
            try {
                List<Reference> refs = DomainUtils.getReferences(dao.getDomainObjects(null, newChildren))
                if (newChildren.retainAll(refs)) {
                    println treeNode.name+" "+treeNode.ownerKey+" changed as a result of missing object deletion: "+oldSize+" -> "+newChildren.size()
                    dirty = true
                }
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace()
            }
            
            if (dirty) {
                if (!DEBUG) {
                    treeNode.setChildren(newChildren)
                    dao.save(treeNode.ownerKey, treeNode)
                }
            }
        }
    }
}

new CleanupTreeNodesScript().run()