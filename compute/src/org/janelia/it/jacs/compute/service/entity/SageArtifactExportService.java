package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String WEBDAV_PREFIX = "http://jacs-webdav.int.janelia.org/WebDAV/";
    
    private static final String NO_CONSENSUS = "No Consensus";
    private static final String SAGE_SOURCE_ID_JFRC = "JFRC";
    private static final String SAGE_FAMILY_ID_COMPUTED = "computed";
    private static final String SAGE_PRODUCT_MIP = "projection_all";
    private static final String SAGE_PRODUCT_TRANSLATION = "translation";
    private static final String SAGE_PRODUCT_TRANSLATION_REFERENCE = "translation_reference";
    private static final String SAGE_PROPERTY_CREATED_BY = "created_by";
    private static final String SAGE_PROPERTY_PUBLISHED = "";
    private static final String SAGE_PROPERTY_VALUE_CREATED_BY = "Janelia Workstation";
    private static final String SAGE_PROPERTY_VALUE_PUBLISHED = "MBEW";
    
    private SageDAO sage;
    private Map<String,Line> lines = new HashMap<String,Line>();
    private List<Integer> sourceSageImageIds;
    
    private CvTerm productMip;
    private CvTerm productTranslation;
    private CvTerm productTranslationReference;
    private CvTerm propertyCreatedBy;
    private CvTerm propertyPublished;
    private CvTerm family;
    private CvTerm source;
    
    public void execute() throws Exception {
        
        String dataSetIdentifierList = (String)processData.getItem("DATA_SET_IDENTIFIERS");
        if (StringUtils.isEmpty(dataSetIdentifierList)) {
            throw new IllegalArgumentException("DATA_SET_IDENTIFIERS may not be null");
        }
        
        Set<String> dataSets = new HashSet<String>();
        for(String ds : Task.listOfStringsFromCsvString(dataSetIdentifierList)) {
            dataSets.add(ds);
        }
        
        this.sage = new SageDAO(logger);
        
        this.productMip = sage.getCvTermByName(SAGE_PRODUCT_MIP);
        this.productTranslation = sage.getCvTermByName(SAGE_PRODUCT_TRANSLATION);
        this.productTranslationReference = sage.getCvTermByName(SAGE_PRODUCT_TRANSLATION_REFERENCE);
        this.propertyCreatedBy = sage.getCvTermByName(SAGE_PROPERTY_CREATED_BY);
        this.propertyPublished = sage.getCvTermByName(SAGE_PROPERTY_PUBLISHED);
        this.family = sage.getCvTermByName(SAGE_FAMILY_ID_COMPUTED);
        this.source = sage.getCvTermByName(SAGE_SOURCE_ID_JFRC);
        
        for(Entity sample : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_SAMPLE)) {
            String dataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            if (dataSetIdentifier==null || !dataSets.contains(dataSetIdentifier)) continue;

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
            
            sample.setEntityData(null); // free memory
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
        
        this.sourceSageImageIds = new ArrayList<Integer>();
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sample)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsmStack) throws Exception {
                String sageId = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_ID);
//                logger.info("Got lsm "+lsmStack.getId()+" sageId="+sageId);
                if (sageId!=null) {
                    sourceSageImageIds.add(new Integer(sageId));    
                }
            }
        });
        
        // Collect artifacts for export
        
        Entity image3d = sample.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        Entity image2d = sample.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
        
        logger.info("Exporting "+sample.getName()+", sources:"+sourceSageImageIds.size()+", primary:"+image3d+" secondary:"+image2d);
        
        // Export to SAGE
        
        if (image3d==null) {
            logger.warn("  Sample has no 3d image");
            return;
        }
        
        if (image2d==null) {
            logger.warn("  Sample has no 3d image");
            return;
        }
        
//        Image image = exportPrimaryImage(image3d, line);
//        exportSecondaryImage(image2d, productMip, image);
//        exportSecondaryImage(image2d, SAGE_PRODUCT_TRANSLATION, sageId);
//        exportSecondaryImage(image2d, SAGE_PRODUCT_TRANSLATION_REFERENCE, sageId);
        
    }
    
    private Image exportPrimaryImage(Entity entity, Line line) throws Exception {
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        Image image = new Image(family, line, source, entity.getName(), getUrl(path), path, true, true, null);
        
        Map<CvTerm,String> consensusValues = new HashMap<CvTerm,String>();
        
        for(Image sourceImage : sage.getImages(sourceSageImageIds)) {
            for(ImageProperty prop : sourceImage.getImageProperties()) {
                if (consensusValues.containsKey(prop.getType())) {
                    String value = consensusValues.get(prop.getType());
                    if ((value==null && prop.getValue()==null) || prop.equals(prop.getValue())) {
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
        
        consensusValues.put(propertyCreatedBy, SAGE_PROPERTY_VALUE_CREATED_BY);
        consensusValues.put(propertyPublished, SAGE_PROPERTY_VALUE_PUBLISHED);
        
        logger.info("  Export "+image.getName());
        
        for(CvTerm type : consensusValues.keySet()) {
            String value = consensusValues.get(type);
            if (value.equals(NO_CONSENSUS)) continue;
            addImageProperty(image, type, value);
            logger.info("  "+type.getDisplayName()+": "+value);
        }
        
        image = sage.saveImage(image);
        logger.info("  Created SAGE primary image "+image.getId()+" with name "+entity.getName());
        return image;
    }
    
    private ImageProperty addImageProperty(Image image, CvTerm type, String value) {
        ImageProperty prop = new ImageProperty(type, image, value, null);
        image.getImageProperties().add(prop);
        return prop;
    }
    
    private SecondaryImage exportSecondaryImage(Entity entity, CvTerm productType, Image sourceImage) throws Exception {
        String path = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        SecondaryImage secondaryImage = new SecondaryImage(sourceImage, productType, entity.getName(), path, getUrl(path), null);
        logger.info("  Created SAGE secondary image "+secondaryImage.getId()+" with name "+entity.getName());
        return secondaryImage;
    }
    
    private String getUrl(String filepath) {
        return WEBDAV_PREFIX+filepath;
    }
}
