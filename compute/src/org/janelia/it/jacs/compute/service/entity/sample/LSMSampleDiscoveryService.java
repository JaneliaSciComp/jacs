package org.janelia.it.jacs.compute.service.entity.sample;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Discovers the SAGE samples associated with the given LSM and processes the corresponding samples.
 */
public class LSMSampleDiscoveryService extends AbstractEntityService {

    private String datasetName;

    public void execute() throws Exception {
        datasetName = processData.getString("DATASET_NAME");

        // retrieve the dataset
        List<Entity> datasets;
        try {
            datasets = entityBean.getUserEntitiesWithAttributeValueAndTypeName(null,
                    EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
                    datasetName, EntityConstants.TYPE_DATA_SET);
        } catch (Exception e) {
            logger.error("Error retrieving dataset entities for " + datasetName, e);
            return;
        }
        if (datasets.size() > 1) {
            logger.warn("More than one dataset found (" + datasets.size() + ") - only the first one will be considered: ");
        }
        Entity dataset = datasets.get(0);
        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, dataset.getOwnerKey(), logger);
        sampleHelper.getDataSets();
        sampleHelper.setDataSetNameFilter(datasetName);

        List<String> lsmNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem("LSM_NAMES")));

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

        Set<String> sampleIds = new LinkedHashSet<>();
        prepareSlideImageGroupsForCurrentDataset(sampleHelper, dataset, slideGroups, sampleIds);
        sampleHelper.annexSamples();

        logger.info("Setting the sample ids output: " + sampleIds);
        processData.putItem("SAMPLE_ID", ImmutableList.copyOf(sampleIds));
    }

    private void prepareSlideImageGroupsForCurrentDataset(SampleHelper sampleHelper,
                                                          Entity dataset,
                                                          Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
                                                          Collection<String> sampleIds) {
        for (String slideCode : slideImagesGroupedBySlideCode.keySet()) {
            try {
                Collection<SlideImage> slideImages = slideImagesGroupedBySlideCode.get(slideCode);
                Entity sampleEntity = prepareSlideImageGroupsBySlideCode(sampleHelper, dataset, slideCode, slideImages);
                sampleIds.add(sampleEntity.getId().toString());
            } catch (Exception e) {
                logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
            }
        }
    }

    private Entity prepareSlideImageGroupsBySlideCode(SampleHelper sampleHelper,
                                                      Entity dataset,
                                                      String slideCode,
                                                      Collection<SlideImage> slideImages)
            throws Exception {
        logger.info("Group images for slideCode " + slideCode);
        Map<String, SlideImageGroup> tileGroups = new LinkedHashMap<>();

        int tileNum = 0;
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
            tileGroup.addFile(slideImage);
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

        return sampleHelper.createOrUpdateSample(null, slideCode, dataset, tileGroupList);
    }
}
