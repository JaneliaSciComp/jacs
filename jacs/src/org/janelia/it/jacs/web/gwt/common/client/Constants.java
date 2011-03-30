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

package org.janelia.it.jacs.web.gwt.common.client;

/**
 * @author Michael Press
 */
public class Constants {
    public static final String ROOT_PANEL_NAME = "input-container"; // Root div in the DOM
    public static final String LOADING_PANEL_NAME = "loadingMsg";
    public static final String HEADER_PANEL_NAME = "gwt_header"; // header div
    public static final String FOOTER_PANEL_NAME = "footer"; // Root div in the DOM

    //
    // URLs
    //
    public static final String VICSWEB_DOMAIN = "www.jcvi.org";
    public static final String SERVLET_CONTEXT = "/vics"; // duplicates jacs.properties but can't get there (synchronously) from GWT
    public static final String IMAGES_DIRECTORY = SERVLET_CONTEXT + "/images";

    //
    // Header text
    //
    public static final String TOOLS_LABEL = "Tools";

    public static final String CLUSTERS_SECTION_LABEL = "Clusters";
    public static final String JOBS_SECTION_LABEL = "Tools";
    public static final String SETS_SECTION_LABEL = "Sets";
    public static final String VIEWERS_SECTION_LABEL = "Viewers";
    public static final String DATA_SECTION_LABEL = "Projects & Data";
    public static final String ADMIN_SECTION_LABEL = "Admin";
    public static final String PREFS_SECTION_LABEL = "Preferences";
    public static final String SEARCH_SECTION_LABEL = "Search";

    public static final String RESEARCH_HOME_LABEL = "Home";
    public static final String SEARCH_MENU_LABEL = "Search";

    public static final String CLUSTERS_ID_SEARCH_LABEL = "Search By ID";
    public static final String CLUSTERS_KEYWORD_SEARCH_LABEL = "Search By Keyword";
    public static final String CLUSTERS_SEQUENCE_SEARCH_LABEL = "Search By Sequence";

    public static final String JOBS_ADVANCED_BLAST_LABEL = "Advanced BLAST";
    public static final String JOBS_PSI_BLAST_LABEL = "PSI-BLAST";
    public static final String JOBS_REVERSE_PSI_BLAST_LABEL = "Reverse PSI-BLAST";
    public static final String JOBS_AP16S_LABEL = "16S/18S Small Sub-Unit Analysis";
    public static final String JOBS_DPD_LABEL = "Degenerate Primer Design";
    public static final String JOBS_ICT_LABEL = "Inter-Site Comparison Tool";
    public static final String JOBS_CPD_LABEL = "Primer Design For Closure";
    public static final String BARCODE_LABEL = "Barcode Designer and Deconvolution";
    public static final String FR_LABEL = "Fragment Recruitment";
    public static final String PROFILE_COMPARISON_LABEL = "Profile Comparison Tools";
    public static final String JOBS_MG_ANNOT_LABEL = "Metagenomics Annotation";
    public static final String JOBS_PROK_ANNOT_LABEL = "Prokaryotic Annotation";
    public static final String JOBS_EUK_ANNOT_LABEL = "Eukaryotic Annotation";
    public static final String RNA_SEQ_PIPELINE_LABEL = "RNA-Seq Pipeline";

    public static final String JOBS_NEW_JOB_LABEL = "BLAST Wizard";
    public static final String JOBS_NEW_CLUSTER_JOB_LABEL = "BLAST Protein Clusters";
    public static final String JOBS_WIZARD_QUERY_SEQ_LABEL = "Page 1 of 3";
    public static final String JOBS_WIZARD_SUBJECT_SEQ_LABEL = "Page 2 of 3";
    public static final String JOBS_WIZARD_PROGRAM_OPTIONS_LABEL = "Page 3 of 3";
    public static final String JOBS_WIZARD_SUBMITTED_LABEL = "Running...";

