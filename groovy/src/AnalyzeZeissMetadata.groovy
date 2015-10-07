import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityLoader
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr

// Globals
//subject = "user:asoy"
f = new JacsUtils(null, false)

file = new PrintWriter("asoy_metadata.txt")

file.print "Owner\tDataSet\tCase\tSample\tObjective\tArea\tChanSpec1\tColors1\tDyes1\tChanSpec2\tColors2\tDyes2"
//file.print "IllChan1Wavelength\tIllChan1Power\tIllChan1PowerBc1\t"
//file.print "IllChan2Wavelength\tIllChan2Power\tIllChan2PowerBc1\t"
//file.print "DetChan1\tDetChan1Dye\tDetChan1Start\tDetChan1End\tDetChan1Gain\t"
//file.print "DetChan2\tDetChan2Dye\tDetChan2Start\tDetChan2End\tDetChan2Gain\t"
//file.print "Power\tGain"
file.print "\n"

//f.e.getUserEntitiesByTypeName(subject, TYPE_SAMPLE).each {
//    Entity sample = (Entity)it;
//    if ("20x".equals(sample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE))) {
//        SampleProcessor processor = new SampleProcessor()
//        if (processor.process(f, sample, file)) {
//            numSamples++;
//        }
//        sample.setEntityData(null);
//    }
//}

//String dataSetIdentifier = "asoy_mb_polarity_case_1";
//for(Entity entity : f.e.getEntitiesWithAttributeValue(subject, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
//    if (entity.entityTypeName.equals("Sample")) {
//        processSample(f,entity,file);
//    }
//    entity.setEntityData(null)
//}

