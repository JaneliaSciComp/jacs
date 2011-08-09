package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:48 AM
 */
public class EJBOntologyFacade extends EJBEntityFacade implements OntologyFacade {
    @Override
    public List<Entity> getOntologies() {
//        return EJBFactory.getRemoteAnnotationBean().
        // todo fix this
        return new ArrayList<Entity>();
    }

    @Override
    public Entity createOntologyAnnotation(String sessionId, String targetEntityId, String keyEntityId, String keyString, String valueEntityId, String valueString, String tag) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyAnnotation(SessionMgr.getUsername(),
                sessionId, targetEntityId, keyEntityId, keyString, valueEntityId, valueString, tag);
    }

    @Override
    public Entity createOntologyRoot(String ontologyName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyRoot(SessionMgr.getUsername(),
                ontologyName);
    }

    @Override
    public EntityData createOntologyTerm(Long parentEntityId, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyTerm(SessionMgr.getUsername(),
                parentEntityId, label, type, orderIndex);
    }

    @Override
    public Entity getOntologyTree(Long rootEntityId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getOntologyTree(SessionMgr.getUsername(),
                rootEntityId);
    }

    @Override
    public List<Entity> getPrivateOntologies() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getPrivateOntologies(SessionMgr.getUsername());
    }

    @Override
    public List<Entity> getPublicOntologies() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getPublicOntologies();
    }

    @Override
    public Entity publishOntology(Long ontologyEntityId, String rootName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().publishOntology(ontologyEntityId, rootName);
    }

    @Override
    public void removeOntologyTerm(Long termEntityId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(SessionMgr.getUsername(), termEntityId);
    }
}
