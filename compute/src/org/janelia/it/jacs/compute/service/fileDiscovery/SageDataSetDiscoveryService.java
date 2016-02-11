package org.janelia.it.jacs.compute.service.fileDiscovery;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.service.entity.sample.SlideImage;
import org.janelia.it.jacs.compute.service.entity.sample.SlideImageGroup;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;

import java.util.*;

/**
 * Discovers images in SAGE which are part of data sets defined in the workstation, and creates or updates Samples 
 * within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */ 
public class SageDataSetDiscoveryService extends AbstractEntityService {
   
	protected FileDiscoveryHelper fileHelper;
	protected SampleHelper sampleHelper;

    protected String dataSetName = null;

    protected int sageRowsProcessed = 0;
    protected int samplesMarkedDesync = 0;
    
    public void execute() throws Exception {

        dataSetName = (String) processData.getItem("DATA_SET_NAME");

        logger.info("Running SAGE data set discovery, ownerKey=" + ownerKey +
                    ", dataSetName=" + dataSetName);

        this.fileHelper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        
        // Clear "visited" flags on all our Samples
        sampleHelper.clearVisited();
        sampleHelper.setDataSetNameFilter(dataSetName);
        
        Entity topLevelFolder = sampleHelper.getTopLevelDataSetFolder();
		logger.info("Will put discovered entities into top level entity "+topLevelFolder.getName()+", id="+topLevelFolder.getId());

        for(Entity dataSet : sampleHelper.getDataSets()) {
            if (dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)==null) {
                logger.info("Skipping non-SAGE data set: "+dataSet.getName());
            }
            else {
                logger.info("Processing data set: "+dataSet.getName());
                processSageDataSet(dataSet);
                sampleHelper.annexSamples();
                markDesyncedSamples(dataSet);
                fixOrderIndices(dataSet);
            }
        }

        logger.info("Processed "+sageRowsProcessed+" rows for "+ownerKey+" ("+dataSetName+"), created "+sampleHelper.getNumSamplesCreated()+
        		" samples, updated "+sampleHelper.getNumSamplesUpdated()+
        		" samples, marked "+sampleHelper.getNumSamplesReprocessed()+
        		" samples for reprocessing, marked "+samplesMarkedDesync+
        		" samples as desynced, annexed "+sampleHelper.getNumSamplesAnnexed()+
        		" samples, added "+sampleHelper.getNumSamplesAdded()+
                " samples to their corresponding data set folders, moved "+sampleHelper.getNumSamplesMovedToBlockedFolder()+
        		" samples to Blocked Data folder.");
        
