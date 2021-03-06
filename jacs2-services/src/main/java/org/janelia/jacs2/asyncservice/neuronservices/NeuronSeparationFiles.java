package org.janelia.jacs2.asyncservice.neuronservices;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NeuronSeparationFiles {
    private String resultDir;
    private String consolidatedLabel;
    private String consolidatedSignal;
    private String consolidatedSignalMip;
    private String reference;
    private String referenceMip;
    private String mappingIssues;
    private List<String> neurons = new ArrayList<>();
    private List<String> separationResults = new ArrayList<>();
    private String fastLoadSubDir;
    private String archiveSubdir;
    private String maskChanSubdir;
    private List<String> maskFiles = new ArrayList<>();
    private List<String> chanFiles = new ArrayList<>();
    private String refMaskFile;
    private String refChanFile;
    private String consolidatedSignalMovieResult;

    public String getResultDir() {
        return resultDir;
    }

    public void setResultDir(String resultDir) {
        this.resultDir = resultDir;
    }

    public String getConsolidatedLabel() {
        return consolidatedLabel;
    }

    public void setConsolidatedLabel(String consolidatedLabel) {
        this.consolidatedLabel = consolidatedLabel;
    }

    public String getConsolidatedSignal() {
        return consolidatedSignal;
    }

    public void setConsolidatedSignal(String consolidatedSignal) {
        this.consolidatedSignal = consolidatedSignal;
    }

    public String getConsolidatedSignalMip() {
        return consolidatedSignalMip;
    }

    public void setConsolidatedSignalMip(String consolidatedSignalMip) {
        this.consolidatedSignalMip = consolidatedSignalMip;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReferenceMip() {
        return referenceMip;
    }

    public void setReferenceMip(String referenceMip) {
        this.referenceMip = referenceMip;
    }

    public String getMappingIssues() {
        return mappingIssues;
    }

    public void setMappingIssues(String mappingIssues) {
        this.mappingIssues = mappingIssues;
    }

    public List<String> getNeurons() {
        return neurons;
    }

    public void setNeurons(List<String> neurons) {
        this.neurons = neurons;
    }

    public void addNeuron(String neuron) {
        this.neurons.add(neuron);
    }

    public List<String> getSeparationResults() {
        return separationResults;
    }

    public void setSeparationResults(List<String> separationResults) {
        this.separationResults = separationResults;
    }

    public void addSeparationResult(String separationResult) {
        this.separationResults.add(separationResult);
    }

    public Optional<String> findSeparationResult() {
        return this.separationResults.stream()
                .filter(rn -> "SeparationResultUnmapped.nsp".equals(rn) || "SeparationResult.nsp".equals(rn))
                .findFirst();
    }

    public String getFastLoadSubDir() {
        return fastLoadSubDir;
    }

    public void setFastLoadSubDir(String fastLoadSubDir) {
        this.fastLoadSubDir = fastLoadSubDir;
    }

    public String getArchiveSubdir() {
        return archiveSubdir;
    }

    public void setArchiveSubdir(String archiveSubdir) {
        this.archiveSubdir = archiveSubdir;
    }

    public String getMaskChanSubdir() {
        return maskChanSubdir;
    }

    public void setMaskChanSubdir(String maskChanSubdir) {
        this.maskChanSubdir = maskChanSubdir;
    }

    public List<String> getMaskFiles() {
        return maskFiles;
    }

    public void setMaskFiles(List<String> maskFiles) {
        this.maskFiles = maskFiles;
    }

    public void addMaskFile(String maskFile) {
        maskFiles.add(maskFile);
    }

    public List<String> getChanFiles() {
        return chanFiles;
    }

    public void setChanFiles(List<String> chanFiles) {
        this.chanFiles = chanFiles;
    }

    public void addChanFile(String chanFile) {
        chanFiles.add(chanFile);
    }

    public String getRefMaskFile() {
        return refMaskFile;
    }

    public void setRefMaskFile(String refMaskFile) {
        this.refMaskFile = refMaskFile;
    }

    public String getRefChanFile() {
        return refChanFile;
    }

    public void setRefChanFile(String refChanFile) {
        this.refChanFile = refChanFile;
    }

    public Optional<String> getNeuronMask(Integer neuronIndex) {
        String neuronMaskName = "neuron_" + neuronIndex + ".mask";
        return this.maskFiles.stream().filter(fn -> fn.contains(neuronMaskName)).findFirst();
    }

    public Optional<String> getNeuronChan(Integer neuronIndex) {
        String neuronChanName = "neuron_" + neuronIndex + ".chan";
        return this.chanFiles.stream().filter(fn -> fn.contains(neuronChanName)).findFirst();
    }

    @JsonIgnore
    public Path getConsolidatedLabelPath() {
        return Paths.get(resultDir, consolidatedLabel);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("resulDir", resultDir)
                .append("consolidatedLabel", consolidatedLabel)
                .build();
    }

    public String getConsolidatedSignalMovieResult() {
        return consolidatedSignalMovieResult;
    }

    public void setConsolidatedSignalMovieResult(String consolidatedSignalMovieResult) {
        this.consolidatedSignalMovieResult = consolidatedSignalMovieResult;
    }
}
