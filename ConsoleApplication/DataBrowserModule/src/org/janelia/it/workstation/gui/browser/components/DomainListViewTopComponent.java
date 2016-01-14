package org.janelia.it.workstation.gui.browser.components;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.gui.editor.DomainObjectSelectionEditor;
import org.janelia.it.workstation.gui.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.gui.browser.gui.editor.ObjectSetEditorPanel;
import org.janelia.it.workstation.gui.browser.gui.support.MouseForwarder;
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
 * Top component which displays lists of domain objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.browser.components//DomainListView//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = DomainListViewTopComponent.TC_NAME,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED
)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.browser.components.DomainListViewTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_DomainListViewAction",
        preferredID = DomainListViewTopComponent.TC_NAME
)
@Messages({
    "CTL_DomainListViewAction=Domain Browser",
    "CTL_DomainListViewTopComponent=Domain Browser"
})
public final class DomainListViewTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(DomainListViewTopComponent.class);

    public static final String TC_NAME = "DomainListViewTopComponent";
    public static final String TC_VERSION = "1.0";
    
    /* Instance variables */
    
    private final InstanceContent content = new InstanceContent();
    private DomainObjectSelectionEditor editor;
    
    public DomainListViewTopComponent() {
        initComponents();
        setName(Bundle.CTL_DomainListViewTopComponent());
        associateLookup(new AbstractLookup(content));
        // Init the viewer manager
        DomainViewerManager.getInstance();
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
        DomainListViewManager.getInstance().activate(this);
    }
    
    @Override
    public void componentClosed() {
    }

    @Override
    protected void componentActivated() {
        DomainListViewManager.getInstance().activate(this);
    }
    
    @Override
    protected void componentDeactivated() {
    }
    
    private DomainObject getCurrent() {
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
    
    public void setEditorClass(Class<? extends DomainObjectSelectionEditor> editorClass) {
        try {
            
            if (editor!=null) {
                remove((JComponent)editor);
                Events.getInstance().unregisterOnEventBus(editor);
                Events.getInstance().unregisterOnEventBus(editor.getEventBusListener());
            }
            
            editor = editorClass.newInstance();
            Events.getInstance().registerOnEventBus(editor.getEventBusListener());
            Events.getInstance().registerOnEventBus(editor);
            
            JComponent editorComponent = (JComponent)editor;
            
            editorComponent.addMouseListener(new MouseForwarder(this, "DomainObjectSelectionEditor->DomainListViewTopComponent"));
            add(editorComponent, BorderLayout.CENTER);
        }
        catch (InstantiationException | IllegalAccessException e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
        setName(editor.getName());
    }
    
    public DomainObjectSelectionEditor getEditor() {
        return editor;
    }
    
    public void loadDomainObject(DomainObject domainObject) {
        
        // Do we already have the given node loaded?
        if (!setCurrent(domainObject)) {
            return;
        }
        
        final Class<? extends DomainObjectSelectionEditor> editorClass = getEditorClass(domainObject);
        if (editorClass==null) {
            return;
        }
        if (editor==null || !editor.getClass().equals(editorClass)) {
            setEditorClass(editorClass);
        }
        editor.loadDomainObject(domainObject);
        setName(domainObject.getName());
    }

    private Class<? extends DomainObjectSelectionEditor> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof Filter) {
            return FilterEditorPanel.class;
        }
        else if (domainObject instanceof ObjectSet) {
            return ObjectSetEditorPanel.class;
        }
        return null;
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
                        loadDomainObject(object);
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
}