        if (samplesMarkedDesync>0) {
            logger.warn("IMPORTANT: "+samplesMarkedDesync+" samples were marked as desynchronized. These need to be manually curated and fixed or retired as soon as possible, to avoid confusion caused by duplicate samples!");
        }
       
    }
    
    /**
     * Provide either imageFamily or dataSetIdentifier. 
     */
    protected void processSageDataSet(Entity dataSet) throws Exception {

        Multimap<String,SlideImage> slideGroups = LinkedListMultimap.create();
        
		String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
		logger.info("Querying SAGE for data set: "+dataSetIdentifier);
		
    	ResultSetIterator iterator = null;
    	try {
        	SageDAO sageDAO = new SageDAO(logger);
			iterator = sageDAO.getAllImagePropertiesByDataSet(dataSetIdentifier);
    		
			// Load all slides for this data set into memory, so that we don't over-stay our welcome on the database cursor
			// in case we need to do some time-intensive stuff (e.g. adding permissions)
        	while (iterator.hasNext()) {
        		Map<String,Object> row = iterator.next();
        		SlideImage slideImage = createSlideImage(row);
				slideGroups.put(slideImage.getSlideCode(), slideImage);
				sageRowsProcessed++;
			}
    	}
        finally {
            if (iterator!=null) {
                try {
                    iterator.close();
                }
                catch (Exception e) {
                    logger.error("processSageDataSet - Unable to close ResultSetIterator for data set "+dataSet.getName()+
                            "\n"+e.getMessage()+"\n. Continuing...");
                }
            }
        }
		
    	// Now process all the slide
		for (String slideCode : slideGroups.keySet()) {
            processSlideGroup(dataSet, slideCode, slideGroups.get(slideCode));
		}
    }
    
    protected SlideImage createSlideImage(Map<String,Object> row) throws Exception {

        SlideImage slideImage = new SlideImage();
        slideImage.setSageId((Long)row.get("image_query_id"));
		slideImage.setSlideCode((String) row.get("light_imagery_slide_code"));
		slideImage.setImagePath((String) row.get("image_query_path"));
		slideImage.setJfsPath((String) row.get("image_query_jfs_path"));
		slideImage.setTileType((String) row.get("light_imagery_tile"));
		slideImage.setLine((String) row.get("image_query_line"));
        slideImage.setCrossBarcode((String) row.get("fly_cross_barcode"));
		slideImage.setChannelSpec((String) row.get("light_imagery_channel_spec"));
		slideImage.setGender((String) row.get("light_imagery_gender"));
		slideImage.setArea((String) row.get("light_imagery_area"));
		slideImage.setAge((String) row.get("light_imagery_age"));
		slideImage.setChannels((String) row.get("light_imagery_channels"));
		slideImage.setMountingProtocol((String) row.get("light_imagery_mounting_protocol"));
        slideImage.setTissueOrientation((String) row.get("light_imagery_tissue_orientation"));
        slideImage.setVtLine((String) row.get("light_imagery_vt_line"));
        
        Date createDate = (Date) row.get("image_query_create_date");
        if (createDate!=null) {
            String tmogDate = ISO8601Utils.format(createDate);
            if (tmogDate!=null) {
                slideImage.setTmogDate(tmogDate);
            }
        }
        
		slideImage.setEffector((String)row.get("fly_effector"));
		String objectiveStr = (String)row.get("light_imagery_objective");
		if (objectiveStr!=null) {
		    if (objectiveStr.contains(Objective.OBJECTIVE_10X.getName())) {
                slideImage.setObjective(Objective.OBJECTIVE_10X.getName());
            }
		    else if (objectiveStr.contains(Objective.OBJECTIVE_20X.getName())) {
    		    slideImage.setObjective(Objective.OBJECTIVE_20X.getName());
    		}
    		else if (objectiveStr.contains(Objective.OBJECTIVE_40X.getName())) {
                slideImage.setObjective(Objective.OBJECTIVE_40X.getName());
            }
    		else if (objectiveStr.contains(Objective.OBJECTIVE_63X.getName())) {
                slideImage.setObjective(Objective.OBJECTIVE_63X.getName());
            }
		}
		String voxelSizeX = (String)row.get("light_imagery_voxel_size_x");
		String voxelSizeY = (String)row.get("light_imagery_voxel_size_y");
		String voxelSizeZ = (String)row.get("light_imagery_voxel_size_z");
		if (voxelSizeX!=null && voxelSizeY!=null && voxelSizeZ!=null) {
		    slideImage.setOpticalRes(voxelSizeX,voxelSizeY,voxelSizeZ);
		}
		String imageSizeX = (String)row.get("light_imagery_dimension_x");
        String imageSizeY = (String)row.get("light_imagery_dimension_y");
        String imageSizeZ = (String)row.get("light_imagery_dimension_z");
        if (imageSizeX!=null && imageSizeY!=null && imageSizeZ!=null) {
            slideImage.setPixelRes(imageSizeX,imageSizeY,imageSizeZ);
        }
		return slideImage;
    }
    
    protected void processSlideGroup(Entity dataSet, String slideCode, Collection<SlideImage> slideGroup) throws Exception {
    	
        if (slideCode==null) {
            for(SlideImage slideImage : slideGroup) {
                logger.error("SAGE id "+slideImage.getSageId()+" has null slide code");
            }
            return;
        }
        
        HashMap<String, SlideImageGroup> tileGroups = new HashMap<>();
        
        logger.info("Processing "+slideCode+", "+slideGroup.size()+" slide images");
        
        int tileNum = 0;
        for(SlideImage slideImage : slideGroup) {
        	
        	if (slideImage.getFile()==null) {
        		logger.warn("Slide code "+slideImage.getSlideCode()+" has an image with a null path, so it is not ready for synchronization.");
        		return;
        	}

            String area = slideImage.getArea();
            String tag = slideImage.getTileType();
            if (tag==null) {
                tag = "Tile "+(tileNum+1);
            }
            
            String groupKey = area+"_"+tag;
        	SlideImageGroup tileGroup = tileGroups.get(groupKey);
            if (tileGroup==null) {
            	tileGroup = new SlideImageGroup(area, tag);
        		tileGroups.put(groupKey, tileGroup);
            }
            
        	tileGroup.addFile(slideImage);
    		if (slideImage.getJfsPath()==null && !slideImage.getFile().exists()) {
    			logger.warn("File referenced by SAGE does not exist: "+slideImage.getImagePath());
    			return;
    		}
        	
            tileNum++;
        }
        
    	List<SlideImageGroup> tileGroupList = new ArrayList<>(tileGroups.values());
    	
        // Sort the pairs by their tag name
        Collections.sort(tileGroupList, new Comparator<SlideImageGroup>() {
			@Override
			public int compare(SlideImageGroup o1, SlideImageGroup o2) {
				return o1.getTag().compareTo(o2.getTag());
			}
		});
        
        sampleHelper.createOrUpdateSample(null, slideCode, dataSet, tileGroupList);
    }
    
    protected void markDesyncedSamples(Entity dataSet) throws Exception {
        String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        Entity dataSetFolder = sampleHelper.getDataSetFolderByIdentifierMap().get(dataSetIdentifier);
        if (dataSetFolder==null) return;
        
        logger.info("Marking desynchronized samples in dataSet: "+dataSet.getName());

        // Make sure to fetch fresh samples, so that we have the latest visited flags
        Map<Long, Entity> samples = new HashMap<>();
        for(Entity entity : entityBean.getChildEntities(dataSetFolder.getId())) {
            if (entity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                samples.put(entity.getId(), entity);    
            }   
        }
        
        List<EntityData> dataSetEds = new ArrayList<>(dataSetFolder.getEntityData());
        for(EntityData ed : dataSetEds) {
            Entity tmpChildEntity = ed.getChildEntity();
            // Ignore Data Set attributes that have no child target entity
            if (null==tmpChildEntity||null==tmpChildEntity.getId()) continue;
            Entity sample = samples.get(tmpChildEntity.getId());
            if (sample==null || !sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) continue;
            if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)==null) {
                // Sample was not visited this time around, it should be marked as desynchronized, and eventually retired
                boolean blocked = EntityConstants.VALUE_BLOCKED.equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS));
                // Ignore blocked samples, they don't need to be synchronized 
                if (!blocked) {
                    logger.info("  Marking unvisited sample as desynced: "+sample.getName()+" (id="+sample.getId()+")");
                    entityBean.setOrUpdateValue(sample.getId(), EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_DESYNC);
                }
                samplesMarkedDesync++;
            }
        }
    }
    
    protected void fixOrderIndices(Entity dataSet) throws Exception {
     
        String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        Entity dataSetFolder = sampleHelper.getDataSetFolderByIdentifierMap().get(dataSetIdentifier);
        if (dataSetFolder==null) return;        
        logger.info("Fixing sample order indicies in "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");

        List<EntityData> orderedData = new ArrayList<>();
        for(EntityData ed : dataSetFolder.getEntityData()) {
            if (ed.getChildEntity()!=null) {
                orderedData.add(ed);
            }
        }
        Collections.sort(orderedData, new Comparator<EntityData>() {
            @Override
            public int compare(EntityData o1, EntityData o2) {
                int c = o1.getChildEntity().getCreationDate().compareTo(o2.getChildEntity().getCreationDate());
                if (c==0) {
                    return o1.getChildEntity().getId().compareTo(o2.getChildEntity().getId());
                }
                return c;
            }
        });
        
        int orderIndex = 0;
        for(EntityData ed : orderedData) {
            if (ed.getOrderIndex()==null || orderIndex!=ed.getOrderIndex()) {
                logger.debug("  Updating "+ed.getChildEntity().getName()+" with order index "+orderIndex+" (was "+ed.getOrderIndex()+")");
                entityBean.updateChildIndex(ed, orderIndex);
            }
            orderIndex++;
        }
    }
}
