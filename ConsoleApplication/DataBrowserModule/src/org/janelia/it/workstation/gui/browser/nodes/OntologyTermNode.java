package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasIdentifier;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.Custom;
import org.janelia.it.jacs.model.domain.ontology.EnumItem;
import org.janelia.it.jacs.model.domain.ontology.EnumText;
import org.janelia.it.jacs.model.domain.ontology.Interval;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.ontology.Tag;
import org.janelia.it.jacs.model.domain.ontology.Text;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.components.OntologyExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.flavors.DomainObjectFlavor;
import org.janelia.it.workstation.gui.browser.nb_action.AddOntologyTermAction;
import org.janelia.it.workstation.gui.browser.nb_action.ApplyAnnotationAction;
import org.janelia.it.workstation.gui.browser.nb_action.PopupLabelAction;
import org.janelia.it.workstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.workstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.util.datatransfer.ExTransferable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node in an ontology. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyTermNode extends InternalNode<OntologyTerm> implements HasIdentifier {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyTermNode.class);
    
    private final OntologyChildFactory childFactory;
    private final WeakReference<Ontology> ontologyRef;
    
    public OntologyTermNode(OntologyChildFactory parentChildFactory, Ontology ontology, OntologyTerm ontologyTerm) {
        this(parentChildFactory, new OntologyChildFactory(ontology, ontologyTerm), ontology, ontologyTerm);    
    }
    
    private OntologyTermNode(OntologyChildFactory parentChildFactory, OntologyChildFactory childFactory, Ontology ontology, OntologyTerm ontologyTerm) {
        super(parentChildFactory, Children.create(childFactory, false), ontologyTerm);
        this.childFactory = childFactory;
        this.ontologyRef = new WeakReference<>(ontology);
        getLookupContents().add(new Index.Support() {

            @Override
            public Node[] getNodes() {
                return getChildren().getNodes();
            }

            @Override
            public int getNodesCount() {
                return getNodes().length;
            }

            @Override
            public void reorder(final int[] order) {
                log.info("reorder children of node "+getOntologyTerm().getName());
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        Ontology ontology = ontologyRef.get();
                        DomainModel model = DomainMgr.getDomainMgr().getModel();
                        OntologyTerm parentTerm = getOntologyTerm();
                        
                        List<Long> childIdOrder = new ArrayList<>();
                        List<OntologyTerm> terms = parentTerm.getTerms();
                        for(int i=0; i<order.length; i++) {
                            int index = order[i];
                            OntologyTerm term = terms.get(index);
                            childIdOrder.add(term.getId());
                        }

                        log.info("reorder children of node "+parentTerm.getId());
                        model.reorderTerms(ontology.getId(), parentTerm.getId(), childIdOrder);
                    }
                    @Override
                    protected void hadSuccess() {
                        // Event model will refresh UI
                    }
                    @Override
                    protected void hadError(Throwable error) {
                        SessionMgr.getSessionMgr().handleException(error);
                    }
                };
                worker.execute();
            }
        });
    }
    
    protected OntologyChildFactory getChildFactory() {
        return childFactory;
    }
    
    public OntologyTermNode getParent() {
        Node parent = getParentNode();
        return parent instanceof OntologyTermNode ? (OntologyTermNode)parent : null;
    }
    
    public OntologyNode getOntologyNode() {
        Node node = this;
        while (node != null) {
            if (node instanceof OntologyNode) {
                return (OntologyNode)node;
            }
            node = node.getParentNode();
        }
        return null;
    }
    
    public Ontology getOntology() {
        return (Ontology)ontologyRef.get();
    }
    
    public OntologyTerm getOntologyTerm() {
        return (OntologyTerm)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getOntologyTerm().getName();
    }
    
    @Override
    public String getSecondaryLabel() {
        return getOntologyTerm().getTypeName();
    }
    
    @Override
    public String getExtraLabel() {
        OntologyNode ontologyNode = getOntologyNode();
        org.janelia.it.workstation.gui.framework.actions.Action action = ontologyNode.getActionForNode(this);
        if (action != null) {
            KeyboardShortcut bind = SessionMgr.getKeyBindings().getBinding(action);
            if (bind != null) {
                return "(" + KeymapUtil.getShortcutText(bind) + ")";
            }
        }
        return null;
    }
    
    @Override
    public Long getId() {
        return getOntologyTerm().getId();
    }
    
    @Override
    public Image getIcon(int type) {
        OntologyTerm term = getOntologyTerm();
        if (term instanceof Category) {
            return Icons.getIcon("folder.png").getImage();
        }
        else if (term instanceof org.janelia.it.jacs.model.domain.ontology.Enum) {
            return Icons.getIcon("folder_page.png").getImage();
        }
        else if (term instanceof Interval) {
            return Icons.getIcon("page_white_code.png").getImage();
        }
        else if (term instanceof Tag) {
            return Icons.getIcon("page_white.png").getImage();
        }
        else if (term instanceof Text) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if (term instanceof Custom) {
            return Icons.getIcon("page_white_text.png").getImage();
        }
        else if (term instanceof EnumItem) {
            return Icons.getIcon("page.png").getImage();
        }
        else if (term instanceof EnumText) {
            return Icons.getIcon("page_go.png").getImage();
        }
        return Icons.getIcon("bullet_error.png").getImage();
    }

    @Override
    public boolean canCut() {
        return true;
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public boolean canRename() {
        return false;
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        log.info("clipboard COPY "+getOntologyTerm());
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        added.put(new ExTransferable.Single(DomainObjectFlavor.SINGLE_FLAVOR) {
            @Override
            protected OntologyTerm getData() {
                return (OntologyTerm) getOntologyTerm();
            }
        });
        return added;
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        log.info("clipboard CUT "+getOntologyTerm());
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(DomainObjectFlavor.SINGLE_FLAVOR) {
            @Override
            protected OntologyTerm getData() {
                return getOntologyTerm();
            }
        });
        added.put(new ExTransferable.Single(DataFlavor.stringFlavor) {
            @Override
            protected String getData() {
                return getPrimaryLabel();
            }
        });
        return added;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();
        actions.add(PopupLabelAction.get());
        actions.add(null);
        actions.add(new InternalNode.CopyNameAction());
        actions.add(new CopyGUIDAction());
        actions.add(null);
        actions.add(new AssignShortcutAction());
        actions.add(AddOntologyTermAction.get());
        actions.add(new RemoveAction());
        actions.add(null);
        actions.add(ApplyAnnotationAction.get());
        //actions.add(new RemoveAnnotationAction());
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Action getPreferredAction() {
        return ApplyAnnotationAction.get();
    }
    
    protected final class CopyGUIDAction extends AbstractAction {

        public CopyGUIDAction() {
            putValue(NAME, "Copy GUID To Clipboard");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            HasIdentifier ontologyTerm = getLookup().lookup(HasIdentifier.class);
            if (ontologyTerm==null) {
                return;
            }
            Transferable t = new StringSelection(ontologyTerm.getId()+"");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
    }
    
    protected final class AssignShortcutAction extends AbstractAction {

        public AssignShortcutAction() {
            putValue(NAME, "Assign Shortcut...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OntologyNode ontologyNode = getOntologyNode();
            org.janelia.it.workstation.gui.framework.actions.Action action = ontologyNode.getActionForNode(OntologyTermNode.this);
            OntologyExplorerTopComponent explorer = OntologyExplorerTopComponent.getInstance();
            explorer.getKeyBindDialog().showForAction(action);            
        }
    }
    
    protected final class RemoveAction extends AbstractAction {

        public RemoveAction() {
            putValue(NAME, "Remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                model.removeTerm(getOntology().getId(), getOntologyTerm().getId());
            }
            catch (Exception ex) {
                SessionMgr.getSessionMgr().handleException(ex);
            }
        }
    }
    
//    protected final class RemoveAnnotationAction extends AbstractAction {
//
//        public RemoveAnnotationAction() {
//            putValue(NAME, "Remove Annotation From Selected Items");
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            
//            OntologyTermNode node = OntologyTermNode.this;
//            OntologyTerm ontologyTerm = getLookup().lookup(OntologyTerm.class);
//            OntologyTermNode key = (ontologyTerm instanceof EnumItem) ? ((OntologyTermNode)node.getParentNode()) : node;
//            Long keyTermId = key.getId();
//            String keyTermValue = key.getDisplayName();
//            
//            log.info("Will remove annotation from all selected entities: {} ({})",keyTermValue,keyTermId);
//            
//            OntologyTermReference keyTermRef = new OntologyTermReference();
//            keyTermRef.setOntologyId(getOntology().getId());
//            keyTermRef.setOntologyTermId(key.getId());
//            
//            DomainListViewTopComponent listView = DomainListViewTopComponent.getActiveInstance();
//            if (listView==null || listView.getEditor()==null) return;
//            
//            DomainObjectSelectionModel selectionModel = listView.getEditor().getSelectionModel();
//            if (selectionModel==null) return;
//
//            // TODO: how to get image model?
//            ImageModel<DomainObject,DomainObjectId> imageModel = listView.getEditor().getImageModel();
//            if (imageModel==null) return;
//            
//            List<DomainObjectId> selectedIds = selectionModel.getSelectedIds();
//
//            DomainModel model = DomainMgr.getDomainMgr().getModel();
//            final List<DomainObject> selectedDomainObjects = model.getDomainObjectsByDomainObjectId(selectedIds);
//            
//            RemoveAnnotationTermAction<DomainObject,DomainObjectId> action = new RemoveAnnotationTermAction<>(imageModel, selectedDomainObjects, keyTermRef, keyTermValue);
//            action.doAction();
//        }
//    }
    
}
