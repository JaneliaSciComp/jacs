
package org.janelia.it.jacs.compute.service.export.processor;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.export.writers.ExportWriter;
import org.janelia.it.jacs.compute.service.export.writers.ExportWriterFactory;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 27, 2008
 * Time: 10:28:19 AM
 */
public abstract class ExportProcessor {
    public Logger _logger;
    public static final String HOME_URL_PROP = "Header.HomeURL";

    protected ExportWriter exportWriter;
    protected ExportTask exportTask;
    protected ExportFileNode exportFileNode;
    protected String homeUrl;

    public ExportProcessor(ExportTask exportTask, ExportFileNode exportFileNode) throws IOException, MissingDataException {
        this.exportTask = exportTask;
        _logger = ProcessDataHelper.getLoggerForTask(exportTask.getObjectId().toString(), this.getClass());
        this.exportFileNode = exportFileNode;
        homeUrl = SystemConfigurationProperties.getInstance().getProperty(HOME_URL_PROP);
        initializeWriter();
    }

    protected void initializeWriter() throws IOException {
        this.exportWriter =
                ExportWriterFactory.getWriterForExportType(
                        exportFileNode.getDirectoryPath() + File.separator + exportTask.getSuggestedFilename(),
                        exportTask.getParameter(ExportTask.PARAM_EXPORT_FORMAT_TYPE),
                        getDataHeaders());

    }

    public void process() throws Exception {
        if (exportWriter != null)
            exportWriter.start();
        execute();
        if (exportWriter != null)
            exportWriter.end();
        applyCompressionToExportFiles();
    }

    public abstract void execute() throws Exception;

    public abstract String getProcessorType();

    protected abstract List<SortArgument> getDataHeaders();

    public ExportWriter getExportWriter() {
        return exportWriter;
    }

    public void setExportWriter(ExportWriter exportWriter) {
        this.exportWriter = exportWriter;
    }

    public String getSuggestedFilename(String defaultName, String defaultPrefix) {
        String filename = null;
        if (exportTask != null) {
            filename = exportTask.getParameter(ExportTask.PARAM_SUGGESTED_FILENAME);
            if (filename != null && filename.lastIndexOf(".") < 0 && defaultPrefix != null) {
                filename = filename + "." + defaultPrefix;
            }
        }
        if (filename == null) {
            filename = defaultName + "." + defaultPrefix;
        }
        return filename;
    }

    public void applyCompressionToExportFiles() throws Exception {
        String suggestedCompression = exportTask.getSuggestedCompressionType();
        if (suggestedCompression == null || suggestedCompression.equals(ExportWriterConstants.COMPRESSION_NONE)) {
            _logger.debug("Found no compression instructions");
        }
        else {
            _logger.debug("Compressing files with compression=" + suggestedCompression);
            File exportDirectory = new File(exportFileNode.getDirectoryPath());
            File[] exportFiles = exportDirectory.listFiles();
            int count = 0;
            for (File exportFile : exportFiles) {
                _logger.debug("Applying compression to file=" + exportFile.getAbsolutePath());
                if (suggestedCompression.equals(ExportWriterConstants.COMPRESSION_GZ)) {
                    FileUtil.gzCompress(exportFile);
                }
                else if (suggestedCompression.equals(ExportWriterConstants.COMPRESSION_ZIP)) {
                    FileUtil.zipCompress(exportFile);
                }
                else {
                    throw new Exception("Do not recognize compression type=" + suggestedCompression);
                }
                exportFile.delete();
                count++;
            }
            _logger.debug("Compression file count=" + count);
        }
    }

    public SortArgument[] getSortArgumentsAsArray() {
        List<SortArgument> saList = exportTask.getSortArguments();
        SortArgument[] saArr = null;
        if (saList != null) {
            saArr = new SortArgument[saList.size()];
            saArr = saList.toArray(saArr);
        }
        return saArr;
    }

}
