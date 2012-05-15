/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 5/6/11
 * Time: 10:47 AM
 */
package org.janelia.it.FlyWorkstation.gui.framework.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrAdapter;
import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.FlyWorkstation.gui.application.SplashPanel;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeyboardShortcut;
import org.janelia.it.FlyWorkstation.gui.framework.keybind.KeymapUtil;
import org.janelia.it.FlyWorkstation.gui.framework.outline.*;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.BrowserModel;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionModelListener;
import org.janelia.it.FlyWorkstation.gui.util.*;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This panel shows images for annotation. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconDemoPanel extends Viewer {

	protected SplashPanel splashPanel;
	protected JToolBar toolbar;
	protected JButton prevButton;
	protected JButton nextButton;
	protected JButton parentButton;
	protected JToggleButton showTitlesButton;
	protected JButton imageRoleButton;
	protected JToggleButton showTagsButton;
	protected JButton refreshButton;
	protected JButton userButton;
	protected JSlider imageSizeSlider;
	protected ImagesPanel imagesPanel;
	protected JPanel statusBar;
	protected JLabel statusLabel;
	protected Hud hud;
	
	// The parent entity which we are displaying children for
	protected RootedEntity contextRootedEntity;
	
	// Children of the parent entity
	protected List<RootedEntity> rootedEntities;
	protected Map<String,RootedEntity> rootedEntityMap;
	protected Map<Long,Entity> entityMap;
	
	protected int currImageSize;
	protected int currTableHeight = ImagesPanel.DEFAULT_TABLE_HEIGHT;
	
	protected final List<String> allUsers = new ArrayList<String>();
	protected final Set<String> hiddenUsers = new HashSet<String>();
	protected final Annotations annotations = new Annotations();

	protected final List<String> allImageRoles = new ArrayList<String>();
	protected String currImageRole;
	
	private SimpleWorker entityLoadingWorker;
	private SimpleWorker annotationLoadingWorker;

	// Listen for key strokes and execute the appropriate key bindings
	protected KeyListener keyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {

			if (KeymapUtil.isModifier(e)) return;
			if (e.getID() != KeyEvent.KEY_PRESSED) return;

			KeyboardShortcut shortcut = KeyboardShortcut.createShortcut(e);
			if (!SessionMgr.getKeyBindings().executeBinding(shortcut)) {

				// No keybinds matched, use the default behavior

				// Ctrl-A or Meta-A to select all
				if (e.getKeyCode() == KeyEvent.VK_A && ((SystemInfo.isMac && e.isMetaDown()) || (e.isControlDown()))) {
					for (RootedEntity rootedEntity : rootedEntities) { 
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), false);
					}
					return;
				}
				
				// Space on a single entity triggers a preview 
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					updateHud();
					hud.showDialog();
					e.consume();
					return;
				}

				// Enter with a single entity selected triggers an outline
				// navigation
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
					if (selectedIds.size() != 1) return;
					String selectedId = selectedIds.get(0);
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, selectedId, true);
					return;
				}

				// Tab and arrow navigation to page through the images
				boolean clearAll = false;
				RootedEntity rootedEntity = null;
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					clearAll = true;
					if (e.isShiftDown()) {
						rootedEntity = getPreviousEntity();
					} else {
						rootedEntity = getNextEntity();
					}
				} else {
					clearAll = true;
					if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						rootedEntity = getPreviousEntity();
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						rootedEntity = getNextEntity();
					}
				}

				if (rootedEntity != null) {
					AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
					if (button != null) {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), clearAll);
						imagesPanel.scrollEntityToCenter(rootedEntity);
					}
				}
			}

			revalidate();
			repaint();

		}
	};

	protected JPopupMenu getPopupMenu(AnnotatedImageButton button) {

		RootedEntity rootedEntity = button.getRootedEntity();
		if (!button.isSelected()) {
			ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntity.getId(), true);
		}
		
		List<String> selectionIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());				
		JPopupMenu popupMenu = null;
		if (selectionIds.size()>1) {
			List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
			for (String entityId : selectionIds) {
				rootedEntityList.add(getRootedEntityById(entityId));
			}
			popupMenu = new EntityContextMenu(rootedEntityList);
			((EntityContextMenu)popupMenu).addMenuItems();
		}
		else {
			popupMenu = new EntityContextMenu(rootedEntity);
            ((EntityContextMenu)popupMenu).addMenuItems();
		}
        
		return popupMenu;
	}
	
	protected void buttonDrillDown(AnnotatedImageButton button) {
		RootedEntity rootedEntity = button.getRootedEntity();
		RootedEntity contextRootedEntity = getContextRootedEntity();
		if (contextRootedEntity==null || contextRootedEntity==rootedEntity) return;
    	if (StringUtils.isEmpty(rootedEntity.getUniqueId())) return;
		ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, rootedEntity.getUniqueId(), true);	
	}
	
	protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
		final String category = getSelectionCategory();
		final RootedEntity rootedEntity = button.getRootedEntity();
		final String rootedEntityId = rootedEntity.getId();
		
		if (multiSelect) {
			// With the meta key we toggle items in the current
			// selection without clearing it
			if (!button.isSelected()) {
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, rootedEntityId, false);
			} 
			else {
				ModelMgr.getModelMgr().getEntitySelectionModel().deselectEntity(category, rootedEntityId);
			}
			// Always request focus on the button that was clicked, 
			// since other buttons may become selected if shift is involved
			button.requestFocus();
		} 
		else {
			// With shift, we select ranges
			String lastSelected = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(getSelectionCategory());
			if (rangeSelect && lastSelected != null) {
				// Walk through the buttons and select everything between the last and current selections
				boolean selecting = false;
				List<RootedEntity> rootedEntities = getRootedEntities();
				for (RootedEntity otherRootedEntity : rootedEntities) {
					if (otherRootedEntity.getId().equals(lastSelected) || otherRootedEntity.getId().equals(rootedEntityId)) {
						if (otherRootedEntity.getId().equals(rootedEntityId)) {
							// Always select the button that was clicked
							ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
						}
						if (selecting) return; // We already selected, this is the end
						selecting = true; // Start selecting
						continue; // Skip selection of the first and last items, which should already be selected
					}
					if (selecting) {
						ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, otherRootedEntity.getId(), false);
					}
				}
				// Always request focus on the button that was clicked, 
				// since other buttons may become selected if shift is involved
				button.requestFocus();
			} 
			else {
				// This is a good old fashioned single button selection
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(category, rootedEntityId, true);
			}
		}
	}
	
	protected MouseListener buttonMouseListener = new MouseHandler() {

		@Override
		protected void popupTriggered(MouseEvent e) {
			if (e.isConsumed()) return;
			AnnotatedImageButton button = (AnnotatedImageButton)e.getComponent();
			getPopupMenu(button).show(e.getComponent(), e.getX(), e.getY());
			e.consume();
		}

		@Override
		protected void doubleLeftClicked(MouseEvent e) {
			if (e.isConsumed()) return;
			AnnotatedImageButton button = (AnnotatedImageButton)e.getComponent();
			buttonDrillDown(button);
			// Double-clicking an image in gallery view triggers an outline selection
    		e.consume();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
			if (e.isConsumed()) return;
			
			Component c = e.getComponent();
			while (!(c instanceof AnnotatedImageButton)) {
				c = c.getParent();
			}
			AnnotatedImageButton button = (AnnotatedImageButton)c;
			
			if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 1) {
				return;
			}
			buttonSelection(button, (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown(), e.isShiftDown());
		}
	};
	
	public IconDemoPanel(final String selectionCategory) {
		this(null, selectionCategory);
	}
	
	public IconDemoPanel(final ViewerSplitPanel viewerContainer, final String selectionCategory) {

		super(viewerContainer, selectionCategory);
		
		currImageRole = EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE;
		
		setBorder(BorderFactory.createEmptyBorder());
		setBackground(Color.white);
		setLayout(new BorderLayout());
		setFocusable(true);

		hud = new Hud();

		hud.getJDialog().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					hud.hideDialog();
				}
				else {
					RootedEntity rootedEntity = null;
					if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_UP) {
						rootedEntity = getPreviousEntity();
					} 
					else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_DOWN) {
						rootedEntity = getNextEntity();
					}
					
					if (rootedEntity==null) {
						hud.hideDialog();
						return;
					}
					ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(selectionCategory, rootedEntity.getId(), true);
					updateHud();
				}
			}
		});
		
		splashPanel = new SplashPanel();
		add(splashPanel);

		toolbar = createToolbar();
		imagesPanel = new ImagesPanel(this);
		imagesPanel.setButtonKeyListener(keyListener);
		imagesPanel.setButtonMouseListener(buttonMouseListener);
		imagesPanel.addMouseListener(new MouseForwarder(this, "ImagesPanel->IconDemoPanel"));
		toolbar.addMouseListener(new MouseForwarder(this, "JToolBar->IconDemoPanel"));
		
		statusBar = new JPanel();
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.LINE_AXIS));
		statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, (Color)UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
		
		statusBar.add(Box.createRigidArea(new Dimension(10,20)));
        statusLabel = new JLabel("");
        statusBar.add(statusLabel);
        statusBar.add(Box.createRigidArea(new Dimension(10,20)));
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				setAsActive();
			}
		});
		
		imageSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int imageSize = source.getValue();
				if (currImageSize == imageSize) return;
				currImageSize = imageSize;
				imagesPanel.rescaleImages(imageSize);
				imagesPanel.recalculateGrid();
				imagesPanel.loadUnloadImages();
			}
		});
		
		this.addKeyListener(getKeyListener());

		ModelMgr.getModelMgr().addModelMgrObserver(new ModelMgrAdapter() {

			@Override
			public void annotationsChanged(final long entityId) {
				if (rootedEntities!=null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							reloadAnnotations(entityId);
							filterEntities();
						}
					});	
				}
			}

			@Override
			public void entitySelected(String category, String entityId, boolean clearAll) {
				if (category.equals(selectionCategory)) {
					imagesPanel.setSelection(entityId, true, clearAll);
					updateHud();
					updateStatusBar();
				}
			}

			@Override
			public void entityDeselected(String category, String entityId) {
				if (category.equals(selectionCategory)) {
					imagesPanel.setSelection(entityId, false, false);
					updateHud();
					updateStatusBar();
				}
			}

			@Override
			public void entityChanged(final long entityId) {
				if (contextRootedEntity==null) return;
				if (contextRootedEntity.getEntity().getId().equals(entityId)) {
					refresh();	
				}
				else {
					SimpleWorker worker = new SimpleWorker() {
						private Entity newEntity;
						
						@Override
						protected void doStuff() throws Exception {
							newEntity = ModelMgr.getModelMgr().getEntityById(entityId+"");
						}
						
						@Override
						protected void hadSuccess() {
							for(AnnotatedImageButton button : imagesPanel.getButtonsByEntityId(entityId)) {
								RootedEntity rootedEntity = button.getRootedEntity();
								if (rootedEntity != null) {
									ModelMgrUtils.updateEntity(rootedEntity.getEntity(), newEntity);	
									button.refresh(rootedEntity);
									button.setViewable(true);
								}
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
			
			@Override
			public void entityRemoved(long entityId) {
				if (contextRootedEntity==null) return;
				if (contextRootedEntity.getEntity()!=null && contextRootedEntity.getEntity().getId().equals(entityId)) {
					goParent();
				}
				else {
					for(RootedEntity rootedEntity : rootedEntities) {
						Entity entity = rootedEntity.getEntity();
						if (entity.getId()!=null && entity.getId().equals(entityId)) {
							removeRootedEntity(rootedEntity);
							return;
						}
					}	
				}
			}

			@Override
			public void entityDataRemoved(long entityDataId) {
				if (contextRootedEntity==null) return;
				if (contextRootedEntity.getEntityData().getId()!=null && contextRootedEntity.getEntityData().getId().equals(entityDataId)) {
					goParent();
				}
				else {
					for(RootedEntity rootedEntity : rootedEntities) {
						EntityData entityData = rootedEntity.getEntityData();
						if (entityData.getId()!=null && entityData.getId().equals(entityDataId)) {
							removeRootedEntity(rootedEntity);
							return;
						}
					}
				}
			}
		});

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				imagesPanel.recalculateGrid();
			}
		});

		annotations.setFilter(new AnnotationFilter() {
			@Override
			public boolean accept(OntologyAnnotation annotation) {
				
				// Hidden by user?
				if (hiddenUsers.contains(annotation.getOwner())) return false;
				AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
				
				// Hidden by session?
				Boolean onlySession = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
						ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY);
				if ((onlySession!=null && !onlySession) || session == null) return true;
				
				// At this point we know there is a current session, and we have to match it
				return (annotation.getSessionId() != null && annotation.getSessionId().equals(session.getId()));
			}
		});

		SessionMgr.getSessionMgr().addSessionModelListener(new SessionModelListener() {
			
			@Override
			public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
				
				if (ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY.equals(key)) {
					Utils.setWaitingCursor(IconDemoPanel.this);
					try {
						imagesPanel.setInvertedColors((Boolean)newValue);
						imagesPanel.repaint();
					} finally {
						Utils.setDefaultCursor(IconDemoPanel.this);
					}	
				}
				else if (ViewerSettingsPanel.ONLY_SESSION_ANNOTATIONS_PROPERTY.equals(key)) {
					refreshAnnotations(null);
				}
				else if (ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY.equals(key)) {
					filterEntities();
				}
				else if (ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY.equals(key)) {
					imagesPanel.setTagTable((Boolean)newValue);
					imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
					imagesPanel.rescaleImages(imagesPanel.getCurrImageSize());
					imagesPanel.recalculateGrid();
					imagesPanel.loadUnloadImages();
				}
				else if (ViewerSettingsPanel.ANNOTATION_TABLES_HEIGHT_PROPERTY.equals(key)) {
					int tableHeight = (Integer)newValue;
					if (currTableHeight == tableHeight) return;
					currTableHeight = tableHeight;
					imagesPanel.resizeTables(tableHeight);
					imagesPanel.rescaleImages(currImageSize);
					imagesPanel.recalculateGrid();
					imagesPanel.scrollSelectedEntitiesToCenter();
					imagesPanel.loadUnloadImages();
				}
			}
			
			@Override
			public void sessionWillExit() {
			}
			
			@Override
			public void browserRemoved(BrowserModel browserModel) {
			}
			
			@Override
			public void browserAdded(BrowserModel browserModel) {
			}
		});
	}

	protected List<RootedEntity> getRootedEntitiesForEntityId(long entityId) {
		List<RootedEntity> rootedEntityList = new ArrayList<RootedEntity>();
		if (rootedEntities==null) return rootedEntityList; 
		for(RootedEntity rootedEntity : rootedEntities) {
			if (rootedEntity.getEntity().getId().equals(entityId)) {
				rootedEntityList.add(rootedEntity);
			}
		}
		return rootedEntityList;
	}

	private void updateStatusBar() {
		if (rootedEntities==null) return;
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		int s = esm.getSelectedEntitiesIds(getSelectionCategory()).size();
		statusLabel.setText(s+" of "+rootedEntities.size()+" selected");
	}
	
	private void updateHud() {
		List<String> selectedIds = ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(getSelectionCategory());
		if (selectedIds.size() != 1) {
			hud.hideDialog();
			return;
		}
		String selectedId = selectedIds.get(0);
		AnnotatedImageButton button = imagesPanel.getButtonById(selectedId);
		if (button instanceof DynamicImageButton) {
			DynamicImageButton d = (DynamicImageButton)button;
			BufferedImage bufferedImage = d.getDynamicImagePanel().getMaxSizeImage();
			if (bufferedImage==null) {
				return;
			}
			hud.setTitle(button.getRootedEntity().getEntity().getName());
			hud.setImage(bufferedImage);
		}
	}
	
	private synchronized void goBack() {
		final EntitySelectionHistory history = getEntitySelectionHistory();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				history.goBack();
			}
		});
	}

	private synchronized void goForward() {
		final EntitySelectionHistory history = getEntitySelectionHistory();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				history.goForward();
			}
		});
	}

	private synchronized void goParent() {
		final String selectedUniqueId = contextRootedEntity.getUniqueId();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(EntitySelectionModel.CATEGORY_OUTLINE, Utils.getParentIdFromUniqueId(selectedUniqueId), true);
			}
		});
	}

	private boolean isParentEnabled() {
		return (contextRootedEntity!=null && !StringUtils.isEmpty(Utils.getParentIdFromUniqueId(contextRootedEntity.getUniqueId())));
	}
	
	protected JToolBar createToolbar() {

		JToolBar toolBar = new JToolBar("Still draggable");
		toolBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, (Color)UIManager.get("windowBorder")), BorderFactory.createEmptyBorder(0, 5, 2, 5)));
		toolBar.setFloatable(true);
		toolBar.setRollover(true);

		prevButton = new JButton();
		prevButton.setIcon(Icons.getIcon("arrow_back.gif"));
		prevButton.setToolTipText("Go back in your browsing history");
		prevButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goBack();
			}
		});
		prevButton.addMouseListener(new MouseForwarder(toolBar, "PrevButton->JToolBar"));
		toolBar.add(prevButton);

		nextButton = new JButton();
		nextButton.setIcon(Icons.getIcon("arrow_forward.gif"));
		nextButton.setToolTipText("Go forward in your browsing history");
		nextButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goForward();
			}
		});
		nextButton.addMouseListener(new MouseForwarder(toolBar, "NextButton->JToolBar"));
		toolBar.add(nextButton);

		parentButton = new JButton();
		parentButton.setIcon(Icons.getIcon("parent.gif"));
		parentButton.setToolTipText("Go to the parent entity");
		parentButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		parentButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goParent();
			}
		});
		parentButton.addMouseListener(new MouseForwarder(toolBar, "ParentButton->JToolBar"));
		toolBar.add(parentButton);

		refreshButton = new JButton();
		refreshButton.setIcon(Icons.getRefreshIcon());
		refreshButton.setFocusable(false);
		refreshButton.setToolTipText("Refresh the current view");
		refreshButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		refreshButton.addMouseListener(new MouseForwarder(toolBar, "RefreshButton->JToolBar"));
		toolBar.add(refreshButton);

		toolBar.addSeparator();

		showTitlesButton = new JToggleButton();
		showTitlesButton.setIcon(Icons.getIcon("text_smallcaps.png"));
		showTitlesButton.setFocusable(false);
		showTitlesButton.setSelected(true);
		showTitlesButton.setToolTipText("Show the image title above each image.");
		showTitlesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
				imagesPanel.recalculateGrid();
				imagesPanel.loadUnloadImages();
			}
		});
		showTitlesButton.addMouseListener(new MouseForwarder(toolBar, "ShowTitlesButton->JToolBar"));
		toolBar.add(showTitlesButton);
		
		showTagsButton = new JToggleButton();
		showTagsButton.setIcon(Icons.getIcon("page_white_stack.png"));
		showTagsButton.setFocusable(false);
		showTagsButton.setSelected(true);
		showTagsButton.setToolTipText("Show annotations below each image");
		showTagsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imagesPanel.setTagVisbility(showTagsButton.isSelected());
				imagesPanel.recalculateGrid();
				imagesPanel.loadUnloadImages();
			}
		});
		showTagsButton.addMouseListener(new MouseForwarder(toolBar, "ShowTagsButton->JToolBar"));
		toolBar.add(showTagsButton);

		toolBar.addSeparator();
		
