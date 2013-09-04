package org.janelia.it.FlyWorkstation.gui.slice_viewer.action;

import java.awt.event.MouseWheelListener;

import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.MouseModalWidget;

public interface WheelMode
extends MouseWheelListener
{
    enum Mode {
        ZOOM,
        SCAN
    }
	public MouseModalWidget getWidget();
	public void setWidget(MouseModalWidget widget, boolean updateCursor);
	Camera3d getCamera();
	void setCamera(Camera3d camera);
}
