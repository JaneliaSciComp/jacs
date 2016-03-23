package org.janelia.it.jacs.compute.service.domain.util;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.io.File;
import java.util.*;

/**
 * A helper class for dealing with common entities such as default images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainHelper {

	private static final boolean DEBUG = false;

    public static final String OBJECTSET_CLASSNAME = ObjectSet.class.getCanonicalName();
    public static final String TREENODE_CLASSNAME = TreeNode.class.getCanonicalName();
    public static final String IMAGE_CLASSNAME = Image.class.getCanonicalName();

    /**
     * Create/return a child ObjetSet.
     * @param parentFolder
     * @param childName
     * @return
     * @throws Exception
     */
    static public ObjectSet createChildObjectSet(TreeNode parentFolder, String subjectKey, String childName) throws Exception {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        if (parentFolder.getChildren()!=null) {
            for (Reference childReference : parentFolder.getChildren()) {
                if (!childReference.getTargetClassName().equals(OBJECTSET_CLASSNAME)) {
                    continue;
                }
                DomainObject child = dao.getDomainObject(subjectKey, childReference);
                if (child.getName().equals(childName)) {
                    return (ObjectSet) child;
                }
            }
        }

        // We need to create a new folder
        ObjectSet set = new ObjectSet();
        set.setName(childName);
        set =  dao.save(subjectKey, set);
        List<Reference> childRef = new ArrayList<>();
        childRef.add(new Reference(OBJECTSET_CLASSNAME, set.getId()));
        dao.addChildren(subjectKey, parentFolder,childRef);
        return set;
    }

    /**
     * Create/return a child TreeNode.
     * @param parentFolder
     * @param childName
     * @return
     * @throws Exception
     */
    static public TreeNode createChildFolder(TreeNode parentFolder, String subjectKey, String childName, Integer index) throws Exception {
        DomainDAO dao = DomainDAOManager.getInstance().getDao();
        if (parentFolder.getChildren()!=null) {
            for (Reference childReference : parentFolder.getChildren()) {
                if (!childReference.getTargetClassName().equals(TREENODE_CLASSNAME)) {
                    continue;
                }
                DomainObject child = dao.getDomainObject(subjectKey, childReference);
                if (child.getName().equals(childName)) {
                    return (TreeNode) child;
                }
            }
        }

        // We need to create a new folder
        TreeNode node = new TreeNode();
        node.setName(childName);
        node =  dao.save(subjectKey, node);
        List<Reference> childRef = new ArrayList<>();
        childRef.add(new Reference(TREENODE_CLASSNAME, node.getId()));
        dao.addChildren(subjectKey, parentFolder, childRef, index);
        return node;
    }



}
