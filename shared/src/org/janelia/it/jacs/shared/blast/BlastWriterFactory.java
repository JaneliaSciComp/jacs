
package org.janelia.it.jacs.shared.blast;

import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLWriter;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 18, 2009
 * Time: 1:36:11 PM
 */
public class BlastWriterFactory {
    public static BlastWriter getWriterForFormat(File parentDir, String format)
            throws Exception {
        BlastWriter bw;
        if (format.equals(BlastTask.FORMAT_XML)) {
//            _logger.debug("Returning ExportCVSWriter for type "+exportType);
            bw = new BlastXMLWriter();
        }
        else if (format.equals(BlastTask.FORMAT_BTAB)) {
//            _logger.debug("Returning ExportHtmlWriter for type "+exportType);
            bw = new BlastBtabWriter();
        }
        else if (format.equals(BlastTask.FORMAT_TAB)) {
            bw = new BlastNCBITabWriter();
        }
        else if (format.equals(BlastTask.FORMAT_TAB_WITH_HEADER)) {
            bw = new BlastNCBITabWithHeaderWriter();
        }
        else if (format.equals(BlastTask.FORMAT_TEXT)) {
//            _logger.debug("Returning ExportXmlWriter for type "+exportType);
            throw new Exception("TEXT Format is not yet available");
//            return new ExportXmlWriter(fullPathFilename, dataHeaders);
        }
        else {
//            _logger.error("Found no ExportWriter for type "+exportType);
            throw new Exception("Invalid format writer requested");
        }

        bw.init(new File(constructPath(parentDir, format)));
        return bw;
    }

    private static String constructPath(File parentDir, String format) {
        return parentDir.getAbsolutePath() + File.separator + BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + "." + format;
    }
}
