package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.asyncservice.fileservices.LinkDataProcessor;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentConfiguration;
import org.janelia.jacs2.asyncservice.imageservices.align.AlignmentUtils;
import org.janelia.jacs2.asyncservice.imageservices.align.ImageCoordinates;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RawFilesAlignmentProcessor executes the alignment "pipeline" and assumes all files are already
 * in v3draw format.
 */
@Named("alignRaw")
public class RawFilesAlignmentProcessor extends AbstractBasicLifeCycleServiceProcessor<Void, List<File>> {

    private static final String MAX_AFFINE_ITERATIONS = "10000x10000x10000x10000";
    private static final String MAX_CC_ITERATIONS ="100x70x50x0x0" ;
    private static final int TARSXEXTDX = 1933;
    private static final int TARSXEXTDY = 1713;
    private static final int TARSXEXTDZ = 640;

    private static final int IMAGE_SIZE_DX = 1450;
    private static final int IMAGE_SIZE_DY = 725;
    private static final int IMAGE_SIZE_DZ = 436;

    static class AlignmentArgs extends ServiceArgs {
        @Parameter(names = {"-nthreads"}, description = "Number of ITK threads")
        Integer nthreads = 16;
        @Parameter(names = "-i1File", description = "The name of the first input file", required = true)
        String input1File;
        @Parameter(names = "-i1Channels", description = "The channels of the first input file", required = true)
        int input1Channels;
        @Parameter(names = "-i1Ref", description = "The reference for the first input file", required = true)
        int input1Ref;
        @Parameter(names = "-i1Res", description = "The resolution of the first input file", required = true)
        String input1Res;
        @Parameter(names = "-i1Dims", description = "The dimensions of the first input file", required = false)
        String input1Dims;
        @Parameter(names = {"-e", "-i1Neurons"}, description = "Input1 neurons file", required = false)
        String input1Neurons;
        @Parameter(names = {"-c", "-config"}, description = "Configuration file", required = true)
        String configFile;
        @Parameter(names = {"-t", "-templateDir"}, description = "Template directory", required = true)
        String templateDir;
        @Parameter(names = {"-k", "-toolsDir"}, description = "Tools directory", required = true)
        String toolsDir;
        @Parameter(names = {"-s", "-step"}, description = "Step", required = false)
        String step;
        @Parameter(names = {"-m", "-mp", "-mountingProtocol"}, description = "Mounting protocol", required = false)
        String mountingProtocol;
        @Parameter(names = {"-alignmentSpace"}, description = "Alignment space name", required = false)
        String alignmentSpace = "";
        @Parameter(names = {"-targetTemplate"}, description = "Target template", required = false)
        String targetTemplate;
        @Parameter(names = {"-targetExtTemplate"}, description = "Target EXT template", required = false)
        String targetExtTemplate;
        @Parameter(names = {"-z", "-zflip"}, arity = 0, description = "Z flip flag", required = false)
        boolean zFlip = false;
        @Parameter(names = "-fslOutputType", description = "FSL output type", required = false)
        String fslOutputType = "NIFTI_GZ";
        @Parameter(names = {"-o", "-w", "-resultsDir"}, description = "Results directory", required = false)
        String resultsDir;
    }

    private final LinkDataProcessor linkDataProcessor;
    private final Vaa3dPluginProcessor vaa3dPluginProcessor;
    private final AffineToInsightConverterProcessor affineToInsightConverterProcessor;
    private final FlirtProcessor flirtProcessor;
    private final AntsToolProcessor antsToolProcessor;
    private final AlignmentVerificationMovieProcessor alignmentVerificationMovieProcessor;
    private final ImageServicesInvocationHelper invocationHelper;

    @Inject
    RawFilesAlignmentProcessor(ServiceComputationFactory computationFactory,
                               JacsServiceDataPersistence jacsServiceDataPersistence,
                               @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                               LinkDataProcessor linkDataProcessor,
                               Vaa3dConverterProcessor vaa3dConverterProcessor,
                               Vaa3dPluginProcessor vaa3dPluginProcessor,
                               NiftiConverterProcessor niftiConverterProcessor,
                               AffineToInsightConverterProcessor affineToInsightConverterProcessor,
                               FlirtProcessor flirtProcessor,
                               AntsToolProcessor antsToolProcessor,
                               WarpToolProcessor warpToolProcessor,
                               AlignmentVerificationMovieProcessor alignmentVerificationMovieProcessor,
                               Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        invocationHelper = new ImageServicesInvocationHelper(jacsServiceDataPersistence,
                linkDataProcessor,
                vaa3dConverterProcessor,
                vaa3dPluginProcessor,
                niftiConverterProcessor,
                warpToolProcessor,
                logger);
        this.linkDataProcessor = linkDataProcessor;
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
        this.affineToInsightConverterProcessor = affineToInsightConverterProcessor;
        this.flirtProcessor = flirtProcessor;
        this.antsToolProcessor = antsToolProcessor;
        this.alignmentVerificationMovieProcessor = alignmentVerificationMovieProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AlignmentArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
            final String resultsPattern = "glob:**/*.{v3draw}";

            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return areAllDependenciesDone(depResults.getJacsServiceData());
            }

