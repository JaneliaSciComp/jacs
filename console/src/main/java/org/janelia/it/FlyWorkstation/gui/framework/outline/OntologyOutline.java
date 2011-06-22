package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import sun.awt.VerticalBagLayout;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
public class OntologyOutline extends JPanel implements ActionListener {
    private static String ADD_COMMAND       = "add";
    private static String REMOVE_COMMAND    = "remove";
    private static String ROOT_COMMAND      = "root";
    private static String SWITCH_COMMAND    = "switch";
    private static String BIND_EDIT_COMMAND = "change_bind";
    private static String BIND_MODE_COMMAND = "bind_mode";

    private JPanel treesPanel;
    private DynamicTree selectedTree;
    private KeyBindFrame keyBindDialog;
    private JToggleButton keyBindButton;

    public OntologyOutline() {
        super(new BorderLayout());

        // Create the components

        JButton addButton = new JButton("Add Node");
        addButton.setActionCommand(ADD_COMMAND);
        addButton.addActionListener(this);

        JButton removeButton = new JButton("Remove Node");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.addActionListener(this);

        JButton newButton = new JButton("New Ontology");
        newButton.setActionCommand(ROOT_COMMAND);
        newButton.addActionListener(this);

        JButton switchButton = new JButton("Switch Ontology");
        switchButton.setActionCommand(SWITCH_COMMAND);
        switchButton.addActionListener(this);

        keyBindButton = new JToggleButton("Set Shortcuts");
        keyBindButton.setActionCommand(BIND_MODE_COMMAND);
        keyBindButton.addActionListener(this);

        // Lay everything out

        this.treesPanel = new JPanel(new VerticalBagLayout());
        add(new JLabel("Ontology Editor"), BorderLayout.NORTH);
        add(treesPanel, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        JPanel panel = new JPanel(new GridBagLayout());

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(addButton, c);

        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(removeButton, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(switchButton, c);

        c.gridx = 1;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(newButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(keyBindButton, c);
        
        add(panel, BorderLayout.SOUTH);

        // Prepare the key binding dialog box

        this.keyBindDialog = new KeyBindFrame();
        keyBindDialog.pack();

        keyBindDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                // refresh the tree in case the key bindings were updated
                DefaultTreeModel treeModel = (DefaultTreeModel)selectedTree.getTree().getModel();
                treeModel.nodeChanged(selectedTree.getCurrentNode());
            }
        });

        // Populate the tree view with the user's first tree

        // Load the tree in the background so that the app starts up first
        SwingWorker<Void, Void> loadTasks = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Entity> ontologyRootList = EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                        EntityConstants.TYPE_ONTOLOGY_ROOT_ID);

