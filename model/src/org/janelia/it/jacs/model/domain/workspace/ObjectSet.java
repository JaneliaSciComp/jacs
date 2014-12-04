package org.janelia.it.jacs.model.domain.workspace;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

@MongoMapped(collectionName = "objectSet")
public class ObjectSet extends AbstractDomainObject {

    private String targetType;
    private List<Long> members;

    public int getNumMembers() {
        return members==null ? 0 : members.size();
    }

    public void addMember(Long memberId) {
        if (members==null) {
            this.members = new ArrayList<Long>();
        }
        if (members.contains(memberId)) {
            return;
        }
        members.add(memberId);
    }

    public void removeMember(Long memberId) {
        if (members==null) {
            return;
        }
        members.remove(memberId);
        if (members.isEmpty()) {
            members = null;
        }
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public List<Long> getMembers() {
        return members;
    }

    public void setMembers(List<Long> members) {
        this.members = members;
    }
}
