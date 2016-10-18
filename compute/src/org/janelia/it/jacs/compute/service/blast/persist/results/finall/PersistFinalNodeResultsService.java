package org.janelia.it.jacs.compute.service.blast.persist.results.finall;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.BlastServiceUtil;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BlastHit;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.shared.blast.ParsedBlastHSP;
import org.janelia.it.jacs.shared.blast.ParsedBlastResult;
import org.janelia.it.jacs.shared.blast.ParsedBlastResultCollection;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLParser;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This service persists the final blast results to the database or to the file system.
 * It's entirely based on work done by Sean Murphy, Todd Safford, and Kelvin Li
 *
 * @author Sean Murphy
 * @author Todd Safford
 * @author Kelvin Li
 * @author Tareq Nabeel
 */
public class PersistFinalNodeResultsService implements IService {

    private static Logger logger = Logger.getLogger(PersistFinalNodeResultsService.class);

    private ParsedBlastResultCollection allParsedBlastResultCollections;
    private BlastResultFileNode resultFileNode;
    private List<File> blastDestOutputDirs;
    private ComputeDAO computeDAO;

    public PersistFinalNodeResultsService() {
    }

    public void execute(IProcessData processData) throws PersistBlastResultsException {
        try {
            init(processData);
            for (File blastDestOutputDir : blastDestOutputDirs) {
                ParsedBlastResultCollection deserializedCollection = BlastServiceUtil.deserializeParsedBlastResultCollection(blastDestOutputDir);
                if (allParsedBlastResultCollections==null) {//first pass
                    allParsedBlastResultCollections =  deserializedCollection;
                } else {
                    allParsedBlastResultCollections.addAll(deserializedCollection);
                }
            }
            persistBlastResults(allParsedBlastResultCollections, resultFileNode);
            saveBlastHitCount();
        } catch (Exception e) {
            throw new PersistBlastResultsException(e);
        }
    }

