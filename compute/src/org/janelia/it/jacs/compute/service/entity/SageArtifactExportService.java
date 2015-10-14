package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.Interval;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;
import org.janelia.it.jacs.model.ontology.types.Tag;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.Observation;
import org.janelia.it.jacs.model.sage.SageSession;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Synchronizes workstation artifacts into the SAGE database for all annotated samples. 
 * If everything is successful, it then annotates the samples as having been exported.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageArtifactExportService extends AbstractEntityService {

	public static final String CREATED_BY = "Janelia Workstation";
	
    private static final String WEBDAV_PREFIX = "http://jacs-webdav.int.janelia.org:8080/Webdav";
    private static final String NO_CONSENSUS = "No Consensus";
    private static final String PUBLISHED_TO = "Split GAL4";
    private static final String ANNOTATION_EXPORT_20X = "Publish20xToWeb";
    private static final String ANNOTATION_EXPORT_63X = "Publish63xToWeb";
    private static final String ANNOTATION_EXPORTED = "PublishedToWeb";
    private static final String PUBLICATION_OWNER = "group:workstation_users";
    
    private SageDAO sage;
    
    private Entity releaseEntity;
    private Map<String,Line> lines = new HashMap<String,Line>();
    private Set<String> dataSetIds = new HashSet<>();
    private Set<String> annotatorKeys = new HashSet<>();
    
    private Date createDate;
    private Entity publishedTerm;
    private CvTerm productMultichannelMip;
    private CvTerm productMultichannelTranslation;
    private CvTerm productSignalsMip;
    private CvTerm productSignalsTranslation;
    private CvTerm propertyPublishedTo;
    private CvTerm propertyToPublish;
    private CvTerm propertyPublishingUser;
    private CvTerm propertyRelease;
    private CvTerm sessionType;
    private CvTerm observationTerm;
    private CvTerm source;
    private CvTerm chanSpec;
    private CvTerm dimensionX;
    private CvTerm dimensionY;
    private CvTerm dimensionZ;
    private CvTerm lab;
    
    private Map<Long,Entity> currLineAnnotationMap = new HashMap<>();
    private List<String> exportedNames = new ArrayList<String>();
    
    public void execute() throws Exception {

        Long releaseEntityId = data.getRequiredItemAsLong("RELEASE_ENTITY_ID");
        this.releaseEntity = entityBean.getEntityById(releaseEntityId);
        if (releaseEntity == null) {
            throw new IllegalArgumentException("Release entity not found with id="+releaseEntityId);
        }

        logger.info("Exporting release to SAGE: "+releaseEntity.getName());
        
        String dataSetsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SETS);
        if (dataSetsStr != null) {
            for (String identifier : dataSetsStr.split(",")) {
                dataSetIds.add(identifier);
            }
        }
        logger.info("Data sets: "+dataSetIds);

        String annotatorsStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATORS);
        if (annotatorsStr != null) {
            for (String key : annotatorsStr.split(",")) {
                annotatorKeys.add(key);
            }
        }
        annotatorKeys.add(releaseEntity.getOwnerKey());
        logger.info("Annotators: "+annotatorKeys);
        
        for(Entity entity : entityBean.getEntitiesByNameAndTypeName(PUBLICATION_OWNER, ANNOTATION_EXPORTED, EntityConstants.TYPE_ONTOLOGY_ELEMENT)) {
            if (publishedTerm!=null) {
                logger.warn("Found multiple terms with name "+ANNOTATION_EXPORTED+". Will use "+publishedTerm.getId());
            }
            else {
                publishedTerm = entity;
            }
        }
        
        if (publishedTerm==null) {
            throw new Exception("No ontology term owned by "+PUBLICATION_OWNER+" was found with name '"+ANNOTATION_EXPORTED+"'");
        }
    	
        this.sage = new SageDAO(logger);
        this.createDate = new Date();
        this.productMultichannelMip = getCvTermByName("product","multichannel_mip");  
        this.productMultichannelTranslation = getCvTermByName("product","multichannel_translation");
        this.productSignalsMip = getCvTermByName("product","signals_mip");
        this.productSignalsTranslation = getCvTermByName("product","signals_translation");
        this.propertyPublishedTo = getCvTermByName("light_imagery","published_to");
        this.propertyToPublish = getCvTermByName("light_imagery","to_publish");
        this.propertyPublishingUser = getCvTermByName("light_imagery","publishing_user");
        this.propertyRelease = getCvTermByName("light_imagery","release");
        this.sessionType = getCvTermByName("flylight_public_annotation","splitgal4_public_annotation");
        this.observationTerm = getCvTermByName("flylight_public_annotation","intensity");
        this.source = getCvTermByName("lab","JFRC");
        this.chanSpec = getCvTermByName("light_imagery","channel_spec");
        this.dimensionX = getCvTermByName("light_imagery","dimension_x");
        this.dimensionY = getCvTermByName("light_imagery","dimension_y");
        this.dimensionZ = getCvTermByName("light_imagery","dimension_z");
        this.lab = getCvTermByName("lab", "rubin");
        
        Entity releasesFolder = null;
        for(Entity entity : entityBean.getEntitiesByNameAndTypeName(releaseEntity.getOwnerKey(), EntityConstants.NAME_FLY_LINE_RELEASES, EntityConstants.TYPE_FOLDER)) {
            if (entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)!=null) {
                releasesFolder = entity;
            }
        }

        if (releasesFolder==null) {
            throw new Exception("No releases folder owned by "+releaseEntity.getOwnerKey()+" was found with name '"+ANNOTATION_EXPORTED+"'");
        }
        
        entityLoader.populateChildren(releasesFolder);
        Entity releaseFolder = EntityUtils.findChildWithName(releasesFolder, releaseEntity.getName());
        entityLoader.populateChildren(releaseFolder);
        
        for(Entity flyLineFolder : EntityUtils.getChildrenOfType(releaseFolder, EntityConstants.TYPE_FOLDER)) {
            
            currLineAnnotationMap.clear();
            for(Entity annotation : annotationBean.getAnnotationsForEntity(null, flyLineFolder.getId())) {
                currLineAnnotationMap.put(annotation.getId(), annotation);    
            }
            
            logger.info("Processing line "+flyLineFolder.getName());
            
            entityLoader.populateChildren(flyLineFolder);
            int reps = 0;
            for(Entity sample : EntityUtils.getChildrenOfType(flyLineFolder, EntityConstants.TYPE_SAMPLE)) {
                if (processSamples(sample)>0) {
                    reps++;
                }
            }
            if (reps<1) {
                logger.warn("Line is not represented: "+flyLineFolder.getName());
            }
            else {
                exportLineAnnotations(flyLineFolder.getName());
            }
        }
       
        if (!exportedNames.isEmpty()) {
	        StringBuilder sb = new StringBuilder();
	        for(String name : exportedNames) {
	        	if (sb.length()>0) sb.append("\n");
	        	sb.append(name);
	        }
	        logger.info("Exported primary image names:\n"+sb);
        }
    }

    private int processSamples(Entity sample) throws Exception {
        String dataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        if (!dataSetIds.contains(dataSetIdentifier)) {
            logger.warn("  Ignoring sample from data set that is not in this release: "+sample.getName());
            return 0;
        }
        int c = 0;
        String sampleObjective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        if (sampleObjective!=null) {
            c += processSample(sample, sample);
        }
        else {
            Entity os20x = getObjectiveSample(sample, Objective.OBJECTIVE_20X);
            Entity os63x = getObjectiveSample(sample, Objective.OBJECTIVE_63X);
            c += processSample(sample, os20x);
            c += processSample(sample, os63x);
            // free memory
            sample.setEntityData(null);
        }
        return c;
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
    
    private int processSample(Entity parentSample, Entity sample) throws Exception {

        logger.info("  Processing sample "+sample.getName());
        Set<String> publishingUsers = new HashSet<>();
        
        String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        boolean export = false;
        
        List<Entity> annotations = new ArrayList<>();
        annotations.addAll(annotationBean.getAnnotationsForEntity(null, sample.getId()));
        if (sample!=parentSample) {
            annotations.addAll(annotationBean.getAnnotationsForEntity(null, parentSample.getId()));
        }

        for(Entity annotation : annotations) {
            if (!annotatorKeys.contains(annotation.getOwnerKey())) {
                continue;
            }
            
            if (annotation.getName().equals(ANNOTATION_EXPORT_20X)) {
                if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                    publishingUsers.add(annotation.getOwnerKey());
                    export = true;
                }
            }
            else if (annotation.getName().equals(ANNOTATION_EXPORT_63X)) {
                if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                    publishingUsers.add(annotation.getOwnerKey());
                    export = true;
                }
            }
            else {
                currLineAnnotationMap.put(annotation.getId(), annotation);
            }
        }
        
        if (export) {
            String publishingUser = publishingUsers.iterator().next();
            if (publishingUsers.size()>1) {
                logger.warn("    More than one user marked "+sample.getName()+" for publication. Using: "+publishingUser);
            }
            exportSample(sample, publishingUser);
        }

        // free memory
        sample.setEntityData(null);
        
        return export ? 1 : 0;
    }
    
    private void exportSample(Entity sample, String publishingUser) throws Exception {

        logger.info("    Exporting "+sample.getName());
        
        // Find fly line
        String lineName = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE);
        Line line = getLineByName(lineName);
                
        // Collect artifacts for export
        Entity postResult = getLatestPostProcessingResult(sample);
        if (postResult==null) {
            logger.error("    Sample has no post-processed artifacts to export");
            return;
        }
        entityLoader.populateChildren(postResult);
        Entity postSupportingData = EntityUtils.getSupportingData(postResult);
        entityLoader.populateChildren(postSupportingData);
                
        // Export to SAGE
        synchronizeArtifacts(sample, postSupportingData, line, publishingUser);
        
        // Annotate as published
    	annotateIfNecessary(sample);
    }
    
    private Entity getLatestPostProcessingResult(Entity sample) throws Exception {
        entityLoader.populateChildren(sample);
        List<Entity> runs = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
        Collections.reverse(runs);
        for(Entity run : runs) {
            entityLoader.populateChildren(run);
            if (!EntityUtils.getChildrenOfType(run, EntityConstants.TYPE_ERROR).isEmpty()) {
                // Skip error runs
                continue;
            }
            List<Entity> postResults = EntityUtils.getChildrenOfType(run, EntityConstants.TYPE_POST_PROCESSING_RESULT);
            Collections.reverse(postResults);
            for(Entity postResult : postResults) {   
            	return postResult;
            }
        }
        return null;
    }
    
    private void synchronizeArtifacts(Entity sample, Entity artifactFiles, Line line, String publishingUser) throws Exception {

        // Convert entity model to domain model
        logger.trace("  Converting sample "+sample.getName());
        
        List<Entity> tiles = EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sample)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE).getAll();

        // Tiles 
        Map<String,ImageArea> imageAreaMap = new HashMap<>();
        for(Entity tile : tiles) {
            logger.trace("    Converting tile "+tile.getName());
            
            String area = tile.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
            ImageArea imageArea = imageAreaMap.get(area);
            if (imageArea==null) {
                imageArea = new ImageArea();
                imageArea.areaName = area;
                imageAreaMap.put(area, imageArea);   
            }
            
            ImageTile imageTile = new ImageTile();
            imageTile.tileName = tile.getName();
            
            entityLoader.populateChildren(tile);

            for(Entity lsmStack : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {
                logger.trace("      Converting LSM "+lsmStack.getName());
                String imageName = lsmStack.getName().substring(0, lsmStack.getName().lastIndexOf('.'));
                ImageStack imageStack = new ImageStack();
                imageStack.name = lsmStack.getId()+"-"+imageName;
                imageStack.tag = imageName;
                imageStack.filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                imageStack.sageId = new Integer(lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID));
                imageStack.chanSpec = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                imageStack.pixelRes = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                imageTile.images.add(imageStack);
            }
            
            if (imageTile.images.size()>1) {
                ImageStack imageStack = new ImageStack();
                ImageStack firstStack = imageTile.images.get(0);
                imageStack.name = tile.getId()+"-"+tile.getName();
                imageStack.tag = imageTile.tileName;
                imageStack.filepath = null;
                imageStack.sageId = null;
                imageStack.chanSpec = firstStack.chanSpec;
                imageStack.pixelRes = firstStack.pixelRes;
                imageTile.mergedImage = imageStack;
            }
            
            imageArea.tiles.add(imageTile); 
        }

        List<Entity> processingResults = EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).getAll();
        
        // Stitched images
        for (ImageArea imageArea : imageAreaMap.values()) {
            if (imageArea.tiles.size()>1) {

                ImageStack imageStack = null;
                
                for(Entity result : processingResults) {
                    String area = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                    if (imageArea.areaName.equals(area)) {
                        if (imageStack!=null) {
                            logger.warn("Multiple results for area: "+area);
                            continue;
                        }
                        imageStack = new ImageStack();
                        entityLoader.populateChildren(result);
                        Entity default3dImage = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                        imageStack.name = default3dImage.getId()+"-stitched-"+area;
                        imageStack.tag = area;
                        imageStack.filepath = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                        imageStack.sageId = null;
                        imageStack.chanSpec = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                        imageStack.pixelRes = default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                    }
                }
                
                if (imageStack!=null) {
                    imageArea.stitchedImage = imageStack;
                }
            }
        }

        logger.debug("  Synchronizing sample "+sample.getName());
        String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);

        for (ImageArea imageArea : imageAreaMap.values()) {
            
            logger.debug("    Synchronizing area '"+imageArea.areaName+"'");
            List<Integer> areaSageImageIds = new ArrayList<>();
            
            for(ImageTile imageTile : imageArea.tiles) {
            
                logger.debug("      Synchronizing tile '"+imageTile.tileName+"'");
                Image tileSourceImage = null;
                                
                List<Integer> tileSageImageIds = new ArrayList<>();
                for(ImageStack imageStack : imageTile.images) {
                    tileSageImageIds.add(imageStack.sageId);
                }
                areaSageImageIds.addAll(tileSageImageIds);

                if (imageTile.mergedImage!=null) {
                    // Merged LSM, create new primary image for the merged tile
                    tileSourceImage = getOrCreatePrimaryImage(imageTile.mergedImage, line, areaSageImageIds, publishingUser);
                }
                else {
                    // Single LSM, make that the primary image
                    List<Image> images = sage.getImages(areaSageImageIds);
                    if (images.isEmpty()) {
                        logger.error("Could not find SAGE image with id: "+areaSageImageIds.get(0));
                        continue;
                    }
                    tileSourceImage = images.get(0);
                    sage.setImageProperty(tileSourceImage, propertyPublishedTo, PUBLISHED_TO, createDate);
                    sage.setImageProperty(tileSourceImage, propertyToPublish, "Y", createDate);
                    sage.setImageProperty(tileSourceImage, propertyPublishingUser, publishingUser, createDate);
                    sage.setImageProperty(tileSourceImage, propertyRelease, releaseEntity.getName(), createDate);
                }
                
                synchronizeSecondaryImages(artifactFiles, tileSourceImage, imageTile.tileName, objective);
                exportedNames.add(tileSourceImage.getName());
            }
            
            if (imageArea.stitchedImage!=null) {
                logger.info("      Synchronizing stitched image for area '"+imageArea.areaName+"'");
                // Merged LSM, create new primary image for the merged tile
                Image areaSourceImage = getOrCreatePrimaryImage(imageArea.stitchedImage, line, areaSageImageIds, publishingUser);
                
                synchronizeSecondaryImages(artifactFiles, areaSourceImage, imageArea.stitchedImage.tag, objective);
                exportedNames.add(areaSourceImage.getName());
            }
        }
    }
    
    private Image getOrCreatePrimaryImage(ImageStack imageStack, Line line, List<Integer> sourceSageImageIds, String publishingUser) throws Exception {

        String imageName = imageStack.name;
        logger.info("  Synchronizing primary image "+imageName);
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
        
        consensusValues.put(propertyPublishedTo, PUBLISHED_TO);
        consensusValues.put(propertyToPublish, "Y");
        consensusValues.put(propertyPublishingUser, publishingUser);
        consensusValues.put(propertyRelease, releaseEntity.getName());
        consensusValues.put(chanSpec, imageStack.chanSpec);
        consensusValues.put(dimensionX, null);
        consensusValues.put(dimensionY, null);
        consensusValues.put(dimensionZ, null);
        
        String pixelRes = imageStack.pixelRes;
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
        
        String path = imageStack.filepath;
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

    private void synchronizeSecondaryImages(Entity artifactRun, Image sourceImage, String tag, String objective) throws Exception {
        logger.info("  Synchronizing secondary images for image: "+sourceImage.getName());
        String tileTag = "-"+tag;
        for(Entity child : artifactRun.getChildren()) {
            String name = child.getName();
            if (!name.contains(tileTag)) continue;
            String type = child.getEntityTypeName();
            if (EntityConstants.TYPE_IMAGE_2D.equals(type)) {
                if (name.contains("_signal")) {
                    getOrCreateSecondaryImage(child, productSignalsMip, sourceImage);
                }
                else if (name.contains("_all")) {
                    getOrCreateSecondaryImage(child, productMultichannelMip, sourceImage);
                }
            }
            else if (EntityConstants.TYPE_MOVIE.equals(type)) {
                if (name.contains("_signal")) {
                    getOrCreateSecondaryImage(child, productSignalsTranslation, sourceImage);
                }
                else if (name.contains("_all")) {
                    getOrCreateSecondaryImage(child, productMultichannelTranslation, sourceImage);  
                }
            } 
            else {
                logger.trace("Ignoring artifact "+child.getName()+" (id="+child.getId()+")");
            }
        }
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

    private void exportLineAnnotations(String lineName) throws Exception {
        
        Line line = getLineByName(lineName);
        SageSession session = sage.getSageSession(lineName, sessionType);
        if (session==null) {
            logger.info("  Creating new session for line "+lineName);
            session = new SageSession(sessionType, lab, line, lineName, null, createDate);
        }
        else {
            logger.info("  Updating existing session for line "+lineName+" (id="+session.getId()+")");
            session.setLine(line);
            session.setLab(lab);
        }
        
        Set<String> annotators = new HashSet<>();
        Map<String,Observation> observationMap = new HashMap<>();
        for(Observation observation : session.getObservations()) {
            observationMap.put(observation.getType().getName(), observation);
        }

        Multimap<String,Entity> annotationDeduper = HashMultimap.<String,Entity>create();
        for(Entity annotationEntity : currLineAnnotationMap.values()) {
            if (!annotatorKeys.contains(annotationEntity.getOwnerKey())) {
                continue;
            }
            String keyEntityId = annotationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
            annotationDeduper.put(keyEntityId, annotationEntity);
        }
        
        for (String keyEntityId : annotationDeduper.keySet()) {

            // Get latest annotation for this key 
            List<Entity> annotations = new ArrayList<>(annotationDeduper.get(keyEntityId));
            Collections.sort(annotations, new Comparator<Entity>() {
                @Override
                public int compare(Entity o1, Entity o2) {
                    return o2.getCreationDate().compareTo(o1.getCreationDate());
                }
            });
            Entity annotationEntity = annotations.get(0);
            
            logger.info("  Processing line annotation: "+annotationEntity.getName());
            
            annotators.add(annotationEntity.getOwnerKey());
            
            Entity keyEntity = entityBean.getEntityById(keyEntityId);
            
            String typeName = keyEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE);
            OntologyElementType type = OntologyElementType.createTypeByName(typeName);
            type.init(keyEntity);
            
            String value = null;
            if (type instanceof Interval) {
                value = annotationEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM);
            }
            else if (type instanceof Tag) {
                value = "1";
            }
            else {
                logger.warn("    Unsupported annotation type: "+typeName);
                continue;
            }
            
            String annotationName = keyEntity.getName();
            CvTerm observationType = getCvTermByName("flylight_public_annotation",annotationName);
            if (observationType==null) {
                logger.warn("    Cannot find corresponding SAGE term for ontology term '"+annotationName+"'");
                continue;
            }

            String obsName = observationType.getName();
            Observation observation = observationMap.get(obsName);
            if (observation==null) {
                observation = new Observation(observationType, session, observationTerm, null, value, createDate);
            }
            else {
                if (observation.getId()==null) {
                    logger.warn("    Multiple annotations for the same term: "+obsName+". Only one will be exported.");
                }
                // Update observation value
                observation.setValue(value);
            }
            
            observationMap.put(obsName, observation);

            for (int i=1; i<annotations.size(); i++) {
                Entity dupAnnotation = annotations.get(i);
                logger.warn("  Ignoring duplicate line annotation: "+dupAnnotation.getName());
            }
        }

        // Everything not in the map is no longer needed and can be deleted
        for (Iterator<Observation> it = session.getObservations().iterator(); it.hasNext(); ) {
            Observation entry = it.next();
            Observation obs = observationMap.get(entry.getType().getName());
            if (obs==null || !obs.getId().equals(entry.getId())) {
                it.remove();    
            }
        }
    
        session.getObservations().addAll(observationMap.values());
        
        logger.info("  Observations: ");
        for(Observation observation : session.getObservations()) {
            logger.info("    "+observation.getType().getName()+"="+observation.getValue()+" (id="+observation.getId()+")");
        }
        
        session.setAnnotator(Task.csvStringFromCollection(annotators));
        logger.info("Saved session '"+session.getName()+"' with "+session.getObservations().size()+" observation");
        sage.saveSageSession(session);
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
        for(Entity annotation : annotationBean.getAnnotationsForEntity(PUBLICATION_OWNER, sample.getId())) {
            String keyEntityId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID);
            if (keyEntityId!=null && keyEntityId.equals(publishedTerm.getId().toString())) {
                logger.trace("  Sample was already exported: "+sample.getName());
                return;
            }
        }

        OntologyAnnotation annotation = new OntologyAnnotation(null, sample.getId(), publishedTerm.getId(), publishedTerm.getName(), null, null);
        annotationBean.createOntologyAnnotation(PUBLICATION_OWNER, annotation);
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
        
    private Line getLineByName(String lineName) throws DaoException {
        Line line = lines.get(lineName);
        if (line==null) {
            line = sage.getLineByName(lineName); 
            lines.put(lineName, line);
        }
        return line;
    }
    
    private class ImageArea {
        String areaName;
        List<ImageTile> tiles = new ArrayList<>();
        ImageStack stitchedImage;
    }
    
    private class ImageTile {
        String tileName;
        List<ImageStack> images = new ArrayList<>();
        ImageStack mergedImage;
    }
    
    private class ImageStack {
        String name;
        String tag;
        String filepath;
        String chanSpec;
        String pixelRes;
        Integer sageId;
    }
}
