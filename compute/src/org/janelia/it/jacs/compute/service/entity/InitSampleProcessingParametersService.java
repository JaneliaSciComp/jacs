package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Extracts stuff about the Sample from the entity model and loads it into
 * simplified objects for use by other services.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleProcessingParametersService extends
		AbstractEntityService {

	protected static final String ARCHIVE_PREFIX = "/archive";

	private FileNode mergeResultNode;

	public void execute() throws Exception {

		mergeResultNode = (FileNode) processData
				.getItem("MERGE_RESULT_FILE_NODE");
		if (mergeResultNode == null) {
			throw new IllegalArgumentException(
					"MERGE_RESULT_FILE_NODE may not be null");
		}

		String sampleEntityId = (String) data
				.getRequiredItemAsString("SAMPLE_ENTITY_ID");
		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
		if (sampleEntity == null) {
			throw new IllegalArgumentException(
					"Sample entity not found with id=" + sampleEntityId);
		}

		AnatomicalArea sampleArea = (AnatomicalArea) processData
				.getItem("SAMPLE_AREA");

		logger.info("Running InitSampleProcessingParametersService for sampleId="
				+ sampleEntityId);

		List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();

		List<Entity> tileEntities = null;
		if (sampleArea != null) {
			logger.info("Processing tiles for area: " + sampleArea.getName());
			tileEntities = entityBean.getEntitiesById(sampleArea.getTileIds());
		} 
		else {
			populateChildren(sampleEntity);
			Entity supportingFiles = EntityUtils.getSupportingData(sampleEntity);
			if (supportingFiles == null) {
				throw new IllegalStateException("Sample does not have Supporting Files child: " + sampleEntityId);
			}
			supportingFiles = entityBean.getEntityTree(supportingFiles.getId());
			tileEntities = EntityUtils.getDescendantsOfType(supportingFiles, EntityConstants.TYPE_IMAGE_TILE, true);
		}

		boolean archived = populateMergedLsmPairs(tileEntities, mergedLsmPairs);
		logger.info("Putting " + archived + " in COPY_FROM_ARCHIVE");
		processData.putItem("COPY_FROM_ARCHIVE", archived);

		if (mergedLsmPairs.isEmpty()) {
			throw new Exception("Sample (id=" + sampleEntityId
					+ ") has no tiles");
		}

		List<String> stackFilenames = new ArrayList<String>();

		// Running stitching?
		Boolean runStitchBool = (Boolean) processData.getItem("RUN_STITCH");

		if (runStitchBool != null && runStitchBool.booleanValue()) {
			FileNode stitchResultNode = (FileNode) processData
					.getItem("STITCH_RESULT_FILE_NODE");
			if (stitchResultNode == null) {
				throw new IllegalArgumentException(
						"STITCH_RESULT_FILE_NODE may not be null");
			}
			File stitchedFile = new File(stitchResultNode.getDirectoryPath(),
					"stitched-" + sampleEntity.getId() + ".v3draw");
			logger.info("Putting " + stitchedFile.getAbsolutePath()
					+ " in STITCHED_FILENAME");
			processData.putItem("STITCHED_FILENAME",
					stitchedFile.getAbsolutePath());
			stackFilenames.add(stitchedFile.getAbsolutePath());
		}

		for (MergedLsmPair mergedLsmPair : mergedLsmPairs) {
			stackFilenames.add(mergedLsmPair.getMergedFilepath());
		}

		String sampleProcessingResultsName = "Sample Processing Results";
		if (sampleArea != null && !StringUtils.isEmpty(sampleArea.getName())) {
			sampleProcessingResultsName += " (" + sampleArea.getName() + ")";
		}

		logger.info("Putting " + mergedLsmPairs.size()
				+ " items in BULK_MERGE_PARAMETERS");
		processData.putItem("BULK_MERGE_PARAMETERS", mergedLsmPairs);

		logger.info("Putting " + stackFilenames.size()
				+ " items in STACK_FILENAMES");
		processData.putItem("STACK_FILENAMES", stackFilenames);

		logger.info("Putting " + sampleProcessingResultsName
				+ " in SAMPLE_PROCESSING_RESULTS_NAME");
		processData.putItem("SAMPLE_PROCESSING_RESULTS_NAME",
				sampleProcessingResultsName);
	}

	private boolean populateMergedLsmPairs(List<Entity> tileEntities, List<MergedLsmPair> mergedLsmPairs) throws Exception {

		boolean archived = false;

		for (Entity tileEntity : tileEntities) {
			
			populateChildren(tileEntity);
			
			String lsmFilepath1 = null;
			String lsmFilepath2 = null;

			Entity first = null;
			for (EntityData ed : tileEntity.getOrderedEntityData()) {
				Entity lsmStack = ed.getChildEntity();
				if (lsmStack != null && lsmStack.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
					String filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
					if (first != null) {
						lsmFilepath2 = filepath;
					} 
					else {
						lsmFilepath1 = filepath;
						first = lsmStack;
					}
				}
			}

			if ("2".equals(first.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS)) && lsmFilepath2 != null) {
				logger.info("Putting 3 channel image first: " + lsmFilepath2);
				// Switch the LSMs so that the 3 channel image always comes first
				String temp = lsmFilepath1;
				lsmFilepath1 = lsmFilepath2;
				lsmFilepath2 = temp;
			}

			File mergedFile = null;

			File lsmFile1 = new File(lsmFilepath1);
			File lsmFile2 = null;
			if (lsmFilepath2 != null) {
				lsmFile2 = new File(lsmFilepath2);
				mergedFile = new File(mergeResultNode.getDirectoryPath(), "tile-" + tileEntity.getId() + ".v3draw");
			} 
			else {
				// lsmFilepath2 may be null
				mergedFile = new File(mergeResultNode.getDirectoryPath(), "tile-" + tileEntity.getId() + ".v3draw");
			}

			String lsmRealPath1 = lsmFile1.getCanonicalPath();
			String lsmRealPath2 = lsmFile2 == null ? null : lsmFile2.getCanonicalPath();

			if (lsmRealPath1.startsWith(ARCHIVE_PREFIX) || (lsmRealPath2 != null && lsmRealPath2.startsWith(ARCHIVE_PREFIX))) {
				archived = true;
			}

			mergedLsmPairs.add(new MergedLsmPair(lsmFilepath1, lsmFilepath2, lsmRealPath1, lsmRealPath2, mergedFile.getAbsolutePath(), tileEntity.getName()));
		}

		return archived;
	}
}
