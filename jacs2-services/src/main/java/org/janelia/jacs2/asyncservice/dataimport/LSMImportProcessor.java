package org.janelia.jacs2.asyncservice.dataimport;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.WrappedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractAnyServiceResultHandler;
import org.janelia.jacs2.asyncservice.imageservices.tools.LSMProcessingTools;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.dataset.DatasetService;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.dataservice.sample.SageDataService;
import org.janelia.jacs2.dataservice.sample.SampleDataService;
import org.janelia.model.jacs2.DataInterval;
import org.janelia.model.jacs2.DomainModelUtils;
import org.janelia.model.jacs2.EntityFieldValueHandler;
import org.janelia.model.jacs2.SampleUtils;
import org.janelia.model.jacs2.SetFieldValueHandler;
import org.janelia.model.jacs2.domain.Reference;
import org.janelia.model.jacs2.domain.sample.DataSet;
import org.janelia.model.jacs2.domain.sample.LSMImage;
import org.janelia.model.jacs2.domain.sample.ObjectiveSample;
import org.janelia.model.jacs2.domain.sample.Sample;
import org.janelia.model.jacs2.domain.sample.SampleProcessingStatus;
import org.janelia.model.jacs2.domain.sample.SampleTile;
import org.janelia.model.jacs2.domain.sample.SampleTileKey;
import org.janelia.model.jacs2.page.PageRequest;
import org.janelia.model.jacs2.page.PageResult;
import org.janelia.model.jacs2.page.SortCriteria;
import org.janelia.model.jacs2.page.SortDirection;
import org.janelia.model.jacs2.sage.ImageLine;
import org.janelia.model.jacs2.sage.SlideImage;
import org.janelia.model.jacs2.sage.SlideImageGroup;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Named("lsmImport")
public class LSMImportProcessor extends AbstractServiceProcessor<List<LSMImportResult>> {
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-{Slide Code}";

    static class LSMImportArgs extends ServiceArgs {
        @Parameter(names = "-sageUser", description = "Sage loader user", required = false)
        String sageUser = "jacs";
        @Parameter(names = "-dataset", description = "Data set name or identifier", required = false)
        String dataset;
        @Parameter(names = "-imageLine", description = "Image line name", required = false)
        String imageLine;
        @Parameter(names = "-slideCodes", description = "Slide codes", required = false)
        List<String> slideCodes = new ArrayList<>();
        @Parameter(names = "-lsmNames", description = "LSM names", required = false)
        List<String> lsmNames = new ArrayList<>();
        @Parameter(names = "-debug", description = "Debug flag", required = false)
        boolean debugFlag;

        List<String> getValues(List<String> paramValues) {
            return paramValues.stream().filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
        }

        boolean hasOnlyLsmNamesFilter() {
            // check if the only filter(s) set are the lsmNames and maybe the dataset
            return StringUtils.isBlank(imageLine) && CollectionUtils.isEmpty(getValues(slideCodes)) &&
                    CollectionUtils.isNotEmpty(getValues(lsmNames));
        }
    }

    private final WrappedServiceProcessor<SageLoaderProcessor, Void> sageLoaderProcessor;
    private final SampleDataService sampleDataService;
    private final DatasetService datasetService;
    private final SageDataService sageDataService;

    @Inject
    LSMImportProcessor(ServiceComputationFactory computationFactory,
                       JacsServiceDataPersistence jacsServiceDataPersistence,
                       @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                       SageLoaderProcessor sageLoaderProcessor,
                       SampleDataService sampleDataService,
                       DatasetService datasetService,
                       SageDataService sageDataService,
                       Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.sageLoaderProcessor = new WrappedServiceProcessor<>(computationFactory, jacsServiceDataPersistence, sageLoaderProcessor);
        this.sampleDataService = sampleDataService;
        this.datasetService = datasetService;
        this.sageDataService = sageDataService;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(LSMImportProcessor.class, new LSMImportArgs());
    }

