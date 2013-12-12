package org.janelia.it.jacs.model.domain.impl.entity;

import org.janelia.it.jacs.model.domain.impl.entity.common.EntityFolder;
import org.janelia.it.jacs.model.domain.impl.entity.imaging.EntityNeuron;
import org.janelia.it.jacs.model.domain.impl.entity.imaging.EntitySample;
import org.janelia.it.jacs.model.domain.interfaces.DomainObjectFactory;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

public class EntityDomainObjectFactory implements DomainObjectFactory<Entity> {

    public DomainObject createDomainObject(Entity entity) {
     
        String type = entity.getEntityTypeName();
        
        if (EntityConstants.TYPE_FOLDER.equalsIgnoreCase(type)) {
            return new EntityFolder(entity);
        }
        else if (EntityConstants.TYPE_SAMPLE.equalsIgnoreCase(type)) {
            return new EntitySample(entity);
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equalsIgnoreCase(type)) {
            return new EntityNeuron(entity);
        }
        
        return null;
    }
    
}
