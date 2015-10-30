
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class NonCodingRNA extends Nucleotide implements IsSerializable, Serializable {
    /*
    * translation
    */
    private String type;
    private String dnaAcc;
    private Nucleotide dnaEntity;
    private Integer dnaBegin;
    private Integer dnaEnd;
    private Integer dnaOrientation;

    /*
    * constructor
    */
    public NonCodingRNA() {
        super(EntityTypeGenomic.NON_CODING_RNA);
    }

    /*
    * getters/setters
    */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDnaAcc() {
        return dnaAcc;
    }

    public void setDnaAcc(String dnaAcc) {
        this.dnaAcc = dnaAcc;
    }

    public Nucleotide getDnaEntity() {
        return dnaEntity;
    }

    public void setDnaEntity(Nucleotide dnaEntity) {
        this.dnaEntity = dnaEntity;
    }

    public Integer getDnaBegin() {
        return dnaBegin;
    }

    public void setDnaBegin(Integer dnaBegin) {
        this.dnaBegin = dnaBegin;
    }

    public Integer getDnaEnd() {
        return dnaEnd;
    }

    public void setDnaEnd(Integer dnaEnd) {
        this.dnaEnd = dnaEnd;
    }

    public Integer getDnaOrientation() {
        return dnaOrientation;
    }

    public void setDnaOrientation(Integer dnaOrientation) {
        this.dnaOrientation = dnaOrientation;
    }
}