    @Override
    public ServiceResultHandler<List<LSMImportResult>> getResultHandler() {
        return new AbstractAnyServiceResultHandler<List<LSMImportResult>>() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return areAllDependenciesDone(depResults.getJacsServiceData());
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<LSMImportResult> collectResult(JacsServiceResult<?> depResults) {
                JacsServiceResult<List<LSMImportResult>> intermediateResult = (JacsServiceResult<List<LSMImportResult>>)depResults;
                return intermediateResult.getResult();
            }

            @Override
            public List<LSMImportResult> getServiceDataResult(JacsServiceData jacsServiceData) {
                return ServiceDataUtils.serializableObjectToAny(jacsServiceData.getSerializableResult(), new TypeReference<List<LSMImportResult>>() {});
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceComputation<JacsServiceResult<List<LSMImportResult>>> process(JacsServiceData jacsServiceData) {
        LSMImportArgs args = getArgs(jacsServiceData);
        if (StringUtils.isBlank(args.dataset) &&
                StringUtils.isBlank(args.imageLine) &&
                CollectionUtils.isEmpty(args.getValues(args.slideCodes)) &&
                CollectionUtils.isEmpty(args.getValues(args.lsmNames))) {
            throw new IllegalArgumentException("No filtering parameter has been specified for the LSM import from Sage.");
        }
        List<SlideImage> slideImages = retrieveSageImages(jacsServiceData.getOwnerKey(), args);
        checkRetrievedSageImages(slideImages, args);

        Map<ImageLine, List<SlideImage>> labImages =
                slideImages.stream().collect(Collectors.groupingBy(
                                si -> new ImageLine(si.getLab(), si.getDataset(), si.getLineName()),
                                Collectors.mapping(Function.identity(), Collectors.toList()))
                );
        List<ServiceComputation<List<LSMImportResult>>> lsmImportComputations = labImages.entrySet().stream()
                .map(lineEntries -> {
                    ImageLine imageLine = lineEntries.getKey();
                    List<SlideImage> lineImages = lineEntries.getValue();
                    List<String> slideImageNames = lineImages.stream().map(SlideImage::getName).collect(Collectors.toList());
                    Map<String, List<SlideImage>> slideImagesToImportBySlideCode =
                            lineImages.stream().collect(
                                    Collectors.groupingBy(
                                            lineImage -> lineImage.getSlideCode(),
                                            Collectors.mapping(Function.identity(), Collectors.toList())
                                    )
                            );
                    String ownerKey = jacsServiceData.getOwnerKey();
                    DataSet ds = datasetService.getDatasetByNameOrIdentifier(ownerKey, imageLine.getDataset());
                    if (ds == null) {
                        logger.error("No dataset record found for {} : {}", ownerKey, lineEntries.getKey());
                        throw new IllegalArgumentException("Invalid dataset identifier " + ownerKey + ":" + imageLine.getDataset());
                    }
                    return sageLoaderProcessor.process(
                            new ServiceExecutionContext.Builder(jacsServiceData)
                                    .build(),
                            new ServiceArg("-sageUser", args.sageUser),
                            new ServiceArg("-lab", imageLine.getLab()),
                            new ServiceArg("-line", imageLine.getName()),
                            new ServiceArg("-configFile", ds.getSageConfigPath()),
                            new ServiceArg("-grammarFile", ds.getSageGrammarPath()),
                            new ServiceArg("-sampleFiles", String.join(",", slideImageNames)),
                            new ServiceArg("-debug", args.debugFlag)
                    ).thenApply(vr -> lineImages.stream()
                                    .map(SampleUtils::createLSMFromSlideImage)
                                    .map(lsm -> {
                                        lsm.setOwnerKey(ds.getOwnerKey());
                                        lsm.setWriters(ds.getWriters());
                                        lsm.setReaders(ds.getReaders());
                                        return lsm;
                                    })
                                    .map(lsm -> importLsm(ownerKey, lsm))
                                    .map(lsm -> lsm.getSlideCode())
                                    .collect(Collectors.toSet())
                    ).thenApply(slideCodes -> {
                        Map<String, List<LSMImage>> allLsmsPerSlideCode = new LinkedHashMap<>();
                        slideCodes.stream().forEach(slideCode -> {
                            LSMImage lsmRef = new LSMImage();
                            lsmRef.setDataSet(ds.getIdentifier());
                            lsmRef.setLine(imageLine.getName());
                            lsmRef.setSlideCode(slideCode);
                            PageRequest pageRequest = new PageRequest();
                            pageRequest.setSortCriteria(ImmutableList.of(new SortCriteria("tmogDate", SortDirection.ASC)));
                            PageResult<LSMImage> matchingLsms = sampleDataService.searchLsms(ownerKey, lsmRef, pageRequest);
                            allLsmsPerSlideCode.put(slideCode, matchingLsms.getResultList());
                        });
                        return allLsmsPerSlideCode;
                    }).thenApply((Map<String, List<LSMImage>> allLsmsForEnteredSlideCodes) -> createSamplesFromSlideGroups(ds, imageLine.getName(), slideImagesToImportBySlideCode, allLsmsForEnteredSlideCodes));
                })
                .collect(Collectors.toList());
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(ImmutableList.copyOf(lsmImportComputations), (sd, listOflsmImportResults) -> this.updateServiceResult(
                        sd,
                        ((List<List<LSMImportResult>>) listOflsmImportResults).stream()
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()))
                );
    }

    private LSMImportArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(getJacsServiceArgsArray(jacsServiceData), new LSMImportArgs());
    }

