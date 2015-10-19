
package org.janelia.it.jacs.compute.service.export.processor;

import org.janelia.it.jacs.compute.access.FeatureDAO;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.export.model.BlastHitResult;
import org.janelia.it.jacs.compute.service.export.model.BlastResultCsvWriter;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BlastHit;
import org.janelia.it.jacs.model.tasks.export.BlastResultExportTask;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 4:20:01 PM
 */
public class MatchingResultsExportProcessor extends ExportProcessor {
    FeatureDAO featureDAO;

    public MatchingResultsExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        super(exportTask, exportFileNode);
        featureDAO = new FeatureDAO(_logger);
    }

    private class IndexedFastaSequence {
        RandomAccessFile raf;
        long offset;
        int length;

        public IndexedFastaSequence(RandomAccessFile raf, long offset, int length) {
            this.raf = raf;
            this.offset = offset;
            this.length = length;
        }

        public RandomAccessFile getRaf() {
            return raf;
        }

        public void setRaf(RandomAccessFile raf) {
            this.raf = raf;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        String getSequence() throws IOException {
            if (offset < 0) {
                return null;
            }
            else {
                raf.seek(offset);
                byte[] buffer = new byte[length];
                raf.readFully(buffer, 0, length);
                String seqWithLineBreaks = new String(buffer);
                return seqWithLineBreaks.replaceAll("\\s+", "");
            }
        }
    }

    public void execute() throws Exception {
        _logger.info("MatchingResultsExportProcessor starting execute()");
        ParameterVO blastTaskIdParameterVO = exportTask.getParameterVO(BlastResultExportTask.BLAST_TASK_ID);
        if (blastTaskIdParameterVO == null) {
            throw new Exception("blastTaskIdParameterVO is null for exportTask=" + exportTask.getObjectId());
        }
        Long blastTaskId = new Long(blastTaskIdParameterVO.getStringValue());

        ParameterVO reqSeqParameterVO = exportTask.getParameterVO(BlastResultExportTask.EXPORT_SEQUENCE_TYPE);
        if (reqSeqParameterVO == null) {
            throw new Exception("ParameterVO of key=" + BlastResultExportTask.EXPORT_SEQUENCE_TYPE +
                    " not found for task=" + exportTask.getObjectId());
        }
        Set<Long> accessionSet = convertStringListToLongs(exportTask.getAccessionList());
        SortArgument[] sortArgumentArr = convertStringListToSortArgumentArr(exportTask.getExportAttributeList());
        _logger.info("MatchingResultsExportProcessor getting blast results by taskId=" + blastTaskId);
        List<Object[]> blastHitArr = featureDAO.getBlastResultsByTaskOrIDs(blastTaskId,
                accessionSet, 0 /*start*/, 0 /*numrows*/, true /*include hsp ranks*/, sortArgumentArr);
        List<BlastHitResult> blastHitResults = new ArrayList<BlastHitResult>();
        _logger.info("MatchingResultsExportProcessor converting data for export");
        for (Object o : blastHitArr) {
            blastHitResults.add(convertDataForExport(o));
        }

        // Fasta case
        if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_FASTA)) {
            if (reqSeqParameterVO.getStringValue().equals(BlastResultExportTask.SEQUENCES_SUBJECT)) {
                for (BlastHitResult bhr : blastHitResults) {
                    try {
                        List<String> list = new ArrayList<String>();
                        list.add(bhr.getSubjectDefline());
                        list.add(bhr.getBlastHit().getSubjectEntity().getBioSequence().getSequence());
                        exportWriter.writeItem(list);
                    }
                    catch (Exception e) {
                        _logger.error("Error retrieving subject sequence for blast job=" + blastTaskId);
                    }
                }
            }
            else if (reqSeqParameterVO.getStringValue().equals(BlastResultExportTask.SEQUENCES_QUERY)) {
                HashSet<String> queryDeflineUniqueFilter = new HashSet<String>();
                Map<Long, RandomAccessFile> queryRafMap = new HashMap<Long, RandomAccessFile>();
                Map<Long, Map<String, IndexedFastaSequence>> querySequenceMap = new HashMap<Long, Map<String, IndexedFastaSequence>>();
                int sequenceCount = 0;
                for (BlastHitResult bhr : blastHitResults) {
                    String queryKey = getDeflineKeyFromString(bhr.getQueryDefline());
                    if (!queryDeflineUniqueFilter.contains(queryKey)) {
                        try {
                            Map<String, IndexedFastaSequence> deflineSequenceMap;
                            if (!querySequenceMap.containsKey(bhr.getBlastHit().getQueryNodeId())) {
                                _logger.info("Setting up deflineSequenceMap for queryId=" + bhr.getBlastHit().getQueryNodeId());
                                deflineSequenceMap = getDeflineSequenceMapByNodeId(bhr.getBlastHit().getQueryNodeId(), queryRafMap);
                                querySequenceMap.put(bhr.getBlastHit().getQueryNodeId(), deflineSequenceMap);
                            }
                            else {
                                deflineSequenceMap = querySequenceMap.get(bhr.getBlastHit().getQueryNodeId());
                            }
                            _logger.info("retrieving sequence for queryId=" + queryKey);
                            String sequence = deflineSequenceMap.get(queryKey).getSequence();
                            if (sequence == null) {
                                FastaFileNode queryNode = (FastaFileNode) featureDAO.getNodeById(bhr.getBlastHit().getQueryNodeId());
                                File queryFile = new File(queryNode.getFastaFilePath());
                                String errorMsg = "Could not find sequence entry for defline=\"" + queryKey + "\" from query fasta file=" + queryFile.getAbsolutePath();
                                _logger.error(errorMsg);
                                throw new Exception(errorMsg);
                            }
                            List<String> list = new ArrayList<String>();
                            list.add(bhr.getQueryDefline());
                            list.add(sequence);
                            exportWriter.writeItem(list);
                            queryDeflineUniqueFilter.add(queryKey);
                            sequenceCount++;
                        }
                        catch (Exception e) {
                            _logger.error("Error retrieving query sequence for blast job=" + blastTaskId);
                        }
                    }
                }
                _logger.info("Retrieved " + sequenceCount + " query sequences");
                for (RandomAccessFile raf : queryRafMap.values()) {
                    try {
                        raf.close();
                    }
                    catch (Exception ex) {
                        _logger.error(ex.getMessage(), ex);
                    }
                }
                _logger.info("Closed all RandomAccessFiles");
            }
            else {
                throw new Exception("Do not recognize request for sequence type=" + reqSeqParameterVO.getStringValue());
            }

        }
        // CSV or Excel case
        else if (exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_CSV) ||
                exportTask.getExportFormatType().equals(ExportWriterConstants.EXPORT_TYPE_EXCEL)) {
            BlastResultCsvWriter blastResultCsvWriter = new BlastResultCsvWriter(exportWriter, blastHitResults);
            blastResultCsvWriter.write();

        }
        // Do not recognize case
        else {
            throw new Exception("Do not recognize export format type=" + exportTask.getExportFormatType());
        }
    }

    protected String getDeflineKeyFromString(String deflineString) {
        String[] deflineArr = deflineString.split("\\s+");
        String deflineKey = deflineArr[0];
        if (!deflineKey.startsWith(">")) {
            deflineKey = ">" + deflineKey;
        }
        return deflineKey;
    }

    protected BlastHitResult convertDataForExport(Object o) {
        Object[] retrievedBHResults = (Object[]) o;
        BlastHitResult blastHitResult = new BlastHitResult();
        blastHitResult.setBlastHit((BlastHit) retrievedBHResults[0]);
        blastHitResult.setQueryDefline((String) retrievedBHResults[1]);
        blastHitResult.setSubjectDefline((String) retrievedBHResults[2]);
        if (retrievedBHResults[3] instanceof Integer) {
            blastHitResult.setHspRank((Integer) retrievedBHResults[3]);
        }
        else if (retrievedBHResults[3] instanceof Long) {
            blastHitResult.setHspRank(((Long) retrievedBHResults[3]).intValue());
        }
        if (retrievedBHResults[4] instanceof Integer) {
            blastHitResult.setNhsps((Integer) retrievedBHResults[4]);
        }
        else if (retrievedBHResults[3] instanceof Long) {
            blastHitResult.setNhsps(((Long) retrievedBHResults[4]).intValue());
        }
        return blastHitResult;
    }

    public String getProcessorType() {
        return "Matching Results";
    }

    protected List<SortArgument> getDataHeaders() {
        return null;
    }

    private Set<Long> convertStringListToLongs(List<String> stringList) {
        if (stringList == null || stringList.size() == 0 || (stringList.size() == 1 && stringList.get(0).trim().equals("")))
            return null;
        Set<Long> longSet = new HashSet<Long>();
        for (String s : stringList) {
            Long l = new Long(s);
            longSet.add(l);
        }
        return longSet;
    }

    private SortArgument[] convertStringListToSortArgumentArr(List<String> stringList) {
        if (stringList == null || stringList.size() == 0 || (stringList.size() == 1 && stringList.get(0).trim().equals("")))
            return null;
        ArrayList<SortArgument> list = new ArrayList<SortArgument>();
        for (String s : stringList) {
            list.add(new SortArgument(s));
        }
        SortArgument[] sa = new SortArgument[list.size()];
        return list.toArray(sa);
    }

    private Map<String, IndexedFastaSequence> getDeflineSequenceMapByNodeId(Long queryNodeId, Map<Long, RandomAccessFile> rafMap) {
        FastaFileNode queryNode = (FastaFileNode) featureDAO.getNodeById(queryNodeId);
        File queryFile = new File(queryNode.getFastaFilePath());
        Map<String, IndexedFastaSequence> deflineSequenceMap = new HashMap<String, IndexedFastaSequence>();
        try {
            RandomAccessFile raf = new RandomAccessFile(queryFile, "r");
            rafMap.put(queryNodeId, raf);
            String defline = null;
            String line;
            long startPosition = -1L;
            long priorPosition;
            long position = raf.getFilePointer();
            while ((line = raf.readLine()) != null) {
                priorPosition = position;
                position = raf.getFilePointer();
                if (line.trim().startsWith(">")) {
                    if (defline != null) {
                        // Add previous sequence to map
                        String queryKey = getDeflineKeyFromString(defline.trim());
                        IndexedFastaSequence ifs = new IndexedFastaSequence(raf, startPosition, new Long(priorPosition - startPosition).intValue());
                        deflineSequenceMap.put(queryKey, ifs);
                    }
                    startPosition = position;
                    defline = line;
                }
                else {
                    // just move along
                }
            }
            // Handle last case
            if (defline != null) {
                // Add previous sequence to map
                String queryKey = getDeflineKeyFromString(defline.trim());
                IndexedFastaSequence ifs = new IndexedFastaSequence(raf, startPosition, new Long(position - startPosition).intValue());
                deflineSequenceMap.put(queryKey, ifs);
            }
            // RandomAccessFile closed later from rafMap values collection
            return deflineSequenceMap;
        }
        catch (Exception ex) {
            _logger.error("Error finding query file=" + queryFile.getAbsolutePath() + " : " + ex.getMessage());
            return deflineSequenceMap;
        }
    }

}
