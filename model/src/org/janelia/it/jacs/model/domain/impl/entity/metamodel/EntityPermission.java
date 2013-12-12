package org.janelia.it.jacs.model.domain.impl.entity.metamodel;

import org.janelia.it.jacs.model.domain.impl.metamodel.AbstractPermission;
import org.janelia.it.jacs.model.entity.EntityActorPermission;

public class EntityPermission extends AbstractPermission {

    private String rights;
    
    public EntityPermission(EntityActorPermission eap) {
        setSubjectKey(eap.getSubjectKey());
        this.rights = eap.getPermissions();
    }

    public String getRights() {
        return rights;
    }
    public void setRights(String rights) {
        this.rights = rights;
    }
    @Override
    public boolean isOwner() {
        return rights.contains("o");
    }
    @Override
    public boolean canRead() {
        return rights.contains("r");
    }
    @Override
    public boolean canWrite() {
        return rights.contains("w");
    }
    
}