    private List<SlideImage> retrieveSageImages(String subject, LSMImportArgs args) {
        String datasetIdentifer = null;
        if (StringUtils.isNotBlank(args.dataset)) {
            DataSet ds = datasetService.getDatasetByNameOrIdentifier(subject, args.dataset);
            if (ds == null) {
                throw new IllegalArgumentException ("Invalid dataset: " + args.dataset);
            }
            datasetIdentifer = ds.getIdentifier();
        }
        return sageDataService.getMatchingImages(datasetIdentifer, args.imageLine, args.getValues(args.slideCodes), args.getValues(args.lsmNames), new PageRequest());
    }

    private void checkRetrievedSageImages(List<SlideImage> slideImages, LSMImportArgs args) {
        if (slideImages.isEmpty()) {
            throw new IllegalArgumentException("No image found in sage to match " + args.toString());
        }
        if (args.hasOnlyLsmNamesFilter() && slideImages.size() != args.getValues(args.lsmNames).size()) {
            Set<String> requestedLsms = ImmutableSet.copyOf(args.getValues(args.lsmNames));
            Set<String> foundLsms = slideImages.stream().map(si -> si.getName()).collect(Collectors.toSet());

            // if the size of the given parameters doesn't match
            if (requestedLsms.size() != foundLsms.size()) {
                logger.error("Requested {} lsms ({}) but found {} sage images ({})", requestedLsms.size(), requestedLsms, foundLsms.size(), foundLsms);
                throw new IllegalArgumentException("Not all requested LSMs were found - " + Sets.difference(requestedLsms, foundLsms).immutableCopy() + " were not found");
            }
        }
    }

    private LSMImage importLsm(String ownerKey, LSMImage lsmImage) {
        PageResult<LSMImage> matchingLsms = sampleDataService.searchLsms(ownerKey, lsmImage, new PageRequest());
        if (matchingLsms.isEmpty()) {
            sampleDataService.createLSM(lsmImage);
            return lsmImage;
        } else if (matchingLsms.getResultList().size() == 1) {
            LSMImage existingLsm = matchingLsms.getResultList().get(0);
            sampleDataService.updateLSM(existingLsm, SampleUtils.updateLsmAttributes(lsmImage, existingLsm));
            return existingLsm;
        } else {
            // there is a potential clash or duplication here
            logger.warn("Multiple candidates found for {}", lsmImage);
            throw new IllegalArgumentException("Import LSM failed due to too many candidates to be merged for " + lsmImage);
        }
    }

    private List<LSMImportResult> createSamplesFromSlideGroups(DataSet dataSet, String line, Map<String, List<SlideImage>> imagesToImportBySlideCode, Map<String, List<LSMImage>> allLsmsForEnteredSlideCodes) {
        return allLsmsForEnteredSlideCodes.entrySet().stream()
                .map(slideLsmsEntry -> createSampleFromSlideGroup(dataSet, line, slideLsmsEntry.getKey(), imagesToImportBySlideCode.get(slideLsmsEntry.getKey()), slideLsmsEntry.getValue()))
                .collect(Collectors.toList());
    }

    private LSMImportResult createSampleFromSlideGroup(DataSet dataSet, String line, String slideCode, List<SlideImage> importedImages, List<LSMImage> lsmImages) {
        List<String> importedImageNames = importedImages.stream().map(image -> image.getName()).collect(Collectors.toList());
        logger.info("Creating or updating sample {} : {} : {} with {}", dataSet.getIdentifier(), line, slideCode, importedImageNames);
        Map<String, Map<SampleTileKey, SlideImageGroup>> lsmsGroupedByAbjectiveAndArea = groupLsmImagesByObjectiveAndArea(lsmImages);
        Optional<Sample> existingSample = findBestSampleMatch(dataSet, slideCode);
        if (!existingSample.isPresent()) {
            // create a new Sample
            Sample newSample = createNewSample(dataSet, slideCode, lsmsGroupedByAbjectiveAndArea);
            logger.info("Created new sample {} for dataset {} and slideCode {}", newSample, dataSet, slideCode);
            return new LSMImportResult(dataSet.getIdentifier(), newSample.getId(), newSample.getName(), line, slideCode, importedImageNames, true);
        } else {
            Sample updatedSample = existingSample.get();
            updateSample(dataSet, updatedSample, lsmsGroupedByAbjectiveAndArea);
            logger.info("Updated sample {} for dataset {} and slideCode {} with {}", updatedSample, dataSet, slideCode, lsmImages);
            return new LSMImportResult(dataSet.getIdentifier(), updatedSample.getId(), updatedSample.getName(), line, slideCode, importedImageNames, false);
        }
    }

