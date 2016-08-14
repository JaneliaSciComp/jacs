package org.janelia.it.jacs.model.domain.enums;

/**
 * The type of image to show for given sample.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum SampleImageType {

    Latest("Latest"),
    LatestUnaligned("Latest Unaligned"),
    LatestAligned("Latest Aligned"),
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
