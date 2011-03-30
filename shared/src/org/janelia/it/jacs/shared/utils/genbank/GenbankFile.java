/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.shared.utils.genbank;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 4, 2007
 * Time: 10:19:30 AM
 */
public class GenbankFile {
    public static final String KEYWORD_BACTERIA = "bacteria";
    public static final String KEYWORD_ARCHAEA = "archaea";
    public static final String KEYWORD_VIRUSES = "viruses";

    public enum HEADING {
        LOCUS, DEFINITION, ACCESSION, VERSION, PROJECT, KEYWORDS, SOURCE, ORGANISM,
        REFERENCE, AUTHORS, CONSRTM, TITLE, JOURNAL, COMMENT, FEATURES, ORIGIN, CONTIG
    }

    // The "value" of each FEATURE is a location  X..Y or complement(X..Y) or multi-exon
    public enum FEATURE {
        source, gene, CDS, sig_peptide, misc_binding, misc_RNA, tRNA, rRNA
    }

    public enum ENTRY_DATA {
        organism, mol_type, strain, plasmid, locus_tag, ec_number, note, codon_start,
        transl_table, product, protein_id, db_xref, translation, pseudo
    }

    public enum TOPOLOGY {
        CIRCULAR, LINEAR
    }

    private String genbankFilePath;
    private String genbankFeatureSource;
    private long moleculeLength = -1;
    private String lengthUnit;
    private String sequenceType;
    private String organism = "";
    private String organismKeywords = "";
    private ArrayList<String> geneEntries = new ArrayList<String>();
    private String topology, definition, accession, version, project, giNumber, moleculeType; //keywords, source
    private String genbankDivision="";
    private String modificationDate="";

    public GenbankFile(String genbankFilePath) throws Exception {
        this.genbankFilePath = genbankFilePath;
        prepopulateValues();
    }

