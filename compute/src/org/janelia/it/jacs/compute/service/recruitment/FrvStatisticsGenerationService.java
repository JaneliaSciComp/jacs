
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectRecruitmentSamplingTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.tasks.GenbankFileInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 30, 2010
 * Time: 4:21:59 PM
 */
public class FrvStatisticsGenerationService implements IService {
    private Task task;

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        // Get the values from the task
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        Logger logger = Logger.getLogger(FrvStatisticsGenerationService.class);
        FileWriter writer = null;
        Scanner scanner = null;
        try {
            // Prep for execution
            task = ProcessDataHelper.getTask(processData);
            if (null==task) {
                throw new ServiceException("No task found in FrvStatisticsGenerationService");
            }
            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Generating Statistics", "Generating Statistics", new Date());
            RecruitmentFileNode resultNode = (RecruitmentFileNode) computeBean.getNodeById(processData.getLong(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID));
            // Build the map of multiplier to blast db.  Used to determine coverage and interest
            List<String> blastDBList;
            String tmpDbList;
            boolean useSamplingStrategy = true;
            if (task instanceof GenomeProjectRecruitmentSamplingTask) {
                tmpDbList = task.getParameter(GenomeProjectRecruitmentSamplingTask.BLASTABLE_DATABASE_NODES);
            }
            else if (task instanceof RecruitmentViewerTask) {
                tmpDbList = task.getParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS);
                useSamplingStrategy = false;
            }
            else {
                throw new ServiceException("Task type "+task.getClass()+" unknown to FrvStatisticsGenerationService!");
            }
            if (null!=tmpDbList && !"".equals(tmpDbList)){
                blastDBList = Task.listOfStringsFromCsvString(tmpDbList);
            }
            else {
                throw new ServiceException("Cannot generate statistics for task="+task.getObjectId()+" as there is no db list provided.");    
            }
            HashMap<String, Long> blastDBToEntrySizeMap = new HashMap<String, Long>();
            for (String tmpBlastDb : blastDBList) {
                BlastDatabaseFileNode tmpNode = (BlastDatabaseFileNode)EJBFactory.getLocalComputeBean().getNodeById(Long.valueOf(tmpBlastDb));
                // NOTE!!!!: This implies the db name is the same as the sample name.
                // We need to relate sample id in the blast hit to the original db the aligned subject piece came from
                blastDBToEntrySizeMap.put(tmpNode.getName(), (long)tmpNode.getSequenceCount());
            }

