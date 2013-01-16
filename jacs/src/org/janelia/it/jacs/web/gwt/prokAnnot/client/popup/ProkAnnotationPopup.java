
package org.janelia.it.jacs.web.gwt.prokAnnot.client.popup;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationBulkTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationLoadGenomeDataTask;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkaryoticAnnotationTask;
import org.janelia.it.jacs.model.user_data.prefs.SubjectPreference;
import org.janelia.it.jacs.web.gwt.common.client.jobs.JobSubmissionListener;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.FileChooserPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.ProjectCodePanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ErrorPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.InfoPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupCenteredLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.submit.SubmitJob;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.panel.GipConfigurationPanel;
import org.janelia.it.jacs.web.gwt.prokAnnot.client.panel.SybaseInfoPanel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 2, 2009
 * Time: 12:07:05 PM
 */
public class ProkAnnotationPopup extends ModalPopupPanel {
    private String _localGenomeDirectory;
    private SybaseInfoPanel _sybaseInfoPanel = new SybaseInfoPanel();
    private List _preferredActionList;
    private static final String JCVI_BASE_DIR = "/usr/local/annotation/";
    private CheckBox _runLoadContigsCheckBox;
    private TextBox _loadContigsTextBox;
    private CheckBox _runRewriteStepCheckBox;
//    private CheckBox _runRewriteCheckerStepCheckBox;
    private CheckBox _runGipRunnerCheckBox;
//    private CheckBox _runGipCheckerCheckBox;
    private CheckBox _runGcContentLoaderCheckBox;
    private CheckBox _runOverlapAnalysisCheckBox;
//    private CheckBox _runSgcCheckerCheckBox;
    private CheckBox _sgcSgcSetup;
    private CheckBox _sgcValetPepHmmIdentify;
    //private CheckBox _runParseForNcRna;
    private CheckBox _sgcSkewUpdate;
    private CheckBox _sgcTerminatorsFinder;
    private CheckBox _sgcRewriteSequences;
    private CheckBox _sgcTransmembraneUpdate;
    private CheckBox _sgcMolecularWeightUpdate;
    private CheckBox _sgcOuterMembraneProteinUpdate;
    private CheckBox _sgcSignalPUpdate;
    private CheckBox _sgcLipoproteinUpdate;
    //    private CheckBox _sgcSgcPsortB;
    private CheckBox _sgcCogSearch;
    private CheckBox _prokHmmer3Search;
    private CheckBox _sgcBtabToMultiAlignment;
    //    private CheckBox _sgcPrositeSearch;
    private CheckBox _sgcAutoGeneCuration;
    private CheckBox _sgcLinkToNtFeatures;
    private CheckBox _sgcTaxonLoader;
    private CheckBox _sgcEvaluateGenomeProperties;
    private CheckBox _sgcAutoFrameShiftDetection;
    private CheckBox _sgcBuildContigFile;
    //    private CheckBox _sgcPressDb1Con;
    private CheckBox _sgcBuildCoordinateSetFile;
    private CheckBox _sgcBuildSequenceFile;
    //    private CheckBox _sgcPressDbSeq;
    private CheckBox _sgcBuildPeptideFile;
    //    private CheckBox _sgcSetDb;
    private CheckBox _sgcCoreHmmCheck;
    private CheckBox _consistencyChecker;

    private CheckBox _runLocusLoaderCheckBox;
    private CheckBox _runAnnEngCheckBox;
    private CheckBox _runAccessionBuilderCheckBox;
    private CheckBox _runShortOrfTrimCheckBox;
    private boolean _useBulkMode;
    private FileChooserPanel _fileChooserPanel;
    private String _annotationMode;
    VerticalPanel gipPanel = new VerticalPanel();
    private GipConfigurationPanel _gipInfoPanel;
    private LoadingLabel _statusMessage = new LoadingLabel();
    private JobSubmissionListener _listener;
    private ListBox _contigListBox = new ListBox();
    private ProjectCodePanel _projectCodePanel = new ProjectCodePanel();

    public ProkAnnotationPopup(String localGenomeDirectoryName, List<String> preferredActionList,
                               boolean useBulkMode, JobSubmissionListener listener, String annotationMode) {
        super("Prokaryotic Annotation Actions - " + annotationMode, false);
        this._localGenomeDirectory = localGenomeDirectoryName;
        this._preferredActionList = preferredActionList;
        this._useBulkMode = useBulkMode;
        this._listener = listener;
        this._annotationMode = annotationMode;
    }

