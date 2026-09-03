// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level container representing a parsed SysML structure across one or more files.
 */
public class SysMLModel {
    private List<SysMLPackage> packages = new ArrayList<>();

    public List<SysMLPackage> getPackages() {
        return packages;
    }

    public void addPackage(SysMLPackage pkg) {
        this.packages.add(pkg);
    }

    public void resolveImports() {
        // Cross-package references resolution
    }

    /**
     * Finds all part definitions annotated with @RosSystemMapping across all packages.
     */
    public List<SysMLPartDef> findSystemRoots() {
        return findSystemRoots(null);
    }

    /**
     * Finds all part definitions annotated with @RosSystemMapping in packages from targetFilePath (or all if null).
     */
    public List<SysMLPartDef> findSystemRoots(String targetFilePath) {
        List<SysMLPartDef> roots = new ArrayList<>();
        for (SysMLPackage pkg : packages) {
            if (targetFilePath != null && !isSamePath(pkg.getSourceFilePath(), targetFilePath)) {
                continue;
            }
            for (SysMLPartDef def : pkg.getPartDefs()) {
                if (def.hasMetadata("RosSystemMapping")) {
                    roots.add(def);
                }
            }
        }
        return roots;
    }

    private boolean isSamePath(String path1, String path2) {
        if (path1 == null || path2 == null) return false;
        try {
            return java.nio.file.Paths.get(path1).toAbsolutePath().normalize()
                .equals(java.nio.file.Paths.get(path2).toAbsolutePath().normalize());
        } catch (Exception e) {
            return path1.equals(path2) || path1.endsWith(path2) || path2.endsWith(path1);
        }
    }

    /**
     * Finds all part definitions annotated with @RosArtifactMapping across all packages.
     */
    public List<SysMLPartDef> findAllArtifactPartDefs() {
        List<SysMLPartDef> defs = new ArrayList<>();
        for (SysMLPackage pkg : packages) {
            for (SysMLPartDef def : pkg.getPartDefs()) {
                if (def.hasMetadata("RosArtifactMapping")) {
                    defs.add(def);
                }
            }
        }
        return defs;
    }

    /**
     * Finds a PartDef by simple name across all packages.
     */
    public SysMLPartDef findPartDef(String name) {
        if (name == null) return null;
        for (SysMLPackage pkg : packages) {
            for (SysMLPartDef def : pkg.getPartDefs()) {
                if (name.equals(def.getName())) {
                    return def;
                }
            }
        }
        return null;
    }

    /**
     * Finds an ActionDef by simple name across all packages.
     */
    public SysMLActionDef findActionDef(String name) {
        if (name == null) return null;
        for (SysMLPackage pkg : packages) {
            for (SysMLActionDef def : pkg.getActionDefs()) {
                if (name.equals(def.getName())) {
                    return def;
                }
            }
        }
        return null;
    }

    /**
     * Finds the ActionDef specializing Exert that corresponds to a given Engine PartDef name.
     */
    public SysMLActionDef findActionDefForEngine(String enginePartDefName) {
        if (enginePartDefName == null) return null;
        for (SysMLPackage pkg : packages) {
            for (SysMLActionDef action : pkg.getActionDefs()) {
                if (action.specializesExert()) {
                    for (SysMLParameter inParam : action.getInParams()) {
                        if (inParam.isEngineRedefinition() && enginePartDefName.equals(inParam.getTypeName())) {
                            return action;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the enclosing package for a given PartDef.
     */
    public SysMLPackage findPackageContaining(SysMLPartDef partDef) {
        if (partDef == null) return null;
        for (SysMLPackage pkg : packages) {
            if (pkg.getPartDefs().contains(partDef)) {
                return pkg;
            }
        }
        return null;
    }

    /**
     * Finds a package by name.
     */
    public SysMLPackage findPackage(String name) {
        if (name == null) return null;
        for (SysMLPackage pkg : packages) {
            if (name.equals(pkg.getName())) {
                return pkg;
            }
        }
        return null;
    }
}
