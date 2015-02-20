/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.OrthogonalPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi;
import org.janelia.it.workstation.gui.large_volume_viewer.action.MouseMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.PanModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.RecentFileList;
import org.janelia.it.workstation.gui.large_volume_viewer.action.TraceMouseModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.WheelMode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZScanScrollModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZoomMouseModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.action.ZoomScrollModeAction;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;

/**
 * External controller of the Quad View UI.  Distances it from incoming
 * directives.
 * 
 * @author fosterl
 */
public class QuadViewController implements ViewStateListener {
    private QuadViewUi ui;
    private final AnnotationManager annoMgr;
    private final LargeVolumeViewer lvv;
    private final QuadViewController.QvucMouseWheelModeListener qvucmwListener = new QuadViewController.QvucMouseWheelModeListener();
    private final Collection<MouseWheelModeListener> relayListeners = new ArrayList<>();
           
    public QuadViewController(QuadViewUi ui, AnnotationManager annoMgr, LargeVolumeViewer lvv) {
        this.ui = ui;
        this.annoMgr = annoMgr;
        this.lvv = lvv;
        lvv.setMessageListener(new QvucMessageListener());
    }
    
    @Override
    public void setCameraFocus(Vec3 focus) {
        ui.setCameraFocus(focus);
    }
    
    @Override
    public void loadColorModel(String colorModelString) {
        ui.imageColorModelFromString(colorModelString);
    }
    
    @Override
    public void pathTraceRequested(Long id) {
        ui.pathTraceRequested(id);
    }
    
    @Override
    public void centerNextParent() {
        ui.centerNextParentMicron();
    }
    
    public void registerForEvents(PanModeAction pma) {
        pma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(ZoomMouseModeAction zmma) {
        zmma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(TraceMouseModeAction tmma) {
        tmma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(ZoomScrollModeAction zsma) {
        zsma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(ZScanScrollModeAction zssma) {
        zssma.setMwmListener(qvucmwListener);
    }
    
    public void registerForEvents(OrthogonalPanel op) {
        op.setMessageListener(new QvucMessageListener());
        relayListeners.add(op);
    }
    
    public void registerForEvents(RecentFileList rfl) {
        rfl.setUrlLoadListener(new QvucUrlLoadListener());
    }
    
    public void mouseModeChanged(MouseMode.Mode mode) {
        lvv.setMouseMode(mode);
        ui.setMouseMode(mode);
        for (MouseWheelModeListener l: relayListeners) {
            l.setMode(mode);
        }
    }
    
    public void wheelModeChanged(WheelMode.Mode mode) {
        lvv.setWheelMode(mode);
        for (MouseWheelModeListener l: relayListeners) {
            l.setMode(mode);
        }
    }
    
    private class QvucMouseWheelModeListener implements MouseWheelModeListener {

        @Override
        public void setMode(MouseMode.Mode modeId) {
            mouseModeChanged(modeId);
        }

        @Override
        public void setMode(WheelMode.Mode modeId) {
            wheelModeChanged(modeId);
        }
        
    }
    
    private class QvucUrlLoadListener implements UrlLoadListener {

        @Override
        public void loadUrl(URL url) {
            ui.loadRender(url);
        }
        
    }
    
    private class QvucMessageListener implements MessageListener {

        @Override
        public void message(String msg) {
            ui.setStatusLabelText(msg);
        }
        
    }
}
