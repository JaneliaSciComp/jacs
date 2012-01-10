
package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Provides a set of enumerated type constants and a factory method for creating the desired TitledBox type
 *
 * @author Michael Press
 */
public class TitledBoxFactory {
    public enum BoxType {
        TITLED_BOX, SECONDARY_BOX, TERTIARY_BOX, CLEAR_BOX
    }

    public static TitledBox createTitledBox(String title, BoxType boxType) {
        return createTitledBox(title, boxType, /*show action links*/ true);
    }

    public static TitledBox createTitledBox(String title, BoxType boxType, boolean showActionLinks) {
        if (boxType == BoxType.TITLED_BOX)
            return new TitledBox(title, showActionLinks);
        else if (boxType == BoxType.SECONDARY_BOX)
            return new SecondaryTitledBox(title, showActionLinks);
        else if (boxType == BoxType.CLEAR_BOX)
            return new ClearTitledBox(title, showActionLinks);
        else if (boxType == BoxType.TERTIARY_BOX)
            return new TertiaryTitledBox(title, showActionLinks);
        else // default to TitledBox
            return new TitledBox(title, showActionLinks);
    }
}
