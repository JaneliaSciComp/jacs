package org.janelia.it.jacs.compute.service.domain.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.entity.EntityConstants;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * Discovers images in SAGE which are part of data sets defined in the workstation, and creates or updates Samples 
 * within the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */ 
public class SageDataSetDiscoveryService extends AbstractDomainService {

    private SageDAO sageDAO;
    private SampleHelperNG sampleHelper;

	private Map<String, Map<String, Object>> lineMap = new HashMap<>();
    private String dataSetName = null;
    private int sageRowsProcessed = 0;
    private int samplesMarkedDesync = 0;

    public void execute() throws Exception {

        this.dataSetName = data.getItemAsString("DATA_SET_NAME");

        logger.info("Running SAGE data set discovery, ownerKey=" + ownerKey + ", dataSetName=" + dataSetName);

        this.sageDAO = new SageDAO(logger);
        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger);

        // Clear "visited" flags on all our Samples
        sampleHelper.clearVisited();
        sampleHelper.setDataSetNameFilter(dataSetName);

        buildLinePropertyMap();
        
        for(DataSet dataSet : sampleHelper.getDataSets()) {
            if (!dataSet.isSageSync()) {
                logger.info("Skipping non-SAGE data set: "+dataSet.getName());
            }
            else {
                logger.info("Processing data set: "+dataSet.getName());
                processSageDataSet(dataSet);
                markDesyncedSamples(dataSet);
            }
        }

        logger.info("Processed "+sageRowsProcessed+" rows for "+ownerKey+" ("+dataSetName+"), created "+sampleHelper.getNumSamplesCreated()+
                " samples, updated "+sampleHelper.getNumSamplesUpdated()+
                " samples, marked "+sampleHelper.getNumSamplesReprocessed()+
                " samples for reprocessing, marked "+samplesMarkedDesync+
                " samples as desynced, added "+sampleHelper.getNumSamplesAdded()+
                " samples to their corresponding data set folders, moved "+sampleHelper.getNumSamplesMovedToBlockedFolder()+
                " samples to Blocked Data folder.");
        
        if (samplesMarkedDesync > 0) {
            logger.warn("IMPORTANT: "+samplesMarkedDesync+" samples were marked as desynchronized. These need to be manually curated and fixed or retired as soon as possible, to avoid confusion caused by duplicate samples!");
        }

    }
    
	private void buildLinePropertyMap() throws Exception {
		logger.info("Building property map for all lines");
		ResultSetIterator iterator = null;
		try {
			iterator = sageDAO.getAllLineProperties();
			while (iterator.hasNext()) {
				Map<String, Object> lineProperties = iterator.next();
				lineMap.put((String) lineProperties.get(SageDAO.LINE_PROP_LINE_TERM), lineProperties);
			}
		} 
		finally {
			if (iterator != null) {
                try {
                	iterator.close();
                }
                catch (Exception e) {
                    logger.error("Unable to close ResultSetIterator for line properties "+
                            "\n"+e.getMessage()+"\n. Continuing...");
                }
			}
		}
		logger.info("Retrieved properties for " + lineMap.size() + " lines");
    }
    
    /**
     * Provide either imageFamily or dataSetIdentifier. 
     */
    private void processSageDataSet(DataSet dataSet) throws Exception {

        Multimap<String,LSMImage> slideGroups = LinkedListMultimap.create();
        
        String dataSetIdentifier = dataSet.getIdentifier();
        logger.info("Querying SAGE for data set: "+dataSetIdentifier);

        ResultSetIterator iterator = null;
        try {
            iterator = sageDAO.getAllImagePropertiesByDataSet(dataSetIdentifier);

            while (iterator.hasNext()) {
                Map<String,Object> row = iterator.next();
                
                Map<String,Object> allProps = new HashMap<>(row);
				String line = (String) row.get(SageDAO.IMAGE_PROP_LINE_TERM);
				if (line != null) {
					Map<String, Object> lineProperties = lineMap.get(line);
					if (lineProperties != null) {
						allProps.putAll(lineProperties);
					}
				}
                
                SlideImage slideImage = new SlideImage(allProps);
                LSMImage lsm = sampleHelper.createOrUpdateLSM(slideImage);
                slideGroups.put(slideImage.getSlideCode(), lsm);
                sageRowsProcessed++;
            }
        }
        finally {
            if (iterator!=null) {
                try {
                    iterator.close();
                }
                catch (Exception e) {
                    logger.error("Unable to close ResultSetIterator for data set "+dataSet.getName()+
                            "\n"+e.getMessage()+"\n. Continuing...");
                }
            }
        }

        // Now process all the slide
        for (String slideCode : slideGroups.keySet()) {
            processSlideGroup(dataSet, slideCode, slideGroups.get(slideCode));
        }
    }
    
    private void processSlideGroup(DataSet dataSet, String slideCode, Collection<LSMImage> slideGroup) throws Exception {

        if (slideCode==null) {
            for(LSMImage lsmImage : slideGroup) {
                logger.error("SAGE id "+lsmImage.getSageId()+" has null slide code");
            }
            return;
        }
        
        HashMap<String, SlideImageGroup> tileGroups = new HashMap<>();
        
        logger.debug("Processing "+slideCode+", "+slideGroup.size()+" slide images");
        
        int tileNum = 0;
        for(LSMImage lsmImage : slideGroup) {
        	
        	if (lsmImage.getFilepath()==null) {
        		logger.warn("Slide code "+lsmImage.getSlideCode()+" has an image with a null path, so it is not ready for synchronization.");
        		return;
        	}

            String area = lsmImage.getAnatomicalArea();
            String tag = lsmImage.getTile();
            if (tag==null) {
                tag = "Tile "+(tileNum+1);
            }
            
            String groupKey = area+"_"+tag;
            SlideImageGroup tileGroup = tileGroups.get(groupKey);
            if (tileGroup==null) {
                tileGroup = new SlideImageGroup(area, tag);
                tileGroups.put(groupKey, tileGroup);
            }

            tileGroup.addFile(lsmImage);
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
        
        sampleHelper.createOrUpdateSample(slideCode, dataSet, tileGroupList);
    }

    private void markDesyncedSamples(DataSet dataSet) throws Exception {
        String dataSetIdentifier = dataSet.getIdentifier();

        logger.info("Marking desynchronized samples in dataSet: "+dataSet.getName());

        // Make sure to fetch fresh samples, so that we have the latest visited flags
        for(Sample sample : domainDao.getSamplesForDataSet(ownerKey, dataSetIdentifier)) {
            if (!sample.getVisited()) {
                // Sample was not visited this time around, it should be marked as desynchronized, and eventually retired
                boolean blocked = EntityConstants.VALUE_BLOCKED.equals(sample.getStatus());
                boolean retired = EntityConstants.VALUE_RETIRED.equals(sample.getStatus());
                // Ignore blocked and retired samples, they don't need to be synchronized 
                if (!blocked && !retired) {
                    logger.info("  Marking unvisited sample as desynced: "+sample.getName()+" (id="+sample.getId()+")");
                    domainDao.updateProperty(ownerKey, Sample.class, sample.getId(), "status", DomainConstants.VALUE_DESYNC);
                }
                samplesMarkedDesync++;
            }
        }
    }
}
