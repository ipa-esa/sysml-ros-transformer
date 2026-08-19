// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a package scope in SysML.
 */
public class SysMLPackage {
    private String name;
    private List<String> imports = new ArrayList<>();
    private List<SysMLPartDef> partDefs = new ArrayList<>();
    private List<SysMLActionDef> actionDefs = new ArrayList<>();
    private List<SysMLFlow> flows = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getImports() {
        return imports;
    }

    public void addImport(String imp) {
        this.imports.add(imp);
    }

    public List<SysMLPartDef> getPartDefs() {
        return partDefs;
    }

    public void addPartDef(SysMLPartDef partDef) {
        this.partDefs.add(partDef);
    }

    public List<SysMLActionDef> getActionDefs() {
        return actionDefs;
    }

    public void addActionDef(SysMLActionDef actionDef) {
        this.actionDefs.add(actionDef);
    }

    public List<SysMLFlow> getFlows() {
        return flows;
    }

    public void addFlow(SysMLFlow flow) {
        this.flows.add(flow);
    }
}
