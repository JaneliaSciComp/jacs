
package org.janelia.it.jacs.compute.service.genomeProject;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 30, 2010
 * Time: 3:12:23 PM
 */
public class GenbankFileWriter {
    private ArrayList<ContigFileVO> _contigFileList = new ArrayList<ContigFileVO>();
    private Long _totalLength = (long) 0;

    public GenbankFileWriter(ArrayList<ContigFileVO> contigFileList, Long totalLength) {
        this._contigFileList = contigFileList;
        this._totalLength = totalLength;
    }

    public void write() throws Exception {
        Scanner scanner = null;
        FileWriter writer = null;
        int totalGBKLengths = 0;
        int bufferSize = 10000;
        File exampleChildFile = new File(_contigFileList.get(0).getFilePath());
        File parentDir = exampleChildFile.getParentFile();
        String testAccession = exampleChildFile.getName().substring(exampleChildFile.getName().indexOf("_") + 1);
        testAccession = GenomeProjectFileNode.PREFIX_REFSEQ_COMPLETE + testAccession.substring(0, 4);
        try {
            GenbankFile exampleGbk = new GenbankFile(exampleChildFile.getAbsolutePath());

            if (null == parentDir) {
                throw new ServiceException("Unable to find parent directory for GenbankFileWriter");
            }
            writer = new FileWriter(new File(parentDir + File.separator + testAccession + "." + GenomeProjectFileNode.GENBANK_FILE_EXTENSION));
            scanner = new Scanner(exampleChildFile);
            while (scanner.hasNextLine()) {
                String tmpLine = scanner.nextLine();
                if (tmpLine.startsWith(GenbankFile.HEADING.LOCUS.toString())) {
                    writer.write(GenbankFile.HEADING.LOCUS.toString() + "\t" + testAccession + "\t" + _totalLength + " " +
                            exampleGbk.getLengthUnit() + "\t" + exampleGbk.getSequenceType() + "\t" + exampleGbk.getTopology() +
                            "\t"+ exampleGbk.getGenbankDivision() + "\t"+ exampleGbk.getModificationDate()+"\n");
                }
                else if (tmpLine.startsWith(GenbankFile.HEADING.ACCESSION.toString())) {
                    writer.write(GenbankFile.HEADING.ACCESSION.toString() + "\t" + testAccession + "\n");
                }
                else if (tmpLine.startsWith(GenbankFile.HEADING.VERSION.toString())) {
                    writer.write(GenbankFile.HEADING.VERSION.toString() + "\t" + testAccession + "\n");
                }
                else if (tmpLine.startsWith(GenbankFile.HEADING.REFERENCE.toString())) {
                    writer.write(GenbankFile.HEADING.REFERENCE.toString() + "\n");
                }
                else if (tmpLine.startsWith(GenbankFile.HEADING.FEATURES.toString())) {
                    // At features stop reading
                    break;
                }
                else {
                    writer.write(tmpLine + "\n");
                }
            }
            StringBuffer sbuf = new StringBuffer();
            // Testing the in-memory route first
            for (Iterator<ContigFileVO> contigFileVOIterator = _contigFileList.iterator(); contigFileVOIterator.hasNext();) {
                ContigFileVO contigFileVO = contigFileVOIterator.next();
                System.out.println("File: " + contigFileVO.getFilePath() + " , Size: " + contigFileVO.getLength());
                GenbankFile tmpGbk = new GenbankFile(contigFileVO.getFilePath());
                totalGBKLengths += tmpGbk.getMoleculeLength();
                sbuf.append(tmpGbk.getFastaFormattedSequence());
                if (contigFileVOIterator.hasNext()) {
                    totalGBKLengths += bufferSize;
                    for (int i = 0; i < bufferSize; i++) {
                        sbuf.append("N");
                    }
                }
            }
            String finalSequence = sbuf.toString();
            finalSequence = finalSequence.replace("\n", "");
            finalSequence = finalSequence.replace(" ", "");
            writer.write(GenbankFile.HEADING.ORIGIN.toString());
            for (int i = 0; i < finalSequence.length(); i++) {
                if (i % 60 == 0) {
                    writer.write("\n\t" + (i + 1));
                }
                if (i % 10 == 0) {
                    writer.write(" ");
                }
                writer.write(finalSequence.charAt(i));
            }
            writer.write("\n//\n");
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
            try {
                if (null != writer) {
                    writer.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Finally, validate the sequence size
        GenbankFile newGBK = new GenbankFile(parentDir + File.separator + testAccession + "." + GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
        String testSeq = newGBK.getFastaFormattedSequence();
        testSeq = testSeq.replaceAll(" ", "");
        testSeq = testSeq.replaceAll("\n", "");
        if (totalGBKLengths != testSeq.length()) {
            throw new ServiceException("GenbankFileWriter: final length does not match reported lengths");
        }
    }
}