                if (null!=ontologyRootList && ontologyRootList.size()>=1) {
                    initializeTree(ontologyRootList.get(0));
                }
                return null;
            }
        };

        loadTasks.execute();
    }

    private DynamicTree initializeTree(Entity ontologyRoot) {

        // Create a new tree and add all the nodes to it

        ActionableEntity rootAE = new ActionableEntity(ontologyRoot);
        rootAE.setAction(new NavigateToNodeAction(ontologyRoot));

        selectedTree = new DynamicTree(rootAE);
        addNodes(selectedTree, null, rootAE);

        // Set the key listener
        
        final JTree tree = selectedTree.getTree();
        KeyListener defaultKeyListener = tree.getKeyListeners()[0];
        tree.removeKeyListener(defaultKeyListener);
        tree.addKeyListener(keyListener);

        // Replace the tree in the panel

        selectedTree.expandAll();
        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        // Context menu for nodes

        final JPopupMenu popup = new JPopupMenu();
        popup.setOpaque(true);
        popup.setLightWeightPopupEnabled(true);

        JMenuItem mi = new JMenuItem("Assign shortcut...");
        mi.addActionListener(this);
        mi.setActionCommand(BIND_EDIT_COMMAND);
        popup.add(mi);

        mi = new JMenuItem("Add child node");
        mi.addActionListener(this);
        mi.setActionCommand(ADD_COMMAND);
        popup.add(mi);

        mi = new JMenuItem("Remove this node");
        mi.addActionListener(this);
        mi.setActionCommand(REMOVE_COMMAND);
        popup.add(mi);

        // Mouse listener which keeps track of doubleclicks on nodes, and rightclicks to show the context menu

        tree.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    tree.setSelectionRow(row);
                    if (e.isPopupTrigger()) {
                        popup.show( (JComponent)e.getSource(),
                                e.getX(), e.getY() );
                    }
                    else if (e.getClickCount()==2) {
                        ActionableEntity curr = selectedTree.getCurrentNode().getEntityNode();
                        if (!(curr.getAction() instanceof NavigateToNodeAction))
                            curr.getAction().doAction();
                    }
                }
            }
            public void mousePressed(MouseEvent e) {
                // We have to also listen for mousePressed because OSX generates the popup trigger here
                // instead of mouseReleased like any sane OS.
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row >= 0) {
                    tree.setSelectionRow(row);
                    if (e.isPopupTrigger()) {
                        popup.show( (JComponent)e.getSource(),
                                e.getX(), e.getY() );
                    }
                }
            }
        });


        this.updateUI();
        return selectedTree;
    }

    public void navigateToEntityNode(Entity entity) {
        selectedTree.navigateToEntityNode(entity);
    }

    private void addNodes(DynamicTree tree, EntityMutableTreeNode parentNode, ActionableEntity node) {
        EntityMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = tree.addObject(parentNode, node);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = tree.rootNode;
        }
        Entity entity = node.getEntity();
        if (entity.getEntityData() != null) {
            for (EntityData tmpData : entity.getEntityData()) {
                Entity childEntity = tmpData.getChildEntity();
                ActionableEntity actionEntity = new ActionableEntity(childEntity);

                if (tmpData.getChildEntity().getEntityData().isEmpty()) {
                    // leaf node
                    actionEntity.setAction(new AddOrRemoveTagAction(childEntity));
                }
                else {
                    // category node
                    actionEntity.setAction(new NavigateToNodeAction(childEntity));
                }

                addNodes(tree, newNode, actionEntity);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (ADD_COMMAND.equals(command)) {
            if (selectedTree == null) {
                JOptionPane.showMessageDialog(this, "No ontology selected.");
            }
            // Add button clicked
            String termName = (String)JOptionPane.showInputDialog(
                    this,
                    "Ontology Term:\n",
                    "Adding to "+selectedTree.getCurrentNodeName(),
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);

            if ((termName == null) || (termName.length() <= 0)) {
                return;
            }
            EJBFactory.getRemoteAnnotationBean().createOntologyTerm(System.getenv("USER"), selectedTree.getCurrentNodeId(),
                    termName);
            updateSelectedTreeEntity();
        }
        else if (REMOVE_COMMAND.equals(command)) {
            // Remove button clicked
            int deleteConfirmation = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete this term?",
                    "Delete Term",
                    JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation!=0) {
                return;
            }
            EJBFactory.getRemoteAnnotationBean().removeOntologyTerm(System.getenv("USER"), selectedTree.getCurrentNodeId());
            updateSelectedTreeEntity();
        }
        else if (ROOT_COMMAND.equals(command)) {
            // New Root button clicked.
            String rootName = (String)JOptionPane.showInputDialog(
                    this,
                    "Ontology Root Name:\n",
                    "New Ontology",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);

            if ((rootName == null) || (rootName.length() <= 0)) {
                JOptionPane.showMessageDialog(this, "Require a valid name", "Ontology Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Entity newOntologyRoot = EJBFactory.getRemoteAnnotationBean().createOntologyRoot(System.getenv("USER"), rootName);
            initializeTree(newOntologyRoot);
        }
        else if (SWITCH_COMMAND.equals(command)) {
            List<Entity> ontologyRootList = EJBFactory.getRemoteAnnotationBean().getUserEntitiesByType(System.getenv("USER"),
                    EntityConstants.TYPE_ONTOLOGY_ROOT_ID);
            ArrayList<String> ontologyNames = new ArrayList<String>();
            for (Entity entity : ontologyRootList) {
                ontologyNames.add(entity.getName());
            }
            String choice = (String)JOptionPane.showInputDialog(
                    this,
                    "Choose an ontology:\n",
                    "Ontology Chooser",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    ontologyNames.toArray(),
                    ontologyNames.get(0));

            if ((choice != null) && (choice.length() > 0)) {
                for (Entity ontologyEntity : ontologyRootList) {
                    if (ontologyEntity.getName().equals(choice)) {
                        initializeTree(ontologyEntity);
                        break;
                    }
                }
            }
        }
        else if (BIND_EDIT_COMMAND.equals(command)) {
            EntityMutableTreeNode treeNode = selectedTree.getCurrentNode();
            if (treeNode != null) {
                ActionableEntity ae = treeNode.getEntityNode();
                if (ae != null)
                    keyBindDialog.showForAction(ae.getAction());
            }
        }
    }

    // todo This is toooooooo brute-force
    private void updateSelectedTreeEntity(){
        Entity entity= EJBFactory.getRemoteAnnotationBean().getUserEntityById(System.getenv("USER"), selectedTree.rootNode.getEntityId());
        if (null!=selectedTree || entity.getName().equals(selectedTree.rootNode.getEntityName())){
            initializeTree(entity);
        }
        this.updateUI();
    }

    // Listen for key strokes and execute the appropriate key bindings
    private KeyListener keyListener = new KeyAdapter() {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (KeymapUtil.isModifier(e)) return;
                KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);

                if (keyBindButton.isSelected()) {

                    // Set the key bind

                    ActionableEntity actionEntity = selectedTree.getCurrentNode().getEntityNode();
                    ConsoleApp.getKeyBindings().setBinding(shortcut, actionEntity.getAction());

                    // Refresh the entire tree (another key bind may have been overridden)

                    JTree tree = selectedTree.getTree();
                    DefaultTreeModel treeModel = (DefaultTreeModel)tree.getModel();
                    selectedTree.refreshDescendants((EntityMutableTreeNode)treeModel.getRoot());

                    // Move to the next row

                    selectedTree.navigateToNextRow();

                }
                else {
                    ConsoleApp.getKeyBindings().executeBinding(shortcut);
                }
            }
        }
    };
}
