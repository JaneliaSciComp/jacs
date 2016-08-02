package org.janelia.it.jacs.model.domain.tiledMicroscope;

import java.awt.Color;

import org.janelia.it.jacs.shared.utils.ColorUtils;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TmNeuronStyle {

    private boolean visibility;
    private String colorHex;

    public TmNeuronStyle() {
    }

    public TmNeuronStyle(boolean visibility, String colorHex) {
        this.visibility = visibility;
        this.colorHex = colorHex;
    }

    public TmNeuronStyle(boolean visibility, Color color) {
        this.visibility = visibility;
        setColor(color);
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public Color getColor() {
        return ColorUtils.fromHex(colorHex);
    }

    public void setColor(Color color) {
        this.colorHex = ColorUtils.toHex(color);
    }




}
