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

package org.janelia.it.jacs.web.control.blast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.*;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.UploadUserSequencePanel;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 8, 2006
 * Time: 1:30:24 PM
 */
public class FileUploadController extends JcviGWTSpringController {

    static Logger logger = Logger.getLogger(FileUploadController.class.getName());
    private static final String UPLOAD_SCRATCH_DIR_PROP = "Upload.ScratchDir";
    private static final String BLAST_QUERYFILE_UNIQUE_SEQUENCE_COUNT = "BlastQueryFile.UniqueSequenceCount";

    public ModelAndView handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        try {
            logger.info("Got inside the FileUploadController. Request type:" + httpServletRequest.getClass().getSimpleName());
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) httpServletRequest;
            String userNodeName = multipartRequest.getParameter(UploadUserSequencePanel.UPLOAD_SEQUENCE_NAME_PARAM);
            for (Iterator iterator = multipartRequest.getFileNames(); iterator.hasNext();) {
                String tmpFileKey = (String) iterator.next();
                MultipartFile tmpFile = multipartRequest.getFile(tmpFileKey);
                String tmpFileName = tmpFile.getOriginalFilename();
                String returnMessage;
                //Return the file text
                httpServletResponse.setContentType("text/plain");
                if (tmpFileName == null || tmpFileName.length() == 0) {
                    httpServletResponse.getWriter().write("Error - No file name provided.");
                    return null;
                }
                InputStream fileStream = tmpFile.getInputStream();
                if (fileStream == null || fileStream.available() == 0) {
                    httpServletResponse.getWriter().write("Error - No data in file: " + tmpFileName);
                    break;
                }

                String sequenceType = "";
                boolean error = false;
                String errorMessage = null;
                String fileExtension = "";
                String tmpFilename = "";
                if (tmpFileName.lastIndexOf(".") > 0) {
                    fileExtension = tmpFileName.substring(tmpFileName.lastIndexOf(".") + 1);
                }
                // Check for the FASTA files
                if (Constants.EXTENSION_FA.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_FAA.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_MPFA.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_FFN.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_FASTA.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_FNA.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_SEQ.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_FSA.equalsIgnoreCase(fileExtension)||
                        Constants.EXTENSION_PEP.equalsIgnoreCase(fileExtension)) {
                    FastaFileInfo info;
                    try {
                        info = createTmpFastaFile(fileStream);
                        if (info != null) {
                            if (info.errorMessage == null) {
                                sequenceType = info.type;
                                FastaFileNode fastaFileNode = createFastaFileNodeFromInfo(info, userNodeName);
                                // todo this is in a for-loop.  The line below breaks for multiple files
                                tmpFilename = info.filename;
                                httpServletRequest.getSession().setAttribute(Constants.UPLOADED_FILE_NODE_KEY, fastaFileNode);
                            }
                            else {
                                error = true;
                                errorMessage = info.errorMessage;
                            }
                        }
                    }
                    catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        error = true;
                        errorMessage = e.getMessage();
                    }
                }
                else if (Constants.EXTENSION_FRG.equalsIgnoreCase(fileExtension)) {
                    tmpFilename = createTmpFile(tmpFileName, fileStream);
                    FragmentFileNode tmpFragmentFileNode = new FragmentFileNode(null,
                            null, tmpFileName.substring(0, tmpFileName.lastIndexOf(".")),
                            "Fragment file", Node.VISIBILITY_PRIVATE, "",
                            0, null);
                    tmpFragmentFileNode.setPathOverride(tmpFilename);
                    httpServletRequest.getSession().setAttribute(Constants.UPLOADED_FILE_NODE_KEY, tmpFragmentFileNode);
                }
                // GenericService text files
                else if (Constants.EXTENSION_TXT.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_PROF.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_QUAL.equalsIgnoreCase(fileExtension) ||
                        Constants.EXTENSION_ALN.equalsIgnoreCase(fileExtension)) {
                    tmpFilename = createTmpFile(tmpFileName, fileStream);

                    GenericFileNode genericFileNode = new GenericFileNode(null, /* owner */
                            null, /* task */
                            tmpFileName.substring(0, tmpFileName.lastIndexOf(".")), /* filename */
                            "File uploaded on " + new Date(), /* description */
                            Node.VISIBILITY_PRIVATE, /* visibility */
                            "generic", /* sequence type */
                            null);
                    genericFileNode.setPathOverride(tmpFilename);
                    httpServletRequest.getSession().setAttribute(Constants.UPLOADED_FILE_NODE_KEY, genericFileNode);
                }
                // PDF Files
                else if (Constants.EXTENSION_PDF.equalsIgnoreCase(fileExtension)) {
                    tmpFilename = createTmpFile(tmpFileName, fileStream);

                    GenericPDFFileNode genericPDFFileNode = new GenericPDFFileNode(null, /* owner */
                            null, /* task */
                            tmpFileName.substring(0, tmpFileName.lastIndexOf(".")), /* filename */
                            "PDF uploaded on " + new Date(), /* description */
                            Node.VISIBILITY_PRIVATE, /* visibility */
                            "generic_pdf", /* sequence type */
                            null);
                    genericPDFFileNode.setPathOverride(tmpFilename);
                    httpServletRequest.getSession().setAttribute(Constants.UPLOADED_FILE_NODE_KEY, genericPDFFileNode);
                }
                else {
                    error = true;
                    errorMessage = "Do not recognize upload file extension type=" + fileExtension;
                }

