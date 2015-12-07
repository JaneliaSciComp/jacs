import static org.janelia.it.jacs.model.entity.EntityConstants.*

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityLoader
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

class AnalyzeZeissMetadataScript {
    
    private static final boolean DEBUG = false;
    private final JacsUtils f;
    private PrintWriter file;
    private List<Entity> tiles = new ArrayList<Entity>();
        
    
    def dyeMapping = [
        
        YoshiMBPolarityCase1_20x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 594",
        YoshiMBPolarityCase1_63x : "presynaptic=Alexa Fluor 633,Alexa Fluor 647,Cy5;ignore=DY-547,Alexa Fluor 568;membrane=Alexa Fluor 594;reference=Alexa Fluor 488,Cy2",
        YoshiMBPolarityCase2_20x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 594",
        YoshiMBPolarityCase2_63x : "presynaptic=Alexa Fluor 568,Cy3;membrane=Alexa Fluor 594;reference=Alexa Fluor 488,Cy2",
        
        YoshiMBPolarityCase3_20x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 633,Alexa Fluor 647,Cy5",
        YoshiMBPolarityCase3_63x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 633,Alexa Fluor 647,Cy5;presynaptic=Alexa Fluor 568,Cy3",
        NernaPolarityCase3_20x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 633,Alexa Fluor 647,Cy5;presynaptic=Alexa Fluor 568,Cy3,DY-547",
        NernaPolarityCase3_63x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 633,Alexa Fluor 647,Cy5;presynaptic=Alexa Fluor 568,Cy3,DY-547",
        PolarityCase3WithCMTKVnc_20x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 633,Alexa Fluor 647,Cy5;presynaptic=Alexa Fluor 568,Cy3,DY-547",
        PolarityCase3WithCMTKVnc_63x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 633,Alexa Fluor 647,Cy5;presynaptic=Alexa Fluor 568,Cy3,DY-547",
        
        YoshiMBPolarityCase4_20x : "membrane=Alexa Fluor 488,Cy2;reference=Alexa Fluor 568,Cy5",
        YoshiMBPolarityCase4_63x : "membrane=Alexa Fluor 488,Cy2;reference=Alexa Fluor 568,Cy5",
        PolarityCase4WithCMTKVnc_20x : "membrane=Alexa Fluor 488,Cy2;reference=Alexa Fluor 568,Cy5",
        PolarityCase4WithCMTKVnc_63x : "membrane=Alexa Fluor 488,Cy2;reference=Alexa Fluor 568,Cy5",
        
        YoshiMBSplitMCFOCase1_20x : "reference=Alexa Fluor 488,Cy2;membrane=Alexa Fluor 594",
        YoshiMBSplitMCFOCase1_63x : "membrane_ha=,Alexa Fluor 633,Alexa Fluor 647,Cy5;membrane_v5=DY-547,Alexa Fluor 568;membrane_flag=Alexa Fluor 594;reference=Alexa Fluor 488,Cy2",
        NernaMCFOCase1_20x : "membrane_ha=Alexa Fluor 633,Alexa Fluor 647,Cy5;membrane_v5=Alexa Fluor 546,Alexa Fluor 555,Alexa Fluor 568,DY-547;membrane_flag=Alexa Fluor 594;reference=Alexa Fluor 488,Cy2",
        NernaMCFOCase1_63x : "membrane_ha=Alexa Fluor 633,Alexa Fluor 647,Cy5;membrane_v5=Alexa Fluor 546,Alexa Fluor 555,Alexa Fluor 568,DY-547;membrane_flag=Alexa Fluor 594;reference=Alexa Fluor 488,Cy2",
        NernaMCFOCase1Without20xMerge_20x : "membrane_ha=Alexa Fluor 633,Alexa Fluor 647,Cy5;membrane_v5=DY-547,Alexa Fluor 568;reference=Alexa Fluor 488,Cy2",
        NernaMCFOCase1Without20xMerge_63x : "membrane_ha=Alexa Fluor 633,Alexa Fluor 647,Cy5;membrane_v5=DY-547,Alexa Fluor 568;membrane_flag=Alexa Fluor 594;reference=Alexa Fluor 488,Cy2",
        WolfftMCFOCase1Unaligned_20x : "reference=Alexa Fluor 488,Cy2",
        WolfftMCFOCase1Unaligned_63x : "membrane_ha,membrane_v5,membrane_flag,reference"
    
    ]
    
