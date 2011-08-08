package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.hibernate.Hibernate;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNode;
import org.janelia.it.FlyWorkstation.gui.framework.tree.LazyTreeNodeExpansionWorker;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * A tree of Entities that may load lazily. Manages all the asynchronous loading and tree updating that happens in the
 * case of lazy nodes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityTree extends JPanel {

    protected final JPanel treesPanel;
    protected DynamicTree selectedTree;
    protected boolean lazy;

    public EntityTree() {
        this(false);
    }

    public EntityTree(boolean lazy) {
        super(new BorderLayout());

        this.lazy = lazy;
        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);
    }

    public void showLoadingIndicator() {
        treesPanel.removeAll();
        treesPanel.add(new JLabel(Icons.loadingIcon));
    }

    public void initializeTree(final Long rootId) {
        if (null != selectedTree) {
            ToolTipManager.sharedInstance().unregisterComponent(selectedTree);
        }

        treesPanel.removeAll();

        if (rootId == null) return;

        treesPanel.add(new JLabel(Icons.loadingIcon));
        this.updateUI();

        SimpleWorker loadingWorker = new SimpleWorker() {

            private Entity rootEntity;

            protected void doStuff() throws Exception {
                if (lazy) {
                    rootEntity = ModelMgr.getModelMgr().getEntityById(rootId.toString());
                }
                else {
                    // TODO: do we want to be loading a cached tree?
                    rootEntity = ModelMgr.getModelMgr().getCachedEntityTree(rootId);
                }
            }

            protected void hadSuccess() {
                try {
                    initializeTree(rootEntity);
                    if (null != selectedTree) {
                        ToolTipManager.sharedInstance().registerComponent(selectedTree);
                    }
                }
                catch (Exception e) {
                    hadError(e);
                }
            }

            protected void hadError(Throwable error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(EntityTree.this, "Error loading folders", "Folder Load Error", JOptionPane.ERROR_MESSAGE);
                treesPanel.removeAll();
                EntityTree.this.updateUI();
            }

        };

        loadingWorker.execute();
    }

    public void initializeTree(final Entity rootEntity) {

        createNewTree(rootEntity);
        addNodes(null, rootEntity);

        selectedTree.expand(selectedTree.getRootNode(), true);

        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        EntityTree.this.updateUI();
    }

    /**
     * Get all the descendants of the given entity which are of a certain type.
     * TODO: move this to Entity class
     *
     * @param entity
     * @param typeName
     * @return
     */
    public List<Entity> getDescendantsOfType(Entity entity, String typeName) {

        List<Entity> items = new ArrayList<Entity>();
        if (typeName.equals(entity.getEntityType().getName())) {
            items.add(entity);
        }

        for (EntityData entityData : entity.getOrderedEntityData()) {
            Entity child = entityData.getChildEntity();
            if (child != null) {
                items.addAll(getDescendantsOfType(child, typeName));
            }
        }

        return items;
    }

    public DynamicTree getDynamicTree() {
        return selectedTree;
    }

    public JTree getTree() {
        return selectedTree.getTree();
    }

    public Entity getCurrentRootEntity() {
        return (Entity) selectedTree.getRootNode().getUserObject();
    }

    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user left clicks a node.
     *
     * @param e
     */
    protected void nodeClicked(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user presses down on a node.
     *
     * @param e
     */
    protected void nodePressed(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user double clicks a node.
     *
     * @param e
     */
    protected void nodeDoubleClicked(MouseEvent e) {
    }

    private void createNewTree(Entity root) {

        selectedTree = new DynamicTree(root, true, lazy) {

            protected void showPopupMenu(MouseEvent e) {
                EntityTree.this.showPopupMenu(e);
            }

            protected void nodeClicked(MouseEvent e) {
                EntityTree.this.nodeClicked(e);
            }

            protected void nodePressed(MouseEvent e) {
                EntityTree.this.nodePressed(e);
            }

            protected void nodeDoubleClicked(MouseEvent e) {
                EntityTree.this.nodeDoubleClicked(e);
            }

            @Override
            public void expandNodeWithLazyChildren(final DefaultMutableTreeNode node) {

                SimpleWorker loadingWorker = new LazyTreeNodeExpansionWorker(selectedTree, node, false) {
                    protected void doneExpanding() {
                        // Re-expand the node because the model was updated
                        expand(node, true);
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
                    }
                };

                loadingWorker.execute();
            }

            @Override
            public void loadLazyNodeData(DefaultMutableTreeNode node, boolean recurse) {
                Entity entity = (Entity) node.getUserObject();
                loadLazyEntity(entity, recurse);
            }

            @Override
            public int recreateChildNodes(DefaultMutableTreeNode node) {
                Entity entity = (Entity) node.getUserObject();
                ArrayList<EntityData> edList = new ArrayList<EntityData>(entity.getEntityData());

                if (!areLoaded(edList)) {
                    throw new IllegalStateException("replaceLazyChildren called on node whose children have not been loaded");
                }

                selectedTree.removeChildren(node);
                return addChildren(node, edList);
            }

            @Override
            public void expandAll(final boolean expand) {

                if (!expand || !isLazyLoading()) {
                    super.expandAll(expand);
                    return;
                }

                // Expanding a lazy tree is hard. Let's eager load the entire thing first.

                getDynamicTree().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                final SimpleWorker loadingWorker = new SimpleWorker() {

                    private Entity rootEntity;

                    @Override
                    protected void doStuff() throws Exception {
                        long rootId = ((Entity) getRootNode().getUserObject()).getId();
                        rootEntity = ModelMgr.getModelMgr().getEntityTree(rootId);
                    }

                    @Override
                    protected void hadSuccess() {
                        // The tree is no longer lazy
                        setLazyLoading(false);
                        getDynamicTree().setCursor(Cursor.getDefaultCursor());
                        DefaultMutableTreeNode rootNode = getRootNode();
                        rootNode.setUserObject(rootEntity);
                        recreateChildNodes(rootNode, true);
                        expandAll(new TreePath(rootNode), expand);
                        SwingUtilities.updateComponentTreeUI(EntityTree.this);
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        getDynamicTree().setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(EntityTree.this, "Error loading tree", "Internal Error", JOptionPane.ERROR_MESSAGE);
                    }
                };

                loadingWorker.execute();
            }
        };

        // Replace the cell renderer

        selectedTree.setCellRenderer(new EntityTreeCellRenderer());
    }

    private void loadLazyEntity(Entity entity, boolean recurse) {

        if (!areLoaded(entity.getEntityData())) {
            Set<Entity> childEntitySet = ModelMgr.getModelMgr().getChildEntities(entity.getId());
            Map<Long, Entity> childEntityMap = new HashMap<Long, Entity>();
            for (Entity childEntity : childEntitySet) {
                childEntityMap.put(childEntity.getId(), childEntity);
            }

            // Replace the entity data with real objects
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    ed.setChildEntity(childEntityMap.get(ed.getChildEntity().getId()));
                }
            }
        }

        if (recurse) {
            for (EntityData ed : entity.getEntityData()) {
                if (ed.getChildEntity() != null) {
                    loadLazyEntity(ed.getChildEntity(), true);
                }
            }
        }
    }

    private boolean areLoaded(Collection<EntityData> eds) {
        for (EntityData entityData : eds) {
            if (!Hibernate.isInitialized(entityData.getChildEntity())) {
                return false;
            }
        }
        return true;
    }

    private void addNodes(DefaultMutableTreeNode parentNode, Entity newEntity) {

        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, newEntity);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.getRootNode();
        }

        Set<EntityData> dataSet = newEntity.getEntityData();
        List<EntityData> childDataList = new ArrayList<EntityData>();

        for (EntityData ed : dataSet) {
            if (ed.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) {
                childDataList.add(ed);
            }
        }

        if (childDataList.isEmpty()) return;

        // Test for proxies
        boolean allLoaded = areLoaded(childDataList);

        if (!allLoaded) {
            selectedTree.addObject(newNode, new LazyTreeNode());
        }
        else {
            addChildren(newNode, childDataList);
        }

    }

    private int addChildren(DefaultMutableTreeNode parentNode, List<EntityData> dataList) {

        Collections.sort(dataList, new Comparator<EntityData>() {
            @Override
            public int compare(EntityData o1, EntityData o2) {
                if (o1.getOrderIndex() == null) {
                    if (o2.getOrderIndex() == null) {
                        Entity child1 = o1.getChildEntity();
                        Entity child2 = o2.getChildEntity();
                        if (child1 == null) {
                            if (child2 == null) {
                                return o1.getId().compareTo(o2.getId());
                            }
                            else {
                                return -1;
                            }
                        }
                        else if (child2 == null) {
                            return 1;
                        }
                        return child1.getName().compareTo(child2.getName());

                    }
                    return -1;
                }
                else if (o2.getOrderIndex() == null) {
                    return 1;
                }
                return o1.getOrderIndex().compareTo(o2.getOrderIndex());
            }
        });

        int c = 0;
        for (EntityData entityData : dataList) {
            Entity child = entityData.getChildEntity();
            if (child != null) {
                addNodes(parentNode, child);
                c++;
            }
        }
        return c;
    }
}