    private String getSampleNamePattern(DataSet dataSet) {
        String sampleNamePattern = dataSet==null ? null : dataSet.getSampleNamePattern();
        return sampleNamePattern == null ? DEFAULT_SAMPLE_NAME_PATTERN : sampleNamePattern;
    }

    private Sample createNewSample(DataSet dataSet, String slideCode, Map<String, Map<SampleTileKey, SlideImageGroup>> lsmsGroupedByAbjectiveAndArea) {
        Sample newSample = new Sample();
        newSample.setOwnerKey(dataSet.getOwnerKey());
        newSample.setWriters(dataSet.getWriters());
        newSample.setReaders(dataSet.getReaders());
        newSample.setDataSet(dataSet.getIdentifier());
        newSample.setSlideCode(slideCode);
        newSample.setStatus(SampleProcessingStatus.New.name());
        SampleUtils.updateSampleAttributes(
                newSample,
                lsmsGroupedByAbjectiveAndArea
                        .values()
                        .stream()
                        .flatMap((Map<SampleTileKey, SlideImageGroup> area) -> area.values().stream())
                        .collect(Collectors.toList())
        );
        Map<LSMImage, Map<String, EntityFieldValueHandler<?>>> lsmUpdates = new LinkedHashMap<>();
        Map<String, ObjectiveSample> newSampleObjectives = new LinkedHashMap<>();
        lsmsGroupedByAbjectiveAndArea.forEach((objective, areas) -> {
            ObjectiveSample objectiveSample = createNewObjective(objective, areas, lsmUpdates);
            newSample.addObjective(objectiveSample);
            newSampleObjectives.put(objective, objectiveSample);
        });
        newSample.setName(DomainModelUtils.replaceVariables(getSampleNamePattern(dataSet), DomainModelUtils.getFieldValues(newSample)));
        sampleDataService.createSample(newSample);
        Reference newSampleRef = Reference.createFor(newSample);

        // update LSM's sampleRef
        lsmsGroupedByAbjectiveAndArea.forEach((objective, areas) -> {
            areas.forEach((SampleTileKey tileKey, SlideImageGroup areaGroup) -> {
                areaGroup.getImages().forEach((LSMImage lsm) -> {
                    Map<String, EntityFieldValueHandler<?>> updatedLsmFields = lsmUpdates.get(lsm);
                    if (updatedLsmFields == null) {
                        updatedLsmFields = new LinkedHashMap<>();
                        lsmUpdates.put(lsm, updatedLsmFields);
                    }
                    lsm.setSampleRef(newSampleRef);
                    updatedLsmFields.put("sampleRef", new SetFieldValueHandler<>(newSampleRef));
                });
            });
        });
        lsmUpdates.forEach((lsm, updates) -> sampleDataService.updateLSM(lsm, updates));
        return newSample;
    }

