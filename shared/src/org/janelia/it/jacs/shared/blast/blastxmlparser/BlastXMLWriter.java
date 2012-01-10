
package org.janelia.it.jacs.shared.blast.blastxmlparser;

import org.janelia.it.jacs.model.genomics.BlastHit;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.shared.blast.BlastWriter;
import org.janelia.it.jacs.shared.blast.ParsedBlastHSP;
import org.janelia.it.jacs.shared.blast.ParsedBlastResult;
import org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb.*;
import org.janelia.it.jacs.shared.utils.FileUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Apr 25, 2007
 * Time: 10:33:53 AM
 */
public class BlastXMLWriter extends BlastWriter {

    private final static String BEGINING = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<!DOCTYPE BlastOutput PUBLIC \"-//NCBI//NCBI BlastOutput/EN\" \"http://www.ncbi.nlm.nih.gov/dtd/NCBI_BlastOutput.dtd\">\n<BlastOutput>\n";
    private final static String END = "\n</BlastOutput>";

    private ObjectFactory jbObjectFactory = new ObjectFactory();

    JAXBContext jaxbContext = null;

    public BlastXMLWriter() throws Exception {
        jaxbContext = JAXBContext.newInstance("org.janelia.it.jacs.shared.blast.blastxmlparser.jaxb");
    }

    public void setBlastSource(BlastResultNode brn) {
        setBlastSource((IBlastOutputFormatTask) brn.getTask(),
                brn.getBlastHitResultSet(),
                brn.getDeflineMap());
    }

    public void setBlastSource(IBlastOutputFormatTask task, Set<BlastHit> blastHits, Map<String, String> deflineMap) {
        this.task = task;
        this.deflineMap = deflineMap;
        blastCollection = new ArrayList<ParsedBlastResult>();
        for (BlastHit bh : blastHits) {
            blastCollection.add(new ParsedBlastResult(bh));
        }
    }

