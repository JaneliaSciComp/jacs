package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Exports workstation artifacts into the SAGE database. This service has two modes, which are used in the 
 * pipeline of the same name:
 * 
 * - GET_SAMPLES_NEEDING_ARTIFACTS: This mode gathers samples which have been annotated for export and filters out
 *     those which have already been exported. It then produces two lists, SAMPLE_20X_ID and SAMPLE_60X_ID, which
 *     contain those samples that still need MBEW artifacts to be generated.
 *     
 * - EXPORT_TO_SAGE: This mode re-gathers the samples which have been annotated for export and then actually
 *     creates corresponding rows in SAGE (unless they already exist). If everything is succcessful, it then annotates
 *     the same as having been exported.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageArtifactExportService extends AbstractEntityService {

    private static final String WEBDAV_PREFIX = "http://jacs-webdav.int.janelia.org/WebDAV";
    private static final String NO_CONSENSUS = "No Consensus";
    private static final String CREATED_BY = "Janelia Workstation";
    private static final String PUBLISHED_TO = "MBEW";
    private static final String ARTIFACT_PIPELINE_RUN_PREFIX = "MBEW Pipeline";
    private static final String ANNOTATION_EXPORT_20X = "Publish20xToMBEW";
    private static final String ANNOTATION_EXPORT_63X = "Publish63xToMBEW";
    private static final String ANNOTATION_EXPORTED = "PublishedToMBEW";
    
    private String mode;
    private SageDAO sage;
    private Map<String,Line> lines = new HashMap<String,Line>();
    private Map<String,List<Integer>> sourceSageImageIdsByArea;
    private Date createDate;
    
    private CvTerm productMip;
    private CvTerm productTranslation;
    private CvTerm productTranslationReference;
    private CvTerm propertyPublished;
    private CvTerm source;
    private CvTerm chanSpec;
    private CvTerm dimensionX;
    private CvTerm dimensionY;
    private CvTerm dimensionZ;
    private Entity publishedTerm;
    
    public void execute() throws Exception {
        
        this.mode = data.getRequiredItemAsString("MODE");

        for(Entity entity : entityBean.getEntitiesByNameAndTypeName(ownerKey, ANNOTATION_EXPORTED, EntityConstants.TYPE_ONTOLOGY_ELEMENT)) {
            if (publishedTerm!=null) {
                logger.warn("Found multiple terms with name "+ANNOTATION_EXPORTED+" will use "+publishedTerm.getId());
            }
            else {
                publishedTerm = entity;
            }
        }
        
        if ("GET_SAMPLES_NEEDING_ARTIFACTS".equals(mode)) {
            List<Entity> samples20x = getSamplesForExport(ANNOTATION_EXPORT_20X);
            List<Long> sampleIds20x = getSampleIdsNeedingArtifacts(samples20x);
            data.putItem("SAMPLE_20X_ID", sampleIds20x);
            List<Entity> samples63x = getSamplesForExport(ANNOTATION_EXPORT_63X);
            List<Long> sampleIds63x = getSampleIdsNeedingArtifacts(samples63x);
            data.putItem("SAMPLE_60X_ID", sampleIds63x);
        }
        else if ("EXPORT_TO_SAGE".equals(mode)) {
            List<Entity> samples = getSamplesForExport(ANNOTATION_EXPORT_20X);
            samples.addAll(getSamplesForExport(ANNOTATION_EXPORT_63X));
            exportSamples(samples);
        } 
    }
    
    private List<Entity> getSamplesForExport(String annotationTerm) throws Exception {
        
        logger.info("Finding samples annotated with '"+annotationTerm+"'...");
        
        List<Entity> samples = new ArrayList<Entity>();
        for(Entity sample : annotationBean.getEntitiesAnnotatedWithTerm(ownerKey, annotationTerm)) {
            if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                logger.warn("  Entity annotated with '"+annotationTerm+"' is not a sample: "+sample.getId());
                continue;
            }
            if (ANNOTATION_EXPORT_20X.equals(annotationTerm)) {
                Entity os = getObjectiveSample(sample, Objective.OBJECTIVE_20X);
                if (os==null) {
                    logger.warn("  Entity annotated with '"+annotationTerm+"' does not have a 20x sample: "+sample.getId());
                }
                else {
                    addIfNotAlreadyExported(samples, os);   
                }
            }
            else if (ANNOTATION_EXPORT_63X.equals(annotationTerm)) {
                Entity os = getObjectiveSample(sample, Objective.OBJECTIVE_63X);
                if (os==null) {
                    logger.warn("  Entity annotated with '"+annotationTerm+"' does not have a 63x sample: "+sample.getId());
                }
                else {
                    addIfNotAlreadyExported(samples, os);
                }
            }   
        }
        return samples;
    }
    
    private void addIfNotAlreadyExported(List<Entity> samples, Entity sample) throws Exception {
        if (publishedTerm!=null) {
            for(Entity annotation : annotationBean.getAnnotationsForEntity(ownerKey, sample.getId())) {
                String keyEntityId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
                if (keyEntityId!=null && keyEntityId.equals(publishedTerm.getId().toString())) {
                    logger.info("  Sample was already exported: "+sample.getName());
                    return;
                }
            }
        }
        
        logger.info("  Sample will be exported: "+sample.getName());
        samples.add(sample);    
    }
    
    private Entity getObjectiveSample(Entity sample, Objective objective) throws Exception {
        String sampleObjective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        if (objective.getName().equals(sampleObjective)) {
            return sample;
        }
        entityLoader.populateChildren(sample);
        for(Entity child : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE)) {
            String childObjective = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective.getName().equals(childObjective)) {
                return child;
            }    
        }
        return null;
    }
    
    private List<Long> getSampleIdsNeedingArtifacts(List<Entity> samples) throws Exception {
        List<Long> sampleIds = new ArrayList<Long>();
        for(Entity sample : samples) {
            if (getArtifactRun(sample)==null) {
                logger.info("Sample needs MBEW artifact generation "+sample.getName());
                sampleIds.add(sample.getId());
            }
        }
        return sampleIds;
    }
    
    private Entity getArtifactRun(Entity sample) throws Exception {
        entityLoader.populateChildren(sample);
        for(Entity child : sample.getChildren()) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_PIPELINE_RUN) && child.getName().startsWith(ARTIFACT_PIPELINE_RUN_PREFIX)) {
                return child;
            }
        }
        return null;
    }
        
    private void exportSamples(List<Entity> samples) throws Exception {
        
        this.sage = new SageDAO(logger);
        this.createDate = new Date();
        this.productMip = getCvTermByName("product","projection_all");
        this.productTranslation = getCvTermByName("product","translation");
        this.productTranslationReference = getCvTermByName("product","translation_reference");
        this.propertyPublished = getCvTermByName("light_imagery","published_to");
        this.source = getCvTermByName("lab","JFRC");
        this.chanSpec = getCvTermByName("light_imagery","channel_spec");
        this.dimensionX = getCvTermByName("light_imagery","dimension_x");
        this.dimensionY = getCvTermByName("light_imagery","dimension_y");
        this.dimensionZ = getCvTermByName("light_imagery","dimension_z");

        if (publishedTerm==null) {
            logger.error("No ontology term found with name "+ANNOTATION_EXPORTED+". Exported samples will not be annotated as exported!");
        }
        
        for(Entity sample : samples) {
            exportSample(sample);
            sample.setEntityData(null); // free memory
        }
    }
    
    private void exportSample(Entity sample) throws Exception {

        logger.info("Exporting "+sample.getName());
        
        // Find fly line
        
        String lineName = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE);
        Line line = lines.get(lineName);
        if (line==null) {
            line = sage.getLineByName(lineName); 
            lines.put(lineName, line);
        }
        
        // Find source ids for the primary sample image
        
        this.sourceSageImageIdsByArea = new HashMap<String,List<Integer>>();
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sample)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsmStack) throws Exception {
                String sageId = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
                String area = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                if (sageId!=null) {
                    List<Integer> ids = sourceSageImageIdsByArea.get(area);
                    if (ids==null) {
                        ids = new ArrayList<Integer>();
                        sourceSageImageIdsByArea.put(area, ids);
                    }
                    ids.add(new Integer(sageId));    
                }
            }
        });
        
        // Collect artifacts for export
        Entity artifactRun = getArtifactRun(sample);
        if (artifactRun==null) {
            logger.error("  Sample has no MBEW artifacts to export");
            return;
        }
        entityLoader.populateChildren(artifactRun);

        exportArtifactsForArea(sample, artifactRun, "Brain", line);
        exportArtifactsForArea(sample, artifactRun, "VNC", line); 
        
        if (publishedTerm!=null) {
            OntologyAnnotation annotation = new OntologyAnnotation(null, sample.getId(), publishedTerm.getId(), publishedTerm.getName(), null, null);
            annotationBean.createOntologyAnnotation(ownerKey,annotation);
        }
    }
    
    private void exportArtifactsForArea(Entity sample, Entity artifactRun, String area, Line line) throws Exception {

        List<Integer> sourceSageImageIds = sourceSageImageIdsByArea.get(area);
        if (sourceSageImageIds==null) {
            if ("Brain".equals(area)) {
                sourceSageImageIds = sourceSageImageIdsByArea.get("");
                if (sourceSageImageIds==null) {
                    logger.warn("No source images found for Brain in "+sample.getName());   
                }
            }
            else {
                // This sample has no VNC
                return;    
            }
        }
        
        Image sourceImage = null;
        if (sourceSageImageIds.size()==1) {
            sourceImage = sage.getImages(sourceSageImageIds).get(0);
        }
        else {
            Entity image3d = EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean))
                .startAt(sample)
                .childOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                .withAttribute(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, area)
                .childrenOfAttr(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)
                .getLast();
            sourceImage = getOrCreatePrimaryImage(image3d, line, sourceSageImageIds);
        }
        
        for(Entity child : artifactRun.getChildren()) {
            String name = child.getName();
            if (name.endsWith(area+".avi")) {
                getOrCreateSecondaryImage(child, productTranslationReference, sourceImage);
                
            }
            else if (name.endsWith(area+"_MIP.png")) {
                getOrCreateSecondaryImage(child, productMip, sourceImage);
                
            }
            else if (name.endsWith(area+"_Signal.avi")) {
                getOrCreateSecondaryImage(child, productTranslation, sourceImage);
                
            }
            else if (name.endsWith(area+"_MIP_Signal.png")) {
                // Ignored
            }
        }
    }
    
    private Image getOrCreatePrimaryImage(Entity entity, Line line, List<Integer> sourceSageImageIds) throws Exception {
        
        String imageName = entity.getId()+"-"+entity.getName();
        Image image = sage.getImageByName(imageName);
        if (image!=null) {
            // TODO: update image properties?
            logger.info("  Image already exists in SAGE as "+image.getId()+" with name "+image.getName());
            return image;
        }
        
        Map<CvTerm,String> consensusValues = new HashMap<CvTerm,String>();
        CvTerm consensusFamily = null;
        
        for(Image sourceImage : sage.getImages(sourceSageImageIds)) {
            
            if (consensusFamily!=null && !sourceImage.getFamily().equals(consensusFamily)) {
                throw new Exception("No family consensus across SAGE images: "+sourceSageImageIds+" ("+sourceImage.getFamily().getId()+"!="+consensusFamily.getId()+")");
            }
            
            consensusFamily = sourceImage.getFamily();
            
            for(ImageProperty prop : sourceImage.getImageProperties()) {
                if (consensusValues.containsKey(prop.getType())) {
                    String value = consensusValues.get(prop.getType());
                    if ((value==null && prop.getValue()==null) || value.equals(prop.getValue())) {
                        // Consensus 
                    }
                    else {
                        consensusValues.put(prop.getType(), NO_CONSENSUS);
                    }
                }
                else {
                    consensusValues.put(prop.getType(), prop.getValue());
                }
            }
        }
        
        String channelSpec = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        
        consensusValues.put(propertyPublished, PUBLISHED_TO);
        consensusValues.put(chanSpec, channelSpec);
        consensusValues.put(dimensionX, null);
        consensusValues.put(dimensionY, null);
        consensusValues.put(dimensionZ, null);
        
        String pixelRes = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
        if (pixelRes!=null) {
            String[] res = pixelRes.split("x");
            if (res.length==3) {
                consensusValues.put(dimensionX, res[0]);
                consensusValues.put(dimensionY, res[1]);
                consensusValues.put(dimensionZ, res[2]);
            }
            else {
                logger.warn("Cannot parse pixel resolution: "+res);
            }
        }
        
        logger.info("Exporting "+entity.getName());

        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        image = new Image(consensusFamily, line, source, imageName, getUrl(path), path, true, true, CREATED_BY, createDate);
        
        for(CvTerm type : consensusValues.keySet()) {
            type.getId(); // ensure that the type id is loaded
            String value = consensusValues.get(type);
            logger.info("      "+type.getName()+": "+value);
            if (value == null || value.equals(NO_CONSENSUS)) continue;
            addImageProperty(image, type, value);
        }
        
        image = sage.saveImage(image);
        logger.info("  Created SAGE primary image "+image.getId()+" with name "+image.getName());
        return image;
    }
    
    private ImageProperty addImageProperty(Image image, CvTerm type, String value) {
        ImageProperty prop = new ImageProperty(type, image, value, createDate);
        image.getImageProperties().add(prop);
        return prop;
    }
    
    private ImageProperty setImageProperty(Image image, CvTerm type, String value) throws Exception {
    	for(ImageProperty property : image.getImageProperties()) {
    		if (property.getType().equals(type)) {
    			property.setValue(value);
    			return sage.saveImageProperty(property);
    		}
    	}
        ImageProperty prop = new ImageProperty(type, image, value, createDate);
        image.getImageProperties().add(prop);
        sage.saveImageProperty(prop);
        return prop;
    }
    
    private SecondaryImage getOrCreateSecondaryImage(Entity entity, CvTerm productType, Image sourceImage) throws Exception {

        String imageName = entity.getId()+"-"+entity.getName();
        SecondaryImage secondaryImage = sage.getSecondaryImageByName(imageName);
        if (secondaryImage!=null) {
            // TODO: update image properties?
            logger.info("  Secondary image already exists in SAGE as "+secondaryImage.getId()+" with name "+secondaryImage.getName());
            return secondaryImage;
        }
        
        
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        secondaryImage = new SecondaryImage(sourceImage, productType, entity.getId()+"-"+entity.getName(), path, getUrl(path), createDate);
        sage.saveSecondaryImage(secondaryImage);
        logger.info("  Created SAGE secondary image "+secondaryImage.getId()+" with name "+secondaryImage.getName());
        return secondaryImage;
    }
    
    private String getUrl(String filepath) {
        return WEBDAV_PREFIX+filepath;
    }
    
    private CvTerm getCvTermByName(String cvName, String termName) throws DaoException {
        CvTerm term = sage.getCvTermByName(cvName, termName);
        if (term==null) {
            throw new IllegalStateException("No such term: "+termName+" in CV "+cvName);
        }
        return term;
    }
}
