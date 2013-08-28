package org.janelia.it.jacs.model.entity.cv;

public enum SampleImageType implements NamedEnum {

    Latest("Latest"),
    Unaligned20x("Unaligned 20x"),
    Unaligned63x("Unaligned 63x"),
    Aligned20x("Aligned 20x"),
    Aligned63x("Aligned 63x");
    
    private String name;

    private SampleImageType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
