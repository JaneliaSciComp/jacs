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
public class InitSampleProcessingParametersService extends AbstractEntityService {

	protected static final String ARCHIVE_PREFIX = "/archive";

	private FileNode mergeResultNode;

	public void execute() throws Exception {

		mergeResultNode = (FileNode) processData.getItem("MERGE_RESULT_FILE_NODE");

		String sampleEntityId = (String) data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
		contextLogger.info("Running InitSampleProcessingParametersService for sample " + sampleEntityId);
		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
		if (sampleEntity == null) {
			throw new IllegalArgumentException("Sample entity not found with id=" + sampleEntityId);
		}

		AnatomicalArea sampleArea = (AnatomicalArea) processData.getItem("SAMPLE_AREA");

		List<Entity> tileEntities = null;
		if (sampleArea != null) {
			contextLogger.info("Processing tiles for area: " + sampleArea.getName());
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

		List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();
		boolean archived = populateMergedLsmPairs(tileEntities, mergedLsmPairs);
		contextLogger.info("Putting " + archived + " in COPY_FROM_ARCHIVE");
		processData.putItem("COPY_FROM_ARCHIVE", archived);


        if (sampleArea==null) {
            // Create dummy area to hold the LSM information
            sampleArea = new AnatomicalArea("");
        }
        
	    sampleArea.setMergedLsmPairs(mergedLsmPairs);
		
		if (mergedLsmPairs.isEmpty()) {
			throw new Exception("Sample (id=" + sampleEntityId + ") has no tiles");
		}

		List<String> stackFilenames = new ArrayList<String>();

		// Running stitching?
		Boolean runStitchBool = (Boolean) processData.getItem("RUN_STITCH");
		if (runStitchBool != null && runStitchBool.booleanValue()) {
			FileNode stitchResultNode = (FileNode) processData.getItem("STITCH_RESULT_FILE_NODE");
			if (stitchResultNode == null) {
				throw new IllegalArgumentException("STITCH_RESULT_FILE_NODE may not be null");
			}
			File stitchedFile = new File(stitchResultNode.getDirectoryPath(), "stitched-" + sampleEntity.getId() + ".v3draw");
			String stitchedFilepath = stitchedFile.getAbsolutePath();
			sampleArea.setStitchedFilename(stitchedFilepath);
			stackFilenames.add(stitchedFilepath);
		}

		for (MergedLsmPair mergedLsmPair : mergedLsmPairs) {
			stackFilenames.add(mergedLsmPair.getMergedFilepath());
		}

		String sampleProcessingResultsName = "Sample Processing Results";
		if (!StringUtils.isEmpty(sampleArea.getName())) {
			sampleProcessingResultsName += " (" + sampleArea.getName() + ")";
		}
		
		contextLogger.info("Putting " + stackFilenames.size()+ " items in STACK_FILENAMES");
		processData.putItem("STACK_FILENAMES", stackFilenames);

		contextLogger.info("Putting " + sampleProcessingResultsName+ " in SAMPLE_PROCESSING_RESULTS_NAME");
		processData.putItem("SAMPLE_PROCESSING_RESULTS_NAME", sampleProcessingResultsName);

        contextLogger.info("Putting updated " + sampleArea + " back into SAMPLE_AREA");
        processData.putItem("SAMPLE_AREA", sampleArea);
	}

	private boolean populateMergedLsmPairs(List<Entity> tileEntities, List<MergedLsmPair> mergedLsmPairs) throws Exception {

		boolean archived = false;

		for (Entity tileEntity : tileEntities) {
			
			populateChildren(tileEntity);
			
			Entity lsm1 = null;
			Entity lsm2 = null;

			Entity first = null;
			for (EntityData ed : tileEntity.getOrderedEntityData()) {
				Entity lsmStack = ed.getChildEntity();
				if (lsmStack != null && lsmStack.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
					if (first != null) {
						lsm2 = lsmStack;
					} 
					else {
						lsm1 = lsmStack;
						first = lsmStack;
					}
				}
			}

			if (lsm2 != null && "2".equals(first.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS))) {
				contextLogger.info("Putting 3 channel image first: " + lsm2.getName());
				// Switch the LSMs so that the 3 channel image always comes first
				Entity temp = lsm1;
				lsm1 = lsm2;
				lsm2 = temp;
			}

			Long lsmId1 = lsm1==null?null:lsm1.getId();
			Long lsmId2 = lsm2==null?null:lsm2.getId();
			String lsmFilepath1 = lsm1 == null ? null : EntityUtils.getFilePath(lsm1);
			String lsmFilepath2 = lsm2 == null ? null : EntityUtils.getFilePath(lsm2);
			String lsmRealPath1 = lsmFilepath1 == null ? null : new File(lsmFilepath1).getCanonicalPath();
			String lsmRealPath2 = lsmFilepath2 == null ? null : new File(lsmFilepath2).getCanonicalPath();

			if (lsmRealPath1.startsWith(ARCHIVE_PREFIX) || (lsmRealPath2 != null && lsmRealPath2.startsWith(ARCHIVE_PREFIX))) {
				archived = true;
			}

			String mergedFilepath = mergeResultNode == null ? null : new File(mergeResultNode.getDirectoryPath(), "tile-" + tileEntity.getId() + ".v3draw").getAbsolutePath();
			MergedLsmPair mergedPair = new MergedLsmPair(lsmId1, lsmId2, lsmFilepath1, lsmFilepath2, lsmRealPath1, lsmRealPath2, mergedFilepath, tileEntity.getName());
			mergedLsmPairs.add(mergedPair);
		}

		return archived;
	}
}
