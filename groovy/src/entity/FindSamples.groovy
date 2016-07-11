package entity

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.janelia.it.jacs.model.entity.Entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;

String ownerKey = "group:flylight";
f = new JacsUtils(ownerKey, false);

new File("dataset_assignments.txt").withWriter { out ->

    out.writeLine("Line\tSlide Code\tTile\nEffector\tObjective\tImager\tDimensions\tOld\tNew\tGroup")

    for(Entity sample : f.e.getUserEntitiesByTypeName(ownerKey, TYPE_SAMPLE)) {

        items = sample.getName().split("-")

        line = items[0]
        slideCode = items[1]
        tile = ""
        if (items.length>2) {
            tile = items[2]
        }

        dataSet = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)

        SolrQuery query = new SolrQuery("sage_slide_code_t:"+slideCode)
        query.rows = 1
        docs = f.s.search(ownerKey, query, false)?.response?.results

        if (docs!=null && !docs.isEmpty()) {
            doc = docs.get(0)
            effector = doc.get("sage_effector_t")
            imager = doc.get("sage_created_by_t")

            x = doc.get("sage_dimension_x_l")
            y = doc.get("sage_dimension_y_l")
            z = doc.get("sage_dimension_z_l")
            dimensions = x+"x"+y+"x"+z

            String objective = doc.get("sage_objective_t")
            if (objective.contains("63x")) {
                objective = "63x"
            }
            if (objective.contains("40x")) {
                objective = "40x"
            }
            if (objective.contains("20x")) {
                objective = "20x"
            }
            else {
                //objective = ""
            }

            newDataSet = null
            if (dataSet.equals("system_flylight_whole_brain")) {
    //            newDataSet = "nerna_whole_brain"
            }
            else if (dataSet.equals("system_flylight_optic_span")) {
    //            newDataSet = "nerna_optic_span"
            }
            else if (dataSet.equals("system_flylight_optic_central_border")) {
    //            newDataSet = "nerna_optic_central_border"
            }
            else if (dataSet.equals("system_flylight_optic_lobe_tile")) {
                if (tile.startsWith("Right")) {
                    newDataSet = "nerna_optic_lobe_right"
                }
                else {
                    newDataSet = "nerna_optic_lobe_left"
                }
            }
            else if (dataSet.equals("system_flylight_other")) {
    //            newDataSet = "nerna_other"
            }
            else if (dataSet.equals("system_flylight_central_brain")) {
                newDataSet = "asoy_mb_flp_out_63x_512px"
            }
            else if (dataSet.equals("system_flylight_central_tile")) {
                newDataSet = "wolfft_central_tile"
            }

            if (newDataSet!=null) {
                a = ""
                if (line == "GMR_57C10_AD_01") {
                    if (effector == "1xLwt_attp40_4stop1") {
                        a = "1"
                    }
                    else if (effector == "Two_recombinase_flipouts_A") {
                        a = "2"
                    }
                }
                else if (line.startsWith("GMR_") && line.endsWith("_AE_01")) {
                    if (effector == "57C10L_attp8_4stop1" || effector == "57C10PEST_attp18_4stop1") {
                        a = "3"
                    }
                }

                out.writeLine(line+"\t"+slideCode+"\t"+tile+"\t"+effector+"\t"+objective+"\t"+imager+"\t"+dimensions+"\t"+dataSet+"\t"+newDataSet+"\t"+a)
            }
        }

    }
}

println "Done"