package org.janelia.it.jacs.compute.ws;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.rmi.Remote;
import java.rmi.RemoteException;

@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService()
public interface FlyWorkstationWS extends Remote {

    String addAnnotation(@WebParam(name = "owner") String owner,
                            @WebParam(name = "namespace") String namespace,
                            @WebParam(name = "term") String term,
                            @WebParam(name = "value") String value,
                            @WebParam(name = "comment") String comment,
                            @WebParam(name = "conditional") String conditional) throws RemoteException;

    String deleteAnnotation(@WebParam(name = "owner") String owner,
                             @WebParam(name = "uniqueIdentifier") String uniqueIdentifier) throws RemoteException;

    String getAnnotationsForUser(@WebParam(name = "owner") String owner) throws RemoteException;

    String editAnnotation(@WebParam(name = "owner") String owner,
                           @WebParam(name = "uniqueIdentifier") String uniqueIdentifier,
                           @WebParam(name = "namespace") String namespace,
                           @WebParam(name = "term") String term,
                           @WebParam(name = "value") String value,
                           @WebParam(name = "comment") String comment,
                           @WebParam(name = "conditional") String conditional) throws RemoteException;
}


