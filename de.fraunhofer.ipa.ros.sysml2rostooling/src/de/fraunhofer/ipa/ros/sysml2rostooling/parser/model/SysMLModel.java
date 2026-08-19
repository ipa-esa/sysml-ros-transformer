// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level container representing a parsed SysML structure.
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
}