    /**
     * Overwrite parent class to insure UTF8 encoding
     *
     * @param outFile
     * @throws IOException
     */
    public void init(File outFile) throws IOException {
        bufferedWriter = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(FileUtil.ensureFileExists(outFile.getAbsolutePath())), "UTF8"));
    }

    public void writeTopPortion() throws Exception {
        ParsedBlastResult pbr = null;
        if (blastCollection.size() > 0)
            pbr = blastCollection.iterator().next();

        bufferedWriter.write(BEGINING);

        // Unfortunately, JAXB only outputs XMLSerializable objects.  Primitive wrapper
        // objects are not XMLSerializable .. so have to manually write these
        bufferedWriter.print("<BlastOutput_program>");
        bufferedWriter.print(task.getTaskName());
        bufferedWriter.println("</BlastOutput_program>");

        bufferedWriter.print("<BlastOutput_version>");
        bufferedWriter.print(pbr == null ? UNSPECIFIED_STR : pbr.getBlastVersion());
        bufferedWriter.println("</BlastOutput_version>");

        bufferedWriter.print("<BlastOutput_reference>");
        bufferedWriter.print(BLAST_REFERENCE);
        bufferedWriter.println("</BlastOutput_reference>");

        bufferedWriter.print("<BlastOutput_db>");
        bufferedWriter.print(getBlastDB());
        bufferedWriter.println("</BlastOutput_db>");

        bufferedWriter.print("<BlastOutput_query-ID>");
        bufferedWriter.print(pbr == null ? "" : pbr.getQueryId());
        bufferedWriter.println("</BlastOutput_query-ID>");

        bufferedWriter.print("<BlastOutput_query-def>");
        bufferedWriter.print(pbr == null ? "" : deflineMap.get(pbr.getQueryId()));
        bufferedWriter.println("</BlastOutput_query-def>");

        bufferedWriter.print("<BlastOutput_query-len>");
        bufferedWriter.print(pbr == null ? "" : String.valueOf(pbr.getQueryLength()));
        bufferedWriter.println("</BlastOutput_query-len>");

        Parameters jbParameters = populateParameters();
        serialize(bufferedWriter, jbParameters, true, false);


    }

    public void writeBottomPortion() throws IOException {
        bufferedWriter.println(END);
    }

    public void startQueryPortion() throws IOException {
        bufferedWriter.println("\n<BlastOutput_iterations>");
    }

    public void endQueryPortion() throws IOException {
        bufferedWriter.println("</BlastOutput_iterations>");
    }


    public void writeSingleQueryPortion(String qID, List<ParsedBlastResult> pbrList, long queryCounter) throws Exception {
//    private BigInteger writeBody(BlastOutputType jbBlastOutput, BufferedWriter bufferedWriter,
//                                 BlastXMLWriter blastXMLWriter, BigInteger currenIterationCount) throws Exception {


//        Iteration singleIteration = jbObjectFactory.createIteration();
        IterationType iterationType = jbObjectFactory.createIterationType();


//        TreeSet<ParsedBlastResult> pbrSet = pbrMap.get(queryAcc);
        Hit jbHit = populateHit(pbrList, deflineMap);

        iterationType.setIterationHits(jbHit);
        iterationType.setIterationIterNum(BigInteger.valueOf(queryCounter));
        //jbIterationType.setIterationMessage();
        iterationType.setIterationQueryDef(deflineMap.get(qID));
        iterationType.setIterationQueryID(qID);
        iterationType.setIterationQueryLen(BigInteger.valueOf(Long.valueOf(pbrList.get(0).getQueryLength())));
        //jbIterationType.setIterationStat();
//        singleIteration.(iterationType);

        serialize(bufferedWriter, iterationType, true, false);
        bufferedWriter.println(""); // end of line

    }


    /**
     * Use this if you want to serialize a JAXB object that is BlastOutput
     */
    public void serialize(Writer writer) throws Exception {
        serialize(writer, getBlastOutput());
    }

    /**
     * Use this if you want to serialize a JAXB object that is BlastOutput
     */
    public void serialize(OutputStream outputStream) throws Exception {
        serialize(outputStream, getBlastOutput());
    }

    /**
     * Use this if you want to serialize a JAXB object that is BlastOutput
     */
    public void serialize(Writer writer, Object object) throws Exception {
        serialize(writer, object, true, true);
    }

    /**
     * Use this if you want to serialize a JAXB object that is BlastOutput
     */
    public void serialize(OutputStream outputStream, Object object) throws Exception {
        serialize(outputStream, object, true, true);
    }

    /**
     * Use this if you want to serialize a JAXB object that is NOT BlastOutput. Both formatOutput and includeXmlDeclaration
     * would have to be false in this case.
     */
    public void serialize(Writer writer, Object object, boolean formatOutput, boolean includeXmlDeclaration) throws Exception {
        Marshaller jbMarshaller = createMarshaller(formatOutput, includeXmlDeclaration);
/**
 * After JAXB 2.x upgrade, we started getting following exception:
 [com.sun.istack.SAXException2: unable to marshal type "org.janelia.it.jacs.shared.blastxmlparser.jaxb.Parameters" as an element because it is missing an @XmlRootElement annotation]
 at com.sun.xml.bind.v2.runtime.MarshallerImpl.write(MarshallerImpl.java:331)
 at com.sun.xml.bind.v2.runtime.MarshallerImpl.marshal(MarshallerImpl.java:257)
 at javax.xml.bind.helpers.AbstractMarshallerImpl.marshal(AbstractMarshallerImpl.java:96)
 at org.janelia.it.jacs.shared.blastxmlparser.BlastXMLWriter.serialize(BlastXMLWriter.java:105)

 jbMarshaller.marshal(object, writer);
 */
        // Following workaround suggested in http://weblogs.java.net/blog/kohsuke/archive/2006/03/why_does_jaxb_p.html
        // does not work because it creates an extra parent element that violates the schema
        // jbMarshaller.marshal( new JAXBElement(new javax.xml.namespace.QName("uri","local"), Object.class, object ),writer);

        // Following workaround in http://www.javadonkey.com/blog/unable-to-marshal-type-xmlrootelement-annotation/
        // works !!
        ObjectFactory objFactory = new ObjectFactory();
        if (object instanceof Parameters) {
            jbMarshaller.marshal(objFactory.createBlastOutputParam((Parameters) object), writer);
        }
        else if (object instanceof Iteration) {
            jbMarshaller.marshal(objFactory.createBlastOutputIterations((Iteration) object), writer);
        }
        else if (object instanceof BlastOutputType) {
            jbMarshaller.marshal(objFactory.createBlastOutput((BlastOutputType) object), writer);
        }
        else if (object instanceof IterationType) {
            jbMarshaller.marshal(new JAXBElement(new QName("", "Iteration"), IterationType.class, object), writer);
        }
        else {
            jbMarshaller.marshal(object, writer);
        }
    }

    /**
     * Use this if you want to serialize a JAXB object that is NOT BlastOutput. Both formatOutput and includeXmlDeclaration
     * would have to be false in this case.
     */
    public void serialize(OutputStream outputStream, Object object, boolean formatOutput, boolean includeXmlDeclaration) throws Exception {
        Marshaller jbMarshaller = createMarshaller(formatOutput, includeXmlDeclaration);
        jbMarshaller.marshal(object, outputStream);
    }

    public BlastOutputType getBlastOutput() throws Exception {
        if (blastCollection == null || task == null) {
            throw new Exception("Either blastCollection or task is null.  Did you setBlastSource prior to serialize(writer)?");
        }
        BlastOutputType jbBlastOutput = populateBlastOutput(blastCollection);
        if (jbBlastOutput == null) {
            throw new Exception("jbBlastOutput is null in serialize.");
        }
        return jbBlastOutput;
    }

    private Marshaller createMarshaller(boolean formatOutput, boolean includeXmlDeclaration) throws Exception {
        //Marshaller jbMarshaller = jbObjectFactory.createMarshaller();
        Marshaller jbMarshaller = jaxbContext.createMarshaller();
        jbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        jbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
//        if (!formatOutput) {
//            jbMarshaller.setProperty("com.sun.xml.bind.indentString", "");
//        }
        jbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, !includeXmlDeclaration);
        //jbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, new Boolean(includeXmlDeclaration));
        //jbMarshaller.setProperty("com.sun.xml.bind.xmlDeclaration", new Boolean(includeXmlDeclaration));
        return jbMarshaller;
    }

    private BlastOutputType populateBlastOutput(Collection<ParsedBlastResult> pbrCol) throws Exception {

        BlastOutputType jbBlastOutput = jbObjectFactory.createBlastOutputType();

        jbBlastOutput.setBlastOutputDb(UNSPECIFIED_STR);

        Iteration jbIteration = populateIteration(pbrCol, deflineMap);
        jbBlastOutput.setBlastOutputIterations(jbIteration);

        //jbBlastOutput.setBlastOutputMbstat();

        Parameters jbParameters = populateParameters();
        jbBlastOutput.setBlastOutputParam(jbParameters);
        jbBlastOutput.setBlastOutputProgram(task.getTaskName());

        // Get first record
        Iterator pbrIt = pbrCol.iterator();
        if (pbrIt.hasNext()) {
            ParsedBlastResult pbr = (ParsedBlastResult) (pbrIt.next());
            if (pbr.getBlastVersion() != null) {
                jbBlastOutput.setBlastOutputVersion(pbr.getBlastVersion());
            }
            else {
                jbBlastOutput.setBlastOutputVersion(UNSPECIFIED_STR);
            }

            jbBlastOutput.setBlastOutputQueryDef(deflineMap.get(pbr.getQueryId()));
            jbBlastOutput.setBlastOutputQueryID(pbr.getQueryId());
            jbBlastOutput.setBlastOutputQueryLen(BigInteger.valueOf(pbr.getQueryLength()));
            //jbBlastOutput.setBlastOutputQuerySeq();
        }
        else {
            jbBlastOutput.setBlastOutputVersion(UNSPECIFIED_STR);
        }

        jbBlastOutput.setBlastOutputReference(BLAST_REFERENCE);
        return jbBlastOutput;
    }