    private void updateSample(DataSet dataSet, Sample sample, Map<String, Map<SampleTileKey, SlideImageGroup>> lsmsGroupedByAbjectiveAndArea) {
        Map<String, EntityFieldValueHandler<?>> updatedSampleFields = SampleUtils.updateSampleAttributes(
                sample,
                lsmsGroupedByAbjectiveAndArea
                        .values()
                        .stream()
                        .flatMap((Map<SampleTileKey, SlideImageGroup> area) -> area.values().stream())
                        .collect(Collectors.toList())
        );
        Map<LSMImage, Map<String, EntityFieldValueHandler<?>>> lsmUpdates = new LinkedHashMap<>();
        lsmsGroupedByAbjectiveAndArea.forEach((String objective, Map<SampleTileKey, SlideImageGroup> sampleTileGroups) -> {
            sample.lookupObjective(objective)
                    .map(existingObjectiveSample -> {
                        sampleTileGroups.forEach((tileKey, tileGroup) -> {
                            existingObjectiveSample.findSampleTile(tileKey.getTileName(), tileKey.getArea())
                                    .map(indexedSampleTile -> {
                                        SampleTile existingSampleTile = indexedSampleTile.getReference();
                                        // for existing tiles check if all lsms are present
                                        tileGroup.getImages().forEach(lsm -> {
                                            existingSampleTile.findLsmReference(lsm)
                                                    .orElseGet(() -> {
                                                        // if lsm reference not found make sure we update the lsm tile name
                                                        Reference tileLsmReference = Reference.createFor(lsm);
                                                        updateLsmTileName(lsm, existingSampleTile.getName(), lsmUpdates);
                                                        existingSampleTile.addLsmReference(tileLsmReference);
                                                        return tileLsmReference;
                                                    });
                                        });
                                        int sampleNumSignals = tileGroup.countTileSignalChannels();
                                        int sampleNumChannels = sampleNumSignals + 1;
                                        if (StringUtils.isBlank(existingObjectiveSample.getChanSpec()) || sampleNumChannels > existingObjectiveSample.getChanSpec().length()) {
                                            String chanSpec = LSMProcessingTools.createChanSpec(sampleNumChannels, sampleNumChannels);
                                            existingObjectiveSample.setChanSpec(chanSpec);
                                        }
                                        return existingSampleTile;
                                    })
                                    .orElseGet(() -> {
                                        SampleTile newSampleTile = createNewSampleTile(tileKey, tileGroup, lsmUpdates);
                                        existingObjectiveSample.addTiles(newSampleTile);
                                        int sampleNumSignals = tileGroup.countTileSignalChannels();
                                        int sampleNumChannels = sampleNumSignals + 1;
                                        if (StringUtils.isBlank(existingObjectiveSample.getChanSpec()) || sampleNumChannels > existingObjectiveSample.getChanSpec().length()) {
                                            String chanSpec = LSMProcessingTools.createChanSpec(sampleNumChannels, sampleNumChannels);
                                            existingObjectiveSample.setChanSpec(chanSpec);
                                        }
                                        return newSampleTile;
                                    });
                        });
                        return existingObjectiveSample;
                    })
                    .orElseGet(() -> {
                        ObjectiveSample newObjectiveSample = createNewObjective(objective, sampleTileGroups, lsmUpdates);
                        sample.addObjective(newObjectiveSample);
                        return newObjectiveSample;
                    });
        });
        // persist Sample updates
        updatedSampleFields.put("objectiveSamples", new SetFieldValueHandler<>(sample.getObjectiveSamples()));
        String sampleNameAfterUpdates = DomainModelUtils.replaceVariables(getSampleNamePattern(dataSet), DomainModelUtils.getFieldValues(sample));
        if (!StringUtils.equals(sampleNameAfterUpdates, sample.getName())) {
            sample.setName(sampleNameAfterUpdates);
            updatedSampleFields.put("name", new SetFieldValueHandler<>(sampleNameAfterUpdates));
        }
        sampleDataService.updateSample(sample, updatedSampleFields);
        // persist LSM updates
        lsmUpdates.forEach((lsm, updates) -> sampleDataService.updateLSM(lsm, updates));
    }

    private ObjectiveSample createNewObjective(String objectiveName, Map<SampleTileKey, SlideImageGroup> objectiveTileGroups, Map<LSMImage, Map<String, EntityFieldValueHandler<?>>> lsmUpdates) {
        ObjectiveSample objectiveSample = new ObjectiveSample();
        objectiveSample.setObjective(objectiveName);
        int sampleNumSignals = objectiveTileGroups.entrySet().stream()
                .map((Map.Entry<SampleTileKey, SlideImageGroup> tileEntry) -> {
                    SampleTileKey tileKey = tileEntry.getKey();
                    SlideImageGroup tileGroup = tileEntry.getValue();
                    SampleTile tile = createNewSampleTile(tileKey, tileGroup, lsmUpdates);
                    objectiveSample.addTiles(tile);
                    return tileGroup.countTileSignalChannels();
                })
                .reduce(0, (n1, n2) -> {
                    if (n1 == 0) {
                        return n2;
                    } else {
                        if (n1 != n2 && n2 != 0) {
                            logger.warn("There is no consensus between tiles regarding the number of signal channels {} vs {}",
                                    n1, n2);
                        }
                        return n1;
                    }
                });
        if (sampleNumSignals > 0) {
            int sampleNumChannels = sampleNumSignals + 1;
            String chanSpec = LSMProcessingTools.createChanSpec(sampleNumChannels, sampleNumChannels);
            objectiveSample.setChanSpec(chanSpec);
        }
        return objectiveSample;
    }

