package org.janelia.it.workstation.gui.browser.components;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.gui.editor.DomainObjectEditor;
import org.janelia.it.workstation.gui.browser.gui.editor.SampleEditorPanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays domain object viewers. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.browser.components//DomainViewer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = DomainViewerTopComponent.TC_NAME,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor2", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.browser.components.DomainViewerTopComponent")
//@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_DomainViewerAction",
        preferredID = DomainViewerTopComponent.TC_NAME
)
@Messages({
    "CTL_DomainViewerAction=Domain Object Viewer",
    "CTL_DomainViewerTopComponent=Domain Object Viewer",
    "HINT_DomainViewerTopComponent=Domain Object Viewer"
})
public final class DomainViewerTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(DomainViewerTopComponent.class);
    
    public static final String TC_NAME = "DomainViewerTopComponent";
    public static final String TC_VERSION = "1.0";
        
    /* Instance variables */
    
    private final InstanceContent content = new InstanceContent();
    private DomainObjectEditor<DomainObject> editor;
    
    public DomainViewerTopComponent() {
        initComponents();
        setName(Bundle.CTL_DomainViewerTopComponent());
        setToolTipText(Bundle.HINT_DomainViewerTopComponent());
        associateLookup(new AbstractLookup(content));
        // Init the viewer manager
        SampleResultViewerManager.getInstance();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void componentOpened() {
        DomainViewerManager.getInstance().activate(this);
    }

    @Override
    public void componentClosed() {
    }
    
    @Override
    protected void componentActivated() {
        log.info("Activating domain viewer");
        DomainViewerManager.getInstance().activate(this);
        if (editor!=null) {
            editor.activate();
        }
    }
    
    @Override
    protected void componentDeactivated() {
        if (editor!=null) {
            editor.deactivate();
        }
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", TC_VERSION);
        DomainObject current = getCurrent();
        if (current!=null) {
            String objectRef = Reference.createFor(current).toString();
            log.info("Writing state: {}",objectRef);
            p.setProperty("objectRef", objectRef);
        }
        else {
            p.remove("objectRef");
        }
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        final String objectStrRef = p.getProperty("objectRef");
        log.info("Reading state: {}",objectStrRef);
        if (TC_VERSION.equals(version) && objectStrRef!=null) {

            SimpleWorker worker = new SimpleWorker() {
                DomainObject object;
                
                @Override
                protected void doStuff() throws Exception {
                    object = DomainMgr.getDomainMgr().getModel().getDomainObject(Reference.createFor(objectStrRef));
                }

                @Override
                protected void hadSuccess() {
                    if (object!=null) {
                        loadDomainObject(object, false);
                    }
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };
            worker.execute();
            
        }
    }

    public DomainObject getCurrent() {
        return getLookup().lookup(DomainObject.class);
    }

    private boolean setCurrent(DomainObject domainObject) {
        DomainObject curr = getCurrent();
        if (domainObject.equals(curr)) {
            return false;
        }
        if (curr!=null) {
            content.remove(curr);
        }
        content.add(domainObject);
        return true;
    }
    
    public void setEditorClass(Class<? extends DomainObjectEditor> editorClass) {
        try {
            if (editor!=null) {
                remove((JComponent)editor);
                Events.getInstance().unregisterOnEventBus(editor.getEventBusListener());
            }
            editor = editorClass.newInstance();
            add((JComponent)editor, BorderLayout.CENTER);
            Events.getInstance().registerOnEventBus(editor.getEventBusListener());
            revalidate();
            repaint();
        }
        catch (InstantiationException | IllegalAccessException e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        setName(editor.getName());
    }
    
    public DomainObjectEditor<? extends DomainObject> getEditor() {
        return editor;
    }
        
    public void loadDomainObject(DomainObject domainObject, boolean isUserDriven) {
        try {
            if (domainObject instanceof NeuronFragment) {
                NeuronFragment fragment = (NeuronFragment) domainObject;
                domainObject = DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());
            }
            else if (domainObject instanceof LSMImage) {
                LSMImage lsmImage = (LSMImage) domainObject;
                domainObject = DomainMgr.getDomainMgr().getModel().getDomainObject(lsmImage.getSample());
            }

            final Class<? extends DomainObjectEditor> editorClass = getEditorClass(domainObject);
            if (editorClass == null) {
                // TODO: comment this exception back in after initial development is complete
                //throw new IllegalStateException("No viewer defined for domain object of type "+domainObject.getClass().getName());
                log.info("No viewer defined for domain object of type {}", domainObject.getClass().getName());
                return;
            }

            // Do we already have the given node loaded?
            if (!setCurrent(domainObject)) {
                return;
            }

            if (editor == null || !editor.getClass().equals(editorClass)) {
                setEditorClass(editorClass);
            }
            editor.loadDomainObject(domainObject, isUserDriven, null);
            setName(editor.getName());
        }  catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    private static Class<? extends DomainObjectEditor> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof Sample) {
            return SampleEditorPanel.class;
        }
        else if (domainObject instanceof NeuronFragment) {
            return SampleEditorPanel.class;
        }
        else if (domainObject instanceof LSMImage) {
            return SampleEditorPanel.class;
        }
        return null;
    }
    
    public static boolean isSupported(DomainObject domainObject) {
        return getEditorClass(domainObject)!=null;
    }
}