            @Override
            public List<File> collectResult(JacsServiceResult<?> depResults) {
                AlignmentArgs args = getArgs(depResults.getJacsServiceData());
                return FileUtils.lookupFiles(getResultsDir(args), 1, resultsPattern)
                        .map(Path::toFile)
                        .collect(Collectors.toList());
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            AlignmentArgs args = getArgs(jacsServiceData);
            Files.createDirectories(getResultsDir(args));
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected JacsServiceResult<Void> submitServiceDependencies(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        AlignmentConfiguration alignConfig = AlignmentUtils.parseAlignConfig(args.configFile);
        ImageCoordinates inputResolution = AlignmentUtils.parseCoordinates(args.input1Res);

        Path subjectFile = getWorkingFile(args.input1File, jacsServiceData); // => SUBSX
        Path targetFile = getWorkingFile(args.targetTemplate, jacsServiceData); // => TARSX
        Path targetExtFile = getWorkingFile(args.targetExtTemplate, jacsServiceData); // => TARSXEXT

        Path targetExtNiftiFile = getNiftiChannelFile(targetExtFile, 0, jacsServiceData); // => TARSXNII
        Path targetExtDownsampledFile = getNiftiDownsampledFile(targetExtFile, jacsServiceData); // => FDS

        Path isotropicSubjectFile = getIsotropicFile(subjectFile, jacsServiceData); // => SUBSXIS
        Path resizedSubjectFile = getResizedFile(subjectFile, jacsServiceData); // => SUBSXRS
        Path resizedSubjectRefChannelFile = getRefChannelFile(resizedSubjectFile, jacsServiceData); // => SUBSXRSRFC

        Path resizedSubjectRefChannelNiftiFile = getNiftiChannelFile(resizedSubjectRefChannelFile, 0, jacsServiceData); // => SUBSXRSRFCNII
        Path resizedSubjectRefChannelDownsampledFile = getNiftiDownsampledFile(resizedSubjectRefChannelFile, jacsServiceData); // => MDS

        Path rotationsMatFile = getRotationsMatrixFile(subjectFile, jacsServiceData); // => RCMAT
        Path insightRotationsFile = getInsightRotationsMatrixFile(subjectFile, jacsServiceData); // => RCOUT
        Path rotationsAffineFile = getAffineRotationsMatrixFile(subjectFile, jacsServiceData); // => $RCAFFINE

        Path rotatedResizedSubjectRefChannelFile = getRotatedFile(resizedSubjectRefChannelFile, jacsServiceData); // => SUBSXRFCROT
        Path rotatedResizedSubjectRefChanneltNiftiFile = getNiftiChannelFile(rotatedResizedSubjectRefChannelFile, 0, jacsServiceData); // => SUBNII

        Path rotatedResizedSubjectFile = getRotatedFile(resizedSubjectFile, jacsServiceData); // => SUBSXRSROT

        Path globalSymmetricTransformFilePrefix = getGlobalSymmetricTransformFilePrefix(subjectFile, jacsServiceData); // => SIMMETRIC txmi
        Path symmetricAffineTransformFile = getAffineTransformFile(globalSymmetricTransformFilePrefix, jacsServiceData); // => AFFINEMATRIX
        Path rotatedSubjectGlobalAllignedFile = getGlobalAlignedFile(rotatedResizedSubjectFile, jacsServiceData); // => SUBSXRSROTGA
        Path resizedRotatedSubjectGlobalAllignedFile = getResizedFile(rotatedSubjectGlobalAllignedFile, jacsServiceData); // => SUBSXRSROTGARS
        List<Path> resizedRotatedSubjectGlobalAlignedNiftiFiles = getNiftiChannelsFiles(resizedRotatedSubjectGlobalAllignedFile, args.input1Channels, jacsServiceData); // => MOVINGNIICI, MOVINGNIICII, MOVINGNIICIII, MOVINGNIICIV

        Path resizedTargetFile = getResizedFile(targetFile, jacsServiceData); // => TARSXRS
        Path resizedTargetNiftiFile = getNiftiChannelFile(resizedTargetFile, 0, jacsServiceData); // => FIXEDNII, FIX

        Path resizedSubjectGlobalAlignedRefChannelNiftiFile = resizedRotatedSubjectGlobalAlignedNiftiFiles.get(args.input1Ref - 1);  // => MOVINGNIICR, MOV

        Path localSymmetricTransformFilePrefix = getLocalSymmetricTransformFilePrefix(subjectFile, jacsServiceData); // => SIMMETRIC ccmi
        Path localAffineTransformFile = getAffineTransformFile(localSymmetricTransformFilePrefix, jacsServiceData); // => AFFINEMATRIXLOCAL
        Path localWarpFile = getWarpTransformFile(localSymmetricTransformFilePrefix, jacsServiceData); // => FWDDISPFIELD

        createWorkingCopy(Paths.get(args.templateDir, args.targetTemplate), targetFile);
        createWorkingCopy(Paths.get(args.templateDir, args.targetExtTemplate), targetExtFile);
        // $Vaa3D -x ireg -f NiftiImageConverter -i $TARSXEXT
        JacsServiceData targetExtToNiftiServiceData =
                invocationHelper.convertToNiftiImage(targetExtFile, targetExtNiftiFile,
                        "Convert extended target to nifti",
                        jacsServiceData);
        //  $Vaa3D -x ireg -f zflip -i ${SUBSX} -o ${TEMPSUBJECT}
        JacsServiceData flippedSubjectServiceData = zFlip(Paths.get(args.input1File), subjectFile, args.zFlip,
                "Flip subject",
                jacsServiceData);
        // $Vaa3D -x ireg -f isampler -i $SUBSX -o $SUBSXIS -p "#x $ISRX #y $ISRY #z $ISRZ"
        JacsServiceData isotropicSubjectSamplingServiceData = invocationHelper.isotropicSampling(
                subjectFile, alignConfig, inputResolution, isotropicSubjectFile,
                null, // no other parameters for subject sampling
                "Isotropic sampling",
                jacsServiceData,
                flippedSubjectServiceData);
        // $Vaa3D -x ireg -f resizeImage -o $SUBSXRS -p "#s $SUBSXIS #t $TARSXEXT #y 1"
        JacsServiceData resizeSubjectServiceData = invocationHelper.resizeToTarget(
                isotropicSubjectFile, targetExtFile, resizedSubjectFile,
                "#y 1",
                "Resize subject to target",
                jacsServiceData,
                isotropicSubjectSamplingServiceData);
        // $Vaa3D -x refExtract -f refExtract -i $SUBSXRS -o $SUBSXRSRFC -p "#c $SUBSXREF";
        JacsServiceData extractRefChannelServiceData = invocationHelper.extractRefFromSubject(
                resizedSubjectFile, resizedSubjectRefChannelFile, args.input1Ref,
                "Extract reference channel",
                jacsServiceData,
                resizeSubjectServiceData);
        // convert the ref channel to Nifti
        // $Vaa3D -x ireg -f NiftiImageConverter -i $SUBSXRSRFC
        JacsServiceData resizedSubjectToNiftiServiceData = invocationHelper.convertToNiftiImage(
                resizedSubjectRefChannelFile, resizedSubjectRefChannelNiftiFile,
                "Convert ref channel to nifti",
                jacsServiceData,
                extractRefChannelServiceData);
        double downsampleFactor = 0.125;
        // downsample the target with ration 1/8
        // $Vaa3D -x ireg -f resamplebyspacing -i $TARSXNII -o $FDS -p "#x $DSFAC #y $DSFAC #z $DSFAC"
        JacsServiceData targetDownsampleServiceData = invocationHelper.downsampleImage(
                targetExtNiftiFile, targetExtDownsampledFile, downsampleFactor,
                "Downsample 1/8 ext target",
                jacsServiceData,
                targetExtToNiftiServiceData);
        // downsample the subject with ration 1/8
        // $Vaa3D -x ireg -f resamplebyspacing -i $SUBSXRSRFCNII -o $MDS -p "#x $DSFAC #y $DSFAC #z $DSFAC"
        JacsServiceData subjectDownsampleServiceData =
                invocationHelper.downsampleImage(
                        resizedSubjectRefChannelNiftiFile, resizedSubjectRefChannelDownsampledFile, downsampleFactor,
                        "Downsample 1/8 ref subject channel",
                        jacsServiceData,
                        resizedSubjectToNiftiServiceData);
        // find the rotations with FLIRT
        // $FLIRT -in $MDS -ref $FDS -omat $RCMAT -cost mutualinfo -searchrx -180 180 -searchry -180 180 -searchrz -180 180 -dof 12 -datatype char
        JacsServiceData estimateRotationsServiceData = findRotationMatrix(resizedSubjectRefChannelDownsampledFile, targetExtDownsampledFile, rotationsMatFile, args.fslOutputType,
                "Find the rotations",
                jacsServiceData,
                targetDownsampleServiceData, subjectDownsampleServiceData);
        // Convert the affine matrix to insight
        JacsServiceData affine2InsightServiceData = convertAffineToInsight(rotationsMatFile, insightRotationsFile,
                "Convert affine matrix to insight",
                jacsServiceData,
                estimateRotationsServiceData);
        // $Vaa3D -x ireg -f extractRotMat -i $RCOUT -o $RCAFFINE
        JacsServiceData affinePrepServiceData = prepareAffineTransformation(insightRotationsFile, rotationsAffineFile,
                "Prepare affine transformations",
                jacsServiceData,
                affine2InsightServiceData);
        // rotate the subject
        // $Vaa3D -x ireg -f iwarp -o $SUBSXRFCROT -p "#s $SUBSXRSRFC #t $TARSXEXT #a $RCAFFINE"
        JacsServiceData rotateSubjectServiceData = rotateSubject(resizedSubjectRefChannelFile, targetExtFile, rotationsAffineFile, rotatedResizedSubjectRefChannelFile,
                "Rotate subject",
                jacsServiceData,
                extractRefChannelServiceData, affinePrepServiceData);
        // convert rotated subject to Nifti
        // $Vaa3D -x ireg -f NiftiImageConverter -i $SUBSXRFCROT
        JacsServiceData subjectToNiftiServiceData = invocationHelper.convertToNiftiImage(
                rotatedResizedSubjectRefChannelFile, rotatedResizedSubjectRefChanneltNiftiFile,
                "Convert rotated subject to nifti",
                jacsServiceData,
                rotateSubjectServiceData);
        // global alignment of the subject to target
        // $ANTS 3 -m  MI[ $TARSXNII, $SUBNII, 1, 32] -o $SIMMETRIC -i 0 --number-of-affine-iterations $MAXITERATIONS #--rigid-affine true
        JacsServiceData globalAlignServiceData = globalAlignSubjectToTarget(targetExtNiftiFile, rotatedResizedSubjectRefChanneltNiftiFile, globalSymmetricTransformFilePrefix,
                "Global alignment of subject to target",
                jacsServiceData,
                targetExtToNiftiServiceData, subjectToNiftiServiceData);
        // rotate recentered object
        // $Vaa3D -x ireg -f iwarp2 -o $SUBSXRSROT -p "#s $SUBSXRS #a $RCAFFINE #dx $TARSXEXTDX #dy $TARSXEXTDY #dz $TARSXEXTDZ"
        JacsServiceData rotateRecenteredSubjectServiceData = invocationHelper.applyIWarp2Transformation(
                resizedSubjectFile, rotationsAffineFile, rotatedResizedSubjectFile,
                null,
                TARSXEXTDX, TARSXEXTDY, TARSXEXTDZ,
                "Rotate recentered object",
                jacsServiceData,
                resizeSubjectServiceData, rotateSubjectServiceData);
        // affine transform rotated subject
        // $Vaa3D -x ireg -f iwarp2 -o $SUBSXRSROTGA -p "#s $SUBSXRSROT #a $AFFINEMATRIX #dx $TARSXEXTDX #dy $TARSXEXTDY #dz $TARSXEXTDZ"
        JacsServiceData globalAlignedSubjectServiceData = invocationHelper.applyIWarp2Transformation(
                rotatedResizedSubjectFile, symmetricAffineTransformFile, rotatedSubjectGlobalAllignedFile,
                null,
                TARSXEXTDX, TARSXEXTDY, TARSXEXTDZ,
                "Affine transform rotated subject",
                jacsServiceData,
                rotateRecenteredSubjectServiceData, globalAlignServiceData);
        // $Vaa3D -x ireg -f genVOIs -p "#s $SUBSXRSROTGA #t $TARSX"
        JacsServiceData voiServiceData = getVOI(rotatedSubjectGlobalAllignedFile, targetFile,
                "Gen VOID for global aligned subject",
                jacsServiceData,
                globalAlignedSubjectServiceData);
        // $Vaa3D -x ireg -f NiftiImageConverter -i $SUBSXRSROTGARS
        JacsServiceData resizedAlignedSubjectToNiftiServiceData =
                invocationHelper.convertToNiftiImage(resizedRotatedSubjectGlobalAllignedFile, resizedRotatedSubjectGlobalAlignedNiftiFiles,
                        "Convert global aligned subject to nifti",
                        jacsServiceData,
                        voiServiceData);
        // $Vaa3D -x ireg -f NiftiImageConverter -i $TARSXRS
        JacsServiceData resizedTargetToNiftiServiceData = invocationHelper.convertToNiftiImage(
                resizedTargetFile, resizedTargetNiftiFile,
                "Convert resized target to nifti",
                jacsServiceData,
                voiServiceData);
        // $ANTS 3 -m  CC[ $FIX, $MOV, 1, 8] -t SyN[0.25]  -r Gauss[3,0] -o $SIMMETRIC -i $MAXITERSCC
        JacsServiceData localAlignServiceData = localAlignSubject(resizedTargetNiftiFile, resizedSubjectGlobalAlignedRefChannelNiftiFile, localSymmetricTransformFilePrefix,
                "Local alignment",
                jacsServiceData,
                resizedAlignedSubjectToNiftiServiceData, resizedTargetToNiftiServiceData);
        List<JacsServiceData> warpServices = new ArrayList<>();
        List<Path> warpedFiles = new ArrayList<>();
        for (int channelNo = 0; channelNo < args.input1Channels; channelNo++) {
            Path inputMovingFile = resizedRotatedSubjectGlobalAlignedNiftiFiles.get(channelNo); // => MOVINGNIICI, MOVINGNIICII, MOVINGNIICIII, MOVINGNIICIV
            Path warpedFile = getDeformedNiftiFile(inputMovingFile, jacsServiceData); // => MOVINGDFRMDCI, MOVINGDFRMDCII, MOVINGDFRMDCIII, MOVINGDFRMDCIV
            warpedFiles.add(warpedFile);
            // $WARP 3 $MOVINGNIICI $MOVINGDFRMDCI -R $FIXEDNII $FWDDISPFIELD $AFFINEMATRIXLOCAL --use-BSpline
            JacsServiceData warpServiceData =
                    invocationHelper.warp(inputMovingFile, warpedFile, ImmutableList.of(resizedTargetNiftiFile, localWarpFile, localAffineTransformFile),
                            "-bspline",
                            "Warp local aligned file",
                            jacsServiceData,
                            localAlignServiceData);
            warpServices.add(warpServiceData);
        }
        Path alignedSubjectFile = getAlignedSubjectFile(args); // => SUBSXALINGED
        Path resizedAlignedSubjectFile = getResizedFile(alignedSubjectFile, jacsServiceData); // => SUBSXDFRMD
        // $Vaa3D -x ireg -f NiftiImageConverter -i $MOVINGDFRMDCI $MOVINGDFRMDCII $MOVINGDFRMDCIII $MOVINGDFRMDCIV -o $SUBSXDFRMD -p "#b 1 #v 1"
        JacsServiceData combineChannelsServiceData =
                invocationHelper.convertFromNiftiImage(warpedFiles, resizedAlignedSubjectFile, "#b 1 #v 1",
                        "Recombine all aligned subject channels into one stack",
                        jacsServiceData,
                        warpServices.toArray(new JacsServiceData[warpServices.size()]));
        // resize to the templates' space

        // $Vaa3D -x ireg -f resizeImage -o $SUBSXALINGED -p "#s $SUBSXDFRMD #t $TARSX #y 1"
        invocationHelper.resizeToTarget(resizedAlignedSubjectFile, targetFile, alignedSubjectFile,
                "#y 1",
                "Restore size of the aligned subject",
                jacsServiceData,
                combineChannelsServiceData);

        // warp neurons
        Path neuronsFile = getWorkingFile(args.input1Neurons, jacsServiceData); // => SUBSXNEURONS
        // ensureRawFileWdiffName "$Vaa3D" "$WORKDIR" "$SUBSXNEURONS" "${SUBSXNEURONS%.*}_SX.v3draw" SUBSXNEURONS
        JacsServiceData neuronsToRawServiceData = convertNeuronsFileToRawFormat(Paths.get(args.input1Neurons), neuronsFile,
                "Ensure neurons labels vaa3d file exists",
                jacsServiceData);
        Path yFlippedNeuronsFile = getYFlippedFile(neuronsFile, jacsServiceData); // => NEURONSYFLIP
        // $Vaa3D -x ireg -f yflip -i $SUBSXNEURONS -o $NEURONSYFLIP
        JacsServiceData yFlippedNeuronsServiceData = yFlip(neuronsFile, yFlippedNeuronsFile,
                "Y-flipping the neurons",
                jacsServiceData,
                neuronsToRawServiceData);
        // $Vaa3D -x ireg -f zflip -i ${NEURONSYFLIP} -o ${NEURONSYFLIP}
        Path zFlippedLabelsFile = getZFlippedFile(neuronsFile, jacsServiceData); // => NEURONSYFLIP
        JacsServiceData zFlippedNeuronsServiceData = zFlip(yFlippedNeuronsFile, zFlippedLabelsFile, args.zFlip,
                "Z-flipping the neurons",
                jacsServiceData,
                yFlippedNeuronsServiceData);
        Path isotropicNeuronsFile = getIsotropicFile(zFlippedLabelsFile, jacsServiceData); // => NEURONSYFLIPIS
        //  $Vaa3D -x ireg -f isampler -i $NEURONSYFLIP -o $NEURONSYFLIPIS -p "#x $ISRX #y $ISRY #z $ISRZ #i 1"
        JacsServiceData isotropicNeronsSamplingServiceData =
                invocationHelper.isotropicSampling(zFlippedLabelsFile, alignConfig, inputResolution, isotropicNeuronsFile,
                        "#i 1",
                        "Isotropic sampling the neurons",
                        zFlippedNeuronsServiceData,
                        zFlippedNeuronsServiceData);
        Path resizedNeuronsFile = getResizedFile(zFlippedLabelsFile, jacsServiceData); // => NEURONSYFLIPISRS
        // $Vaa3D -x ireg -f resizeImage -o $NEURONSYFLIPISRS -p "#s $NEURONSYFLIPIS #t $TARSXEXT #k 1 #i 1 #y 1"
        JacsServiceData resizeNeuronsServiceData =
                invocationHelper.resizeToTarget(isotropicNeuronsFile, targetExtFile, resizedNeuronsFile,
                        "#k 1 #i 1 #y 1",
                        "Resizing the neurons to the target",
                        jacsServiceData,
                        isotropicNeronsSamplingServiceData);
        Path rotatedResizedNeuronsFile =  getRotatedFile(resizedNeuronsFile, jacsServiceData); // => NEURONSYFLIPISRSRT
        // $Vaa3D -x ireg -f iwarp2 -o $NEURONSYFLIPISRSRT -p "#s $NEURONSYFLIPISRS #a $RCAFFINE #dx $TARSXEXTDX #dy $TARSXEXTDY #dz $TARSXEXTDZ #i 1"
        JacsServiceData rotateRecenteredNeuronsServiceData =
                invocationHelper.applyIWarp2Transformation(resizedNeuronsFile, rotationsAffineFile, rotatedResizedNeuronsFile,
                        "#i 1",
                        TARSXEXTDX, TARSXEXTDY, TARSXEXTDZ,
                        "Rotating the neurons",
                        jacsServiceData,
                        resizeNeuronsServiceData, rotateSubjectServiceData);
        Path rotatedNeuronsGlobalAllignedFile = getGlobalAlignedFile(rotatedResizedNeuronsFile, jacsServiceData); // => NEURONSYFLIPISRSRTAFF
        // $Vaa3D -x ireg -f iwarp2 -o $NEURONSYFLIPISRSRTAFF -p "#s $NEURONSYFLIPISRSRT #a $AFFINEMATRIX #dx $TARSXEXTDX #dy $TARSXEXTDY #dz $TARSXEXTDZ #i 1"
        JacsServiceData globalAlignedNeuronsServiceData =
                invocationHelper.applyIWarp2Transformation(rotatedResizedNeuronsFile, symmetricAffineTransformFile, rotatedNeuronsGlobalAllignedFile,
                        "#i 1",
                        TARSXEXTDX, TARSXEXTDY, TARSXEXTDZ,
                        "Affine transform rotated neurons",
                        jacsServiceData,
                        rotateRecenteredNeuronsServiceData, globalAlignServiceData);
        Path flippedAlignedNeuronsFile = getAlignedFile(zFlippedLabelsFile, jacsServiceData); // => SXNEURONALIGNEDRS
        Path resizedAlignedNeuronsFile = getResizedFile(flippedAlignedNeuronsFile, jacsServiceData); // => NEURONSYFLIPISRSRTAFFRS
        // $Vaa3D -x ireg -f resizeImage -o $NEURONSYFLIPISRSRTAFFRS -p "#s $NEURONSYFLIPISRSRTAFF #t $TARSXRS #k 1 #i 1 #y 1"
        JacsServiceData restoreSizeAlignedNeuronsServiceData =
                invocationHelper.resizeToTarget(rotatedNeuronsGlobalAllignedFile, targetFile, resizedAlignedNeuronsFile,
                        "#k 1 #i 1 #y 1",
                        "Restore size of the aligned subject",
                        jacsServiceData,
                        globalAlignedNeuronsServiceData);
        Path resizedAlignedNeuronsNiftiFile = getNiftiChannelFile(resizedAlignedNeuronsFile, 0, jacsServiceData); // => NEURONSNII
        // Vaa3D -x ireg -f NiftiImageConverter -i $NEURONSYFLIPISRSRTAFFRS
        JacsServiceData resizedAlignedNeuronsNiftiServiceData =
                invocationHelper.convertToNiftiImage(resizedAlignedNeuronsFile, resizedAlignedNeuronsNiftiFile,
                        "Converting 63x neurons into Nifti",
                        jacsServiceData,
                        restoreSizeAlignedNeuronsServiceData);
        Path warpedNeuronsNiftiFile = getDeformedNiftiFile(resizedAlignedNeuronsNiftiFile, jacsServiceData); // => NEURONDFMD
        // $WARP 3 $NEURONSNII $NEURONDFMD -R $FIXEDNII $FWDDISPFIELD $AFFINEMATRIXLOCAL --use-NN
        JacsServiceData warpNeuronsServiceData =
                invocationHelper.warp(resizedAlignedNeuronsNiftiFile, warpedNeuronsNiftiFile,
                        ImmutableList.of(resizedTargetNiftiFile, localWarpFile, localAffineTransformFile),
                        "--use-NN",
                        "Warp neurons",
                        jacsServiceData,
                        localAlignServiceData, resizedAlignedNeuronsNiftiServiceData);
        Path scaledFlippedNeuronsFile = getScaledFile(flippedAlignedNeuronsFile, jacsServiceData); // => NEURONALIGNEDYFLIP
        // $Vaa3D -x ireg -f NiftiImageConverter -i $NEURONDFMD -o $NEURONALIGNEDYFLIP -p "#b 1 #v 2 #r 0"
        JacsServiceData combineNeuronChannelsServiceData =
                invocationHelper.convertFromNiftiImage(ImmutableList.of(warpedNeuronsNiftiFile), scaledFlippedNeuronsFile, "#b 1 #v 2 #r 0",
                        "Recombine all aligned neuron channels into one stack",
                        jacsServiceData,
                        warpNeuronsServiceData);
        //  $Vaa3D -x ireg -f resizeImage -o $SXNEURONALIGNEDRS -p "#s $NEURONALIGNEDYFLIP #t $TARSX #k 1 #i 1 #y 1"
        JacsServiceData resizeAlignedNeuronsServiceData =
                invocationHelper.resizeToTarget(scaledFlippedNeuronsFile, targetFile, flippedAlignedNeuronsFile,
                        "#k 1 #i 1 #y 1",
                        "Resize the neurons to the template's space",
                        jacsServiceData,
                        combineNeuronChannelsServiceData);
        Path alignedNeuronsFile = getAlignedNeuronsFile(args); // => SXNEURONALIGNED
        // $Vaa3D -x ireg -f yflip -i $SXNEURONALIGNEDRS -o $SXNEURONALIGNED
        JacsServiceData unYFlippedNeuronsServiceData = yFlip(flippedAlignedNeuronsFile, alignedNeuronsFile,
                "Y-Flipping 63x neurons back",
                jacsServiceData,
                resizeAlignedNeuronsServiceData);
        // verify alignment
        Path alignmentQualityFile = getAlignmentQualityFile(args.input1File, jacsServiceData); // => AQ
        // $Vaa3D -x ireg -f esimilarity -o $AQ -p "#s $SUBSXALINGED #cs $SUBSXREF #t $TARSX"
        JacsServiceData evalServiceData = evaluateAlignment(alignedSubjectFile, args.input1Ref, targetFile,
                alignmentQualityFile,
                "Verify alignment",
                jacsServiceData,
                unYFlippedNeuronsServiceData);

        // create verification movie
        Path verifyMovie = getAlignmentVerificationFile(args);
        // createVerificationMovie.sh -c $CONFIGFILE -k $TOOLDIR -w $WORKDIR -s $SUBSXALINGED -i $TARSX -r $SUBSXREF -o ${FINALOUTPUT}/$ALIGNVERIFY
        createVerificationMovie(alignedSubjectFile,
                targetFile,
                args.input1Ref,
                verifyMovie,
                "Create verification movie",
                jacsServiceData,
                evalServiceData);

        return new JacsServiceResult<>(jacsServiceData);
    }

    private void createWorkingCopy(Path inputFile, Path outputFile) {
        try {
            Path outputDir = outputFile.getParent();
            Files.createDirectories(outputDir);
            Files.createSymbolicLink(outputFile, inputFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JacsServiceData convertNeuronsFileToRawFormat(Path inputFile, Path outputFile,
                                                          String description,
                                                          JacsServiceData jacsServiceData) {
        if ("v3draw".equals(com.google.common.io.Files.getFileExtension(inputFile.toString()))) {
            return invocationHelper.linkData(inputFile, outputFile, description, jacsServiceData);
        } else {
            return invocationHelper.convertFile(inputFile, outputFile, description, jacsServiceData);
        }
    }

    private JacsServiceData yFlip(Path inputFile, Path outputFile, String description,
                                  JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Flip {} along the y axis", inputFile);
        if (outputFile.toFile().exists()) {
            return null;
        }
        JacsServiceData yFlipSubjectsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "yflip"),
                new ServiceArg("-input", inputFile.toString()),
                new ServiceArg("-output", outputFile.toString())
        );
        return submitDependencyIfNotPresent(jacsServiceData, yFlipSubjectsServiceData);
    }

    private JacsServiceData zFlip(Path inputFile, Path outputFile, boolean zFlip,
                                  String description,
                                  JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData zFlipSubjectsServiceData;
        if (zFlip) {
            logger.info("Flip {} along the z axis", inputFile);
            zFlipSubjectsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .description(description)
                            .waitFor(deps)
                            .build(),
                    new ServiceArg("-plugin", "ireg"),
                    new ServiceArg("-pluginFunc", "zflip"),
                    new ServiceArg("-input", inputFile.toString()),
                    new ServiceArg("-output", outputFile.toString())
            );
        } else {
            zFlipSubjectsServiceData = linkDataProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                            .description(description)
                            .waitFor(deps)
                            .build(),
                    new ServiceArg("-source", inputFile.toString()),
                    new ServiceArg("-target", outputFile.toString())
            );
        }
        return submitDependencyIfNotPresent(jacsServiceData, zFlipSubjectsServiceData);
    }

