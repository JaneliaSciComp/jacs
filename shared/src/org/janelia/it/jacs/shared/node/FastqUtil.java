
package org.janelia.it.jacs.shared.node;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 3, 2010
 * Time: 10:32:54 AM
* * <p/>
 * <p/>
 * This utility deals with directories containing a set of FASTQ files, typically
 * from an Illumina run.
 * <p/>
 * The data is either paired or un-paired.
 * <p/>
 * If the data is un-paired, the filenames should be:
 * <p/>
 * <prefix>1_1.<extension>
 * <prefix>2_1.<extension>
 * ...
 * <p/>
 * If the data is paired, the filenames should be:
 * <p/>
 * <prefix>1_1.<extension>   "left"
 * <prefix>1_2.<extension>   "right"
 * <prefix>2_1.<extension>   "left"
 * <prefix>2_2.<extension>   "right"
 * <prefix>3_1.<extension>   "left"
 * <prefix>3_2.<extension>   "right"
 * ...
 * ...
 * <p/>
 * Regardless of the <prefix> and the <extension> in the source directory, the
 * upload service converts the filenames to use:
 * <p/>
 * <prefix> = "s"
 * <extension> = ".fq"
 */


public class FastqUtil {

    public static class LaneAndDirection implements Comparable {
        Integer lane = -1;
        Integer direction = -1;
        String filename;

        public LaneAndDirection(String filename) throws Exception {
            this.filename = filename;
            Pattern filePattern = Pattern.compile("(\\S+)(\\d+)\\_(\\d+)\\.(\\S+)");
            Pattern filePattern2 = Pattern.compile("(\\S+)\\_(\\d+)\\_(\\d+)\\_(\\S+)\\.(\\S+)");

            Matcher matcher = filePattern.matcher(filename);
            Matcher matcher2 = filePattern2.matcher(filename);
            if (matcher.matches()) {
                lane = new Integer(matcher.group(2));
                direction = new Integer(matcher.group(3));
            }
            else if (matcher2.matches()) {
                lane = new Integer(matcher2.group(2));
                direction = new Integer(matcher2.group(3));
            }
            if (direction != 1 && direction != 2) {
                throw new Exception("Fastq filename=" + filename + " must specify direction as <prefix><lane>_<direction>.<extension> as either 1 or 2");
            }
        }

        public int compareTo(Object o) {
            LaneAndDirection other = (LaneAndDirection) o;
            if (other.lane < this.lane) {
                return 1;
            }
            if (other.lane > this.lane) {
                return -1;
            }
            if (other.direction < this.direction) {
                return 1;
            }
            if (other.direction > this.direction) {
                return -1;
            }
            return 1;
        }

        public Integer getLane() {
            return lane;
        }

        public Integer getDirection() {
            return direction;
        }

        public String getFilename() {
            return filename;
        }

        public String getStandardFilename() {
            return "s" + lane + "_" + direction + ".fq";
        }

        public String getOppositePairStandardFilename() {
            return "s" + lane + "_" + (direction == 1 ? "2" : "1") + ".fq";
        }

    }

    public static long countSequencesInFastqFile(File f, File scratchDir, Logger logger) throws Exception {
        SystemCall sc = new SystemCall(null, scratchDir, logger);
        File countFile = new File(scratchDir, f.getName() + ".count");
        String countCmd = "grep \"^@\" " + f.getAbsolutePath() + " | wc -l > " + countFile.getAbsolutePath();
        sc.execute(countCmd, false);
        FileReader fr = new FileReader(countFile);
        BufferedReader br = new BufferedReader(fr);
        String countString;
        try {
            countString = br.readLine();
        }
        finally {
            br.close();
        }
        Long count = new Long(countString.trim());
        sc.cleanup();
        return count;
    }

    public static List<LaneAndDirection> getLaneAndDirectionListFromFastqDir(File sourceDirectory, Logger logger)
            throws Exception {
        File[] filesFromSourceDir = sourceDirectory.listFiles();
        List<LaneAndDirection> ladList = new ArrayList<LaneAndDirection>();
        try {
            for (File f : filesFromSourceDir) {
                LaneAndDirection lad = new LaneAndDirection(f.getName());
                ladList.add(lad);
            }
        }
        catch (Exception ex) {
            logger.error("The only files permissible in this directory are FASTQ files of format <prefix>_<lane>.<extension> or <prefix>_<lane>_<direction>.<extension> : " +
                    sourceDirectory.getAbsolutePath());
            throw ex;
        }
        Collections.sort(ladList);
        return ladList;
    }

    // This method throws an exception if there are some paired files, but their opposite partner files are not present
    public static boolean isLaneAndDirectionListPaired(List<LaneAndDirection> list) throws Exception {
        int pairedCount = 0;
        for (LaneAndDirection lad : list) {
            boolean foundMatch = false;
            for (LaneAndDirection lad2 : list) {
                if (lad2.getOppositePairStandardFilename().equals(lad.getStandardFilename())) {
                    foundMatch = true;
                    break;
                }
            }
            if (foundMatch) {
                pairedCount++;
            }
        }
        if (pairedCount == list.size()) {
            return true;
        }
        else if (pairedCount == 0) {
            return false;
        }
        else {
            StringBuffer sb = new StringBuffer("");
            sb.append("Could not find proper pairings for all filenames in this list:\n");
            for (LaneAndDirection lad : list) {
                sb.append(lad.getFilename()).append("\n");
            }
            throw new Exception(sb.toString());
        }
    }

}
