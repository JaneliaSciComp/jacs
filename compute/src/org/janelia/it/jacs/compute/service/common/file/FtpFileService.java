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

package org.janelia.it.jacs.compute.service.common.file;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.ftp.FtpClientFactory;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.FtpFileTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 16, 2009
 * Time: 4:42:31 PM
 */
public class FtpFileService implements IService {
    public enum FtpOperation {
        FETCH, LIST
    }

    private Logger logger;

    private String ftpSourceDirectory = "";
    private String targetDirectory;
    private String ftpServer;
    private Integer ftpPort;
    private char[] ftpLogin;
    private char[] ftpPassword;
    private List<String> targetExtensions = new ArrayList<String>();
    private FtpOperation fileOperation;

    public FtpFileService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            switch (fileOperation) {
                case FETCH:
                    getFiles();
                    break;
                case LIST:
                    listFiles();
                    break;
                default:
                    throw new IllegalArgumentException("Operation " + fileOperation + " is not supported at this time");
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Initialize the input parameters
     *
     * @param processData params of the task
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             cannot find data required to process
     * @throws java.io.IOException problem accessing file data
     */
    protected void init(IProcessData processData) throws MissingDataException, IOException {
        // implied key is a little weak to use
        ftpServer = processData.getString(FtpFileTask.PARAM_ftpServer);
        ftpPort = processData.getInt(FtpFileTask.PARAM_ftpPort);
        ftpLogin = processData.getString(FtpFileTask.PARAM_ftpLogin).toCharArray();
        ftpPassword = processData.getString(FtpFileTask.PARAM_ftpPassword).toCharArray();
        ftpSourceDirectory = ProcessDataHelper.getTask(processData).getParameter(FtpFileTask.PARAM_ftpSourceDirectory);
        setMode(processData);
    }

    private void setMode(IProcessData processData) throws MissingDataException {
        String operation = (String) processData.getMandatoryItem("mode");
        if (FtpOperation.FETCH.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FtpOperation.FETCH;
            targetExtensions = Task.listOfStringsFromCsvString(ProcessDataHelper.getTask(processData).getParameter(FtpFileTask.PARAM_targetExtensions));
            targetDirectory = ProcessDataHelper.getTask(processData).getParameter(FtpFileTask.PARAM_targetDirectory);
        }
        else if (FtpOperation.LIST.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FtpOperation.LIST;
        }
        else {
            throw new IllegalArgumentException("Invalid mode: " + operation + ".  Valid modes include: " + Arrays.toString(FtpOperation.values()));
        }
    }

    private void getFiles() throws ServiceException {
        // ftp to the location and grab by the extensions
        logger.debug("downloading files...");
        try {
            // Connect and logon to FTP Server
            FTPClient ftp = getFtpClient();

            // List the files in the directory
            FTPFile[] files = ftp.listFiles();
            logger.debug("Number of files in dir: " + files.length);
            for (FTPFile file1 : files) {
                if (extensionMatchesTarget(file1)) {
                    // Download a file from the FTP Server
                    logger.debug("File:" + file1.getName());
                    File file = new File(targetDirectory +
                            File.separator + file1.getName());
                    FileOutputStream fos = new FileOutputStream(file);
                    ftp.retrieveFile(file1.getName(), fos);
                    fos.close();
                }
            }

            // Logout from the FTP Server and disconnect
            ftp.logout();
            ftp.disconnect();

        }
        catch (Exception e) {
            throw new ServiceException("Failed to access ftp", e);
        }
    }

    private FTPClient getFtpClient() throws FileSystemException {
        FTPClient tmpClient = FtpClientFactory.createConnection(ftpServer, ftpPort, ftpLogin, ftpPassword,
                ftpSourceDirectory, new FileSystemOptions());
        logger.debug("Connected to " + ftpServer);
        logger.debug(tmpClient.getReplyString());
        return tmpClient;
    }

    private boolean extensionMatchesTarget(FTPFile file1) {
        for (String targetExtension : targetExtensions) {
            if (file1.getName().toLowerCase().endsWith(targetExtension)) {
                return true;
            }
        }
        return false;
    }

    private void listFiles() throws ServiceException {
        try {
            // List the files in the directory
            FTPClient ftp = getFtpClient();
            FTPFile[] files = ftp.listFiles();
            logger.debug("Number of files in dir: " + files.length);
        }
        catch (IOException e) {
            throw new ServiceException("Failed to access ftp", e);
        }
    }

}
