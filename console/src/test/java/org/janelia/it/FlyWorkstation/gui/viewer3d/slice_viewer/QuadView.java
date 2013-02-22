package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

public class QuadView extends JFrame 
{
	static {
		// Use top menu bar on Mac
		if (System.getProperty("os.name").contains("Mac")) {
			  System.setProperty("apple.laf.useScreenMenuBar", "true");
			  System.setProperty("com.apple.mrj.application.apple.menu.about.name", "QuadView");
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.out.println("Warning: Failed to set native look and feel.");
		}
	}
	
	private static final long serialVersionUID = 1L;
	protected SliceViewer sliceViewer = new SliceViewer();
	protected Action resetViewAction = new ResetViewAction(sliceViewer);
	protected Action resetZoomAction = new ResetZoomAction(sliceViewer);
	protected Action panModeAction = new PanModeAction(sliceViewer);
	protected Action openFolderAction = new OpenFolderAction(sliceViewer, sliceViewer);


	// Zoom 
	protected JSlider zoomSlider = new JSlider(JSlider.VERTICAL);
	protected Action zoomInAction = new ZoomInAction(sliceViewer.getCamera());
	protected Action zoomMouseModeAction = new ZoomMouseModeAction(sliceViewer);
	protected Action zoomOutAction = new ZoomOutAction(sliceViewer.getCamera());
	protected Action zoomScrollModeAction = new ZoomScrollModeAction(sliceViewer);

	// Z scan
    protected JPanel zPanel = new JPanel();
	protected JSlider zScanSlider = new JSlider(JSlider.HORIZONTAL);
    protected JSpinner zScanSpinner = new JSpinner();
	protected Action nextZSliceAction = new NextZSliceAction(sliceViewer, sliceViewer);
	protected Action previousZSliceAction = new PreviousZSliceAction(sliceViewer, sliceViewer);
	protected Action advanceZSlicesAction = new AdvanceZSlicesAction(sliceViewer, sliceViewer, 10);
	protected Action goBackZSlicesAction = new GoBackZSlicesAction(sliceViewer, sliceViewer, -10);
	
	protected QtSlot1<Double> changeZoom = new QtSlot1<Double>(this) {
		@Override
		public void execute(Double zoom) {
			double zoomMin = Math.log(sliceViewer.getMinZoom()) / Math.log(2.0);
			double zoomMax = Math.log(sliceViewer.getMaxZoom()) / Math.log(2.0);
			double zoomLog = Math.log(zoom) / Math.log(2.0);
			double relativeZoom = (zoomLog - zoomMin) / (zoomMax - zoomMin);
			int sliderValue = (int)Math.round(relativeZoom * 1000.0);
			zoomSlider.setValue(sliderValue);
		}
	};
	
	protected QtSlot1<Vec3> changeZ = new QtSlot1<Vec3>(this) {
		@Override
		public void execute(Vec3 focus) {
			int z = (int)Math.round(focus.getZ() / sliceViewer.getZResolution());
			zScanSlider.setValue(z);
			zScanSpinner.setValue(z);
		}
	};
	
