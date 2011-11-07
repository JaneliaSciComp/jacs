/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.control;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Dec 4, 2006
 * Time: 6:17:36 PM
 */
public class EntryPointController extends MultiActionController {
    public ModelAndView admin(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Admin");
    }

    public ModelAndView editProject(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "EditProject");
    }

    public ModelAndView editPublication(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "EditPublication");
    }

    public ModelAndView styleTutorial(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("common/StyleTutorial");
    }

    public ModelAndView home(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "home");
    }

    public ModelAndView header(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Header");
    }

    public ModelAndView testmethod(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Test");
    }

    public ModelAndView frdata(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "FRData");
    }

    public ModelAndView inspect(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Inspect");
    }

    public ModelAndView frv(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Frv");
    }

    public ModelAndView blast(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Blast");
    }

    public ModelAndView advancedblast(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "AdvancedBlast");
    }

    public ModelAndView psiBlast(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "PsiBlast");
    }

    public ModelAndView reversePsiBlast(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "ReversePsiBlast");
    }

    public ModelAndView ap16s(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "AnalysisPipeline16S");
    }

    public ModelAndView dpd(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "DegeneratePrimerDesign");
    }

    public ModelAndView ict(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "IntersiteComparisonTool");
    }

    public ModelAndView cpd(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "ClosurePrimerDesign");
    }

    public ModelAndView barcode(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "BarCodeDesigner");
    }

    public ModelAndView neuronSeparator(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "NeuronSeparator");
    }

    public ModelAndView profileComparison(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "ProfileComparison");
    }

    public ModelAndView neuronalAssayAnalysis(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "NeuronalAssayAnalysis");
    }

    public ModelAndView zlaticLab(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "ZlaticLab");
    }

    public ModelAndView prokAnnotation(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "ProkAnnotation");
    }

    public ModelAndView mgAnnotation(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "MgAnnotation");
    }

    public ModelAndView eukAnnotation(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "EukAnnotation");
    }

    public ModelAndView browseProjects(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "BrowseProjectsPage");
    }

    public ModelAndView downloadByPub(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "DownloadByPubPage");
    }

    public ModelAndView downloadNewFiles(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "DownloadNewFilesPage");
    }

    public ModelAndView projectSamples(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "ProjectSamplesPage");
    }

    public ModelAndView search(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Search");
    }

    public ModelAndView status(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "Status");
    }

    public ModelAndView mapTest(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "MapTest");
    }

    public ModelAndView detail(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "DetailPage");
    }

    public ModelAndView disclaimer(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("common/Disclaimer");
    }

    public ModelAndView rnaSeq(HttpServletRequest request, HttpServletResponse response) {
        return checkedEntryPoint(request, response, "RnaSeq");
    }

    private ModelAndView checkedEntryPoint(HttpServletRequest request, HttpServletResponse response, String viewName) {
        // Due to IceWeasel and a few others, we should include everyone and only exclude the ones with rendering problems
        return new ModelAndView(viewName);
    }

}
