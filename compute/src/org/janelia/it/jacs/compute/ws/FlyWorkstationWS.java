package org.janelia.it.jacs.compute.ws;

import org.janelia.it.jacs.model.entity.Entity;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;
import java.rmi.RemoteException;

@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService()
public interface FlyWorkstationWS extends Remote {

	public String addAnnotation(@WebParam(name = "owner") String owner,
                            @WebParam(name = "namespace") String namespace,
                            @WebParam(name = "term") String term,
                            @WebParam(name = "value") String value,
                            @WebParam(name = "comment") String comment,
                            @WebParam(name = "conditional") String conditional) throws RemoteException;

    public String deleteAnnotation(@WebParam(name = "owner") String owner,
                                   @WebParam(name = "annotatedEntityId") String annotatedEntityId,
                                   @WebParam(name = "tag") String tag) throws RemoteException;

    public String getAnnotationsForUser(@WebParam(name = "owner") String owner) throws RemoteException;

    public String editAnnotation(@WebParam(name = "owner") String owner,
                           @WebParam(name = "uniqueIdentifier") String uniqueIdentifier,
                           @WebParam(name = "namespace") String namespace,
                           @WebParam(name = "term") String term,
                           @WebParam(name = "value") String value,
                           @WebParam(name = "comment") String comment,
                           @WebParam(name = "conditional") String conditional) throws RemoteException;
    
    public Entity getEntity(@WebParam(name = "entityId") String entityId) throws RemoteException;

    public Entity getOntologyTree(@WebParam(name = "userLogin") String userLogin, 
    							  @WebParam(name = "rootId") String rootId) throws RemoteException;
    
}
