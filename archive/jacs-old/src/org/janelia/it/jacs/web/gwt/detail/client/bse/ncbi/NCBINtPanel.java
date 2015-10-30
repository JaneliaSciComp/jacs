
package org.janelia.it.jacs.web.gwt.detail.client.bse.ncbi;

/**
 * The class encapsulates the data and operations needed to render NCBINtPanel
 *
 * @author Tareq Nabeel
 */
public class NCBINtPanel extends NCBIBasePanel {


    private static final String DETAIL_TYPE = "NCBI NT";

    /**
     * The label to display for entity id for TitleBox and error/debug messages e.g. "ORF" or "NCBI"
     *
     * @return The label to display for entity id for TitleBox and error/debug messages
     */
    public String getDetailTypeLabel() {
        return DETAIL_TYPE;
    }


}
