
package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.compute.service.export.writers.ExportWriter;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 30, 2008
 * Time: 3:01:55 PM
 */
public class BseCsvWriter {
    ExportWriter writer;
    List<? extends BaseSequenceEntity> bseList;

    public BseCsvWriter(ExportWriter writer, List<? extends BaseSequenceEntity> bseList) {
        this.writer = writer;
        this.bseList = bseList;
    }

    public void write() throws IOException {
        List<String> headerList = new ArrayList<String>();
        headerList.addAll(BseColumnFormatter.getHeaderList());
        headerList.addAll(SampleColumnFormatter.getHeaderList());
        writer.writeItem(headerList);
        for (BaseSequenceEntity bse : bseList) {
            List<String> colList = new ArrayList<String>();
            colList.addAll(BseColumnFormatter.formatColumns(bse));
            colList.addAll(SampleColumnFormatter.formatColumns(bse.getSample()));
            writer.writeItem(colList);
        }
    }
}