//		userButton = new JButton("Annotations from...");
//		userButton.setIcon(Icons.getIcon("group.png"));
//		userButton.setFocusable(false);
//		userButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				showPopupUserMenu();
//			}
//		});
//		userButton.addMouseListener(new MouseForwarder(toolBar, "UserButton->JToolBar"));
//		toolBar.add(userButton);
//		
//		toolBar.addSeparator();
		
		imageRoleButton = new JButton("Image type...");
		imageRoleButton.setIcon(Icons.getIcon("image.png"));
		imageRoleButton.setFocusable(false);
		imageRoleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupImageRoleMenu();
			}
		});
		imageRoleButton.addMouseListener(new MouseForwarder(toolBar, "ImageRoleButton->JToolBar"));
		toolBar.add(imageRoleButton);

		toolBar.addSeparator();

		imageSizeSlider = new JSlider(ImagesPanel.MIN_THUMBNAIL_SIZE, ImagesPanel.MAX_THUMBNAIL_SIZE,
				ImagesPanel.DEFAULT_THUMBNAIL_SIZE);
		imageSizeSlider.setFocusable(false);
		imageSizeSlider.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
		imageSizeSlider.setToolTipText("Image size percentage");
		imageSizeSlider.addMouseListener(new MouseForwarder(toolBar, "ImageSizeSlider->JToolBar"));
		toolBar.add(imageSizeSlider);

		return toolBar;
	}

	private void showPopupImageRoleMenu() {

		final JPopupMenu imageRoleListMenu = new JPopupMenu();
		final List<String> imageRoles = new ArrayList<String>(allImageRoles);

		for (final String imageRole : imageRoles) {
			JMenuItem roleMenuItem = new JCheckBoxMenuItem(imageRole, imageRole.equals(currImageRole));
			roleMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					currImageRole = imageRole;
					entityLoadDone(null);
				}
			});
			imageRoleListMenu.add(roleMenuItem);
		}

		imageRoleListMenu.show(imageRoleButton, 0, imageRoleButton.getHeight());
	}
	
