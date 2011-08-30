package org.janelia.it.jacs.compute.ws;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;

@SOAPBinding(style = SOAPBinding.Style.RPC)
@Stateless(name = "FlyWorkstationWS")
@WebService(endpointInterface = "org.janelia.it.jacs.compute.ws.FlyWorkstationWS")
@Remote(FlyWorkstationWS.class)
@TransactionTimeout(60)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class FlyWorkstationWSBean extends BaseWSBean {

    // todo The Web service layer needs to be flushed out
    public String addAnnotation(@WebParam(name = "owner") String owner,
                                @WebParam(name = "namespace") String namespace,
                                @WebParam(name = "term") String term,
                                @WebParam(name = "value") String value,
                                @WebParam(name = "comment") String comment,
                                @WebParam(name = "conditional") String conditional){
        logger.debug("Web Services - addAnnotation() acknowledged");
        StringBuffer sbuf = new StringBuffer("");
        try {
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
            String annotationId = "";// add annotation here
            sbuf.append("Annotation id=").append(annotationId).append(" successfully added.\n");
        }
        catch (Exception e) {
            String error = "There was a problem adding annotation " + term + ":" + value + " : " + e.getMessage();
            logger.error(error, e);
            sbuf = new StringBuffer(error + "\n");
        }
        logger.debug("Web Services - addAnnotation() complete");
        return sbuf.toString();
    }
    
    public Entity getEntity(@WebParam(name = "entityId") String entityId) {
        logger.debug("Web Services - getEntity() acknowledged");
        Entity entity = null;
        try {
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
            entity = annotationBean.getEntityById(entityId);
            logger.debug("Web Services - getEntity() complete");
        }
        catch (Exception e) {
            logger.error("There was a problem getting entity "+entityId, e);
        }
        return entity;
    }
    
    public Entity getOntologyTree(@WebParam(name = "userLogin") String userLogin, 
    							  @WebParam(name = "rootId") String rootId) {
        logger.debug("Web Services - getOntologyTree() acknowledged");
        Entity entity = null;
        try {
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
            entity = annotationBean.getOntologyTree(userLogin,Long.parseLong(rootId));
            logger.debug("Web Services - getOntologyTree() complete");
        }
        catch (Exception e) {
            logger.error("There was a problem getting ontology "+rootId, e);
        }
        return entity;
    }
    
}