            // STEP 1: Go through the results and map out the unique molecules.  Grab useful info.
            // Default columns and filename for blast_comb_file.  Expecting a sampling run
            String dataFileTag = RecruitmentFileNode.TAG_BLAST_HITS;
            int moleculeIndex=0, alignmentBeginIndex=2, alignmentEndIndex=3, sampleIndex=24;
            // If making stats from an actual recruitment use the columns for the combinedPlusSitePlusMatePlus.hits file
            if (!useSamplingStrategy){
                dataFileTag = RecruitmentFileNode.TAG_COMBINED_HITS;
                sampleIndex=22;
            }
            File dataFile = new File(resultNode.getFilePathByTag(dataFileTag));
            File statsFile = new File(resultNode.getFilePathByTag(RecruitmentFileNode.TAG_STATS));
            writer = new FileWriter(statsFile);
            scanner = new Scanner(dataFile);
            TreeMap<String, RecruitmentMoleculeCoverageInfo> moleculeMap = new TreeMap<String, RecruitmentMoleculeCoverageInfo>();
            HashMap<String, GenbankFileInfo> genbankMap = RecruitmentDataHelper.getGenbankFileMap();
            while (scanner.hasNextLine()) {
                String[] resultLine = scanner.nextLine().split("\t");
                RecruitmentMoleculeCoverageInfo tmpInfo = null;
                // When sampling we don't run official recruitment so throw away blast data which doesn't align 90% or better
                if (useSamplingStrategy) {
                    Double subjectBegin=Double.valueOf(resultLine[6]);
                    Double subjectEnd=Double.valueOf(resultLine[7]);
                    Double subjectLength=Double.valueOf(resultLine[16]);
                    if (subjectEnd<subjectBegin){ double tmp=subjectEnd;subjectEnd=subjectBegin;subjectBegin=tmp;}
                    double percentageAligned = ((subjectEnd-subjectBegin)/subjectLength);
                    if (percentageAligned<0.9){
//                        System.out.println("Dropping data "+subjectBegin+"\t"+subjectEnd+"\t"+subjectLength+"="+percentageAligned);
                        continue;
                    }
                }
                // Count up the raw number of hits
                if (!moleculeMap.containsKey(resultLine[moleculeIndex])) {
                    try {
                        GenbankFileInfo tmpGbkInfo = genbankMap.get(resultLine[moleculeIndex]);
                        if (null==tmpGbkInfo){
                            logger.error("We cannot find Genbank info for "+resultLine[moleculeIndex]+" yet it had an anlignment!?  Skipping...");
                            continue;
                        }
                        Long tmpLength = tmpGbkInfo.getLength();
                        Long tmpUngappedLength = tmpGbkInfo.getLengthWithoutGaps();
                        tmpInfo = new RecruitmentMoleculeCoverageInfo(resultLine[moleculeIndex]);
                        // Since reads won't align to the giant gaps in draft genomes only use the ungapped size to
                        // determine ratios/interesting hits
                        tmpInfo.setMoleculeLength((tmpUngappedLength<tmpLength)?tmpUngappedLength:tmpLength);
                        moleculeMap.put(resultLine[moleculeIndex], tmpInfo);
                    }
                    catch (Exception e) {
                        String error = "Unable to add item " + ((null != tmpInfo) ? tmpInfo.getMoleculeName() : "") + " to the recruitment path.";
                        logger.error(error);
                        logger.error("Data file:"+dataFile.getAbsolutePath()+",result="+resultLine[moleculeIndex]);
                        throw new ServiceException(error);
                    }
                }
                else {
                    tmpInfo = moleculeMap.get(resultLine[moleculeIndex]);
                }

                // Save the alignment
                Alignment tmpNewAlignment = new Alignment(Long.valueOf(resultLine[alignmentBeginIndex]),
                                                          Long.valueOf(resultLine[alignmentEndIndex]));
                tmpInfo.addAlignmentToSample(resultLine[sampleIndex], tmpNewAlignment);

                // Save the item back into the collection
                moleculeMap.put(tmpInfo.getMoleculeName(), tmpInfo);
            }

