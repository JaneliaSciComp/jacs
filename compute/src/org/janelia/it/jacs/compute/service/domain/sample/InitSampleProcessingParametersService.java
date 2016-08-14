package org.janelia.it.jacs.compute.service.domain.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Extracts stuff about the Sample from the entity model and loads it into
 * simplified objects for use by other services.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSampleProcessingParametersService extends AbstractDomainService {

	protected static final String ARCHIVE_PREFIX = "/archive";

	private Sample sample;
	private ObjectiveSample objectiveSample;
	private FileNode mergeResultNode;

	public void execute() throws Exception {

	    SampleHelperNG sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        this.mergeResultNode = (FileNode) processData.getItem("MERGE_RESULT_FILE_NODE");
        
        contextLogger.info("Running InitSampleProcessingParametersService for sample " + sample.getName());
        
		AnatomicalArea sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
		List<SampleTile> tiles = sampleHelper.getTilesForArea(objectiveSample, sampleArea);
		contextLogger.info("Processing tiles for area " + sampleArea.getName()+": "+tiles);
		
		List<MergedLsmPair> mergedLsmPairs = new ArrayList<MergedLsmPair>();
		boolean archived = populateMergedLsmPairs(tiles, mergedLsmPairs);
		contextLogger.info("Putting " + archived + " in COPY_FROM_ARCHIVE");
		processData.putItem("COPY_FROM_ARCHIVE", archived);
        
	    sampleArea.setMergedLsmPairs(mergedLsmPairs);
		
		if (mergedLsmPairs.isEmpty()) {
			throw new Exception("Sample (id=" + sample.getId() + ") has no tiles");
		}

		List<String> stackFilenames = new ArrayList<String>();

		// Running stitching?
		Boolean runStitchBool = (Boolean) processData.getItem("RUN_STITCH");
		if (runStitchBool != null && runStitchBool.booleanValue()) {
			FileNode stitchResultNode = (FileNode) processData.getItem("STITCH_RESULT_FILE_NODE");
			if (stitchResultNode == null) {
				throw new IllegalArgumentException("STITCH_RESULT_FILE_NODE may not be null");
			}
			File stitchedFile = new File(stitchResultNode.getDirectoryPath(), "stitched-" + sample.getId() + ".v3draw");
			String stitchedFilepath = stitchedFile.getAbsolutePath();
			sampleArea.setStitchedFilepath(stitchedFilepath);
			stackFilenames.add(stitchedFilepath);
		}

		for (MergedLsmPair mergedLsmPair : mergedLsmPairs) {
			stackFilenames.add(mergedLsmPair.getMergedFilepath());
		}

		String sampleProcessingResultsName = "Sample Processing Results";
		if (!StringUtils.isEmpty(sampleArea.getName())) {
			sampleProcessingResultsName += " (" + sampleArea.getName() + ")";
		}
		
		contextLogger.info("Putting " + stackFilenames+ " in STACK_FILENAMES");
		processData.putItem("STACK_FILENAMES", stackFilenames);

		contextLogger.info("Putting " + sampleProcessingResultsName+ " in SAMPLE_PROCESSING_RESULTS_NAME");
		processData.putItem("SAMPLE_PROCESSING_RESULTS_NAME", sampleProcessingResultsName);

        contextLogger.info("Putting updated " + sampleArea + " back into SAMPLE_AREA");
        processData.putItem("SAMPLE_AREA", sampleArea);
	}

	protected boolean populateMergedLsmPairs(List<SampleTile> tileEntities, List<MergedLsmPair> mergedLsmPairs) throws Exception {

		boolean archived = false;

		for (SampleTile sampleTile : tileEntities) {
			
			LSMImage lsm1 = null;
			LSMImage lsm2 = null;
			LSMImage first = null;

            List<LSMImage> lsms = domainDao.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
			for (LSMImage lsmStack : lsms) {
				if (first != null) {
					lsm2 = lsmStack;
				} 
				else {
					lsm1 = lsmStack;
					first = lsmStack;
				}
			}
			
			if (first==null) {
				throw new IllegalStateException("Sample tile has no LSMs: "+sampleTile.getName());
			}

			if (lsm2 != null && "2".equals(first.getNumChannels())) {
				contextLogger.info("Putting 3 channel image first: " + lsm2.getName());
				// Switch the LSMs so that the 3 channel image always comes first
				LSMImage temp = lsm1;
				lsm1 = lsm2;
				lsm2 = temp;
			}

			Long lsmId1 = lsm1==null?null:lsm1.getId();
			Long lsmId2 = lsm2==null?null:lsm2.getId();
			String lsmFilepath1 = lsm1 == null ? null : lsm1.getFilepath();
			String lsmFilepath2 = lsm2 == null ? null : lsm2.getFilepath();
			String lsmRealPath1 = lsmFilepath1 == null ? null : new File(lsmFilepath1).getCanonicalPath();
			String lsmRealPath2 = lsmFilepath2 == null ? null : new File(lsmFilepath2).getCanonicalPath();

			if ((lsmRealPath1!=null && lsmRealPath1.startsWith(ARCHIVE_PREFIX)) || (lsmRealPath2 != null && lsmRealPath2.startsWith(ARCHIVE_PREFIX))) {
				archived = true;
			}

			String mergedFilepath = mergeResultNode == null ? null : new File(mergeResultNode.getDirectoryPath(), "tile-" + domainDao.getNewId() + ".v3draw").getAbsolutePath();
			MergedLsmPair mergedPair = new MergedLsmPair(lsmId1, lsmId2, lsmFilepath1, lsmFilepath2, lsmRealPath1, lsmRealPath2, mergedFilepath, sampleTile.getName());
			mergedLsmPairs.add(mergedPair);
		}

		return archived;
	}
}