    protected void populateContent() {
        this.setWidth("500px");
        ScrollPanel _scrollPanel = new ScrollPanel();
        _scrollPanel.setHeight("500px");
        VerticalPanel _runPipelinePanel = new VerticalPanel();
        _runPipelinePanel.setWidth("500px");
        _gipInfoPanel = new GipConfigurationPanel(_annotationMode);
        gipPanel.add(_gipInfoPanel);
        RoundedButton _showConfigButton = new RoundedButton("Show Config", new ClickListener() {
            public void onClick(Widget widget) {
                new PopupCenteredLauncher(new InfoPopupPanel(getGipConfigurationSettings().replaceAll("\n", "<br>"))).showPopup(null);
            }
        });
        _showConfigButton.setWidth("300px");
        if (!_useBulkMode) {
            gipPanel.add(_showConfigButton);
        }

        _runLoadContigsCheckBox = new CheckBox("Load Contigs");
        _runRewriteStepCheckBox = new CheckBox("Rewrite Sequences");
//        _runRewriteCheckerStepCheckBox = new CheckBox("Rewrite checker");
        //_runParseForNcRna              = new CheckBox("Parse For ncRna");
        _runGipRunnerCheckBox = new CheckBox("GIP");
//        _runGipCheckerCheckBox = new CheckBox("GIP Checker");
        // SGC Sub-items
        _sgcSgcSetup = new CheckBox("SGC Setup");
        _sgcValetPepHmmIdentify = new CheckBox("ValetPep HMM Identify");
        _sgcSkewUpdate = new CheckBox("Skew Update");
        _sgcTerminatorsFinder = new CheckBox("Terminators Finder");
        _sgcRewriteSequences = new CheckBox("Rewrite Sequences");
        _sgcTransmembraneUpdate = new CheckBox("Transmembrane Update");
        _sgcMolecularWeightUpdate = new CheckBox("Molecular Weight Update");
        _sgcOuterMembraneProteinUpdate = new CheckBox("Outer Membrane Protein Update");
        _sgcSignalPUpdate = new CheckBox("SignalP Update");
        _sgcLipoproteinUpdate = new CheckBox("Lipoprotein Update");
//        _sgcSgcPsortB= new CheckBox("SGC PsortB");
        _sgcCogSearch = new CheckBox("Cog Search");
        _prokHmmer3Search = new CheckBox("HMMER3 Search");
        _sgcBtabToMultiAlignment = new CheckBox("Btab-To-Multi Alignment");
//        _sgcPrositeSearch= new CheckBox("Prosite Search");
        _sgcAutoGeneCuration = new CheckBox("Auto-Gene Curation");
        _runOverlapAnalysisCheckBox = new CheckBox("ORF To NTORF Linker");
        _sgcLinkToNtFeatures = new CheckBox("Link To Nt Features");
        _sgcTaxonLoader = new CheckBox("Taxon Loader");
        _sgcEvaluateGenomeProperties = new CheckBox("Evaluate Genome Properties");
        _sgcAutoFrameShiftDetection = new CheckBox("Auto-Frame Shift Detection");
        _sgcBuildContigFile = new CheckBox("Build Contig File");
//        _sgcPressDb1Con= new CheckBox("PressDb 1Con File");
        _sgcBuildCoordinateSetFile = new CheckBox("Build Coordinate Set File");
        _sgcBuildSequenceFile = new CheckBox("Build Sequence File");
//        _sgcPressDbSeq= new CheckBox("PressDb Sequence File");
        _sgcBuildPeptideFile = new CheckBox("Build Peptide File");
//        _sgcSetDb= new CheckBox("SetDb");
        _sgcCoreHmmCheck = new CheckBox("Core HMM Check");
        VerticalPanel sgcPanel = new VerticalPanel();
        sgcPanel.add(_sgcSgcSetup);
        sgcPanel.add(_sgcValetPepHmmIdentify);
        sgcPanel.add(_sgcSkewUpdate);
        sgcPanel.add(_sgcTerminatorsFinder);
        sgcPanel.add(_sgcRewriteSequences);
        sgcPanel.add(_sgcTransmembraneUpdate);
        sgcPanel.add(_sgcMolecularWeightUpdate);
        sgcPanel.add(_sgcOuterMembraneProteinUpdate);
        sgcPanel.add(_sgcSignalPUpdate);
        sgcPanel.add(_sgcLipoproteinUpdate);
//        sgcPanel.add(_sgcSgcPsortB);
        sgcPanel.add(_sgcCogSearch);
        sgcPanel.add(_sgcBtabToMultiAlignment);
//        sgcPanel.add(_sgcPrositeSearch);
        sgcPanel.add(_sgcAutoGeneCuration);
        sgcPanel.add(_runOverlapAnalysisCheckBox);
        sgcPanel.add(_sgcLinkToNtFeatures);
        sgcPanel.add(_sgcTaxonLoader);
        sgcPanel.add(_sgcEvaluateGenomeProperties);
        sgcPanel.add(_sgcAutoFrameShiftDetection);
        sgcPanel.add(_sgcBuildContigFile);
//        sgcPanel.add(_sgcPressDb1Con);
        sgcPanel.add(_sgcBuildCoordinateSetFile);
        sgcPanel.add(_sgcBuildSequenceFile);
//        sgcPanel.add(_sgcPressDbSeq);
        sgcPanel.add(_sgcBuildPeptideFile);
//        sgcPanel.add(_sgcSetDb);
        sgcPanel.add(_sgcCoreHmmCheck);

        _runGcContentLoaderCheckBox = new CheckBox("Percent GC Content Loader");
//        _runSgcCheckerCheckBox = new CheckBox("SGC Checker");
        //_runParalogousCheckBox         = new CheckBox("Paralogous Families Runner");
        _runLocusLoaderCheckBox = new CheckBox("Locus Loader");
        _runAnnEngCheckBox = new CheckBox("Annotation Engineering Parser");
        _runAccessionBuilderCheckBox = new CheckBox("Accession Builder");
        _runShortOrfTrimCheckBox = new CheckBox("Short ORF Trim");
        _consistencyChecker = new CheckBox("Consistency Check");

        // Set the state of the check boxes
        _runLoadContigsCheckBox.setValue(actionSetHas("LOAD_CONTIGS"));
        _runRewriteStepCheckBox.setValue(actionSetHas("REWRITE_STEP"));
//        _runRewriteCheckerStepCheckBox.setValue(actionSetHas("REWRITE_CHECKER"));
        //_runParseForNcRna.setValue(actionSetHas("Parse For ncRna"));
        _runGipRunnerCheckBox.setValue(actionSetHas("GIP_RUNNER"));
//        _runGipCheckerCheckBox.setValue(actionSetHas("GIP_CHECKER"));
        _sgcSgcSetup.setValue(actionSetHas("SGC Setup"));
        _sgcValetPepHmmIdentify.setValue(actionSetHas("ValetPep HMM Identify"));
        _sgcSkewUpdate.setValue(actionSetHas("Skew Update"));
        _sgcTerminatorsFinder.setValue(actionSetHas("Terminators Finder"));
        _sgcRewriteSequences.setValue(actionSetHas("Rewrite Sequences"));
        _sgcTransmembraneUpdate.setValue(actionSetHas("Transmembrane Update"));
        _sgcMolecularWeightUpdate.setValue(actionSetHas("Molecular Weight Update"));
        _sgcOuterMembraneProteinUpdate.setValue(actionSetHas("Outer Membrane Protein Update"));
        _sgcSignalPUpdate.setValue(actionSetHas("SignalP Update"));
        _sgcLipoproteinUpdate.setValue(actionSetHas("Lipoprotein Update"));
//        _sgcSgcPsortB.setValue(actionSetHas("SGC PsortB"));
        _sgcCogSearch.setValue(actionSetHas("Cog Search"));
        _prokHmmer3Search.setValue(actionSetHas("HMM3 Search"));
        _sgcBtabToMultiAlignment.setValue(actionSetHas("Btab-To-Multi Alignment"));
//        _sgcPrositeSearch.setValue(actionSetHas("Prosite Search"));
        _sgcAutoGeneCuration.setValue(actionSetHas("Auto-Gene Curation"));
        _sgcLinkToNtFeatures.setValue(actionSetHas("Link To Nt Features"));
        _sgcTaxonLoader.setValue(actionSetHas("Taxon Loader"));
        _sgcEvaluateGenomeProperties.setValue(actionSetHas("Evaluate Genome Properties"));
        _sgcAutoFrameShiftDetection.setValue(actionSetHas("Auto-Frame Shift Detection"));
        _sgcBuildContigFile.setValue(actionSetHas("Build Contig File"));
//        _sgcPressDb1Con.setValue(actionSetHas("PressDb 1Con File"));
        _sgcBuildCoordinateSetFile.setValue(actionSetHas("Build Coordinate Set File"));
        _sgcBuildSequenceFile.setValue(actionSetHas("Build Sequence File"));
//        _sgcPressDbSeq.setValue(actionSetHas("PressDb Sequence File"));
        _sgcBuildPeptideFile.setValue(actionSetHas("Build Peptide File"));
//        _sgcSetDb.setValue(actionSetHas("SetDb"));
        _sgcCoreHmmCheck.setValue(actionSetHas("Core HMM Check"));
        _runGcContentLoaderCheckBox.setValue(actionSetHas("GC_CONTENT_LOAD"));
        _runOverlapAnalysisCheckBox.setValue(actionSetHas("OVERLAP_RUNNER"));
//        _runSgcCheckerCheckBox.setValue(actionSetHas("SGC_CHECKER"));
//        _runParalogousCheckBox.setValue(actionSetHas("PARALOGOUS_RUNNER"));
        _runLocusLoaderCheckBox.setValue(actionSetHas("LOCUS_LOADER"));
        _runAnnEngCheckBox.setValue(actionSetHas("ANNENG_PARSER"));
        _runAccessionBuilderCheckBox.setValue(actionSetHas("ACCESSION_BUILDER"));
        _runShortOrfTrimCheckBox.setValue(actionSetHas("SHORT_ORF_TRIM_RUNNER"));
        _consistencyChecker.setValue(actionSetHas("Consistency Checker"));

        HorizontalPanel selectionPanel = new HorizontalPanel();
        selectionPanel.add(new RoundedButton("Select All", new ClickListener() {
            public void onClick(Widget sender) {
                setValue(true);
            }
        }));
        selectionPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        selectionPanel.add(new RoundedButton("Unselect All", new ClickListener() {
            public void onClick(Widget sender) {
                setValue(false);
            }
        }));

        _contigListBox.addItem(ProkaryoticAnnotationTask.CONTIG_TYPE_FASTA);
        _contigListBox.addItem(ProkaryoticAnnotationTask.CONTIG_TYPE_GOPHER);
        _loadContigsTextBox = new TextBox();
        _loadContigsTextBox.setVisibleLength(60);
        _runLoadContigsCheckBox.setStyleName("nowrapText");
        HorizontalPanel contigsPanel = new HorizontalPanel();
        contigsPanel.add(_runLoadContigsCheckBox);
        contigsPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        contigsPanel.add(_contigListBox);
        contigsPanel.add(_loadContigsTextBox);

        CenteredWidgetHorizontalPanel actionPanel = new CenteredWidgetHorizontalPanel();
        actionPanel.add(new RoundedButton("Submit", new ClickListener() {
            public void onClick(Widget sender) {
                ProkaryoticAnnotationTask _currentTask;
                ArrayList<String> finalActionList = new ArrayList<String>();

                if (_useBulkMode && ProkaryoticAnnotationTask.MODE_CMR_GENOME.equals(_annotationMode)) {
                    _currentTask = new ProkaryoticAnnotationBulkTask();
                    _currentTask.setParameter(ProkaryoticAnnotationBulkTask.PARAM_genomeListFile, _fileChooserPanel.getUploadedFileName());
                    _currentTask.setJobName("Bulk NCBI Genome Annotation");
                }
                else if (_useBulkMode && ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(_annotationMode)) {
                    _currentTask = new ProkaryoticAnnotationBulkTask();
                    _currentTask.setParameter(ProkaryoticAnnotationBulkTask.PARAM_genomeListFile, _fileChooserPanel.getUploadedFileName());
                    _currentTask.setJobName("Bulk JCVI Genome Annotation");
                    if (_runLoadContigsCheckBox.getValue()) {
                        _currentTask.setParameter(ProkaryoticAnnotationTask.LOAD_CONTIGS,ProkaryoticAnnotationTask.CONTIG_TYPE_FASTA);
                        finalActionList.add(ProkaryoticAnnotationTask.LOAD_CONTIGS);
                    }
                }
                else {
                    _currentTask = new ProkaryoticAnnotationTask();
                    _currentTask.setJobName(_localGenomeDirectory);
                }

                if (!_useBulkMode && ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(_annotationMode)) {
                    if (_runLoadContigsCheckBox.getValue() && ((null != _loadContigsTextBox.getText() &&
                            !"".equals(_loadContigsTextBox.getText())))) {
                        finalActionList.add(ProkaryoticAnnotationTask.LOAD_CONTIGS);
                        _currentTask.setParameter(ProkaryoticAnnotationTask.LOAD_CONTIGS,
                                _contigListBox.getItemText(_contigListBox.getSelectedIndex()));
                        _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_contigFilePath, _loadContigsTextBox.getText());
                    }
                    else if (_runLoadContigsCheckBox.getValue() &&
                            (null == _loadContigsTextBox.getText() || "".equals(_loadContigsTextBox.getText()))) {
                        new PopupCenteredLauncher(new ErrorPopupPanel("Load Contigs is checked but no file path has been provided.")).showPopup(null);
                        return;
                    }
                }
                if (_runRewriteStepCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.REWRITE_STEP);
                }
//                if (_runRewriteCheckerStepCheckBox.getValue()) {
//                    finalActionList.add(ProkaryoticAnnotationTask.REWRITE_CHECKER);
//                }
                //if (_runParseForNcRna.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.ParseForNcRna);}
                if (_prokHmmer3Search.getValue()){
                    finalActionList.add(ProkaryoticAnnotationTask.Hmmer3Search);
                }
                if (_runGipRunnerCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.GIP_RUNNER);
                }
//                if (_runGipCheckerCheckBox.getValue()) {
//                    finalActionList.add(ProkaryoticAnnotationTask.GIP_CHECKER);
//                }

                if (_sgcSgcSetup.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.SgcSetup);
                }
                if (_sgcValetPepHmmIdentify.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.ValetPepHmmIdentify);
                }
                if (_sgcSkewUpdate.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.SkewUpdate);
                }
                if (_sgcTerminatorsFinder.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.TerminatorsFinder);
                }
                if (_sgcRewriteSequences.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.RewriteSequences);
                }
                if (_sgcTransmembraneUpdate.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.TransmembraneUpdate);
                }
                if (_sgcMolecularWeightUpdate.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.MolecularWeightUpdate);
                }
                if (_sgcOuterMembraneProteinUpdate.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.OuterMembraneProteinUpdate);
                }
                if (_sgcSignalPUpdate.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.SignalPUpdate);
                }
                if (_sgcLipoproteinUpdate.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.LipoproteinUpdate);
                }
