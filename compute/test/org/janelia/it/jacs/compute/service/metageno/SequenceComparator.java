
package org.janelia.it.jacs.compute.service.metageno;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jgoll
 * Date: Mar 31, 2009
 * Time: 12:07:08 PM
 *
 */
public class SequenceComparator {

    public static SequenceComparator instance;

    public static SequenceComparator getInstance() {
		if(instance==null)
			instance = new SequenceComparator();
		return instance;
	}

    //takes fasta input files and returns true if sequence data is identical
    public boolean compare(String fileNameA,String fileNameB) {
        boolean isEqual = false;
        Map<String, ArrayList<String>> hashA = this.readFastaData(fileNameA);
        Map<String, ArrayList<String>> hashB = this.readFastaData(fileNameB);
        isEqual = this.compare(hashA,hashB);

        //try to free memory
        hashA=null;
        hashB=null;
        Runtime rt = Runtime.getRuntime();
        rt.gc(); 

        return isEqual;
    }

    //returns hash of reads ids and CRC64 checksums of
    // associated sequences (HasMap containg ArrayLists)
    private Map readFastaData(String fileName) {

        //init local variables
        HashMap<String, ArrayList<String>> reads = new HashMap<String, ArrayList<String>>();
        boolean isFirstEntry = true;
        BufferedReader fileReader = null;
        StringBuffer sequenceBuffer = null;
        String line, id = "";

        try {
            fileReader = new BufferedReader(new FileReader(fileName));

            //process fasta file
            while ((line = fileReader.readLine()) != null) {

                //if first fasta header
                if (line.startsWith(">") && isFirstEntry) {
                    id = getIdFromDefline(line);
                    sequenceBuffer = new StringBuffer();
                    isFirstEntry = false;
                }
                //if any other fasta header
                else if (line.startsWith(">") && !isFirstEntry) {

                    String checksum = Checksum.getInstance().calcCRC64(sequenceBuffer.toString());

                    //adds reads to the hash map
                    addChecksum(reads, id, checksum);

                    //reset sequenceBuffer
                    id = getIdFromDefline(line);
                    sequenceBuffer = new StringBuffer();
                }
                //else append sequence
                else {
                    sequenceBuffer.append(line.trim());
                }
            }
        } catch (IOException e) {
            System.out.println("Input file " + fileName + " could not be read:");
            e.printStackTrace();
        }
        finally {
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //process last entries
        String checksum = Checksum.getInstance().calcCRC64(sequenceBuffer.toString());

        //adds reads to the hash map
        addChecksum(reads, id, checksum);

        return reads;
    }

    //adds a sequence checksum to the hashmap
    private void addChecksum(Map m, String id, String checksum) {
        //if read id exists in hash add checksum to existing array
        if (m.containsKey(id)) {
            ArrayList<String> orfs = (ArrayList<String>) m.get(id);
            orfs.add(checksum);
        }
        //if read id does not exist create new array
        else {
            ArrayList<String> orfs = new ArrayList<String>();
            orfs.add(checksum);
            m.put(id, orfs);
        }
    }

    private String getIdFromDefline(String header) {
        String id = header.split("/read_id=")[1].split(" ")[0].trim();

        return id;
    }

    //returns true if the two hashmaps match
    private boolean compare(Map<String, ArrayList<String>> hashA, Map<String, ArrayList<String>> hashB) {

        //set local variables
        boolean isEqual = true;
        final Set<String> keysA = hashA.keySet();
        final Set<String> keysB = hashB.keySet();

        //if the number of hash keys differs
        if (hashA.size() != hashB.size()) {
            isEqual = false;
        }
        //if the two read id sets don't match
        else if (!keysA.equals(keysB)) {
            isEqual = false;
            //return isEqual;
        }
        //compare hash values
        else {
            //loop through hash
            for (String keyA : keysA) {

                //get checksums
                ArrayList<String> checksumsA = hashA.get(keyA);
                ArrayList<String> checksumsB = hashB.get(keyA);

                //if checksum is not present in the other set
                if (checksumsA!=null && checksumsB==null) {
                    isEqual = false;
                    break;
                } else if (checksumsA.size() != checksumsB.size()) {
                    isEqual = false;
                    break;
                } else {
                    //inital number of differences
                    int checksumDifferences = checksumsA.size();

                    //loop through checksums and check all permutations
                    for (String orfA : checksumsA) {
                        for (String orfB : checksumsB) {
                            if (orfA.equals(orfB)) {
                                //count down
                                checksumDifferences--;
                            }
                        }
                    }

                    //if there are checksum differences
                    if (checksumDifferences > 0) {
                        isEqual = false;
                        break;
                    }
                }
            }

        }
      
        return isEqual;
    }
}

