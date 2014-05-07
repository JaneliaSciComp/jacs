package org.janelia.it.jacs.compute.mongo;

import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Folder;
import org.janelia.it.jacs.model.domain.MaterializedView;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.TreeNode;
import org.janelia.it.jacs.model.domain.Workspace;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * Unit tests for workspaces, folders, and views.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class WorkspaceTest extends MongoDbTest {
    
    @Test
    public void testSubjects() {
        for(Subject subject : dao.getCollection("subject").find().as(Subject.class)) {
            Assert.notNull(subject.getId());
            Assert.notNull(subject.getKey());
        }
    }
    
    @Test
    public void testWorkspaceLeetlab() {
        testWorkspace("group:leetlab");
    }

    @Test
    public void testWorkspaceAsoy() {
        testWorkspace("user:asoy");
    }

    @Test
    public void testWorkspaceNerna() {
        testWorkspace("user:nerna");
    }

    @Test
    public void testUserWorkspaceIncludesGroupWorkspace() {

        String userKey = "user:leey10";
        String groupKey = "group:leetlab";        

        Set<Long> userObjIds = new HashSet<Long>();
        for(Workspace workspace : dao.getWorkspaces(userKey)) {
            for(DomainObject obj : dao.getDomainObjects(userKey, workspace.getChildren())) {
                userObjIds.add(obj.getId());
            }
        }
        
        Set<Long> groupObjIds = new HashSet<Long>();
        for(Workspace workspace : dao.getWorkspaces(groupKey)) {
            for(DomainObject obj : dao.getDomainObjects(groupKey, workspace.getChildren())) {
                groupObjIds.add(obj.getId());
            }
        }

        Assert.isTrue(userObjIds.containsAll(groupObjIds));
    }
    
    private void testWorkspace(String subjectKey) {
        int numWorkspaces = 0;
        int numFolders = 0;
        int numViews = 0;
        int numChildren = 0;
        for(Workspace workspace : dao.getWorkspaces(subjectKey)) {
            numWorkspaces += 1;
            for(DomainObject obj : dao.getDomainObjects(subjectKey, workspace.getChildren())) {
                numWorkspaces += 1;
                if (obj instanceof MaterializedView) {
                    numViews += 1;
                }
                else if (obj instanceof Folder) {
                    numFolders += 1;
                }
                TreeNode node = (TreeNode)obj;
                for(DomainObject child : dao.getDomainObjects(subjectKey, node.getChildren())) {
                    if (child==null) continue;
                    numChildren += 1;
                }
            }
        }
        
        Assert.isTrue(numWorkspaces>0);
        Assert.isTrue(numFolders>0);
        Assert.isTrue(numViews>0);
        Assert.isTrue(numChildren>0);
    }
    
}
