//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml.transform;

import java.util.*;

import org.eclipse.emf.ecore.InternalEObject;

import system.System;
import system.RosNode;
import system.SubSystem;
import system.Component;
import system.Connection;
import system.RosInterface;
import system.RosSystemConnection;
import system.InterfaceReference;
import system.RosPublisherReference;
import system.RosSubscriberReference;
import system.RosServiceServerReference;
import system.RosServiceClientReference;
import system.RosActionServerReference;
import system.RosActionClientReference;
import ros.Node;
import ros.Publisher;
import ros.Subscriber;
import ros.ServiceServer;
import ros.ServiceClient;
import ros.ActionServer;
import ros.ActionClient;
import ros.TopicSpec;
import ros.ServiceSpec;
import ros.ActionSpec;
import ros.Package;
import ros.Artifact;

/**
 * Transforms a ROS System model into a SysMLResult data structure.
 */
public class RosSystem2SysMLTransformer {

    public static class SysMLResult {
        public String packageName;
        public String systemName;
        public String fromFile;
        public List<ModeletTypeResult> modeletTypes = new ArrayList<>();
        public List<EngineResult> engines = new ArrayList<>();
        public List<ExertResult> exerts = new ArrayList<>();
        public List<FlowResult> flows = new ArrayList<>();
    }

    public static class ModeletTypeResult {
        public String name;
        public String rosType;
    }

    public static class EngineResult {
        public String defName;
        public String instanceName;
        public String rosPackage;
        public String rosArtifact;
        public String rosNamespace;
    }

    public static class ExertResult {
        public String name;
        public String engineDefName;
        public String engineInstanceName;
        public List<ExertParamResult> inParams = new ArrayList<>();
        public List<ExertParamResult> outParams = new ArrayList<>();
    }

    public static class ExertParamResult {
        public String name;
        public String modeletTypeName;
        public boolean isMultiple;
    }

    public static class FlowResult {
        public String sourceExertInstance;
        public String sourceParam;
        public String targetExertInstance;
        public String targetParam;
    }

    public SysMLResult transform(System rossystem) {
        return transform(rossystem, null, null);
    }

