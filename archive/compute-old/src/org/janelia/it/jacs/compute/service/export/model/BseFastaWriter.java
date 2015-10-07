
package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.compute.service.export.writers.ExportFastaWriter;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 30, 2008
 * Time: 3:02:09 PM
 */
public class BseFastaWriter {
    ExportFastaWriter writer;
    List<? extends BaseSequenceEntity> bseList;

    public BseFastaWriter(ExportFastaWriter writer, List<? extends BaseSequenceEntity> bseList) {
        this.writer = writer;
        this.bseList = bseList;
    }

    public void write() throws IOException {
        for (BaseSequenceEntity bse : bseList) {
            List<String> pl = new ArrayList<String>();
            pl.add(bse.getDefline());
            pl.add(bse.getBioSequence().getSequence());
            writer.writeItem(pl);
        }
    }

}
