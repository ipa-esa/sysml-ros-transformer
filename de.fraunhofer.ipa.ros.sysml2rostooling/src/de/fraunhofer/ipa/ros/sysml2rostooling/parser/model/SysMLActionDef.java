// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SysML action definition (e.g. Exert specializations).
 */
public class SysMLActionDef {
    private String name;
    private List<String> superTypes = new ArrayList<>();
    private List<SysMLMetadata> metadata = new ArrayList<>();
    private List<SysMLParameter> inParams = new ArrayList<>();
    private List<SysMLParameter> outParams = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSuperTypes() {
        return superTypes;
    }

    public void addSuperType(String superType) {
        this.superTypes.add(superType);
    }

    public List<SysMLMetadata> getMetadata() {
        return metadata;
    }

    public void addMetadata(SysMLMetadata meta) {
        this.metadata.add(meta);
    }

    public List<SysMLParameter> getInParams() {
        return inParams;
    }

    public void addInParam(SysMLParameter param) {
        this.inParams.add(param);
    }

    public List<SysMLParameter> getOutParams() {
        return outParams;
    }

    public void addOutParam(SysMLParameter param) {
        this.outParams.add(param);
    }

    public boolean specializesExert() {
        return superTypes.contains("Exert");
    }
}
