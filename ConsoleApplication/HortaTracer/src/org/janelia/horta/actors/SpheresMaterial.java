/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.actors;

import java.awt.Color;
import java.io.IOException;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class SpheresMaterial extends BasicMaterial
{
    private int colorIndex = 0;
    
    private final float[] color = new float[] {1, 0, 0, 1};

    public SpheresMaterial() {
        shaderProgram = new SpheresShader();
    }

    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        gl.glEnable(GL3.GL_VERTEX_PROGRAM_POINT_SIZE); // important with my latest Windows nvidia driver 10/20/2014
        mesh.displayParticles(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        colorIndex = 0;
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        colorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "color");
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (colorIndex == 0) 
            init(gl);
        super.load(gl, camera);
        gl.glUniform4fv(colorIndex, 1, color, 0);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }
    
    public void setColor(Color color) {
        this.color[0] = color.getRed()/255f;
        this.color[1] = color.getGreen()/255f;
        this.color[2] = color.getBlue()/255f;
        this.color[3] = color.getAlpha()/255f;
        // Convert sRGB to linear-ish (RGB, but not alpha)
        for (int i = 0; i < 3; ++i)
            this.color[i] = this.color[i] * this.color[i];
    }
    
    public Color getColor()
    {
        return new Color(
                // convert linear to sRGB (but not alpha)
                (float)Math.sqrt(color[0]), 
                (float)Math.sqrt(color[1]), 
                (float)Math.sqrt(color[2]), 
                color[3]);
    }    
    
    private static class SpheresShader extends BasicShaderProgram
    {
        public SpheresShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresVrtx330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresGeom330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresFrag330.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}
