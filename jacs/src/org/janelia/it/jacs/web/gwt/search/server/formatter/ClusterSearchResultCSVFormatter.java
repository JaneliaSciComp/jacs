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

package org.janelia.it.jacs.web.gwt.search.server.formatter;

import org.janelia.it.jacs.model.genomics.AnnotationDescription;
import org.janelia.it.jacs.web.gwt.search.client.model.ClusterResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 7, 2007
 * Time: 5:04:54 PM
 */
public class ClusterSearchResultCSVFormatter extends SearchResultCSVFormatter<ClusterResult> {

    protected ClusterSearchResultCSVFormatter() {
        super();
    }

    public String[] getResultHeadings() {
        return new String[]{
                "Final Cluster",
                "# Core Clusters",
                "# Proteins",
                "# Non-Redundant",
                "Gene Symbols",
                "Protein Functions",
                "EC #",
                "Gene Ontology"
        };
    }

    public List<String> getResultFields(ClusterResult clusterResult) {
        ArrayList<String> fields = new ArrayList<String>();
/*
        fields.add(clusterResult.getAccession());
        StringBuffer documentResultField = new StringBuffer();
        if(documentResultField.length() == 0) {
            fields.add(null);
        } else {
            fields.add(documentResultField.toString());
        }
*/
        fields.add(clusterResult.getFinalAccession());
        fields.add(String.valueOf(clusterResult.getNumCoreClusters()));
        fields.add(String.valueOf(clusterResult.getNumProteins()));
        fields.add(String.valueOf(clusterResult.getNumNRProteins()));

        String geneAnnotation = clusterResult.getGeneSymbols();
        if (geneAnnotation == null || geneAnnotation.length() == 0) {
            fields.add(null);
        }
        else {
            fields.add(geneAnnotation);
        }

        String proteinFunctionsAnnotation = clusterResult.getProteinFunctions();
        if (proteinFunctionsAnnotation == null || proteinFunctionsAnnotation.length() == 0) {
            fields.add(null);
        }
        else {
            fields.add(proteinFunctionsAnnotation);
        }

        List annotations = clusterResult.getEcAnnotationDescription();
        StringBuffer annotationField = new StringBuffer();
        for (Object annotationObj : annotations) {
            AnnotationDescription annotation = (AnnotationDescription) annotationObj;
            if (annotationField.length() > 0) {
                annotationField.append(';');
            }
            annotationField.append(annotation.getId());
            annotationField.append(" - ");
            annotationField.append(annotation.getDescription());
        }
        if (annotationField.length() == 0) {
            fields.add(null);
        }
        else {
            fields.add(annotationField.toString());
        }

        annotations = clusterResult.getGoAnnotationDescription();
        annotationField = new StringBuffer();
        for (Object annotationObj : annotations) {
            AnnotationDescription annotation = (AnnotationDescription) annotationObj;
            if (annotationField.length() > 0) {
                annotationField.append(';');
            }
            annotationField.append(annotation.getId());
            annotationField.append(" - ");
            annotationField.append(annotation.getDescription());
        }
        if (annotationField.length() == 0) {
            fields.add(null);
        }
        else {
            fields.add(annotationField.toString());
        }

        return fields;
    }

}
