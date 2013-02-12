package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseRenderer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.ViewportGL;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.GLOrthoCamera;

public class SliceRenderer
extends BaseRenderer
{
	ViewportGL viewport = new ViewportGL();
	GLOrthoCamera glCamera = new GLOrthoCamera(camera);
	
	public SliceRenderer() {
		glCamera.setCamera(camera);
		glCamera.setViewport(viewport);
		actors.add(new TileActor());
	}

    @Override
    public void display(GLAutoDrawable gLDrawable) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
        displayBackground(gl);
        // set camera
        glCamera.setUp(gl);
        for (GLActor a : actors) {
        		a.display(gl);
        }
        glCamera.tearDown(gl);
    		gl.glFlush();
    }

    @Override
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) 
    {
        final GL2 gl = gLDrawable.getGL().getGL2();
        viewport.reshape(gl, width, height);
    }
    
    @Override
    public void setCamera(Camera3d camera) {
    		super.setCamera(camera);
    		glCamera.setCamera(camera);
    }
}
