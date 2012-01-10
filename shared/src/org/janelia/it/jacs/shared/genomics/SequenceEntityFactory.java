
package org.janelia.it.jacs.shared.genomics;

import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.shared.node.FastaUtil;

/**
 * Factory class for creating the appropriate BaseSequenceEntity
 * given the accession number, the defline and the sequence bytes
 */
public class SequenceEntityFactory {

    static public void copySequenceEntity(BaseSequenceEntity src, BaseSequenceEntity dst) {
        dst.setEntityId(src.getEntityId());
        dst.setCameraAcc(src.getCameraAcc());
        dst.setExternalAcc(src.getExternalAcc());
        dst.setDefline(src.getDefline());
        dst.setDescription(src.getDescription());
        dst.setTaxonId(src.getTaxonId());
        dst.setExternalAcc(src.getExternalAcc());
        dst.setExternalSource(src.getExternalSource());
        dst.setNcbiGINumber(src.getNcbiGINumber());
        dst.setOrganism(src.getOrganism());
        dst.setReplacedBy(src.getReplacedBy());
        dst.setObsFlag(src.getObsFlag());
        dst.setSourceId(src.getSourceId());
        dst.setSampleAcc(src.getSampleAcc());
        String srcSequenceData = src.getSequence();
        if (srcSequenceData != null && srcSequenceData.length() > 0) {
            dst.setSequence(srcSequenceData);
        }
    }

    static public BaseSequenceEntity createSequenceEntity(String accessionNo,
                                                          String defline,
                                                          SequenceType entitySequenceType,
                                                          String sequenceData) {
        BaseSequenceEntity sequenceEntity;
        int accessionType = AccessionIdentifierUtil.getAccType(accessionNo);
        String sequenceType = null;
        if (entitySequenceType != null) {
            sequenceType = entitySequenceType.getDescription();
        }
        else if (sequenceData != null && sequenceData.length() > 0) {
            sequenceType = FastaUtil.determineSequenceType(sequenceData);
        }
        SequenceEntityDeflineInitializer entityDeflineInitializer;
        switch (accessionType) {
            case AccessionIdentifierUtil.CAMERA_READ_ACC:
                sequenceEntity = new Read();
                entityDeflineInitializer = new ReadDeflineInitializer();
                break;
            case AccessionIdentifierUtil.CAMERA_ORF_ACC:
                sequenceEntity = new ORF();
                entityDeflineInitializer = new ORFDeflineInitializer();
                break;
            case AccessionIdentifierUtil.CAMERA_PROTEIN_ACC:
                sequenceEntity = new Protein();
                entityDeflineInitializer = new ProteinDeflineInitializer();
                break;
            case AccessionIdentifierUtil.CAMERA_SCAFFOLD_ACC:
                sequenceEntity = new Scaffold();
                entityDeflineInitializer = new GenericDeflineInitializer();
                break;
            default:
                if (sequenceType != null && sequenceType.length() > 0) {
                    if (sequenceType.equals(SequenceType.NUCLEOTIDE)) {
                        sequenceEntity = new Nucleotide();
                        entityDeflineInitializer = new GenericDeflineInitializer();
                    }
                    else if (sequenceType.equals(SequenceType.PEPTIDE)) {
                        sequenceEntity = new Protein();
                        entityDeflineInitializer = new ProteinDeflineInitializer();
                    }
                    else {
                        throw new IllegalArgumentException("Invalid sequence type for: " + accessionNo);
                    }
                }
                else {
                    throw new IllegalArgumentException("Unspecified sequence type for: " + accessionNo);
                }
                break;
        }
        sequenceEntity.setCameraAcc(accessionNo);
        entityDeflineInitializer.initialize(sequenceEntity, defline);
        if (sequenceData != null && sequenceData.length() > 0) {
            sequenceEntity.setSequence(sequenceData);
        }
        return sequenceEntity;
    }

    /**
     * The method duplicates a base sequence entity so that the generated class will always have
     * the class type in agreement with its accession
     */
    static public BaseSequenceEntity duplicateSequenceEntity(BaseSequenceEntity src) {
        BaseSequenceEntity dupEntity =
                createSequenceEntity(src.getCameraAcc(), src.getDefline(), src.getSequenceType(), null);
        copySequenceEntity(src, dupEntity);
        return dupEntity;
    }

    /**
     * Verifies whether the entity's class matches the accession type
     *
     * @param sequenceEntity
     * @return 0 - unknonwn
     *         -1 - class doesn't match the accession type
     *         1 - class matches the accession type
     */
    static public int verifySequenceEntityClass(BaseSequenceEntity sequenceEntity) {
        int result = 0;
        if (sequenceEntity == null) {
            return result;
        }
        int accessionType = AccessionIdentifierUtil.getAccType(sequenceEntity.getCameraAcc());
        switch (accessionType) {
            case AccessionIdentifierUtil.CAMERA_READ_ACC:
                if (sequenceEntity instanceof Read) {
                    result = 1;
                }
                else {
                    result = -1;
                }
                break;
            case AccessionIdentifierUtil.CAMERA_ORF_ACC:
                if (sequenceEntity instanceof ORF) {
                    result = 1;
                }
                else {
                    result = -1;
                }
                break;
            case AccessionIdentifierUtil.CAMERA_PROTEIN_ACC:
                if (sequenceEntity instanceof Protein) {
                    result = 1;
                }
                else {
                    result = -1;
                }
                break;
            case AccessionIdentifierUtil.CAMERA_SCAFFOLD_ACC:
                if (sequenceEntity instanceof Scaffold) {
                    result = 1;
                }
                else {
                    result = -1;
                }
                break;
            default:
                if (sequenceEntity instanceof Nucleotide) {
                    result = 1;
                }
                else if (sequenceEntity instanceof Peptide) {
                    result = 1;
                }
                else {
                    result = -1;
                }
                break;
        }
        return result;
    }

}