    public SysMLResult transform(System rossystem, String ros2Content, String rossystemContent) {
        SysMLResult result = new SysMLResult();
        result.systemName = rossystem.getName();
        result.fromFile = rossystem.getFromFile();
        result.packageName = (result.systemName != null ? result.systemName : "Unknown") + "_architecture";

        Map<String, String> ifaceTypeMap = parseRos2InterfaceTypes(ros2Content);
        Map<String, String[]> nodeArtifactMap = parseNodeArtifactMappings(rossystemContent);

        Map<String, ModeletTypeResult> modeletTypeMap = new LinkedHashMap<>();

        // Process Components
        for (Component component : rossystem.getComponents()) {
            if (component instanceof RosNode) {
                RosNode node = (RosNode) component;
                
                // 3. Discover Engines
                EngineResult engine = new EngineResult();
                engine.instanceName = node.getName();
                engine.defName = capitalize(engine.instanceName) + "Engine";
                engine.rosNamespace = node.getNamespace();
                
                Node rosNodeRef = node.getFrom();
                if (rosNodeRef != null && !rosNodeRef.eIsProxy() && rosNodeRef.eContainer() instanceof Artifact) {
                    Artifact artifact = (Artifact) rosNodeRef.eContainer();
                    engine.rosArtifact = artifact.getName();
                    if (artifact.eContainer() instanceof Package) {
                        Package pkg = (Package) artifact.eContainer();
                        engine.rosPackage = pkg.getName();
                    }
                }

                // If proxy or uncontained, resolve from proxy URI or text map
                if (engine.rosPackage == null || engine.rosArtifact == null || "null".equals(engine.rosPackage)) {
                    if (rosNodeRef != null && rosNodeRef.eIsProxy()) {
                        org.eclipse.emf.common.util.URI proxyUri = ((InternalEObject) rosNodeRef).eProxyURI();
                        if (proxyUri != null && proxyUri.fragment() != null) {
                            String frag = proxyUri.fragment();
                            int lastSlash = frag.lastIndexOf('/');
                            String refStr = lastSlash >= 0 ? frag.substring(lastSlash + 1) : frag;
                            if (refStr.contains(".")) {
                                String[] parts = refStr.split("\\.");
                                if (parts.length >= 2) {
                                    engine.rosPackage = parts[0];
                                    engine.rosArtifact = parts[1];
                                }
                            }
                        }
                    }
                    if (engine.rosPackage == null || engine.rosArtifact == null || "null".equals(engine.rosPackage)) {
                        String[] mapped = nodeArtifactMap.get(node.getName());
                        if (mapped != null) {
                            engine.rosPackage = mapped[0];
                            engine.rosArtifact = mapped[1];
                        }
                    }
                }

                if (engine.rosPackage == null || "null".equals(engine.rosPackage)) {
                    engine.rosPackage = engine.instanceName + "_pkg";
                }
                if (engine.rosArtifact == null || "null".equals(engine.rosArtifact)) {
                    engine.rosArtifact = engine.instanceName + "_artifact";
                }

                result.engines.add(engine);

                // 4. Build Exerts
                ExertResult exert = new ExertResult();
                exert.name = capitalize(engine.instanceName) + "Exert";
                exert.engineDefName = engine.defName;
                exert.engineInstanceName = engine.instanceName;

                for (RosInterface iface : node.getRosinterfaces()) {
                    String rosType = getRosType(iface.getReference());
                    if (rosType == null || "unknown/msg/Unknown".equals(rosType)) {
                        rosType = resolveInterfaceRosType(iface, engine.rosArtifact, ifaceTypeMap);
                    }
                    String shortName = getShortTypeName(rosType);
                    
                    if (rosType != null && !modeletTypeMap.containsKey(rosType)) {
                        ModeletTypeResult mtr = new ModeletTypeResult();
                        mtr.name = shortName;
                        mtr.rosType = rosType;
                        modeletTypeMap.put(rosType, mtr);
                    }

                    ExertParamResult param = new ExertParamResult();
                    param.name = iface.getName();
                    param.modeletTypeName = shortName;
                    param.isMultiple = false;

                    InterfaceReference ref = iface.getReference();
                    if (ref instanceof RosPublisherReference || 
                        ref instanceof RosServiceClientReference || 
                        ref instanceof RosActionClientReference) {
                        exert.outParams.add(param);
                    } else if (ref instanceof RosSubscriberReference || 
                               ref instanceof RosServiceServerReference || 
                               ref instanceof RosActionServerReference) {
                        exert.inParams.add(param);
                    }
                }
                result.exerts.add(exert);
            }
        }

        result.modeletTypes.addAll(modeletTypeMap.values());

        // 5. Build Flows
        for (Connection conn : rossystem.getConnections()) {
            if (conn instanceof RosSystemConnection) {
                RosSystemConnection rsc = (RosSystemConnection) conn;
                FlowResult flow = new FlowResult();
                
                RosInterface fromIface = rsc.getFrom();
                RosInterface toIface = rsc.getTo();
                
                if (fromIface != null && fromIface.eContainer() instanceof RosNode) {
                    RosNode fromNode = (RosNode) fromIface.eContainer();
                    flow.sourceExertInstance = fromNode.getName() + "Exert";
                    flow.sourceParam = fromIface.getName();
                }
                
                if (toIface != null && toIface.eContainer() instanceof RosNode) {
                    RosNode toNode = (RosNode) toIface.eContainer();
                    flow.targetExertInstance = toNode.getName() + "Exert";
                    flow.targetParam = toIface.getName();
                }
                
                result.flows.add(flow);
            }
        }

        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getRosType(InterfaceReference ref) {
        if (ref instanceof RosPublisherReference) {
            Publisher pub = ((RosPublisherReference) ref).getFrom();
            return (pub != null && !pub.eIsProxy()) ? getTopicSpecRosType(pub.getMessage()) : null;
        } else if (ref instanceof RosSubscriberReference) {
            Subscriber sub = ((RosSubscriberReference) ref).getFrom();
            return (sub != null && !sub.eIsProxy()) ? getTopicSpecRosType(sub.getMessage()) : null;
        } else if (ref instanceof RosServiceServerReference) {
            ServiceServer ss = ((RosServiceServerReference) ref).getFrom();
            return (ss != null && !ss.eIsProxy()) ? getServiceSpecRosType(ss.getService()) : null;
        } else if (ref instanceof RosServiceClientReference) {
            ServiceClient sc = ((RosServiceClientReference) ref).getFrom();
            return (sc != null && !sc.eIsProxy()) ? getServiceSpecRosType(sc.getService()) : null;
        } else if (ref instanceof RosActionServerReference) {
            ActionServer as = ((RosActionServerReference) ref).getFrom();
            return (as != null && !as.eIsProxy()) ? getActionSpecRosType(as.getAction()) : null;
        } else if (ref instanceof RosActionClientReference) {
            ActionClient ac = ((RosActionClientReference) ref).getFrom();
            return (ac != null && !ac.eIsProxy()) ? getActionSpecRosType(ac.getAction()) : null;
        }
        return null;
    }

    private String resolveInterfaceRosType(RosInterface iface, String artifactName, Map<String, String> ifaceTypeMap) {
        InterfaceReference ref = iface.getReference();
        if (ref != null) {
            Object fromObj = getReferenceFrom(ref);
            if (fromObj instanceof InternalEObject && ((InternalEObject) fromObj).eIsProxy()) {
                org.eclipse.emf.common.util.URI proxyUri = ((InternalEObject) fromObj).eProxyURI();
                if (proxyUri != null && proxyUri.fragment() != null) {
                    String frag = proxyUri.fragment();
                    int lastSlash = frag.lastIndexOf('/');
                    String refStr = lastSlash >= 0 ? frag.substring(lastSlash + 1) : frag;
                    if (ifaceTypeMap.containsKey(refStr)) {
                        return ifaceTypeMap.get(refStr);
                    }
                    if (refStr.contains("::")) {
                        String subName = refStr.substring(refStr.indexOf("::") + 2);
                        if (ifaceTypeMap.containsKey(subName)) {
                            return ifaceTypeMap.get(subName);
                        }
                    }
                }
            }
        }

        if (artifactName != null && ifaceTypeMap.containsKey(artifactName + "::" + iface.getName())) {
            return ifaceTypeMap.get(artifactName + "::" + iface.getName());
        }

        if (ifaceTypeMap.containsKey(iface.getName())) {
            return ifaceTypeMap.get(iface.getName());
        }

        for (Map.Entry<String, String> entry : ifaceTypeMap.entrySet()) {
            String key = entry.getKey();
            String keyName = key.contains("::") ? key.substring(key.indexOf("::") + 2) : key;
            if (iface.getName().endsWith(keyName) || iface.getName().contains(keyName)) {
                return entry.getValue();
            }
        }

        return inferRosTypeFromInterfaceName(iface.getName());
    }

    private Object getReferenceFrom(InterfaceReference ref) {
        if (ref instanceof RosPublisherReference) return ((RosPublisherReference) ref).getFrom();
        if (ref instanceof RosSubscriberReference) return ((RosSubscriberReference) ref).getFrom();
        if (ref instanceof RosServiceServerReference) return ((RosServiceServerReference) ref).getFrom();
        if (ref instanceof RosServiceClientReference) return ((RosServiceClientReference) ref).getFrom();
        if (ref instanceof RosActionServerReference) return ((RosActionServerReference) ref).getFrom();
        if (ref instanceof RosActionClientReference) return ((RosActionClientReference) ref).getFrom();
        return null;
    }

    private String inferRosTypeFromInterfaceName(String ifaceName) {
        if (ifaceName == null) return "std_msgs/msg/String";
        String lower = ifaceName.toLowerCase();
        if (lower.contains("image")) return "sensor_msgs/msg/Image";
        if (lower.contains("detect")) return "vision_msgs/msg/Detection2DArray";
        if (lower.contains("camera_info")) return "sensor_msgs/msg/CameraInfo";
        if (lower.contains("scan") || lower.contains("laser")) return "sensor_msgs/msg/LaserScan";
        if (lower.contains("cloud") || lower.contains("points")) return "sensor_msgs/msg/PointCloud2";
        if (lower.contains("twist") || lower.contains("cmd_vel") || lower.contains("vel")) return "geometry_msgs/msg/Twist";
        if (lower.contains("odom")) return "nav_msgs/msg/Odometry";
        if (lower.contains("pose")) return "geometry_msgs/msg/PoseStamped";
        if (lower.contains("imu")) return "sensor_msgs/msg/Imu";
        return "std_msgs/msg/" + capitalize(ifaceName);
    }

    private String getTopicSpecRosType(TopicSpec spec) {
        if (spec == null || spec.eIsProxy()) return null;
        if (spec.eContainer() instanceof Package) {
            Package pkg = (Package) spec.eContainer();
            return pkg.getName() + "/msg/" + spec.getName();
        }
        return "std_msgs/msg/" + spec.getName();
    }

    private String getServiceSpecRosType(ServiceSpec spec) {
        if (spec == null || spec.eIsProxy()) return null;
        if (spec.eContainer() instanceof Package) {
            Package pkg = (Package) spec.eContainer();
            return pkg.getName() + "/srv/" + spec.getName();
        }
        return "std_msgs/srv/" + spec.getName();
    }

    private String getActionSpecRosType(ActionSpec spec) {
        if (spec == null || spec.eIsProxy()) return null;
        if (spec.eContainer() instanceof Package) {
            Package pkg = (Package) spec.eContainer();
            return pkg.getName() + "/action/" + spec.getName();
        }
        return "std_msgs/action/" + spec.getName();
    }

    private String getShortTypeName(String rosType) {
        if (rosType == null) return "Unknown";
        int lastSlash = rosType.lastIndexOf('/');
        return lastSlash >= 0 ? rosType.substring(lastSlash + 1) : rosType;
    }

    public static Map<String, String> parseRos2InterfaceTypes(String ros2Content) {
        Map<String, String> artifactIfaceToRosType = new LinkedHashMap<>();
        if (ros2Content != null) {
            String currentArtifact = null;
            String currentIface = null;
            for (String rawLine : ros2Content.split("\n")) {
                String trimmed = rawLine.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                if (rawLine.startsWith("    ") && !rawLine.startsWith("      ") && trimmed.endsWith(":")) {
                    currentArtifact = trimmed.substring(0, trimmed.length() - 1).trim();
                } else if (rawLine.startsWith("        ") && !rawLine.startsWith("          ") && trimmed.endsWith(":")) {
                    currentIface = trimmed.substring(0, trimmed.length() - 1).trim();
                } else if (trimmed.startsWith("type:")) {
                    String rosType = trimmed.substring("type:".length()).trim().replace("\"", "");
                    if (currentArtifact != null && currentIface != null) {
                        artifactIfaceToRosType.put(currentArtifact + "::" + currentIface, rosType);
                        artifactIfaceToRosType.put(currentIface, rosType);
                    }
                }
            }
        }
        return artifactIfaceToRosType;
    }

    public static Map<String, String[]> parseNodeArtifactMappings(String rossystemContent) {
        Map<String, String[]> nodeToPkgArtifact = new LinkedHashMap<>();
        if (rossystemContent != null) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"?([a-zA-Z0-9_]+)\"?:[\\s\\n]+from:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher m = p.matcher(rossystemContent);
            while (m.find()) {
                String nodeName = m.group(1);
                String fromRef = m.group(2);
                String[] parts = fromRef.split("\\.");
                if (parts.length >= 2) {
                    nodeToPkgArtifact.put(nodeName, new String[]{parts[0], parts[1]});
                } else {
                    nodeToPkgArtifact.put(nodeName, new String[]{"unknown_pkg", fromRef});
                }
            }
        }
        return nodeToPkgArtifact;
    }

