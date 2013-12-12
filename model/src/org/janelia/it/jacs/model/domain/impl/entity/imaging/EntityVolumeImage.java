package org.janelia.it.jacs.model.domain.impl.entity.imaging;

import org.janelia.it.jacs.model.domain.impl.entity.metamodel.EntityDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.imaging.Masked3d;
import org.janelia.it.jacs.model.domain.interfaces.imaging.Viewable2d;
import org.janelia.it.jacs.model.domain.interfaces.imaging.Viewable3d;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

public class EntityVolumeImage extends EntityDomainObject implements Viewable2d, Viewable3d, Masked3d {

    public EntityVolumeImage(Entity entity) {
        super(entity);
    }

    public String getName() {
    	String name = super.getName();
    	// Remove file extension, e.g. Reference.v3dpbd -> Reference
    	int dot = name.indexOf('.');
    	if (dot>0) name = name.substring(0, dot);
        return name;
    }
    
    @Override
    public String get2dImageFilepath() {
        return getAttributeValue(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }
    
    public String get3dImageFilepath() {
        return getAttributeValue(EntityConstants.ATTRIBUTE_FILE_PATH);
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
