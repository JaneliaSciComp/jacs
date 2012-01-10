
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class Protein extends Peptide implements IsSerializable, Serializable {

    /*
    * translation
    */
    private String orfAcc;
    private ORF orfEntity;
    private String dnaAcc;
    private Nucleotide dnaEntity;
    private Integer dnaBegin;
    private Integer dnaEnd;
    private Integer dnaOrientation;
    private String translationTable;
    private String stop5Prime;
    private String stop3Prime;

    /*
    * constructor
    */
    public Protein() {
        super(EntityTypeGenomic.PROTEIN);
    }

    /*
    * getters/setters
    */
    public String getOrfAcc() {
        return orfAcc;
    }

    public void setOrfAcc(String orfAcc) {
        this.orfAcc = orfAcc;
    }

    public ORF getOrfEntity() {
        return orfEntity;
    }

    public void setOrfEntity(ORF orfEntity) {
        this.orfEntity = orfEntity;
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

    public String getTranslationTable() {
        return translationTable;
    }

    public void setTranslationTable(String translationTable) {
        this.translationTable = translationTable;
    }

    public String getStop5Prime() {
        return stop5Prime;
    }

    public void setStop5Prime(String stop5Prime) {
        this.stop5Prime = stop5Prime;
    }

    public String getStop3Prime() {
        return stop3Prime;
    }

    public void setStop3Prime(String stop3Prime) {
        this.stop3Prime = stop3Prime;
    }
}
