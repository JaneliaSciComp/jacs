
package org.janelia.it.jacs.web.gwt.prokAnnot.client.panel;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationTask;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 6, 2009
 * Time: 3:41:30 PM
 */
public class GipConfigurationPanel extends VerticalPanel {
    public static final String SELECT_STRING = "-- Select --";
    public static final String SOURCE_FASTA = "Single/Multiple FASTA";
    public static final String SOURCE_DB = "DB";
    public static final String YES = "Yes";
    public static final String NO = "No";

    private String _annotationMode = "";
    private boolean runInNtGenomeMode = false;
    private ListBox _loadMoleculeListBox = new ListBox();
    private VerticalPanel _loadMoleculePanel = new VerticalPanel();
    private ListBox _seqSourceListBox = new ListBox();
    private ListBox _buildMolecule = new ListBox();
    private VerticalPanel _buildMoleculePanel = new VerticalPanel();
    private ListBox _useTrainingSetListBox = new ListBox();

    // load molecule items
    private HorizontalPanel _srcPathPanel = new HorizontalPanel();
    private TextBox _seqSourceFileTextBox = new TextBox();
    private TextBox _name = new TextBox();
    private ListBox _type = new ListBox();
    private ListBox _topology = new ListBox();
    private TextBox _replaceId = new TextBox();
    private RadioButton _defineValues = new RadioButton("loadMoleculeGroup", "Define name, type, topology");
    private RadioButton _replaceAssembly = new RadioButton("loadMoleculeGroup", "Replace Assembly");

    // gene finding item
    private TextBox _trainsetTextBox = new TextBox();

    //build molecule items
    private TextBox _instructions = new TextBox();
    private TextBox _asmblIdTextBox = new TextBox();
    private TextBox _pseudoSeparator = new TextBox(); // "NNNNNCACACACTTAATTAATTAAGTGTGTGNNNNN"
    private ListBox _suppressPMarkListBox = new ListBox();

    // Run Steps
    private ListBox _runParseForncRNA = new ListBox();
    private ListBox _runGlimmer = new ListBox();
    private ListBox _runRewriteSeqs = new ListBox();
    private ListBox _runSearches = new ListBox();
    private ListBox _runAutoAnnotate = new ListBox();

    // Purge Options
    private ListBox _purgeFlatFilesListBox = new ListBox();
    private ListBox _purgeAnnotationsListBox = new ListBox();
    private TextBox _purgeAnnotationsTextBox = new TextBox();

    public GipConfigurationPanel(String annotationMode) {
        super();
        this._annotationMode = annotationMode;
        if (!ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(_annotationMode)) {
            this.runInNtGenomeMode = true;
        }
        init();
    }

    protected void init() {
        // Init "Run" Steps
        initYesNoListBox(_runParseForncRNA, YES);
        initYesNoListBox(_runGlimmer, YES);
        initYesNoListBox(_runRewriteSeqs, YES);
        initYesNoListBox(_runSearches, YES);
        initYesNoListBox(_runAutoAnnotate, YES);
        initYesNoListBox(_purgeFlatFilesListBox, NO);
        initYesNoListBox(_purgeAnnotationsListBox, NO);

        _purgeFlatFilesListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
                if (_purgeFlatFilesListBox.getValue(_purgeFlatFilesListBox.getSelectedIndex()).equals(YES)) {
                    new PopupCenteredLauncher(new InfoPopupPanel("Purging flat files will delete all BER and HMM search results.<br>Use with care!")).showPopup(null);
                }
            }
        });
        initYesNoListBox(_loadMoleculeListBox, YES);
        _loadMoleculePanel.setVisible(true);
        _loadMoleculeListBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent changeEvent) {
                _loadMoleculePanel.setVisible(YES.equals(_loadMoleculeListBox.getValue(_loadMoleculeListBox.getSelectedIndex())));
            }
        });
        if (ProkaryoticAnnotationTask.MODE_CMR_GENOME.equals(this._annotationMode)) {
            _loadMoleculeListBox.setSelectedIndex(1);
            _loadMoleculePanel.setVisible(false);
        }
        _seqSourceListBox.addItem(SOURCE_FASTA);
        _seqSourceListBox.addItem(SOURCE_DB);
        _seqSourceListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
//                Window.alert("Sequence Source: "+_seqSourceListBox.getValue(_seqSourceListBox.getSelectedIndex()));
                String tmpSelection = _seqSourceListBox.getValue(_seqSourceListBox.getSelectedIndex());
                _srcPathPanel.setVisible(SOURCE_FASTA.equals(tmpSelection));
            }
        });
        initYesNoListBox(_buildMolecule, NO);
        _buildMolecule.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
