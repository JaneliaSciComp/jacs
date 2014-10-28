package org.janelia.it.jacs.compute.service.activeData.visitor.alignment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.service.activeData.scanner.AlignmentSampleScanner;
import org.janelia.it.jacs.compute.service.activeData.visitor.ActiveVisitor;
import org.janelia.it.jacs.compute.service.activeData.visitor.IdentityEntityLoader;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by murphys on 10/3/14.
 */
public class AlignmentResultVisitor extends ActiveVisitor {

    Logger logger = Logger.getLogger(AlignmentResultVisitor.class);

    @Override
    public Boolean call() throws Exception {
        Entity sampleEntity = (Entity)contextMap.get(AlignmentSampleScanner.SAMPLE_ENTITY);
        AlignmentSampleScanner.SampleInfo sampleInfo = (AlignmentSampleScanner.SampleInfo)contextMap.get(AlignmentSampleScanner.SAMPLE_INFO);

        final List<Entity> alignmentResultList=new ArrayList<>();
        final Set<Long> visitedSet=new HashSet<>();

        // Get all Alignment Results and order
        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(sampleEntity, new EntityVisitor() {
            public void visit(Entity v) throws Exception {
                if (v.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                    alignmentResultList.add(v);
                }
            }
        }, visitedSet);
        Collections.sort(alignmentResultList, new Comparator<Entity>() {
            public int compare(Entity a, Entity b) {
                if (a.getId() < b.getId()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        for (Entity alignmentResult : alignmentResultList) {
            AlignmentSampleScanner.AlignmentResult ar = new AlignmentSampleScanner.AlignmentResult();
            ar.id=alignmentResult.getId();
            ar.spaceDescriptor=alignmentResult.getValueByAttributeName("Alignment Space");
            AlignmentSampleScanner.AlignedStackInfo stackInfo=getAlignedStackInfo(alignmentResult);
            AlignmentSampleScanner.NeuronSeparationInfo separationInfo=getNeuronSeparationInfo(alignmentResult);

            // Quality by CSV
            List<File> qualityCsvFiles=getQualityCsvFiles(alignmentResult);
            if (qualityCsvFiles.size()>0) {
                Float qiScore=getQiScoreFromCsv(qualityCsvFiles.get(0));
                if (qiScore!=null) {
                    ar.qiByCsv=qiScore;
                }
            }

            // Quality by properties of NCC type
            List<File> nccByProperyFiles=getNccByPropertyFiles(alignmentResult);
            if (nccByProperyFiles.size()>0) {
                Float qualByPropNcc=getNccQualScoreFromPropertyFile(nccByProperyFiles.get(0));
                if (qualByPropNcc!=null) {
                    ar.qualByPropNcc=qualByPropNcc;
                }
            }

            List<AlignmentSampleScanner.AlignmentResult> arList=sampleInfo.alignmentResultList;
            if (arList==null) {
                arList=new ArrayList<>();
                sampleInfo.alignmentResultList=arList;
            }
            if (stackInfo!=null) {
                ar.alignedStackInfo=stackInfo;
            }
            if (separationInfo!=null) {
                ar.neuronSeparationInfo=separationInfo;
            }
            arList.add(ar);
        }

        return true;
    }

    private Float getQiScoreFromCsv(File csvFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(csvFile));
            String firstLine = br.readLine();
            String secondLine = br.readLine();
            br.close();
            String[] valArr = secondLine.split(",");
            Float f = new Float(valArr[0].trim());
            return f;
        } catch (Exception ex) {
            return null;
        }
    }

    private Float getNccQualScoreFromPropertyFile(File propFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(propFile));
            Float nccScore=null;
            for (String line= br.readLine(); line!=null && nccScore==null; line=br.readLine()) {
                String[] vals=line.split("=");
                if (vals[0].equals("alignment.quality.score.ncc")) {
                    nccScore=new Float(vals[1].trim());
                }
            }
            br.close();
            return nccScore;
        } catch (Exception ex) {
            return null;
        }
    }

