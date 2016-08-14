package org.janelia.it.jacs.compute.service.domain;

/**
 * Find annotations with missing targets or ontology terms.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OrphanAnnotationCheckerService extends AbstractDomainService {

    public transient static final String PARAM_deleteAnnotationsMissingTargets = "remove annotations missing targets";
    public transient static final String PARAM_deleteAnnotationsMissingTerms = "remove annotations missing terms";
    
    private boolean deleteAnnotationsMissingTargets = false;
    private boolean deleteAnnotationsMissingTerms = false;
    
    public void execute() throws Exception {

        deleteAnnotationsMissingTargets = Boolean.parseBoolean(task.getParameter(PARAM_deleteAnnotationsMissingTargets));
        deleteAnnotationsMissingTerms = Boolean.parseBoolean(task.getParameter(PARAM_deleteAnnotationsMissingTerms));

        logger.info("Finding orphan annotations for "+ownerKey);
        logger.info("    deleteAnnotationsMissingTargets="+deleteAnnotationsMissingTargets);
        logger.info("    deleteAnnotationsMissingTerms="+deleteAnnotationsMissingTerms+" (this feature is not implemented at this time)");
    
        // TODO: port this service to use domain objects
        throw new UnsupportedOperationException("This service is out of order");
        
//        List<Long> annotationIds = annotationBean.getOrphanAnnotationIdsMissingTargets(ownerKey);
//        logger.info("Found "+annotationIds.size()+" orphan annotations");
//        
//        int numDeleted = 0;
//        
//        if (deleteAnnotationsMissingTargets) {
//            for(Long id : annotationIds) {
//            	entityBean.deleteEntityById(id);
//            	numDeleted++;
//            }
//        }
//        else {
//            logger.info("Orphan annotations:");
//            for(Entity entity : entityBean.getEntitiesById(annotationIds)) {
//                logger.info(entity.getEntityTypeName()+" - "+entity.getName()+" (id="+entity.getId()+")");
//            }
//        }
//        
//        logger.info("Done with orphan annotation deletion. Deleted "+numDeleted+" annotation entities.");
    }
}
