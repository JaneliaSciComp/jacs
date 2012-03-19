
package org.janelia.it.jacs.shared.genomics;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.DefLineFormat;

import java.util.Map;

/**
 * This class is used to initialize the Base Sequence Entity from the defline
 */
abstract public class SequenceEntityDeflineInitializer {

    protected DefLineFormat deflineFormat;

    public SequenceEntityDeflineInitializer() {
        deflineFormat = new DefLineFormat();
    }

    /**
     * copies the sequence data from a source to a destination;
     * For now it seems that the method is needed only here and only because we don't have all
     * the data set properly (e.g. ORFs are marked as GenericDNA sequences therefore retrieved as DNA).
     * Moreover once we have all the data set properly the method may not even be needed since Hibernate
     * should retrieve the appropriate proxy
     *
     * @param src
     * @param dst
     */
    public void copySequence(BaseSequenceEntity src, BaseSequenceEntity dst) {
        dst.setEntityId(src.getEntityId());
        dst.setAccession(src.getAccession());
        dst.setExternalAcc(src.getExternalAcc());
        dst.setDefline(src.getDefline());
        dst.setSequence(src.getSequence());
    }

    public void initialize(BaseSequenceEntity entity, String defline) {
        if (defline != null && defline.length() > 0) {
            if (defline.startsWith(entity.getAccession())) {
                entity.setDefline(defline);
            }
            else {
                entity.setDefline(entity.getAccession() + " " + defline);
            }
            Map deflineMap = deflineFormat.parseDefline(defline);
            initialize(entity, deflineMap);
        }
        else {
            entity.setDefline(entity.getAccession());
        }
    }

    public void initialize(BaseSequenceEntity entity, Map defline) {
    }

}
