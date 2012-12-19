package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Find annotations with missing targets or ontology terms.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OrphanAnnotationCheckerService implements IService {

    public transient static final String PARAM_removeAnnotationsMissingTargets = "remove annotations missing targets";
    public transient static final String PARAM_removeAnnotationsMissingTerms = "remove annotations missing terms";
    
    protected Logger logger;
    protected Task task;
    protected String username;
    protected EntityBeanLocal entityBean;
    
    private boolean deleteAnnotationsMissingTargets = false;
    private boolean deleteAnnotationsMissingTerms = false;
    
    private int numAnnotations = 0;
    private int numAnnotationsMissingTargets = 0;
    private int numAnnotationsMissingTerms = 0;
    private int numAnnotationsDeleted = 0;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            username = task.getOwner();
            deleteAnnotationsMissingTargets = Boolean.parseBoolean(task.getParameter(PARAM_removeAnnotationsMissingTargets));
            deleteAnnotationsMissingTerms = Boolean.parseBoolean(task.getParameter(PARAM_removeAnnotationsMissingTerms));

            logger.info("Orphan annotations checker");
            logger.info("    deleteAnnotationsMissingTargets="+deleteAnnotationsMissingTargets);
            logger.info("    deleteAnnotationsMissingTerms="+deleteAnnotationsMissingTerms);
            
            logger.info("Finding orphan annotations");
            List<Entity> annotations = entityBean.getEntitiesByTypeName(EntityConstants.TYPE_ANNOTATION);

            logger.info("Processing "+annotations.size()+" annotations");            
            List<Long> toDelete = new ArrayList<Long>();
            for(Entity entity : annotations) {
            	OntologyAnnotation annotation = new OntologyAnnotation();
            	annotation.init(entity);
            	Long targetEntityId = annotation.getTargetEntityId();
            	Long termEntityId = annotation.getKeyEntityId();
            	Long valueEntityId = annotation.getValueEntityId();
            	
            	if (targetEntityId!=null) {
            		if (entityBean.getEntityById(targetEntityId.toString()) == null) {
                		logger.info("Annotation "+entity.getId()+" (by "+entity.getOwnerKey()+") is missing its target, "+targetEntityId);
                		if (deleteAnnotationsMissingTargets) {
                			numAnnotationsMissingTargets++;
                			toDelete.add(entity.getId());
                		}
            		}
            	}
            	else if (termEntityId!=null) {
            		if (entityBean.getEntityById(termEntityId.toString()) == null) {
                		logger.info("Annotation "+entity.getId()+" (by "+entity.getOwnerKey()+") is missing its key term, "+termEntityId);
                		if (deleteAnnotationsMissingTerms) {
                			numAnnotationsMissingTerms++;
                			toDelete.add(entity.getId());
                		}
            		}
            	}
            	else if (valueEntityId!=null) {
            		if (entityBean.getEntityById(valueEntityId.toString()) == null) {
                		logger.info("Annotation "+entity.getId()+" (by "+entity.getOwnerKey()+") is missing its value term, "+valueEntityId);
                		if (deleteAnnotationsMissingTerms) {
                			numAnnotationsMissingTerms++;
                			toDelete.add(entity.getId());
                		}
            		}
            	}
            	
            	numAnnotations++;
            }
            
            logger.info("Deleting "+toDelete.size()+" annotations");
            for(Long id : toDelete) {
            	entityBean.deleteEntityById(id);
            }
            
            logger.info("Considered "+numAnnotations+" annotations. "+numAnnotationsMissingTargets
            		+" were missing targets, and "+numAnnotationsMissingTerms+" were missing terms. Deleted "
            		+numAnnotationsDeleted+" annotations.");
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running OrphanAnnotationCheckerService:" + e.getMessage(), e);
        }
    }
    
    
}
