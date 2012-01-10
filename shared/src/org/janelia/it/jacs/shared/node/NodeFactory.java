
package org.janelia.it.jacs.shared.node;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Nucleotide;
import org.janelia.it.jacs.model.genomics.Peptide;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.user_data.User;

import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 18, 2007
 * Time: 10:43:56 AM
 */
public class NodeFactory {

    private static Logger logger = Logger.getLogger(NodeFactory.class);

    private static BaseSequenceEntity parseEntry(User targetUser, String entry) {
        //todo All FASTA parsing should be located in the same, single class
        String[] deflineAndSequence = FastaUtil.parseDeflineAndSequence(entry);
        String defLine = deflineAndSequence[0];
        String sequence = deflineAndSequence[1];
        String tmpSequenceType = FastaUtil.determineSequenceType(sequence);

        BaseSequenceEntity newSequenceEntity;
        // todo This also lives in Data Service Impl
        if (SequenceType.NUCLEOTIDE.equalsIgnoreCase(tmpSequenceType)) {
            newSequenceEntity = new Nucleotide();
        }
        else if (SequenceType.PEPTIDE.equalsIgnoreCase(tmpSequenceType)) {
            newSequenceEntity = new Peptide();
        }
        else {
            logger.warn("Could not determine the nature of the sequence.");
            return null;
        }

        newSequenceEntity.setSequence(sequence);
        newSequenceEntity.setDescription(defLine);
        newSequenceEntity.setOwner(targetUser);
        return newSequenceEntity;
    }

    public static String determineFastaSequenceType(String fastaText) {
        Scanner fastaTokenizer = new Scanner(fastaText);
        try {
            fastaTokenizer.useDelimiter("\n>");
            String entry;
            String tmpSeqType = null;

            while (fastaTokenizer.hasNext() && (entry = fastaTokenizer.next()) != null) {
                if (entry.length() > 0 && (!entry.substring(0, 1).equals(">"))) entry = ">" + entry;
                entry = entry.trim();
                BaseSequenceEntity newBse = parseEntry(null /*targetUser*/, entry);
                if (null != newBse) {
                    // Ensure Homogeneous FASTA file
                    if (null == tmpSeqType) {
                        tmpSeqType = newBse.getSequenceType().getResidueType();
                    }
                    else {
                        if (!tmpSeqType.equalsIgnoreCase(newBse.getSequenceType().getResidueType())) {
                            throw new RuntimeException("The FASTA file contains multiple sequence types which is not supperted yet.\n " +
                                    "The problem sequence is: " + entry + " which is of type " +
                                    newBse.getSequenceType().getResidueType() +
                                    " and the expected sequence type was " + tmpSeqType);
                        }
                    }
                }
            }
            if (tmpSeqType == null) {
                return SequenceType.NOT_SPECIFIED;
            }
            else {
                return tmpSeqType;
            }
        }
        finally {
            fastaTokenizer.close();
        }
    }

}
