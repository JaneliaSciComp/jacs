package org.janelia.it.jacs.model.domain.interfaces.imaging;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;

public interface Neuron extends DomainObject, MaskIndexed, Viewable2d, Masked3d {

    public Integer getMaskIndex();
}
