package org.janelia.it.jacs.compute.service.domain.discovery;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.util.ResultSetIterator;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;

/**
 * Extraction of code to be reused by multiple "Discovery Service" classes.  Encapsulated here
 * for the sake of reuse.
 *
 * Created by fosterl on 8/10/2016.
 */
public class SageDiscoverServiceHelper {

    private Logger logger = Logger.getLogger(SageDiscoverServiceHelper.class);
    private Map<String, Map<String, Object>> lineMap = new HashMap<>();
    private Set<Long> visitedLsmIds = new HashSet<>();
    private Set<Long> visitedSampleIds = new HashSet<>();
    private SageDAO sageDAO;
    private SampleHelperNG sampleHelper;

    private int sageRowsProcessed = 0;

    public SageDiscoverServiceHelper(SampleHelperNG sampleHelper) throws Exception {
        this.sageDAO = new SageDAO(logger);
        this.sampleHelper = sampleHelper;
        buildLinePropertyMap();
    }

    public int getSageRowsProcessed() {
        return sageRowsProcessed;
    }

    public Set<Long> getVisitedLsmIds() {
        return visitedLsmIds;
    }

    public Set<Long> getVisitedSampleIds() {
        return visitedSampleIds;
    }

    /**
     * Provide either imageFamily or dataSetIdentifier.
     */
    public void processSageDataSet(DataSet dataSet) throws Exception {
        processSageDataSet(dataSet, null);
    }

    /**
     * Provide either imageFamily or dataSetIdentifier.
     */
    public void processSageDataSet(DataSet dataSet, Collection<String> includedLsmNames) throws Exception {

        boolean includeAllLsms = includedLsmNames == null  ||  includedLsmNames.isEmpty();
        Multimap<String,LSMImage> slideGroups = LinkedListMultimap.create();

        String dataSetIdentifier = dataSet.getIdentifier();
        logger.info("Querying SAGE for data set: "+dataSetIdentifier);

        ResultSetIterator iterator = null;
        try {
            iterator = sageDAO.getAllImagePropertiesByDataSet(dataSetIdentifier);

            while (iterator.hasNext()) {
                Map<String,Object> row = iterator.next();
                String lsmName = (String)row.get("image_query_name");

                if (includeAllLsms  ||  includedLsmNames.contains(lsmName)) {
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

        // Now process all the slides
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

}
