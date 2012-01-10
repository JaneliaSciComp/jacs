
package org.janelia.it.jacs.compute.service.common.zip;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This service is responsible for zipping files.  It currently does not maintain the path of the zip entries.
 * <p/>
 * It zips the files found in sourceDirs that matched sourceFilePattern and writes them to
 * a zip file called zipFileName in zipFileDestDir.  This service's source file search is not
 * recursive; it does not attempt to
 * <p/>
 * Inputs: sourceDir, sourceFilePattern, zipFileDestDir, zipFileName
 * Output: ZIP_OUTPUT
 *
 * @author Tareq Nabeel
 */
public class ZipService implements IService {

    private Logger logger;

    private enum ZipMode {
        COMPRESS, DECOMPRESS
    }

    private List<File> sourceDirs = new ArrayList<File>();
    private Pattern sourceFilePattern;
    private File zipFile;
    private final static int BYTE_ARRAY_BUFF_SIZE = 1000000;    // 1 MB

    private ZipMode zipMode = ZipMode.COMPRESS;

    public ZipService() {
    }

    public void execute(IProcessData processData) throws ZipException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            switch (zipMode) {
                case COMPRESS:
                    zipFiles();
                    processData.putItem("ZIP_OUTPUT", zipFile.getAbsolutePath());
                    break;
                case DECOMPRESS:
                    throw new IllegalArgumentException("Mode " + zipMode + " is not supported at this time");
            }
        }
        catch (ZipException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ZipException(e);
        }
    }

    /**
     * @param processData
     * @throws MissingDataException
     * @throws IOException
     */
    protected void init(IProcessData processData) throws MissingDataException, IOException {
        setMode(processData);

        setSourceDir(processData);
        // The file pattern to match on within each sourceDir
        sourceFilePattern = Pattern.compile((String) processData.getMandatoryItem("sourceFilePattern"));
        // The destination directory of zip file
        String zipFileDestDir = (String) processData.getMandatoryItem("zipFileDestDir");
        // The zip file name (excluding file path)
        String zipFileName = (String) processData.getMandatoryItem("zipFileName");

        File zipDestDir = FileUtil.ensureDirExists(zipFileDestDir);
        zipFile = FileUtil.createNewFile(zipDestDir.getAbsolutePath() + File.separator + zipFileName);
    }

    /**
     * This method zips the files found in <code>sourceDirs</code> that matched <code>sourceFilePattern</code>
     * and writes them to a zip file called <code>zipFileName</code> in <code>zipFileDestDir</code>
     *
     * @throws ZipException
     * @throws IOException
     */
    private void zipFiles() throws ZipException, IOException {
        ZipOutputStream zipOuputStream = new ZipOutputStream(new FileOutputStream(zipFile));
        for (File sourceDir : sourceDirs) {
            File[] sourceFiles = sourceDir.listFiles(new SourceFileFilter());
            if (sourceFiles != null) {
                for (File sourceFile : sourceFiles) {
                    ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
                    zipOuputStream.putNextEntry(zipEntry);
                    if (logger.isInfoEnabled()) {
                        logger.info("Adding zip entry " + sourceFile.getAbsolutePath() + " to " + zipFile.getAbsolutePath());
                    }
                    // sourceFile could be large. Don't load all in memory
                    // byte[] fileContents = FileUtil.getFileContentsAsByteArray(sourceFile);
                    if (sourceFile.length() > BYTE_ARRAY_BUFF_SIZE) {
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile), BYTE_ARRAY_BUFF_SIZE);
                        byte[] buf = new byte[BYTE_ARRAY_BUFF_SIZE];
                        int b;
                        while ((b = bis.read(buf, 0, BYTE_ARRAY_BUFF_SIZE)) != -1) {
                            zipOuputStream.write(buf, 0, b);
                        }
                        bis.close();

                    }
                    else {
                        byte[] fileContents = FileUtil.getFileContentsAsByteArray(sourceFile);
                        zipOuputStream.write(fileContents);
                    }
                    zipOuputStream.closeEntry();
                }
            }
        }
        zipOuputStream.close();
    }

    // make sure target filename matches filter pattern and
    // 1. the target filename isn't a directory
    // 2. the target filename isn't the output file
    private class SourceFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            boolean filterResult = false;
            if (sourceFilePattern.matcher(name).matches()) {
                File targetFile = new File(dir, name);
                if (targetFile.isFile()
                        && (zipFile.compareTo(new File(dir, name)) != 0)) {
                    filterResult = true;
                }
            }
            return filterResult;
        }
    }

    private void setSourceDir(IProcessData processData) throws MissingDataException, IOException {
        // One or more source directories must be supplied as input
        Object sourceDirObj = processData.getMandatoryItem("sourceDir");
        if (sourceDirObj instanceof String) {
            sourceDirs.add(FileUtil.checkFileExists((String) sourceDirObj));
        }
        else if (sourceDirObj instanceof File) {
            sourceDirs.add((File) sourceDirObj);
        }
        else if (sourceDirObj instanceof List) {
            sourceDirs = (List<File>) sourceDirObj;
        }
    }

    private void setMode(IProcessData processData) throws MissingDataException {
        String operation = (String) processData.getMandatoryItem("mode");
        if (ZipMode.COMPRESS.toString().equalsIgnoreCase(operation)) {
            this.zipMode = ZipMode.COMPRESS;
        }
        else if (ZipMode.DECOMPRESS.toString().equalsIgnoreCase(operation)) {
            this.zipMode = ZipMode.DECOMPRESS;
        }
        else {
            throw new IllegalArgumentException("Invalid mode: " + operation + ".  Valid modes include: " + Arrays.toString(ZipMode.values()));
        }
    }
}
