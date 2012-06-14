package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This service loads annotations by creating an ontology to accomodate the annotations in a text file, and then
 * loading them. The following parameters are required:
 * ANNOTATIONS_FILEPATH - absolute path to Arnim's annotations text file
 * ONTOLOGY_NAME - the name of the ontology to use, or create if it doesn't already exist
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationImportService implements IService {
	
    protected Logger logger;
    protected Task task;
    protected User user;
    protected Date createDate;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    
    protected Map<String, HashSet<String>> annotations = new HashMap<String,HashSet<String>>();
	
    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            annotationBean = EJBFactory.getLocalAnnotationBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
        	
        	String annotationsFilepath = (String)processData.getItem("ANNOTATIONS_FILEPATH");
        	if (annotationsFilepath == null) {
        		throw new IllegalArgumentException("ANNOTATIONS_FILEPATH may not be null");
        	}

        	String ontologyName = (String)processData.getItem("ONTOLOGY_NAME");
        	if (ontologyName == null) {
        		throw new IllegalArgumentException("ONTOLOGY_NAME may not be null");
        	}
        	
        	readAnnotations(new File(annotationsFilepath));
        	
        	logger.info("Creating ontology");
        	Entity ontologyTree = null;
        		
        	Set<Entity> matchingOntologies = entityBean.getUserEntitiesByName(user.getUserLogin(), ontologyName);
        	
        	if (matchingOntologies!=null && !matchingOntologies.isEmpty()) {
        		//ontologyTree = matchingOntologies.iterator().next();
        		//ontologyTree = annotationBean.getOntologyTree(user.getUserLogin(), ontologyTree.getId());
        		throw new Exception("Reusing an existing ontology is not yet supported. Delete the ontology first.");
        	}
        	
        	ontologyTree = annotationBean.createOntologyRoot(user.getUserLogin(), ontologyName);
        	
        	EntityData textAnnotEd = annotationBean.createOntologyTerm(user.getUserLogin(), ontologyTree.getId(), "Annotation", OntologyElementType.createTypeByName("Custom"), 0);
        	Entity textAnnot = textAnnotEd.getChildEntity();
        	
//        	EntityData enums = annotationBean.createOntologyTerm(user.getUserLogin(), ontologyTree.getId(), "Enumerations", OntologyElementType.createTypeByName("Category"), 0);
//        	EntityData yesNoEnum = annotationBean.createOntologyTerm(user.getUserLogin(), enums.getId(), "Boolean", OntologyElementType.createTypeByName("Enum"), 0);
//        	EntityData yesEnumItem = annotationBean.createOntologyTerm(user.getUserLogin(), yesNoEnum.getId(), "Yes", OntologyElementType.createTypeByName("EnumItem"), 0);
//        	EntityData noEnumItem = annotationBean.createOntologyTerm(user.getUserLogin(), yesNoEnum.getId(), "No", OntologyElementType.createTypeByName("EnumItem"), 0);

        	logger.info("Creating annotations");

        	int c = 0;
        	for(String entityName : annotations.keySet()) {
        		Set<Entity> entities = entityBean.getEntitiesByName(entityName);
        		if (entities==null || entities.isEmpty()) {
        			logger.error("Could not find entity with the name: "+entityName);
        			continue;
        		}
        		if (entities.size()>1) {
        			logger.error("Found more than 1 entity with the name: "+entityName);
        			continue;
        		}
        		Entity entity = entities.iterator().next();
        		List<String> annots = new ArrayList<String>(annotations.get(entityName));
        		Collections.sort(annots);
        		for(String annot : annots) {
            		OntologyAnnotation annotation = new OntologyAnnotation(null, entity.getId(), textAnnot.getId(), textAnnot.getName(), null, annot);
            		annotationBean.createOntologyAnnotation(user.getUserLogin(), annotation);
            		c++;
        		}
        		logger.info("Annotated entity: "+entity.getName()+" (id="+entity.getId()+")");
        	}

        	logger.info("Created "+c+" annotations");
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void readAnnotations(File representativesFile) throws Exception {
		Scanner scanner = new Scanner(representativesFile);
        try {
            while (scanner.hasNextLine()){
                String entityName = scanner.next();
            	String line = scanner.nextLine();
            	if (StringUtils.isEmpty(entityName) || StringUtils.isEmpty(line)) continue;
            	
                HashSet<String> annotSet = annotations.get(entityName);
                if (annotSet==null) {
                	annotSet = new HashSet<String>();
                	annotations.put(entityName, annotSet);
                }
                
                // Some Arnim-specific processing
                for(String annot : line.split("\t")) {
                	if (!StringUtils.isEmpty(annot)) {
                		if (annot.startsWith("Qi:")) {
                			annot = annot.replaceFirst(":", ".");
                		}
                		annot = annot.replaceAll(":", "_");
                		annotSet.add(annot);
                	}
                }
            }
        }
        finally {
        	scanner.close();
        }
    }
}
