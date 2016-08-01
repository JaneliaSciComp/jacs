package org.janelia.it.jacs.compute.service.entity.sample;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;

import java.util.*;

/**
 * Discovers the SAGE samples associated with the given LSM and processes the corresponding samples.
 */
public class LSMSampleDiscoveryService extends AbstractEntityService {

    private String datasetName;

    public void execute() throws Exception {
        datasetName = processData.getString("DATASET_NAME");
        String ownerKey = processData.getString("OWNER");

        // retrieve the dataset
        DataSet dataset = null;
        try {
            DomainDAL dal = DomainDAL.getInstance();
            // ASSUME-FOR-NOW: the owner key is the key of user owning the data, not the pipeline.
            dataset = dal.getDataSetByIdentifier(ownerKey, datasetName);
            //processData.
            //                    entityBean.getUserEntitiesWithAttributeValueAndTypeName(null,
            //                    EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
            //                    datasetName, EntityConstants.TYPE_DATA_SET);
        } catch (Exception e) {
            logger.error("Error retrieving datasets for " + datasetName, e);
            return;
        }

        // Populate the sample helper.
        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, dataset.getOwnerKey(), logger);
        sampleHelper.setDataSetNameFilter(datasetName);
        sampleHelper.getDataSets();

        // Get all LSM names from the process configuration data.
        List<String> lsmNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem("LSM_NAMES")));

        // Build a mapping from slide code to all slide images for that code.
        SageDAO sageDao = new SageDAO(logger);
        Multimap<String, SlideImage> slideGroups = LinkedListMultimap.create();
        for (String lsmName : lsmNames) {
            try {
                SlideImage slideImage = sageDao.getSlideImageByDatasetAndLSMName(datasetName, lsmName);
                if (slideImage.getSlideCode() != null)
                    slideGroups.put(slideImage.getSlideCode(), slideImage);
                else
                    throw new IllegalArgumentException("Invalid slide code value - slideCode should not be null");
            } catch (DaoException e) {
                logger.warn("Error while retrieving image for " + lsmName, e);
            }
        }

        // Process data from all the slide image codes.  Collect affected samples' ids for downstream processing.
        Set<String> sampleIds = new LinkedHashSet<>();
        prepareSlideImageGroupsForCurrentDataset(sampleHelper, dataset, slideGroups, sampleIds);
        sampleHelper.annexSamples();

        logger.info("Setting the sample ids output: " + sampleIds);
        processData.putItem("SAMPLE_ID", ImmutableList.copyOf(sampleIds));
    }

    private void prepareSlideImageGroupsForCurrentDataset(SampleHelperNG sampleHelper,
                                                          DataSet dataset,
                                                          Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
                                                          Collection<String> sampleIds) {
        for (String slideCode : slideImagesGroupedBySlideCode.keySet()) {
            try {
                Collection<SlideImage> slideImages = slideImagesGroupedBySlideCode.get(slideCode);
                Sample sampleEntity = prepareSlideImageGroupsBySlideCode(sampleHelper, dataset, slideCode, slideImages);
                sampleIds.add(sampleEntity.getId().toString());
            } catch (Exception e) {
                logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
            }
        }
    }

    private Sample prepareSlideImageGroupsBySlideCode(SampleHelperNG sampleHelper,
                                                      DataSet dataset,
                                                      String slideCode,
                                                      Collection<SlideImage> slideImages)
            throws Exception {
        logger.info("Group images for slideCode " + slideCode);
        Map<String, SlideImageGroup> tileGroups = new LinkedHashMap<>();

        int tileNum = 0;
        List<LSMImage> allImages = new ArrayList<>();
        for (SlideImage slideImage : slideImages) {
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
            LSMImage lsmImage = sampleHelper.createOrUpdateLSM(slideImage);
            tileGroup.addFile(lsmImage);
            allImages.add(lsmImage);
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

        //String slideCode, DataSet dataSet, Collection<LSMImage> lsms
        return sampleHelper.createOrUpdateSample(slideCode, dataset, allImages);
    }
}
