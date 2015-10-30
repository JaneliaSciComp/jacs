
package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 30, 2008
 * Time: 3:01:40 PM
 */
public class BseColumnFormatter extends ColumnFormatter {
    public static final Map<BseHeader, String> headerMap = new HashMap<BseHeader, String>();

    public static enum BseHeader {
        ACCESSION,
        NCBI_GI,
        LENGTH,
        ORGANISM,
        TAXON_ID,
        DEFLINE
    }

    static {
        headerMap.put(BseHeader.ACCESSION, "Accession");
        headerMap.put(BseHeader.NCBI_GI, "NCBI gi");
        headerMap.put(BseHeader.LENGTH, "Length");
        headerMap.put(BseHeader.ORGANISM, "Organism");
        headerMap.put(BseHeader.TAXON_ID, "Taxon ID");
        headerMap.put(BseHeader.DEFLINE, "Defline");
    }

    public static List<String> getHeaderList() {
        List<String> headerList = new ArrayList<String>();
        for (BseHeader h : BseHeader.values()) {
            headerList.add(headerMap.get(h));
        }
        return headerList;
    }

    public static List<String> formatColumns(BaseSequenceEntity bse) {
        List<String> pl = new ArrayList<String>();
        add(pl, bse.getAccession());                    //ACCESSION,
        add(pl, bse.getNcbiGINumber() + "");            //NCBI_GI,
        add(pl, bse.getBioSequence().getLength() + ""); //LENGTH,
        add(pl, bse.getOrganism());                     //ORGANISM,
        add(pl, bse.getTaxonId() + "");                 //TAXON_ID,
        add(pl, bse.getDefline().replaceAll(",", " ")); //DEFLINE,
        return pl;
    }

}
