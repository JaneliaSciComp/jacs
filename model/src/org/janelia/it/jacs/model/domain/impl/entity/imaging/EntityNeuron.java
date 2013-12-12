package org.janelia.it.jacs.model.domain.impl.entity.imaging;

import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.imaging.Neuron;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

public class EntityNeuron extends EntityDomainObject implements Neuron {

    public EntityNeuron(Entity entity) {
        super(entity);
    }

    @Override
    public Integer getMaskIndex() {
        String value = getAttributeValue(EntityConstants.ATTRIBUTE_NUMBER);
        if (value==null) return null;
        return Integer.parseInt(value)+1;
    }

    @Override
    public String get2dImageFilepath() {
        return getAttributeValue(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }

    @Override
    public String getMask3dImageFilepath() {
        return getAttributeValue(EntityConstants.ATTRIBUTE_MASK_IMAGE);
    }

    @Override
    public String getChan3dImageFilepath() {
        return getAttributeValue(EntityConstants.ATTRIBUTE_CHAN_IMAGE);
    }
}
