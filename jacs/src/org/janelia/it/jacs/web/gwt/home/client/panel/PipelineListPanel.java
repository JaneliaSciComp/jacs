
package org.janelia.it.jacs.web.gwt.home.client.panel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.SystemProps;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

public class PipelineListPanel extends TitledBox {

    public PipelineListPanel() {
        super("Pipelines", true);

        setWidth("300px"); // min width when contents are hidden

        // Add 16S18S pipeline analysis link
        Link analysisPipeline16SLink = new Link("16S/18S Small Sub-Unit Analysis", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getAnalysisPipeline16SUrl(), "_self", "");
            }
        });

        // Add prok annotation link
        Link prokAnnotLink = new Link("Prokaryotic Annotaion", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getProkAnnotationUrl(), "_self", "");
            }
        });

        // Add meta genomic annotation link
        Link metaGenomicAnnotLink = new Link("Metagenomic Annotaion", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getMgAnnotationUrl(), "_self", "");
            }
        });


        // Add degenerate primer design link
//        Link degeneratePrimerDesignLink =  new Link("Degenerate Primer Design", new ClickListener() {
//            public void onClick(Widget sender) {
//                Window.open(UrlBuilder.getDegeneratePrimerDesignUrl(), "_self","");
//            }
//        });

        // Add Profile Comparison link
        Link profileCompToolLink = new Link("Sequence Profile Comparison Tool", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getSequenceProfileComparisonUrl(), "_self", "");
            }
        });

        // Add intersite link
//        Link intersiteCompToolLink=  new Link("Inter-Site Comparison Tool", new ClickListener() {
//            public void onClick(Widget sender) {
//                Window.open(UrlBuilder.getIntersiteComparisonToolUrl(), "_self","");
//            }
//        });

        // Add primer design link
//        Link primerDesignForClosureLink=  new Link("Primer Design For Closure", new ClickListener() {
//            public void onClick(Widget sender) {
//                Window.open(UrlBuilder.getClosurePrimerDesignUrl(), "_self","");
//            }
//        });

        // Add barcode design link
        Link barcodeDesignLink = new Link("Barcode Designer and Deconvolution", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getBarcodeDesignerUrl(), "_self", "");
            }
        });

        // Add FR Data link
        Link frDataLink = new Link("Fragment Recruitment", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getFRPipelineUrl(), "_self", "");
            }
        });

        // Add Inspect link
        Link inspectLink = new Link("Inspect Peptide Mapper", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getInspectUrl(), "_self", "");
            }
        });

        // Add RNA-Seq link
        Link rnaSeqPipelineLink = new Link("RNA-Seq Pipeline", new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(UrlBuilder.getRnaSeqPipelineUrl(), "_self", "");
            }
        });

        boolean showMetagenomicPipeline = SystemProps.getBoolean("MgAnnotation.Server", false);
        // Add all the links to the titlebox.
        add(analysisPipeline16SLink);
        add(barcodeDesignLink);
        add(frDataLink);
        add(inspectLink);
        if (showMetagenomicPipeline) {
            add(metaGenomicAnnotLink);
        }
        add(prokAnnotLink);
        add(rnaSeqPipelineLink);
        add(profileCompToolLink);
//        add(degeneratePrimerDesignLink);
//        add(intersiteCompToolLink);
//        add(primerDesignForClosureLink);

    }

}