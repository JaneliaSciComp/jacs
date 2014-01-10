package org.janelia.it.jacs.model.domain.impl.entity.metamodel;

import org.janelia.it.jacs.model.domain.impl.metamodel.AbstractPermission;
import org.janelia.it.jacs.model.entity.EntityActorPermission;

public class EntityPermission extends AbstractPermission {

    private String rights;
    
    public EntityPermission(EntityActorPermission eap) {
        setSubjectKey(eap.getSubjectKey());
        this.rights = eap.getPermissions();
    }
    
    public EntityPermission(String subjectKey, boolean isOwner, boolean canRead, boolean canWrite) {
        setSubjectKey(subjectKey);
        String owner = isOwner?"o":"";
        String read = canRead?"r":"";
        String write = canWrite?"w":"";
        this.rights = owner+read+write;
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
