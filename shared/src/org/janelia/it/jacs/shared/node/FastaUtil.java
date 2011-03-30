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

package org.janelia.it.jacs.shared.node;

import org.janelia.it.jacs.model.genomics.SequenceType;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Feb 23, 2007
 * Time: 3:06:43 PM
 */
public class FastaUtil {
    public static String SPACES = "                                                                               ";
    private static final Boolean UseAmbiguityCodes = true;

    /**
     * This should eventually work on the content of the file, and not it's name!
     *
     * @param fullpath
     * @return
     */
    public static boolean isFastaFile(String fullpath) {
        //TODO: convert to work on content of the file, and not on filename
        // TODO: change code which uses Constants.java to use this method.  This should all exist in a single location 
        String path = fullpath.toLowerCase();
        return path.endsWith(".fasta") || path.endsWith("fa") || path.endsWith("fna") || path.endsWith("fsa") ||
                path.endsWith(".ffn") || path.endsWith(".seq") || path.endsWith(".mpfa") || path.endsWith("faa") ||
                path.endsWith(".pep");
    }

    public static String formatFasta(String defline, String sequence, Integer width, String lineSeparator) throws IOException {
        StringWriter writer = new StringWriter(sequence.length());
        writeFormattedFasta(writer, defline, sequence, width, lineSeparator);
        return writer.toString();
    }

    public static String formatFasta(String defline, String sequence, Integer width) throws IOException {
        // Tareq: Changing this to use writeFormattedFasta per Leonid's request
        StringWriter writer = new StringWriter(sequence.length());
        writeFormattedFasta(writer, defline, sequence, width);
        return writer.toString();
    }

    public static String wrapDeflineAsText(String defline, int targetWidth, int indentDepth) {
        return wrapDefline(defline, targetWidth, indentDepth, " ", "\n");
    }

    public static String wrapDeflineAsHTML(String defline, int targetWidth, int indentDepth) {
        return wrapDefline(defline, targetWidth, indentDepth, "&nbsp;", "<br>");
    }

    public static String wrapDefline(String defline, int targetWidth, int indentDepth, String indentChar, String lineBreak) {
        if (defline == null)
            return ("");

        String[] tokens = defline.split("\\s+");
        if (tokens.length == 0)
            return ("");

        Integer lineWidth;
        StringBuffer out = new StringBuffer();
        StringBuffer indent = new StringBuffer();
        for (int i = 0; i < indentDepth; i++) // Create an indent
            indent.append(indentChar);

        out.append(tokens[0]);
        lineWidth = tokens[0].length();
        for (int i = 1; i < tokens.length; i++) {
            Integer tryLen = tokens[i].length();
            if (tryLen + lineWidth < targetWidth - indentDepth) {
                out.append(" ").append(tokens[i]);
                lineWidth += (tryLen + 1);
            }
            else {
                if (tryLen >= targetWidth - indentDepth) {
                    out.append(lineBreak).append(indent).append(tokens[i]).append(lineBreak).append(indent);
                    lineWidth = indentDepth;
                }
                else {
                    out.append(lineBreak).append(indent).append(tokens[i]);
                    lineWidth = tryLen + indentDepth;
                }
            }
        }

        return out.toString();
    }

    public String stripFirstAcc(String defline) {
        String splitOutput[] = defline.split(" ", 2);
        return ">" + splitOutput[1];
    }

    public static void writeFormattedFast(Writer writer, String defline, String sequence) throws IOException {
        writeFormattedFasta(writer, defline, sequence, -1);
    }

    public static void writeFormattedFasta(Writer writer, String defline, String sequence, int seqCharsPerLine) throws IOException {
        writeFormattedFasta(writer, defline, sequence, seqCharsPerLine, "\n");
    }

    public static void writeFormattedFasta(Writer writer, String defline, String sequence, int seqCharsPerLine, String lineSeparator)
            throws IOException {
        formatDefline(writer, defline, lineSeparator);
        formatSequence(writer, sequence, seqCharsPerLine, lineSeparator);
        writer.write(lineSeparator);
    }