f.e.getUserEntitiesByTypeName(null, TYPE_DATA_SET).each {
    Entity dataSet = it
    String dataSetIdentifier = dataSet.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
    if (dataSetIdentifier.contains("case_")) {
        println "Processing "+dataSetIdentifier
        for(Entity entity : f.e.getEntitiesWithAttributeValue(dataSet.ownerKey, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
            if (entity.entityTypeName.equals("Sample")) {
                processSample(f,entity,file);
            }
            entity.setEntityData(null)
        }
    }
}

//f.e.getUserEntitiesByTypeName(subject, TYPE_SAMPLE).each {
//    Entity sample = (Entity)it;
//    processSample(f,sample,file);
//}
    
file.close()
println "Done"


def processSample(JacsUtils f, Entity sample, PrintWriter file) {
    SampleProcessor processor = new SampleProcessor()
    f.loadChildren(sample)
    String dataSetIdentifier = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
    println "    Processing "+sample.name
    List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
    childSamples.each {
        processor.process(f, it, dataSetIdentifier, file)
    }
}


class SampleProcessor {
    
    List<Entity> tiles = new ArrayList<Entity>();
//    String currJsonFilepath;
    
    public boolean process(JacsUtils f, Entity sample, String dataSetIdentifier, PrintWriter file) {
        
        tiles.clear();
                
        EntityVistationBuilder.create(new EJBEntityLoader(f.e)).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .run(new EntityVisitor() {
            public void visit(Entity tile) throws Exception {
                tiles.add(tile);
            }
        });
        
        String objective = sample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)
        String sampleName = sample.name.replaceAll("~20x","").replaceAll("~63x","")
        
        for(Entity tile : tiles) {
            
            String area = tile.getValueByAttributeName(ATTRIBUTE_ANATOMICAL_AREA);
            
            if (!"Brain".equalsIgnoreCase(area) && !"VNC".equalsIgnoreCase(area)) {
                // Skip junk like VNC-verify and error areas
                continue;
            }
                        
            String dataSetCase = dataSetIdentifier.substring(dataSetIdentifier.length()-1)
            
            file.print sample.ownerKey
            file.print "\t"
            file.print dataSetIdentifier
            file.print "\t"
            file.print dataSetCase
            file.print "\t"
            file.print sampleName
            file.print "\t"
            file.print objective
            file.print "\t"
            file.print area
            
            f.loadChildren(tile)
            List<Entity> lsms = EntityUtils.getChildrenOfType(tile, "LSM Stack")
            
//            EntityVistationBuilder.create(new EJBEntityLoader(f.e)).startAt(sample)
//                    .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
//                    .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
//                    .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
//                    .childrenOfType(EntityConstants.TYPE_TEXT_FILE)
//                    .run(new EntityVisitor() {
//                public void visit(Entity textFile) throws Exception {
//                    String lsmName = lsm.name.replaceAll(".bz2","");
//                    if (textFile.name.startsWith(lsmName) && textFile.name.endsWith(".json")) {
//                        currJsonFilepath = textFile.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
//                    }
//                }
//            });
//        
//            if (currJsonFilepath==null) {
//                println "  No JSON metadata file found for LSM file "+lsm.name
//                continue;
//            }
            
            // Sort by number of channels descending
            Collections.sort(lsms, new Comparator<Entity>() {
                @Override
                int compare(Entity o1, Entity o2) {
                    Integer c1 = new Integer(o1.getValueByAttributeName(ATTRIBUTE_CHANNEL_SPECIFICATION).length())
                    Integer c2 = new Integer(o2.getValueByAttributeName(ATTRIBUTE_CHANNEL_SPECIFICATION).length())
                    return c2.compareTo(c1);
                }
            }); 
            
            for(Entity lsm : lsms) {
                processLsm(f, lsm, file)
            }
        
            if (lsms.size()==1) {
                processLsm(f, null, file)
            }

//            LSMMetadata zm = getMetadata(lsm);

//            List<IlluminationChannel> illChannels = new ArrayList<IlluminationChannel>();
//            List<DetectionChannel> detChannels = new ArrayList<DetectionChannel>();
//
//            for(Track track : zm.getNonBleachTracks()) {
//                for(IlluminationChannel illChannel : track.getIlluminationChannels()) {
//                    file.print "\t"
//                    file.print illChannel.getWavelength()
//                    file.print "\t"
//                    file.print illChannel.getPower()
//                    file.print "\t"
//                    file.print illChannel.getPowerBc1()
//                    illChannels.add(illChannel);
//                }
//                for(DetectionChannel detChannel : track.getDetectionChannels()) {
//                    file.print "\t"
//                    file.print detChannel.getName()
//                    file.print "\t"
//                    file.print detChannel.getDyeName()
//                    file.print "\t"
//                    file.print detChannel.getWavelengthStart()
//                    file.print "\t"
//                    file.print detChannel.getWavelengthEnd()
//                    file.print "\t"
//                    file.print detChannel.getDetectorGain()
//                    detChannels.add(detChannel);
//                }
//            }
//
//            Collections.sort(illChannels, new Comparator<IlluminationChannel>() {
//                @Override
//                int compare(IlluminationChannel o1, IlluminationChannel o2) {
//                    Double o1w = Double.parseDouble(o1.getWavelength());
//                    Double o2w = Double.parseDouble(o2.getWavelength());
//                    return o1w.compareTo(o2w);
//                }
//            });
//
//            String dyePrefix = "Alexa Fluor ";
//
//            Collections.sort(detChannels, new Comparator<DetectionChannel>() {
//                @Override
//                int compare(DetectionChannel o1, DetectionChannel o2) {
//                    Double o1w = Double.parseDouble(o1.getDyeName().substring(dyePrefix.length()));
//                    Double o2w = Double.parseDouble(o2.getDyeName().substring(dyePrefix.length()));
//                    return o1w.compareTo(o2w);
//                }
//            })
//
//            int signalIndex = chanspec.indexOf('s');
//            String power = illChannels.get(signalIndex).getPowerBc1();
//            String gain = detChannels.get(signalIndex).getDetectorGain();
//
//            file.print "\t"
//            file.print power
//            file.print "\t"
//            file.print gain

            file.println ""
        }


        return true;
    }
    
    private void processLsm(JacsUtils f, Entity lsm, PrintWriter file) {
        
        if (lsm==null) {
            file.print "\t\t\t"
            return
        }
        
        file.print "\t"
        file.print lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_SPECIFICATION)
        file.print "\t"
        file.print lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_COLORS)
        file.print "\t"
        file.print lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_DYE_NAMES)
    }
    
    
//    private LSMMetadata getMetadata(Entity lsm) {
//        File jsonDataFile = SessionMgr.getSessionMgr().getCachedFile(currJsonFilepath, false);
//        return LSMMetadata.fromFile(jsonDataFile);
//    }
}
