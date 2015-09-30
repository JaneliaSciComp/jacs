
package org.janelia.it.jacs.web.gwt.search.server.formatter;

import org.janelia.it.jacs.model.genomics.AnnotationDescription;
import org.janelia.it.jacs.web.gwt.search.client.model.ProteinResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 7, 2007
 * Time: 5:04:54 PM
 */
public class ProteinSearchResultCSVFormatter extends SearchResultCSVFormatter<ProteinResult> {

    protected ProteinSearchResultCSVFormatter() {
        super();
    }

    public String[] getResultHeadings() {
        return new String[]{
                "Protein Accession",
                "External Accession",
                "NCBI GI",
                "Taxonomy",
                "Final Cluster",
                "Core Cluster",
                "Protein Function",
                "Gene Symbol",
                "Gene Ontology",
                "EC #",
                "Length"
        };
    }

    public List<String> getResultFields(ProteinResult proteinResult) {
        ArrayList<String> fields = new ArrayList<String>();
        fields.add(proteinResult.getAccession());
        fields.add(proteinResult.getExternalAccession());
        fields.add(proteinResult.getNcbiGiNumber());
        fields.add(proteinResult.getTaxonomy());

        String clusterAcc = proteinResult.getFinalCluster();
        fields.add(clusterAcc);
        clusterAcc = proteinResult.getCoreCluster();
        fields.add(clusterAcc);

        StringBuffer annotationField = new StringBuffer(proteinResult.getProteinFunction());
        if (annotationField.length() == 0) {
            fields.add(null);
        }
        else {
            fields.add(annotationField.toString());
        }

        annotationField = new StringBuffer(proteinResult.getGeneNames());
        if (annotationField.length() == 0) {
            fields.add(null);
        }
        else {
            fields.add(annotationField.toString());
        }

        List annotations = proteinResult.getGoAnnotationDescription();
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

        annotations = proteinResult.getEcAnnotationDescription();
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

        fields.add(proteinResult.getSequenceLength() != null ?
                String.valueOf(proteinResult.getSequenceLength()) :
                null);
        return fields;
    }
}