    def dataSetMapping = [
        
        nerna_ol_split_mcfo_case_1 : "NernaMCFOCase1",
        nerna_vt_mcfo_case_1 : "NernaMCFOCase1",
        nerna_whole_brain_mcfo_case_1 : "NernaMCFOCase1",
        nerna_mcfo_case_1 : "NernaMCFOCase1",
        ditp_mcfo_case_1 : "NernaMCFOCase1",
        dicksonlab_mcfo_case_1_p1 : "NernaMCFOCase1",
        dolanm_mcfo_case_1 : "NernaMCFOCase1",
        senr_silencing_hits_mcfo_case_1 : "NernaMCFOCase1",
        goldammerj_ti_mcfo_case_1  : "NernaMCFOCase1",
        dolanm_ti_mcfo_case_1 : "NernaMCFOCase1",
        itom10_ti_mcfo_case_1 : "NernaMCFOCase1",
        nerna_polarity_case_3 : "NernaPolarityCase3",
        ditp_polarity_case_3  : "NernaPolarityCase3",
        dicksonlab_polarity_case_3  : "NernaPolarityCase3",
        dolanm_polarity_case_3 : "NernaPolarityCase3",
        goldammerj_ti_polarity_case_3 :  "NernaPolarityCase3",
        dolanm_ti_polarity_case_3  : "NernaPolarityCase3",
        itom10_ti_polarity_case_3  : "NernaPolarityCase3",
        hustons_neck_motor_neurons : "PolarityCase3WithCMTKVnc",
        leiz_frutsplit : "PolarityCase3WithCMTKVnc",
        wolfft_central_tile_mcfo_case_1 : "WolfftMCFOCase1",
        wolfft_central_tile_mcfo_vt_case_1 : "WolfftMCFOCase1",
        wolfft_mcfo_ssplit_case_1 :"WolfftMCFOCase1Unaligned",
        dolanm_vnc_checker : "YoshiMacroPolarityCase4",
        asoy_mb_polarity_case_1 : "YoshiMBPolarityCase1",
        asoy_mb_polarity_case_2 : "YoshiMBPolarityCase2",
        wolfft_polarity_ssplit_case_3 : "YoshiMBPolarityCase3",
        asoy_mb_polarity_case_3 : "YoshiMBPolarityCase3",
        asoy_mb_polarity_case_4 : "YoshiMBPolarityCase4",
        dicksonlab_trial_case_4 : "YoshiMBPolarityCase4",
        nerna_polarity_case_4   : "YoshiMBPolarityCase4",
        dolanm_split_screen :"YoshiMBPolarityCase4",
        wolfft_jenetta_case_4  : "YoshiMBPolarityCase4",
        namikis_polarity_case_4 : "YoshiMBPolarityCase4",
        asoy_mb_split_mcfo_case_1  : "YoshiMBSplitMCFOCase1",
        
    ]
    
    
    public AnalyzeZeissMetadataScript() {
        f = new JacsUtils(null, false)
    }
    
