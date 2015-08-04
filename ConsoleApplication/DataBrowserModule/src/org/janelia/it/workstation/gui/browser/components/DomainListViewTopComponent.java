package org.janelia.it.workstation.gui.browser.components;

import com.google.common.eventbus.Subscribe;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.workstation.gui.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.gui.browser.gui.editor.DomainObjectEditor;
import org.janelia.it.workstation.gui.browser.gui.editor.ObjectSetEditorPanel;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.gui.browser.nodes.DomainObjectNode;
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
 * Top component which displays children of domain objects.
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
    "CTL_DomainListViewTopComponent=Domain Browser",
    "HINT_DomainListViewTopComponent=Domain Browser"
})
public final class DomainListViewTopComponent extends TopComponent {

    private final static Logger log = LoggerFactory.getLogger(DomainListViewTopComponent.class);
    
    public static final String TC_NAME = "DomainListViewTopComponent";
    
    /* Manage the active instance of this top component */
    
    private static DomainListViewTopComponent activeInstance;
    private static void activate(DomainListViewTopComponent instance) {
        activeInstance = instance;
    }
    private static boolean isActive(DomainListViewTopComponent instance) {
        return activeInstance == instance;
    }
    public static DomainListViewTopComponent getActiveInstance() {
        return activeInstance;
    }
    
    /* Instance variables */
    
    private final InstanceContent content = new InstanceContent();
    private DomainObjectEditor editor;
            
    public DomainListViewTopComponent() {
        initComponents();
        setName(Bundle.CTL_DomainListViewTopComponent());
        setToolTipText(Bundle.HINT_DomainListViewTopComponent());
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
        DomainObjectNode domainObjectNode = getCurrent();
        DomainExplorerTopComponent.getInstance().selectNode(domainObjectNode);
    }
    
    @Override
    protected void componentDeactivated() {
    }
    
    private DomainObjectNode getCurrent() {
        return getLookup().lookup(DomainObjectNode.class);
    }

    private boolean setCurrent(DomainObjectNode domainObjectNode) {
        DomainObjectNode curr = getCurrent();
        if (curr==domainObjectNode) {
            return false;
        }
        if (curr!=null) {
            content.remove(curr);
        }
        content.add(domainObjectNode);
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
        if (!isActive(this)) {
            log.debug("Browser is not active");
            return;
        }
        
        // We only care about events generated by the explorer
        if (event.getSource()!=DomainExplorerTopComponent.getInstance()) {
            log.debug("Event source is not explorer: {}",event);
            return;
        }
        
        // We only care about selection events
        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }
        
        requestVisible();
        
        DomainObjectNode domainObjectNode = event.getDomainObjectNode();

        // Do we already have the given node loaded?
        if (!setCurrent(domainObjectNode)) {
            return;
        }
        
        final DomainObject domainObject = domainObjectNode.getDomainObject();
        final Class<? extends DomainObjectEditor> editorClass = getEditorClass(domainObject);
        if (editorClass==null) {
            return;
        }
        if (editor==null || !editor.getClass().equals(editorClass)) {
            setEditorClass(editorClass);
        }
        editor.loadDomainObject(domainObject);
        setName(domainObject.getName());
    }

    private Class<? extends DomainObjectEditor> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof Filter) {
            return FilterEditorPanel.class;
        }
        else if (domainObject instanceof ObjectSet) {
            return ObjectSetEditorPanel.class;
        }
        return null;
    }
    
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
