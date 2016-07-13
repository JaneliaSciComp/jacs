package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils

import com.google.common.base.Charsets
import com.google.common.io.Files

class GenerateOpticLobeExportScript2 {
    
    private static final filename = "olist.txt";
    private static final username = "user:nerna"
    private static final targetDirVarName = "OUTDIR"
    private static final targetDirVar = "\$"+targetDirVarName
    private static final targetDir = "/mnt/export/OpticLobes"
    private static final boolean exportUnaligned = true;
    private static final boolean exportAligned = false;
    private static final boolean exportConsolidatedLabel = true;
    
    private final JacsUtils f;
    
    public GenerateOpticLobeExportScript2() {
        f = new JacsUtils(username, false)
    }
    
    public void run() {
        
        println "#!/bin/sh"
        println "# Export script for samples specified in "+filename
        println targetDirVarName+"="+targetDir
        println "mkdir "+targetDirVar
        
        List<String> lines = Files.readLines(new File(filename), Charsets.UTF_8);
        lines.each {
            println ""
            String sampleName = it;
            Set<Entity> matchingSamples = f.e.getEntitiesByNameAndTypeName(username, sampleName, TYPE_SAMPLE)
            if (matchingSamples.isEmpty()) {
                println "# Missing "+sampleName
            }
            else if (matchingSamples.size()>1) {
                println "# More than one "+sampleName
            }
            for (Entity sample : matchingSamples) {
                processSample(sample);
                // Free memory
                sample.setEntityData(null)
            }
        }
    }
    
    private void processSample(Entity sample) {
        
        f.loadChildren(sample)
        
        String status = sample.getValueByAttributeName(ATTRIBUTE_STATUS)
        if (status.equals(VALUE_BLOCKED)) {
            println "# Sample was blocked: "+sample.name
            return
        }
        
        Entity run = EntityUtils.getLatestChildOfType(sample, TYPE_PIPELINE_RUN)
        if (run==null) {
            println "# Missing secondary data: "+sample.name
            return
        }
        f.loadChildren(run)
        
        println "# Exporting "+sample.name
        
        String dir = targetDirVar+"/"+sample.name+"/"
        println "mkdir "+dir
        
        if (exportUnaligned) {
            Entity sp = EntityUtils.getLatestChildOfType(run, TYPE_SAMPLE_PROCESSING_RESULT)
            f.loadChildren(sp)
            
            Entity unalignedImage = sp.getChildByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE);
            if (unalignedImage!=null) {
                String unalignedFilepath = unalignedImage.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                println "cp "+unalignedFilepath+" "+dir
            }
            if (exportConsolidatedLabel) {
                Entity ns = EntityUtils.getLatestChildOfType(sp, TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
                exportNeuronSeparation(ns, dir, "Unaligned")
            }
        }
        
        if (exportAligned) {
            Entity ar = EntityUtils.getLatestChildOfType(run, TYPE_ALIGNMENT_RESULT)
            if (ar!=null) {
                f.loadChildren(ar)
                Entity alignedImage = ar.getChildByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE);
                if (alignedImage!=null) {
                    String alignedFilepath = alignedImage.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
                    println "cp "+alignedFilepath+" "+dir
                }
                if (exportConsolidatedLabel) {
                    Entity ns = EntityUtils.getLatestChildOfType(ar, TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
                    exportNeuronSeparation(ns, dir, "Aligned")
                }
            }   
        }
    }
    
    private void exportNeuronSeparation(Entity ns, String targetDir, String prefix) {
        f.loadChildren(ns)
        Entity nssd = EntityUtils.getSupportingData(ns)
        f.loadChildren(nssd)
        Entity labelFile = EntityUtils.findChildWithName(nssd, "ConsolidatedLabel.v3dpbd")
        if (labelFile!=null) {
            String labelFilepath = labelFile.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
            println "cp "+labelFilepath+" "+targetDir+""+prefix+"ConsolidatedLabel.v3dpbd"
        }
    }
}

GenerateOpticLobeExportScript2 script = new GenerateOpticLobeExportScript2();
script.run();
System.exit(0);
