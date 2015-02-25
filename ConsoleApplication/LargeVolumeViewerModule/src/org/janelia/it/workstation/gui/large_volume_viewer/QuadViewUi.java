package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.camera.BasicObservableCamera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.TileServer.LoadStatus;
import org.janelia.it.workstation.gui.large_volume_viewer.action.*;
import org.janelia.it.workstation.gui.large_volume_viewer.action.MouseMode.Mode;
import org.janelia.it.workstation.gui.large_volume_viewer.action.OrthogonalModeAction.OrthogonalMode;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationPanel;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.LargeVolumeViewerTranslator;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.signal.Signal;
import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot;
import org.janelia.it.workstation.signal.Slot1;
import org.janelia.it.workstation.tracing.AnchoredVoxelPath;
import org.janelia.it.workstation.tracing.PathTraceToParentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.media.opengl.GLProfile;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Vector;
import java.util.List;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.MatrixDrivenSWCExchanger;
import org.janelia.it.workstation.gui.passive_3d.Snapshot3DLauncher;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.shared.util.SWCDataConverter;

/** 
 * Main window for QuadView application.
 * Maintained using Google WindowBuilder design tool.
 * 
 * @author Christopher M. Bruns
 *
 */
@SuppressWarnings("serial")
public class QuadViewUi extends JPanel
{
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(QuadViewUi.class);
	
    private static final String IMAGES_FOLDER_OPEN = "folder_open.png";
    private static final String IMAGES_GREEN_CHECK = "Green_check.png";
    private static final String IMAGES_SPINNER = "spinner.gif";
    private static final String IMAGES_MOUSE_SCROLL = "mouse_scroll.png";
    private static final String IMAGES_MOUSE_LEFT = "mouse_left.png";
    
    public static GLProfile glProfile = GLProfile.get(GLProfile.GL2);

	private boolean bAllowOrthoView = true; // false until ready for release
	
	// One shared camera for all viewers.
	// (there's only one viewer now actually, but you know...)
	private BasicObservableCamera3d camera = new BasicObservableCamera3d();
	GLContextSharer orthoViewContextSharer = new GLContextSharer(glProfile);

	private LargeVolumeViewer largeVolumeViewer = new LargeVolumeViewer(
			orthoViewContextSharer.getCapabilities(),
			orthoViewContextSharer.getChooser(),
			orthoViewContextSharer.getContext(),
			camera);

	// TODO - promote volumeImage, colorModel out of sliceviewer
	TileServer tileServer = largeVolumeViewer.getTileServer();
	private SharedVolumeImage volumeImage = tileServer.getSharedVolumeImage();
	private ImageColorModel imageColorModel = new ImageColorModel(volumeImage.getMaximumIntensity(), volumeImage.getNumberOfChannels());

	// Four quadrants for orthogonal views
	OrthogonalPanel neViewer = new OrthogonalPanel(CoordinateAxis.X, orthoViewContextSharer);
	OrthogonalPanel swViewer = new OrthogonalPanel(CoordinateAxis.Y, orthoViewContextSharer);
	OrthogonalPanel nwViewer = new OrthogonalPanel(CoordinateAxis.Z, orthoViewContextSharer);
	// TODO 3D view
	// TODO obsolete zViewerPanel in favor of OrthogonalPanel
	JPanel zViewerPanel = new JPanel();
	JComponent seViewer = zViewerPanel; // should be same as Z...

	// Group orthogonal viewers for use in action constructors
	List<TileConsumer> allSliceViewers = Arrays.asList(new TileConsumer[] {
			nwViewer.getViewer(),
			neViewer.getViewer(),
			swViewer.getViewer(),
            largeVolumeViewer
		});
	
	private boolean modifierKeyPressed = false;
	private JPanel zScanPanel = new JPanel();
	private JSlider zScanSlider = new JSlider();
	private JSpinner zScanSpinner = new JSpinner();
	private JSlider zoomSlider = new JSlider(SwingConstants.VERTICAL, 0, 1000, 500);
	
	//private JPanel colorPanel = new JPanel();
    private JMenuBar menuBar = new JMenuBar();
    private JPanel toolBarPanel = new JPanel();
	private JSplitPane splitPane = new JSplitPane();    
    private SliderPanel sliderPanel = new SliderPanel(imageColorModel);

	private JLabel statusLabel = new JLabel("status area");
	private LoadStatusLabel loadStatusLabel = new LoadStatusLabel();
	
	ZScanMode zScanMode = new ZScanMode(volumeImage);
	
	// annotation things
    private AnnotationPanel annotationPanel;
	private AnnotationModel annotationModel = new AnnotationModel();
	private AnnotationManager annotationMgr = new AnnotationManager(annotationModel, this, tileServer);
    private LargeVolumeViewerTranslator largeVolumeViewerTranslator = new LargeVolumeViewerTranslator(annotationModel, largeVolumeViewer);

	// Actions
	private final Action openFolderAction = new OpenFolderAction(largeVolumeViewer, this);
	private RecentFileList recentFileList = new RecentFileList(new JMenu("Open Recent"));
	private final Action resetViewAction = new ResetViewAction(allSliceViewers, volumeImage);
	private final Action resetColorsAction = new ResetColorsAction(imageColorModel);
	// mode actions (and groups)
	private final ZoomMouseModeAction zoomMouseModeAction = new ZoomMouseModeAction();
	private final PanModeAction panModeAction = new PanModeAction();
    private Skeleton skeleton = new Skeleton();
    private final TraceMouseModeAction traceMouseModeAction = new TraceMouseModeAction();
    // 
	private final ButtonGroup mouseModeGroup = new ButtonGroup();
	private final ZScanScrollModeAction zScanScrollModeAction = new ZScanScrollModeAction();
	private final ZoomScrollModeAction zoomScrollModeAction = new ZoomScrollModeAction();
	private final ButtonGroup scrollModeGroup = new ButtonGroup();
	private final OrthogonalModeAction orthogonalModeAction = new OrthogonalModeAction();
	// zoom actions
	private final Action zoomInAction = new ZoomInAction(camera);
	private final Action zoomOutAction = new ZoomOutAction(camera);
	private final Action zoomMaxAction = new ZoomMaxAction(camera, volumeImage);
	private final Action resetZoomAction = new ResetZoomAction(allSliceViewers, volumeImage);
	// Z scan actions
	private final SliceScanAction nextZSliceAction = new NextZSliceAction(volumeImage, camera);
	private final SliceScanAction previousZSliceAction = new PreviousZSliceAction(volumeImage, camera);
	private final SliceScanAction advanceZSlicesAction = new AdvanceZSlicesAction(volumeImage, camera, 10);
	private final SliceScanAction goBackZSlicesAction = new GoBackZSlicesAction(volumeImage, camera, -10);
    // go to actions
    private final GoToLocationAction goToLocationAction = new GoToLocationAction(camera);