    public static void formatDefline(Writer writer, String defline, String lineSeparator) throws IOException {
        if (defline == null || defline.length() == 0) {
            throw new IllegalArgumentException("Null or 0 lengthed defline.");
        }
        if (defline.charAt(0) != '>') {
            writer.write('>');
        }
        writer.write(defline);
        if (defline.charAt(defline.length() - 1) != '\n') {
            writer.write(lineSeparator);
        }
    }

    public static void formatSequence(Writer writer, String sequence, int seqCharsPerLine, String lineSeparator) throws IOException {
        for (int i = 0; i < sequence.length(); i++) {
            if (seqCharsPerLine > 0 && (i % seqCharsPerLine == 0) && i > 0) {
                writer.write(lineSeparator);
            }
            writer.write(sequence.charAt(i));
        }
    }

    public static String[] parseDeflineAndSequence(String fastaText) {
        String defLine = fastaText.substring(1, fastaText.indexOf("\n"));
        String sequence = fastaText.substring(fastaText.indexOf("\n"), fastaText.length());
        sequence = sequence.replaceAll("\\s", "");
        sequence = sequence.trim();
        return new String[]{defLine, sequence};
    }

    public static String determineSequenceType(File fastaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fastaFile));
        String firstSequence;
        try {
            reader.readLine();
            firstSequence = reader.readLine();
        }
        finally {
            reader.close();
        }
        return determineSequenceType(firstSequence, UseAmbiguityCodes);
    }

    public static String determineSequenceType(String sequence) {
        return determineSequenceType(sequence, UseAmbiguityCodes);
    }

    public static long[] findSequenceCountAndTotalLength(String fastaText) throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(fastaText));
        return findSequenceCountAndTotalLength(br);
    }

    public static long[] findSequenceCountAndTotalLength(File fastaFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fastaFile));
        return findSequenceCountAndTotalLength(br);
    }

    public static long[] findSequenceCountAndTotalLength(BufferedReader br) throws IOException {
        long sequenceCount = 0;
        long sequenceLength = 0;
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith(">")) {
                    sequenceCount++;
                }
                else {
                    sequenceLength += line.length();
                }
            }
        }
        finally {
            br.close();
        }
        return new long[]{sequenceCount, sequenceLength};
    }

    private static String determineSequenceType(String sequence, boolean useAmbiguityCodes) {
        if (sequence == null) {
            return SequenceType.NUCLEOTIDE;
        }
        if (sequence.trim().length() == 0) {
            return SequenceType.NUCLEOTIDE;
        }
        char[] sequenceChars = sequence.toCharArray();
        int nScore = 0;
        int pScore = 0;
        int nScoreWithAmb = 0;
        int checkLength = 1000; // arbitrary
        if (sequenceChars.length < checkLength) {
            checkLength = sequenceChars.length;
        }
        HashSet<Character> nucleotideCodes = new HashSet<Character>();
        HashSet<Character> nucleotideCodesWithAmb = new HashSet<Character>();
        nucleotideCodes.addAll(Arrays.asList(
                'A', 'C', 'G', 'T', 'U', 'N'
        ));
        nucleotideCodesWithAmb.addAll(nucleotideCodes);
        nucleotideCodesWithAmb.addAll(Arrays.asList(
                'B', 'D', 'H', 'K', 'M', 'R', 'S', 'V', 'W', 'Y', 'X'
        ));
        for (int i = 0; i < checkLength; i++) {
            char c = Character.toUpperCase(sequenceChars[i]);
            if (nucleotideCodes.contains(c)) {
                nScore++;
                pScore++;
            }
            else {
                pScore++;
            }
            if (nucleotideCodesWithAmb.contains(c)) {
                nScoreWithAmb++;
            }
        }
        if (pScore > nScore) {
            // If there are a non-trivial count of characters, and all characters
            // belong to nucleotide ambiguity space, and >90% of the characters
            // are 'regular' nucleotide characters, then assume 'nucleotide'.
            float percAmbScore = ((float) nScoreWithAmb - nScore) / (float) nScore;
            if (useAmbiguityCodes &&
                    pScore == nScoreWithAmb &&
                    pScore > 16 &&
                    percAmbScore < 0.10) {
                return SequenceType.NUCLEOTIDE;
            }
            return SequenceType.PEPTIDE;
        }
        else {
            return SequenceType.NUCLEOTIDE;
        }
    }


}