    public void run() {
        
        try {
            file = new PrintWriter("newspec.txt")
            file.print "Owner\tDataSet\tSample\tObjective\tArea\tTile\tChanSpec1\tNewSpec1\tChanSpec2\tNewSpec2\n"
            
            f.e.getUserEntitiesByTypeName(null, TYPE_DATA_SET).each {
                Entity dataSet = it
                String dataSetIdentifier = dataSet.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
                String pipeline = dataSetMapping[dataSetIdentifier]
                if (pipeline!=null) {
                    println "Processing "+dataSetIdentifier
                    for(Entity entity : f.e.getEntitiesWithAttributeValue(dataSet.ownerKey, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
                        if (entity.entityTypeName.equals("Sample")) {
                            processSample(f, entity, dataSetIdentifier, pipeline, file);
                        }
                        entity.setEntityData(null)
                    }
                }
            }
        }
        finally {
            file.close()
        }
            
        println "Done"
    }
    
    def processSample(JacsUtils f, Entity sample, String dataSetIdentifier, String pipeline, PrintWriter file) {
        def status = sample.getValueByAttributeName(ATTRIBUTE_STATUS)
        if (status.equals(VALUE_DESYNC) || status.equals(VALUE_RETIRED)) return;
        f.loadChildren(sample)
        //println "    Processing "+sample.name
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
        childSamples.each {
            processObjectiveSample(f, it, dataSetIdentifier, pipeline, file)
        }
    }
        
    def processObjectiveSample(JacsUtils f, Entity sample, String dataSetIdentifier, String pipeline, PrintWriter file) {
        
        tiles.clear();
                
        EntityVistationBuilder.create(new EJBEntityLoader(f.e)).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .run(new EntityVisitor() {
            public void visit(Entity tile) throws Exception {
                tiles.add(tile);
            }
        });
        
        String sampleName = sample.name.replaceAll("~20x","").replaceAll("~63x","")
        String objective = sample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)
        def dyeMapKey = pipeline+"_"+objective
        def dyeSpec = dyeMapping[dyeMapKey]
        if (dyeSpec==null) {
            throw new Exception("No dye spec found for "+dyeMapKey)
        }
        Map<String,String> dyeToTagMap = getDyeToTagMap(dyeSpec)
        
        for(Entity tile : tiles) {
            
            String area = tile.getValueByAttributeName(ATTRIBUTE_ANATOMICAL_AREA);
            
            file.print sample.ownerKey
            file.print "\t"
            file.print dataSetIdentifier
            file.print "\t"
            file.print sampleName
            file.print "\t"
            file.print objective
            file.print "\t"
            file.print area
            file.print "\t"
            file.print tile.name
            
            f.loadChildren(tile)
            List<Entity> lsms = EntityUtils.getChildrenOfType(tile, "LSM Stack")
            
            // Sort by number of channels descending
            Collections.sort(lsms, new Comparator<Entity>() {
                @Override
                int compare(Entity o1, Entity o2) {
                    String nc1 = o1.getValueByAttributeName(ATTRIBUTE_NUM_CHANNELS)
                    String nc2 = o2.getValueByAttributeName(ATTRIBUTE_NUM_CHANNELS)
                    if (nc1==null || nc2==null) return 0;
                    Integer c1 = new Integer(nc1)
                    Integer c2 = new Integer(nc2)
                    return c2.compareTo(c1);
                }
            }); 
        
            for(Entity lsm : lsms) {
                processLsm(f, lsm, dyeToTagMap, file)
            }
        
            if (lsms.size()==1) {
                file.print "\t\t"
            }

            file.println ""
        }
    }
    
    private void processLsm(JacsUtils f, Entity lsm, Map<String,String> dyeToTagMap, PrintWriter file) {
        
        def chanspec = lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_SPECIFICATION)
        def newspec = getNewSpec(lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_DYE_NAMES), dyeToTagMap)
        file.print "\t"
        file.print chanspec==null?"":chanspec
        file.print "\t"
        file.print newspec==null?"":newspec
    }
    
    private Map<String,String> getDyeToTagMap(String dyeSpec) {
        
        Map<String,String> dyeToTagMap = new HashMap<String,String>();
        
        String[] channels = dyeSpec.split(";");
        for(String channel : channels) {
            String[] parts = channel.split("=");
            String channelTag = parts[0];
            String[] channelDyes = parts[1].split(",");
            for(String dye : channelDyes) {
                if (dyeToTagMap.containsKey(dye)) {
                    throw new Exception("Dye "+dye+" is already mapped as "+dyeToTagMap.get(dye));
                }
                dyeToTagMap.put(dye, channelTag);
            }
        }
        
        return dyeToTagMap
    }
    
    private String getNewSpec(String dyeNames, Map<String,String> dyeToTagMap) {
        if (dyeNames==null) return null
        StringBuilder newSpec = new StringBuilder()
        for(String dye : dyeNames.split(",")) {
            String tag = dyeToTagMap.get(dye);
            if (tag==null) { 
                println "   No mapping for dye: "+dye
                newSpec.append("?")
            }
            else if (tag.startsWith("membrane")) {
                newSpec.append("m")
            }
            else if (tag.startsWith("reference")) {
                newSpec.append("r")
            }
            else if (tag.startsWith("presynaptic")) {
                newSpec.append("p")
            }
            else if (tag.startsWith("ignore")) {
                newSpec.append("x")
            }
            else {
                println "   Unrecognized tag: "+tag
                newSpec.append("?")
            }
        }
        return newSpec.toString();
    }
}

AnalyzeZeissMetadataScript script = new AnalyzeZeissMetadataScript();
script.run();
