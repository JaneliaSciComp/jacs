package org.janelia.it.jacs.compute.service.fileDiscovery;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected int samplesMovedToRetiredFolder = 0;
    
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

    	logger.info("Processing data sets...");
        for(Entity dataSet : sampleHelper.getDataSets()) {
            if (dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAGE_SYNC)==null) {
                logger.info("Skipping non-SAGE data set: "+dataSet.getName());
            }
            else {
                logger.info("Processing data set: "+dataSet.getName());
                processSageDataSet(dataSet);
                sampleHelper.annexSamples();
                cleanUnvisitedSamples(dataSet);
                fixOrderIndices(dataSet);
            }
        }

        logger.info("Processed "+sageRowsProcessed+" rows, created "+sampleHelper.getNumSamplesCreated()+
        		" samples, updated "+sampleHelper.getNumSamplesUpdated()+" samples, added "+sampleHelper.getNumSamplesAdded()+
        		" samples to their corresponding data set folders. Annexed "+sampleHelper.getNumSamplesAnnexed()+
        		" samples, moved "+sampleHelper.getNumSamplesMovedToBlockedFolder()+
        		" samples to Blocked Data folder, and moved "+samplesMovedToRetiredFolder+" samples to the Retired Data folder.");
    }
    
    /**
     * Provide either imageFamily or dataSetIdentifier. 
     */
    protected void processSageDataSet(Entity dataSet) throws Exception {
    	
    	SageDAO sageDAO = new SageDAO(logger);
    	ResultSetIterator iterator = null;
    	try {
    		List<SlideImage> slideGroup = null;
    		String currSlideCode = null;
    		
    		String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
			logger.info("Querying SAGE for data set: "+dataSetIdentifier);
			iterator = sageDAO.getImagesByDataSet(dataSetIdentifier);
    		
        	while (iterator.hasNext()) {
        		Map<String,Object> row = iterator.next();
        		SlideImage slideImage = createSlideImage(row);
        		
				if (!slideImage.getSlideCode().equals(currSlideCode)) {
					// Process the current group
					if (slideGroup != null) {
		                processSlideGroup(dataSet, currSlideCode, slideGroup);
					}
					// Start a new group
					currSlideCode = slideImage.getSlideCode();
					slideGroup = new ArrayList<SlideImage>();
				}
				
				slideGroup.add(slideImage);
				sageRowsProcessed++;
			}

			// Process the last group
			if (slideGroup != null) {
                processSlideGroup(dataSet, currSlideCode, slideGroup);
			}
    	}
        finally {
        	if (iterator!=null) {
        	    iterator.close();
        	}
        }
    }
    
    protected SlideImage createSlideImage(Map<String,Object> row) {
        SlideImage slideImage = new SlideImage();
        slideImage.setSageId((Long)row.get("id"));
		slideImage.setSlideCode((String)row.get("slide_code"));
		slideImage.setImagePath((String)row.get("path"));
		slideImage.setTileType((String)row.get("tile"));
		slideImage.setLine((String)row.get("line"));
        slideImage.setCrossBarcode((String)row.get("cross_barcode"));
		slideImage.setChannelSpec((String)row.get("channel_spec"));
		slideImage.setGender((String)row.get("gender"));
		slideImage.setArea((String)row.get("area"));
		slideImage.setAge((String)row.get("age"));
		slideImage.setChannels((String)row.get("channels"));
		slideImage.setMountingProtocol((String)row.get("mounting_protocol"));
		slideImage.setEffector((String)row.get("effector"));
		String objectiveStr = (String)row.get("objective");
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
		String voxelSizeX = (String)row.get("voxel_size_x");
		String voxelSizeY = (String)row.get("voxel_size_y");
		String voxelSizeZ = (String)row.get("voxel_size_z");
		if (voxelSizeX!=null && voxelSizeY!=null && voxelSizeZ!=null) {
		    slideImage.setOpticalRes(voxelSizeX,voxelSizeY,voxelSizeZ);
		}
		String imageSizeX = (String)row.get("dimension_x");
        String imageSizeY = (String)row.get("dimension_y");
        String imageSizeZ = (String)row.get("dimension_z");
        if (imageSizeX!=null && imageSizeY!=null && imageSizeZ!=null) {
            slideImage.setPixelRes(imageSizeX,imageSizeY,imageSizeZ);
        }
		return slideImage;
    }
    
    protected void processSlideGroup(Entity dataSet, String slideCode, List<SlideImage> slideGroup) throws Exception {
    	
        HashMap<String, SlideImageGroup> tileGroups = new HashMap<String, SlideImageGroup>();
        
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
    		if (!slideImage.getFile().exists()) {
    			logger.warn("File referenced by SAGE does not exist: "+slideImage.getImagePath());
    			return;
    		}
        	
            tileNum++;
        }
        
    	List<SlideImageGroup> tileGroupList = new ArrayList<SlideImageGroup>(tileGroups.values());
    	
        // Sort the pairs by their tag name
        Collections.sort(tileGroupList, new Comparator<SlideImageGroup>() {
			@Override
			public int compare(SlideImageGroup o1, SlideImageGroup o2) {
				return o1.getTag().compareTo(o2.getTag());
			}
		});
        
        sampleHelper.createOrUpdateSample(null, slideCode, dataSet, tileGroupList);
    }
    
    protected void cleanUnvisitedSamples(Entity dataSet) throws Exception {
        String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        Entity dataSetFolder = sampleHelper.getDataSetFolderByIdentifierMap().get(dataSetIdentifier);
        if (dataSetFolder==null) return;
        
        logger.info("Cleaning unvisited samples in dataSet: "+dataSet.getName());

        // Make sure to fetch fresh samples, so that we have the latest visited flags
        Map<Long, Entity> samples = new HashMap<Long, Entity>();
        for(Entity entity : entityBean.getChildEntities(dataSetFolder.getId())) {
            samples.put(entity.getId(), entity);
        }
        
        Set<Long> retiredIds = new HashSet<Long>();
        Entity retiredDataFolder = sampleHelper.getRetiredDataFolder();
        for(EntityData ed : retiredDataFolder.getEntityData()) {
            if (ed.getChildEntity()!=null) {
                retiredIds.add(ed.getChildEntity().getId());
            }
        }
        
        List<EntityData> dataSetEds = new ArrayList<EntityData>(dataSetFolder.getEntityData());
        for(EntityData ed : dataSetEds) {
            Entity sample = samples.get(ed.getChildEntity().getId());
            if (sample==null || !sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) continue;
            if (sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)==null) {
                // Sample was not visited this time around, it should be:
                if (retiredIds.contains(sample.getId())) {
                    logger.info("  Sample was already retired. Removing from data folder: "+sample.getName()+" (id="+sample.getId()+")");   
                    entityBean.deleteEntityData(ed);
                }
                else {
                    entityBean.loadLazyEntity(sample, false);
                    boolean blocked = EntityConstants.VALUE_BLOCKED.equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS));
                    // Ignore blocked samples, they don't need to be retired
                    if (!blocked) {
                        logger.info("  Moving unvisited sample to retired data folder: "+sample.getName()+" (id="+sample.getId()+")");
                        dataSetFolder.getEntityData().remove(ed);
                        retiredDataFolder.getEntityData().add(ed);
                        ed.setParentEntity(retiredDataFolder);
                        entityBean.saveOrUpdateEntityData(ed);
                        sample.setName(sample.getName()+"-Retired");
                        entityBean.saveOrUpdateEntity(sample);
                    }
                }
                samplesMovedToRetiredFolder++;
            }
        }
    }
    
    protected void fixOrderIndices(Entity dataSet) throws Exception {
     
        String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        Entity dataSetFolder = sampleHelper.getDataSetFolderByIdentifierMap().get(dataSetIdentifier);
        if (dataSetFolder==null) return;        
        logger.info("Fixing sample order indicies in "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");

        List<EntityData> orderedData = new ArrayList<EntityData>();
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
