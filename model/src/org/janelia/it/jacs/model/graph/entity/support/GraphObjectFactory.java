package org.janelia.it.jacs.model.graph.entity.support;

/**
 * An interface for constructing graph objects from a set of source object types. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface GraphObjectFactory<T,U,V> {

    public Object getNodeInstance(T sourceObject) throws Exception;

    public Object getRelationshipInstance(U sourceObject) throws Exception;

    public Object getPermissionInstance(V sourceObject) throws Exception;
}