    private AlignmentSampleScanner.AlignedStackInfo getAlignedStackInfo(Entity alignmentResult) throws Exception {
        final Set<Long> visitedSet=new HashSet<>();
        final List<Entity> alignedStackList=new ArrayList<>();
        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(alignmentResult, new EntityVisitor() {
            public void visit(Entity v) throws Exception {
                if (v.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D) ||
                    v.getEntityTypeName().equals(EntityConstants.TYPE_ALIGNED_BRAIN_STACK)) {
                    if (v.getName().startsWith("Aligned")) {
                        alignedStackList.add(v);
                    }
                }
            }
        }, visitedSet);
        if (alignedStackList.size()==0) {
            return null;
        } else {
            Entity alignedStack=alignedStackList.get(0);
            AlignmentSampleScanner.AlignedStackInfo stackInfo=new AlignmentSampleScanner.AlignedStackInfo();
            stackInfo.id=alignedStack.getId();
            stackInfo.path=alignedStack.getValueByAttributeName("File Path");
            stackInfo.pixelResolution=alignedStack.getValueByAttributeName("Pixel Resolution");
            stackInfo.opticalResolution=alignedStack.getValueByAttributeName("Optical Resolution");

            String aisString=alignedStack.getValueByAttributeName("Alignment Inconsistency Score");

            if (aisString!=null) {
                if (aisString.contains(",")) {
                    logger.error("stackId="+stackInfo.id+" contains commans in pure Float attribute Alignment Inconsistency Score");
                }
                stackInfo.alignmentInconsistencyScore=new Float(aisString.trim());
            }

            String aisComboString=alignedStack.getValueByAttributeName("Alignment Inconsistency Scores");
            if (aisComboString!=null) {
                String[] spArr=aisComboString.split(",");
                if (spArr.length==3) {
                    stackInfo.alignmentInconsistencyScore_0=new Float(spArr[0].trim());
                    stackInfo.alignmentInconsistencyScore_1=new Float(spArr[1].trim());
                    stackInfo.alignmentIncosistencyScore_2=new Float(spArr[2].trim());
                }
            }

            String mvString=alignedStack.getValueByAttributeName("Alignment Model Violation Score");
            if (mvString!=null) {
                stackInfo.alignmentModelViolationScore=new Float(mvString.trim());
            }

            String nccString=alignedStack.getValueByAttributeName("Alignment Normalized Cross Correlation Score");
            if (nccString!=null) {
                stackInfo.alignmentNccScore=new Float(nccString.trim());
            }

            return stackInfo;
        }
    }

    private List<File> getQualityCsvFiles(Entity alignmentResult) throws Exception {
        final Set<Long> visitedSet=new HashSet<>();
        final List<File> qualityCsvFiles=new ArrayList<>();
        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(alignmentResult, new EntityVisitor() {
            public void visit(Entity v) throws Exception {
                if (v.getEntityTypeName().equals(EntityConstants.TYPE_TEXT_FILE) &&
                        v.getName().endsWith("quality.csv")) {
                    qualityCsvFiles.add(new File(v.getValueByAttributeName("File Path")));
                }
            }
        }, visitedSet);
        return qualityCsvFiles;
    }

    private List<File> getNccByPropertyFiles(Entity alignmentResult) throws Exception {
        final Set<Long> visitedSet=new HashSet<>();
        final List<File> nccPropFiles=new ArrayList<>();
        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(alignmentResult, new EntityVisitor() {
            public void visit(Entity v) throws Exception {
                if (v.getEntityTypeName().equals(EntityConstants.TYPE_TEXT_FILE) && v.getName().toLowerCase().contains("align") &&
                        v.getName().endsWith(".properties")) {
                    nccPropFiles.add(new File(v.getValueByAttributeName("File Path")));
                }
            }
        }, visitedSet);
    return nccPropFiles;
    }

    private AlignmentSampleScanner.NeuronSeparationInfo getNeuronSeparationInfo(Entity alignmentResult) throws Exception {
        // Under "Alignment Result"
        //         "Neuron Separator Pipeline Result"
        //           "Supporting Data" called "Supporting Files"
        //              "Image 3D" called "ConsolidatedLabel.v3dpbd"
        //              "Image 3D" called "ConsolidatedSignal.v3dpbd"
        //           "Neuron Fragment Collection" called "Neuron Fragments"
        //              "Neuron Fragment" called "Neuron Fragment <x>"
        //                "Mask Image" called "Mask Image" ?
        final AlignmentSampleScanner.NeuronSeparationInfo sepInfo=new AlignmentSampleScanner.NeuronSeparationInfo();
        Set<Entity> arChildren=alignmentResult.getChildren();
        for (Entity separatorResult : arChildren) {
            if (separatorResult.getEntityTypeName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                sepInfo.id=separatorResult.getId();
                Set<Entity> srChildren=separatorResult.getChildren();
                for (Entity src : srChildren) {
                    if (src.getEntityTypeName().equals(EntityConstants.TYPE_SUPPORTING_DATA)) {
                        final Set<Long> visitedSet=new HashSet<>();
                        EntityVistationBuilder.create(new IdentityEntityLoader()).setVisitRootOwnerOwnedEntitiesOnly(false).runRecursively(src, new EntityVisitor() {
                            public void visit(Entity v) throws Exception {
                                if (v.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D) && v.getName().toLowerCase().contains("consolidated")) {
                                    if (v.getName().toLowerCase().startsWith("consolidatedlabel")) {
                                        sepInfo.consolidatedLabelPath = v.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                    }
                                    if (v.getName().toLowerCase().startsWith("consolidatedsignal")) {
                                        sepInfo.consolidatedSignalPath=v.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                                    }
                                }
                            }
                        }, visitedSet);
                    } else if (src.getEntityTypeName().equals(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION)) {
                        Set<Entity> fragments=src.getChildren();
                        for (Entity fragment : fragments) {
                            if (fragment.getEntityTypeName().equals(EntityConstants.TYPE_NEURON_FRAGMENT)) {
                                List<AlignmentSampleScanner.NeuronFragmentInfo> fragmentInfoList=sepInfo.fragmentInfoList;
                                if (fragmentInfoList==null) {
                                    fragmentInfoList=new ArrayList<>();
                                    sepInfo.fragmentInfoList=fragmentInfoList;
                                }
                                AlignmentSampleScanner.NeuronFragmentInfo fragmentInfo=new AlignmentSampleScanner.NeuronFragmentInfo();
                                fragmentInfo.id=fragment.getId();
                                String indexString=fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
                                if (indexString!=null) {
                                    fragmentInfo.index=new Integer(indexString.trim());
                                }
                                Set<EntityData> fed=fragment.getEntityData();
                                for (EntityData ed : fed) {
                                    if (ed.getValue()!=null && ed.getValue().endsWith(".mask")) {
                                        fragmentInfo.maskPath=ed.getValue();
                                    }
                                }
                                sepInfo.fragmentInfoList.add(fragmentInfo);
                            }
                        }
                        Collections.sort(sepInfo.fragmentInfoList, new Comparator<AlignmentSampleScanner.NeuronFragmentInfo>() {
                            @Override
                            public int compare(AlignmentSampleScanner.NeuronFragmentInfo o1, AlignmentSampleScanner.NeuronFragmentInfo o2) {
                                if (o1.index>o2.index) {
                                    return -1;
                                } else {
                                    return 1;
                                }
                            }
                        });
                        sepInfo.neuronCount=sepInfo.fragmentInfoList.size();
                    }
                }
            }
        }
        return sepInfo;
    }

}
