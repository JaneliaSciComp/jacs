package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.util.JFSUtils;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;
import org.janelia.it.jacs.model.domain.ontology.Tag;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Experiment;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.Observation;
import org.janelia.it.jacs.model.sage.SageSession;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.model.tasks.Task;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Synchronizes workstation artifacts into the SAGE database for all annotated samples. 
 * If everything is successful, it then annotates the samples as having been exported.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageArtifactExportService extends AbstractDomainService {

	public static final String CREATED_BY = "Janelia Workstation";
    public static final String ANNOTATION_EXPORT_20X = "Publish20xToWeb";
    public static final String ANNOTATION_EXPORT_63X = "Publish63xToWeb";
    public static final String ANNOTATION_EXPORTED_20X = "Published20xToWeb";
    public static final String ANNOTATION_EXPORTED_63X = "Published63xToWeb";
    public static final String NO_CONSENSUS = "No Consensus";
    public static final String PUBLISHED_TO = "Split GAL4";
    public static final String PUBLICATION_OWNER = "group:workstation_users";
    public static final String PUBLICATION_ONTOLOGY_NAME = "Publication";
    public static final String LINE_ANNOTATION_CV_NAME = "alps_splitgal4_public_annotation";
    private static final String SCALITY_JFS_PREFIX = "/scality";
    
    private SageDAO sage;
    private SampleHelperNG sampleHelper;
    
    private LineRelease release;
    private Map<String,Line> lines = new HashMap<String,Line>();
    private Set<String> dataSetIds = new HashSet<>();
    private Set<String> annotatorKeys = new HashSet<>();
    
    private Ontology publicationOntology;
    private OntologyTerm publishedTerm20x;
    private OntologyTerm publishedTerm63x;
    private CvTerm productMultichannelMip;
    private CvTerm productMultichannelTranslation;
    private CvTerm productSignalsMip;
    private CvTerm productSignalsTranslation;
    private CvTerm propertyPublishedTo;
    private CvTerm propertyToPublish;
    private CvTerm propertyPublishingUser;
    private CvTerm propertyRelease;
    private CvTerm propertyWorkstationSampleId;
    private CvTerm propertyChanSpec;
    private CvTerm propertyDimensionX;
    private CvTerm propertyDimensionY;
    private CvTerm propertyDimensionZ;
    private CvTerm sessionExperimentType;
    private CvTerm observationTerm;
    private CvTerm lab;

    private Experiment releaseExperiment;
    private Map<Long,Annotation> currLineAnnotationMap = new HashMap<>();
    private List<String> exportedNames = new ArrayList<String>();
    
    private Date createDate;
    
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger);
        
        Long releaseId = data.getRequiredItemAsLong("RELEASE_ENTITY_ID");
        this.release = domainDao.getDomainObject(ownerKey, LineRelease.class, releaseId);
        if (release == null) {
            throw new IllegalArgumentException("Release not found with id="+releaseId);
        }
       
        if (!release.isSageSync()) {
        	logger.info("Skipping release with disabled SAGE sync: "+release.getName());
        	return;
        }
        
        logger.info("Exporting release to SAGE: "+release.getName());
        
        dataSetIds.addAll(release.getDataSets());
        logger.info("Data sets: "+dataSetIds);

        annotatorKeys.addAll(release.getAnnotators());
        annotatorKeys.add(release.getOwnerKey());
        logger.info("Annotators: "+annotatorKeys);
        
        this.publicationOntology = getPublicationOntology();
        this.publishedTerm20x = getPublishedTerm(publicationOntology, ANNOTATION_EXPORTED_20X);
        this.publishedTerm63x = getPublishedTerm(publicationOntology, ANNOTATION_EXPORTED_63X);
        this.sage = new SageDAO(logger);
        this.createDate = new Date();
        this.productMultichannelMip = getCvTermByName("product","multichannel_mip");  
        this.productMultichannelTranslation = getCvTermByName("product","multichannel_translation");
        this.productSignalsMip = getCvTermByName("product","signals_mip");
        this.productSignalsTranslation = getCvTermByName("product","signals_translation");
        this.propertyPublishedTo = getCvTermByName("light_imagery","published_to");
        this.propertyToPublish = getCvTermByName("light_imagery","to_publish");
        this.propertyPublishingUser = getCvTermByName("light_imagery","publishing_user");
        this.propertyRelease = getCvTermByName("light_imagery","alps_release");
        this.propertyWorkstationSampleId = getCvTermByName("light_imagery","workstation_sample_id"); 
        this.propertyChanSpec = getCvTermByName("light_imagery","channel_spec");
        this.propertyDimensionX = getCvTermByName("light_imagery","dimension_x");
        this.propertyDimensionY = getCvTermByName("light_imagery","dimension_y");
        this.propertyDimensionZ = getCvTermByName("light_imagery","dimension_z");
        this.sessionExperimentType = getCvTermByName("flylight_public_annotation","splitgal4_public_annotation");
        this.observationTerm = getCvTermByName("flylight_public_annotation","intensity");
        this.lab = getCvTermByName("lab","JFRC");

        // Get or create experiment that represents this release in SAGE
        String releaseOwner = DomainUtils.getNameFromSubjectKey(release.getOwnerKey());
        
        String experimentName = release.getName()+" ("+releaseOwner+")";
        
        releaseExperiment = sage.getExperiment(experimentName, sessionExperimentType, releaseOwner);
        if (releaseExperiment==null) {
            releaseExperiment = new Experiment(sessionExperimentType, lab, experimentName, releaseOwner, createDate);
            releaseExperiment = sage.saveExperiment(releaseExperiment);
            logger.info("Created new experiment ("+releaseExperiment.getId()+") for release "+experimentName);
        }
        else {
            logger.info("Using existing experiment ("+releaseExperiment.getId()+") for release "+experimentName);
        }
        
        // Walk through release folder and export samples and line annotations
        TreeNode releaseFolder = findReleaseFolder();

        for(ObjectSet flyLineFolder : domainDao.getDomainObjectsAs(releaseFolder.getChildren(), ObjectSet.class)) {
        
            currLineAnnotationMap.clear();
            for(Annotation annotation : domainDao.getAnnotations(null, Reference.createFor(flyLineFolder))) {
                currLineAnnotationMap.put(annotation.getId(), annotation);    
            }
            
            logger.info("Processing line "+flyLineFolder.getName());
            
            int reps = 0;
            for (Sample sample : domainDao.getDomainObjects(ownerKey, Sample.class, flyLineFolder.getMembers())) {
                String line = sample.getLine();
                if (!line.equals(flyLineFolder.getName())) {
                    logger.warn("Ignoring sample "+sample.getName()+" that is categorized in folder "+flyLineFolder.getName()+" but should be "+line+".");
                    continue;
                }
                if (processSamples(sample)>0) {
                    reps++;
                }
            }
            if (reps>0) {
                exportLineAnnotations(flyLineFolder.getName());
            }
        }
       
        // Log the exported names
        if (logger.isDebugEnabled()) {
	        if (!exportedNames.isEmpty()) {
		        StringBuilder sb = new StringBuilder();
		        for(String name : exportedNames) {
		        	if (sb.length()>0) sb.append("\n");
		        	sb.append(name);
		        }
		        logger.debug("Exported primary image names:\n"+sb);
	        }
        }
    }

    private Ontology getPublicationOntology() {

        Ontology publicationOntology = null;
        for(Ontology ontology : domainDao.getDomainObjectsByName(ownerKey, Ontology.class, PUBLICATION_ONTOLOGY_NAME)) {
            if (publicationOntology==null) {
                publicationOntology = ontology;
            }
            else {
                logger.warn("More than one ontology found! Make sure that "+PUBLICATION_OWNER+" only has a single Ontology named "+PUBLICATION_ONTOLOGY_NAME);
            }
        }
        
        if (publicationOntology!=null) { 
            return publicationOntology;
        }
        else {
            throw new IllegalStateException("No publication ontology found. Make sure that "+PUBLICATION_OWNER+" has an Ontology named "+PUBLICATION_ONTOLOGY_NAME);
        }
    }
    
    private OntologyTerm getPublishedTerm(Ontology publicationOntology, String termName) {
        OntologyTerm publishedTerm = DomainUtils.findTerm(publicationOntology, termName);
        
        if (publishedTerm==null) {
            throw new IllegalStateException("No ontology term owned by "+PUBLICATION_OWNER+" was found with name '"+termName+"'");
        }
        
        return publishedTerm;
    }

    private TreeNode findReleaseFolder() throws Exception {
        TreeNode releasesFolder = sampleHelper.createOrVerifyRootEntity(EntityConstants.NAME_FLY_LINE_RELEASES, true, false);
        if (releasesFolder==null) {
            throw new Exception("No releases folder owned by "+release.getOwnerKey()+" was found with name '"+DomainConstants.NAME_FLY_LINE_RELEASES+"'");
        }
        
        for(TreeNode childFolder : domainDao.getDomainObjectsAs(releasesFolder.getChildren(), TreeNode.class)) {
            if (childFolder.getName().equals(release.getName())) {
                return childFolder;
            }
        }
        
        return null;
    }

    private int processSamples(Sample sample) throws Exception {
        String dataSetIdentifier = sample.getDataSet();
        if (!dataSetIds.contains(dataSetIdentifier)) {
            logger.warn("  Ignoring sample from data set that is not in this release: "+sample.getName());
            return 0;
        }
        
        List<Annotation> annotations = domainDao.getAnnotations(null, Reference.createFor(sample));
        logger.trace("  Processing sample "+sample.getName());
        int c = 0;
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            c += processSample(dataSetIdentifier, objectiveSample, annotations);    
        }
        return c;
    }
    
    private int processSample(String dataSetIdentifier, ObjectiveSample objectiveSample, List<Annotation> sampleAnnotations) throws Exception {

        Set<String> publishingSubjectKeys = new HashSet<>();
        
        String objective = objectiveSample.getObjective();
        boolean export = false;
        boolean exported = false;
        
        Map<Long,Annotation> annotationMap = new HashMap<>();
        for(Annotation annotation : sampleAnnotations) {
            
            if (annotation.getName().equals(ANNOTATION_EXPORTED_20X) || annotation.getName().equals(ANNOTATION_EXPORTED_63X)) {
                exported = true;
            }
            
            if (!annotatorKeys.contains(annotation.getOwnerKey())) {
                continue;
            }
            
            if (annotation.getName().equals(ANNOTATION_EXPORT_20X)) {
                if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                    publishingSubjectKeys.add(annotation.getOwnerKey());
                    export = true;
                }
            }
            else if (annotation.getName().equals(ANNOTATION_EXPORT_63X)) {
                if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                    publishingSubjectKeys.add(annotation.getOwnerKey());
                    export = true;
                }
            }
            else {
                annotationMap.put(annotation.getId(), annotation);
            }
        }
        
        if (export) {
            currLineAnnotationMap.putAll(annotationMap);
            String publishingUser = DomainUtils.getNameFromSubjectKey(publishingSubjectKeys.iterator().next());
            if (publishingSubjectKeys.size()>1) {
                logger.warn("    More than one user marked "+objectiveSample.getName()+" for publication. Using: "+publishingUser);
            }
            if (syncSample(objectiveSample, dataSetIdentifier, publishingUser)) {
	            // Annotate as published
	            publishSample(objectiveSample);
            }
            else {
            	unpublishSample(objectiveSample);
            }
        }
        else {
            if (exported) {
                // This sample was exported in the past, but is marked as not export. We need to mark everything in SAGE for un-publishing.
                unpublishSample(objectiveSample);
            }
        }

        return export ? 1 : 0;
    }
    
    private boolean syncSample(ObjectiveSample objectiveSample, String dataSetIdentifier, String publishingUser) throws Exception {

        logger.info("  Exporting "+dataSetIdentifier+" - "+objectiveSample.getName());
        
        // Find fly line
        String lineName = objectiveSample.getParent().getLine();
        Line line = getLineByName(lineName);
                
        // Collect artifacts for export
        SamplePostProcessingResult postResult = objectiveSample.getLatestSuccessfulRun().getLatestResultOfType(SamplePostProcessingResult.class);
        if (postResult==null) {
            logger.error("    Sample has no post-processed artifacts to export");
            return false;
        }
                
        // Convert entity model to domain model
        logger.trace("    Converting sample "+objectiveSample.getName());

        List<SampleTile> tiles = objectiveSample.getTiles();

        // Tiles 
        Map<String,ImageArea> imageAreaMap = new HashMap<>();
        for(SampleTile tile : tiles) {
            logger.trace("    Converting tile "+tile.getName());
            
            String area = tile.getAnatomicalArea();
            ImageArea imageArea = imageAreaMap.get(area);
            if (imageArea==null) {
                imageArea = new ImageArea();
                imageArea.areaName = area;
                imageAreaMap.put(area, imageArea);   
            }
            
            ImageTile imageTile = new ImageTile();
            imageTile.tileName = tile.getName();
            
            for(LSMImage lsmStack : domainDao.getDomainObjectsAs(tile.getLsmReferences(), LSMImage.class)) {
                logger.trace("      Converting LSM "+lsmStack.getName());
                String imageName = lsmStack.getName().substring(0, lsmStack.getName().lastIndexOf('.'));
                ImageStack imageStack = new ImageStack();
                imageStack.name = lsmStack.getId()+"-"+imageName;
                imageStack.tag = imageName;
                imageStack.filepath = lsmStack.getFilepath();
                imageStack.sageId = lsmStack.getSageId();
                imageStack.chanSpec = lsmStack.getChanSpec();
                imageStack.pixelRes = lsmStack.getImageSize();
                imageTile.images.add(imageStack);
            }
            
            if (imageTile.images.size()>1) {
                ImageStack imageStack = new ImageStack();
                ImageStack firstStack = imageTile.images.get(0);
                imageStack.name = objectiveSample.getParent().getId()+"-"+objectiveSample.getObjective()+"-"+tile.getName();
                imageStack.tag = imageTile.tileName;
                imageStack.filepath = null;
                imageStack.sageId = null;
                imageStack.chanSpec = firstStack.chanSpec;
                imageStack.pixelRes = firstStack.pixelRes;
                imageTile.mergedImage = imageStack;
            }
            
            imageArea.tiles.add(imageTile); 
        }

        List<SampleProcessingResult> processingResults = objectiveSample.getLatestSuccessfulRun().getSampleProcessingResults();
        
        // Stitched images
        for (ImageArea imageArea : imageAreaMap.values()) {
            if (imageArea.tiles.size()>1) {

                ImageStack imageStack = null;
                
                for(SampleProcessingResult result : processingResults) {
                    String area = result.getAnatomicalArea();
                    if (imageArea.areaName.equals(area)) {
                        if (imageStack!=null) {
                            logger.warn("Multiple results for area: "+area);
                            continue;
                        }
                        imageStack = new ImageStack();
                        imageStack.name = result.getId()+"-stitched-"+area;
                        imageStack.tag = area;
                        imageStack.filepath = DomainUtils.getDefault3dImageFilePath(result);
                        imageStack.sageId = null;
                        imageStack.chanSpec = result.getChannelSpec();
                        imageStack.pixelRes = result.getImageSize();
                    }
                }
                
                if (imageStack!=null) {
                    imageArea.stitchedImage = imageStack;
                }
            }
        }

        String objective = objectiveSample.getObjective();

        for (ImageArea imageArea : imageAreaMap.values()) {
            
            logger.debug("    Synchronizing area '"+imageArea.areaName+"'");
            List<Image> areaSageImages = new ArrayList<>();
            
            for(ImageTile imageTile : imageArea.tiles) {
            
                logger.debug("      Synchronizing tile '"+imageTile.tileName+"'");
                Image tileSourceImage = null;
                                
                List<Integer> tileSageImageIds = new ArrayList<>();
                for(ImageStack imageStack : imageTile.images) {
                    tileSageImageIds.add(imageStack.sageId);
                }
                List<Image> images = sage.getImages(tileSageImageIds);
                areaSageImages.addAll(images);

                if (images.size()!=tileSageImageIds.size()) {
                	logger.warn("Could not find all SAGE images in list: "+tileSageImageIds);
                	return false;
                }
                
                if (imageTile.mergedImage!=null) {
                    // Merged LSM, create new primary image for the merged tile
                    tileSourceImage = getOrCreatePrimaryImage(objectiveSample, imageTile.mergedImage, line, images, publishingUser);
                }
                else {
                    // Single LSM, make that the primary image
                    tileSourceImage = images.get(0);
                    sage.setImageProperty(tileSourceImage, propertyPublishedTo, PUBLISHED_TO, createDate);
                    sage.setImageProperty(tileSourceImage, propertyToPublish, "Y", createDate);
                    sage.setImageProperty(tileSourceImage, propertyPublishingUser, publishingUser, createDate);
                    sage.setImageProperty(tileSourceImage, propertyRelease, release.getName(), createDate);
                    sage.setImageProperty(tileSourceImage, propertyWorkstationSampleId, objectiveSample.getParent().getId().toString(), createDate);
                    logger.info("    Using existing primary image: "+tileSourceImage.getId());
                }
                
                synchronizeSecondaryImages(postResult, tileSourceImage, imageTile.tileName, objective);
                exportedNames.add(tileSourceImage.getName());
            }
            
            if (imageArea.stitchedImage!=null) {
                logger.debug("      Synchronizing stitched image for area '"+imageArea.areaName+"'");
                // Merged LSM, create new primary image for the merged tile
                Image areaSourceImage = getOrCreatePrimaryImage(objectiveSample, imageArea.stitchedImage, line, areaSageImages, publishingUser);
                synchronizeSecondaryImages(postResult, areaSourceImage, imageArea.stitchedImage.tag, objective);
                exportedNames.add(areaSourceImage.getName());
            }
        }
        
        return true;
    }
    
    private Image getOrCreatePrimaryImage(ObjectiveSample objectiveSample, ImageStack imageStack, Line line, List<Image> sourceSageImages, String publishingUser) throws Exception {

        String imageName = imageStack.name;
        logger.debug("    Synchronizing primary image "+imageName);
        Image image = sage.getImageByName(imageName);
        
        Map<CvTerm,String> consensusValues = new HashMap<CvTerm,String>();
        CvTerm consensusFamily = null;
        
        for(Image sourceImage : sourceSageImages) {
            
            if (consensusFamily!=null && !sourceImage.getFamily().equals(consensusFamily)) {
                throw new Exception("No family consensus across SAGE images: "+sourceImage.getFamily().getId()+"!="+consensusFamily.getId());
            }
            
            consensusFamily = sourceImage.getFamily();

			if (consensusFamily==null) {
				logger.warn("LSM source image has no family: "+sourceImage.getId());
			}
			
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
        consensusValues.put(propertyRelease, release.getName());
        consensusValues.put(propertyWorkstationSampleId, objectiveSample.getParent().getId().toString());
        consensusValues.put(propertyChanSpec, imageStack.chanSpec);
        consensusValues.put(propertyDimensionX, null);
        consensusValues.put(propertyDimensionY, null);
        consensusValues.put(propertyDimensionZ, null);
        
        String pixelRes = imageStack.pixelRes;
        if (pixelRes!=null) {
            String[] res = pixelRes.split("x");
            if (res.length==3) {
                consensusValues.put(propertyDimensionX, res[0]);
                consensusValues.put(propertyDimensionY, res[1]);
                consensusValues.put(propertyDimensionZ, res[2]);
            }
            else {
                logger.warn("Cannot parse pixel resolution: "+res);
            }
        }
        
        String path = imageStack.filepath;
        String url = getWebdavUrl(path);
        
        if (image!=null) {
            if (imageStack.filepath.startsWith(SCALITY_JFS_PREFIX)) {
                image.setJfsPath(imageStack.filepath);    
            }
            else {
                image.setPath(imageStack.filepath);
            }
        	image.setFamily(consensusFamily);
        	image.setLine(line);
        	image.setSource(lab);
        	image.setUrl(url);
        	image.setRepresentative(true);
        	image.setDisplay(true);
        	image.setCreatedBy(CREATED_BY);
            image = sage.saveImage(image);
            logger.debug("      Updated SAGE primary image "+image.getId());
        }
        else {
            image = new Image(consensusFamily, line, lab, imageName, url, imageStack.filepath, true, true, CREATED_BY, createDate);
            if (imageStack.filepath.startsWith(SCALITY_JFS_PREFIX)) {
                image.setJfsPath(imageStack.filepath);    
            }
            else {
                image.setPath(imageStack.filepath);
            }
            image = sage.saveImage(image);
            logger.debug("      Created SAGE primary image "+image.getId());
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

        logger.info("    Synchronized primary image: "+image.getId());
        return image;
    }

    private void synchronizeSecondaryImages(SamplePostProcessingResult artifactRun, Image sourceImage, String tag, String objective) throws Exception {
        logger.debug("    Synchronizing secondary images for image: "+sourceImage.getName());
        List<Integer> sageIds = new ArrayList<>();
        
        FileGroup tileGroup = artifactRun.getGroup(tag);
                
        SecondaryImage secImage = getOrCreateSecondaryImage(DomainUtils.getFilepath(tileGroup, FileType.SignalMip), productSignalsMip, sourceImage);
        sageIds.add(secImage.getId());
        
        SecondaryImage secImage2 = getOrCreateSecondaryImage(DomainUtils.getFilepath(tileGroup, FileType.AllMip), productMultichannelMip, sourceImage);
        sageIds.add(secImage2.getId());
        
        SecondaryImage secImage3 = getOrCreateSecondaryImage(DomainUtils.getFilepath(tileGroup, FileType.SignalMovie), productSignalsTranslation, sourceImage);
        sageIds.add(secImage3.getId());
        
        SecondaryImage secImage4 = getOrCreateSecondaryImage(DomainUtils.getFilepath(tileGroup, FileType.AllMovie), productMultichannelTranslation, sourceImage);
        sageIds.add(secImage4.getId());
        
        logger.info("    Synchronized secondary images: "+sageIds);
    }
    
    private SecondaryImage getOrCreateSecondaryImage(String path, CvTerm productType, Image sourceImage) throws Exception {
        File file = new File(path);
        String imageName = file.getName();
        String url = getWebdavUrl(path);
        
        SecondaryImage secondaryImage = sage.getSecondaryImageByName(imageName);
        
        if (secondaryImage!=null) {
            secondaryImage.setPath(path);
            secondaryImage.setUrl(url);
            sage.saveSecondaryImage(secondaryImage);
            logger.debug("      Updated SAGE secondary image "+secondaryImage.getId());
        }
        else {
            secondaryImage = new SecondaryImage(sourceImage, productType, imageName, path, url, createDate);
            sage.saveSecondaryImage(secondaryImage);
            logger.debug("      Created SAGE secondary image "+secondaryImage.getId());
        }
        
        return secondaryImage;
    }
    
    private void exportLineAnnotations(String lineName) throws Exception {
        
        Line line = getLineByName(lineName);
        SageSession session = sage.getSageSession(lineName, sessionExperimentType, releaseExperiment);
        if (session==null) {
            logger.info("  Will creating new session for line "+lineName);
            session = new SageSession(sessionExperimentType, lab, line, lineName, releaseExperiment, null, createDate);
        }
        else {
            logger.info("  Updating existing session for line "+lineName+" (id="+session.getId()+")");
            session.setLine(line);
            session.setLab(lab);
            session.setExperiment(releaseExperiment);
        }
        
        Set<String> annotators = new HashSet<>();
        Map<String,Observation> newObservationMap = new HashMap<>();
        Map<String,Observation> currObservationMap = new HashMap<>();
        for(Observation observation : session.getObservations()) {
            currObservationMap.put(observation.getType().getName(), observation);
        }

        Multimap<Long,Annotation> annotationDeduper = HashMultimap.<Long,Annotation>create();
        for(Annotation annotation : currLineAnnotationMap.values()) {
            if (!annotatorKeys.contains(annotation.getOwnerKey())) {
                continue;
            }
            Long keyEntityId = annotation.getKeyTerm().getOntologyTermId();
            annotationDeduper.put(keyEntityId, annotation);
        }
        
        Map<Long,Ontology> ontologyCache = new HashMap<>();
        
        for (Long keyEntityId : annotationDeduper.keySet()) {

            // Get latest annotation for this key 
            List<Annotation> annotations = new ArrayList<>(annotationDeduper.get(keyEntityId));
            Collections.sort(annotations, new Comparator<Annotation>() {
                @Override
                public int compare(Annotation o1, Annotation o2) {
                    return o2.getCreationDate().compareTo(o1.getCreationDate());
                }
            });
            Annotation annotationEntity = annotations.get(0);
            
            logger.debug("    Processing line annotation: "+annotationEntity.getName());
            
            annotators.add(DomainUtils.getNameFromSubjectKey(annotationEntity.getOwnerKey()));
            
            Long ontologyId = annotationEntity.getKeyTerm().getOntologyId();
            Ontology ontology = ontologyCache.get(ontologyId);
            if (ontology == null) {
                ontology = domainDao.getDomainObject(null, Ontology.class, ontologyId);
                ontologyCache.put(ontologyId, ontology);
            }
            
            OntologyTerm keyTerm = DomainUtils.findTerm(ontology, annotationEntity.getKeyTerm().getOntologyTermId());
            
            String value = null;
            if (keyTerm instanceof Interval) {
                value = annotationEntity.getValue();
            }
            else if (keyTerm instanceof Tag) {
                value = "1";
            }
            else {
                logger.warn("    Unsupported annotation type: "+keyTerm.getClass().getName());
                continue;
            }
            
            String annotationName = keyTerm.getName();
            CvTerm observationType = sage.getCvTermByName(LINE_ANNOTATION_CV_NAME,annotationName);
            if (observationType==null) {
                logger.warn("    Cannot find corresponding SAGE term for ontology term '"+annotationName+"'");
                continue;
            }

            String obsName = observationType.getName();
            Observation observation = currObservationMap.get(obsName);
            if (observation==null) {
                observation = new Observation(observationType, session, observationTerm, releaseExperiment, value, createDate);
            }
            else {
                if (observation.getId()==null) {
                    logger.warn("    Multiple annotations for the same term: "+obsName+". Only one will be exported.");
                }
                // Update observation value
                observation.setValue(value);
            }
            
            newObservationMap.put(obsName, observation);

            for (int i=1; i<annotations.size(); i++) {
                Annotation dupAnnotation = annotations.get(i);
                logger.warn("    Ignoring duplicate line annotation: "+dupAnnotation.getName());
            }
        }

        session.getObservations().clear();
        session.getObservations().addAll(newObservationMap.values());
        
        if (!session.getObservations().isEmpty()) {
	        logger.info("    Observations: ");
	        for(Observation observation : session.getObservations()) {
	            logger.info("      "+observation.getType().getName()+"="+observation.getValue()+" (id="+observation.getId()+")");
	        }
        }
        
        session.setAnnotator(Task.csvStringFromCollection(annotators));
        logger.debug("    Saved session '"+session.getName()+"' with "+session.getObservations().size()+" observations");
        sage.saveSageSession(session);
    }
    
	/**
	 * Mark sample as having been published 
	 * @param objectiveSample
	 * @throws Exception
	 */
    private void publishSample(ObjectiveSample objectiveSample) throws Exception {
        Sample sample = objectiveSample.getParent();
        String objective = objectiveSample.getObjective();
        OntologyTerm publishedTerm;
        if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
            publishedTerm = publishedTerm20x;
        }
        else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
            publishedTerm = publishedTerm63x;
        }
        else {
            logger.warn("Objective "+objective+" not supported for export for sample: "+sample.getId());
            return;
        }
        for(Annotation annotation : domainDao.getAnnotations(null, Reference.createFor(sample))) {
            if (annotation.getOwnerKey().equals(PUBLICATION_OWNER) && annotation.getKeyTerm().getOntologyTermId().equals(publishedTerm.getId())) {
                logger.trace("  Sample was already exported: "+objectiveSample.getName());
                return;
            }
        }

        domainDao.createAnnotation(PUBLICATION_OWNER, Reference.createFor(sample), OntologyTermReference.createFor(publishedTerm), null);
    }
    
    /**
     * Set to_publish=0 for all the images related to the given sample, and then delete any "Published" annotations on it.
     * @param sample
     * @throws Exception
     */
    private void unpublishSample(ObjectiveSample objectiveSample) throws Exception {
        Sample sample = objectiveSample.getParent();
        String objective = objectiveSample.getObjective();
        OntologyTerm publishedTerm;
        if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
            publishedTerm = publishedTerm20x;
        }
        else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
            publishedTerm = publishedTerm63x;
        }
        else {
            logger.warn("Objective "+objective+" not supported for export for sample: "+sample.getId());
            return;
        }
        boolean stillPublished = false;
        for(Annotation annotation : domainDao.getAnnotations(null, Reference.createFor(sample))) {
            if (annotation.getOwnerKey().equals(PUBLICATION_OWNER) && annotation.getKeyTerm().getOntologyTermId().equals(publishedTerm.getId())) {
                domainDao.remove(PUBLICATION_OWNER, annotation);
            }
            else if (annotation.getKeyTerm().getOntologyTermId().equals(publishedTerm20x.getId())) {
                stillPublished = true;
            }
            else if (annotation.getKeyTerm().getOntologyTermId().equals(publishedTerm63x.getId())) {
                stillPublished = true;
            }
        }

        if (!stillPublished) {
            for(Image image : sage.getImagesByPropertyValue(propertyWorkstationSampleId, sample.getId().toString())) {
                logger.info("    Unpublishing primary image "+image.getName()+" (id="+image.getId()+")");
                sage.setImageProperty(image, propertyToPublish, "N", createDate);
            }   
        }
        
    }
    
    private String getWebdavUrl(String filepath) {
    	if (filepath==null) return null;
        return JFSUtils.getWebdavUrlForJFSPath(filepath);
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
