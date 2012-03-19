
package org.janelia.it.jacs.web.gwt.common.client.util;

/**
 * @author Michael Press
 */
public class UrlBuilder {
    public static final String BASE_URL = "/jacs";
    public static final String GWT_BASE_URL = BASE_URL + "/gwt/";

    public static String getResearchHomeUrl() {
        return GWT_BASE_URL + "Home/Home.htm";
    }

    public static String getFrvUrl() {
        return GWT_BASE_URL + "FRV/Frv.htm";
    }

    public static String getAdvancedBlastUrl() {
        return GWT_BASE_URL + "AdvancedBlast/AdvancedBlast.htm";
    }

    public static String getReversePsiBlastUrl() {
        return GWT_BASE_URL + "ReversePsiBlast/ReversePsiBlast.htm";
    }

    public static String getPsiBlastUrl() {
        return GWT_BASE_URL + "PsiBlast/PsiBlast.htm";
    }

    public static String getAnalysisPipeline16SUrl() {
        return GWT_BASE_URL + "AnalysisPipeline16S/AnalysisPipeline16S.htm";
    }

    public static String getDegeneratePrimerDesignUrl() {
        return GWT_BASE_URL + "DegeneratePrimerDesign/DegeneratePrimerDesign.htm";
    }

    public static String getIntersiteComparisonToolUrl() {
        return GWT_BASE_URL + "IntersiteComparisonTool/IntersiteComparisonTool.htm";
    }

    public static String getClosurePrimerDesignUrl() {
        return GWT_BASE_URL + "ClosurePrimerDesign/ClosurePrimerDesign.htm";
    }

    public static String getBarcodeDesignerUrl() {
        return GWT_BASE_URL + "BarcodeDesigner/BarcodeDesigner.htm";
    }

    public static String getProkAnnotationUrl() {
        return GWT_BASE_URL + "ProkAnnot/ProkAnnotation.htm";
    }

    public static String getMgAnnotationUrl() {
        return GWT_BASE_URL + "MgAnnot/MgAnnotation.htm";
    }

    public static String getEukAnnotationUrl() {
        return GWT_BASE_URL + "EukAnnot/EukAnnotation.htm";
    }

    public static String getBlastWizardUrl() {
        return GWT_BASE_URL + "Blast/Blast.htm";
    }

    public static String getProjectsUrl() {
        return GWT_BASE_URL + "BrowseProjectsPage/BrowseProjectsPage.oa";
    }

    public static String getSamplesUrl() {
        return GWT_BASE_URL + "ProjectSamplesPage/ProjectSamplesPage.oa";
    }

    public static String getPubsUrl() {
        return GWT_BASE_URL + "DownloadByPubPage/DownloadByPubPage.oa";
    }

    public static String getNewFilesUrl() {
        return GWT_BASE_URL + "DownloadNewFilesPage/DownloadNewFilesPage.htm";
    }

    public static String getSearchUrl() {
        return GWT_BASE_URL + "Search/Search.oa";
    }

    public static String getStatusUrl() {
        return GWT_BASE_URL + "Status/Status.htm";
    }

    public static String getDetailUrl() {
        return GWT_BASE_URL + "DetailPage/DetailPage.htm";
    }

    public static String getBlastReportUrl() {
        return BASE_URL + "/admin/blastTaskReport.htm";
    }

    public static String getTaskReportUrl() {
        return BASE_URL + "/admin/taskReport.htm";
    }

    public static String getHealthMonitorUrl() {
        return BASE_URL + "/healthMonitor.chk";
    }

    public static String getSequenceProfileComparisonUrl() {
        return GWT_BASE_URL + "ProfileComparison/ProfileComparison.htm";
    }

    public static String getNeuronalAssayAnalysisUrl() {
        return GWT_BASE_URL + "NeuronalAssayAnalysis/NeuronalAssayAnalysis.htm";
    }

    public static String getTICUrl() {
        return GWT_BASE_URL + "TIC/TIC.htm";
    }

    public static String getZlaticLabUrl() {
        return GWT_BASE_URL + "ZlaticLab/ZlaticLab.htm";
    }

    public static String getDiskUsageReportUrl() {
        return GWT_BASE_URL + "Admin/Admin.htm";
    }

    public static String getRnaSeqPipelineUrl() {
        return GWT_BASE_URL + "RnaSeq/RnaSeq.htm";
    }

    public static String getFRPipelineUrl() {
        return GWT_BASE_URL + "FRData/FRData.htm";
    }

    public static String getInspectUrl() {
        return GWT_BASE_URL + "Inspect/Inspect.htm";
    }

    public static String getNeuronSeparatorPipeline() {
        return GWT_BASE_URL + "NeuronSeparator/NeuronSeparator.htm";
    }
}
