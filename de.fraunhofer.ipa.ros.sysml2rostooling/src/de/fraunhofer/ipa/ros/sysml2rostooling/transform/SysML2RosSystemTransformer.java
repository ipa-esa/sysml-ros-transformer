// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.fraunhofer.ipa.ros.sysml2rostooling.parser.SysMLParser;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLActionDef;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLActionUsage;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLFlow;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLMetadata;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLModel;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLPackage;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLParameter;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLPartDef;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLPartUsage;

/**
 * Transforms a SysMLModel into a RosSystemResult using Strategy 1 (explicit @RosSystemMapping).
 */
public class SysML2RosSystemTransformer {

    public static class RosSystemResult {
        public String systemName;
        public String fromFile;
        public List<RosNodeResult> nodes = new ArrayList<>();
        public List<RosConnectionResult> connections = new ArrayList<>();
        
        public String getSystemName() { return systemName; }
        public String getFromFile() { return fromFile; }
        public List<RosNodeResult> getNodes() { return nodes; }
        public List<RosConnectionResult> getConnections() { return connections; }
    }

    public static class RosNodeResult {
        public String name;
        public String fromRef;
        public String namespace;
        public List<RosInterfaceResult> interfaces = new ArrayList<>();
        
        public String getName() { return name; }
        public String getFromRef() { return fromRef; }
        public String getNamespace() { return namespace; }
        public List<RosInterfaceResult> getInterfaces() { return interfaces; }
    }

    public static class RosInterfaceResult {
        public String name;
        public String direction;
        public String fromRef;
        
        public String getName() { return name; }
        public String getDirection() { return direction; }
        public String getFromRef() { return fromRef; }
    }

    public static class RosConnectionResult {
        public String fromInterface;
        public String toInterface;
        
        public RosConnectionResult(String fromInterface, String toInterface) {
            this.fromInterface = fromInterface;
            this.toInterface = toInterface;
        }
        public String getFromInterface() { return fromInterface; }
        public String getToInterface() { return toInterface; }
    }

    /**
     * Transforms the given SysMLModel into a list of RosSystemResult objects.
     * Enforces Strategy 1: The model must contain at least one root PartDef annotated with @RosSystemMapping.
     *
     * @param model the parsed SysML model across one or more files
     * @return the list of generated RosSystemResult objects
     * @throws IllegalArgumentException if no @RosSystemMapping is present in the model
     */
    public List<RosSystemResult> transform(SysMLModel model) {
        return transform(model, null);
    }

