
package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.tasks.export.SequenceExportTask;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.jobs.AsyncExportTaskController;
import org.janelia.it.jacs.web.gwt.common.client.popup.download.DownloadPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.ArrayList;

/**
 * @author Cristian Goina
 */
public class DownloadSequencePopup extends DownloadPopup {
    private BaseSequenceEntity sequenceEntity;
    private String description;

    public DownloadSequencePopup(BaseSequenceEntity sequenceEntity,
                                 String description) {
        this(sequenceEntity, description, false);
    }

    public DownloadSequencePopup(BaseSequenceEntity sequenceEntity,
                                 String description,
                                 boolean realizeNow) {
        super("Download Sequence", realizeNow);
        this.sequenceEntity = sequenceEntity;
        this.description = description;
    }

    protected void populateContent() {
        if (sequenceEntity == null) {
            add(HtmlUtils.getHtml("No entity has been provided", "error"));
        }
        else {
            addDescription(description, sequenceEntity.getAccession());
            add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
            add(createDownloadLink("Download as",
                    new Link(ExportWriterConstants.EXPORT_TYPE_TEXT, new DownloadSequenceClickListener(ExportWriterConstants.EXPORT_TYPE_TEXT))));
            add(createDownloadLink("Download as",
                    new Link(ExportWriterConstants.COMPRESSION_ZIP, new DownloadSequenceClickListener(ExportWriterConstants.COMPRESSION_ZIP))));
            add(createDownloadLink("Download as",
                    new Link(ExportWriterConstants.COMPRESSION_GZ, new DownloadSequenceClickListener(ExportWriterConstants.COMPRESSION_GZ))));
        }
        // Here's how to get rid of that pesky popup!
        addCloseLink();
    }

    private class DownloadSequenceClickListener implements ClickListener {
        private String outputType;

        private DownloadSequenceClickListener(String outputType) {
            this.outputType = outputType;
        }

        public void onClick(Widget w) {
            SystemWebTracker.trackActivity("DownloadSequence",
                    new String[]{
                            description,
                            outputType
                    });
            ArrayList<String> accessionList = new ArrayList<String>();
            accessionList.add(sequenceEntity.getAccession());
            SequenceExportTask exportTask = new SequenceExportTask(ExportWriterConstants.EXPORT_TYPE_FASTA, accessionList, null);
            if (ExportWriterConstants.COMPRESSION_ZIP.equals(outputType) ||
                    ExportWriterConstants.COMPRESSION_GZ.equals(outputType)) {
                exportTask.setSuggestedCompressionType(outputType);
            }
            new AsyncExportTaskController(exportTask).start();
        }
    }

}