    private JacsServiceData findRotationMatrix(Path subjectFile, Path targetFile, Path rotationsMatFile, String fslOutputType,
                                               String description,
                                               JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Find rotations {}", rotationsMatFile);
        JacsServiceData rotateServiceData = flirtProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-in", subjectFile.toString()),
                new ServiceArg("-ref", targetFile.toString()),
                new ServiceArg("-omat", rotationsMatFile.toString()),
                new ServiceArg("-cost", "mutualinfo"),
                new ServiceArg("-searchrx", "-180", "180"),
                new ServiceArg("-searchry", "-180", "180"),
                new ServiceArg("-searchrz", "-180", "180"),
                new ServiceArg("-dof", "12"),
                new ServiceArg("-datatype", "char"),
                new ServiceArg("-fslOutputType", fslOutputType)
        );
        return submitDependencyIfNotPresent(jacsServiceData, rotateServiceData);
    }

    private JacsServiceData convertAffineToInsight(Path rotationsMatFile, Path insightRotationsFile,
                                                   String description,
                                                   JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData affineToInsightServiceData = affineToInsightConverterProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-input", rotationsMatFile.toString()),
                new ServiceArg("-output", insightRotationsFile.toString())
        );
        submitDependencyIfNotPresent(jacsServiceData, affineToInsightServiceData);
        return affineToInsightServiceData;
    }

    private JacsServiceData prepareAffineTransformation(Path insightRotationsFile, Path rotationsAffineFile,
                                                        String description,
                                                        JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData estimateRotationsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "extractRotMat"),
                new ServiceArg("-input", insightRotationsFile.toString()),
                new ServiceArg("-output", rotationsAffineFile.toString())
        );
        return submitDependencyIfNotPresent(jacsServiceData, estimateRotationsServiceData);
    }

    private JacsServiceData rotateSubject(Path subjectFile, Path targetExtFile, Path transformationsFile, Path rotatedSubjectFile,
                                          String description,
                                          JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData estimateRotationsServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "iwarp"),
                new ServiceArg("-output", rotatedSubjectFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetExtFile)),
                new ServiceArg("-pluginParams", String.format("#a %s", transformationsFile))
        );
        return submitDependencyIfNotPresent(jacsServiceData, estimateRotationsServiceData);
    }

    private JacsServiceData globalAlignSubjectToTarget(Path targetNiftiFile, Path subjectNiftiFile, Path symmetricTransformFile,
                                                       String description,
                                                       JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Align subject to target");
        JacsServiceData alignSubjectToTargetServiceData = antsToolProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-dims", "3"),
                new ServiceArg("-metric",
                        String.format("MI[%s, %s, %d, %d]",
                                targetNiftiFile,
                                subjectNiftiFile,
                                1,
                                32)),
                new ServiceArg("-output", symmetricTransformFile.toString()),
                new ServiceArg("-iterations", "0"),
                new ServiceArg("-affineIterations", MAX_AFFINE_ITERATIONS)
        );
        return submitDependencyIfNotPresent(jacsServiceData, alignSubjectToTargetServiceData);
    }

    private JacsServiceData getVOI(Path subjectFile, Path targetFile,
                                   String description,
                                   JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Resize subject {} to original target {}", subjectFile, targetFile);
        JacsServiceData resizeSubjectServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "genVOIs"),
                new ServiceArg("-pluginParams", String.format("#s %s", subjectFile)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetFile))
        );
        return submitDependencyIfNotPresent(jacsServiceData, resizeSubjectServiceData);
    }

    private JacsServiceData localAlignSubject(Path targetNiftiFile, Path refChannelSubjectNiftiFile, Path symmetricTransformFile,
                                              String description,
                                              JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Align subject to target");
        JacsServiceData alignSubjectToTargetServiceData = antsToolProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-dims", "3"),
                new ServiceArg("-metric",
                        String.format("CC[%s, %s, %d, %d]",
                                targetNiftiFile,
                                refChannelSubjectNiftiFile,
                                1,
                                8)),
                new ServiceArg("-transformationModel", String.format("SyN[%f]", 0.25)),
                new ServiceArg("-roi", String.format("Gauss[%d,%d]", 3, 0)),
                new ServiceArg("-output", symmetricTransformFile.toString()),
                new ServiceArg("-iterations", MAX_CC_ITERATIONS)
        );
        return submitDependencyIfNotPresent(jacsServiceData, alignSubjectToTargetServiceData);
    }

    private JacsServiceData evaluateAlignment(Path alignedFile, int referenceChannel, Path targetFile,
                                              Path outputFile,
                                              String description,
                                              JacsServiceData jacsServiceData, JacsServiceData... deps) {
        logger.info("Evaluate alignment of {}, {}, {} => {}", alignedFile, referenceChannel, targetFile, outputFile);
        JacsServiceData evalServiceData = vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-plugin", "ireg"),
                new ServiceArg("-pluginFunc", "esimilarity"),
                new ServiceArg("-output", outputFile.toString()),
                new ServiceArg("-pluginParams", String.format("#s %s", alignedFile)),
                new ServiceArg("-pluginParams", String.format("#cs %d", referenceChannel)),
                new ServiceArg("-pluginParams", String.format("#t %s", targetFile))
        );
        return submitDependencyIfNotPresent(jacsServiceData, evalServiceData);
    }

    private JacsServiceData createVerificationMovie(Path subjectFile,
                                                    Path targetFile,
                                                    int referenceChannel,
                                                    Path outputFile,
                                                    String description,
                                                    JacsServiceData jacsServiceData, JacsServiceData... deps) {
        JacsServiceData movieServiceData = alignmentVerificationMovieProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .description(description)
                        .waitFor(deps)
                        .build(),
                new ServiceArg("-subject", subjectFile.toString()),
                new ServiceArg("-target", targetFile.toString()),
                new ServiceArg("-reference", String.valueOf(referenceChannel)),
                new ServiceArg("-output", outputFile.toString())
        );
        return submitDependencyIfNotPresent(jacsServiceData, movieServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        // generate metadata
        JacsServiceData jacsServiceData = depResults.getJacsServiceData();
        AlignmentArgs args = getArgs(jacsServiceData);
        AlignmentConfiguration alignConfig = AlignmentUtils.parseAlignConfig(args.configFile);

        Path alignedSubjectFile = getAlignedSubjectFile(args); // => SUBSXALINGED
        Path alignmentVerificationFile = getAlignmentVerificationFile(args); // => ALIGNVERIFY
        Path alignmentDescriptor = getAlignmentResultsDescriptor(args); // => META

        Path alignmentQualityFile = getAlignmentQualityFile(args.input1File, jacsServiceData); // => AQ
        Path alignedNeuronsFile = getAlignedNeuronsFile(args); // => SXNEURONALIGNED

        try {
            List<String> alignmentQualityContent = Files.readAllLines(alignmentQualityFile);
            String score = alignmentQualityContent.stream().filter(StringUtils::isNotBlank).findFirst().orElse("");

            Files.write(alignmentDescriptor,
                    ImmutableList.<String>builder()
                            .add(String.format("alignment.stack.filename=%s", alignedSubjectFile))
                            .add(String.format("alignment.image.channels=%d", args.input1Channels))
                            .add(String.format("alignment.image.refchan=%d", args.input1Ref))
                            .add(String.format("alignment.verify.filename=%s", alignmentVerificationFile))
                            .add(String.format("alignment.space.name=%s", args.alignmentSpace))
                            .add(String.format("alignment.resolution.voxels=%fx%fx%f",
                                    alignConfig.misc.vSzIsX63x,
                                    alignConfig.misc.vSzIsY63x,
                                    alignConfig.misc.vSzIsZ63x))
                            .add(String.format("alignment.image.size=%dx%dx%d",
                                    IMAGE_SIZE_DX,
                                    IMAGE_SIZE_DY,
                                    IMAGE_SIZE_DZ))
                            .add("alignment.bounding.box=")
                            .add("alignment.objective=63x")
                            .add(String.format("alignment.quality.score.ncc=%s", score))
                            .add(String.format("neuron.masks.filename=%s", alignedNeuronsFile))
                            .add("default=true")
                            .build()
            );

            // copy the transformations
            Path subjectFile = Paths.get(args.input1File);
            Path workingCCMIPrefix = getLocalSymmetricTransformFilePrefix(subjectFile, jacsServiceData);
            Files.copy(
                    getAffineRotationsMatrixFile(subjectFile, jacsServiceData),
                    FileUtils.getFilePath(getTransformationsResultsDir(args), "rotationsAffine.txt"));
            Files.copy(
                    getGlobalSymmetricTransformFilePrefix(subjectFile, jacsServiceData),
                    FileUtils.getFilePath(getTransformationsResultsDir(args), "txmiAffine.txt"));
            Files.copy(
                    getWarpTransformFile(workingCCMIPrefix, jacsServiceData),
                    FileUtils.getFilePath(getTransformationsResultsDir(args), "ccmiWarp.nii.gz"));
            Files.copy(
                    getInverseWarpTransformFile(workingCCMIPrefix, jacsServiceData),
                    FileUtils.getFilePath(getTransformationsResultsDir(args), "ccmiInverseWarp.nii.gz"));
            Files.copy(
                    getAffineTransformFile(workingCCMIPrefix, jacsServiceData),
                    FileUtils.getFilePath(getTransformationsResultsDir(args), "ccmiAffine.txt"));

            return computationFactory.newCompletedComputation(depResults);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private AlignmentArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentArgs.parse(jacsServiceData.getArgsArray(), new AlignmentArgs());
    }

    private Path getResultsDir(AlignmentArgs args) {
        return Paths.get(args.resultsDir, com.google.common.io.Files.getNameWithoutExtension(args.input1File));
    }

    private Path getBrainResultsDir(AlignmentArgs args) {
        return FileUtils.getFilePath(getResultsDir(args), "Brains");
    }

    private Path getNeuronsResultsDir(AlignmentArgs args) {
        return FileUtils.getFilePath(getResultsDir(args), "Neurons");
    }

    private Path getTransformationsResultsDir(AlignmentArgs args) {
        return FileUtils.getFilePath(getResultsDir(args), "Transformations");
    }

    private Path getAlignmentVerificationFile(AlignmentArgs args) {
        return FileUtils.getFilePath(getResultsDir(args), "VerifyMovie.mp4");
    }

    private Path getAlignedSubjectFile(AlignmentArgs args) {
        return FileUtils.getFilePath(getBrainResultsDir(args), "Aligned63xScale.v3draw");
    }

    private Path getAlignedNeuronsFile(AlignmentArgs args) {
        return FileUtils.getFilePath(getNeuronsResultsDir(args), "NeuronAligned63xScale.v3draw");
    }

    private Path getAlignmentResultsDescriptor(AlignmentArgs args) {
        return FileUtils.getFilePath(getBrainResultsDir(args), "Aligned63xScale.properties");
    }

    private Path getWorkingFile(String inputFileName, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(inputFileName) + ".v3draw");
    }

    private List<Path> getNiftiChannelsFiles(Path fp, int nchannels, JacsServiceData jacsServiceData) {
        return IntStream
                .range(0, nchannels)
                .mapToObj(channelNo -> getNiftiChannelFile(fp, channelNo, jacsServiceData))
                .collect(Collectors.toList());
    }

    private Path getNiftiChannelFile(Path fp, int channelNo, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) +String.format("_c%d.nii", channelNo));
    }

    private Path getNiftiDownsampledFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_ds.nii");
    }

    private Path getIsotropicFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_is.v3draw");
    }

    private Path getResizedFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_rs.v3draw");
    }

    private Path getScaledFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_scaled.v3draw");
    }

    private Path getRotatedFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_rotated.v3draw");
    }

    private Path getRefChannelFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_refChn.v3draw");
    }

    private Path getRotationsMatrixFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_rotations.mat");
    }

    private Path getInsightRotationsMatrixFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_rotations.txt");
    }

    private Path getAffineRotationsMatrixFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_rotationsAffine.txt");
    }

    private Path getGlobalAlignedFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_globalAligned.v3draw");
    }

    private Path getGlobalSymmetricTransformFilePrefix(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_txmi");
    }

    private Path getLocalSymmetricTransformFilePrefix(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_ccmi");
    }

    private Path getAffineTransformFile(Path prefix, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(prefix.toString()) + "Affine.txt");
    }

    private Path getWarpTransformFile(Path prefix, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(prefix.toString()) + "Warp.nii.gz");
    }

    private Path getInverseWarpTransformFile(Path prefix, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(prefix.toString()) + "InverseWarp.nii.gz");
    }

    private Path getAlignedFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_aligned.v3draw");
    }

    private Path getDeformedNiftiFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_deformed.nii");
    }

    private Path getYFlippedFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_yflip.v3draw");
    }

    private Path getZFlippedFile(Path fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp.toString()) + "_zflip.v3draw");
    }

    private Path getAlignmentQualityFile(String fp, JacsServiceData jacsServiceData) {
        return Paths.get(getWorkingDirectory(jacsServiceData).toString(), com.google.common.io.Files.getNameWithoutExtension(fp) + "_AlignmentQuality.txt");
    }

}
