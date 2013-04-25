package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

/**
 * Find annotations with missing targets or ontology terms.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OrphanAnnotationCheckerService extends AbstractEntityService {

    public transient static final String PARAM_removeAnnotationsMissingTargets = "remove annotations missing targets";
    public transient static final String PARAM_removeAnnotationsMissingTerms = "remove annotations missing terms";
    
    private boolean deleteAnnotationsMissingTargets = false;
    private boolean deleteAnnotationsMissingTerms = false;
    
    public void execute() throws Exception {

        deleteAnnotationsMissingTargets = Boolean.parseBoolean(task.getParameter(PARAM_removeAnnotationsMissingTargets));
        deleteAnnotationsMissingTerms = Boolean.parseBoolean(task.getParameter(PARAM_removeAnnotationsMissingTerms));

        logger.info("Orphan annotations checker");
        logger.info("    deleteAnnotationsMissingTargets="+deleteAnnotationsMissingTargets);
        logger.info("    deleteAnnotationsMissingTerms="+deleteAnnotationsMissingTerms+" (this feature is not implemented at this time)");
    
        logger.info("Finding orphan annotations missing targets");
        List<Long> annotationIds = annotationBean.getOrphanAnnotationIdsMissingTargets(null);
        logger.info("Found "+annotationIds.size()+" annotations");
        
        int numDeleted = 0;
        
        if (deleteAnnotationsMissingTargets) {
            logger.info("Deleting "+annotationIds.size()+" annotations");
            for(Long id : annotationIds) {
            	entityBean.deleteEntityById(id);
            	numDeleted++;
            }
        }
        
        logger.info("Done with orphan annotation deletion. Deleted "+numDeleted+" annotation entities.");
    }
}
