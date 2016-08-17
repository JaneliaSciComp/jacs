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

    private Set<Long> visitedLsmIds;
    private Set<Long> visitedSampleIds;
    private String dataSetName = null;
    private int sageRowsProcessed = 0;
    private int lsmsMarkedDesync = 0;
    private int samplesMarkedDesync = 0;

    public void execute() throws Exception {

        this.dataSetName = data.getItemAsString("DATA_SET_NAME");

        logger.info("Running SAGE data set discovery, ownerKey=" + ownerKey + ", dataSetName=" + dataSetName);

        this.sageDAO = new SageDAO(logger);
        this.sampleHelper = new SampleHelperNG(ownerKey, logger);
        sampleHelper.setDataSetNameFilter(dataSetName);

        SageDiscoverServiceHelper sageDiscoverServiceHelper = new SageDiscoverServiceHelper(sampleHelper);

        for(DataSet dataSet : sampleHelper.getDataSets()) {
            if (!dataSet.isSageSync()) {
                logger.info("Skipping non-SAGE data set: "+dataSet.getName());
            }
            else {
                logger.info("Processing data set: "+dataSet.getName());
                sageDiscoverServiceHelper.processSageDataSet(dataSet);
                visitedLsmIds = sageDiscoverServiceHelper.getVisitedLsmIds();
                visitedSampleIds = sageDiscoverServiceHelper.getVisitedSampleIds();
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
    
    private void markDesynced(DataSet dataSet) throws Exception {
        String dataSetIdentifier = dataSet.getIdentifier();

        logger.info("Marking desynchronized LSMs in dataSet: "+dataSet.getName());
        for(LSMImage lsm : domainDao.getActiveLsmsForDataSet(ownerKey, dataSetIdentifier)) {
            if (!visitedLsmIds.contains(lsm.getId())) {
                logger.info("  Marking unvisited LSM as desynced: "+lsm.getName()+" (id="+lsm.getId()+")");
                domainDao.updateProperty(ownerKey, LSMImage.class, lsm.getId(), "sageSynced", false);
                lsmsMarkedDesync++;
            }
        }
        
        logger.info("Marking desynchronized samples in dataSet: "+dataSet.getName());
        for(Sample sample : domainDao.getActiveSamplesForDataSet(ownerKey, dataSetIdentifier)) {
            if (!visitedSampleIds.contains(sample.getId())) {
                // Sample was not visited this time around, it should be marked as desynchronized
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
