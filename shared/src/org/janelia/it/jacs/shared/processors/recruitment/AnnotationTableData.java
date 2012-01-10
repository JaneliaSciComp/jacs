
package org.janelia.it.jacs.shared.processors.recruitment;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 29, 2007
 * Time: 4:28:04 PM
 */
public class AnnotationTableData implements IsSerializable, Serializable {
    private String begin;
    private String end;
    private String strand;
    private String length;
    private String proteinId;
    private String dbXRef;
    private String product;

    public AnnotationTableData() {
    }

    public AnnotationTableData(String proteinId, String dbXRef, String product, String begin,
                               String end, String strand, String length) {
        this.proteinId = proteinId;
        this.dbXRef = dbXRef;
        this.product = product;
        this.begin = begin;
        this.end = end;
        this.strand = strand;
        this.length = length;
    }

    public String getProteinId() {
        return proteinId;
    }

    public void setProteinId(String genbankProteinId) {
        this.proteinId = genbankProteinId;
    }

    public String getDbXRef() {
        return dbXRef;
    }

    public void setDbXRef(String dbXRef) {
        this.dbXRef = dbXRef;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

}
