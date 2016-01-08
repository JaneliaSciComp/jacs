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
public class LSMSampleInitService extends AbstractEntityService {

    private SampleHelper sampleHelper;
    private String owner;

    public void execute() throws Exception {
        sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        sampleHelper.getDataSets();

        owner = processData.getString("TASK_OWNER");
        List<String> lsmNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem("LSM_NAMES")));

        SageDAO sageDao = new SageDAO(logger);

        Map<String, Multimap<String,SlideImage>> slideGroupsByDataset = new HashMap<>();
        for (String lsmName : lsmNames) {
            try {
                SlideImage slideImage = sageDao.getSlideImageByOwnerAndLSMName(lsmName);
                String datasetName  = slideImage.getDatasetName();
                Multimap<String,SlideImage> slideGroups = slideGroupsByDataset.get(datasetName);
                if (slideGroups == null) {
                    slideGroups = LinkedListMultimap.create();
                    slideGroupsByDataset.put(datasetName, slideGroups);
                }
                slideGroups.put(slideImage.getSlideCode(), slideImage);
            } catch (DaoException e) {
                logger.warn("Error while retrieving image for " + lsmName, e);
            }
        }

        Set<String> sampleEntityIds = new LinkedHashSet<>();
        List<Task> sageLoadingTasks = new ArrayList<>();
        for (String datasetName : slideGroupsByDataset.keySet()) {
            prepareSlideImageGroupsForDataset(owner, datasetName, slideGroupsByDataset.get(datasetName), sampleEntityIds, sageLoadingTasks);
        }

        processData.putItem("SAGE_TASK", sageLoadingTasks);
        processData.putItem("SAMPLE_ENTITY_ID", ImmutableList.copyOf(sampleEntityIds));
    }

    private void prepareSlideImageGroupsForDataset(String owner, String datasetName,
                                                   Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
                                                   Collection<String> sampleEntityIds,
                                                   List<Task> targetTasks) {
        List<Entity> datasets;
        try {
            String subjectKey = "user:" + owner;
            datasets = entityBean.getUserEntitiesWithAttributeValueAndTypeName(subjectKey,
                    EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
                    datasetName, EntityConstants.TYPE_DATA_SET);
        } catch (Exception e) {
            logger.error("Error retrieving dataset entities for " + datasetName, e);
            return;
        }
        for (Entity dataset : datasets) {
            sampleHelper.setDataSetNameFilter(dataset.getName());
            String configPath = dataset.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_CONFIG_PATH).getValue();
            String grammarPath = dataset.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_GRAMMAR_PATH).getValue();

            for (String slideCode : slideImagesGroupedBySlideCode.keySet()) {
                try {
                    Collection<SlideImage> slideImages = slideImagesGroupedBySlideCode.get(slideCode);
                    String[] labAndLine = prepareSlideImageGroupsBySlideCode(dataset, slideCode,
                            slideImages,
                            sampleEntityIds);
                    List<String> slideImageNames = ImmutableList.copyOf(Iterables.transform(slideImages, new Function<SlideImage, String>() {
                        @Nullable
                        @Override
                        public String apply(SlideImage slideImage) {
                            return slideImage.getImageName();
                        }
                    }));
                    SageLoaderTask sageLoaderTask = new SageLoaderTask(owner,
                            new ArrayList<Event>(),
                            slideImageNames,
                            labAndLine[1],
                            configPath,
                            grammarPath,
                            labAndLine[0],
                            "true",
                            null);
                    sageLoaderTask.setParentTaskId(task.getObjectId());
                    computeBean.saveOrUpdateTask(sageLoaderTask);
                    targetTasks.add(sageLoaderTask);

                } catch (Exception e) {
                    logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
                }
            }
        }
    }

    private String[] prepareSlideImageGroupsBySlideCode(Entity dataset,
                                                        String slideCode,
                                                        Collection<SlideImage> slideImages,
                                                        Collection<String> sampleEntityIds)
            throws Exception {
        Map<String, SlideImageGroup> tileGroups = new LinkedHashMap<>();

        String line = null;
        String lab = null;
        int tileNum = 0;
        for (SlideImage slideImage : slideImages) {
            String area = slideImage.getArea();
            String tag = slideImage.getTileType();
            if (tag==null) {
                tag = "Tile "+(tileNum+1);
            }
            if (lab == null) {
                lab = slideImage.getLab();
            } else if (!lab.equals(slideImage.getLab())) {
                logger.warn("Lab value for " + slideImage.getImageName() + " - " + slideImage.getLab()
                        + "  does not match " + lab);
            }
            if (line == null) {
                line = slideImage.getLine();
            } else if (!line.equals(slideImage.getLine())) {
                logger.warn("Line value for " + slideImage.getImageName() + " - " + slideImage.getLine()
                        + "  does not match " + line);
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

        Entity sampleEntity = sampleHelper.createOrUpdateSample(null, slideCode, dataset, tileGroupList);
        sampleEntityIds.add(sampleEntity.getId().toString());
        return new String[] {lab, line};
    }
}
