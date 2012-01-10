
package org.janelia.it.jacs.web.gwt.common.client.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AlignmentType implements IsSerializable {

    public static final AlignmentType PARENT_CHILD_RELATIONSHIP = new AlignmentType(0, "Parent Child Relationship", "");
    public static final AlignmentType BLAST_HIT = new AlignmentType(1, "Blast Hit", "");
    public static final AlignmentType READ_MAPPING = new AlignmentType(3, "Read Mapping", "");

    private int code;
    private String name;
    private String description;

    public AlignmentType() {
    }

    public AlignmentType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
