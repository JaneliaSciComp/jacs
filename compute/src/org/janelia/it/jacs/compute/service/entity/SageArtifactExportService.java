package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
 * Synchronizes workstation artifacts into the SAGE database for all annotated samples. 
 * If everything is successful, it then annotates the samples as having been exported.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageArtifactExportService extends AbstractEntityService {

	public static final String CREATED_BY = "Janelia Workstation";
	
    private static final String WEBDAV_PREFIX = "http://jacs-webdav.int.janelia.org/WebDAV";
    private static final String NO_CONSENSUS = "No Consensus";
    private static final String PUBLISHED_TO = "MBEW";
    private static final String ARTIFACT_PIPELINE_RUN_PREFIX = "MBEW Pipeline";
    private static final String ANNOTATION_EXPORT_20X = "Publish20xToMBEW";
    private static final String ANNOTATION_EXPORT_63X = "Publish63xToMBEW";
    private static final String ANNOTATION_EXPORTED = "PublishedToMBEW";
    
    private SageDAO sage;
    private Map<String,Line> lines = new HashMap<String,Line>();
    private Map<String,List<Integer>> sourceSageImageIdsByArea;
    private Date createDate;

    private Entity publishedTerm;
    private CvTerm productMultichannelMip;
    private CvTerm productMultichannelTranslation;
    private CvTerm productSignalsMip;
    private CvTerm productSignalsTranslation;
    private CvTerm productSignal1Mip;
    private CvTerm productSignal1Translation;
    private CvTerm propertyPublished;
    private CvTerm source;
    private CvTerm chanSpec;
    private CvTerm dimensionX;
    private CvTerm dimensionY;
    private CvTerm dimensionZ;
    private List<String> exportedNames = new ArrayList<String>();
    
    public void execute() throws Exception {

        for(Entity entity : entityBean.getEntitiesByNameAndTypeName(ownerKey, ANNOTATION_EXPORTED, EntityConstants.TYPE_ONTOLOGY_ELEMENT)) {
            if (publishedTerm!=null) {
                logger.warn("Found multiple terms with name "+ANNOTATION_EXPORTED+". Will use "+publishedTerm.getId());
            }
            else {
                publishedTerm = entity;
            }
        }
        
        if (publishedTerm==null) {
            throw new Exception("No ontology term owned by "+ownerKey+" was found with name '"+ANNOTATION_EXPORTED+"'");
        }
    	
        this.sage = new SageDAO(logger);
        this.createDate = new Date();
        this.productMultichannelMip = getCvTermByName("product","multichannel_mip");  
        this.productMultichannelTranslation = getCvTermByName("product","multichannel_translation");
        this.productSignalsMip = getCvTermByName("product","signals_mip");
        this.productSignalsTranslation = getCvTermByName("product","signals_translation");
        this.productSignal1Mip = getCvTermByName("product","signal1_mip");
        this.productSignal1Translation = getCvTermByName("product","signal1_translation");
        this.propertyPublished = getCvTermByName("light_imagery","published_to");
        this.source = getCvTermByName("lab","JFRC");
        this.chanSpec = getCvTermByName("light_imagery","channel_spec");
        this.dimensionX = getCvTermByName("light_imagery","dimension_x");
        this.dimensionY = getCvTermByName("light_imagery","dimension_y");
        this.dimensionZ = getCvTermByName("light_imagery","dimension_z");

        List<Entity> samples = getSamplesForExport(ANNOTATION_EXPORT_20X);
        samples.addAll(getSamplesForExport(ANNOTATION_EXPORT_63X));
        exportSamples(samples);
        
        if (!exportedNames.isEmpty()) {
	        StringBuilder sb = new StringBuilder();
	        for(String name : exportedNames) {
	        	if (sb.length()>0) sb.append("\n");
	        	sb.append(name);
	        }
	        logger.info("Exported primary image names:\n"+sb);
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
                    logger.info("  Sample will be synchronized to SAGE: "+os.getName());
                    samples.add(os);    
                }
            }
            else if (ANNOTATION_EXPORT_63X.equals(annotationTerm)) {
                Entity os = getObjectiveSample(sample, Objective.OBJECTIVE_63X);
                if (os==null) {
                    logger.warn("  Entity annotated with '"+annotationTerm+"' does not have a 63x sample: "+sample.getId());
                }
                else {
                    logger.info("  Sample will be synchronized to SAGE: "+os.getName());
                    samples.add(os);  
                }
            }   
        }
        return samples;
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
        
    private void exportSamples(List<Entity> samples) throws Exception {
        for(Entity sample : samples) {
            logger.info("Synchronizing "+sample.getName());
            String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        	if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                export20xSample(sample);
        	}
        	else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                export63xSample(sample);
        	}
        	else {
        		logger.warn("Sample with unsupported objective ("+objective+")");
        	}
            sample.setEntityData(null); // free memory
        }
    }
    
    private void export20xSample(Entity sample) throws Exception {

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
        Entity artifactRun = getLatestArtifactRun(sample);
        if (artifactRun==null) {
            logger.error("  Sample has no MBEW artifacts to export");
            return;
        }
        entityLoader.populateChildren(artifactRun);

        // Export to SAGE
        synchronize20xArtifactsForArea(sample, artifactRun, "Brain", line);
        synchronize20xArtifactsForArea(sample, artifactRun, "VNC", line); 
        
        // Annotate as published
    	annotateIfNecessary(sample);
    }
    
    private void export63xSample(Entity sample) throws Exception {
        
        // Find fly line
        String lineName = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE);
        Line line = lines.get(lineName);
        if (line==null) {
            line = sage.getLineByName(lineName); 
            lines.put(lineName, line);
        }
        
        // Collect artifacts for export
        Entity artifactRun = getLatestArtifactRun(sample);
        if (artifactRun==null) {
            logger.error("  Sample has no MBEW artifacts to export");
            return;
        }
        entityLoader.populateChildren(artifactRun);

        // Export to SAGE
        synchronize63xArtifactsForArea(sample, artifactRun, line);
        
        // Annotate as published
    	annotateIfNecessary(sample);
    }
    
    private Entity getLatestArtifactRun(Entity sample) throws Exception {
        entityLoader.populateChildren(sample);
        List<Entity> children = sample.getOrderedChildren();
        Collections.reverse(children);
        for(Entity child : children) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_PIPELINE_RUN) && child.getName().startsWith(ARTIFACT_PIPELINE_RUN_PREFIX)) {
            	return child;
            	
            }
        }
        return null;
    }
    
    private void synchronize20xArtifactsForArea(Entity sample, Entity artifactRun, String area, Line line) throws Exception {

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
        	// Single LSM source image
        	List<Image> images = sage.getImages(sourceSageImageIds);
        	if (images.isEmpty()) {
        		logger.error("Could not find SAGE image with id: "+sourceSageImageIds.get(0));
        		return;
        	}
            sourceImage = images.get(0);
            sage.setImageProperty(sourceImage, propertyPublished, PUBLISHED_TO, createDate);
        }
        else {
        	// Multiple LSMs were merged or stitched to create the artifact, so we need a new primary image in SAGE
            Entity image3d = EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean))
                .startAt(sample)
                .childOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                .withAttribute(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, area)
                .childrenOfAttr(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)
                .getLast();
            sourceImage = getOrCreatePrimaryImage(image3d, line, sourceSageImageIds);
        }
        
        synchronizeSecondaryImages(artifactRun, sourceImage, area, "20x");
        
        exportedNames.add(sourceImage.getName());
    }
    
    private void synchronize63xArtifactsForArea(Entity sample, Entity artifactRun, Line line) throws Exception {

        // Find source ids for the primary sample image
    	
        List<Entity> tiles = EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean))
                .startAt(sample)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .getAll();

    	List<Integer> allSourceSageImageIds = new ArrayList<Integer>();
    	
    	logger.trace("  Synchronizing "+tiles.size()+" tiles for "+sample.getName());
    	
        // Image tiles
        for(Entity tile : tiles) {

        	entityLoader.populateChildren(tile);
        	List<Integer> sourceSageImageIds = new ArrayList<Integer>();
        	for(Entity lsmStack : tile.getChildren()) {
        		String sageId = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
        		if (sageId!=null) {
        			sourceSageImageIds.add(new Integer(sageId));
        		}
        	}
        	
        	allSourceSageImageIds.addAll(sourceSageImageIds);
        	        	
            Image sourceImage = null;
            if (sourceSageImageIds.size()==1) {
            	// Single LSM source image
            	List<Image> images = sage.getImages(sourceSageImageIds);
            	if (images.isEmpty()) {
            		logger.error("Could not find SAGE image with id: "+sourceSageImageIds.get(0));
            		continue;
            	}
                sourceImage = images.get(0);
                sage.setImageProperty(sourceImage, propertyPublished, PUBLISHED_TO, createDate);
            }
            else {

            	String imageName = null;
            	String chanspec = null;
            	String pixelRes = null;
            	
                for(Entity child : artifactRun.getChildren()) {
                    String name = child.getName();
                    if (child.getEntityTypeName().equals(EntityConstants.TYPE_MOVIE)) {
                        if (name.contains(tile.getName()) && !name.contains("Signal") && !name.contains("_MIP")) {
                        	if (imageName!=null) {
                        		logger.warn("Overriding "+imageName+" with "+name.substring(0, name.lastIndexOf('.')));
                        	}
                        	chanspec = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                        	pixelRes = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                        	imageName = name.substring(0, name.lastIndexOf('.'));
                        }
                    }
                }
                
                if (imageName==null) {
                	logger.error("Artifacts for tile "+tile.getName()+" not found in artifact run "+artifactRun.getId());
                	continue;
                }
                
                if (chanspec==null) {
                	logger.warn("Movie artifact for tile "+tile.getName()+" is missing chanspec");
                }
                
                if (pixelRes==null) {
                	logger.warn("Movie artifact for tile "+tile.getName()+" is missing pixel resolution");
                }
                
            	// Multiple LSMs were merged or stitched to create the artifact, so we need a new primary image in SAGE
            	Entity image3d = new Entity();
            	image3d.setName(imageName);
            	image3d.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, chanspec);
            	image3d.setValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, pixelRes);
            	image3d.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, null);
                sourceImage = getOrCreatePrimaryImage(image3d, line, sourceSageImageIds);
            }

            synchronizeSecondaryImages(artifactRun, sourceImage, tile.getName(), "63x");
            exportedNames.add(sourceImage.getName());
        }

        // Stitched image if there is more than one tile
        if (tiles.size()>1) {
	        Entity image3d = EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean))
	            .startAt(sample)
	            .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
	            .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
	            .childrenOfAttr(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE).getLast();

            Image sourceImage = getOrCreatePrimaryImage(image3d, line, allSourceSageImageIds);
            synchronizeSecondaryImages(artifactRun, sourceImage, "stitched", "63x");
            exportedNames.add(sourceImage.getName());
        }
    }
    
    private void synchronizeSecondaryImages(Entity artifactRun, Image sourceImage, String tileName, String objective) throws Exception {
    	logger.debug("  Synchronizing secondary images for tile: "+tileName);
    	String tileTag = "-"+tileName;
        for(Entity child : artifactRun.getChildren()) {
            String name = child.getName();
            if (!name.contains(tileTag)) continue;
        	String type = child.getEntityTypeName();
            if (EntityConstants.TYPE_IMAGE_2D.equals(type)) {
            	if (name.contains("Signal")) {
             		getOrCreateSecondaryImage(child, "20x".equals(objective)?productSignal1Mip:productSignalsMip, sourceImage);
            	}
            	else {
            		getOrCreateSecondaryImage(child, productMultichannelMip, sourceImage);
            	}
            }
            else if (EntityConstants.TYPE_MOVIE.equals(type)) {
            	if (name.contains("Signal")) {
             		getOrCreateSecondaryImage(child, "20x".equals(objective)?productSignal1Translation:productSignalsTranslation, sourceImage);
            	}
            	else {
            		getOrCreateSecondaryImage(child, productMultichannelTranslation, sourceImage);	
            	}
            } 
            else {
            	logger.trace("Ignoring artifact "+child.getName()+" (id="+child.getId()+")");
            }
        }
    }
    
    private Image getOrCreatePrimaryImage(Entity entity, Line line, List<Integer> sourceSageImageIds) throws Exception {
        
        String imageName = entity.getId()==null?entity.getName():(entity.getId()+"-"+entity.getName());
        Image image = sage.getImageByName(imageName);
        
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
        
        logger.info("  Synchronizing "+entity.getName());
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        String url = getUrl(path);
        
        if (image!=null) {
        	image.setFamily(consensusFamily);
        	image.setLine(line);
        	image.setSource(source);
        	image.setUrl(url);
        	image.setRepresentative(true);
        	image.setDisplay(true);
        	image.setCreatedBy(CREATED_BY);
            image = sage.saveImage(image);
            logger.info("    Updated SAGE primary image "+image.getId()+" with name "+image.getName());
        }
        else {
            image = new Image(consensusFamily, line, source, imageName, url, path, true, true, CREATED_BY, createDate);
            image = sage.saveImage(image);
            logger.info("    Created SAGE primary image "+image.getId()+" with name "+image.getName());
        }
        
        List<CvTerm> keys = new ArrayList<CvTerm>(consensusValues.keySet());
        Collections.sort(keys, new Comparator<CvTerm>() {
			@Override
			public int compare(CvTerm o1, CvTerm o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
        
        for(CvTerm type : keys) {
            type.getId(); // ensure that the type id is loaded
            String value = consensusValues.get(type);
            logger.trace("      "+type.getName()+": "+value);
            if (value == null || value.equals(NO_CONSENSUS)) continue;
            sage.setImageProperty(image, type, value, createDate);
        }
        
        return image;
    }
    
    private SecondaryImage getOrCreateSecondaryImage(Entity entity, CvTerm productType, Image sourceImage) throws Exception {
        String imageName = entity.getId()+"-"+entity.getName();
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        String url = getUrl(path);
        
        SecondaryImage secondaryImage = sage.getSecondaryImageByName(imageName);
        
        if (secondaryImage!=null) {
            secondaryImage.setPath(path);
            secondaryImage.setUrl(url);
            sage.saveSecondaryImage(secondaryImage);
            logger.info("    Updated SAGE secondary image "+secondaryImage.getId()+" with name "+secondaryImage.getName());
        }
        else {
            secondaryImage = new SecondaryImage(sourceImage, productType, imageName, path, getUrl(path), createDate);
            sage.saveSecondaryImage(secondaryImage);
            logger.info("    Created SAGE secondary image "+secondaryImage.getId()+" with name "+secondaryImage.getName());
        }
        
        return secondaryImage;
    }

	/**
	 * Mark sample as having been published 
	 * @param sample
	 * @throws Exception
	 */
    private void annotateIfNecessary(Entity sample) throws Exception {
    	if (publishedTerm==null) {
    		return;
    	}
        for(Entity annotation : annotationBean.getAnnotationsForEntity(ownerKey, sample.getId())) {
            String keyEntityId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
            if (keyEntityId!=null && keyEntityId.equals(publishedTerm.getId().toString())) {
                logger.trace("  Sample was already exported: "+sample.getName());
                return;
            }
        }

        OntologyAnnotation annotation = new OntologyAnnotation(null, sample.getId(), publishedTerm.getId(), publishedTerm.getName(), null, null);
        annotationBean.createOntologyAnnotation(ownerKey,annotation);
    }
    
    private String getUrl(String filepath) {
    	if (filepath==null) return null;
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
