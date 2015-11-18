package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This service loads annotations by creating an ontology to accomodate the annotations in a text file, and then
 * loading them. The following parameters are required:
 * ANNOTATIONS_FILEPATH - absolute path to Arnim's annotations text file
 * ONTOLOGY_NAME - the name of the ontology to use, or create if it doesn't already exist
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationImportService extends AbstractEntityService {
	
    protected Date createDate;
    protected Map<String, HashSet<String>> annotationMap = new HashMap<String,HashSet<String>>();
    protected Entity textAnnot;
	protected int count = 0;
	
    public void execute() throws Exception {
    	
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
    		
    	Set<Entity> matchingOntologies = entityBean.getUserEntitiesByName(ownerKey, ontologyName);
    	
    	if (matchingOntologies!=null && !matchingOntologies.isEmpty()) {
    		//ontologyTree = matchingOntologies.iterator().next();
    		//ontologyTree = annotationBean.getOntologyTree(user.getUserLogin(), ontologyTree.getId());
    		throw new Exception("Reusing an existing ontology is not yet supported. Delete the ontology first.");
    	}
    	
    	ontologyTree = annotationBean.createOntologyRoot(ownerKey, ontologyName);
    	
    	EntityData textAnnotEd = annotationBean.createOntologyTerm(ownerKey, ontologyTree.getId(), "Annotation", OntologyElementType.createTypeByName("Custom"), 0);
    	textAnnot = textAnnotEd.getChildEntity();
    	
//        	EntityData enums = annotationBean.createOntologyTerm(user.getUserLogin(), ontologyTree.getId(), "Enumerations", OntologyElementType.createTypeByName("Category"), 0);
//        	EntityData yesNoEnum = annotationBean.createOntologyTerm(user.getUserLogin(), enums.getId(), "Boolean", OntologyElementType.createTypeByName("Enum"), 0);
//        	EntityData yesEnumItem = annotationBean.createOntologyTerm(user.getUserLogin(), yesNoEnum.getId(), "Yes", OntologyElementType.createTypeByName("EnumItem"), 0);
//        	EntityData noEnumItem = annotationBean.createOntologyTerm(user.getUserLogin(), yesNoEnum.getId(), "No", OntologyElementType.createTypeByName("EnumItem"), 0);

    	logger.info("Creating annotations");

    	for(String entityName : annotationMap.keySet()) {
    		Set<Entity> entities = entityBean.getEntitiesByName(entityName);
    		if (entities==null || entities.isEmpty()) {
    			logger.warn("Could not find entity with the name: "+entityName);
    			continue;
    		}
    		if (entities.size()>1) {
    			logger.warn("Found more than 1 entity with the name: "+entityName);
    			continue;
    		}
    		Entity entity = entities.iterator().next();
    		List<String> annots = new ArrayList<String>(annotationMap.get(entityName));
    		Collections.sort(annots);
    		annotate(entity, annots);

    		if (EntityConstants.TYPE_SCREEN_SAMPLE.equals(entity.getEntityTypeName())) {
	    		Set<Long> parentIds = entityBean.getParentIdsForAttribute(entity.getId(), EntityConstants.ATTRIBUTE_REPRESENTATIVE_SAMPLE);
	    		if (parentIds != null) {
	    			List<Entity> represented = entityBean.getEntitiesById(new ArrayList<Long>(parentIds));	
	    			for(Entity rep : represented) {
	    				annotate(rep, annots);
	    			}
	    		}
    		}
    	}

    	logger.info("Created "+count+" annotations");
    }

    private void annotate(Entity entity, List<String> annots) throws ComputeException {
		for(String annot : annots) {
    		OntologyAnnotation annotation = new OntologyAnnotation(null, entity.getId(), textAnnot.getId(), textAnnot.getName(), null, annot);
    		annotationBean.createOntologyAnnotation(ownerKey, annotation);
    		count++;
		}
		logger.info("Annotated entity: "+entity.getName()+" (id="+entity.getId()+")");
    }
    
    private void readAnnotations(File representativesFile) throws Exception {
		Scanner scanner = new Scanner(representativesFile);
        try {
            while (scanner.hasNextLine()){
                String entityName = scanner.next();
            	String line = scanner.nextLine();
            	if (StringUtils.isEmpty(entityName) || StringUtils.isEmpty(line)) continue;
            	
                HashSet<String> annotSet = annotationMap.get(entityName);
                if (annotSet==null) {
                	annotSet = new HashSet<String>();
                	annotationMap.put(entityName, annotSet);
                }
                
                // Some Arnim-specific processing
                for(String annot : line.split("\t")) {
                	if (!StringUtils.isEmpty(annot)) {
                		if (annot.startsWith("Qi:")) {
                			annot = annot.replaceFirst("(\\d+):(\\d+)", "$1.$2");
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
