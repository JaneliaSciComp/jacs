package org.janelia.it.workstation.gui.browser.components;

import com.google.common.eventbus.Subscribe;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JComponent;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.gui.browser.gui.editor.DomainObjectEditor;
import org.janelia.it.workstation.gui.browser.gui.editor.SampleEditorPanel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
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
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.browser.components.DomainViewerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
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

    private final static Logger log = LoggerFactory.getLogger(DomainViewerTopComponent.class);
    
    public static final String TC_NAME = "DomainViewerTopComponent";
    
    /* Manage the active instance of this top component */
    
    private static DomainViewerTopComponent activeInstance;
    private static void activate(DomainViewerTopComponent instance) {
        activeInstance = instance;
    }
    private static boolean isActive(DomainViewerTopComponent instance) {
        return activeInstance == instance;
    }
    public static DomainViewerTopComponent getActiveInstance() {
        return activeInstance;
    }
    
    /* Instance variables */
    
    private final InstanceContent content = new InstanceContent();
    private DomainObjectEditor<DomainObject> editor;
    
    public DomainViewerTopComponent() {
        initComponents();
        setName(Bundle.CTL_DomainViewerTopComponent());
        setToolTipText(Bundle.HINT_DomainViewerTopComponent());
        associateLookup(new AbstractLookup(content));
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
        Events.getInstance().registerOnEventBus(this);
        activate(this);
    }

    @Override
    public void componentClosed() {
        Events.getInstance().unregisterOnEventBus(this);
    }
    
    @Override
    protected void componentActivated() {
        activate(this);
        // TODO: reselect in parent browser?
//        DomainObjectNode domainObjectNode = getCurrent();
//        DomainExplorerTopComponent.getInstance().selectNode(domainObjectNode);
    }
    
    @Override
    protected void componentDeactivated() {
    }

    private DomainObject getCurrent() {
        return getLookup().lookup(DomainObject.class);
    }

    private boolean setCurrent(DomainObject domainObject) {
        DomainObject curr = getCurrent();
        if (curr==domainObject) {
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
    
    public DomainObjectEditor getEditor() {
        return editor;
    }
    
    @Subscribe
    public void loadDomainObject(DomainObjectSelectionEvent event) {

        // We only care about events if we're active
        if (!isActive(this) || !isVisible()) {
            log.debug("Viewer is not active");
            return;
        }
        
        // We only care about events generated by the explorer
        if (!hasAncestorWithType((Component)event.getSource(),DomainListViewTopComponent.class)) {
            log.debug("Event source is not a list view: {}",event);
            return;
        }
        
        // We only care about selection events
        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }
        
        if (!event.isClearAll()) {
            log.debug("Event is not clear all: {}",event);
            return;
        }
        
//        requestVisible();
        loadDomainObject(event.getDomainObject());
    }
    
    public void loadDomainObject(DomainObject domainObject) {
        // Do we already have the given node loaded?
        if (!setCurrent(domainObject)) {
            return;
        }
        
        final Class<? extends DomainObjectEditor> editorClass = getEditorClass(domainObject);
        if (editorClass==null) {
            // TODO: comment this exception back in after initial development is complete
            //throw new IllegalStateException("No viewer defined for domain object of type "+domainObject.getClass().getName());
            log.info("No viewer defined for domain object of type {}",domainObject.getClass().getName());
            return;
        }
        if (editor==null || !editor.getClass().equals(editorClass)) {
            setEditorClass(editorClass);
        }
        editor.loadDomainObject(domainObject);
        setName(StringUtils.abbreviate(domainObject.getName(), 30));
        
    }

    private boolean hasAncestorWithType(Component component, Class<?> clazz) {
        if (clazz==null) return false;
        Component c = component;
        while (c!=null) {
            log.trace("check if {} is assignable from {}",clazz.getName(),c.getClass().getName());
            if (clazz.isAssignableFrom(c.getClass())) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    private Class<? extends DomainObjectEditor> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof Sample) {
            return SampleEditorPanel.class;
        }
        return null;
    }
    
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
