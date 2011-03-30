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

package org.janelia.it.jacs.web.control.download;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.server.access.FileNodeDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Scanner;

/**
 * User: lfoster
 * Date: Sep 27, 2006
 * Time: 4:52:40 PM
 * <p/>
 * Spring controller to setup an FTP delivery location and URL and redirect to it.
 */
public class FileDeliveryController implements Controller {

    private static final int BUFSIZE = 8096;
    private String _baseFileLocation;
    private String _ftpFileLocation;
    private String _ftpHostLocation;
    private String _ftpLinkTargetLocation;
    private static Logger log = Logger.getLogger(FileDeliveryController.class);
    private FileNodeDAO fileNodeDAO;
    private String _filestoreBaseFileLocation;
    private String _filestoreFtpLinkTargetLocation;
    private ComputeBeanRemote computeBean;

    /**
     * Implementation for Controller.  Literally handles the request, by sending the file that
     * was asked-for in the request, to the output stream.
     *
     * @param request  a servlet request.
     * @param response a servlet response.
     * @return null, to indicate handling locally.
     * @throws ServletException Ignore
     * @throws IOException      from called method.
     */
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String taskId = request.getParameter("taskId");
        request.getRemoteUser();
        try {
            // Looking for the generic implementation first - URL's which passed parameter taskId
            if (null != taskId && !"".equals(taskId)) {
                FileNode[] resultNodes = fileNodeDAO.getResultFileNodesByTaskId(Long.valueOf(taskId));
                // Since the export task should only have one result node we can look for it directly
                if (null == resultNodes[0]) {
                    log.error("There is no ExportFileNode to export bytes from.");
                    //throw new ServletException("Unable to stream back the file.  It cannot be found.");
                }
                File exportDir = new File(resultNodes[0].getDirectoryPath());
                log.info("FileDeliveryController taskId=" + taskId + " resultExportDir=" + exportDir.getAbsolutePath());
                File[] fileList = exportDir.listFiles();
                File targetFile = null;
                // todo : refactor this special case out into a new Export task
                if (fileList.length > 1) {
                    for (File f : fileList) {
                        if (f.getName().equals("blastResults.zip")) {
                            log.info("Using targetFile=" + f.getAbsolutePath());
                            targetFile = f;
                        }
                    }
                }
                if (targetFile == null && 1 != fileList.length) {
                    String errorMsg = "Expecting find one and only one file in the export location.";
                    log.error(errorMsg);
                    //throw new ServletException(errorMsg);
                }
                else if (targetFile == null) {
                    targetFile = fileList[0];
                }
                // If the export file is a link to another file, use the name and stream back
                String filename;
                String absolutePath;
                String targetNodeId;
                if (ExportFileNode.EXTERNAL_LINK_FILE.equals(targetFile.getName())) {
                    Scanner scanner = new Scanner(targetFile);
                    String[] pieces = scanner.nextLine().split("\t");
                    targetNodeId = pieces[0];
                    FileNode targetNode = fileNodeDAO.getFileNodeById(Long.valueOf(targetNodeId));
                    absolutePath = targetNode.getDirectoryPath() + File.separator + pieces[1];
                    if (!(new File(absolutePath)).exists()) {
                        throw new IOException("File not found.");
                    }
                    filename = pieces[2];
                }
                else {
                    absolutePath = targetFile.getAbsolutePath();
                    filename = targetFile.getName();
                }
                doDownload(response, filename, absolutePath);
                return null;
            }
        }
        catch (DaoException e) {
            log.error("Unable to return the file requested from task " + taskId);
            return null;
        }

        // If you get this far, parameter "taskId" was not passed, thus we need to ship files another way
        String originalFileName = request.getParameter("inputfilename");
        String suggestedFileName = request.getParameter("suggestedfilename");
        String filestoreBacked = request.getParameter("filestoreBacked");
        boolean isFilestoreBacked = (null == filestoreBacked || "".equals(filestoreBacked)) ? false : Boolean.valueOf(filestoreBacked);

        if (null != originalFileName && !"".equals(originalFileName)) {
            // NOTE: I don't think _ftpHostLocation is ever null...
            if (_ftpHostLocation == null)
                doDownload(response, suggestedFileName, originalFileName);
            else
                doFtp(response, suggestedFileName, originalFileName, isFilestoreBacked);
        }

