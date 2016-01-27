package org.janelia.it.jacs.model.domain.workspace;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A static set of objects in a single collection. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="objectSet",label="Object Set")
@SearchType(key="objectSet",label="Object Set")
public class ObjectSet extends AbstractDomainObject implements IsParent {

    @SearchAttribute(key="class_name_txt",label="Class Name",display=false)
    private String className;
    private List<Long> members;

    public boolean hasMembers() {
    	return members!=null && !members.isEmpty();
    }

    @JsonIgnore
    public int getNumMembers() {
        return members==null ? 0 : members.size();
    }

    public void addMember(Long memberId) {
        if (members==null) {
            this.members = new ArrayList<>();
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
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Long> getMembers() {
        return members;
    }

    public void setMembers(List<Long> members) {
        this.members = members;
    }
}