    /**
     * Transforms the given SysMLModel into a list of RosSystemResult objects,
     * filtering for system root(s) defined in the specified targetFilePath.
     *
     * @param model the parsed SysML model across one or more files
     * @param targetFilePath optional path of the selected SysML file to transform
     * @return the list of generated RosSystemResult objects
     * @throws IllegalArgumentException if no @RosSystemMapping is present in the target file
     */
    public List<RosSystemResult> transform(SysMLModel model, String targetFilePath) {
        List<SysMLPartDef> systemRoots = model.findSystemRoots(targetFilePath);
        if (systemRoots.isEmpty()) {
            if (targetFilePath != null) {
                throw new IllegalArgumentException(
                    "No system composition annotated with @RosSystemMapping was found in the selected file: " + targetFilePath + ". " +
                    "Please select a file containing the root system or annotate the main system with @RosSystemMapping(systemName = \"...\")."
                );
            } else {
                throw new IllegalArgumentException(
                    "No system composition annotated with @RosSystemMapping was found. " +
                    "Please annotate the root system part definition with @RosSystemMapping(systemName = \"...\") to specify the main system."
                );
            }
        }

        List<RosSystemResult> results = new ArrayList<>();

        for (SysMLPartDef systemPart : systemRoots) {
            RosSystemResult result = new RosSystemResult();
            SysMLMetadata sysMeta = systemPart.getMetadata("RosSystemMapping");
            
            result.systemName = sysMeta.getAttribute("systemName");
            if (result.systemName == null || result.systemName.isBlank()) {
                result.systemName = systemPart.getName().toLowerCase();
            }
            result.fromFile = sysMeta.getAttribute("fromFile");

            SysMLPackage systemPkg = model.findPackageContaining(systemPart);

            // Map PartDef names to instance names
            Map<String, String> partDefToInstanceName = new LinkedHashMap<>();
            Map<String, String> actionUsageToNodeName = new LinkedHashMap<>();
            
            for (SysMLPartUsage usage : systemPart.getParts()) {
                partDefToInstanceName.put(usage.getTypeName(), usage.getName());
            }

            for (SysMLActionUsage actionUsage : systemPart.getActions()) {
                SysMLActionDef actionDef = model.findActionDef(actionUsage.getTypeName());
                if (actionDef != null) {
                    for (SysMLParameter inParam : actionDef.getInParams()) {
                        if (inParam.isEngineRedefinition()) {
                            String nodeInstance = partDefToInstanceName.get(inParam.getTypeName());
                            if (nodeInstance == null) {
                                nodeInstance = toDefaultInstanceName(inParam.getTypeName());
                            }
                            actionUsageToNodeName.put(actionUsage.getName(), nodeInstance);
                        }
                    }
                }
            }

            // Determine which component part defs belong to this system
            List<SysMLPartDef> componentsToInclude = new ArrayList<>();
            if (!systemPart.getParts().isEmpty()) {
                // Multi-file composition style: part defs referenced by part usages
                for (SysMLPartUsage usage : systemPart.getParts()) {
                    SysMLPartDef compDef = model.findPartDef(usage.getTypeName());
                    if (compDef != null && compDef.hasMetadata("RosArtifactMapping")) {
                        componentsToInclude.add(compDef);
                    }
                }
            } else if (systemPkg != null) {
                // Single-package style fallback: include artifact part defs in the same package
                for (SysMLPartDef p : systemPkg.getPartDefs()) {
                    if (p.hasMetadata("RosArtifactMapping")) {
                        componentsToInclude.add(p);
                    }
                }
            }

            // If still empty, scan entire model for artifact part defs
            if (componentsToInclude.isEmpty()) {
                componentsToInclude = model.findAllArtifactPartDefs();
            }

            // Create RosNodeResult for each component
            for (SysMLPartDef p : componentsToInclude) {
                RosNodeResult node = new RosNodeResult();
                String instanceName = partDefToInstanceName.get(p.getName());
                if (instanceName == null) {
                    instanceName = toDefaultInstanceName(p.getName());
                }
                node.name = instanceName;

                SysMLMetadata artiMeta = p.getMetadata("RosArtifactMapping");
                String rosPackage = artiMeta.getAttribute("rosPackage");
                String rosArtifact = artiMeta.getAttribute("rosArtifact");
                node.fromRef = rosPackage + "." + rosArtifact;
                node.namespace = artiMeta.getAttribute("rosNamespace");
                
                // Find Exert specializations for this node across all packages
                SysMLActionDef action = model.findActionDefForEngine(p.getName());
                if (action != null) {
                    for (SysMLParameter inParam : action.getInParams()) {
                        if (inParam.isEngineRedefinition()) {
                            continue;
                        }
                        if (inParam.isModeletsRedefinition() && ("Modelet".equals(inParam.getTypeName()) || "Base::Anything".equals(inParam.getTypeName()))) {
                            continue;
                        }
                        RosInterfaceResult iface = createInterface(inParam, "sub->", node.name, rosArtifact, model);
                        node.interfaces.add(iface);
                    }
                    for (SysMLParameter outParam : action.getOutParams()) {
                        if (outParam.isOutputRedefinition() && ("Base::Anything".equals(outParam.getTypeName()) || "Modelet".equals(outParam.getTypeName()))) {
                            continue;
                        }
                        RosInterfaceResult iface = createInterface(outParam, "pub->", node.name, rosArtifact, model);
                        node.interfaces.add(iface);
                    }
                }
                result.nodes.add(node);
            }

            // Map flows from the system package
            if (systemPkg != null) {
                for (SysMLFlow flow : systemPkg.getFlows()) {
                    String sourceNode = actionUsageToNodeName.getOrDefault(flow.getSourcePart(), flow.getSourcePart());
                    String targetNode = actionUsageToNodeName.getOrDefault(flow.getTargetPart(), flow.getTargetPart());
                    String fromInterface = sourceNode + "_" + flow.getSourceFeature();
                    String toInterface = targetNode + "_" + flow.getTargetFeature();
                    result.connections.add(new RosConnectionResult(fromInterface, toInterface));
                }
            }

            results.add(result);
        }

        return results;
    }