    public static final String JOBS_JOB_RESULTS_LABEL = "My Job Results";
    public static final String JOBS_BLAST_RESULTS_LABEL = "My Blast Jobs";
    public static final String JOBS_JOBS_LABEL = "My Jobs";
    public static final String JOBS_JOB_DETAILS_LABEL = "Job Details";
    public static final String JOBS_SEQUENCE_DETAILS_LABEL = "Sequence Details";
    public static final String JOBS_FRV_LABEL = "Fragment Recruitment Viewer";

    public static final String SEARCH_MAIN_LABEL = "Search Main";
    public static final String SEARCH_ALL = "Search All";
    public static final String SEARCH_ACCESSION = "Accessions";
    public static final String SEARCH_READS = "Reads";
    public static final String SEARCH_PROTEINS = "Proteins";
    public static final String SEARCH_CLUSTERS = "Clusters";
    public static final String SEARCH_PUBLICATIONS = "Publications";
    public static final String SEARCH_PROJECTS = "Projects";
    public static final String SEARCH_SAMPLES = "Samples";
    public static final String SEARCH_WEBSITE = "Website";

    public static final String ERROR_LABEL = "Error";

    public static final String SETS_NEW_SET_LABEL = "New Set";
    public static final String SETS_VIEW_SETS_LABEL = "View Sets";
    public static final String SETS_SEARCH_SETS_LABEL = "Search Sets";

    public static final String VIEWERS_FRV_LABEL = "Fragment Recruitment Viewer";
    public static final String VIEWERS_MSA_LABEL = "Multiple Sequence Alignment";

    public static final String DATA_BROWSE_PROJECTS_LABEL = "Projects";
    public static final String DATA_BROWSE_SAMPLES_LABEL = "Project Samples";
    public static final String DATA_BROWSE_PUBLICATIONS_LABEL = "Publications";
    public static final String DATA_PUBLICATIONS_AND_DATA_LABEL = "Publications and Data";
    public static final String DATA_NEW_FILES_LABEL = "New Files";

    public static final String ADMIN_TOOLS_LABEL = "Administrative Tools";
    public static final String EDIT_PROJECT_SECTION_LABEL = "Edit Project";
    public static final String EDIT_PUBLICATION_SECTION_LABEL = "Edit Publication";

    public static final String PREFS_USER_PREFS_LABEL = "My Preferences";

    public static final int MAX_BLAST_HITS = 500;

    public static final String UPLOADED_FILE_NODE_KEY = "UploadedFileNodeKey";
    public static final String OUTER_TEXT_SEPARATOR = "@@@_1_@@@";
    public static final String INNER_TEXT_SEPARATOR = "@@@_2_@@@";
    public static final String ERROR_TEXT_SEPARATOR = "@@@_ERROR_@@@";

    // File Types Known To The System
    public static final String EXTENSION_FSA = "fsa";
    public static final String EXTENSION_FNA = "fna";
    public static final String EXTENSION_FAA = "faa";
    public static final String EXTENSION_FFN = "ffn";
    public static final String EXTENSION_MPFA = "mpfa";
    public static final String EXTENSION_FA = "fa";
    public static final String EXTENSION_FASTA = "fasta";
    public static final String EXTENSION_FRG = "frg";
    public static final String EXTENSION_SEQ = "seq";
    public static final String EXTENSION_QUAL = "qual";
    public static final String EXTENSION_PEP = "pep";

    // Sequence Alignment or profile files
    public static final String EXTENSION_PROF = "prof";
    public static final String EXTENSION_ALN = "aln";

    // for text and pdf files
    public static final String EXTENSION_TXT = "txt";
    public static final String EXTENSION_PDF = "pdf";

    // this a temporary compile only constant until the google search is completely removed
    // it's been added here so that it's removal will generate all errors at compile time
    public static final boolean USE_GOOGLE_API_FOR_WEBSITE_SEARCH = false;
}