            // STEP 2: Write out the statistics data.
            // molecule id, mol length, unique coverage length, bases in reads,
            // ratio of bases in reads to unique coverage length, fractional coverage to mol length,
            // raw number of hits, library, genome project node id
            for (String s : moleculeMap.keySet()) {
                RecruitmentMoleculeCoverageInfo tmpInfo = moleculeMap.get(s);
                StringBuffer tmpLine;
                HashMap<String, ArrayList<Alignment>> coverageMap = tmpInfo.getCoverageMap();
                Long aggregateTotalAlignmentBases = 0l;
                // Loop through the samples recruited for the molecule
                // Print a row for the individual samples
                ArrayList<String> excludeSampleFromAggregateList = new ArrayList<String>();
                DecimalFormat format = new DecimalFormat("0.000");
                for (String sample : coverageMap.keySet()) {
                    Long blastDbEntrySize = blastDBToEntrySizeMap.get(sample);
                    boolean randomlySelect = (blastDbEntrySize > 5000);
                    double targetEntryCount = ((blastDbEntrySize * 0.01) <= 5000) ? 5000 : (blastDbEntrySize * 0.01);
                    if (!randomlySelect) {
                        targetEntryCount = blastDbEntrySize;
                    }
                    // Recruitment sampling is how many times smaller than the whole db?
                    double scalingMultiplier;
                    if (targetEntryCount>0) {
                        scalingMultiplier = blastDbEntrySize/targetEntryCount;
                    }
                    else {
                        scalingMultiplier = 0;
                    }
                    tmpLine = new StringBuffer();
                    Long tmpUniqueCoverageLength = tmpInfo.getUniqueCoverageLengthForSample(sample);
                    Long tmpTotalAlignmentBasesBySample = tmpInfo.getTotalAlignmentBasesBySample(sample);
                    // Molecule name
                    tmpLine.append(tmpInfo.getMoleculeName()).append("\t");
                    // Molecule Length - minus gaps
                    tmpLine.append(tmpInfo.getMoleculeLength()).append("\t");
                    // Length of unique coverage on the molecule
                    tmpLine.append(tmpUniqueCoverageLength).append("\t");
                    // Total aligned bases by sample aligned to
                    tmpLine.append(tmpTotalAlignmentBasesBySample).append("\t");
                    // Total aligned bases by sample over unique coverage length
                    double totalAlignedBasesBySampleOverUniqueCoverageLength;
                    if (tmpUniqueCoverageLength>0) {
                        totalAlignedBasesBySampleOverUniqueCoverageLength = tmpTotalAlignmentBasesBySample.floatValue() / tmpUniqueCoverageLength.floatValue();
                    }
                    else {
                        totalAlignedBasesBySampleOverUniqueCoverageLength = 0;
                    }
                    tmpLine.append(format.format(totalAlignedBasesBySampleOverUniqueCoverageLength)).append("\t");
                    // Ratio (scaled-up) of unique coverage over the total molecule length
                    double ratioScaledUpOfUniqueCoverageOverTotalMolLength = (tmpUniqueCoverageLength.floatValue()*scalingMultiplier) / tmpInfo.getMoleculeLength().floatValue();
                    tmpLine.append(format.format(ratioScaledUpOfUniqueCoverageOverTotalMolLength)).append("\t");
                    // Calculate expected coverage
                    double expectedCoverage;
                    if (totalAlignedBasesBySampleOverUniqueCoverageLength>0) {
                        expectedCoverage = ((tmpUniqueCoverageLength.floatValue()/tmpInfo.getMoleculeLength().floatValue())*scalingMultiplier)/
                            totalAlignedBasesBySampleOverUniqueCoverageLength;
                    }
                    else {
                        expectedCoverage = 0;
                    }
                    tmpLine.append(format.format(expectedCoverage)).append("\t");
                    // If this criteria is met the aggregateRemainingSamples will exclude this in the calc
                    if (expectedCoverage>=0.50){
                        excludeSampleFromAggregateList.add(sample);
                    }
                    // Total number of alignments to the sample for this molecule
                    tmpLine.append(tmpInfo.getAlignmentCountForSample(sample)).append("\t");
                    // Sample name
                    tmpLine.append(sample).append("\t");
                    // Node id of the molecule in question
                    tmpLine.append(genbankMap.get(tmpInfo.getMoleculeName()).getGenomeProjectNodeId()).append("\n");
                    writer.write(tmpLine.toString());
                    if (!excludeSampleFromAggregateList.contains(sample)) {
                        aggregateTotalAlignmentBases += tmpTotalAlignmentBasesBySample;
                    }
                }
                // Print a row for the aggregate of the loser samples; where the aggregate is alignments from samples not good
                // enough to hit the 50% cutoff for change of unique coverage for a read.
                // NOTE: No samples might be in the aggregate (because they all scored high).  Prevent / by 0!!!!!!
                Long aggregateTotalUniqueCoverageLength = tmpInfo.getTotalUniqueAggregateCoverageLength(excludeSampleFromAggregateList);
                Long totalDbEntrySize = 0l;
                Long usedSamplingDbSize = 0l;
                for (String sample : coverageMap.keySet()) {
                    if (!excludeSampleFromAggregateList.contains(sample)) {
                        Long blastDbEntrySize = blastDBToEntrySizeMap.get(sample);
                        totalDbEntrySize+=blastDbEntrySize;
                        boolean randomlySelect = (blastDbEntrySize > 5000);
                        double targetEntryCount = ((blastDbEntrySize * 0.01) <= 5000) ? 5000 : (blastDbEntrySize * 0.01);
                        if (!randomlySelect) {
                            targetEntryCount = blastDbEntrySize;
                        }
                        usedSamplingDbSize+=(long)targetEntryCount;
                    }
                }
                long aggregateScalingMultiplier;
                if (null!=usedSamplingDbSize && 0!=usedSamplingDbSize && null!=totalDbEntrySize) {
                    aggregateScalingMultiplier = totalDbEntrySize/usedSamplingDbSize;
                }
                else {
                    aggregateScalingMultiplier = 0;
                }
                tmpLine = new StringBuffer();
                tmpLine.append(tmpInfo.getMoleculeName()).append("\t");
                tmpLine.append(tmpInfo.getMoleculeLength()).append("\t");
                tmpLine.append(aggregateTotalUniqueCoverageLength).append("\t");
                tmpLine.append(aggregateTotalAlignmentBases).append("\t");
                double totalAlignedBasesByAggregateOverUniqueCoverageLength;
                if (null!=aggregateTotalUniqueCoverageLength && 0!=aggregateTotalUniqueCoverageLength) {
                    totalAlignedBasesByAggregateOverUniqueCoverageLength = aggregateTotalAlignmentBases.floatValue() / aggregateTotalUniqueCoverageLength.floatValue();
                }
                else {
                    totalAlignedBasesByAggregateOverUniqueCoverageLength = 0;
                }
                tmpLine.append(format.format(totalAlignedBasesByAggregateOverUniqueCoverageLength)).append("\t");
                // Ratio (scaled-up) of unique coverage over the total molecule length
                double ratioScaledUpOfUniqueCoverageOverTotalMolLength = (aggregateTotalUniqueCoverageLength.floatValue()*aggregateScalingMultiplier) / tmpInfo.getMoleculeLength().floatValue();
                tmpLine.append(format.format(ratioScaledUpOfUniqueCoverageOverTotalMolLength)).append("\t");
                // Calculate expected coverage
                double expectedCoverage;
                if (0!=totalAlignedBasesByAggregateOverUniqueCoverageLength) {
                    expectedCoverage = ((aggregateTotalUniqueCoverageLength.floatValue() / tmpInfo.getMoleculeLength().floatValue())*aggregateScalingMultiplier)/totalAlignedBasesByAggregateOverUniqueCoverageLength;
                }
                else {
                    expectedCoverage=0;
                }
                tmpLine.append(format.format(expectedCoverage)).append("\t");
                tmpLine.append(tmpInfo.getTotalNumberAggregateAlignments(excludeSampleFromAggregateList)).append("\t");
                tmpLine.append(RecruitmentMoleculeCoverageInfo.AGGREGATE_SAMPLES).append("\t");
                tmpLine.append(genbankMap.get(tmpInfo.getMoleculeName()).getGenomeProjectNodeId()).append("\n");
                writer.write(tmpLine.toString());
            }

        }
        catch (Exception e) {
            if (null!=task) {
                logger.error("\n\n\nError in FrvStatisticsGenerationService generating statistics for user " + task.getOwner() + ", task=" + task.getObjectId() + "\nERROR:" + e.getMessage());
                // Try to record the error
                try {
                    computeBean.saveEvent(task.getObjectId(), Event.ERROR_EVENT, "Error executing the FRV statistics", new Date());
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            throw new ServiceException(e);
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class RecruitmentMoleculeCoverageInfo {
        public static final String AGGREGATE_SAMPLES = "aggregateSamples";
        private String moleculeName;
        private HashMap<String, ArrayList<Alignment>> coverageMap = new HashMap<String, ArrayList<Alignment>>();
        private Long moleculeLength = 0l;

        private RecruitmentMoleculeCoverageInfo(String moleculeName) {
            this.moleculeName = moleculeName;
        }

        public String getMoleculeName() {
            return moleculeName;
        }

        public void setMoleculeName(String moleculeName) {
            this.moleculeName = moleculeName;
        }

        public HashMap<String, ArrayList<Alignment>> getCoverageMap() {
            return coverageMap;
        }

        public void addAlignmentToSample(String sample, Alignment alignment) {
            ArrayList<Alignment> tmpAlignments = coverageMap.get(sample);
            if (null == tmpAlignments) {
                tmpAlignments = new ArrayList<Alignment>();
            }
            tmpAlignments.add(alignment);
            coverageMap.put(sample, tmpAlignments);
        }

        public Long getMoleculeLength() {
            return moleculeLength;
        }

        public void setMoleculeLength(Long moleculeLength) {
            this.moleculeLength = moleculeLength;
        }

        public Long getTotalAlignmentBasesBySample(String sample) {
            ArrayList<Alignment> tmpAlignments = coverageMap.get(sample);
            long totalBases = 0;
            for (Alignment tmpAlignment : tmpAlignments) {
                totalBases += tmpAlignment.length();
            }
            return totalBases;
        }

        public Integer getAlignmentCountForSample(String sample) {
            return coverageMap.get(sample).size();
        }

        public Long getUniqueCoverageLengthForSample(String sample) throws ServiceException {
            // Do the collision detection and then report final coverage length
            List<Alignment> uniqueAlignments = getCoverageAlignments(coverageMap.get(sample));
            Long coverageLength = 0l;
            for (Alignment uniqueAlignment : uniqueAlignments) {
                coverageLength += uniqueAlignment.length();
            }
            return coverageLength;
        }

        public Long getTotalUniqueAggregateCoverageLength(List<String> excludedSamples) throws ServiceException {
            // Grab all alignments and create a unique set
            ArrayList<Alignment> totalAlignments = new ArrayList<Alignment>();
            for (String s : coverageMap.keySet()) {
                if (!excludedSamples.contains(s)) {
                    totalAlignments.addAll(coverageMap.get(s));
                }
            }
            List<Alignment> uniqueAlignments = getCoverageAlignments(totalAlignments);
            Long coverageLength = 0l;
            for (Alignment uniqueAlignment : uniqueAlignments) {
                coverageLength += uniqueAlignment.length();
            }
            return coverageLength;
        }

        private ArrayList<Alignment> getCoverageAlignments(ArrayList<Alignment> targetAlignments) throws ServiceException {
            ArrayList<Alignment> returnList = new ArrayList<Alignment>();
            // Sort the alignments by begin points
            Collections.sort(targetAlignments);
            for (Alignment tmpNewAlignment : targetAlignments) {
                boolean newAlignmentIntersectsPrevious = false;
                for (Alignment returnAlignment : returnList) {
                    // If not the criteria below, they must be intersecting
                    if (!(tmpNewAlignment.getEnd() <= returnAlignment.getBegin()) && !(tmpNewAlignment.getBegin() >= returnAlignment.getEnd())) {
                        //System.out.println("Old alignment was\t("+alignment.getBegin()+","+alignment.getEnd()
                        //        +") and new alignment ("+tmpNewAlignment.getBegin()+","+tmpNewAlignment.getEnd()+")");
                        returnAlignment.setBegin(tmpNewAlignment.getBegin() <= returnAlignment.getBegin() ? tmpNewAlignment.getBegin() : returnAlignment.getBegin());
                        returnAlignment.setEnd(tmpNewAlignment.getEnd() >= returnAlignment.getEnd() ? tmpNewAlignment.getEnd() : returnAlignment.getEnd());
                        //System.out.println("New alignment is\t("+alignment.getBegin()+","+alignment.getEnd()+")\n");
                        newAlignmentIntersectsPrevious = true;
                        break;
                    }
                }
                if (!newAlignmentIntersectsPrevious) {
                    returnList.add(tmpNewAlignment);
                }
            }

            //  Check if the returnList has overlapping bases
            boolean alignmentsStillOverlap = false;
            for (Alignment alignment : returnList) {
                for (Alignment returnAlignment : returnList) {
                    // If not the criteria below, they must be intersecting
                    if (!(alignment.getEnd() <= returnAlignment.getBegin()) && !(alignment.getBegin() >= returnAlignment.getEnd())) {
                        if (!(alignment.getEnd()==returnAlignment.getEnd()&&alignment.getBegin()==returnAlignment.getBegin())) {
//                            System.out.println("The return alignments overlap!!!!!!!!!!!!!!!!");
//                            System.out.println("Return:"+returnAlignment.getBegin()+" "+returnAlignment.getEnd());
//                            System.out.println("Align :"+alignment.getBegin()+" "+alignment.getEnd());
                            alignmentsStillOverlap = true;
                        }
                    }
                }
            }
            if (alignmentsStillOverlap) {
                throw new ServiceException("The unique coverage calculation is failing in the FrvStatisticsGenerationService!");
            }
            return returnList;
        }

        public Long getTotalNumberAggregateAlignments(List<String> excludedSamples) {
            Long numAlignments = 0l;
            for (String s : coverageMap.keySet()) {
                if (!excludedSamples.contains(s)) {
                    List<Alignment> tmpAlignments = coverageMap.get(s);
                    if (null != tmpAlignments) {
                        numAlignments += tmpAlignments.size();
                    }
                }
            }
            return numAlignments;
        }
    }

    /**
     * Quick-and-dirty alignment class.  Sorting results in an ordering by begin coordinate.
     */
    public class Alignment implements Comparable {
        private long begin;
        private long end;

        public Alignment(long begin, long end) {
            if (begin<end) {
                this.begin = begin;
                this.end = end;
            }
            else {
                this.begin=end;
                this.end  =begin;
            }
        }

        public long getBegin() {
            return begin;
        }

        public void setBegin(long begin) {
            this.begin = begin;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        public long length() {
            return end - begin;
        }

        @Override
        public int compareTo(Object o) {
            return Long.valueOf(this.begin).compareTo(((Alignment)o).getBegin());
        }

    }

}
