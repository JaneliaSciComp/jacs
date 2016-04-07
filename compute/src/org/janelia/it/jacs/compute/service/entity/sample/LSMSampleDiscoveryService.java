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

    private SampleHelper sampleHelper;
    private String datasetName;

    public void execute() throws Exception {
        datasetName = processData.getString("DATASET_NAME");

        sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
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
                slideGroups.put(slideImage.getSlideCode(), slideImage);
            } catch (DaoException e) {
                logger.warn("Error while retrieving image for " + lsmName, e);
            }
        }

        Set<String> sampleDatasetWithEntityIds = new LinkedHashSet<>();
        prepareSlideImageGroupsForCurrentDataset(slideGroups, sampleDatasetWithEntityIds);

        processData.putItem("SAMPLE_DATASET_ID_WITH_ENTITY_ID", ImmutableList.copyOf(sampleDatasetWithEntityIds));
    }

    private void prepareSlideImageGroupsForCurrentDataset(Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
                                                          Collection<String> sampleDatasetWithEntityIds) {
        List<Entity> datasets;
        try {
            datasets = entityBean.getUserEntitiesWithAttributeValueAndTypeName(null,
                    EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
                    datasetName, EntityConstants.TYPE_DATA_SET);
        } catch (Exception e) {
            logger.error("Error retrieving dataset entities for " + datasetName, e);
            return;
        }
        for (Entity dataset : datasets) {
            for (String slideCode : slideImagesGroupedBySlideCode.keySet()) {
                try {
                    Collection<SlideImage> slideImages = slideImagesGroupedBySlideCode.get(slideCode);
                    prepareSlideImageGroupsBySlideCode(dataset, slideCode,
                            slideImages,
                            sampleDatasetWithEntityIds);
                } catch (Exception e) {
                    logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
                }
            }
        }
    }

    private Entity prepareSlideImageGroupsBySlideCode(Entity dataset,
                                                      String slideCode,
                                                      Collection<SlideImage> slideImages,
                                                      Collection<String> sampleDatasetWithEntityIds)
            throws Exception {
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
