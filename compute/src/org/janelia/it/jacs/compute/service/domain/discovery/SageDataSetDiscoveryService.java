package org.janelia.it.jacs.compute.service.domain.discovery;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * Discovers images in SAGE which are part of data sets defined in the workstation, and creates or updates Samples 
 * within the domain model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */ 
public class SageDataSetDiscoveryService extends AbstractDomainService {

    private SageDAO sageDAO;
    private SampleHelperNG sampleHelper;

    private Set<Long> visitedLsmIds = new HashSet<>();
    private Set<Long> visitedSampleIds = new HashSet<>();
	private Map<String, Map<String, Object>> lineMap = new HashMap<>();
    private String dataSetName = null;
    private int sageRowsProcessed = 0;
    private int lsmsMarkedDesync = 0;
    private int samplesMarkedDesync = 0;

    public void execute() throws Exception {

        this.dataSetName = data.getItemAsString("DATA_SET_NAME");

        logger.info("Running SAGE data set discovery, ownerKey=" + ownerKey + ", dataSetName=" + dataSetName);

        this.sageDAO = new SageDAO(logger);
        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger);
        sampleHelper.setDataSetNameFilter(dataSetName);

        buildLinePropertyMap();
        
        for(DataSet dataSet : sampleHelper.getDataSets()) {
            if (!dataSet.isSageSync()) {
                logger.info("Skipping non-SAGE data set: "+dataSet.getName());
            }
            else {
                logger.info("Processing data set: "+dataSet.getName());
                processSageDataSet(dataSet);
                markDesynced(dataSet);
            }
        }

        logger.info("Processed "+sageRowsProcessed+" rows for "+ownerKey+" ("+dataSetName+"), created "+sampleHelper.getNumSamplesCreated()+
                " samples, updated "+sampleHelper.getNumSamplesUpdated()+
                " samples, marked "+sampleHelper.getNumSamplesReprocessed()+
                " samples for reprocessing, marked "+lsmsMarkedDesync+
                " lsms as desynched, marked "+samplesMarkedDesync+
                " samples as desynced");

        if (lsmsMarkedDesync > 0) {
            logger.warn("IMPORTANT: "+lsmsMarkedDesync+" LSMs were marked as desynchronized. These need to be manually curated and fixed or deleted as soon as possible.");
        }
        
        if (samplesMarkedDesync > 0) {
            logger.warn("IMPORTANT: "+samplesMarkedDesync+" samples were marked as desynchronized. These need to be manually curated and fixed or deleted as soon as possible.");
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
                
                LSMImage lsm = sampleHelper.createOrUpdateLSM(new SlideImage(allProps));
                slideGroups.put(lsm.getSlideCode(), lsm);
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
    
    private void processSlideGroup(DataSet dataSet, String slideCode, Collection<LSMImage> lsms) throws Exception {
    
        for(LSMImage lsm : lsms) {
        	
        	if (lsm.getSlideCode()==null) {
        		logger.error("SAGE id "+lsm.getSageId()+" has null slide code");
        		return;
        	}
        	
	    	if (lsm.getFilepath()==null) {
	    		logger.warn("Slide code "+lsm.getSlideCode()+" has an image with a null path, so it is not ready for synchronization.");
	    		return;
	    	}
	    	
	    	visitedLsmIds.add(lsm.getId());
        }
    	
        Sample sample = sampleHelper.createOrUpdateSample(slideCode, dataSet, lsms);
        visitedSampleIds.add(sample.getId());
    }
    
    private void markDesynced(DataSet dataSet) throws Exception {
        String dataSetIdentifier = dataSet.getIdentifier();

        logger.info("Marking desynchronized LSMs in dataSet: "+dataSet.getName());
        for(LSMImage lsm : domainDao.getLSMsForDataSet(ownerKey, dataSetIdentifier)) {
            if (!visitedLsmIds.contains(lsm.getId())) {
                logger.info("  Marking unvisited LSM as desynced: "+lsm.getName()+" (id="+lsm.getId()+")");
                domainDao.updateProperty(ownerKey, LSMImage.class, lsm.getId(), "sageSynced", false);
                lsmsMarkedDesync++;
            }
        }
        
        logger.info("Marking desynchronized samples in dataSet: "+dataSet.getName());
        for(Sample sample : domainDao.getSamplesForDataSet(ownerKey, dataSetIdentifier)) {
            if (!visitedSampleIds.contains(sample.getId())) {
                // Sample was not visited this time around, it should be marked as desynchronized, and eventually retired
                boolean blocked = DomainConstants.VALUE_BLOCKED.equals(sample.getStatus());
                boolean retired = DomainConstants.VALUE_RETIRED.equals(sample.getStatus());
                if (!blocked && !retired) {
                    logger.info("  Marking unvisited sample as desynced: "+sample.getName()+" (id="+sample.getId()+")");
                    domainDao.updateProperty(ownerKey, Sample.class, sample.getId(), "status", DomainConstants.VALUE_DESYNC);
                }
                domainDao.updateProperty(ownerKey, Sample.class, sample.getId(), "sageSynced", false);
                samplesMarkedDesync++;
            }
        }
    }
}