//                if (_sgcSgcPsortB.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.SgcPsortB);}
                if (_sgcCogSearch.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.CogSearch);
                }
                if (_sgcBtabToMultiAlignment.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.BtabToMultiAlignment);
                }
//                if (_sgcPrositeSearch.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.PrositeSearch);}
                if (_sgcAutoGeneCuration.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.AutoGeneCuration);
                }
                if (_sgcLinkToNtFeatures.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.LinkToNtFeatures);
                }
                if (_sgcTaxonLoader.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.TaxonLoader);
                }
                if (_sgcEvaluateGenomeProperties.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.EvaluateGenomeProperties);
                }
                if (_sgcAutoFrameShiftDetection.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.AutoFrameShiftDetection);
                }
                if (_sgcBuildContigFile.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.BuildContigFile);
                }
//                if (_sgcPressDb1Con.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.PressDb1Con);}
                if (_sgcBuildCoordinateSetFile.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.BuildCoordinateSetFile);
                }
                if (_sgcBuildSequenceFile.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.BuildSequenceFile);
                }
//                if (_sgcPressDbSeq.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.PressDbSeq);}
                if (_sgcBuildPeptideFile.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.BuildPeptideFile);
                }
//                if (_sgcSetDb.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.SetDb);}
                if (_sgcCoreHmmCheck.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.CoreHMMCheck);
                }

                if (_runGcContentLoaderCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.GC_CONTENT_LOAD);
                }
                if (_runOverlapAnalysisCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.OVERLAP_RUNNER);
                }