    private SampleTile createNewSampleTile(SampleTileKey tileKey, SlideImageGroup areaGroup, Map<LSMImage, Map<String, EntityFieldValueHandler<?>>> lsmUpdates) {
        SampleTile tile = new SampleTile();
        tile.setAnatomicalArea(tileKey.getArea());
        tile.setName(tileKey.getTileName());
        areaGroup.getImages().forEach(lsm -> {
            updateLsmTileName(lsm, tile.getName(), lsmUpdates);
            tile.addLsmReference(Reference.createFor(lsm));
        });
        return tile;
    }

    private void updateLsmTileName(LSMImage lsm, String tileName, Map<LSMImage, Map<String, EntityFieldValueHandler<?>>> lsmUpdates) {
        if (StringUtils.isBlank(lsm.getTile())) {
            Map<String, EntityFieldValueHandler<?>> updatedLsmFields = lsmUpdates.get(lsm);
            if (updatedLsmFields == null) {
                updatedLsmFields = new LinkedHashMap<>();
                lsmUpdates.put(lsm, updatedLsmFields);
            }
            lsm.setTile(tileName);
            updatedLsmFields.put("tile", new SetFieldValueHandler<>(tileName));
        }
    }

    private Optional<Sample> findBestSampleMatch(DataSet dataSet, String slideCode) {
        Sample sampleRef = new Sample();
        sampleRef.setDataSet(dataSet.getIdentifier());
        sampleRef.setSlideCode(slideCode);

        PageRequest pageRequest = new PageRequest();
        pageRequest.setSortCriteria(ImmutableList.of(new SortCriteria("creationDate", SortDirection.DESC)));
        PageResult<Sample> sampleCandidates = sampleDataService.searchSamples(dataSet.getOwnerKey(), sampleRef, new DataInterval<>(null, null), pageRequest);

        if (sampleCandidates.isEmpty()) {
            return Optional.empty();
        } else if (sampleCandidates.getResultList().size() == 1) {
            // there's no conflict
            return Optional.of(sampleCandidates.getResultList().get(0));
        } else {
            // retrieve first synced sample(i.e. latest sync-ed since the candidates should be ordered by creationDate desc)
            // or if none was synced get the latest sample created
            Sample selectedSample = sampleCandidates.getResultList().stream()
                    .filter(s -> s.isSageSynced())
                    .findFirst()
                    .orElseGet(() -> sampleCandidates.getResultList().get(0))
            ;
            // unsync all the other samples
            sampleCandidates.getResultList().stream()
                    .filter(s -> s != selectedSample)
                    .forEach(s -> {
                        s.setSageSynced(false);
                        s.setStatus(SampleProcessingStatus.Retired.name());
                        sampleDataService.updateSample(s, ImmutableMap.of("sageSynced", new SetFieldValueHandler<>(s.isSageSynced()), "status", new SetFieldValueHandler<>(s.getStatus())));
                    });
            return Optional.of(selectedSample);
        }
    }

    private Map<String, Map<SampleTileKey, SlideImageGroup>> groupLsmImagesByObjectiveAndArea(List<LSMImage> lsmImages) {
        Map<String, Map<SampleTileKey, SlideImageGroup>> areaImagesByObjective = new LinkedHashMap<>();
        int tileNum = 0;
        for(LSMImage lsm : lsmImages) {
            // Extract LSM metadata
            String objective = StringUtils.defaultIfBlank(lsm.getObjective(), "");
            String tag = StringUtils.defaultIfBlank(lsm.getTile(), "Tile "+(tileNum+1));
            String anatomicalArea = StringUtils.defaultIfBlank(lsm.getAnatomicalArea(), "");

            // Group LSMs by objective, tile and area
            Map<SampleTileKey, SlideImageGroup> areasForObjective = areaImagesByObjective.get(objective);
            if (areasForObjective == null) {
                areasForObjective = new LinkedHashMap<>();
                areaImagesByObjective.put(objective, areasForObjective);
            }

            SampleTileKey tileKey = new SampleTileKey(tag, anatomicalArea);
            SlideImageGroup tileLsms = areasForObjective.get(tileKey);
            if (tileLsms == null) {
                tileLsms = new SlideImageGroup(anatomicalArea, tag);
                areasForObjective.put(tileKey, tileLsms);
                tileNum++;
            }
            tileLsms.addImage(lsm);
        }
        return areaImagesByObjective;
    }

}
