import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBEntityLoader
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.workstation.gui.framework.exception_handlers.ExitHandler
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.workstation.gui.util.panels.ApplicationSettingsPanel
import org.janelia.it.workstation.gui.util.panels.UserAccountSettingsPanel
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel
import org.janelia.it.workstation.shared.util.ConsoleProperties
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.IlluminationChannel
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Track

// Globals
subject = "user:asoy"
f = new JacsUtils(subject, false)

login()

file = new PrintWriter("asoy_metadata_20x.txt")

int numSamples = 0

file.print "DataSet\tSample\tArea\tLSM\tNumChannels\tChanSpec\tColors\tDyes\t"
file.print "IllChan1Wavelength\tIllChan1Power\tIllChan1PowerBc1\t"
file.print "IllChan2Wavelength\tIllChan2Power\tIllChan2PowerBc1\t"
file.print "DetChan1\tDetChan1Dye\tDetChan1Start\tDetChan1End\tDetChan1Gain\t"
file.print "DetChan2\tDetChan2Dye\tDetChan2Start\tDetChan2End\tDetChan2Gain\t"
file.print "Power\tGain\n"

f.e.getUserEntitiesByTypeName(subject, TYPE_SAMPLE).each {
    Entity sample = (Entity)it;
    if ("20x".equals(sample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE))) {
        SampleProcessor processor = new SampleProcessor()
        if (processor.process(f, sample, file)) {
            numSamples++;
        }
        sample.setEntityData(null);
    }
}

file.close()
println "Num samples processed: "+numSamples

class SampleProcessor {
    
    List<Entity> lsms = new ArrayList<Entity>();
    String currJsonFilepath;
    
    public boolean process(JacsUtils f, Entity sample, PrintWriter file) {
        
        String dataSetIdentifier = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)

        if (dataSetIdentifier==null) {
            Entity parentSample = f.e.getAncestorWithType(sample.ownerKey, sample.id, TYPE_SAMPLE)
            dataSetIdentifier = parentSample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
        }   
             
        if (dataSetIdentifier=="asoy_cell_count") return false;
        
        EntityVistationBuilder.create(new EJBEntityLoader(f.e)).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsmStack) throws Exception {
                lsms.add(lsmStack);
            }
        });

        println "Processing "+sample.name

        for(Entity lsm : lsms) {

            EntityVistationBuilder.create(new EJBEntityLoader(f.e)).startAt(sample)
                    .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                    .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                    .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                    .childrenOfType(EntityConstants.TYPE_TEXT_FILE)
                    .run(new EntityVisitor() {
                public void visit(Entity textFile) throws Exception {
                    if (textFile.name.startsWith(lsm.name) && textFile.name.endsWith(".json")) {
                        currJsonFilepath = textFile.getValueByAttributeName(ATTRIBUTE_FILE_PATH)   
                    }
                }
            });
        
            if (currJsonFilepath==null) {
                println "  No JSON metadata file found for LSM file "+lsm.name
                continue;
            }
            
            String area = lsm.getValueByAttributeName(ATTRIBUTE_ANATOMICAL_AREA);
            
            if (!"Brain".equalsIgnoreCase(area) && !"VNC".equalsIgnoreCase(area)) {
                // Skip junk like VNC-verify and error areas
                continue;
            }

            String chanspec = lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_SPECIFICATION);

            file.print dataSetIdentifier
            file.print "\t"
            file.print sample.name
            file.print "\t"
            file.print area
            file.print "\t"
            file.print lsm.name
            file.print "\t"
            file.print lsm.getValueByAttributeName(ATTRIBUTE_NUM_CHANNELS)
            file.print "\t"
            file.print chanspec
            file.print "\t"
            file.print lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_COLORS)
            file.print "\t"
            file.print lsm.getValueByAttributeName(ATTRIBUTE_CHANNEL_DYE_NAMES)

            LSMMetadata zm = getMetadata(lsm);

            List<IlluminationChannel> illChannels = new ArrayList<IlluminationChannel>();
            List<DetectionChannel> detChannels = new ArrayList<DetectionChannel>();

            for(Track track : zm.getNonBleachTracks()) {
                for(IlluminationChannel illChannel : track.getIlluminationChannels()) {
                    file.print "\t"
                    file.print illChannel.getWavelength()
                    file.print "\t"
                    file.print illChannel.getPower()
                    file.print "\t"
                    file.print illChannel.getPowerBc1()
                    illChannels.add(illChannel);
                }
                for(DetectionChannel detChannel : track.getDetectionChannels()) {
                    file.print "\t"
                    file.print detChannel.getName()
                    file.print "\t"
                    file.print detChannel.getDyeName()
                    file.print "\t"
                    file.print detChannel.getWavelengthStart()
                    file.print "\t"
                    file.print detChannel.getWavelengthEnd()
                    file.print "\t"
                    file.print detChannel.getDetectorGain()
                    detChannels.add(detChannel);
                }
            }

            Collections.sort(illChannels, new Comparator<IlluminationChannel>() {
                @Override
                int compare(IlluminationChannel o1, IlluminationChannel o2) {
                    Double o1w = Double.parseDouble(o1.getWavelength());
                    Double o2w = Double.parseDouble(o2.getWavelength());
                    return o1w.compareTo(o2w);
                }
            });

            String dyePrefix = "Alexa Fluor ";

            Collections.sort(detChannels, new Comparator<DetectionChannel>() {
                @Override
                int compare(DetectionChannel o1, DetectionChannel o2) {
                    Double o1w = Double.parseDouble(o1.getDyeName().substring(dyePrefix.length()));
                    Double o2w = Double.parseDouble(o2.getDyeName().substring(dyePrefix.length()));
                    return o1w.compareTo(o2w);
                }
            })

            int signalIndex = chanspec.indexOf('s');
            String power = illChannels.get(signalIndex).getPowerBc1();
            String gain = detChannels.get(signalIndex).getDetectorGain();

            file.print "\t"
            file.print power
            file.print "\t"
            file.print gain

            file.println ""
        }


        return true;
    }
    
    private LSMMetadata getMetadata(Entity lsm) {
        File jsonDataFile = SessionMgr.getSessionMgr().getCachedFile(currJsonFilepath, false);
        return LSMMetadata.fromFile(jsonDataFile);
    }
}
    


// Need this in order to use WebDAV
def login() {
    try {
           // This try block is copied from ConsoleApp. We may want to consolidate these in the future.
           ConsoleProperties.load();
           FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
           final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
           sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
           sessionMgr.registerExceptionHandler(new ExitHandler());
           sessionMgr.registerPreferenceInterface(ApplicationSettingsPanel.class, ApplicationSettingsPanel.class);
           sessionMgr.registerPreferenceInterface(UserAccountSettingsPanel.class, UserAccountSettingsPanel.class);
           sessionMgr.registerPreferenceInterface(ViewerSettingsPanel.class, ViewerSettingsPanel.class);
           SessionMgr.getSessionMgr().loginSubject();
    }
    catch (Exception e) {
           SessionMgr.getSessionMgr().handleException(e);
           SessionMgr.getSessionMgr().systemExit();
    }
}