    /**
     * This method parses the GBK files, from LOCUS to FEATURES, and sets the commonly accessed attributes
     *
     * @throws java.io.FileNotFoundException Couldn't find the file in question
     */
    private void prepopulateValues() throws Exception {
        Scanner scanner = null;
        try {
            scanner = getScanner();
            if (!scanner.hasNextLine()) {
                throw new FileNotFoundException("The file is empty.");
            }
            String line = scanner.nextLine().trim();
            boolean nextLineLoaded;
            while (null != line) {
                nextLineLoaded = false;
                if (line.startsWith(HEADING.LOCUS.toString())) {
                    // Extract all the LOCUS info
                    String[] split = line.split("\\s+");
                    if (split.length >= 7) {
                        moleculeLength = Long.valueOf(split[2]);
                        lengthUnit = split[3];
                        sequenceType = split[4];
                        topology = split[5];
                        genbankDivision = split[6];
                        modificationDate = split[7];
                    }
                }
                else if (line.startsWith(HEADING.DEFINITION.toString())) {
                    String tmpDef = line.substring(line.indexOf(" ")).trim();
                    String nextLine = scanner.nextLine().trim();
                    while (true) {
                        if (!nextLine.startsWith(HEADING.ACCESSION.toString())) {
                            tmpDef = tmpDef + " " + nextLine;
                            tmpDef = tmpDef.replaceAll("\n", "");
                            nextLine = scanner.nextLine().trim();
                        }
                        else {
                            line = nextLine;
                            nextLineLoaded = true;
                            break;
                        }
                    }
                    definition = tmpDef;
                }
                else if (line.startsWith(HEADING.ACCESSION.toString())) {
                    // todo Should this be a list? Example: CP000713 AAPX01000000 AAPX01000001-AAPX01000042
                    String[] split = line.split("\\s+");
                    if (split.length >= 2) {
                        accession = split[1];
                    }
                }
                else if (line.startsWith(HEADING.VERSION.toString())) {
                    String[] split = line.split("\\s+");
                    // Refseq and Genbank
                    if (split.length >= 3) {
                        version = split[1];
                        if (split[2].indexOf("GI") >= 0 && split[2].indexOf(":") >= 0) {
                            giNumber = split[2].substring(split[2].indexOf(":") + 1);
                        }
                        else {
                            giNumber = split[2];
                        }
                    }
                }
                // NOTE: Probably should parse from HEADING to HEADING and treat it as a single string.
                // Guessing the classification line seems to be a little fragile.
                else if (line.startsWith(HEADING.ORGANISM.toString())) {
                    // Grab the organism name
                    organism = line.substring(HEADING.ORGANISM.toString().length() + 1).trim();
                    // Now, get greedy and grab the organism keywords
                    while (scanner.hasNextLine()) {
                        String nextLine = scanner.nextLine().trim();
                        if (!nextLine.startsWith(HEADING.REFERENCE.toString()) && !nextLine.startsWith(HEADING.FEATURES.toString())
                                && !nextLine.startsWith(HEADING.ORIGIN.toString())) {
                            if (nextLine.indexOf(";") >= 0 || nextLine.indexOf(".") >= 0) {
                                organismKeywords = organismKeywords +
                                        ((null == organismKeywords || "".equals(organismKeywords)) ? "" : " ") + nextLine;
                            }
                            // If we don't see the classification semicolon then the next line must be part of the
                            // ORGANISM value
                            else {
                                organism = organism + " " + nextLine;
                            }
                        }
                        else {
                            line = nextLine;
                            nextLineLoaded = true;
                            break;
                        }
                    }
                }
                else if (line.startsWith(HEADING.PROJECT.toString())) {
                    String[] split = line.split("\\s+");
                    if (split.length >= 2) {
                        project = split[1];
                    }
                }
                else if (line.startsWith(HEADING.FEATURES.toString())) {
                    // Grab info for the Molecule type.  Look for hints from FEATURE to the first gene
                    String tmpLine = scanner.nextLine().trim();
                    boolean moleculeTypeFound = false;
                    while (null != tmpLine && !tmpLine.startsWith(FEATURE.gene.toString())) {
                        if (tmpLine.startsWith("/" + ENTRY_DATA.plasmid.toString())) {
                            moleculeType = "Plasmid " + tmpLine.substring(tmpLine.indexOf("\"") + 1, tmpLine.lastIndexOf("\""));
                            moleculeTypeFound = true;
                            break;
                        }
                        else {
                            if (scanner.hasNextLine()) {
                                tmpLine = scanner.nextLine().trim();
                            }
                            else {
                                tmpLine = null;
                            }
                        }
                    }
                    while (!moleculeTypeFound) {
                        // If the plasmid attribute is not found try to assign to a chromosome
                        // todo This doesn't exactly handle when there are multiple chromosomes
                        // Give each if statement a chance to override the previous check.  There should be a better way.
                        if (definition.toLowerCase().indexOf("complete genome") >= 0 ||
                                definition.toLowerCase().indexOf("complete sequence") >= 0 ||
                                definition.toLowerCase().indexOf("chromosome") >= 0) {
                            moleculeType = "Chromosome";
                            break;
                        }
                        if (definition.toLowerCase().indexOf("plasmid") >= 0) {
                            moleculeType = "Plasmid";
                            break;
                        }
                        if (definition.toLowerCase().indexOf("virus") >= 0 || definition.toLowerCase().indexOf("phage") >= 0) {
                            moleculeType = "Virus";
                            break;
                        }
                        if (null != organismKeywords && organismKeywords.toLowerCase().indexOf(KEYWORD_VIRUSES) >= 0) {
                            moleculeType = "Virus";
                            break;
                        }
                        if (null != organismKeywords && organismKeywords.toLowerCase().indexOf(KEYWORD_ARCHAEA) >= 0) {
                            moleculeType = "Archaea";
                            break;
                        }
                        if (definition.toLowerCase().indexOf("whole genome shotgun") >= 0) {
                            moleculeType = "WGS";
                            break;
                        }
                        if (null != accession && accession.startsWith("NZ")) {
                            moleculeType = "WGS";
                            break;
                        }
                        if (null == moleculeType) {
                            System.err.println("Unable to determine molecule type for " + this.genbankFilePath + ".  Setting to \"pseudomolecule\".");
                            moleculeType = "pseudomolecule";
                        }
                    }
                    // leave the loop as we only care about values this far
                    return;
                }

                // If the next line was already loaded, jump out
                if (nextLineLoaded) {
                    continue;
                }
                // otherwise, set the next line
                if (scanner.hasNextLine()) {
                    line = scanner.nextLine().trim();
                }
                else {
                    line = null;
                }
                // todo Still need to complete from SOURCE to the rest of the file
            }
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
        }
    }

