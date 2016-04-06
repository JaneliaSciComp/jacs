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

    private String datasetName;

    public void execute() throws Exception {
        datasetName = processData.getString("DATASET_NAME");
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

        List<Task> sageLoadingTasks = new ArrayList<>();
        prepareSlideImageGroupsForCurrentDataset(slideGroups, sageLoadingTasks);

        processData.putItem("SAGE_TASK", sageLoadingTasks);
    }

    private void prepareSlideImageGroupsForCurrentDataset(Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
                                                          List<Task> targetTasks) {
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
            String owner = extractOwner(dataset.getOwnerKey());
            String configPath = dataset.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_CONFIG_PATH).getValue();
            String grammarPath = dataset.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_SAGE_GRAMMAR_PATH).getValue();

            for (String slideCode : slideImagesGroupedBySlideCode.keySet()) {
                try {
                    Collection<SlideImage> slideImages = slideImagesGroupedBySlideCode.get(slideCode);
                    String[] labAndLine = prepareSlideImageGroupsBySlideCode(slideImages);
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

    private String extractOwner(String ownerKey) {
        int ownerSeparatorIndex = ownerKey.indexOf(':');
        String owner;
        if (ownerSeparatorIndex != -1) {
            owner = ownerKey.substring(ownerSeparatorIndex);
        } else {
            owner = ownerKey;
        }
        return owner;
    }

    private String[] prepareSlideImageGroupsBySlideCode(Collection<SlideImage> slideImages) {
        String line = null;
        String lab = null;
        for (SlideImage slideImage : slideImages) {
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
        }
        return new String[] {lab, line};
    }
}
