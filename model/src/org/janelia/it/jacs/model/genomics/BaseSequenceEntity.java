
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.User;

import java.io.Serializable;

abstract public class BaseSequenceEntity implements Serializable, IsSerializable, Comparable {

    public static Integer FORWARD_ORIENTATION = 1;
    public static Integer REVERSE_ORIENTATION = -1;
    /*
    * system attributes
    */
    private Long entityId;
    private EntityTypeGenomic entityType;
    protected User owner;
    private Integer sourceId = 0;
    private Boolean obsFlag = Boolean.FALSE;
    private String replacedBy;
    /*
    * fasta attributes
    */
    private String accession;
    private String defline;
    private BioSequence bioSequence;
    private Integer sequenceLength;
    /*
    * general attributes
    */
    private String externalSource;
    private String externalAcc;
    private Integer ncbiGINumber;
    /*
    * genomic context
    */
    private String organism;
    private Integer taxonId;
    private String assemblyAcc;
    private Assembly assembly;
    private String sampleAcc;
    private Sample sample;
    private String libraryAcc;
    private Library library;

    /*
    * constructors
    */
    public BaseSequenceEntity() {
        this.entityType = EntityTypeGenomic.UNKNOWN;
    }

    protected BaseSequenceEntity(EntityTypeGenomic entityType) {
        this.entityType = entityType;
    }

    /*
    * methods
    */
    public int compareTo(Object o) {
        return this.accession.compareTo(((BaseSequenceEntity) o).getAccession());
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public EntityTypeGenomic getEntityType() {
        return entityType;
    }

    protected void setEntityType(EntityTypeGenomic entityType) {
        this.entityType = entityType;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public Boolean getObsFlag() {
        return obsFlag;
    }

    public void setObsFlag(Boolean obsFlag) {
        this.obsFlag = obsFlag;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }

    public String getDefline() {
        return defline;
    }

    public void setDefline(String defline) {
        this.defline = defline;
    }

    public BioSequence getBioSequence() {
        return bioSequence;
    }

    public void setBioSequence(BioSequence bioSequence) {
        this.bioSequence = bioSequence;
    }

    public Integer getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(Integer sequenceLength) {
        this.sequenceLength = sequenceLength;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getExternalAcc() {
        return externalAcc;
    }

    public void setExternalAcc(String externalAcc) {
        this.externalAcc = externalAcc;
    }

    public Integer getNcbiGINumber() {
        return ncbiGINumber;
    }

    public void setNcbiGINumber(Integer ncbiGINumber) {
        this.ncbiGINumber = ncbiGINumber;
    }

    /*
    * old methods for back compatibility
    * set Sequence - one of three ways
    * 1. as a BioSequence object
    * 2. as a String (creates a BioSequence object from the string)
    * 3. as a Subfeature
    */
    public String getFullExternalAcc() {
        if (externalAcc == null || externalSource == null) return null;
        else return externalSource.concat("|").concat(externalAcc);
    }

    public String getExternalAccLocation() {
        return externalSource;
    }

    public void setDescription(String description) {
        this.defline = description;
    }

    public String getDescription() {
        return defline;
    }

    /*
    * sequence manipulation
    */
    public SequenceType getSequenceType() {
        return getEntityType().getSequenceType();
    }

    public String getSequence() {
        return bioSequence == null ? null : bioSequence.getSequence();
    }

    abstract public void setSequence(String sequence);

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

    public String getAssemblyAcc() {
        return assemblyAcc;
    }

    public void setAssemblyAcc(String assemblyAcc) {
        this.assemblyAcc = assemblyAcc;
    }

    public Assembly getAssembly() {
        return assembly;
    }

    public void setAssembly(Assembly assembly) {
        this.assembly = assembly;
    }

    public String getSampleAcc() {
        return sampleAcc;
    }

    public void setSampleAcc(String sampleAcc) {
        this.sampleAcc = sampleAcc;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

/*
    public Set getDmaTags() {
        return dmaTags;
    }

    public void setDmaTags(Set dmaTags) {
        this.dmaTags = dmaTags;
    }    
*/

    public String getLibraryAcc() {
        return libraryAcc;
    }

    public void setLibraryAcc(String libraryAcc) {
        this.libraryAcc = libraryAcc;
    }

    public Library getLibrary() {
        return library;
    }

    public void setLibrary(Library library) {
        this.library = library;
    }
}
