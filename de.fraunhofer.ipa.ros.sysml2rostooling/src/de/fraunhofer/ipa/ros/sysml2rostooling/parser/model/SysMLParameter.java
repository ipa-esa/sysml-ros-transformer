// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parameter in a SysML action.
 */
public class SysMLParameter {
    private String name;
    private String typeName;
    private String multiplicity;
    private String redefines;
    private String subsets;
    private List<SysMLMetadata> metadata = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getMultiplicity() {
        return multiplicity;
    }

    public void setMultiplicity(String multiplicity) {
        this.multiplicity = multiplicity;
    }

    public String getRedefines() {
        return redefines;
    }

    public void setRedefines(String redefines) {
        this.redefines = redefines;
    }

    public String getSubsets() {
        return subsets;
    }

    public void setSubsets(String subsets) {
        this.subsets = subsets;
    }

    public List<SysMLMetadata> getMetadata() {
        return metadata;
    }

    public void addMetadata(SysMLMetadata meta) {
        this.metadata.add(meta);
    }

    public boolean isModeletsRedefinition() {
        return (redefines != null && redefines.contains("modelets")) || 
               (subsets != null && subsets.contains("modelets"));
    }

    public boolean isEngineRedefinition() {
        return (redefines != null && (redefines.contains("eng") || redefines.contains("engine"))) ||
               (subsets != null && (subsets.contains("eng") || subsets.contains("engine")));
    }

    public boolean isOutputRedefinition() {
        return (redefines != null && (redefines.contains("output") || redefines.contains("results"))) || 
               (subsets != null && (subsets.contains("output") || subsets.contains("results")));
    }
}
