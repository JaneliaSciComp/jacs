
package org.janelia.it.jacs.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 15, 2008
 * Time: 9:42:21 AM
 */
public class CollectionHost implements IsSerializable {

    private Long hostId;
    private String organism = "";
    private Integer taxonId;
    private String hostDetails = "";

    public CollectionHost() {
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public Integer getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Integer taxonId) {
        this.taxonId = taxonId;
    }

    public String getHostDetails() {
        return hostDetails;
    }

    public void setHostDetails(String hostDetails) {
        this.hostDetails = hostDetails;
    }
}
