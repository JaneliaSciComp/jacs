package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.sage.CvTerm;
import org.janelia.it.jacs.model.sage.Image;
import org.janelia.it.jacs.model.sage.ImageProperty;
import org.janelia.it.jacs.model.sage.Line;
import org.janelia.it.jacs.model.sage.SecondaryImage;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Export workstation artifacts into the SAGE database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SageArtifactExportService extends AbstractEntityService {

    private static final String WEBDAV_PREFIX = "http://jacs-webdav.int.janelia.org/WebDAV";
    
    private static final String NO_CONSENSUS = "No Consensus";
    private static final String CREATED_BY = "Janelia Workstation";
    private static final String PUBLISHED_TO = "MBEW";
    
    private SageDAO sage;
    private Map<String,Line> lines = new HashMap<String,Line>();
    private Map<String,List<Integer>> sourceSageImageIdsByArea;
    private Set<String> dataSets;
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
    
    public void execute() throws Exception {
        
        String dataSetIdentifierList = (String)processData.getItem("DATA_SET_IDENTIFIERS");
        if (StringUtils.isEmpty(dataSetIdentifierList)) {
            throw new IllegalArgumentException("DATA_SET_IDENTIFIERS may not be null");
        }
        
        this.dataSets = new HashSet<String>();
        for(String ds : Task.listOfStringsFromCsvString(dataSetIdentifierList)) {
            dataSets.add(ds);
        }
        
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
        
        // TODO: remove this step, it's only for testing
        for(Image image : sage.getImagesByCreator(CREATED_BY)) {
            logger.info("Deleting existing workstation image: "+image.getName()+".");
            sage.removeImage(image);
        }
        
        String[] lines = {"MB010B","MB011A","MB011B","MB011C","MB012B","MB013B","MB014B","MB052B"};
        
        for(String line : lines) {
            line = "GMR_"+line;
            for(Entity entity : entityBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_LINE, line)) {
                if (entity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE) && !entity.getName().contains("~")) {
                    processSample(entity);
                }
            }
            
        }
        
//        Entity sample = entityBean.getEntityById(1844663952051535970L);
//        processSample(sample);
        
//        for(Entity sample : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE)) {
//            processSample(sample);
//            sample.setEntityData(null); // free memory
//        }
    }
    
    private void processSample(Entity sample) throws Exception {
        String dataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        if (dataSetIdentifier==null || !dataSets.contains(dataSetIdentifier)) return;

        populateChildren(sample);
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
        if (childSamples.isEmpty()) {
            exportArtifactForSample(sample);
        }
        else {
            for(Entity childSample : childSamples) {
                exportArtifactForSample(childSample);
            }
        }
    }
    
    private void exportArtifactForSample(Entity sample) throws Exception {

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

        Entity image3d = sample.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        
        if (image3d==null) {
            logger.error("  Sample has no 3d image");
            return;
        }
        
        populateChildren(image3d);
        Entity image2d = image3d.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        Entity movie = image3d.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
        
        // Export to SAGE
        
        if (image2d==null) {
            logger.error("  Sample has no 3d image");
            return;
        }
        
        if (movie==null) {
            logger.warn("  Sample has no movie");
        }

        List<Integer> sourceSageImageIds = sourceSageImageIdsByArea.get("Brain");
        if (sourceSageImageIds==null) {
            logger.warn("No Brain area found, trying empty string in set: "+sourceSageImageIdsByArea.keySet());
            sourceSageImageIds = sourceSageImageIdsByArea.get("");
        }
        if (sourceSageImageIds==null) {
            logger.warn("No empty string found, trying random value in set: "+sourceSageImageIdsByArea.keySet());
            sourceSageImageIds = sourceSageImageIdsByArea.values().iterator().next();
        }
        logger.info("Exporting "+sample.getName()+", sources:"+sourceSageImageIds.size()+", primary:"+image3d+" secondary:"+image2d);
        
        Image image = exportPrimaryImage(image3d, line, sourceSageImageIds);
        exportSecondaryImage(image2d, productMip, image);
        if (movie!=null) {
            exportSecondaryImage(movie, productTranslation, image);
        }
//        exportSecondaryImage(image2d, productTranslationReference, image);
        
    }
    
    private Image exportPrimaryImage(Entity entity, Line line, List<Integer> sourceSageImageIds) throws Exception {
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        
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
        
        logger.info("  Exporting "+entity.getName());
        
        Image image = new Image(consensusFamily, line, source, entity.getId()+"-"+entity.getName(), getUrl(path), path, true, true, CREATED_BY, createDate);
        
        for(CvTerm type : consensusValues.keySet()) {
            type.getId(); // ensure that the type id is loaded
            String value = consensusValues.get(type);
            logger.info("      "+type.getName()+": "+value);
            if (value == null || value.equals(NO_CONSENSUS)) continue;
            addImageProperty(image, type, value);
        }
        
        image = sage.saveImage(image);
        logger.info("    Created SAGE primary image "+image.getId()+" with name "+image.getName());
        return image;
    }
    
    private ImageProperty addImageProperty(Image image, CvTerm type, String value) {
        ImageProperty prop = new ImageProperty(type, image, value, createDate);
        image.getImageProperties().add(prop);
        return prop;
    }
    
    private SecondaryImage exportSecondaryImage(Entity entity, CvTerm productType, Image sourceImage) throws Exception {
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        SecondaryImage secondaryImage = new SecondaryImage(sourceImage, productType, entity.getId()+"-"+entity.getName(), path, getUrl(path), createDate);
        sage.saveSecondaryImage(secondaryImage);
        logger.info("    Created SAGE secondary image "+secondaryImage.getId()+" with name "+secondaryImage.getName());
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