    private String toDefaultInstanceName(String typeName) {
        String raw = typeName;
        if (raw.endsWith("Engine") && raw.length() > 6) {
            raw = raw.substring(0, raw.length() - 6);
        }
        return Character.toLowerCase(raw.charAt(0)) + raw.substring(1);
    }

    private RosInterfaceResult createInterface(SysMLParameter param, String defaultDir, String nodeName, String rosArtifact, SysMLModel model) {
        RosInterfaceResult iface = new RosInterfaceResult();
        
        SysMLMetadata ifaceMeta = null;
        SysMLMetadata typeMeta = null;
        for (SysMLMetadata m : param.getMetadata()) {
            if ("RosInterfaceMapping".equals(m.getName())) ifaceMeta = m;
            if ("RosTypeMapping".equals(m.getName())) typeMeta = m;
        }

        if (typeMeta == null && param.getTypeName() != null) {
            SysMLPartDef typeDef = model.findPartDef(param.getTypeName());
            if (typeDef != null) {
                typeMeta = typeDef.getMetadata("RosTypeMapping");
            }
        }

        if (ifaceMeta != null && ifaceMeta.getAttribute("interfaceName") != null) {
            iface.name = ifaceMeta.getAttribute("interfaceName");
        } else {
            iface.name = nodeName + "_" + param.getName();
        }

        iface.direction = defaultDir;
        if (typeMeta != null) {
            String rosType = typeMeta.getAttribute("rosType");
            if (rosType != null) {
                if (rosType.contains("/srv/")) {
                    iface.direction = "sub->".equals(defaultDir) ? "ss->" : "sc->";
                } else if (rosType.contains("/action/")) {
                    iface.direction = "sub->".equals(defaultDir) ? "as->" : "ac->";
                }
            }
        }
        
        iface.fromRef = rosArtifact + "::" + param.getName();
        return iface;
    }

    /**
     * Generates .rossystem formatted text directly from a RosSystemResult.
     */
    public String generateText(RosSystemResult system) {
        StringBuilder sb = new StringBuilder();
        sb.append(system.systemName).append(":\n");
        if (system.fromFile != null) {
            sb.append("  fromFile: \"").append(system.fromFile).append("\"\n");
        }
        sb.append("  nodes:\n");
        for (RosNodeResult node : system.nodes) {
            sb.append("    \"").append(node.name).append("\":\n");
            sb.append("      from: \"").append(node.fromRef).append("\"\n");
            if (node.namespace != null) {
                sb.append("      namespace: \"").append(node.namespace).append("\"\n");
            }
            if (!node.interfaces.isEmpty()) {
                sb.append("      interfaces:\n");
                for (RosInterfaceResult iface : node.interfaces) {
                    sb.append("        - \"").append(iface.name).append("\": ")
                      .append(iface.direction).append(" \"")
                      .append(iface.fromRef).append("\"\n");
                }
            }
        }
        if (!system.connections.isEmpty()) {
            sb.append("  connections:\n");
            for (RosConnectionResult conn : system.connections) {
                sb.append("    - [\"").append(conn.fromInterface).append("\", \"")
                  .append(conn.toInterface).append("\"]\n");
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: SysML2RosSystemTransformer <input.sysml> [additional.sysml ...]");
            System.exit(1);
        }
        try {
            SysMLParser parser = new SysMLParser();
            SysMLModel model = parser.parse(Arrays.asList(args));
            SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
            List<RosSystemResult> results = transformer.transform(model);
            for (RosSystemResult result : results) {
                System.out.println(transformer.generateText(result));
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
