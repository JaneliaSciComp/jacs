package org.janelia.it.jacs.model.graph.support;

/**
 * An interface for constructing graph objects from a set of source object types. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface GraphObjectTransformer<T,U,V> {

	/**
	 * Instantiate the appropriate type of class (marked with the @amp;GraphNode annotation) for the given native graph object.
	 * @param sourceObject
	 * @return
	 * @throws Exception
	 */
    public Object getNodeInstance(T nativeNode) throws Exception;

    /**
     * Instantiate the appropriate type of class (marked with the @amp;GraphRelationship annotation) for the given native graph object.
     * @param sourceObject
     * @return
     * @throws Exception
     */
    public Object getRelationshipInstance(U nativeRelationship) throws Exception;

    /**
     * Instantiate the appropriate type of class (marked with the @amp;GraphPermission annotation) for the given native graph object.
     * @param sourceObject
     * @return
     * @throws Exception
     */
    public Object getPermissionInstance(V nativePermission) throws Exception;
    
    /**
     * Reconstitute the native node object based on the given domain object.
     * @param node
     * @return
     * @throws Exception
     */
    public T getNativeNode(Object node) throws Exception;
    
    /**
     * Reconstitute the native relationship object based on the given domain object.
     * @param node
     * @return
     * @throws Exception
     */
    public U getNativeRelationship(Object node) throws Exception;
    
    /**
     * Reconstitute the native permission object based on the given domain object.
     * @param node
     * @return
     * @throws Exception
     */
    public V getNativePermission(Object node) throws Exception;
}
