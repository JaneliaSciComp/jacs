package org.janelia.it.jacs.compute.api;

import java.util.List;

import javax.ejb.Local;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A local interface to queries against specific types in the entity model, mainly
 * ontologies and annotations. Affords access to everything in AnnotationBeanRemote. 
 * 
 * @see AnnotationBeanRemote
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Deprecated
@Local
public interface AnnotationBeanLocal extends AnnotationBeanRemote {

	/**
	 * Just like createOntologyAnnotation, but it doesn't log anything, or trigger a reindexing. Useful for pipelines 
	 * which create annotations en-masse. 
	 * @param userLogin
	 * @param annotation
	 * @return
	 * @throws ComputeException
	 */
	public Entity createSilentOntologyAnnotation(String userLogin, OntologyAnnotation annotation) throws ComputeException;

	/**
	 * Delete an attribute from all entities for a given user. 
	 * @param ownerKey
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void deleteAttribute(String ownerKey, String attributeName) throws ComputeException;
	
	/**
	 * Return the ids of all orphan annotations where the target entity id is no longer in existence. 
	 * @param subjectKey
	 * @return
	 * @throws ComputeException
	 */
	public List<Long> getOrphanAnnotationIdsMissingTargets(String subjectKey) throws ComputeException;
}
