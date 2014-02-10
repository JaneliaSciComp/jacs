package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.IlluminationChannel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Track;

/**
 * Runs a Fiji macro that requires a 20x set of LSMs (Brain, and possibly VNC) as input.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunFiji20xBrainVNCMacro extends RunFijiMacroService {

    private static final String dyePrefix = "Alexa Fluor ";
    
    private Entity brainLsm;
    private Entity vncLsm;
    private String jsonFilepath;
    private Integer power;
    private Integer gain;
    
    @Override
    protected String getMacroParameter(Entity sampleEntity, String outputFilePrefix) throws Exception {

        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsmStack) throws Exception {
                String area = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                if ("VNC".equalsIgnoreCase(area)) {
                    vncLsm = lsmStack;
                } 
                else if ("Brain".equalsIgnoreCase(area)) {
                    brainLsm = lsmStack;
                }
            }
        });
        
        String brainFilepath = "";
        String vncFilepath = "";
        
        if (brainLsm!=null) {
            registerLsmAttributes(sampleEntity, brainLsm);
            brainFilepath = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        }
        
        if (vncLsm!=null) {
            registerLsmAttributes(sampleEntity, vncLsm);
            vncFilepath = vncLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        }
        
        String chanSpec = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (vncLsm!=null) {
            String vncChanSpec = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
            if (!chanSpec.equals(vncChanSpec)) {
                throw new IllegalStateException("Brain chanspec ("+chanSpec+") does not match VNC chanspec ("+vncChanSpec+")");
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(outputFilePrefix);
        sb.append(",");
        sb.append(brainFilepath);
        sb.append(",");
        sb.append(vncFilepath);
        sb.append(",");
        sb.append(power);
        sb.append(",");
        sb.append(gain);
        sb.append(",");
        sb.append(chanSpec);
        return sb.toString();
    }
    
    private void registerLsmAttributes(final Entity sampleEntity, final Entity lsm) throws Exception {

        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                .childrenOfType(EntityConstants.TYPE_TEXT_FILE)
                .run(new EntityVisitor() {
            public void visit(Entity textFile) throws Exception {
                if (textFile.getName().startsWith(lsm.getName()) && textFile.getName().endsWith(".json")) {
                    jsonFilepath = textFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                }
            }
        });
    
        if (jsonFilepath==null) {
            throw new IllegalStateException("No JSON metadata file found for LSM file "+lsm.getName());
        }
        
        String area = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        if (!"Brain".equalsIgnoreCase(area) && !"VNC".equalsIgnoreCase(area)) {
            // Skip junk like VNC-verify and incorrectly annotated areas
            return;
        }

        String chanspec = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);

        LSMMetadata zm = LSMMetadata.fromFile(new File(jsonFilepath));

        List<IlluminationChannel> illChannels = new ArrayList<IlluminationChannel>();
        List<DetectionChannel> detChannels = new ArrayList<DetectionChannel>();

        for(Track track : zm.getNonBleachTracks()) {
            illChannels.addAll(track.getIlluminationChannels());
            detChannels.addAll(track.getDetectionChannels());
        }

        Collections.sort(illChannels, new Comparator<IlluminationChannel>() {
            @Override
            public int compare(IlluminationChannel o1, IlluminationChannel o2) {
                Double o1w = Double.parseDouble(o1.getWavelength());
                Double o2w = Double.parseDouble(o2.getWavelength());
                return o1w.compareTo(o2w);
            }
        });

        Collections.sort(detChannels, new Comparator<DetectionChannel>() {
            @Override
            public int compare(DetectionChannel o1, DetectionChannel o2) {
                Double o1w = Double.parseDouble(o1.getDyeName().substring(dyePrefix.length()));
                Double o2w = Double.parseDouble(o2.getDyeName().substring(dyePrefix.length()));
                return o1w.compareTo(o2w);
            }
        });

        int signalIndex = chanspec.indexOf('s');
        Float powerFloat = new Float(illChannels.get(signalIndex).getPowerBc1());
        Float gainFloat = new Float(detChannels.get(signalIndex).getDetectorGain());

        Integer power = Math.round(powerFloat);
        Integer gain = Math.round(gainFloat);
        
        if (this.power!=null) {
            if (!this.power.equals(power)) {
                logger.warn("Inconsistent power: "+this.power+"!="+power);
            }
        }
        else {
            this.power = power;
        }

        if (this.gain!=null) {
            if (!this.gain.equals(gain)) {
                logger.warn("Inconsistent gain: "+this.gain+"!="+gain);
            }
        }
        else {
            this.gain = gain;
        }
    }
}
