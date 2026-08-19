//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml.transform;

import java.util.*;

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
import ros.AmentPackage;

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
        SysMLResult result = new SysMLResult();
        result.systemName = rossystem.getName();
        result.fromFile = rossystem.getFromFile();
        result.packageName = (result.systemName != null ? result.systemName : "Unknown") + "_architecture";

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
                if (rosNodeRef != null && rosNodeRef.eContainer() instanceof Artifact) {
                    Artifact artifact = (Artifact) rosNodeRef.eContainer();
                    engine.rosArtifact = artifact.getName();
                    if (artifact.eContainer() instanceof Package) {
                        Package pkg = (Package) artifact.eContainer();
                        engine.rosPackage = pkg.getName();
                    }
                }
                result.engines.add(engine);

                // 4. Build Exerts
                ExertResult exert = new ExertResult();
                exert.name = capitalize(engine.instanceName) + "Exert";
                exert.engineDefName = engine.defName;
                exert.engineInstanceName = engine.instanceName;

                for (RosInterface iface : node.getRosinterfaces()) {
                    String rosType = getRosType(iface.getReference());
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
            return pub != null ? getTopicSpecRosType(pub.getMessage()) : null;
        } else if (ref instanceof RosSubscriberReference) {
            Subscriber sub = ((RosSubscriberReference) ref).getFrom();
            return sub != null ? getTopicSpecRosType(sub.getMessage()) : null;
        } else if (ref instanceof RosServiceServerReference) {
            ServiceServer ss = ((RosServiceServerReference) ref).getFrom();
            return ss != null ? getServiceSpecRosType(ss.getService()) : null;
        } else if (ref instanceof RosServiceClientReference) {
            ServiceClient sc = ((RosServiceClientReference) ref).getFrom();
            return sc != null ? getServiceSpecRosType(sc.getService()) : null;
        } else if (ref instanceof RosActionServerReference) {
            ActionServer as = ((RosActionServerReference) ref).getFrom();
            return as != null ? getActionSpecRosType(as.getAction()) : null;
        } else if (ref instanceof RosActionClientReference) {
            ActionClient ac = ((RosActionClientReference) ref).getFrom();
            return ac != null ? getActionSpecRosType(ac.getAction()) : null;
        }
        return null;
    }

    private String getTopicSpecRosType(TopicSpec spec) {
        if (spec == null) return "unknown/msg/Unknown";
        Package pkg = (Package) spec.eContainer();
        return pkg.getName() + "/msg/" + spec.getName();
    }

    private String getServiceSpecRosType(ServiceSpec spec) {
        if (spec == null) return "unknown/srv/Unknown";
        Package pkg = (Package) spec.eContainer();
        return pkg.getName() + "/srv/" + spec.getName();
    }

    private String getActionSpecRosType(ActionSpec spec) {
        if (spec == null) return "unknown/action/Unknown";
        Package pkg = (Package) spec.eContainer();
        return pkg.getName() + "/action/" + spec.getName();
    }

    private String getShortTypeName(String rosType) {
        if (rosType == null) return "Unknown";
        int lastSlash = rosType.lastIndexOf('/');
        return lastSlash >= 0 ? rosType.substring(lastSlash + 1) : rosType;
    }
}