	// annotation-related
    private final CenterNextParentAction centerNextParentAction = new CenterNextParentAction();
    private TileFormat tileFormat;
    
    private Snapshot3DLauncher snapshot3dLauncher;

	private final Action clearCacheAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			clearCache();
		}
	};
	private final Action autoContrastAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			largeVolumeViewer.autoContrastNow();
		}
	};
	private final Action collectGarbageAction = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			System.gc();
		}
	};

	public Signal1<MouseMode.Mode> mouseModeChangedSignal = 
		    new Signal1<MouseMode.Mode>();
	    public Signal1<WheelMode.Mode> wheelModeChangedSignal = 
	        new Signal1<WheelMode.Mode>();

    public Signal1<AnchoredVoxelPath> addAnchoredPathRequestSignal = new Signal1<AnchoredVoxelPath>();

    public Signal1<PathTraceToParentRequest> tracePathRequestedSignal = new Signal1<PathTraceToParentRequest>();

    public Signal closeWorkspaceRequestSignal = new Signal();

	private Slot1<MouseMode.Mode> onMouseModeChangedSlot = new Slot1<MouseMode.Mode>() {
		@Override
		public void execute(Mode mode) {
			// Only display anchors in Trace mode
			if (mode == MouseMode.Mode.TRACE)
				getSkeletonActor().setAnchorsVisible(true);
			else
				getSkeletonActor().setAnchorsVisible(false);
		}
	};
	    
	// Slots
	private Slot1<URL> loadUrlSlot = new Slot1<URL>() {
		@Override
		public void execute(URL url) {
			loadRender(url);
		}
	};
	    
	protected Slot1<Vec3> changeZ = new Slot1<Vec3>() {
		@Override
		public void execute(Vec3 focus) {
			int z = (int)Math.round((focus.getZ()-0.5) / volumeImage.getZResolution());
			zScanSlider.setValue(z);
			zScanSpinner.setValue(z);
		}
	};

	protected Slot1<Double> changeZoom = new Slot1<Double>() {
		@Override
		public void execute(Double zoom) {
			double zoomMin = Math.log(getMinZoom()) / Math.log(2.0);
			double zoomMax = Math.log(getMaxZoom()) / Math.log(2.0);
			double zoomLog = Math.log(zoom) / Math.log(2.0);
			double relativeZoom = (zoomLog - zoomMin) / (zoomMax - zoomMin);
			int sliderValue = (int)Math.round(relativeZoom * 1000.0);
			zoomSlider.setValue(sliderValue);
		}
	};
	
	public Slot1<Vec3> setCameraFocusSlot = new Slot1<Vec3>() {
		@Override
		public void execute(Vec3 focus) {
			camera.setFocus(focus);
		}
	};
	
	protected Slot updateRangesSlot = new Slot() {
		@Override
		public void execute() 
		{
			// Z range
			double zMin = volumeImage.getBoundingBox3d().getMin().getZ();
			double zMax = volumeImage.getBoundingBox3d().getMax().getZ();
			int z0 = (int)Math.round(zMin / volumeImage.getZResolution());
			int z1 = (int)Math.round(zMax / volumeImage.getZResolution()) - 1;
			if (z0 > z1)
				z1 = z0;
			// Z-scan is only relevant if there is more than one slice.
			boolean useZScan = ((z1 - z0) > 1);
			if (useZScan) {
				zScanPanel.setVisible(true);
				largeVolumeViewer.setWheelMode(WheelMode.Mode.SCAN);
				zScanScrollModeAction.setEnabled(true);
				zScanScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
				int z = (int)Math.round((camera.getFocus().getZ()-0.5) / volumeImage.getZResolution());
				if (z < z0)
					z = z0;
				if (z > z1)
					z = z1;
				zScanSlider.setMinimum(z0);
				zScanSlider.setMaximum(z1);
				zScanSlider.setValue(z);
				zScanSpinner.setModel(new SpinnerNumberModel(z, z0, z1, 1));
				// Allow octree zsteps to depend on zoom
				tileFormat = tileServer.getLoadAdapter().getTileFormat();
                updateSWCDataConverter();
                
				zScanMode.setTileFormat(tileFormat);
				nextZSliceAction.setTileFormat(tileFormat);
				previousZSliceAction.setTileFormat(tileFormat);
				advanceZSlicesAction.setTileFormat(tileFormat);
				goBackZSlicesAction.setTileFormat(tileFormat);
                skeleton.setTileFormat(tileFormat);
			}
			else { // no Z scan
				zScanPanel.setVisible(false);
				zoomScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
				zScanScrollModeAction.setEnabled(false);
			}
            
            snapshot3dLauncher.setMaxIntensity(volumeImage.getMaximumIntensity());
            snapshot3dLauncher.setNumberOfChannels(volumeImage.getNumberOfChannels());
		}
		// TODO update zoom range too?
	};

	public Slot1<String> setStatusMessageSlot = new Slot1<String>() {
		@Override
		public void execute(String message) {
			statusLabel.setText(message);
		}
	};
	
	private Slot1<LoadStatus> onLoadStatusChangedSlot = new Slot1<LoadStatus>() {
		@Override
		public void execute(LoadStatus status) {
			// log.info("load status ordinal "+status.ordinal());
			loadStatusLabel.setLoadStatus(status);
		}
	};

    public Slot centerNextParentSlot = new Slot() {
        @Override
        public void execute() {
            Anchor anchor = getSkeletonActor().getNextParent();
            if (anchor != null) {
                setCameraFocusSlot.execute(
                        anchor.getLocation()
                );
            }
        }
    };
	
    public Slot1<Long> tracePathSegmentSlot = new Slot1<Long>() {
        @Override
        public void execute(Long annotationID) {
            // this needs to happen before you draw anchored paths; should
            //  go somewhere else so it only happens once, but not clear where;
            //  not clear we have a trigger for when the image is loaded enough for
            //  this info to be available (it loads asynchronously)
            getSkeletonActor().setTileFormat(
                    tileServer.getLoadAdapter().getTileFormat());

            // construct new request; add image data to anchor and pass it on
            PathTraceToParentRequest request = new PathTraceToParentRequest(annotationID);
            request.setImageVolume(volumeImage);
            request.setTextureCache(tileServer.getTextureCache());
            tracePathRequestedSignal.emit(request);
        }
    };

    public Slot1<String> loadColorModelSlot = new Slot1<String>() {
        @Override
        public void execute(String modelString) {
            imageColorModelFromString(modelString);
        }
    };

    public Slot colorModelUpdatedSlot = new Slot() {
        @Override
        public void execute() {
            sliderPanel.updateLockButtons();
        }
    };

	/**
	 * Create the frame.
	 */
	public QuadViewUi(JFrame parentFrame, Entity initialEntity, boolean overrideFrameMenuBar)
	{
		volumeImage.volumeInitializedSignal.connect(onVolumeLoadedSlot);
        volumeImage.volumeInitializedSignal.connect(annotationMgr.onVolumeLoadedSlot);
		largeVolumeViewer.setImageColorModel(imageColorModel);
		camera.getViewChangedSignal().connect(tileServer.refreshCurrentTileSetSlot);
		tileServer.loadStatusChangedSignal.connect(onLoadStatusChangedSlot);
		sliderPanel.setVisible(false);
        
		setupUi(parentFrame, overrideFrameMenuBar);
        interceptModifierKeyPresses();
        interceptModeChangeGestures();
        setupAnnotationGestures();

        // connect up text UI and model with graphic UI(s):
        skeleton.addAnchorRequestedSignal.connect(annotationMgr.addAnchorRequestedSlot);
        tracePathRequestedSignal.connect(annotationMgr.tracePathRequestedSlot);
        closeWorkspaceRequestSignal.connect(annotationMgr.closeWorkspaceRequestedSlot);
        skeleton.subtreeDeleteRequestedSignal.connect(annotationMgr.deleteSubtreeRequestedSlot);
        skeleton.linkDeleteRequestedSignal.connect(annotationMgr.deleteLinkRequestedSlot);
        skeleton.splitAnchorRequestedSignal.connect(annotationMgr.splitAnchorRequestedSlot);
        skeleton.rerootNeuriteRequestedSignal.connect(annotationMgr.rerootNeuriteRequestedSlot);
        skeleton.splitNeuriteRequestedSignal.connect(annotationMgr.splitNeuriteRequestedSlot);
        getSkeletonActor().nextParentChangedSignal.connect(annotationMgr.selectAnnotationSlot);
        skeleton.anchorMovedSignal.connect(annotationMgr.moveAnchorRequestedSlot);
        // Nb: skeleton.anchorMovedSilentSignal intentially does *not* connect to annotationMgr!
        skeleton.pathTraceRequestedSignal.connect(tracePathSegmentSlot);
        annotationModel.pathTraceRequestedSignal.connect(tracePathSegmentSlot);
        addAnchoredPathRequestSignal.connect(annotationMgr.addPathRequestedSlot);
        skeleton.addEditNoteRequestedSignal.connect(annotationMgr.addEditNoteRequestedSlot);

        // Toggle skeleton actor with v key
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, false), "vKeyPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, true), "vKeyReleased");
        ActionMap actionMap = getActionMap();
        actionMap.put("vKeyPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
    			// log.info("Skeleton off");
    			getSkeletonActor().setVisible(false);
            }
        });
        actionMap.put("vKeyReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
    			// log.info("Skeleton on");
    			getSkeletonActor().setVisible(true);
            }
        });

        largeVolumeViewerTranslator.connectSkeletonSignals(skeleton);
        largeVolumeViewerTranslator.cameraPanToSignal.connect(setCameraFocusSlot);
        largeVolumeViewerTranslator.loadColorModelSignal.connect(loadColorModelSlot);


		// must come after setupUi() (etc), since it triggers UI changes:
		annotationMgr.setInitialEntity(initialEntity);

        //
        clearCacheAction.putValue(Action.NAME, "Clear Cache");
        clearCacheAction.putValue(Action.SHORT_DESCRIPTION, 
				"Empty image cache (for testing only)");
        //
        autoContrastAction.putValue(Action.NAME, "Auto Contrast");
        autoContrastAction.putValue(Action.SHORT_DESCRIPTION, 
				"Optimize contrast for current view");
        // 
        collectGarbageAction.putValue(Action.NAME, "Collect Garbage");
        //
        largeVolumeViewer.statusMessageChanged.connect(setStatusMessageSlot);
        largeVolumeViewer.setSkeleton(skeleton);
        //
        largeVolumeViewer.setWheelMode(WheelMode.Mode.SCAN);
        // Respond to orthogonal mode changes
        orthogonalModeAction.orthogonalModeChanged.connect(new Slot1<OrthogonalMode>() {
            @Override
            public void execute(OrthogonalMode mode) {
                if (mode == OrthogonalMode.ORTHOGONAL)
                    setOrthogonalMode();
                else if (mode == OrthogonalMode.Z_VIEW)
                    setZViewMode();
                // repaint(); // not necessary.
            }
        });
        setZViewMode();
        // Connect mode changes to widgets
        // First connect mode actions to one signal
        panModeAction.setMouseModeSignal.connect(mouseModeChangedSignal);
        zoomMouseModeAction.setMouseModeSignal.connect(mouseModeChangedSignal);
        traceMouseModeAction.setMouseModeSignal.connect(mouseModeChangedSignal);
        zoomScrollModeAction.setWheelModeSignal.connect(wheelModeChangedSignal);
        zScanScrollModeAction.setWheelModeSignal.connect(wheelModeChangedSignal);
        mouseModeChangedSignal.connect(onMouseModeChangedSlot);
        // Next connect that signal to various widgets
        mouseModeChangedSignal.connect(largeVolumeViewer.setMouseModeSlot);
        wheelModeChangedSignal.connect(largeVolumeViewer.setWheelModeSlot);
        // annotation-related actions:
        centerNextParentAction.centerNextParentSignal.connect(centerNextParentSlot);
        annotationPanel.centerAnnotationSignal.connect(centerNextParentSlot);
        // go to location action
        goToLocationAction.gotoLocationSignal.connect(setCameraFocusSlot);
        // TODO other orthogonal viewers
        OrthogonalPanel viewPanels[] = {neViewer, swViewer, nwViewer};
        SkeletonActor sharedSkeletonActor = getSkeletonActor();
        sharedSkeletonActor.setSkeleton(largeVolumeViewer.getSkeleton());
        for (OrthogonalPanel v : viewPanels) {
            mouseModeChangedSignal.connect(v.setMouseModeSlot);
            wheelModeChangedSignal.connect(v.setWheelModeSlot);
            v.setCamera(camera);
            // TODO - move most of this setup into OrthogonalViewer class.
            v.getViewer().statusMessageChanged.connect(setStatusMessageSlot);
            v.setSharedVolumeImage(volumeImage);
            v.setSystemMenuItemGenerator(new MenuItemGenerator() {
                @Override
                public List<JMenuItem> getMenus(MouseEvent event) {
                    List<JMenuItem> result = new Vector<>();
                    result.add(addFileMenuItem());
                    result.addAll(snapshot3dLauncher.getSnapshotMenuItems());
                    result.add(addViewMenuItem());
                    return result;
                }
            });
            v.setTileServer(tileServer);
            v.getViewer().getSliceActor().setImageColorModel(imageColorModel);
            imageColorModel.getColorModelChangedSignal().connect(v.getViewer().repaintSlot);
            imageColorModel.getColorModelChangedSignal().connect(colorModelUpdatedSlot);

            final boolean bShowTileOutlines = false; // Debugging aid
            if (bShowTileOutlines) {
            	v.getViewer().addActor(new TileOutlineActor(v.getViewTileManager())); // for debugging
            }
            // Add skeleton actor AFTER slice actor
            v.getViewer().setSkeletonActor(sharedSkeletonActor);
        }
        // Set starting interaction modes
        panModeAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        zoomScrollModeAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
	}
    
	public void clearCache() {
		tileServer.clearCache();
	}
	
	private double getMaxZoom() {
		double maxRes = Math.min(volumeImage.getXResolution(), Math.min(
				volumeImage.getYResolution(),
				volumeImage.getZResolution()));
		return 300.0 / maxRes; // 300 pixels per voxel is probably zoomed enough...
	}
	
	private double getMinZoom() {
		double result = getMaxZoom();
		BoundingBox3d box = volumeImage.getBoundingBox3d();
		Vec3 volSize = new Vec3(box.getWidth(), box.getHeight(), box.getDepth());
		for (TileConsumer viewer : allSliceViewers) {
			if (! viewer.isShowing())
				continue;
			int w = viewer.getViewport().getWidth();
			int h = viewer.getViewport().getHeight();
			if (w <= 0)
				continue;
			if (h <= 0)
				continue;
			// Fit two of the whole volume on the screen
			// Rotate volume to match viewer orientation
			Vec3 rotSize = viewer.getViewerInGround().inverse().times(volSize);
			double zx = 0.5 * w / Math.abs(rotSize.x());
			double zy = 0.5 * h / Math.abs(rotSize.y());
			double z = Math.min(zx, zy);
			result = Math.min(z, result);	
		}
		return result;
	}
	
    private void updateSWCDataConverter() {
        SWCDataConverter swcDataConverter = new SWCDataConverter();
        swcDataConverter.setSWCExchanger(
                new MatrixDrivenSWCExchanger(tileFormat)
        );
        annotationModel.setSWCDataConverter(swcDataConverter);
    }
	
	private void setOrthogonalMode() {
		nwViewer.setVisible(true);
		neViewer.setVisible(true);
		swViewer.setVisible(true);
		seViewer.setVisible(true);
		tileServer.refreshCurrentTileSetSlot.execute();
	}
	
	private SkeletonActor getSkeletonActor() {
		return largeVolumeViewer.getSkeletonActor();
	}
	
	private void setZViewMode() {
		nwViewer.setVisible(true);
		neViewer.setVisible(false);
		swViewer.setVisible(false);
		seViewer.setVisible(false);
		tileServer.refreshCurrentTileSetSlot.execute();
	}
	
	private void setupUi(JFrame parentFrame, boolean overrideFrameMenuBar) {
        setBounds(100, 100, 994, 653);
        setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// glassPane.setVisible(true);
        setupMenu(parentFrame, overrideFrameMenuBar);
        JPanel toolBarPanel = setupToolBar();
		
		// JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(1.00);
        // FW-2805: specify min size explicitly, or it'll grab a large
        //  default minimum from who knows where and blow the vertical extent way up:
        splitPane.setMinimumSize(new Dimension(0, 10));
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		toolBarPanel.add(splitPane, BorderLayout.CENTER);

        JPanel rightComponentPanel = new JPanel();
        rightComponentPanel.setLayout( new BorderLayout() );
        rightComponentPanel.add( sliderPanel, BorderLayout.CENTER );
        ColorButtonPanel colorButtonPanel = new ColorButtonPanel( imageColorModel, 1 );
        rightComponentPanel.add( colorButtonPanel, BorderLayout.EAST );
		splitPane.setRightComponent(rightComponentPanel);
        imageColorModel.getColorModelInitializedSignal().connect(new org.janelia.it.workstation.signal.Slot() {
			@Override
			public void execute() {
				splitPane.resetToPreferredSizes();
			}
		});

        sliderPanel.guiInit();
        
        JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setResizeWeight(1.00);
		splitPane.setLeftComponent(splitPane_1);

        // this contains the viewer panel plus the z scroll bar
        JPanel viewerPlusPanel = new JPanel();
        viewerPlusPanel.setLayout(new BoxLayout(viewerPlusPanel, BoxLayout.X_AXIS));
		splitPane_1.setLeftComponent(viewerPlusPanel);

		JPanel viewerPanel = new JPanel();
        viewerPlusPanel.add(viewerPanel);
		viewerPanel.setLayout(new GridBagLayout());

		// Stupid WindowBuilder won't accept reuse of GridBagConstraints object;
		// ...so the usual Java "create another class"...
		class QuadrantConstraints extends GridBagConstraints {
			public QuadrantConstraints(int x, int y) {
				this.gridx = x; this.gridy = y;
				this.gridwidth = 1; this.gridheight = 1;
				this.fill = GridBagConstraints.BOTH;
				this.weightx = 1.0;
				this.weighty = 1.0;
				this.insets = new Insets(1,1,1,1);				
			}
		};
		
		// Four quadrants for orthogonal views
		// One panel for Z slice viewer (upper left northwest)
		viewerPanel.add(nwViewer, new QuadrantConstraints(0,0));
		// other three quadrants
		viewerPanel.add(neViewer, new QuadrantConstraints(1,0));
		viewerPanel.add(swViewer, new QuadrantConstraints(0,1));
		viewerPanel.add(seViewer, new QuadrantConstraints(1,1));
		
		largeVolumeViewer.setCamera(camera);
		largeVolumeViewer.setBackground(Color.DARK_GRAY);
		zViewerPanel.setLayout(new BoxLayout(zViewerPanel, BoxLayout.Y_AXIS));
		zViewerPanel.add(largeVolumeViewer);
		volumeImage.volumeInitializedSignal.connect(updateRangesSlot);
		camera.getZoomChangedSignal().connect(changeZoom);
        camera.getFocusChangedSignal().connect(changeZ);
		
		// JPanel zScanPanel = new JPanel();
		zViewerPanel.add(zScanPanel);
		zScanPanel.setLayout(new BoxLayout(zScanPanel, BoxLayout.X_AXIS));
	
        ToolButton button_2 = new ToolButton(goBackZSlicesAction);
		button_2.setAction(goBackZSlicesAction);
		button_2.setMargin(new Insets(0, 0, 0, 0));
		button_2.setHideActionText(true);
		button_2.setAlignmentX(0.5f);
		zScanPanel.add(button_2);
		
		ToolButton button_1 = new ToolButton(previousZSliceAction);
		button_1.setAction(previousZSliceAction);
		button_1.setMargin(new Insets(0, 0, 0, 0));
		button_1.setHideActionText(true);
		button_1.setAlignmentX(0.5f);
		zScanPanel.add(button_1);
		zScanSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				setZSlice(zScanSlider.getValue());
			}
		});
		
		// JSlider zScanSlider = new JSlider();
		zScanSlider.setPreferredSize(new Dimension(32767, 29));
		zScanSlider.setMajorTickSpacing(10);
		zScanSlider.setPaintTicks(true);
		zScanPanel.add(zScanSlider);
		
		ToolButton button_3 = new ToolButton(nextZSliceAction);
		button_3.setAction(nextZSliceAction);
		button_3.setMargin(new Insets(0, 0, 0, 0));
		button_3.setHideActionText(true);
		button_3.setAlignmentX(0.5f);
		zScanPanel.add(button_3);
		
		ToolButton button_4 = new ToolButton(advanceZSlicesAction);
		button_4.setAction(advanceZSlicesAction);
		button_4.setMargin(new Insets(0, 0, 0, 0));
		button_4.setHideActionText(true);
		button_4.setAlignmentX(0.5f);
		zScanPanel.add(button_4);
		zScanSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				setZSlice((Integer)zScanSpinner.getValue());
			}
		});
		
		// JSpinner zScanSpinner = new JSpinner();
		zScanSpinner.setPreferredSize(new Dimension(75, 28));
		zScanSpinner.setMaximumSize(new Dimension(120, 28));
		zScanSpinner.setMinimumSize(new Dimension(65, 28));
		zScanPanel.add(zScanSpinner);
		
		JPanel controlsPanel = new JPanel();
		splitPane_1.setRightComponent(controlsPanel);
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		viewerPlusPanel.add(panel_1);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
		
		ToolButton zoomInButton = new ToolButton(zoomInAction);
		zoomInButton.setAlignmentX(0.5f);
		zoomInButton.setMargin(new Insets(0, 0, 0, 0));
		zoomInButton.setHideActionText(true);
		zoomInButton.setAction(zoomInAction);
		// Use a more modest auto repeat for zoom
		zoomInButton.setAutoRepeatDelay(150);
		panel_1.add(zoomInButton);
		
		// JSlider zoomSlider = new JSlider();
		zoomSlider.setOrientation(SwingConstants.VERTICAL);
		zoomSlider.setMaximum(1000);
		// Kludge to get decent vertical JSlider on Windows
		zoomSlider.setPaintTicks(true);
		zoomSlider.setMajorTickSpacing(1000);
		//
		panel_1.add(zoomSlider);
		zoomSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				int value = zoomSlider.getValue();
				double relativeZoom = value / 1000.0;
				// log scale
				double zoomMin = Math.log(getMinZoom()) / Math.log(2.0);
				double zoomMax = Math.log(getMaxZoom()) / Math.log(2.0);
				double zoom = zoomMin + relativeZoom * (zoomMax - zoomMin);
				zoom = Math.pow(2.0, zoom);
				camera.setPixelsPerSceneUnit(zoom);
			}
		});
		
		ToolButton zoomOutButton = new ToolButton(zoomOutAction);
		zoomOutButton.setAction(zoomOutAction);
		zoomOutButton.setMargin(new Insets(0, 0, 0, 0));
		zoomOutButton.setHideActionText(true);
		zoomOutButton.setAlignmentX(0.5f);
		zoomOutButton.setAutoRepeatDelay(150); // slow down auto zoom
		panel_1.add(zoomOutButton);
		
		JPanel buttonsPanel = new JPanel();
		controlsPanel.add(buttonsPanel);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		
		JButton btnNewButton_1 = new JButton("New button");
		btnNewButton_1.setAction(resetZoomAction);
		buttonsPanel.add(btnNewButton_1);
		
		JButton btnNewButton = new JButton("New button");
		btnNewButton.setAction(zoomMaxAction);
		buttonsPanel.add(btnNewButton);
		
		JButton resetViewButton = new JButton("New button");
		resetViewButton.setAction(resetViewAction);
		buttonsPanel.add(resetViewButton);

        JButton gotoLocationButton = new JButton("New button");
        gotoLocationButton.setAction(goToLocationAction);
        buttonsPanel.add(gotoLocationButton);

		Component verticalGlue = Box.createVerticalGlue();
		buttonsPanel.add(verticalGlue);
		
		buttonsPanel.add(loadStatusLabel);
		
		JButton btnClearCache = new JButton("Clear Cache");
		btnClearCache.setAction(clearCacheAction);
		buttonsPanel.add(btnClearCache);
		
		Component verticalGlue_1 = Box.createVerticalGlue();
		buttonsPanel.add(verticalGlue_1);
		
		JButton autoContrastButton = new JButton();
		autoContrastButton.setAction(autoContrastAction);
		buttonsPanel.add(autoContrastButton);
		
		JButton resetColorsButton = new JButton();
		resetColorsButton.setAction(resetColorsAction);
		buttonsPanel.add(resetColorsButton);
		
        annotationPanel = new AnnotationPanel(annotationMgr, annotationModel, largeVolumeViewerTranslator);
        controlsPanel.add(annotationPanel);


		JPanel statusBar = new JPanel();
		statusBar.setMaximumSize(new Dimension(32767, 30));
		statusBar.setMinimumSize(new Dimension(10, 30));
		add(statusBar);
		statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));
		
		statusBar.add(statusLabel);
		
        // For large volume viewer popup menu.
        largeVolumeViewer.setSystemMenuItemGenerator(new MenuItemGenerator() {
            @Override
            public List<JMenuItem> getMenus(MouseEvent event) {
                List<JMenuItem> result = new Vector<JMenuItem>();
                result.add(addFileMenuItem());
                result.add(addViewMenuItem());
                return result;
            }
        });
	}

	private void interceptModifierKeyPresses() 
	{ 
        // Intercept Shift key strokes at the highest level JComponent we can find.
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_DOWN_MASK, false),
        		"ModifierPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, KeyEvent.CTRL_DOWN_MASK, false),
        		"ModifierPressed");
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0, true),
				"ModifierReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true),
				"ModifierReleased");

        ActionMap actionMap = getActionMap();
        actionMap.put("ModifierPressed", new AbstractAction() 
        {
			private static final long serialVersionUID = 1L;
			@Override
            public void actionPerformed(ActionEvent e) {
                setModifierKeyPressed(true);
            }
        });
        actionMap.put("ModifierReleased", new AbstractAction() 
        {
			private static final long serialVersionUID = 1L;
			@Override
            public void actionPerformed(ActionEvent e) {
                setModifierKeyPressed(false);
            }
        });
	}
	
	private void interceptModeChangeGestures()
	{
        // Press "H" (hand) for Pan mode, etc.
        Action modeActions[] = {
        		panModeAction, 
        		zoomMouseModeAction, 
        		traceMouseModeAction,
        		zoomInAction,
        		zoomOutAction,
        		nextZSliceAction,
        		previousZSliceAction,
                goToLocationAction
        		};
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        for (Action action : modeActions) {
        	KeyStroke accelerator = (KeyStroke)action.getValue(Action.ACCELERATOR_KEY);
        	String actionName = (String)action.getValue(Action.NAME);
        	inputMap.put(accelerator, actionName);
        	getActionMap().put(actionName, action);
        }
	}

    private void setupAnnotationGestures() {
        // like the two "intercept" routines, but annotation-related;
        //  broken out for clarity and organization more than anything

        Action modeActions[] = {
                centerNextParentAction
        };
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        for (Action action : modeActions) {
            KeyStroke accelerator = (KeyStroke)action.getValue(Action.ACCELERATOR_KEY);
            String actionName = (String)action.getValue(Action.NAME);
            inputMap.put(accelerator, actionName);
            getActionMap().put(actionName, action);
        }

    }

	private JPanel setupToolBar() {
		add(toolBarPanel);
		toolBarPanel.setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar = new JToolBar();
		toolBarPanel.add(toolBar, BorderLayout.NORTH);
		
		JLabel lblNewLabel_1 = new JLabel("");
		lblNewLabel_1.setToolTipText("Mouse Mode:");
		lblNewLabel_1.setFocusable(false);
		lblNewLabel_1.setIcon(Icons.getIcon(IMAGES_MOUSE_LEFT));
		toolBar.add(lblNewLabel_1);

		// TODO - create a shared base class for these mode buttons
		JToggleButton traceMouseModeButton = new JToggleButton("Trace");
		mouseModeGroup.add(traceMouseModeButton);
		traceMouseModeButton.setAction(traceMouseModeAction);
		traceMouseModeButton.setMargin(new Insets(0, 0, 0, 0));
		traceMouseModeButton.setHideActionText(true);
		traceMouseModeButton.setFocusable(false);
		toolBar.add(traceMouseModeButton);
		
		JToggleButton tglBtnPanMode = new JToggleButton("");
		mouseModeGroup.add(tglBtnPanMode);
		tglBtnPanMode.setSelected(true);
		tglBtnPanMode.setAction(panModeAction);
		tglBtnPanMode.setMargin(new Insets(0, 0, 0, 0));
		tglBtnPanMode.setHideActionText(true);
		tglBtnPanMode.setFocusable(false);
		toolBar.add(tglBtnPanMode);
		
		JToggleButton tglbtnZoomMouseMode = new JToggleButton("");
		mouseModeGroup.add(tglbtnZoomMouseMode);
		tglbtnZoomMouseMode.setMargin(new Insets(0, 0, 0, 0));
		tglbtnZoomMouseMode.setFocusable(false);
		tglbtnZoomMouseMode.setHideActionText(true);
		tglbtnZoomMouseMode.setAction(zoomMouseModeAction);
		toolBar.add(tglbtnZoomMouseMode);

		toolBar.addSeparator();
		
		JLabel scrollModeLabel = new JLabel("");
		scrollModeLabel.setIcon(Icons.getIcon(IMAGES_MOUSE_SCROLL));
		scrollModeLabel.setFocusable(false);
		toolBar.add(scrollModeLabel);
		
		JToggleButton toggleButton = new JToggleButton("");
		scrollModeGroup.add(toggleButton);
		toggleButton.setSelected(true);
		toggleButton.setAction(zScanScrollModeAction);
		toggleButton.setMargin(new Insets(0, 0, 0, 0));
		toggleButton.setHideActionText(true);
		toggleButton.setFocusable(false);
		toolBar.add(toggleButton);
		
		JToggleButton toggleButton_1 = new JToggleButton("");
		scrollModeGroup.add(toggleButton_1);
		toggleButton_1.setAction(zoomScrollModeAction);
		toggleButton_1.setMargin(new Insets(0, 0, 0, 0));
		toggleButton_1.setHideActionText(true);
		toggleButton_1.setFocusable(false);
		toolBar.add(toggleButton_1);
		
		toolBar.addSeparator();
		
		// Temporarily disable orthogonal mode button for next release
		// (until feature is done)
		if (bAllowOrthoView) {
			JButton orthogonalModeButton = new JButton("");
			orthogonalModeButton.setAction(orthogonalModeAction);
			orthogonalModeButton.setMargin(new Insets(0, 0, 0, 0));
			orthogonalModeButton.setHideActionText(true);
			orthogonalModeButton.setFocusable(false);
			toolBar.add(orthogonalModeButton);
		} else {
			// orthogonalModeButton.setEnabled(false);
		}
		
		return toolBarPanel;
	}

	private void setModifierKeyPressed(boolean pressed) 
	{
		// Has the status changed since last time?
		if (pressed == modifierKeyPressed)
			return; // no change
		modifierKeyPressed = pressed; // changed!
		// Shift to select zoom scroll mode
		if (pressed)
			zoomScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
		else if (zScanScrollModeAction.isEnabled())
			zScanScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
	}
	
	private boolean setZSlice(int z) {
		Vec3 oldFocus = camera.getFocus();
		int oldValue = (int)Math.round(oldFocus.getZ() / volumeImage.getZResolution() - 0.5);
		if (oldValue == z)
			return false; // camera is already pretty close
		double halfVoxel = 0.5 * volumeImage.getZResolution();
		double newZ = z * volumeImage.getZResolution() + halfVoxel;
		double minZ = volumeImage.getBoundingBox3d().getMin().getZ() + halfVoxel;
		double maxZ = volumeImage.getBoundingBox3d().getMax().getZ() - halfVoxel;
		newZ = Math.max(newZ, minZ);
		newZ = Math.min(newZ, maxZ);
		camera.setFocus(new Vec3(oldFocus.getX(), oldFocus.getY(), newZ));
		return true;
	}

    private void setupMenu(JFrame parentFrame, boolean overrideFrameMenuBar) {
        if (overrideFrameMenuBar) {
            parentFrame.setJMenuBar(menuBar);
            addFileMenuItem();
            addEditMenuItem();
            addViewMenuItem();
            addHelpMenuItem();
        }
        else {
            toolBarPanel.add(addViewMenuItem());
            // Listener for clicking on buttons
//            JPanel tempPanel = new JPanel();
//            tempPanel.setLayout(new BoxLayout(tempPanel, BoxLayout.LINE_AXIS));
//            tempPanel.add(menuBar);
//            tempPanel.add(Box.createHorizontalGlue());
//            add(tempPanel);
        }
    }

    private JMenuItem addViewMenuItem() {
        JMenu mnView = new JMenu("View");
        menuBar.add(mnView);

        JMenu mnMouseMode = new JMenu("Mouse Mode");
        mnMouseMode.setIcon(Icons.getIcon(IMAGES_MOUSE_LEFT));
        mnView.add(mnMouseMode);

        JRadioButtonMenuItem panModeItem = new JRadioButtonMenuItem("New radio item");
        panModeItem.setSelected(true);
        panModeItem.setAction(panModeAction);
        mnMouseMode.add(panModeItem);

        JRadioButtonMenuItem zoomMouseModeItem = new JRadioButtonMenuItem("New radio item");
        zoomMouseModeItem.setAction(zoomMouseModeAction);
        mnMouseMode.add(zoomMouseModeItem);

        JRadioButtonMenuItem traceMouseModeItem = new JRadioButtonMenuItem();
        traceMouseModeItem.setAction(traceMouseModeAction);
        mnMouseMode.add(traceMouseModeItem);
        
        JMenu mnScrollMode = new JMenu("Scroll Mode");
        mnScrollMode.setIcon(Icons.getIcon(IMAGES_MOUSE_SCROLL));
        mnView.add(mnScrollMode);

        JRadioButtonMenuItem rdbtnmntmNewRadioItem = new JRadioButtonMenuItem("New radio item");
        rdbtnmntmNewRadioItem.setSelected(true);
        rdbtnmntmNewRadioItem.setAction(zScanScrollModeAction);
        mnScrollMode.add(rdbtnmntmNewRadioItem);

        JRadioButtonMenuItem mntmNewMenuItem_2 = new JRadioButtonMenuItem("New menu item");
        mntmNewMenuItem_2.setAction(zoomScrollModeAction);
        mnScrollMode.add(mntmNewMenuItem_2);

        JSeparator separator = new JSeparator();
        mnView.add(separator);

        JMenu mnZoom = new JMenu("Zoom");
        mnView.add(mnZoom);

        mnZoom.add(resetZoomAction);
        mnZoom.add(zoomOutAction);
        mnZoom.add(zoomInAction);
        mnZoom.add(zoomMaxAction);

        JSeparator separator_1 = new JSeparator();
        mnView.add(separator_1);

        JMenu mnZScan = new JMenu("Z Scan");
        mnView.add(mnZScan);

        JMenuItem mntmNewMenuItem = new JMenuItem("New menu item");
        mntmNewMenuItem.setAction(goBackZSlicesAction);
        mnZScan.add(mntmNewMenuItem);

        JMenuItem menuItem_2 = new JMenuItem("New menu item");
        menuItem_2.setAction(previousZSliceAction);
        mnZScan.add(menuItem_2);

        JMenuItem menuItem_1 = new JMenuItem("New menu item");
        menuItem_1.setAction(nextZSliceAction);
        mnZScan.add(menuItem_1);

        JMenuItem menuItem = new JMenuItem("New menu item");
        menuItem.setAction(advanceZSlicesAction);
        mnZScan.add(menuItem);

        JSeparator separator_2 = new JSeparator();
        mnView.add(separator_2);

        JMenuItem mntmNewMenuItem_3 = new JMenuItem("New menu item");
        mntmNewMenuItem_3.setAction(resetColorsAction);
        mnView.add(mntmNewMenuItem_3);
        return mnView;
    }

    public JMenuItem addFileMenuItem() {
        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmNewMenuItem_1 = new JMenuItem("New menu item");
        mntmNewMenuItem_1.setAction(openFolderAction);
        mnFile.add(mntmNewMenuItem_1);

        JMenu recentFileMenu = new JMenu("Open Recent");
        recentFileMenu.setVisible(false);
        mnFile.add(recentFileMenu);
        recentFileList = new RecentFileList(recentFileMenu);
        recentFileList.getOpenUrlRequestedSignal().connect(loadUrlSlot);

        return mnFile;
    }

    public JMenuItem addEditMenuItem() {
        JMenu mnEdit = new JMenu("Edit");
        menuBar.add(mnEdit);
        return mnEdit;
    }

    public JMenuItem addHelpMenuItem() {
        JMenu mnHelp = new JMenu("Help");
        menuBar.add(mnHelp);
        return mnHelp;
    }

    public JPopupMenu getButtonPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(addFileMenuItem());
        popupMenu.add(addViewMenuItem());
        return popupMenu;
    }

    /**
     * given a string containing the canonical Linux path to the data,
     * open the data in the viewer
     *
     * @param canonicalLinuxPath
     * @return
     * @throws MalformedURLException
     */

    public boolean loadFile(String canonicalLinuxPath) throws MalformedURLException {

        // on Linux, this just works, as the input path is the Linux path;
        //  for Mac and Windows, we need to guess the mount point of the
        //  shared disk and alter the path accordingly

        File testFile = new File(canonicalLinuxPath);
        if (!testFile.exists()) {
            // must be on Mac or Windows; but first, which of the
            //  possible Linux prefixes are we looking at?
            // OK to compare as strings, because we know the
            //  input path is Linux
            String [] mbmPrefixes = {"/groups/mousebrainmicro/mousebrainmicro/",
                    "/nobackup/mousebrainmicro/"};
            Path linuxPrefix = null;
            for (String testPrefix: mbmPrefixes) {
                if (canonicalLinuxPath.startsWith(testPrefix)) {
                    linuxPrefix = new File(testPrefix).toPath();
                }
            }
            Path partialPath = testFile.toPath().subpath(linuxPrefix.getNameCount(), testFile.toPath().getNameCount());
            // System.out.println("linuxPrefix = " + linuxPrefix);
            // System.out.println("partialPath = " + partialPath);

            // now we just need to assemble pieces: a root plus a mount name
            //  plus the partial path we just extracted; the root is OS dependent:
            String osName = System.getProperty("os.name").toLowerCase();
            List<Path> prefixesToTry = new Vector<>();
            if (osName.contains("win")) {
                for (File fileRoot : File.listRoots()) {
                    prefixesToTry.add(fileRoot.toPath());
                }
            } else if (osName.contains("os x")) {
                // for Mac, it's a lot simpler:
                prefixesToTry.add(new File("/Volumes").toPath());
            }
            // System.out.println("prefixes to try: " + prefixesToTry);

            // the last, middle piece can be nothing or one of these
            //  mounts names (nothing = enclosing dir is mounted directly
            String [] mountNames = {"", "mousebrainmicro",
                    "nobackup/mousebrainmicro", "mousebrainmicro/mousebrainmicro"};

            boolean found = false;
            for (Path prefix: prefixesToTry) {
                if (found) {
                    break;
                }
                for (String mount: mountNames) {
                    if (mount.length() > 0) {
                        testFile = prefix.resolve(new File(mount).toPath()).resolve(partialPath).toFile();
                    } else {
                        testFile = prefix.resolve(partialPath).toFile();
                    }
                    // System.out.println("trying " + testFile);
                    if (testFile.exists()) {
                        // System.out.println("file exists: " + testFile);
                        found = true;
                        break;
                    }
                }
            }
        } // end if Mac or Windows
        // by now, if we ain't got the path, we ain't got the path
        if (!testFile.exists()) {
            JOptionPane.showMessageDialog(this.getParent(),
                    "Error opening folder " + testFile.getName()
                    +" \nIs the file share mounted?",
                    "Folder does not exist.",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // July 1, 2013 elevate url loading from LargeVolumeViewer to QuadViewUi.
        URL url = testFile.toURI().toURL();
        snapshot3dLauncher = new Snapshot3DLauncher(
                largeVolumeViewer.getTileServer(),
                largeVolumeViewer.getSliceAxis(),
                camera,
                getSubvolumeProvider(),
                canonicalLinuxPath,
                url,
                imageColorModel
        );
        snapshot3dLauncher.setAnnotationManager(annotationMgr);
        volumeImage.setRemoteBasePath(canonicalLinuxPath);
        return loadURL(url);
    }

    /**
     * this is called only via right-click File > Open folder menu
     */
    public boolean loadRender(URL url) {
        // need to close/clear workspace and sample here:
        closeWorkspaceRequestSignal.emit();

        // then just go ahead and load the file
        boolean rtnVal = loadURL(url);
        updateSWCDataConverter();
        return rtnVal;
    }

    public boolean loadURL(URL url) {
    	// Check if url exists first...
    	try {
    		url.openStream();
        	return volumeImage.loadURL(url);
    	} catch (IOException exc) {
            JOptionPane.showMessageDialog(this.getParent(),
                    "Error opening folder " + url
                    +" \nIs the file share mounted?",
                    "Could not open folder",
                    JOptionPane.ERROR_MESSAGE);
            return false;
    	}
    }

    public String imageColorModelAsString() {
        return imageColorModel.asString();
    }

    public void imageColorModelFromString(String modelString) {
        imageColorModel.fromString(modelString);
    }

    public Slot1<URL> onVolumeLoadedSlot = new Slot1<URL>() {
		@Override
		public void execute(URL url) {
			recentFileList.add(url);
			imageColorModel.reset(volumeImage.getMaximumIntensity(), volumeImage.getNumberOfChannels());
			resetViewAction.actionPerformed(null);

            getSkeletonActor().setTileFormat(
                    tileServer.getLoadAdapter().getTileFormat());

		}
    };

    /**
     * this method returns a provider of read-only subvolume of data (maximum zoom,
     * intended for calculations)
     */
    public SubvolumeProvider getSubvolumeProvider(){
        return new SubvolumeProvider(volumeImage, tileServer);
    }

    static class LoadStatusLabel extends JLabel {
    	private LoadStatus loadStatus = null;
    	private ImageIcon busyIcon;
    	private ImageIcon checkIcon;
    	private ImageIcon emptyIcon;
    	
    	LoadStatusLabel() {
    		this.busyIcon = Icons.getIcon(IMAGES_SPINNER);
    		this.checkIcon = Icons.getIcon(IMAGES_GREEN_CHECK);
    		this.emptyIcon = Icons.getIcon(IMAGES_FOLDER_OPEN);
    		// Place text over icon
    		setHorizontalTextPosition(JLabel.CENTER);
    		setVerticalTextPosition(JLabel.CENTER);
    		//
    		setLoadStatus(TileServer.LoadStatus.UNINITIALIZED);
    	}
    	
    	void setLoadStatus(LoadStatus loadStatus) {
    		if (this.loadStatus == loadStatus)
    			return; // no change
    		this.loadStatus = loadStatus;
    		setText(Integer.toString(loadStatus.ordinal() - 1));
    		if (loadStatus.ordinal() >= TileServer.LoadStatus.BEST_TEXTURES_LOADED.ordinal())
    			setIcon(checkIcon);
    		else if (loadStatus.ordinal() >= TileServer.LoadStatus.NO_TEXTURES_LOADED.ordinal())
    			setIcon(busyIcon);
    		else
    			setIcon(emptyIcon);
    	}
    };

}
