// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

/**
 * Represents a flow connection in SysML.
 */
public class SysMLFlow {
    private String sourcePart;
    private String sourceFeature;
    private String targetPart;
    private String targetFeature;

    public SysMLFlow() {}

    public SysMLFlow(String sourcePart, String sourceFeature, String targetPart, String targetFeature) {
        this.sourcePart = sourcePart;
        this.sourceFeature = sourceFeature;
        this.targetPart = targetPart;
        this.targetFeature = targetFeature;
    }

    public String getSourcePart() {
        return sourcePart;
    }

    public void setSourcePart(String sourcePart) {
        this.sourcePart = sourcePart;
    }

    public String getSourceFeature() {
        return sourceFeature;
    }

    public void setSourceFeature(String sourceFeature) {
        this.sourceFeature = sourceFeature;
    }

    public String getTargetPart() {
        return targetPart;
    }

    public void setTargetPart(String targetPart) {
        this.targetPart = targetPart;
    }

    public String getTargetFeature() {
        return targetFeature;
    }

    public void setTargetFeature(String targetFeature) {
        this.targetFeature = targetFeature;
    }
}