	protected QtSlot updateRangesSlot = new QtSlot(this) {
		@Override
		public void execute() 
		{
			// Z range
			double zMin = sliceViewer.getBoundingBox3d().getMin().getZ();
			double zMax = sliceViewer.getBoundingBox3d().getMax().getZ();
			int z0 = (int)Math.round(zMin / sliceViewer.getZResolution());
			int z1 = (int)Math.round(zMax / sliceViewer.getZResolution());
			assert z1 >= z0;
			zPanel.setVisible((z1 - z0) > 1);
			int z = (int)Math.round(sliceViewer.getFocus().getZ() / sliceViewer.getZResolution());
			if (z < z0)
				z = z0;
			if (z > z1)
				z = z1;
			zScanSlider.setMinimum(z0);
			zScanSlider.setMaximum(z1);
			zScanSlider.setValue(z);
			zScanSpinner.setModel(new SpinnerNumberModel(z, z0, z1, 1));
		}
	};

	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            		new QuadView();
            }
        });
	}

	public QuadView() {
		setTitle("QuadView");
        setupUi();
	}
	
	protected void setupUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		createMenus();
		// Top level container - status bar vs rest
		Container container = getContentPane();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.add(createStatusBar(container));
		// Next level - tool bar vs rest - tool bar requires BorderLayout parent
		Container parent = container;
		container = new JPanel();
		container.setLayout(new BorderLayout(0,0));
		container.add(createToolBar(), BorderLayout.NORTH);
		parent.add(container, 0); // put main area north of status bar
		// Next level - splitter dividing viewer from controls
		parent = container;
		JPanel viewerPanel = new JPanel();
		JPanel controlPanel = new JPanel();
		controlPanel.setMinimumSize(new Dimension(0, 0)); // So split pane can hide controls
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				viewerPanel, controlPanel);
		parent.add(splitPane, BorderLayout.CENTER);
		splitPane.setContinuousLayout(true); // too optimistic?
		splitPane.setResizeWeight(1.0); // Controls' size stays fixed
        // Slice widget
		viewerPanel.setLayout(new BoxLayout(viewerPanel, BoxLayout.Y_AXIS));
        viewerPanel.add(sliceViewer);
        sliceViewer.setPreferredSize( new Dimension( 800, 700 ) );
        // Z slider below slice viewer
        viewerPanel.add(zPanel);
        zPanel.setLayout(new BoxLayout(zPanel, BoxLayout.X_AXIS));
        zPanel.add(new ToolButton(goBackZSlicesAction));
        zPanel.add(new ToolButton(previousZSliceAction));
        zScanSlider.setMajorTickSpacing(10);
        zScanSlider.setMinimum(0);
        zScanSlider.setMaximum(100);
        zScanSlider.setPaintTicks(true);
        // zScanSlider.setPaintTrack(false); // just seeing...
        zPanel.add(zScanSlider);
        zScanSlider.addChangeListener(new ZScanSliderListener(zScanSlider, sliceViewer));
        sliceViewer.getCamera().getFocusChangedSignal().connect(changeZ);
        zPanel.add(new ToolButton(nextZSliceAction));
        zPanel.add(new ToolButton(advanceZSlicesAction));
        zScanSpinner.setPreferredSize(new Dimension(65, zScanSpinner.getPreferredSize().height));
        zScanSpinner.setMaximumSize(zScanSpinner.getPreferredSize());
        zPanel.add(zScanSpinner);
        sliceViewer.getDataChangedSignal().connect(updateRangesSlot);
        // Controls
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		Container upperControls = new JPanel();
		controlPanel.add(upperControls);
		upperControls.setLayout(new BoxLayout(upperControls, BoxLayout.X_AXIS));
		// sliders
		Container slidersPanel = new JPanel();
		upperControls.add(slidersPanel);
		// upperControls.add(Box.createHorizontalGlue()); // allow space between sliders and buttons
		slidersPanel.setLayout(new BoxLayout(slidersPanel, BoxLayout.X_AXIS));
		// zoom slider
		JPanel zoomPanel = new JPanel();
		slidersPanel.add(zoomPanel);
		slidersPanel.add(Box.createHorizontalGlue());
		zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.Y_AXIS));
		// put a border to suggest that the zoom buttons belong with the slider
		zoomPanel.setBorder(BorderFactory.createEtchedBorder());
		zoomPanel.add(new ToolButton(zoomInAction));
		zoomSlider.setMinimum(0);
		zoomSlider.setMaximum(1000);
		zoomSlider.addChangeListener(new ZoomSliderListener(zoomSlider, sliceViewer));
		// connect signal from camera back to slider
		sliceViewer.getZoomChangedSignal().connect(changeZoom);
		zoomPanel.add(zoomSlider);
		zoomPanel.add(new ToolButton(zoomOutAction));
		// buttons
		Container buttonsPanel = new JPanel();
		upperControls.add(buttonsPanel);
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
		buttonsPanel.add(new JButton(resetZoomAction));
		buttonsPanel.add(new JButton("Zoom Max"));
		buttonsPanel.add(new JButton(resetViewAction));
		buttonsPanel.add(Box.createVerticalGlue());
		// colors
		JPanel colorsPanel = new JPanel();
		controlPanel.add(colorsPanel);
		colorsPanel.setBorder(new TitledBorder("Color Channels"));
		colorsPanel.setLayout(new BoxLayout(colorsPanel, BoxLayout.Y_AXIS));
		colorsPanel.add(new ColorChannelWidget(0));
		colorsPanel.add(new ColorChannelWidget(1));
		colorsPanel.add(new ColorChannelWidget(2));
		colorsPanel.add(new JButton("Reset Colors"));
        //Display the window.
        pack();
        setSize( getContentPane().getPreferredSize() );
        setLocation(100, 100); // TODO persist latest geometry
        setVisible(true);
	}
	
	protected void createMenus() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu, submenu;
		JMenuItem item;

		menu = new JMenu("File");
		// item = new JMenuItem("Open Folder...");
		menu.add(openFolderAction);
		submenu = new JMenu("Open Recent");
		submenu.setEnabled(false); // until we find some recent items...
		menu.add(submenu);
		menuBar.add(menu);

		menu = new JMenu("Edit");
		menu.add(new UndoAction());
		menu.add(new RedoAction());
		menuBar.add(menu);

		menu = new JMenu("View");
		submenu = new JMenu("Mouse Mode");
		submenu.setIcon(Icons.getIcon("mouse_left.png"));
		// only one mouse mode is active at a time
		ButtonGroup group = new ButtonGroup();
		item = new JRadioButtonMenuItem(panModeAction);
		group.add(item);
		submenu.add(item);
		item.setSelected(true);
		item = new JRadioButtonMenuItem(zoomMouseModeAction);
		group.add(item);
		submenu.add(item);
		menu.add(submenu);
		submenu = new JMenu("Scroll Mode");
		submenu.setIcon(Icons.getIcon("mouse_scroll.png"));		
		group = new ButtonGroup();
		item = new JRadioButtonMenuItem(zoomScrollModeAction);
		group.add(item);
		submenu.add(item);
		item.setSelected(true);
		menu.add(submenu);
		menu.addSeparator();
		menu.add(resetViewAction);
		menu.add(resetZoomAction);
		menu.add(zoomOutAction);
		menu.add(zoomInAction);
		menuBar.add(menu);

		menu = new JMenu("Help");
		menuBar.add(menu);

		setJMenuBar(menuBar);
	}
	
	protected JComponent createStatusBar(Container parent) {
		// http://stackoverflow.com/questions/3035880/how-can-i-create-a-bar-in-the-bottom-of-a-java-app-like-a-status-bar
		JPanel statusPanel = new JPanel();
		statusPanel.setPreferredSize(new Dimension(parent.getWidth(), 20));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		JLabel statusLabel = new JLabel("");
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLabel);
		return statusPanel;
	}

	protected JComponent createToolBar() {
		JToolBar toolBar = new JToolBar();

		JLabel mouseModeLabel = new ToolBarIcon("mouse_left.png");
		mouseModeLabel.setToolTipText("Mouse mode:");
		toolBar.add(mouseModeLabel);
		ButtonGroup group = new ButtonGroup();
		ToolModeButton button = new ToolModeButton(panModeAction);
		group.add(button);
		toolBar.add(button);
		button.setSelected(true);
		button = new ToolModeButton(zoomMouseModeAction);
		group.add(button);
		toolBar.add(button);
		
		toolBar.addSeparator();

		mouseModeLabel = new ToolBarIcon("mouse_scroll.png");
		mouseModeLabel.setToolTipText("Scroll wheel mode:");
		toolBar.add(mouseModeLabel);
		group = new ButtonGroup();
		button = new ToolModeButton(zoomScrollModeAction);
		group.add(button);
		toolBar.add(button);
		button.setSelected(true);

		toolBar.addSeparator();
		
		return toolBar;
	}


	static class ZScanSliderListener implements ChangeListener
	{
		JSlider slider;
		VolumeViewer viewer;
		int previousValue = -1;
		
		ZScanSliderListener(JSlider slider, VolumeViewer viewer) 
		{
			this.viewer = viewer;
			this.slider = slider;
		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			int value = this.slider.getValue();
			if (value == previousValue)
				return;
			previousValue = value;
			Vec3 oldFocus = viewer.getFocus();
			int oldValue = (int)Math.round(oldFocus.getZ() / viewer.getZResolution());
			if (oldValue == value)
				return; // camera is already pretty close
			double newZ = value * viewer.getZResolution();
			viewer.setFocus(new Vec3(oldFocus.getX(), oldFocus.getY(), newZ));
		}
	}
	
	
	// Allow camera to respond to dragging Zoom Slider
	static class ZoomSliderListener implements ChangeListener
	{
		JSlider slider;
		VolumeViewer viewer;
		int previousValue = -1;
		
		ZoomSliderListener(JSlider slider, VolumeViewer viewer) {
			this.slider = slider;
			this.viewer = viewer;
		}

		@Override
		public void stateChanged(ChangeEvent e) 
		{
			int value = this.slider.getValue();
			if (value == previousValue)
				return;
			previousValue = value;
			double relativeZoom = value / 1000.0;
			// log scale
			double zoomMin = Math.log(viewer.getMinZoom()) / Math.log(2.0);
			double zoomMax = Math.log(viewer.getMaxZoom()) / Math.log(2.0);
			double zoom = zoomMin + relativeZoom * (zoomMax - zoomMin);
			zoom = Math.pow(2.0, zoom);
			viewer.setPixelsPerSceneUnit(zoom);
		}
	}
	
}