    /**
     * Transforms .rossystem content (and optional .ros2 content for types) into a SysMLResult.
     */
    public SysMLResult transformText(String rossystemContent, String ros2Content) {
        SysMLResult result = new SysMLResult();

        Map<String, String> artifactIfaceToRosType = parseRos2InterfaceTypes(ros2Content);

        // Parse rossystem content
        java.util.regex.Pattern sysNamePattern = java.util.regex.Pattern.compile("^\\s*([a-zA-Z0-9_]+):", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher sysNameMatcher = sysNamePattern.matcher(rossystemContent);
        if (sysNameMatcher.find()) {
            result.systemName = sysNameMatcher.group(1);
        } else {
            result.systemName = "test_system";
        }
        result.packageName = result.systemName + "_architecture";

        java.util.regex.Pattern fromFilePattern = java.util.regex.Pattern.compile("fromFile:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher fromFileMatcher = fromFilePattern.matcher(rossystemContent);
        if (fromFileMatcher.find()) {
            result.fromFile = fromFileMatcher.group(1);
        }

        Map<String, ModeletTypeResult> modeletTypeMap = new LinkedHashMap<>();

        // Parse nodes block
        java.util.regex.Pattern nodeBlockPattern = java.util.regex.Pattern.compile("\"?([a-zA-Z0-9_]+)\"?:[\\s\\n]+from:\\s*\"([^\"]+)\"([\\s\\S]*?)(?=(?:\"?[a-zA-Z0-9_]+\"?:[\\s\\n]+from:)|connections:|\\Z)");
        java.util.regex.Matcher nodeMatcher = nodeBlockPattern.matcher(rossystemContent);

        while (nodeMatcher.find()) {
            String nodeInstance = nodeMatcher.group(1);
            String fromRef = nodeMatcher.group(2);
            String rest = nodeMatcher.group(3);

            EngineResult engine = new EngineResult();
            engine.instanceName = nodeInstance;
            engine.defName = capitalize(nodeInstance) + "Engine";

            String[] fromParts = fromRef.split("\\.");
            if (fromParts.length >= 2) {
                engine.rosPackage = fromParts[0];
                engine.rosArtifact = fromParts[1];
            } else {
                engine.rosPackage = "unknown_pkg";
                engine.rosArtifact = fromRef;
            }

            java.util.regex.Pattern nsPattern = java.util.regex.Pattern.compile("namespace:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher nsMatcher = nsPattern.matcher(rest);
            if (nsMatcher.find()) {
                engine.rosNamespace = nsMatcher.group(1);
            }
            result.engines.add(engine);

            ExertResult exert = new ExertResult();
            exert.name = capitalize(nodeInstance) + "Exert";
            exert.engineDefName = engine.defName;
            exert.engineInstanceName = engine.instanceName;

            java.util.regex.Pattern ifaceLinePattern = java.util.regex.Pattern.compile("-\\s*\"?([a-zA-Z0-9_]+)\"?:\\s*(pub->|sub->|ss->|sc->|as->|ac->)\\s*\"([^\"]+)\"");
            java.util.regex.Matcher ifaceLineMatcher = ifaceLinePattern.matcher(rest);

            while (ifaceLineMatcher.find()) {
                String ifaceName = ifaceLineMatcher.group(1);
                String dir = ifaceLineMatcher.group(2);
                String ifaceFromRef = ifaceLineMatcher.group(3);

                String rosType = artifactIfaceToRosType.get(ifaceFromRef);
                if (rosType == null) {
                    rosType = artifactIfaceToRosType.get(ifaceName);
                }
                if (rosType == null) {
                    rosType = inferRosTypeFromInterfaceName(ifaceFromRef.contains("::") ? ifaceFromRef.substring(ifaceFromRef.indexOf("::") + 2) : ifaceName);
                }
                String shortTypeName = getShortTypeName(rosType);

                if (!modeletTypeMap.containsKey(rosType)) {
                    ModeletTypeResult mtr = new ModeletTypeResult();
                    mtr.name = shortTypeName;
                    mtr.rosType = rosType;
                    modeletTypeMap.put(rosType, mtr);
                }

                ExertParamResult param = new ExertParamResult();
                param.name = ifaceName;
                param.modeletTypeName = shortTypeName;
                param.isMultiple = false;

                if ("pub->".equals(dir) || "sc->".equals(dir) || "ac->".equals(dir)) {
                    exert.outParams.add(param);
                } else {
                    exert.inParams.add(param);
                }
            }
            result.exerts.add(exert);
        }

        result.modeletTypes.addAll(modeletTypeMap.values());

        // Parse connections block
        java.util.regex.Pattern connPattern = java.util.regex.Pattern.compile("-\\s*\\[\"?([a-zA-Z0-9_]+)\"?\\s*,\\s*\"?([a-zA-Z0-9_]+)\"?\\]");
        java.util.regex.Matcher connMatcher = connPattern.matcher(rossystemContent);

        while (connMatcher.find()) {
            String fromIface = connMatcher.group(1);
            String toIface = connMatcher.group(2);

            FlowResult flow = new FlowResult();
            for (ExertResult exert : result.exerts) {
                for (ExertParamResult p : exert.outParams) {
                    if (p.name.equals(fromIface)) {
                        flow.sourceExertInstance = exert.engineInstanceName + "Exert";
                        flow.sourceParam = p.name;
                    }
                }
                for (ExertParamResult p : exert.inParams) {
                    if (p.name.equals(toIface)) {
                        flow.targetExertInstance = exert.engineInstanceName + "Exert";
                        flow.targetParam = p.name;
                    }
                }
            }
            result.flows.add(flow);
        }

        return result;
    }

    /**
     * Generates valid SysML v2 textual representation from SysMLResult.
     */
    public String generateSysMLText(SysMLResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("//****************************************************************************/\n");
        sb.append("//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //\n");
        sb.append("//  Licensed under the Apache License, Version 2.0                           //\n");
        sb.append("//****************************************************************************/\n\n");
        sb.append("package ").append(result.packageName).append(" {\n");
        sb.append("    private import CSCore::*;\n");
        sb.append("    private import CSRosBridge::*;\n\n");

        sb.append("    // ─── Modelet Types ───\n");
        for (ModeletTypeResult type : result.modeletTypes) {
            sb.append("    @RosTypeMapping { rosType = \"").append(type.rosType).append("\"; }\n");
            sb.append("    part def ").append(type.name).append(" specializes Modelet;\n\n");
        }

        sb.append("    // ─── Engine Definitions ───\n");
        for (EngineResult engine : result.engines) {
            sb.append("    @RosArtifactMapping { rosPackage = \"").append(engine.rosPackage)
              .append("\"; rosArtifact = \"").append(engine.rosArtifact).append("\"");
            if (engine.rosNamespace != null) {
                sb.append("; rosNamespace = \"").append(engine.rosNamespace).append("\"");
            }
            sb.append("; }\n");
            sb.append("    part def ").append(engine.defName).append(" specializes Engine;\n\n");
        }

        sb.append("    // ─── Exert Definitions ───\n");
        for (ExertResult exert : result.exerts) {
            sb.append("    action def ").append(exert.name).append(" specializes Exert {\n");
            for (int i = 0; i < exert.inParams.size(); i++) {
                ExertParamResult param = exert.inParams.get(i);
                sb.append("        in ").append(param.name).append(" : ").append(param.modeletTypeName);
                if (i == 0) {
                    sb.append(" subsets Exert::modelets");
                }
                sb.append(";\n");
            }
            sb.append("        in ref engine : ").append(exert.engineDefName).append(" redefines Exert::engine;\n");
            for (int i = 0; i < exert.outParams.size(); i++) {
                ExertParamResult param = exert.outParams.get(i);
                sb.append("        out ").append(param.name).append(" : ").append(param.modeletTypeName);
                if (i == 0) {
                    sb.append(" subsets Exert::output");
                }
                sb.append(";\n");
            }
            sb.append("    }\n\n");
        }

        sb.append("    // ─── System Composition ───\n");
        sb.append("    @RosSystemMapping { systemName = \"").append(result.systemName).append("\"");
        if (result.fromFile != null) {
            sb.append("; fromFile = \"").append(result.fromFile).append("\"");
        }
        sb.append("; }\n");
        sb.append("    part def ").append(capitalize(result.systemName)).append(" {\n");
        for (EngineResult engine : result.engines) {
            sb.append("        part ").append(engine.instanceName).append(" : ").append(engine.defName).append(";\n");
        }
        sb.append("\n");
        for (ExertResult exert : result.exerts) {
            sb.append("        perform action ").append(exert.engineInstanceName).append("Exert : ").append(exert.name).append(" {\n");
            sb.append("            in ref engine = ").append(exert.engineInstanceName).append(";\n");
            sb.append("        }\n");
        }
        sb.append("\n");
        for (FlowResult flow : result.flows) {
            sb.append("        flow from ").append(flow.sourceExertInstance).append(".").append(flow.sourceParam)
              .append(" to ").append(flow.targetExertInstance).append(".").append(flow.targetParam).append(";\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            java.lang.System.err.println("Usage: RosSystem2SysMLTransformer <input.rossystem> [nodes.ros2]");
            java.lang.System.exit(1);
        }
        try {
            String rossystemContent = java.nio.file.Files.readString(java.nio.file.Paths.get(args[0]));
            String ros2Content = args.length > 1 ? java.nio.file.Files.readString(java.nio.file.Paths.get(args[1])) : null;
            RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
            SysMLResult result = transformer.transformText(rossystemContent, ros2Content);
            java.lang.System.out.println(transformer.generateSysMLText(result));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