//	private void showPopupUserMenu() {
//
//		final JPopupMenu userListMenu = new JPopupMenu();
//
//		UserColorMapping userColors = ModelMgr.getModelMgr().getUserColorMapping();
//
//		// Save the list of users so that when the function actually runs, the
//		// users it affects are the same users that were displayed
//		final List<String> savedUsers = new ArrayList<String>(allUsers);
//
//		JMenuItem allUsersMenuItem = new JCheckBoxMenuItem("All users", hiddenUsers.isEmpty());
//		allUsersMenuItem.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				if (hiddenUsers.isEmpty()) {
//					for (String username : savedUsers) {
//						hiddenUsers.add(username);
//					}
//				} else {
//					hiddenUsers.clear();
//				}
//				refreshAnnotations(null);
//			}
//		});
//		userListMenu.add(allUsersMenuItem);
//
//		userListMenu.addSeparator();
//
//		for (final String username : savedUsers) {
//			JMenuItem userMenuItem = new JCheckBoxMenuItem(username, !hiddenUsers.contains(username));
//			userMenuItem.setBackground(userColors.getColor(username));
//			userMenuItem.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					if (hiddenUsers.contains(username))
//						hiddenUsers.remove(username);
//					else
//						hiddenUsers.add(username);
//					refreshAnnotations(null);
//				}
//			});
//			userMenuItem.setIcon(Icons.getIcon("user.png"));
//			userListMenu.add(userMenuItem);
//		}
//
//		userListMenu.show(userButton, 0, userButton.getHeight());
//	}
	
	public void showLoadingIndicator() {
		removeAll();
		add(new JLabel(Icons.getLoadingIcon()));
		this.updateUI();
	}

	public boolean areTitlesVisible() {
		return showTitlesButton.isSelected();
	}

	public boolean areTagsVisible() {
		return showTagsButton.isSelected();
	}
	
	public void loadEntity(RootedEntity rootedEntity) {
		loadEntity(rootedEntity, null);
	}
	
	public synchronized void loadEntity(RootedEntity rootedEntity, final Callable<Void> success) {
		
		this.contextRootedEntity = rootedEntity;
		if (contextRootedEntity==null) return;
		
		Entity entity = contextRootedEntity.getEntity();

		getEntitySelectionHistory().pushHistory(contextRootedEntity.getUniqueId());
		setTitle(entity.getName());
		
		List<EntityData> eds = entity.getOrderedEntityData();
		List<EntityData> children = new ArrayList<EntityData>();
		for(EntityData ed : eds) {
			Entity child = ed.getChildEntity();
			if (!EntityUtils.isHidden(ed) && child!=null) {
				children.add(ed);
			}
		}
		
		List<RootedEntity> rootedEntities = new ArrayList<RootedEntity>();
		for(EntityData ed : children) {
			String childId = EntityOutline.getChildUniqueId(rootedEntity.getUniqueId(), ed);
			rootedEntities.add(new RootedEntity(childId, ed));
		}

		if (rootedEntities.isEmpty()) {
			rootedEntities.add(rootedEntity);
		}
		
		loadImageEntities(rootedEntities, success); 
	}

	public void loadImageEntities(final List<RootedEntity> rootedEntities) {
		loadImageEntities(rootedEntities, null);
	}

	private synchronized void loadImageEntities(final List<RootedEntity> rootedEntities, final Callable<Void> success) {

		// Indicate a load
		showLoadingIndicator();
		
		// Cancel previous loads
		if (entityLoadingWorker != null && !entityLoadingWorker.isDone()) {
			System.out.println("Cancel previous entity load");
			entityLoadingWorker.disregard();
		}
		imagesPanel.cancelAllLoads();

		// Update back/forward navigation
		EntitySelectionHistory history = getEntitySelectionHistory();
		prevButton.setEnabled(history.isBackEnabled());
		nextButton.setEnabled(history.isNextEnabled());
		parentButton.setEnabled(isParentEnabled());

		// Temporarily disable scroll loading
		imagesPanel.setScrollLoadingEnabled(false);
		
		entityLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				List<RootedEntity> loadedRootedEntities = new ArrayList<RootedEntity>();
				for (RootedEntity rootedEntity : rootedEntities) {
					if (!EntityUtils.isInitialized(rootedEntity.getEntity())) {
						System.out.println("Warning: had to load entity "+rootedEntity.getEntity().getId());
						rootedEntity.getEntityData().setChildEntity(ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId()+""));
					}
					EntityData defaultImageEd = rootedEntity.getEntity().getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
					if (defaultImageEd!= null && defaultImageEd.getValue() == null && defaultImageEd.getChildEntity()!=null) {
						System.out.println("Warning: had to load default image "+rootedEntity.getEntity().getName());
						defaultImageEd.setChildEntity(ModelMgr.getModelMgr().getEntityById(defaultImageEd.getChildEntity().getId() + ""));
					}
					loadedRootedEntities.add(rootedEntity);
				}
				setRootedEntities(loadedRootedEntities);
				annotations.init(getDistinctEntities());
			}

			protected void hadSuccess() {
				entityLoadDone(success);
			}

			protected void hadError(Throwable error) {
				entityLoadError(error);
			}
		};

		entityLoadingWorker.execute();
	}
	
	private synchronized void entityLoadDone(final Callable<Void> success) {
		
		if (!SwingUtilities.isEventDispatchThread())
			throw new RuntimeException("IconDemoPanel.entityLoadDone called outside of EDT");

		imagesPanel.setRootedEntities(getRootedEntities());
		refreshAnnotations(null);

		showAllEntities();
		filterEntities();

		Boolean invertImages = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.INVERT_IMAGE_COLORS_PROPERTY);
		Boolean tagTable = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.SHOW_ANNOTATION_TABLES_PROPERTY);
		if (invertImages==null) invertImages = false;
        if (tagTable==null) tagTable = false;

		imagesPanel.setTagTable(tagTable);
		imagesPanel.setTagVisbility(showTagsButton.isSelected());
		imagesPanel.setTitleVisbility(showTitlesButton.isSelected());
		imagesPanel.setInvertedColors(invertImages);
		
		// Since the images are not loaded yet, this will just resize the empty
		// buttons so that we can calculate the grid correctly
		imagesPanel.resizeTables(imagesPanel.getCurrTableHeight());
		imagesPanel.rescaleImages(imagesPanel.getCurrImageSize());
		
		revalidate();
		repaint();

		// Wait until everything is recomputed
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				imagesPanel.recalculateGrid();
				imagesPanel.setScrollLoadingEnabled(true);
				imagesPanel.loadUnloadImages();
				// Select the first entity
				ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(getSelectionCategory(), rootedEntities.get(0).getId(), true);
				// Finally, we're done, we can call the success callback
				if (success != null) {
					try {
						success.call();
					} 
					catch (Exception e) {
						SessionMgr.getSessionMgr().handleException(e);
					}
				}
			}
		});
	}

	private synchronized void entityLoadError(Throwable error) {

		error.printStackTrace();
		if (rootedEntities != null) {
			JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading annotations", "Data Loading Error",
					JOptionPane.ERROR_MESSAGE);
			imagesPanel.setRootedEntities(rootedEntities);
			showAllEntities();
			// TODO: set read-only mode
		} else {
			JOptionPane.showMessageDialog(IconDemoPanel.this, "Error loading session", "Data Loading Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void removeRootedEntity(final RootedEntity rootedEntity) {
		int index = rootedEntities.indexOf(rootedEntity);
		if (index < 0) return;
		rootedEntities.remove(index);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				imagesPanel.removeRootedEntity(rootedEntity);
				imagesPanel.recalculateGrid();
				imagesPanel.loadUnloadImages();
				revalidate();
				repaint();
			}
		});
	}

	private void filterEntities() {

		AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
		if (session == null) {
			return;
		}
		session.clearCompletedIds();
		Set<Long> completed = session.getCompletedEntityIds();
		
		imagesPanel.showAllButtons();
		Boolean hideAnnotated = (Boolean)SessionMgr.getSessionMgr().getModelProperty(
				ViewerSettingsPanel.HIDE_ANNOTATED_PROPERTY);
		if (hideAnnotated!=null && hideAnnotated) {
			imagesPanel.hideButtons(completed);	
		}
	}

	/**
	 * Reload the annotations from the database and then refresh the UI.
	 */
	public synchronized void reloadAnnotations() {

		if (annotations == null || rootedEntities == null)
			return;

		annotationLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				annotations.init(getDistinctEntities());
			}

			protected void hadSuccess() {
				refreshAnnotations(null);
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		annotationLoadingWorker.execute();
	}

	/**
	 * Reload the annotations from the database and then refresh the UI.
	 */
	public synchronized void reloadAnnotations(final Long entityId) {

		if (annotations == null || rootedEntities == null)
			return;

		annotationLoadingWorker = new SimpleWorker() {

			protected void doStuff() throws Exception {
				annotations.reload(entityId);
			}

			protected void hadSuccess() {
				refreshAnnotations(entityId);
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		annotationLoadingWorker.execute();
	}

	/**
	 * Refresh the annotation display in the UI, but do not reload anything from
	 * the database.
	 */
	private synchronized void refreshAnnotations(Long entityId) {

		// Refresh all user list
		allUsers.clear();
		for (OntologyAnnotation annotation : annotations.getAnnotations()) {
			if (!allUsers.contains(annotation.getOwner()))
				allUsers.add(annotation.getOwner());
		}
		Collections.sort(allUsers);

		if (entityId == null) {
			imagesPanel.loadAnnotations(annotations);
		} else {
			imagesPanel.loadAnnotations(annotations, entityId);
		}

	}
	
	@Override
	public void refresh() {

		if (contextRootedEntity==null) return;
		
		SimpleWorker refreshWorker = new SimpleWorker() {

			RootedEntity rootedEntity = contextRootedEntity;
			
			protected void doStuff() throws Exception {
				Entity entity = ModelMgr.getModelMgr().getEntityById(rootedEntity.getEntity().getId()+"");
				if (entity==null) return;
				ModelMgrUtils.loadLazyEntity(entity, false);
				rootedEntity.getEntityData().setChildEntity(entity);
			}

			protected void hadSuccess() {
				if (rootedEntity.getEntity()==null) {
					clear();
				}
				else {
					loadEntity(rootedEntity);	
				}
			}

			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};

		refreshWorker.execute();
	}

	public void clear() {
		this.contextRootedEntity = null;
		this.rootedEntities = null;
		this.rootedEntityMap = null;
		this.entityMap = null;
		
		setTitle("");
		removeAll();
		add(splashPanel, BorderLayout.CENTER);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			}
		});
	}

	public synchronized void showAllEntities() {

		removeAll();
		add(toolbar, BorderLayout.NORTH);
		add(imagesPanel, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		
		revalidate();
		repaint();

		// Focus on the panel so that it can receive keyboard input
		requestFocusInWindow();
	}

	public RootedEntity getPreviousEntity() {
		int i = rootedEntities.indexOf(getLastSelectedEntity());
		if (i < 1) {
			// Already at the beginning
			return null;
		}
		return rootedEntities.get(i - 1);
	}

	public RootedEntity getNextEntity() {
		int i = rootedEntities.indexOf(getLastSelectedEntity());
		if (i > rootedEntities.size() - 2) {
			// Already at the end
			return null;
		}
		return rootedEntities.get(i + 1);
	}

	public synchronized List<RootedEntity> getRootedEntities() {
		return rootedEntities;
	}

	private synchronized void setRootedEntities(List<RootedEntity> rootedEntities) {
		this.rootedEntities = rootedEntities;
		this.rootedEntityMap = new HashMap<String,RootedEntity>();
		this.entityMap = new HashMap<Long,Entity>();
		
		Set<String> imageRoles = new HashSet<String>();
		for(RootedEntity rootedEntity : rootedEntities) {
			
			rootedEntityMap.put(rootedEntity.getId(), rootedEntity);
			entityMap.put(rootedEntity.getEntity().getId(), rootedEntity.getEntity());
			
			Entity entity = rootedEntity.getEntity();
			for(EntityData ed : entity.getEntityData()) {
				String attrName = ed.getEntityAttribute().getName();
				if (attrName.endsWith("Image")) {
					imageRoles.add(attrName);
				}
			}
		}
		
		allImageRoles.clear();
		allImageRoles.addAll(imageRoles);
		Collections.sort(allImageRoles);
		
		imageRoleButton.setEnabled(!allImageRoles.isEmpty());
	}

	public synchronized RootedEntity getLastSelectedEntity() {
		String entityId = ModelMgr.getModelMgr().getEntitySelectionModel().getLastSelectedEntityId(getSelectionCategory());
		if (entityId == null) return null;
		AnnotatedImageButton button = imagesPanel.getButtonById(entityId);
		if (button == null) return null;
		return button.getRootedEntity();
	}

	public synchronized List<RootedEntity> getSelectedEntities() {
		List<RootedEntity> selectedEntities = new ArrayList<RootedEntity>();
		if (rootedEntities==null) return selectedEntities;
		for(RootedEntity rootedEntity : rootedEntities) {
			AnnotatedImageButton button = imagesPanel.getButtonById(rootedEntity.getId());
			if (button.isSelected()) {
				selectedEntities.add(rootedEntity);
			}
		}
		return selectedEntities;
	}
	
	public EntityData getEntityDataWithEntityId(Long entityId) {
		for(RootedEntity rootedEntity : rootedEntities) {
			if (rootedEntity.getEntity().getId().equals(entityId)) {
				return rootedEntity.getEntityData();
			}
		}
		return null;
	}

	public String getCurrImageRole() {
		return currImageRole;
	}

	public ImagesPanel getImagesPanel() {
		return imagesPanel;
	}

	public JSlider getImageSizeSlider() {
		return imageSizeSlider;
	}

	public SplashPanel getSplashPanel() {
		return splashPanel;
	}

	public KeyListener getKeyListener() {
		return keyListener;
	}

	public double getCurrImageSizePercent() {
		return currImageSize;
	}

	public Annotations getAnnotations() {
		return annotations;
	}

	public RootedEntity getContextRootedEntity() {
		return contextRootedEntity;
	}

	public List<Entity> getDistinctEntities() {
		Map<Long,Entity> entities = new HashMap<Long,Entity>();
		for(RootedEntity rootedEntity : rootedEntities) {
			entities.put(rootedEntity.getEntity().getId(), rootedEntity.getEntity());
		}
		return new ArrayList<Entity>(entities.values());
	}
	
	@Override
	public RootedEntity getRootedEntityById(String id) {
		if (rootedEntityMap==null) return null;
		return rootedEntityMap.get(id);
	}
	
	@Override	
	public Entity getEntityById(Long id) {
		if (entityMap==null) return null;
		return entityMap.get(id);
	}
}