    public String getAccession() {
        return accession;
    }

    public String getProject() {
        return project;
    }

    public long getMoleculeLength() {
        return moleculeLength;
    }

    public String getLengthUnit() {
        return lengthUnit;
    }

    public String getSequenceType() {
        return sequenceType;
    }

    public String getDefinition() {
        return definition;
    }

    public String getVersionString() {
        return version;
    }

    public String getGINumber() {
        return giNumber;
    }

    public String getTopology() {
        return topology;
    }

    public String getMoleculeType() {
        return moleculeType;
    }

    /**
     * This method is very busy.  Every institution names these things differently.
     * Example of above prefix BT_C1_ and BT_P1_ would have a problem.
     *
     * @return - the locus prefix string
     * @throws FileNotFoundException - unable to find the file to parse
     */
    public String getLocusPrefix() throws FileNotFoundException {
        Scanner scanner = null;
        HashSet<String> tmpResults = new HashSet<String>();
        try {
            scanner = getScanner();
            // Walk till FEATURES
            while (scanner.hasNextLine() && !scanner.nextLine().startsWith(HEADING.FEATURES.toString())) {
            }
            while (scanner.hasNextLine()) {
                // Find the locus_tag and grab the prefix
                String tmpLine = scanner.nextLine().trim();
                // If you get to ORIGIN you've gone too far...
                if (tmpLine.indexOf(HEADING.ORIGIN.toString()) >= 0) {
                    break;
                }
                if (tmpLine.indexOf(ENTRY_DATA.locus_tag.toString()) >= 0) {
                    // Remove the GD equals sign!
                    tmpLine = tmpLine.substring(tmpLine.indexOf("=") + 1);
                    // Get rid of the quotes
                    tmpLine = tmpLine.replaceAll("\"", "");
                    int locusBegin = 0;
                    int locusEnd = 0;
                    // Find the division between Alpha and wholly numeric; hopefully, the first locus_tag is good enough
                    int totalLength = tmpLine.length();
                    for (int i = totalLength - 1; i >= 0; i--) {
                        if (!Character.isDigit(tmpLine.charAt(i))) {
                            locusEnd = i;
                            break;
                        }
                    }

                    // Might want to add a sanity check that the locus suffix is a number (if they're always supposed to be)
                    tmpLine = tmpLine.substring(locusBegin, locusEnd + 1);
                    tmpResults.add(tmpLine);
                }
            }
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
        }
        if (0 == tmpResults.size()) {
            return null;
        }
        String locusString = "";
        for (Iterator<String> stringIterator = tmpResults.iterator(); stringIterator.hasNext();) {
            String s = stringIterator.next();
            locusString += s;
            if (stringIterator.hasNext()) {
                locusString += ",";
            }
        }
        return locusString;
    }

    private Scanner getScanner() throws FileNotFoundException {
        return new Scanner(new File(genbankFilePath));
    }