//**********************************************************************************
//*                                                                                *
//*                            Iteration ~ QUERY SEQUENCE                          *
//*                                                                                *

    //**********************************************************************************
    private Iteration populateIteration(Collection<ParsedBlastResult> parsedBlastResultSet, Map<String, String> deflineMap) throws Exception {
        Iteration jbIteration = jbObjectFactory.createIteration();

        // First group by query accession and sort
        HashMap<String, TreeSet<ParsedBlastResult>> pbrMap = new HashMap<String, TreeSet<ParsedBlastResult>>();
        for (ParsedBlastResult pbr : parsedBlastResultSet) {
            String queryAcc = pbr.queryId;
            if (!pbrMap.containsKey(queryAcc)) {
                pbrMap.put(queryAcc, new TreeSet<ParsedBlastResult>());
            }
            pbrMap.get(queryAcc).add(pbr);
        }

        // get the reference to the jaxb list we want to populate
        List<IterationType> jbIterationList = jbIteration.getIteration();

        // Iterate over each query
        Long iterationCounter = 1L;

        // Sort the keys by query so we can loop by order of query
        TreeSet<String> sortedKeySet = new TreeSet<String>();
        sortedKeySet.addAll(pbrMap.keySet());

        // PER QUERY loop
        for (String queryAcc : sortedKeySet) {

            IterationType jbIterationType = jbObjectFactory.createIterationType();

            TreeSet<ParsedBlastResult> pbrSet = pbrMap.get(queryAcc);
            Hit jbHit = populateHit(pbrSet, deflineMap);

            jbIterationType.setIterationHits(jbHit);
            jbIterationType.setIterationIterNum(BigInteger.valueOf(iterationCounter));
            //jbIterationType.setIterationMessage();
            jbIterationType.setIterationQueryDef(deflineMap.get(queryAcc));
            jbIterationType.setIterationQueryID(queryAcc);
            jbIterationType.setIterationQueryLen(BigInteger.valueOf(Long.valueOf(pbrSet.first().getQueryLength())));
            //jbIterationType.setIterationStat();

            jbIterationList.add(jbIterationType);
            iterationCounter++;
        }

        return jbIteration;
    }

    //**********************************************************************************
    //*                                                                                *
    //*                                  Hit ~ SUBJECT SEQUENCE                        *
    //*                                                                                *
    //**********************************************************************************
    private Hit populateHit(Collection<ParsedBlastResult> pbrSet, Map<String, String> deflineMap) throws Exception {
        // everythig in pbrSet should have the same query id
//        Map<String, TreeSet<ParsedBlastResult>> sbjBhMap = new HashMap<String, TreeSet<ParsedBlastResult>>();
        List<ParsedBlastResult> sortedPbrList = new LinkedList<ParsedBlastResult>();
//        Set<String> checkAccSet = new HashSet<String>();
        for (ParsedBlastResult pbr : pbrSet) {
//            String sbjAcc = pbr.getSubjectId();
//            if (checkAccSet.contains(sbjAcc)) {
//                // each hit must have a unique subject ID. detection of multiple subject IDs is a fatal error
//                System.out.println("BlastXMLWriter WARNING: detected multiple hits on the same subject (" + sbjAcc + ") for query ID " + quueryID);
//            }
//            else {
                sortedPbrList.add(pbr);
//            }
        }
        // Sort the pbr list so we can loop in order of relevance - e-value is the key to order by!
        Collections.sort(sortedPbrList);


        // Hit is the container for HitType's
        Hit jbHit = jbObjectFactory.createHit();
        // Get reference to list where we will store the HitType's
        List<HitType> jbHitTypeList = jbHit.getHit();

        // PER SUBJECT loop
        Integer hitCounter = 1;
        for (ParsedBlastResult pbr : sortedPbrList) {
            String sbjAcc = pbr.getSubjectId();
            HitType jbHitType = jbObjectFactory.createHitType();
            jbHitType.setHitAccession(sbjAcc);   //ordering by subject accession?  sequence order in database? can't really tell
            jbHitType.setHitDef(deflineMap.get(sbjAcc));
            jbHitType.setHitId(sbjAcc); // hit subject sequence id
            jbHitType.setHitLen(BigInteger.valueOf(pbr.getSubjectLength()));
            jbHitType.setHitNum(BigInteger.valueOf((hitCounter).longValue()));     // counter for each subject

            List<Hsp> alignmentHSPList = populateAlignmentHSPs(pbr);
            for (Hsp hsp : alignmentHSPList) {
                jbHitType.setHitHsps(hsp); // multiple hsps per alignment
            }

            jbHitTypeList.add(jbHitType);
            hitCounter++;
        }

        return jbHit;
    }

    private List<Hsp> populateAlignmentHSPs(ParsedBlastResult pbr) throws Exception {
        // everything in here should have the same query and subject id
//**********************************************************************************
//*                                                                                *
//*                         Hsp ~ collection of HspType                            *
//*                            HspType ~ specific HSP                              *
//*                                                                                *
//**********************************************************************************
        List<Hsp> alignmentHSPs = new ArrayList<Hsp>();
        Hsp jbHsp = jbObjectFactory.createHsp();
        List<HspType> jbHspTypeList = jbHsp.getHsp();

        List<ParsedBlastHSP> hspList = pbr.getHspList();
        Integer hspCounter = 1;
        for (ParsedBlastHSP hsp : hspList) {
            HspType jbHspType = jbObjectFactory.createHspType();
            jbHspType.setHspEvalue(hsp.getExpectScore());

            jbHspType.setHspAlignLen(BigInteger.valueOf(hsp.getLengthAlignment()));
            jbHspType.setHspBitScore(hsp.getBitScore());
            jbHspType.setHspGaps(BigInteger.valueOf(hsp.getSubjectGaps() + hsp.getQueryGaps()));
            jbHspType.setHspHitFrame(BigInteger.valueOf(hsp.getSubjectFrame()));

            /*
               all blast programs' hsp hit start and end base pair values
               appear in ascending value order (i.e. start < end) in output xml;
            */
            jbHspType.setHspHitFrom(BigInteger.valueOf(hsp.getSubjectBegin() + 1));
            jbHspType.setHspHitTo(BigInteger.valueOf(hsp.getSubjectEnd()));

            jbHspType.setHspHseq(hsp.getSubjectAlignString());
            jbHspType.setHspIdentity(BigInteger.valueOf(hsp.getNumberIdentical()));
            jbHspType.setHspMidline(hsp.getMidline());
            jbHspType.setHspNum(BigInteger.valueOf(hspCounter));
            jbHspType.setHspPositive(BigInteger.valueOf(hsp.getNumberSimilar() + hsp.getNumberIdentical()));
            jbHspType.setHspQseq(hsp.getQueryAlignString());
            jbHspType.setHspQueryFrame(BigInteger.valueOf(hsp.getQueryFrame()));

            /*
               most blast programs' hsp query start and end base pair values
               appear in ascending value order (i.e. start < end) in output xml.

               however, the blastn and megablast programs seem to have a "feature"
               where if the subject sequence's match orientation is in the reverse
               direction (or the subject frame's value is < 0), then the query start
               and end base pair values appear in descending value order (i.e. start > end)
               in the output xml
            */
            jbHspType.setHspQueryFrom(BigInteger.valueOf(hsp.getQueryBegin() + 1));
            jbHspType.setHspQueryTo(BigInteger.valueOf(hsp.getQueryEnd()));

            if ("blastn".equalsIgnoreCase(pbr.getProgramUsed())
                    || "megablast".equalsIgnoreCase(pbr.getProgramUsed())) {
                if (hsp.getSubjectFrame() != null && hsp.getSubjectFrame() < 0) {
                    jbHspType.setHspQueryFrom(BigInteger.valueOf(hsp.getQueryEnd()));
                    jbHspType.setHspQueryTo(BigInteger.valueOf(hsp.getQueryBegin() + 1));
                }
            }
            jbHspType.setHspScore(hsp.getHspScore());

            jbHspTypeList.add(jbHspType);
            hspCounter++;
        }
        alignmentHSPs.add(jbHsp);
        return alignmentHSPs;
    }


    private Parameters populateParameters() throws Exception {

        Parameters jbParameters = jbObjectFactory.createParameters();

        ParametersType jbParametersType = jbObjectFactory.createParametersType();
        //jbParametersType.setParametersEntrezQuery(task.getParameter);
        jbParametersType.setParametersExpect(task.getExpectationValue());
        jbParametersType.setParametersFilter(task.getFilterQuerySequence());
        jbParametersType.setParametersGapExtend(task.getGapExtensionCost());
        jbParametersType.setParametersGapOpen(task.getGapOpeningCost());        //jbParametersType.setParametersInclude(task.getParameter());
        jbParametersType.setParametersMatrix(task.getMatrix());
        BigInteger matchReward = task.getMatchReward();
        if (matchReward != null) {
            jbParametersType.setParametersScMatch(matchReward);
        }
        BigInteger mismatchPenalty = task.getMismatchPenalty();
        if (mismatchPenalty != null) {
            jbParametersType.setParametersScMismatch(mismatchPenalty);
        }
        jbParameters.setParameters(jbParametersType);
        return jbParameters;
    }


}
