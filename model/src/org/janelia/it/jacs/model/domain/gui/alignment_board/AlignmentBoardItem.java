package org.janelia.it.jacs.model.domain.gui.alignment_board;

import java.util.List;

import org.janelia.it.jacs.model.domain.Reference;

public class AlignmentBoardItem {

    private Reference target;
    private boolean visible;
    private String inclusionStatus;
    private String color;
    private String renderMethod;
    private List<AlignmentBoardItem> children;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public Reference getTarget() {
        return target;
    }

    public void setTarget(Reference target) {
        this.target = target;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getInclusionStatus() {
        return inclusionStatus;
    }

    public void setInclusionStatus(String inclusionStatus) {
        this.inclusionStatus = inclusionStatus;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getRenderMethod() {
        return renderMethod;
    }

    public void setRenderMethod(String renderMethod) {
        this.renderMethod = renderMethod;
    }

    public List<AlignmentBoardItem> getChildren() {
        return children;
    }

    public void setChildren(List<AlignmentBoardItem> children) {
        this.children = children;
    }
}