        // Maybe you just want to export a file from a file node. Used by - ap16s pipeline results
        String nodeTaskId = request.getParameter("nodeTaskId");
        String filetag = request.getParameter("fileTag");

        if (null != nodeTaskId && !"".equals(nodeTaskId)) {
            try {
                FileNode[] tmpFileNode = fileNodeDAO.getResultFileNodesByTaskId(Long.valueOf(nodeTaskId));
                if (1 != tmpFileNode.length) {
                    return null;
                }
                FileNode targetFileNode = tmpFileNode[0];
                String targetPath = null;
                // NOTE:  This assumes that the filestore available to the web server is the same as the compute server
                boolean deleteAfterStream = false;
                // Check for the case where the entire node is requested
                if ("archive".equalsIgnoreCase(filetag)) {
                    try {
                        String targetFileName = computeBean.createTemporaryFileNodeArchive("archive", targetFileNode.getObjectId().toString());
                        targetPath = targetFileNode.getDirectoryPath() + File.separator + targetFileName;
                        deleteAfterStream = true;
                    }
                    catch (RemoteException e) {
                        log.error("Error trying to move the files via archive:" + e.getMessage(), e);
                        return null;
                    }
                }
                else {
                    targetPath = targetFileNode.getFilePathByTag(filetag);
                }
                File tmpFile = new File(targetPath);
                doDownload(response, tmpFile.getName(), tmpFile.getAbsolutePath());
                if (deleteAfterStream) {
                    tmpFile.delete();
                    log.debug("Temporary archive file deleted.");
                }
            }
            catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        return null;        // Signal: am handling this operation here.

    }

    /**
     * Dependency: Need to be told what FTP server address to hit for information.
     * <p/>
     * Format: expect input to have partial URL:
     * ftp.camera.org
     * optional :port, as in ftp.camera.org:23/loc
     *
     * @param ftpHostLocation host:port/location format
     */
    public void setFtpHostLocation(String ftpHostLocation) {
// Later, can enforce this, for now just wish to be able to run devs on any platform.
//        if (ftpHost == null)
//            throw new IllegalArgumentException("No host provided");
//
        if (ftpHostLocation == null || ftpHostLocation.length() == 0)
            return;

        _ftpHostLocation = ftpHostLocation;

    }

    /**
     * Dependency: must be told where to place files for FTP access.
     * <p/>
     * Format: expect input to be a file-system location, and
     * if no trailing slash is given, provide one.
     *
     * @param ftpFileLocation - place to get the files for FTP
     */
    public void setFtpFileLocation(String ftpFileLocation) {
// Later, can enforce this.  For now, wish to be able to run devs on any platform.
//        if (ftpFileLocation == null)
//            throw new IllegalArgumentException("No host provided");
//
        if (ftpFileLocation == null || ftpFileLocation.length() == 0)
            return;

        String slash = "/"; // Assume to run on Unix O/S

        _ftpFileLocation = ftpFileLocation;
        if (!ftpFileLocation.endsWith(slash)) {
            _ftpFileLocation += slash;
        }
        if (!ftpFileLocation.startsWith(slash)) {
            _ftpFileLocation = slash + _ftpFileLocation;
        }
    }

    /**
     * Soft links may need to be specified as relative paths, to properly deal
     * with restrictions placed on an anonymous ftp account.  If so, set the
     * alternate value, here.  This setting will prefix link source locations,
     * as used in "ln -s source target", or put differently, the prefix will
     * be for the REAL FILE, as opposed to the soft link.
     *
     * @param ftpLinkTargetLocation prefix to softlink real file.
     */
    public void setFtpLinkTargetLocation(String ftpLinkTargetLocation) {
        _ftpLinkTargetLocation = ftpLinkTargetLocation;
    }

    /**
     * Dependency: on the proper location to test against (or prefix)
     * before finding a file to return.
     * <p/>
     * Format: expect input to have a trailing slash.  No attempt will be
     * made to establish starting slash, since this can vary from system
     * to system.
     *
     * @param baseFileLocation - base path to the files
     */
    public void setBaseFileLocation(String baseFileLocation) {
        // Note: do not expect to set this very often, so not caching the
        // separator.
        String trailingSlash = "/";
        String separatorPropertyName = "file.separator";
        try {
            trailingSlash = System.getProperty(separatorPropertyName);

        }
        catch (Exception ex) {
            // Avoid except-ing out, just because file sep could not
            // be established.
            log.error(ex.getMessage() +
                    ": Failed to retrieve system's " +
                    separatorPropertyName +
                    ".  Using " + trailingSlash);

        }
        if (!baseFileLocation.endsWith(trailingSlash)) {
            baseFileLocation += trailingSlash;
        }
        _baseFileLocation = baseFileLocation;
    }