//                if (_runSgcCheckerCheckBox.getValue()) {
//                    finalActionList.add(ProkaryoticAnnotationTask.SGC_CHECKER);
//                }
//                if (_runParalogousCheckBox.getValue()) {finalActionList.add(ProkaryoticAnnotationTask.PARALOGOUS_RUNNER);}
                if (_runLocusLoaderCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.LOCUS_LOADER);
                }
                if (_runAnnEngCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.ANNENG_PARSER);
                }
                if (_runAccessionBuilderCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.ACCESSION_BUILDER);
                }
                if (_runShortOrfTrimCheckBox.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.SHORT_ORF_TRIM_RUNNER);
                }
                if (_consistencyChecker.getValue()) {
                    finalActionList.add(ProkaryoticAnnotationTask.CONSISTENCY_CHECKER);
                }
                if (0 >= finalActionList.size()) {
                    new PopupCenteredLauncher(new ErrorPopupPanel("No annotation actions are checked.")).showPopup(null);
                    return;
                }
                _currentTask.setActionList(finalActionList);
                _currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_targetDirectory, JCVI_BASE_DIR + _localGenomeDirectory);
                submitJob(_currentTask);
            }
        }));
        actionPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        actionPanel.add(new RoundedButton("Close", new ClickListener() {
            public void onClick(Widget sender) {
                hide();
            }
        }));

        HorizontalPanel tmpDirPanel = new HorizontalPanel();
        tmpDirPanel.add(HtmlUtils.getHtml("Organism Directory:", "prompt"));
        tmpDirPanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        tmpDirPanel.add(HtmlUtils.getHtml(_localGenomeDirectory, "prompt"));

        _runPipelinePanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        if (!_useBulkMode) {
            _runPipelinePanel.add(tmpDirPanel);
        }

        VerticalPanel _bulkModePanel = new VerticalPanel();
        ArrayList<FileChooserPanel.FILE_TYPE> types = new ArrayList<FileChooserPanel.FILE_TYPE>();
        types.add(FileChooserPanel.FILE_TYPE.txt);
        _fileChooserPanel = new FileChooserPanel(new SelectionListener() {
            public void onSelect(String value) {
                // do nothing
            }

            public void onUnSelect(String value) {
                // do nothing
            }
        }, types);
        _bulkModePanel.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
        HorizontalPanel tmpBulkPanel = new HorizontalPanel();
        tmpBulkPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        tmpBulkPanel.add(HtmlUtils.getHtml("Choose genome list file", "prompt"));
        tmpBulkPanel.add(_fileChooserPanel);
        if (ProkaryoticAnnotationTask.MODE_CMR_GENOME.equals(_annotationMode)) {
            _bulkModePanel.add(HtmlUtils.getHtml("A genome list file is a file in the format of: <br>organism directory &lt;tab&gt; NCBI ftp directory location", "prompt"));
        }
        else if (ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(_annotationMode)) {
            _bulkModePanel.add(HtmlUtils.getHtml("A genome list file is a file in the format of: <br>organism directory &lt;tab&gt; contig FASTA file path", "prompt"));
            _bulkModePanel.add(new ExternalLink("More info can be found here", SystemProps.getString("ProkAnnotation.HelpURL", "")));
        }
        _bulkModePanel.add(tmpBulkPanel);

        HorizontalPanel tmpProjectPanel = new HorizontalPanel();
        tmpProjectPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        tmpProjectPanel.add(HtmlUtils.getHtml("Project Code: ", "prompt"));
        tmpProjectPanel.add(_projectCodePanel);

        _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _runPipelinePanel.add(_sybaseInfoPanel);
        _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _runPipelinePanel.add(tmpProjectPanel);
        _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        if (_useBulkMode) {
            _runPipelinePanel.add(_bulkModePanel);
            _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        }
        _runPipelinePanel.add(HtmlUtils.getHtml("Please select desired actions:", "prompt"));
        _runPipelinePanel.add(contigsPanel);
        _runPipelinePanel.add(_runRewriteStepCheckBox);
