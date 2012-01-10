
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * This is in invisible panel that supports child widgets being dragged around within the panel.
 * <p/>
 * To make an internal (child) widget draggable, call getDragController().makeDraggable(widget) or
 * getDragController().makeDraggable(widget, widget);
 *
 * @author Michael Press
 */
public class DraggableAreaPanel extends Composite {
    private PickupDragController _dragController;
    private AbsolutePanel _absolutePanel;

    public DraggableAreaPanel() {
        super();
        init();
    }

    private void init() {
        // Create a DragController to allow clients to specify that widgets are draggable.  We'll set the draggable
        // area container to null, which allows dragging anywhere in the browser
        _dragController = new PickupDragController(null, true);

        // Create an AbsolutePanel for clients to add widgets to.  Add a spacer (to push the added content down)
        // because this panel's CSS style sets a negative margin (to push it up).  This is necessary so content
        // with a negative margin (like the label on a TitledBox) doesn't get clipped.
        _absolutePanel = new AbsolutePanel();
        _absolutePanel.setStyleName("dragBoundaryArea");
        _absolutePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));

        // Required for Composite
        initWidget(_absolutePanel);
    }

    public DragController getDragController() {
        return _dragController;
    }

    public void add(Widget widget) {
        _absolutePanel.add(widget);
    }

    public void clear() {
        _absolutePanel.clear();
    }

    public boolean remove(Widget widget) {
        return _absolutePanel.remove(widget);
    }
}
