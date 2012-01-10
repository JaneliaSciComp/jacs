
package org.janelia.it.jacs.shared.blast.blastxmlparser;

import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.janelia.it.jacs.shared.TestUtils;
import org.janelia.it.jacs.shared.blast.ParsedBlastHSP;
import org.janelia.it.jacs.shared.blast.ParsedBlastResult;
import org.janelia.it.jacs.shared.blast.ParsedBlastResultCollection;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * BlastXMLParser Tester.
 *
 * @author <Authors name>
 * @since <pre>12/26/2006</pre>
 */
public class BlastXMLParserTest extends TestCase {

    static Logger logger = Logger.getLogger(BlastXMLParserTest.class.getName());
    static{
        Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout()));
    }

    public BlastXMLParserTest() {
        super();
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBlastnOutputParsing() throws Exception {
        executeParse("blastn.xml");
    }

    public void testBlastpOutputParsing() throws Exception {
        executeParse("blastp.xml");
    }

    public void testBlastxOutputParsing() throws Exception {
        executeParse("blastx.xml");
    }

    public void testTBlastnOutputParsing() throws Exception {
        executeParse("tblastn.xml");
    }

    public void testTBlastxOutputParsing() throws Exception {
        executeParse("tblastx.xml");
    }

    public void testMegaBlastOutputParsing() throws Exception {
        executeParse("megablast.xml");
    }

    private void executeParse(String programName){
        try {
            logger.info("Starting... "+programName+" test....");
            String root="shared"+File.separator+"testfiles"+File.separator+"BlastParser"+File.separator+"BLAST Outputs"+File.separator;
            String fname = TestUtils.getTestFile(root + programName).getAbsolutePath();
            logger.info("Working on: "+fname);
            BlastXMLParser bxp = new BlastXMLParser();
            bxp.loadXML(new File(fname));
            bxp.transferToParsedBlastResultSet();
            ParsedBlastResultCollection pbrs = bxp.getParsedBlastResultCollection();
            writeToFile(pbrs, new File(fname+".parsed"));


            // Count HSPs
            Integer xmlNumHSPs=countMatches(new FileReader(fname), "<Hsp>");
            Integer parsedNumHSPs=0;
            Iterator<ParsedBlastResult> iterPbrs = pbrs.iterator();
            while ( iterPbrs.hasNext() ) {
                parsedNumHSPs += ( iterPbrs.next()).getHspList().size();
            }

            logger.info("Num HSPs: XML:"+xmlNumHSPs+" Parsed:"+parsedNumHSPs);
            if(xmlNumHSPs.intValue()!=parsedNumHSPs.intValue()){
                fail("Number of HSPs do not match between parsed and XML");
            }

            Integer xmlUniqueSubjects=countUnique(new FileReader(fname), "<Hit_def>(.+)</Hit_def>");
            Integer parsedUniqueSubjects=bxp.getSubjectIDsSet().size();
            logger.info("Num Subjects Hit: XML:"+xmlUniqueSubjects+" Parsed:"+parsedUniqueSubjects);
            if(xmlUniqueSubjects.intValue()!=parsedUniqueSubjects.intValue()){
                fail("Number of Hit Subjects do not match between parsed and XML");
            }

            Integer xmlBitScoreSum=extractSum(new FileReader(fname), "<Hsp_bit-score>(.+)</Hsp_bit-score>").intValue();
            Float parsedBitScoreSum=0F;
            for(Iterator<ParsedBlastResult> pbrIt=pbrs.iterator(); pbrIt.hasNext();){
                for (Iterator pbrHspIt= pbrIt.next().getHspList().iterator(); pbrHspIt.hasNext();){
                    parsedBitScoreSum+=((ParsedBlastHSP)pbrHspIt.next()).getBitScore();
                }
            }
            logger.info("Bit Score Sum: XML:"+xmlBitScoreSum+" Parsed:"+parsedBitScoreSum.intValue());
            if(xmlBitScoreSum!=parsedBitScoreSum.intValue()){
                fail("Bit Score Sum does not match between parsed and XML");
            }

//            // Print out what is defline map
//            Map defMap=bxp.getDeflineMap();
//            Collection co=defMap.values();
//            for(it=co.iterator(); it.hasNext(); ){
//                logger.debug(it.next() + "\n");
//            }

            logger.info("\n");
            logger.info("Done.");
        }
        catch (javax.xml.bind.JAXBException e) {
            fail("Failed on "+programName+": "+e.getMessage());
        }
        catch (FileNotFoundException e) {
            fail("Failed on "+programName+": "+e.getMessage());
        }
        catch (IOException e) {
            fail("Failed on "+programName+": "+e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Failed on "+programName+": "+e.getMessage());
        }
    }


    public void writeToFile(ParsedBlastResultCollection pbrCollection, File outputFile) throws java.io.IOException {
        PrintStream ostream = new PrintStream(
                new BufferedOutputStream(
                        new FileOutputStream(outputFile)), true);

        Iterator<ParsedBlastResult> it;
        Integer i=0;
        for(it= pbrCollection.iterator();it.hasNext();){
            ParsedBlastResult pbr=it.next();
            ostream.println(pbr.toString());
        }
        ostream.close();
    }


        private Integer countMatches(Reader theReader, String patString) throws java.io.IOException{
        // Returns string that matched if input string matches pattern

        BufferedReader bfrdr = new BufferedReader(theReader);
        Pattern thePattern = Pattern.compile(patString);
        String curLine;
        Integer count=0;

        while((curLine=bfrdr.readLine())!=null){
            Scanner lineScanner=new Scanner(curLine);
            String foundLine=lineScanner.findInLine(thePattern);
            if(foundLine!=null){
                count++;
            }
        }

        return count;
    }

    private Integer countUnique(Reader theReader, String patString) throws java.io.IOException{
        // Returns string that matched if input string matches pattern

        BufferedReader bfrdr = new BufferedReader(theReader);
        Pattern thePattern = Pattern.compile(patString);
        String curLine;
        Set<String> matchSet=new HashSet<String>();

        while((curLine=bfrdr.readLine())!=null){
            Scanner lineScanner=new Scanner(curLine);
            String foundLine=lineScanner.findInLine(thePattern);
            if(foundLine!=null){
                MatchResult theMatchResult=lineScanner.match();
                matchSet.add(theMatchResult.group(1));
            }
        }

        return matchSet.size();
    }

    private Float extractSum(Reader theReader, String patString) throws java.io.IOException{
        // Returns string that matched if input string matches pattern

        BufferedReader bfrdr = new BufferedReader(theReader);
        Pattern thePattern = Pattern.compile(patString);
        String curLine;
        Float sum=0F;

        while((curLine=bfrdr.readLine())!=null){
            Scanner lineScanner=new Scanner(curLine);
            String foundLine=lineScanner.findInLine(thePattern);
            if(foundLine!=null){
                MatchResult theMatchResult=lineScanner.match();
                sum+=Float.parseFloat(theMatchResult.group(1));
            }
        }

        return sum;
    }

}