//        _runPipelinePanel.add(_runRewriteCheckerStepCheckBox);
        //_runPipelinePanel.add(_runParseForNcRna);
        _runPipelinePanel.add(_runGipRunnerCheckBox);
        _runPipelinePanel.add(_prokHmmer3Search);
        _runPipelinePanel.add(gipPanel);
//        _runPipelinePanel.add(_runGipCheckerCheckBox);
        _runPipelinePanel.add(sgcPanel);
        _runPipelinePanel.add(_runGcContentLoaderCheckBox);
//        _runPipelinePanel.add(_runSgcCheckerCheckBox);
        if (ProkaryoticAnnotationTask.MODE_ANNOTATION_SERVICE.equals(_annotationMode)) {
            _runPipelinePanel.remove(contigsPanel);
            sgcPanel.remove(_runOverlapAnalysisCheckBox);
            sgcPanel.remove(_sgcLinkToNtFeatures);
            _runPipelinePanel.remove(_runRewriteStepCheckBox);
//            _runPipelinePanel.add(_runParalogousCheckBox);
            _runPipelinePanel.add(_runLocusLoaderCheckBox);
            _runPipelinePanel.add(_runAnnEngCheckBox);
            _runPipelinePanel.add(_runAccessionBuilderCheckBox);
        }
        if (ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(_annotationMode)) {
            sgcPanel.remove(_runOverlapAnalysisCheckBox);
            sgcPanel.remove(_sgcLinkToNtFeatures);
            _runPipelinePanel.remove(_runRewriteStepCheckBox);
//            _runPipelinePanel.remove(_runRewriteCheckerStepCheckBox);
            _runPipelinePanel.add(_runShortOrfTrimCheckBox);
            _runPipelinePanel.add(_runLocusLoaderCheckBox);
        }
        if (ProkaryoticAnnotationTask.MODE_CMR_GENOME.equals(_annotationMode)) {
            _runPipelinePanel.remove(contigsPanel);
        }
        _runPipelinePanel.add(_consistencyChecker);
        _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        _runPipelinePanel.add(selectionPanel);
        _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _runPipelinePanel.add(actionPanel);
        _runPipelinePanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        _runPipelinePanel.setVisible(true);
        _scrollPanel.add(_runPipelinePanel);
        add(_scrollPanel);
    }

    /**
     * There is no contains() method on list!?!!!!!  Bah!
     *
     * @param targetAction action name looking for
     * @return boolean if the action is in the list provided
     */
    private boolean actionSetHas(String targetAction) {
        if (null == _preferredActionList) Window.alert("The list in null!");
        return _preferredActionList.contains(targetAction);
    }

    private void setValue(boolean checkValue) {
        _runLoadContigsCheckBox.setValue(checkValue);
        _runRewriteStepCheckBox.setValue(checkValue);
//        _runRewriteCheckerStepCheckBox.setValue(checkValue);
        //_runParseForNcRna.setValue(checkValue);
        _runGipRunnerCheckBox.setValue(checkValue);
//        _runGipCheckerCheckBox.setValue(checkValue);
        _runGcContentLoaderCheckBox.setValue(checkValue);
        _runOverlapAnalysisCheckBox.setValue(checkValue);
//        _runSgcCheckerCheckBox.setValue(checkValue);
        _sgcSgcSetup.setValue(checkValue);
        _sgcValetPepHmmIdentify.setValue(checkValue);
        _sgcSkewUpdate.setValue(checkValue);
        _sgcTerminatorsFinder.setValue(checkValue);
        _sgcRewriteSequences.setValue(checkValue);
        _sgcTransmembraneUpdate.setValue(checkValue);
        _sgcMolecularWeightUpdate.setValue(checkValue);
        _sgcOuterMembraneProteinUpdate.setValue(checkValue);
        _sgcSignalPUpdate.setValue(checkValue);
        _sgcLipoproteinUpdate.setValue(checkValue);
//        _sgcSgcPsortB.setValue(checkValue);
        _sgcCogSearch.setValue(checkValue);
        _prokHmmer3Search.setValue(checkValue);
        _sgcBtabToMultiAlignment.setValue(checkValue);
//        _sgcPrositeSearch.setValue(checkValue);
        _sgcAutoGeneCuration.setValue(checkValue);
        _sgcLinkToNtFeatures.setValue(checkValue);
        _sgcTaxonLoader.setValue(checkValue);
        _sgcEvaluateGenomeProperties.setValue(checkValue);
        _sgcAutoFrameShiftDetection.setValue(checkValue);
        _sgcBuildContigFile.setValue(checkValue);
//        _sgcPressDb1Con.setValue(checkValue);
        _sgcBuildCoordinateSetFile.setValue(checkValue);
        _sgcBuildSequenceFile.setValue(checkValue);
//        _sgcPressDbSeq.setValue(checkValue);
        _sgcBuildPeptideFile.setValue(checkValue);
//        _sgcSetDb.setValue(checkValue);
        _sgcCoreHmmCheck.setValue(checkValue);
        //_runParalogousCheckBox.setValue(checkValue);
        _runLocusLoaderCheckBox.setValue(checkValue);
        _runAnnEngCheckBox.setValue(checkValue);
        _runAccessionBuilderCheckBox.setValue(checkValue);
        _runShortOrfTrimCheckBox.setValue(checkValue);
        _consistencyChecker.setValue(checkValue);
    }

    // todo Everything from here down can probably be put in a base class
    private void submitJob(Task currentTask) {
        if (null == _sybaseInfoPanel.getUsername() || "".equals(_sybaseInfoPanel.getUsername())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase username is required.")).showPopup(null);
            return;
        }
        else {
            currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_username, _sybaseInfoPanel.getUsername());
            Preferences.setSubjectPreference(new SubjectPreference("sbLogin", "ProkPipeline", _sybaseInfoPanel.getUsername()));
        }

        //validate project code
        if (!_projectCodePanel.isCurrentProjectCodeValid() || null==_projectCodePanel.getProjectCode() ||
            "".equals(_projectCodePanel.getProjectCode())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A valid project code is required.")).showPopup(null);
            return;
        }
        else {
            currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_project, _projectCodePanel.getProjectCode());
        }

        if ((null == _fileChooserPanel.getUploadedFileName() || "".equals(_fileChooserPanel.getUploadedFileName())) && _useBulkMode) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A genome list file is required.")).showPopup(null);
            return;
        }

        if (null == _sybaseInfoPanel.getSybasePassword() || "".equals(_sybaseInfoPanel.getSybasePassword())) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Sybase password is required.")).showPopup(null);
            return;
        }
        else {
            currentTask.setParameter(ProkaryoticAnnotationLoadGenomeDataTask.PARAM_sybasePassword, _sybaseInfoPanel.getSybasePassword());
            Preferences.setSubjectPreference(new SubjectPreference("sbPass", "ProkPipeline", _sybaseInfoPanel.getSybasePassword()));
        }

        // If gip configuration was required, mark it in the task
        if (!_gipInfoPanel.isConfigurationValid()) {
            new PopupCenteredLauncher(new ErrorPopupPanel("A Gip configuration problem is preventing processing.<br>Please check your settings with \"Show Config\"and retry.")).showPopup(null);
            return;
        }
        currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString, getGipConfigurationSettings());

        // Set the annotation mode
        currentTask.setParameter(ProkaryoticAnnotationTask.PARAM_annotationMode, _annotationMode);

        _statusMessage.showSubmittingMessage();
        new SubmitJob(currentTask, new MyJobSubmissionListener()).runJob();
        hide();
    }

    private String getGipConfigurationSettings() {
        String configSettings = "";
        configSettings += "user: " + _sybaseInfoPanel.getUsername() + "\n";
        configSettings += "password: " + _sybaseInfoPanel.getSybasePassword() + "\n";
        configSettings += "db: " + _localGenomeDirectory.toLowerCase() + "\n";
        configSettings += "grant_num: " + _projectCodePanel.getProjectCode() + "\n";
        configSettings += _gipInfoPanel.getConfigurationSettings();
        return configSettings;
    }

    private class MyJobSubmissionListener implements JobSubmissionListener {
        public void onFailure(Throwable caught) {
            _statusMessage.showFailureMessage();
        }

        public void onSuccess(String jobId) {
            _statusMessage.showSuccessMessage();
            _listener.onSuccess(jobId);
        }
    }

}
