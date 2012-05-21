package org.janelia.it.jacs.compute.api;

import javax.ejb.Local;

/**
 * A local interface to queries against specific types in the entity model, mainly
 * ontologies and annotations. Affords access to everything in AnnotationBeanRemote. 
 * 
 * @see AnnotationBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Local
public interface AnnotationBeanLocal extends AnnotationBeanRemote {
	
}
