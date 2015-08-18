package org.janelia.it.workstation.gui.browser.api.facade.impl;

import java.util.Collection;
import java.util.List;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Implementation of the DomainFacade using a direct MongoDB connection.
 * 
 * NOT FOR PRODUCTION USE!
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MongoDomainFacade implements DomainFacade {

    protected static final String MONGO_SERVER_URL = "mongo-db";
    protected static final String MONGO_DATABASE = "jacs";
    protected static final String MONGO_USERNAME = "flyportal";
    protected static final String MONGO_PASSWORD = "flyportal";
    
    private final DomainDAO dao;
    
    public MongoDomainFacade() throws Exception {
        this.dao = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
    }
    
    @Override
    public List<Subject> getSubjects() {
        return dao.getSubjects();
    }
    
    @Override
    public DomainObject getDomainObject(Class<? extends DomainObject> domainClass, Long id) {
        return dao.getDomainObject(SessionMgr.getSubjectKey(), domainClass, id);
    }

    @Override
    public DomainObject getDomainObject(Reference reference) {
        return dao.getDomainObject(SessionMgr.getSubjectKey(), reference);
    }

    @Override
    public List<DomainObject> getDomainObjects(List<Reference> references) {
        return dao.getDomainObjects(SessionMgr.getSubjectKey(), references);
    }

    @Override
    public List<DomainObject> getDomainObjects(String type, Collection<Long> ids) {
        return dao.getDomainObjects(SessionMgr.getSubjectKey(), type, ids);
    }

    @Override
    public List<Annotation> getAnnotations(Collection<Long> targetIds) {
        return dao.getAnnotations(SessionMgr.getSubjectKey(), targetIds);
    }

    @Override
    public Workspace getDefaultWorkspace() {
        return dao.getDefaultWorkspace(SessionMgr.getSubjectKey());
    }

    @Override
    public Collection<Workspace> getWorkspaces() {
        return dao.getWorkspaces(SessionMgr.getSubjectKey());
    }

    @Override
    public Collection<Ontology> getOntologies() {
        return dao.getOntologies(SessionMgr.getSubjectKey());
    }

    @Override
    public void changePermissions(String type, Collection<Long> ids, String granteeKey, String rights, boolean grant) throws Exception {
        dao.changePermissions(SessionMgr.getSubjectKey(), type, ids, granteeKey, rights, grant);
    }

    @Override
    public TreeNode create(TreeNode treeNode) throws Exception {
        return dao.save(SessionMgr.getSubjectKey(), treeNode);
    }

    @Override
    public ObjectSet create(ObjectSet objectSet) throws Exception {
        return dao.save(SessionMgr.getSubjectKey(), objectSet);
    }

    @Override
    public Filter create(Filter filter) throws Exception {
        return dao.save(SessionMgr.getSubjectKey(), filter);
    }
    
    @Override
    public Filter update(Filter filter) throws Exception {
        return dao.save(SessionMgr.getSubjectKey(), filter);
    }
    
    @Override
    public TreeNode reorderChildren(TreeNode treeNode, int[] order) throws Exception {
        return dao.reorderChildren(SessionMgr.getSubjectKey(), treeNode, order);
    }

    @Override
    public TreeNode addChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        return dao.addChildren(SessionMgr.getSubjectKey(), treeNode, references);
    }

    @Override
    public TreeNode removeChildren(TreeNode treeNode, Collection<Reference> references) throws Exception {
        return dao.removeChildren(SessionMgr.getSubjectKey(), treeNode, references);
    }
    
    @Override
    public ObjectSet addMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
        return dao.addMembers(SessionMgr.getSubjectKey(), objectSet, references);
    }

    @Override
    public ObjectSet removeMembers(ObjectSet objectSet, Collection<Reference> references) throws Exception {
         return dao.removeMembers(SessionMgr.getSubjectKey(), objectSet, references);
    }

    @Override
    public DomainObject updateProperty(DomainObject domainObject, String propName, String propValue) {
        return dao.updateProperty(SessionMgr.getSubjectKey(), domainObject, propName, propValue);
    }
}