    private void saveBlastHitCount() throws DaoException {
        Long blastHitCount = 0L;
        if (allParsedBlastResultCollections!=null) {
            blastHitCount = (long) allParsedBlastResultCollections.size();
        }
        this.resultFileNode.setBlastHitCount(blastHitCount);
        computeDAO.genericSave(resultFileNode);
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException, ClassNotFoundException {
        computeDAO = new ComputeDAO();
        blastDestOutputDirs = (List<File>)processData.getMandatoryItem(BlastProcessDataConstants.BLAST_DEST_OUTPUT_DIR);
        resultFileNode = (BlastResultFileNode) ProcessDataHelper.getResultFileNode(processData);
    }

    private BlastResultNode persistBlastResults(ParsedBlastResultCollection parsedBlastResultCollection,
                                                BlastResultFileNode resultFileNode) throws Exception {
        BlastXMLParser bxmlp = new BlastXMLParser();
        bxmlp.setPbrSet(parsedBlastResultCollection);

        // Get the subject/query ID's translated from accession to entity id through hibernate
        Set<String> idSet = new HashSet<String>();
        idSet.addAll(bxmlp.getQueryIDsSet());
        idSet.addAll(bxmlp.getSubjectIDsSet());
        if (logger.isInfoEnabled()) {
            logger.info("Total idSet Size: " + idSet.size());
        }
        // Create new BlastResultNode
        BlastResultNode bhrdn = new BlastResultNode();

        // Set Blast Related values
        bhrdn.setBlastHitResultSet(transferToBlastHitSet(idSet, bhrdn, bxmlp.getParsedBlastResultCollection()));
        bhrdn.setDeflineMap(bxmlp.getParsedBlastResultCollection().getDeflineMap());

        // Set Node Characteristics
        bhrdn.setVisibility(resultFileNode.getVisibility());
        bhrdn.setTask(resultFileNode.getTask());
        bhrdn.setOwner(resultFileNode.getOwner());
        bhrdn.setName(resultFileNode.getName() + "::BlastResultNode");
        bhrdn.setDescription(resultFileNode.getDescription());
        bhrdn.setDataType(resultFileNode.getDataType());

        logger.debug("Before save or update...\n");
        if (logger.isInfoEnabled()) {
            logger.info("I'm going to persist: a BlastResultNode containing a defline map with " + bhrdn.getDeflineMap().size() + " entries and a blast hit set with " + bhrdn.getBlastHitResultSet().size() + " entries.\n");
        }
        // Calling fresh for the Session Factory in hopes of getting a new "currentSession"
        //computeDAO.saveOrUpdateBlastHitResultNode(bhrdn);

        computeDAO.saveOrUpdate(bhrdn);
        logger.debug("After save or update...\n");
        return bhrdn;
    }

    private Set transferToBlastHitSet(Set<String> idSet, BlastResultNode bhrdn, ParsedBlastResultCollection pbrCollection) {
        Map<String, BaseSequenceEntity> bseIdMap = new HashMap<String, BaseSequenceEntity>();//computeDAO.getEntityIdsByAccessionSet(idSet);

        Set<BlastHit> bhSet = new HashSet<BlastHit>();
        ArrayList<ParsedBlastResult> tmpPBRList = pbrCollection.getParsedBlastResults();

//        if (logger.isInfoEnabled()) {
//            logger.info("Accession to Entity mappings available: " + bseIdMap.size());
//        }
        int rank = 0;
        for (ParsedBlastResult pbr : tmpPBRList) {
            for (ParsedBlastHSP parsedBlastHSP : pbr.getHspList()) {
                BlastHit bh = new BlastHit();

                bh.setRank(rank++);
                bh.setProgramUsed(pbr.programUsed);
                bh.setBlastVersion(pbr.blastVersion);
                bh.setBitScore(parsedBlastHSP.getBitScore());
                bh.setHspScore(parsedBlastHSP.getHspScore());
                bh.setExpectScore(parsedBlastHSP.getExpectScore());
                bh.setComment(pbr.comment);
                bh.setLengthAlignment(parsedBlastHSP.getLengthAlignment());
                bh.setEntropy(parsedBlastHSP.getEntropy());
                bh.setNumberIdentical(parsedBlastHSP.getNumberIdentical());
                bh.setNumberSimilar(parsedBlastHSP.getNumberSimilar());
                bh.setMidline(parsedBlastHSP.getMidline());

                // Subject specific values
                bh.setSubjectAcc(pbr.subjectId);
                bh.setSubjectLength(pbr.subjectLength);
                bh.setSubjectBegin(parsedBlastHSP.getSubjectBegin());
                bh.setSubjectEnd(parsedBlastHSP.getSubjectEnd());
                bh.setSubjectOrientation(parsedBlastHSP.getSubjectOrientation());
                bh.setSubjectGaps(parsedBlastHSP.getSubjectGaps());
                bh.setSubjectGapRuns(parsedBlastHSP.getSubjectGapRuns());
                bh.setSubjectStops(parsedBlastHSP.getSubjectStops());
                bh.setSubjectNumberUnalignable(parsedBlastHSP.getSubjectNumberUnalignable());
                bh.setSubjectFrame(parsedBlastHSP.getSubjectFrame());
                bh.setSubjectAlignString(parsedBlastHSP.getSubjectAlignString());

                // Query specific values
                bh.setQueryNodeId(((BlastTask) resultFileNode.getTask()).getQueryId());
                bh.setQueryAcc(pbr.queryId);
                bh.setQueryLength(pbr.queryLength);
                bh.setQueryBegin(parsedBlastHSP.getQueryBegin());
                bh.setQueryEnd(parsedBlastHSP.getQueryEnd());
                bh.setQueryOrientation(parsedBlastHSP.getQueryOrientation());
                bh.setQueryGaps(parsedBlastHSP.getQueryGaps());
                bh.setQueryGapRuns(parsedBlastHSP.getQueryGapRuns());
                bh.setQueryStops(parsedBlastHSP.getQueryStops());
                bh.setQueryNumberUnalignable(parsedBlastHSP.getQueryNumberUnalignable());
                bh.setQueryFrame(parsedBlastHSP.getQueryFrame());
                bh.setQueryAlignString(parsedBlastHSP.getQueryAlignString());

                // Convert query/subject Id's to base sequence entities
//                BaseSequenceEntity qBse = bseIdMap.get(pbr.queryId);
//                if (qBse != null) {
//                    bh.setQueryEntity(qBse);
//                }
//                BaseSequenceEntity sBse = bseIdMap.get(pbr.subjectId);
//                bh.setSubjectEntity(sBse);
//                if (sBse == null) {
//                    logger.error("Failing Blast: Could not find entity for subjectId:" + pbr.subjectId);
//                    throw new IllegalArgumentException("Could not find subject entity: " + pbr.subjectId);
//                }
                bh.setResultNode(bhrdn);

                // Save it!
                bhSet.add(bh);
            }
        }
        return bhSet;
    }
}