    public void populateAnnotations() throws FileNotFoundException {
        Scanner scanner = null;
        try {
            scanner = getScanner();
            // Only keeping what I need to at the moment...
            boolean saveData = false;
            StringBuffer buf = new StringBuffer();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(HEADING.FEATURES.toString())) {
                    saveData = true;
                }
                if (line.startsWith(HEADING.ORIGIN.toString()) || line.startsWith(HEADING.CONTIG.toString())) {
                    break;
                }

                if (saveData) {
                    buf.append(line).append("\n");
                }
            }
            // todo Feature source in-memory probably too laborious
            genbankFeatureSource = buf.toString();
            try {
                parseFile();
            }
            catch (Exception e) {
                System.out.println("Error parsing the datafile at " + genbankFilePath);
            }
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
        }
    }

    private void parseFile() {
        // Get the annotations
        try {
            String[] pieces = genbankFeatureSource.split("\n");
            StringBuffer tmpGene = new StringBuffer();
            for (String piece : pieces) {
                // If gene found, game on
                if (piece.indexOf("  gene  ") >= 0) {
                    if (!"".equals(tmpGene.toString())) {
                        geneEntries.add(tmpGene.toString());
                        tmpGene = new StringBuffer();
                    }
                    tmpGene.append(piece).append("\n");
                }
                else if (!"".equals(tmpGene.toString())) {
                    tmpGene.append(piece).append("\n");
                }
                if (piece.indexOf(HEADING.ORIGIN.toString()) >= 0 || piece.indexOf(HEADING.CONTIG.toString()) >= 0) {
                    break;
                }
            }
            // Make sure not to append garbage to the last gene if ORIGIN or CONTIG is missing!!!!
            // Don't forget the last gene
            if (!"".equals(tmpGene.toString()) &&
                    !geneEntries.get(geneEntries.size() - 1).equals(tmpGene.toString())) {
                geneEntries.add(tmpGene.toString());
            }
        }
        catch (Throwable e) {
            System.out.println("Error parsing the Genbank file.  Returning nothing.\n" + e.getMessage());
            geneEntries = new ArrayList<String>();
        }
    }

    public ArrayList<String> getGeneEntries(String entryFilterString) {
        if (null == entryFilterString || "".equals(entryFilterString)) return geneEntries;
        else {
            ArrayList<String> returnList = new ArrayList<String>();
            for (String entry : geneEntries) {
                if (("".equals(entryFilterString)) ||
                        getProductForEntry(entry).indexOf(entryFilterString) >= 0) {
                    returnList.add(entry);
                }
            }
            return returnList;
        }
    }

    public static void main(String[] args) {
        try {
            GenbankFile tmpFile = new GenbankFile("S:\\runtime-shared\\filestore\\system\\GenomeProject\\1506511959401431419\\NC_ACGK.gbk");
            System.out.println(tmpFile.getLocusPrefix());
            System.out.println("Test complete.");
//            GenbankFile gb = new GenbankFile("S:\\filestore\\system\\recruitment\\1129192651275370852\\gi78711884sequence.gb");
//            gb.populateAnnotations();
//            List genes = gb.getGenesInRange(794484, 1295210, "");
//            for (Object gene : genes) {
//                String entry = (String) gene;
//                System.out.println(gb.getProductForEntry(entry));
//                System.out.println(gb.getProteinIdForEntry(entry));
//                System.out.println(gb.getDBXRefIdForEntry(entry));
//
//            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getProductForEntry(String entryString) {
        return getValueForAttribute("product", entryString);
    }

    public String getDBXRefIdForEntry(String entryString) {
        return getValueForAttribute("db_xref", entryString);
    }

    public String getProteinIdForEntry(String entryString) {
        return getValueForAttribute("protein_id", entryString);
    }

    private String getValueForAttribute(String attribute, String entry) {
        try {
            if (entry.indexOf("/" + attribute) >= 0) {
                String subentry = entry.substring(entry.indexOf("/" + attribute));
                String value = subentry.substring(subentry.indexOf("\"") + 1, subentry.indexOf("\"", subentry.indexOf("\"") + 1));
                // Need to strip the tabs, newlines and extra whitepsaces
                String[] pieces = value.split("\n");
                StringBuffer buf = new StringBuffer();
                for (String piece : pieces) {
                    buf.append(piece.trim()).append(" ");
                }
                return buf.toString().trim();
            }
        }
        catch (Exception e) {
            System.out.println("Error trying to parse an entry(" + entry + ") for attribute(" + attribute + ")");
        }
        return "";
    }

    /**
     * This method knows how to parse the location strings for annotations
     *
     * @param annotationEntry     - line with the location info
     * @param needBeginCoordinate - boolean to control returning begin value or end
     * @return - double location value
     */
    public long getAnnotationCoordinate(String annotationEntry, boolean needBeginCoordinate) {
        // If there is a newline, remove it
        // todo This does not take care of the multi-exon case
        if (annotationEntry.indexOf("\n") >= 0) {
            annotationEntry = annotationEntry.substring(annotationEntry.indexOf("gene") + 4, annotationEntry.indexOf("\n")).trim();
        }
        // Get rid of the <, > characters for genes at the beginning and end of the axis
        annotationEntry = annotationEntry.replaceAll("<", "").replaceAll(">", "");
        // Remove any "complement()" text
        if (annotationEntry.startsWith("complement")) {
            annotationEntry = annotationEntry.substring(annotationEntry.indexOf('(') + 1, annotationEntry.indexOf(')'));
        }
        // Eliminate "Versioned" coordinate info
        if (annotationEntry.indexOf(":") >= 0) {
            annotationEntry = annotationEntry.substring(annotationEntry.indexOf(":") + 1);
        }
        if (annotationEntry.indexOf(",") >= 0) {
            return -1;
        }
        if (needBeginCoordinate) {
            return Long.parseLong(annotationEntry.substring(0, annotationEntry.indexOf('.')));
        }
        else {
            return Long.parseLong(annotationEntry.substring(annotationEntry.indexOf('.') + 2));
        }
    }

    /**
     * This method is for the RecruitmentViewer, where the user has clicked an annotation and
     * we return all gene entries which intersect that point.
     * Something else will try to figure out which list item was clicked.  A BIG assumption
     * is that the way the annotations were rendered is the same as the way they are
     * returned here.
     * NOTE: If speed is an issue, we COULD probably assume Genbank sorts annotations along
     * the axis.  This would allow us to short-circuit the loop, below, saving time.
     *
     * @param ntPosition       - location along the genome axis
     * @param annotationFilter - looking only for genes with specific string match
     * @return - list of gene entries which intersected the point of interest
     */
    public List<String> getGenesAtLocation(long ntPosition, String annotationFilter) {
        ArrayList<String> returnList = new ArrayList<String>();
        System.out.println("Looking for genes at position " + ntPosition + ", filtered by (" + annotationFilter + ")");
        try {
            for (String geneEntry : geneEntries) {
                // Cut the rest of the entry off at newline
                String tmpLine = geneEntry.substring(geneEntry.indexOf("gene") + 4, geneEntry.indexOf("\n")).trim();
                if (tmpLine.indexOf("join") >= 0 || tmpLine.indexOf("..") < 0) {
                    continue;
                }
                double xBegin = getAnnotationCoordinate(tmpLine, true);
                double xEnd = getAnnotationCoordinate(tmpLine, false);
                if (xBegin <= ntPosition && xEnd >= ntPosition) {
                    if (null == annotationFilter || "".equals(annotationFilter)) {
                        returnList.add(geneEntry);
                    }
                    else {
                        if (getProductForEntry(geneEntry).indexOf(annotationFilter) >= 0) {
                            returnList.add(geneEntry);
                        }
                    }
                }
            }
        }
        catch (Throwable e) {
            System.out.println("Error getting genes at location " + ntPosition + ".\n" + e.getMessage());
            returnList = new ArrayList<String>();
        }
        return returnList;
    }

    /**
     * This method is for the RecruitmentViewer, where the user has selected a bp range.
     * we return all gene entries which fall within that range
     * NOTE: If speed is an issue, we COULD probably assume Genbank sorts annotations along
     * the axis.  This would allow us to short-circuit the loop, below, saving time.
     *
     * @param ntStartPosition  - start location along the genome axis
     * @param ntStopPosition   - stop location along the genome axis
     * @param annotationFilter - string to filter the annotations by
     * @return - list of gene entries which intersected the point of interest
     */
    public List<String> getGenesInRange(long ntStartPosition, long ntStopPosition, String annotationFilter) {
        ArrayList<String> returnList = new ArrayList<String>();
        try {
            for (String geneEntry : geneEntries) {
                // Cut the rest of the entry off at newline
                String tmpLine = geneEntry.substring(geneEntry.indexOf("gene") + 4, geneEntry.indexOf("\n")).trim();
                if (tmpLine.indexOf("join") >= 0 || tmpLine.indexOf("..") < 0) {
                    continue;
                }
                double xBegin = getAnnotationCoordinate(tmpLine, true);
                double xEnd = getAnnotationCoordinate(tmpLine, false);
                if (xBegin >= ntStartPosition && xEnd <= ntStopPosition) {
                    if ((null == annotationFilter || "".equals(annotationFilter)) ||
                            getProductForEntry(geneEntry).indexOf(annotationFilter) >= 0) {
                        returnList.add(geneEntry);
                    }
                }
            }
        }
        catch (Throwable e) {
            System.out.println("Error getting genes in range " + ntStartPosition + " - " + ntStopPosition + ".\n" + e.getMessage());
            returnList = new ArrayList<String>();
        }
        return returnList;
    }

    public boolean entryIsOnForwardStrand(String annotationEntry) {
        return annotationEntry.indexOf("complement") < 0;
    }

    public String getFastaFormattedSequence() throws FileNotFoundException {
        Scanner scanner = null;
        try {
            scanner = getScanner();
            // Only keeping what I need to at the moment...
            boolean saveData = false;
            StringBuffer buf = new StringBuffer();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("ORIGIN")) {
                    saveData = true;
                }
                // If end of file seen, stop
                if (line.indexOf("//") >= 0 && saveData) {
                    break;
                }
                // If saving data, save the line of text
                if (saveData) {
                    buf.append(line).append("\n");
                }
            }
            // Since this is formatted sequence, make sure it ends with a newline.
            if (!buf.toString().endsWith("\n")) {
                buf.append("\n");
            }
            return getSequenceFromRawString(buf.toString());
        }
        finally {
            if (null != scanner) {
                scanner.close();
            }
        }
    }

    private String getSequenceFromRawString(String rawString) {
        StringBuffer buf = new StringBuffer();
        // Cut off the origin.
        rawString = rawString.substring(rawString.indexOf("\n") + 1);
        String[] lines = rawString.split("\n");
        for (String line : lines) {
            // Remove the outer whitespace
            line = line.trim();
            // Cut off the initial line number
            line = line.substring(line.indexOf(" "));
            // Remove the spaces
            line = line.replace(" ", "");
            buf.append(line).append("\n");
        }
        return buf.toString();
    }

    public String getGenbankFilePath() {
        return genbankFilePath;
    }

    public String getOrganism() {
        return organism;
    }

    public String getOrganismKeywords() {
        return organismKeywords;
    }

    public String getGenbankDivision() {
        return genbankDivision;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public String getKingdom() {
        if (organismKeywords.toLowerCase().indexOf(GenbankFile.KEYWORD_VIRUSES) >= 0) {
            return "viral";
        }
        else if (organismKeywords.toLowerCase().indexOf(GenbankFile.KEYWORD_BACTERIA) >= 0) {
            return "bacterial";
        }
        else if (organismKeywords.toLowerCase().indexOf(GenbankFile.KEYWORD_ARCHAEA) >= 0) {
            return "archaea";
        }
        else {
            System.out.println("\n\nCan't determine data type from organism keyword: " + organismKeywords +
                    "\nLocation: " + genbankFilePath + "\n\n");
            return "Unknown";
        }
    }


}