//                Window.alert("New Molecule: "+_buildMolecule.getValue(_buildMolecule.getSelectedIndex()));
                String tmpSelection = _buildMolecule.getItemText(_buildMolecule.getSelectedIndex());
                if (YES.equals(tmpSelection)) {
                    _loadMoleculeListBox.setSelectedIndex(0);
                    _loadMoleculePanel.setVisible(true);
                    _asmblIdTextBox.setText("");
                }
                _buildMoleculePanel.setVisible(YES.equals(tmpSelection));
            }
        });
        initYesNoListBox(_useTrainingSetListBox, NO);
        HorizontalPanel trainingPanel = new HorizontalPanel();
        trainingPanel.add(_useTrainingSetListBox);
        trainingPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        trainingPanel.add(_trainsetTextBox);
        _useTrainingSetListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
//                Window.alert("Use trainset: "+_useTrainingSetListBox.getValue(_useTrainingSetListBox.getSelectedIndex()));
            }
        });
        if (ProkaryoticAnnotationTask.MODE_ANNOTATION_SERVICE.equals(this._annotationMode)) {
            // Set to YES and give the BUILD string
            _useTrainingSetListBox.setSelectedIndex(0);
            _trainsetTextBox.setText("BUILD");
        }
        if (ProkaryoticAnnotationTask.MODE_CMR_GENOME.equals(this._annotationMode)) {
            _useTrainingSetListBox.setSelectedIndex(0);
            _trainsetTextBox.setText("ANNOTATIONS");
        }

        HorizontalPanel purgeAnnotationsPanel = new HorizontalPanel();
        _purgeAnnotationsTextBox.setText("CURRENT");
        _purgeAnnotationsTextBox.setEnabled(false);
        purgeAnnotationsPanel.add(_purgeAnnotationsListBox);
        purgeAnnotationsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        purgeAnnotationsPanel.add(_purgeAnnotationsTextBox);
        _purgeAnnotationsListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
                if (YES.equals(_purgeAnnotationsListBox.getValue(_purgeAnnotationsListBox.getSelectedIndex()))) {
                    new PopupCenteredLauncher(new InfoPopupPanel("Purging annotations will delete info from the database.<br>Use with care!")).showPopup(null);
                    _purgeAnnotationsTextBox.setEnabled(true);
                }
                else {
                    _purgeAnnotationsTextBox.setEnabled(false);
                }
            }
        });

        initYesNoListBox(_suppressPMarkListBox, NO);

        _pseudoSeparator.setVisibleLength(60);
        _pseudoSeparator.setText("NNNNNCACACACTTAATTAATTAAGTGTGTGNNNNN");

        FlexTable grid = new FlexTable();
        grid.setCellSpacing(3);
        grid.getColumnFormatter().setStyleName(0, "text");

        _asmblIdTextBox.setText("ISCURRENT");
        HorizontalPanel tmpAsmblPanel = new HorizontalPanel();
        tmpAsmblPanel.add(HtmlUtils.getHtml("Asmbl_Id:", "nowrapText"));
        tmpAsmblPanel.add(HtmlUtils.getHtml("&nbsp;", "smallspacer"));
        tmpAsmblPanel.add(_asmblIdTextBox);
        int rowCount = 0;
        grid.setWidget(rowCount, 0, tmpAsmblPanel);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Build Molecule:", "nowrapText"));
        grid.setWidget(rowCount, 1, _buildMolecule);
        grid.setWidget(++rowCount, 1, getBuildMoleculePanel());

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Load Molecule:", "nowrapText"));
        grid.setWidget(rowCount, 1, _loadMoleculeListBox);

        FlexTable srcTable = new FlexTable();
        srcTable.setWidget(0, 0, HtmlUtils.getHtml("Sequence Source", "nowraptext"));
        VerticalPanel sourcePanel = new VerticalPanel();
        sourcePanel.add(_seqSourceListBox);
        _srcPathPanel.add(HtmlUtils.getHtml("FASTA Path:", "nowrapText"));
        _srcPathPanel.add(_seqSourceFileTextBox);
        sourcePanel.add(_srcPathPanel);
        srcTable.setWidget(0, 1, sourcePanel);
        srcTable.setWidget(1, 0, _defineValues);
        srcTable.setWidget(1, 1, getMoleculeRedefinedPanel());

        srcTable.setWidget(2, 0, _replaceAssembly);
        srcTable.setWidget(2, 1, getMoleculeReplacePanel());
        _defineValues.setValue(true);
        _loadMoleculePanel.add(srcTable);

        grid.setWidget(++rowCount, 0, _loadMoleculePanel);
        grid.getFlexCellFormatter().setColSpan(rowCount, 0, 2);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Purge Search Results:", "nowrapText"));
        grid.setWidget(rowCount, 1, _purgeFlatFilesListBox);

        if (runInNtGenomeMode) {
            grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Purge Assembly Annotations:", "nowrapText"));
            grid.setWidget(rowCount, 1, purgeAnnotationsPanel);
        }

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Run Parse for ncRNAs:", "nowrapText"));
        grid.setWidget(rowCount, 1, _runParseForncRNA);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Run Glimmer:", "nowrapText"));
        grid.setWidget(rowCount, 1, _runGlimmer);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Use Training Set File:", "nowrapText"));
        grid.setWidget(rowCount, 1, trainingPanel);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Run RewriteSeqs:", "nowrapText"));
        grid.setWidget(rowCount, 1, _runRewriteSeqs);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Run Searches:", "nowrapText"));
        grid.setWidget(rowCount, 1, _runSearches);

        grid.setWidget(++rowCount, 0, HtmlUtils.getHtml("Run AutoAnnotate:", "nowrapText"));
        grid.setWidget(rowCount, 1, _runAutoAnnotate);

        this.add(grid);
    }

    private void initYesNoListBox(ListBox targetListBox, String defaultValue) {
        targetListBox.addItem(YES);
        targetListBox.addItem(NO);
        // Now set the default value
        for (int i = 0; i < targetListBox.getItemCount(); i++) {
            if (defaultValue.equals(targetListBox.getItemText(i))) {
                targetListBox.setSelectedIndex(i);
                break;
            }
        }
    }

    public Widget getBuildMoleculePanel() {
        Grid buildGrid = new Grid(3, 2);
        buildGrid.setWidget(0, 0, HtmlUtils.getHtml("Instructions:", "nowrapText"));
        buildGrid.setWidget(0, 1, _instructions);

        buildGrid.setWidget(1, 0, HtmlUtils.getHtml("Pseudo-Separator:", "nowrapText"));
        buildGrid.setWidget(1, 1, _pseudoSeparator);

        buildGrid.setWidget(2, 0, HtmlUtils.getHtml("Suppress PMark:", "nowrapText"));
        buildGrid.setWidget(2, 1, _suppressPMarkListBox);

        buildGrid.setCellSpacing(3);
        _buildMoleculePanel.add(buildGrid);
        _buildMoleculePanel.setVisible(false);
        return _buildMoleculePanel;
    }

    public Panel getMoleculeRedefinedPanel() {
        _topology.addItem("circular");
        _topology.addItem("linear");

        _type.addItem("chromosome");
        _type.addItem("pseudochromosome");
        _type.addItem("plasmid");
        _type.addItem("pseudomolecule");

        VerticalPanel _sourceSubNewPanel = new VerticalPanel();
        Grid sourceNewGrid = new Grid(3, 2);
        sourceNewGrid.setCellSpacing(3);
        sourceNewGrid.setWidget(0, 0, HtmlUtils.getHtml("Molecule Name:", "nowraptext"));
        sourceNewGrid.setWidget(0, 1, _name);
        sourceNewGrid.setWidget(1, 0, HtmlUtils.getHtml("Molecule Type:", "nowraptext"));
        sourceNewGrid.setWidget(1, 1, _type);
        sourceNewGrid.setWidget(2, 0, HtmlUtils.getHtml("Topology:", "nowraptext"));
        sourceNewGrid.setWidget(2, 1, _topology);
        _sourceSubNewPanel.add(sourceNewGrid);
        return _sourceSubNewPanel;
    }

    public Panel getMoleculeReplacePanel() {
        VerticalPanel _sourceSubOldPanel = new VerticalPanel();
        Grid sourceOldGrid = new Grid(1, 2);
        sourceOldGrid.setCellSpacing(3);
        sourceOldGrid.setWidget(0, 0, HtmlUtils.getHtml("Assembly Id:", "nowraptext"));
        sourceOldGrid.setWidget(0, 1, _replaceId);
        _sourceSubOldPanel.add(sourceOldGrid);
        return _sourceSubOldPanel;
    }


    public String getConfigurationSettings() {
        String configSettings = "";
        configSettings += "build_molecule: " + _buildMolecule.getValue(_buildMolecule.getSelectedIndex()) + "\n";
        if (YES.equals(_buildMolecule.getItemText(_buildMolecule.getSelectedIndex()))) {
            if (null != _instructions.getText() && !"".equals(_instructions.getText())) {
                configSettings += "instructions: " + _instructions.getText() + "\n";
            }
            if (null != _pseudoSeparator.getText() && !"".equals(_pseudoSeparator.getText())) {
                configSettings += "pseudo_separator: " + _pseudoSeparator.getText() + "\n";
            }
            configSettings += "suppress_pmark: " + _suppressPMarkListBox.getValue(_suppressPMarkListBox.getSelectedIndex()) + "\n";
        }
        if (null != _asmblIdTextBox.getText() && !"".equals(_asmblIdTextBox.getText())) {
            configSettings += "asmbl_id: " + _asmblIdTextBox.getText() + "\n";
        }
        configSettings += "load_molecule: " + _loadMoleculeListBox.getValue(_loadMoleculeListBox.getSelectedIndex()) + "\n";
        if (YES.equals(_loadMoleculeListBox.getValue(_loadMoleculeListBox.getSelectedIndex()))) {
            if (SOURCE_FASTA.equals(_seqSourceListBox.getValue(_seqSourceListBox.getSelectedIndex()))) {
                configSettings += "seq_source: " + _seqSourceFileTextBox.getText() + "\n";
            }
            else if (SOURCE_DB.equals(_seqSourceListBox.getValue(_seqSourceListBox.getSelectedIndex()))) {
                configSettings += "seq_source: " + _seqSourceListBox.getValue(_seqSourceListBox.getSelectedIndex()) + "\n";
            }
            if (_defineValues.getValue()) {
                configSettings += "name: " + _name.getText() + "\n";
                configSettings += "type: " + _type.getValue(_type.getSelectedIndex()) + "\n";
                configSettings += "topology: " + _topology.getValue(_topology.getSelectedIndex()) + "\n";
            }
            if (_replaceAssembly.getValue()) {
                configSettings += "replace_id: " + _replaceId.getText() + "\n";
            }
        }
        if (YES.equals(_purgeFlatFilesListBox.getValue(_purgeFlatFilesListBox.getSelectedIndex()))) {
            configSettings += "purge_flat_files: ALL\n";
        }
        if (runInNtGenomeMode && YES.equals(_purgeAnnotationsListBox.getValue(_purgeAnnotationsListBox.getSelectedIndex()))) {
            if (null == _purgeAnnotationsTextBox.getText() || "".equals(_purgeAnnotationsTextBox.getText())) {
                new PopupCenteredLauncher(new ErrorPopupPanel("Purge Assembly Annotations expects \"CURRENT\" or assembly ids.")).showPopup(null);
            }
            else {
                configSettings += "purge_annotation: " + _purgeAnnotationsTextBox.getText() + "\n";
            }
        }
        configSettings += "run_parse_for_ncRNAs: " + _runParseForncRNA.getValue(_runParseForncRNA.getSelectedIndex()) + "\n";
        configSettings += "run_glimmer: " + _runGlimmer.getValue(_runGlimmer.getSelectedIndex()) + "\n";
        configSettings += "write_seqs: " + _runRewriteSeqs.getValue(_runRewriteSeqs.getSelectedIndex()) + "\n";
        configSettings += "run_searches: " + _runSearches.getValue(_runSearches.getSelectedIndex()) + "\n";
        configSettings += "run_autoAnnotate: " + _runAutoAnnotate.getValue(_runAutoAnnotate.getSelectedIndex()) + "\n";
        if (YES.equals(_useTrainingSetListBox.getItemText(_useTrainingSetListBox.getSelectedIndex()))) {
            configSettings += "trainset: " + _trainsetTextBox.getText() + "\n";
        }
        if (YES.equals(_useTrainingSetListBox.getItemText(_useTrainingSetListBox.getSelectedIndex())) &&
                (null == _trainsetTextBox.getText() || "".equals(_trainsetTextBox.getText()))) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A training set file is selected but no path or value provided.")).showPopup(null);
        }

        return configSettings;
    }

    /**
     * Method to allow someone else to query the state of this configuration.  Currently, it is only checking that
     * a valid grid code has been supplied and a training set file has a path or a string value
     *
     * @return boolean as to the validity of the configuration settings
     */
    public boolean isConfigurationValid() {
        if (runInNtGenomeMode && YES.equals(_purgeAnnotationsListBox.getValue(_purgeAnnotationsListBox.getSelectedIndex())) &&
                (null == _purgeAnnotationsTextBox.getText() || "".equals(_purgeAnnotationsTextBox.getText()))) {
            return false;
        }
        if (YES.equals(_useTrainingSetListBox.getItemText(_useTrainingSetListBox.getSelectedIndex())) &&
                (null == _trainsetTextBox.getText() || "".equals(_trainsetTextBox.getText()))) {
            return false;
        }
        return true;
    }

}