    /**
     * Sends a file to the ServletResponse output stream. User
     * may wish to save under a different name.
     *
     * @param resp              The response
     * @param suggestedFileName The name of the file you want to download.
     * @param originalFileName  The name the browser should receive.
     * @throws javax.servlet.ServletException - error with the servlet behavior
     * @throws java.io.IOException            - error streaming the file
     */
    private void doDownload(
            HttpServletResponse resp,
            String suggestedFileName,
            String originalFileName) throws IOException, ServletException {

        log.info("doDownload called with originalFileName=" + originalFileName);
        File f = new File(originalFileName);
        // Export Nodes will have direct paths, all others will probably need the _baseFileLocation prefix
        if (!f.exists()) {
            f = new File(testAndGetFileName(originalFileName, false));
        }
        // Wait for file to exist and have non-zero size
        long maxWaitMs = 1000 * 60; // 60-seconds
        long startTime = new Date().getTime();
        long checkIntervalMs = 1000; // 1-second
        boolean checkFlag = false;
        long size = 0;
        while (!checkFlag && new Date().getTime() < (startTime + maxWaitMs)) {
            try {
                if (f.exists() && f.length() > 0) {
                    checkFlag = true;
                    size = f.length();
                }
                Thread.sleep(checkIntervalMs);
            }
            catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        if (checkFlag) {
            log.debug("Found file to deliver name=" + f.getAbsolutePath() + " size=" + size);
        }
        else {
            log.debug("Not able to confirm existence of file=" + f.getAbsolutePath());
        }

        int length;
        ServletOutputStream op = resp.getOutputStream();

        resp.setContentType("application/octet-stream");

        // These are needed to avoid a download bug in IE.
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "public");
        resp.setHeader("Cache-Control", "max-age=0");

        resp.setHeader("Content-Disposition", "attachment;filename=\"" + suggestedFileName + "\"");
        //
        // Stream to the requester.
        //
        byte[] bbuf = new byte[BUFSIZE];
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(f));
            while ((in != null) && ((length = in.read(bbuf)) != -1)) {
                op.write(bbuf, 0, length);
            }
        }
        catch (IOException ioe) {
            log.error("IO Error " + ioe + " during attempted download of " + f.getName());
        }
        finally {
            if (in != null)
                in.close();
            if (op != null)
                op.flush();
            if (op != null)
                op.close();
        }

    }

    /**
     * Sends a file to FTP server, and then handoff its link to user.
     *
     * @param resp              The response
     * @param suggestedFileName The name of the file you want to download.
     * @param originalFileName  The name the browser should receive.
     * @param isFilestoreBacked - boolean which tries to figure out if download is "official" data or user data from the
     *                          filestore, like for a large blast archive which needs ftp
     * @throws javax.servlet.ServletException - error with the servlet behavior
     * @throws java.io.IOException            - error streaming the file
     */
    private void doFtp(HttpServletResponse resp, String suggestedFileName, String originalFileName,
                       boolean isFilestoreBacked) throws IOException, ServletException {

        // Ensure actual file name is for something available to download.
        String fsFileName = testAndGetFileName(originalFileName, isFilestoreBacked);
        testFileAvailability(fsFileName, originalFileName);

        String fsLinkTo = testAndGetFtpLinkTarget(originalFileName, isFilestoreBacked);
        String randomLinkName = createTempLink(fsLinkTo, suggestedFileName);

        // Setup the FTP link against the temporary file name.
        StringBuffer ftpUrlBuf = new StringBuffer();
        ftpUrlBuf.append("ftp://");
        ftpUrlBuf.append(_ftpHostLocation);
        ftpUrlBuf.append(randomLinkName);

        log.info("FTP via URL: " + ftpUrlBuf.toString());

        // Finally, redirect to the FTP location:
        resp.sendRedirect(ftpUrlBuf.toString());
    }

    /**
     * Given an input filename, and a suggested alternative name, create a temporary,
     * symbolic (soft) link, from a random variant of the alternative name, and the
     * input filename.
     *
     * @param fsFileName        where the true file is located.
     * @param suggestedFileName suggested 'readable' name.
     * @return name of link created here.
     * @throws IOException                    thrown by link creation process in event of problem.
     * @throws javax.servlet.ServletException - error with the servlet behavior
     */
    private String createTempLink(String fsFileName, String suggestedFileName) throws IOException, ServletException {
        // Setup the file link.
        //   Copy the file from its 'discovered' location (where user found it), to
        //   a convenient place for download.
        //String randomLinkName = suggestedFileName + "_" + Long.toString(new Date().getTime());
        String randomLinkName = createRandomLinkName(suggestedFileName);

        // Link the random name to the fs name.
        StringBuffer command = new StringBuffer();
        command.append("ln ")
                .append(fsFileName)
                .append(" ")
                .append(_ftpFileLocation)
                .append(randomLinkName);
        Process p = Runtime.getRuntime().exec(command.toString());
        try {
            p.waitFor();
        }
        catch (InterruptedException ie) {
            log.warn("Command to create link: " + command.toString() + " was interrupted.");
        }
        if (p.exitValue() != 0)
            log.warn(
                    "Possible failure to run link-creation command: " +
                            command.toString() +
                            ", exit code " +
                            p.exitValue());

        // Finally, double check: new thing exists?
        if (!isExistingSymbolicLink(_ftpFileLocation + randomLinkName)) {
            String message = _ftpFileLocation + randomLinkName + " not created against " + fsFileName + " command:" + command;
            log.warn(message);
            throw new ServletException(message);
        }

        return randomLinkName;
    }

    /**
     * This method creates the random link name
     *
     * @param suggestedFileName - duh
     * @return the random link name
     * @throws java.io.IOException - error streaming the file
     */
    private String createRandomLinkName(String suggestedFileName) throws IOException {
        //String randomLinkName = suggestedFileName + "_" + Long.toString(new Date().getTime());

        //Extract the suggested path and file name
        int idxLastFileSeparator = suggestedFileName.lastIndexOf(FileUtil.FILE_SEPARATOR);
        String suggestedFilePath = suggestedFileName.substring(0, idxLastFileSeparator + 1);
        String fileName = suggestedFileName.substring(idxLastFileSeparator + 1);

        String randamLinkPath = suggestedFilePath + System.currentTimeMillis();
        // Create a random dir
        FileUtil.ensureDirExists(_ftpFileLocation + randamLinkPath);

        // Create the random link name
        return randamLinkPath + FileUtil.FILE_SEPARATOR + fileName;
    }

    /**
     * Returns T if link test succeeds, and F if fails.
     *
     * @param linkName what might be a symb-link
     * @return T/F of test
     */
    private boolean isExistingSymbolicLink(String linkName) {

        File tester = new File(linkName);
        return (tester.exists());
    }

    /**
     * Test: may this file be delivered?  Answer by challenging existence, readability, whether it actually
     * IS a file.
     *
     * @param fsFileName       expected file location.
     * @param originalFileName name as given in request.
     * @throws ServletException may be thrown if tests fail.
     * @throws IOException      may be thrown if tests fail.
     */
    private void testFileAvailability(String fsFileName, String originalFileName)
            throws ServletException, IOException {

        // Test: file exists, is a file, and is readable?
        File tester = new File(fsFileName);
        testExistsAndReadable(tester, fsFileName, originalFileName);
        if (!tester.isFile()) {
            log.error("Requested file " + fsFileName + " is not really a file");
            throw new ServletException("Attempt to download non-file " + originalFileName);
        }

    }

    /**
     * Convenience method.
     *
     * @param tester           file to look at.
     * @param fsFileName       some final file name.
     * @param originalFileName some chosen file name.
     * @throws ServletException thrown if fails test.
     * @throws IOException      thrown if fails test.
     */
    private void testExistsAndReadable(File tester, String fsFileName, String originalFileName)
            throws ServletException, IOException {

        // Test: file exists, is a file, and is readable?
        if (!tester.exists()) {
            log.error("Requested file " + originalFileName + " does not exist");
            throw new FileNotFoundException(fsFileName);
        }
        if (!tester.canRead()) {
            log.error("Requested file " + fsFileName + " is not readable by user controlling this application");
            throw new ServletException("Attempt to download protected resource " + originalFileName);
        }

    }

    /**
     * Look at the input filename.  If it does not pass a certain test, then
     * prefix something to it.  If it fails a certain other test, throw an exception.
     *
     * @param originalFileName  - original name of the file
     * @param isFilestoreBacked - boolean which tries to figure out if download is "official" data or user data from the
     *                          filestore, like for a large blast archive which needs ftp
     * @return the adjusted (if necessary) or original filename.
     * @throws ServletException if fails validity test.
     */
    private String testAndGetFtpLinkTarget(String originalFileName, boolean isFilestoreBacked) throws ServletException {
        // NOTE: at some point, may wish to make a soft link to a file.  That can be
        // done from here, as well.
        String returnName;
        try {
            if (originalFileName.indexOf("..") > -1) {
                log.error("Filename: " + originalFileName + " contains relative-path elipses, and cannot be trusted.");
                throw new IllegalArgumentException("Relative path provided for file fetch!");
            }
            else {
                String tmpLinkTargetLocation = isFilestoreBacked ? _filestoreFtpLinkTargetLocation : _ftpLinkTargetLocation;
                returnName = joinPrefixAndSuffix(originalFileName, tmpLinkTargetLocation);
            }
        }
        catch (Exception ex) {
            log.error("Test of the file name to fetch: " + originalFileName + " failed.");
            throw new ServletException(ex);
        }

        return returnName;
    }

    /**
     * Convenience for dealing with quirks in input file name format.
     *
     * @param originalFileName original file name
     * @param prefix           - path to add to the file name
     * @return complete path to the file for transport
     */
    private String joinPrefixAndSuffix(String originalFileName, String prefix) {
        // Avoid double-slash in middle of link target.
        if (originalFileName.startsWith("/") && prefix.endsWith("/")) {
            originalFileName = originalFileName.substring(1);
        }
        else if (!prefix.endsWith("/") && !originalFileName.startsWith("/")) {
            // No slashes between base and suffix.
            originalFileName = "/" + originalFileName;
        }
        return prefix + originalFileName;
    }

    /**
     * Look at the input filename.  If it does not pass a certain test, then
     * prefix something to it.  If it fails a certain other test, throw an exception.
     *
     * @param originalFileName  - original file name
     * @param isFilestoreBacked - boolean which tries to figure out if download is "official" data or user data from the
     *                          filestore, like for a large blast archive which needs ftp
     * @return the adjusted (if necessary) or original filename.
     * @throws ServletException if fails validity test.
     */
    private String testAndGetFileName(String originalFileName, boolean isFilestoreBacked) throws ServletException {
        // NOTE: at some point, may wish to make a soft link to a file.  That can be
        // done from here, as well.
        String returnName;
        try {
            if (originalFileName.indexOf("..") > -1) {
                log.error("Filename: " + originalFileName + " contains relative-path elipses, and cannot be trusted.");
                throw new IllegalArgumentException("Relative path provided for file fetch!");
            }
            else {
                String tmpPrefix = isFilestoreBacked ? _filestoreBaseFileLocation : _baseFileLocation;
                returnName = joinPrefixAndSuffix(originalFileName, tmpPrefix);
            }
        }
        catch (Exception ex) {
            log.error("Test of the file name to fetch: " + originalFileName + " failed.");
            throw new ServletException(ex);
        }

        return returnName;
    }

    public void setFileNodeDAO(org.janelia.it.jacs.server.access.hibernate.FileNodeDAOImpl fileNodeDAO) {
        this.fileNodeDAO = fileNodeDAO;
    }

    public void setFilestoreBaseFileLocation(String filestoreBaseFileLocation) {
        this._filestoreBaseFileLocation = filestoreBaseFileLocation;
    }

    public void setFilestoreFtpLinkTargetLocation(String filestoreFtpLinkTargetLocation) {
        this._filestoreFtpLinkTargetLocation = filestoreFtpLinkTargetLocation;
    }

    public void setComputeServerBean(org.janelia.it.jacs.compute.api.ComputeBeanRemote computeServerBean) {
        this.computeBean = computeServerBean;
    }
}