                // Now check for errors
                if (!error) {
                    returnMessage = Constants.OUTER_TEXT_SEPARATOR +
                            Constants.UPLOADED_FILE_NODE_KEY + Constants.INNER_TEXT_SEPARATOR +
                            sequenceType + Constants.INNER_TEXT_SEPARATOR + tmpFilename +
                            Constants.OUTER_TEXT_SEPARATOR;
                }
                else {
                    returnMessage = Constants.ERROR_TEXT_SEPARATOR +
                            (errorMessage != null ?
                                    errorMessage :
                                    "Unexpected error encounter in FileUploadController") +
                            Constants.ERROR_TEXT_SEPARATOR;
                }
                httpServletResponse.getWriter().write(returnMessage);
            } // end for loop
        }
        catch (Exception e) {
            logger.error("Error in FileUploadController: \n" + e.getMessage(), e);
            httpServletResponse.getWriter().write(Constants.ERROR_TEXT_SEPARATOR +
                    e.getMessage() +
                    Constants.ERROR_TEXT_SEPARATOR);
        }
        return null;
    }

    public static FastaFileNode createFastaFileNodeFromInfo(FastaFileInfo info, String description) {
        FastaFileNode fastaFileNode = null;
        if (info != null && info.errorMessage == null) {
            String sequenceType = info.type;
            fastaFileNode = new FastaFileNode(null /* user */,
                    null /* task */,
                    info.filename,
                    "Sequence uploaded on " + new Date(),
                    Node.VISIBILITY_PRIVATE,
                    sequenceType,
                    info.queryCount, null);
            fastaFileNode.setLength(info.querySequenceLength);
            fastaFileNode.setDescription(description);
        }
        return fastaFileNode;
    }

    /* This method takes a text input stream, creates a temporary file in a scratch area
     * and then returns a temporary filename.
     */
    public static String createTmpFile(String tmpFileName, InputStream inputStream) throws Exception {
        SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
        String tmpDirectoryName = properties.getProperty(UPLOAD_SCRATCH_DIR_PROP);
        File tmpDirectory = new File(tmpDirectoryName);
        if (tmpDirectory.exists()) {
            logger.info("Successfully located directory=" + tmpDirectory.getAbsolutePath());
        }
        else {
            throw new Exception("Could not find directory " + tmpDirectory.getAbsolutePath());
        }

        int tries = 0;
        File tmpUploadFile = null;
        while (tries < 3) {
            tmpUploadFile = new File(tmpDirectory, "upload_" + new Date().getTime() + "_" + tmpFileName);
            if (!tmpUploadFile.exists())
                break;
            // If we get here, this means the file already exists - some other process has created it, etc.
            tries++;
        }
        if (tries > 2 || tmpUploadFile == null) {
            throw new Exception("Could not create temporary upload file=" + (tmpUploadFile == null ?
                    "" :
                    tmpUploadFile.getAbsolutePath()) +
                    " in directory=" + tmpDirectory.getAbsolutePath());
        }

        FileWriter fileWriter = new FileWriter(tmpUploadFile);
        BufferedWriter bw = new BufferedWriter(fileWriter);
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        try {
            while ((line = br.readLine()) != null) {
                bw.write(line + "\n");
            }
        }
        finally {
            try {
                br.close();
            }
            catch (Exception ignore) {
                logger.warn("Error closing the input stream");
            }
            try {
                bw.close();
            }
            catch (IOException ignore) {
                logger.warn("Error closing the output stream");
            }
        }
        return tmpUploadFile.getName();
    }

    /* This method takes a text input stream, creates a temporary fasta file in a scratch area
     * and then returns a temporary filename.
     */
    public static FastaFileInfo createTmpFastaFile(InputStream inputStream) throws Exception {
        SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
        String tmpDirectoryName = properties.getProperty(UPLOAD_SCRATCH_DIR_PROP);
        File tmpDirectory = new File(tmpDirectoryName);
        if (tmpDirectory.exists()) {
            logger.info("Successfully located directory=" + tmpDirectory.getAbsolutePath());
        }
        else {
            throw new Exception("Could not find directory " + tmpDirectory.getAbsolutePath());
        }
        int tries = 0;
        File tmpUploadFile = null;
        while (tries < 3) {
            tmpUploadFile = new File(tmpDirectory, "upload_" + new Date().getTime() + ".fasta");
            if (!tmpUploadFile.exists())
                break;
            // If we get here, this means the file already exists - some other process has created it, etc.
            tries++;
        }
        if (tries > 2 || tmpUploadFile == null) {
            throw new Exception("Could not create temporary upload file=" + (tmpUploadFile == null ?
                    "" :
                    tmpUploadFile.getAbsolutePath()) +
                    " in directory=" + tmpDirectory.getAbsolutePath());
        }

        FileWriter fileWriter = new FileWriter(tmpUploadFile);
        BufferedWriter bw = new BufferedWriter(fileWriter);
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(reader);
        String line;
        StringBuffer typeBuffer = new StringBuffer();
        int typeCount = 0;
        int queryCount = 0;
        long querySequenceLength = 0;
        String fastaUploadErrorMessage = null;
        int lineNumber = 1;


        // Map to hold the unique Fasta identifiers
        Vector<String> fastaIdenteVec = new Vector<String>();

        // Number of fasta entries to be considered for validating for unique identifiers
        int queryFileUniqSeqCount = SystemConfigurationProperties.getInt(BLAST_QUERYFILE_UNIQUE_SEQUENCE_COUNT);

        try {
            Pattern spacePattern;
            Pattern unPrintableCharPattern;
            String trimmedLine;
            boolean defline;

            for (; (line = br.readLine()) != null; lineNumber++) {
                trimmedLine = line.trim();
                if (trimmedLine.length() == 0) {
                    // skip empty lines
                    continue;
                }

                if (queryCount == 0) {
                    // the first line must be a defline
                    if (trimmedLine.charAt(0) != '>') {
                        // this is not a valid defline
                        // therefore not valid fasta file stop right here
                        fastaUploadErrorMessage = "Defline expected - line: " + String.valueOf(lineNumber);
                        break;
                    }
                }

                if (trimmedLine.charAt(0) == '>') {
                    defline = true;

                    if (trimmedLine.length() == 1) {
                        fastaUploadErrorMessage = "Empty defline - line: " + String.valueOf(lineNumber);
                        break;
                    }
                    else {

                        if (queryCount < queryFileUniqSeqCount) {
                            // The lengt of the trimmed line is greater than 1. Now, for the first 1000
                            // fasta record, read the 1st word after the '>'
                            // Which should be the fasta record identifier, which should be unique.


                            // Create a pattern to match breaks with spaces
                            spacePattern = Pattern.compile("[\\s]+");

                            // Split the trimmed line with the pattern
                            String[] result = spacePattern.split(trimmedLine);

                            // The first element of the result should be the identifier of the FASTA record
                            // and it should be uniq. Check if this identifier already exists in the identifier map
                            // If does not exist, add to the map. If it exists, throw an excpetion saying
                            // that the FASTA file does not have unique identifiers.

                            if (fastaIdenteVec.contains(result[0])) {
                                // This is a deplicate fasta identifier, throw an exception.
                                throw new Exception(" Invalid Query FASTA file. Duplicate identifiers found.");
                            }
                            else {
                                // Add the identifier the Fasta identifier vector
                                fastaIdenteVec.add(result[0]);
                            }


                            // Now, check if the line contains any un-printable characters. If does, throw an
                            // exception.

                            unPrintableCharPattern = Pattern.compile("[\t\n\r\f]");
                            Matcher m = unPrintableCharPattern.matcher(trimmedLine);

                            if (m.find()) {
                                // The current line has match for a un-printable character, thow an exception
                                throw new Exception(" Invalid Query FASTA file. Unprintable characters found in defline");
                            }

                        }

                    } // end else

                    queryCount++;
                }
                else {
                    defline = false;
                    querySequenceLength += trimmedLine.length();
                }

                if (typeCount < 1000 && !defline) {
                    typeBuffer.append(trimmedLine);
                    typeCount += trimmedLine.length();
                }
                bw.write(line + "\n");
            }
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                }
                catch (Exception ignore) {
                    logger.warn("Error closing the input stream");
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                }
                catch (IOException ignore) {
                    logger.warn("Error closing the output stream");
                }
            }
        }
        String sequenceType = FastaUtil.determineSequenceType(typeBuffer.toString());
        if (fastaUploadErrorMessage != null) {
            try {
                if (tmpUploadFile.exists()) {
                    tmpUploadFile.delete();
                }
            }
            catch (Exception ignore) {
                logger.warn("Error deleting the temporary upload file");
            }
        }
        return new FastaFileInfo(sequenceType,
                queryCount,
                querySequenceLength,
                fastaUploadErrorMessage,
                tmpUploadFile.getName());
    }

    public static class FastaFileInfo {
        String type;
        int queryCount;
        long querySequenceLength;
        String errorMessage;
        String filename;

        FastaFileInfo(String type, int queryCount, long querySequenceLength, String errorMessage, String filename) {
            this.type = type;
            this.queryCount = queryCount;
            this.querySequenceLength = querySequenceLength;
            this.errorMessage = errorMessage;
            this.filename = filename;
        }

    }

}

