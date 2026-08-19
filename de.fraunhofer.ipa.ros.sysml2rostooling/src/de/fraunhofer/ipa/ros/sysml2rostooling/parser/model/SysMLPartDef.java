// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SysML part definition (Engine, Modelet, Camera, etc.).
 */
public class SysMLPartDef {
    private String name;
    private List<String> superTypes = new ArrayList<>();
    private List<SysMLMetadata> metadata = new ArrayList<>();
    private List<SysMLPartUsage> parts = new ArrayList<>();
    private List<SysMLActionUsage> actions = new ArrayList<>();

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

    public List<SysMLPartUsage> getParts() {
        return parts;
    }

    public void addPartUsage(SysMLPartUsage part) {
        this.parts.add(part);
    }

    public List<SysMLActionUsage> getActions() {
        return actions;
    }

    public void addActionUsage(SysMLActionUsage action) {
        this.actions.add(action);
    }

    public boolean hasMetadata(String metadataName) {
        return getMetadata(metadataName) != null;
    }

    public SysMLMetadata getMetadata(String metadataName) {
        for (SysMLMetadata meta : metadata) {
            if (meta.getName().equals(metadataName)) {
                return meta;
            }
        }
        return null;
    }

    public boolean isSpecializationOf(String typeName) {
        // Naive implementation; full implementation requires model resolution
        return superTypes.contains(typeName);
    }
}
