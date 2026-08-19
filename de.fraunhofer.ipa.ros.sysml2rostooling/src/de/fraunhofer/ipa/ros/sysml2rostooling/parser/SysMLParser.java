// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Regex-based parser for SysML v2 textual syntax.
 */
public class SysMLParser {

    private static final Pattern PKG_PATTERN = Pattern.compile("(?:^|\\s)package\\s+(\\w+)\\s*\\{");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?:private\\s+)?import\\s+([\\w:]+)::\\*\\s*;");
    private static final Pattern METADATA_PATTERN = Pattern.compile("@(\\w+)\\s*\\{([^}]*)\\}");
    private static final Pattern META_ATTR_PATTERN = Pattern.compile("(\\w+)\\s*=\s*\"([^\"]*)\"");
    private static final Pattern PART_DEF_WITH_SUPER_PATTERN = Pattern.compile("(?:abstract\\s+)?part\\s+def\\s+(\\w+)\\s*(?:specializes|:>)\\s+([\\w,\\s]+)\\s*(?:\\{|;)");
    private static final Pattern PART_DEF_NO_SUPER_PATTERN = Pattern.compile("(?:abstract\\s+)?part\\s+def\\s+(\\w+)\\s*\\{");
    private static final Pattern ACTION_DEF_PATTERN = Pattern.compile("action\\s+def\\s+(\\w+)\\s*(?:specializes\\s+([\\w,\\s]+))?\\s*\\{");
    private static final Pattern PARAM_PATTERN = Pattern.compile("(in|out)\\s+(?:ref\\s+)?(?:attribute\\s+)?(\\w+)\\s*:\\s*(\\w+)(?:\\[([^\\]]*)\\])?\\s*(?:redefines\\s+([\\w:]+))?\\s*(?:subsets\\s+([\\w:]+))?");
    private static final Pattern PART_USAGE_PATTERN = Pattern.compile("part\\s+(\\w+)\\s*:\\s*(\\w+)(?:\\[([^\\]]*)\\])?\\s*;");
    private static final Pattern FLOW_PATTERN = Pattern.compile("flow\\s+(?:of\\s+\\w+\\s+)?from\\s+(\\w+)\\.(\\w+)\\s+to\\s+(\\w+)\\.(\\w+)");
    private static final Pattern ACTION_USAGE_PATTERN = Pattern.compile("perform\\s+action\\s+(\\w+)\\s*:\\s*(\\w+)");

    public SysMLModel parse(List<String> filePaths) throws IOException {
        SysMLModel model = new SysMLModel();
        for (String path : filePaths) {
            String content = Files.readString(Paths.get(path));
            parseContent(content, path, model);
        }
        model.resolveImports();
        return model;
    }

    public SysMLModel parseContent(String content, String fileName, SysMLModel model) {
        String stripped = stripComments(content);
        Matcher pkgMatcher = PKG_PATTERN.matcher(stripped);
        int searchStart = 0;
        while (pkgMatcher.find(searchStart)) {
            String pkgName = pkgMatcher.group(1);
            int openBracePos = pkgMatcher.end() - 1;
            int closeBracePos = findMatchingBrace(stripped, openBracePos);
            if (closeBracePos != -1) {
                String pkgContent = stripped.substring(openBracePos + 1, closeBracePos);
                parsePackage(pkgContent, pkgName, model);
                searchStart = closeBracePos + 1;
            } else {
                searchStart = pkgMatcher.end();
            }
        }
        return model;
    }

    private String stripComments(String content) {
        // Strip block comments
        String result = content.replaceAll("/\\*.*?\\*/", "");
        // Strip line comments
        result = result.replaceAll("//.*", "");
        return result;
    }

    private void parsePackage(String packageContent, String packageName, SysMLModel model) {
        SysMLPackage pkg = new SysMLPackage();
        pkg.setName(packageName);
        
        Matcher impMatcher = IMPORT_PATTERN.matcher(packageContent);
        while (impMatcher.find()) {
            pkg.addImport(impMatcher.group(1));
        }

        List<SysMLMetadata> pendingMetadata = new ArrayList<>();
        int i = 0;
        while (i < packageContent.length()) {
            Matcher metaMatcher = METADATA_PATTERN.matcher(packageContent.substring(i));
            Matcher partDefSuperMatcher = PART_DEF_WITH_SUPER_PATTERN.matcher(packageContent.substring(i));
            Matcher partDefNoSuperMatcher = PART_DEF_NO_SUPER_PATTERN.matcher(packageContent.substring(i));
            Matcher actionDefMatcher = ACTION_DEF_PATTERN.matcher(packageContent.substring(i));
            Matcher flowMatcher = FLOW_PATTERN.matcher(packageContent.substring(i));

            int metaIdx = metaMatcher.find() ? metaMatcher.start() : Integer.MAX_VALUE;
            int partSuperIdx = partDefSuperMatcher.find() ? partDefSuperMatcher.start() : Integer.MAX_VALUE;
            int partNoSuperIdx = partDefNoSuperMatcher.find() ? partDefNoSuperMatcher.start() : Integer.MAX_VALUE;
            int actionIdx = actionDefMatcher.find() ? actionDefMatcher.start() : Integer.MAX_VALUE;
            int flowIdx = flowMatcher.find() ? flowMatcher.start() : Integer.MAX_VALUE;

            int minIdx = Math.min(Math.min(Math.min(metaIdx, partSuperIdx), Math.min(partNoSuperIdx, actionIdx)), flowIdx);

            if (minIdx == Integer.MAX_VALUE) {
                break;
            }

            if (minIdx == metaIdx) {
                pendingMetadata.add(parseMetadata(metaMatcher.group(0)));
                i += metaMatcher.end();
            } else if (minIdx == partSuperIdx) {
                String name = partDefSuperMatcher.group(1);
                String supers = partDefSuperMatcher.group(2);
                int startBrace = packageContent.indexOf('{', i + partDefSuperMatcher.start());
                int endPos = i + partDefSuperMatcher.end();
                if (startBrace != -1 && startBrace < packageContent.indexOf(';', i + partDefSuperMatcher.start())) {
                    int endBrace = findMatchingBrace(packageContent, startBrace);
                    if (endBrace != -1) {
                        String body = packageContent.substring(startBrace + 1, endBrace);
                        SysMLPartDef def = parsePartDefBody(name, supers, body, pendingMetadata, pkg);
                        pkg.addPartDef(def);
                        endPos = endBrace + 1;
                    }
                } else {
                    SysMLPartDef def = parsePartDefBody(name, supers, "", pendingMetadata, pkg);
                    pkg.addPartDef(def);
                }
                pendingMetadata.clear();
                i = endPos;
            } else if (minIdx == partNoSuperIdx) {
                String name = partDefNoSuperMatcher.group(1);
                int startBrace = packageContent.indexOf('{', i + partDefNoSuperMatcher.start());
                int endPos = i + partDefNoSuperMatcher.end();
                if (startBrace != -1) {
                    int endBrace = findMatchingBrace(packageContent, startBrace);
                    if (endBrace != -1) {
                        String body = packageContent.substring(startBrace + 1, endBrace);
                        SysMLPartDef def = parsePartDefBody(name, null, body, pendingMetadata, pkg);
                        pkg.addPartDef(def);
                        endPos = endBrace + 1;
                    }
                }
                pendingMetadata.clear();
                i = endPos;
            } else if (minIdx == actionIdx) {
                String name = actionDefMatcher.group(1);
                String supers = actionDefMatcher.group(2);
                int startBrace = packageContent.indexOf('{', i + actionDefMatcher.start());
                int endPos = i + actionDefMatcher.end();
                if (startBrace != -1) {
                    int endBrace = findMatchingBrace(packageContent, startBrace);
                    if (endBrace != -1) {
                        String body = packageContent.substring(startBrace + 1, endBrace);
                        SysMLActionDef def = parseActionDefBody(name, supers, body, pendingMetadata);
                        pkg.addActionDef(def);
                        endPos = endBrace + 1;
                    }
                }
                pendingMetadata.clear();
                i = endPos;
            } else if (minIdx == flowIdx) {
                SysMLFlow flow = new SysMLFlow(flowMatcher.group(1), flowMatcher.group(2), flowMatcher.group(3), flowMatcher.group(4));
                pkg.addFlow(flow);
                i += flowMatcher.end();
            }
        }
        model.addPackage(pkg);
    }

    private SysMLMetadata parseMetadata(String content) {
        Matcher m = METADATA_PATTERN.matcher(content);
        if (m.find()) {
            SysMLMetadata meta = new SysMLMetadata();
            meta.setName(m.group(1));
            String attrs = m.group(2);
            Matcher attrMatcher = META_ATTR_PATTERN.matcher(attrs);
            while (attrMatcher.find()) {
                meta.setAttribute(attrMatcher.group(1), attrMatcher.group(2));
            }
            return meta;
        }
        return null;
    }

    private SysMLPartDef parsePartDefBody(String name, String supers, String body, List<SysMLMetadata> metadata, SysMLPackage pkg) {
        SysMLPartDef def = new SysMLPartDef();
        def.setName(name);
        if (supers != null) {
            for (String s : supers.split(",")) {
                def.addSuperType(s.trim());
            }
        }
        for (SysMLMetadata meta : metadata) {
            def.addMetadata(meta);
        }

        Matcher partUsgMatcher = PART_USAGE_PATTERN.matcher(body);
        while (partUsgMatcher.find()) {
            SysMLPartUsage usage = new SysMLPartUsage();
            usage.setName(partUsgMatcher.group(1));
            usage.setTypeName(partUsgMatcher.group(2));
            usage.setMultiplicity(partUsgMatcher.group(3));
            def.addPartUsage(usage);
        }

        Matcher actionUsgMatcher = ACTION_USAGE_PATTERN.matcher(body);
        while (actionUsgMatcher.find()) {
            SysMLActionUsage usage = new SysMLActionUsage();
            usage.setName(actionUsgMatcher.group(1));
            usage.setTypeName(actionUsgMatcher.group(2));
            def.addActionUsage(usage);
        }

        Matcher flowMatcher = FLOW_PATTERN.matcher(body);
        while (flowMatcher.find()) {
            SysMLFlow flow = new SysMLFlow(flowMatcher.group(1), flowMatcher.group(2), flowMatcher.group(3), flowMatcher.group(4));
            pkg.addFlow(flow);
        }

        return def;
    }

    private SysMLActionDef parseActionDefBody(String name, String supers, String body, List<SysMLMetadata> metadata) {
        SysMLActionDef def = new SysMLActionDef();
        def.setName(name);
        if (supers != null) {
            for (String s : supers.split(",")) {
                def.addSuperType(s.trim());
            }
        }
        for (SysMLMetadata meta : metadata) {
            def.addMetadata(meta);
        }

        // Action can have pending metadata before parameters, but for simplicity we associate it here:
        // Actually we need to match metadata before param
        List<SysMLMetadata> pendingParamMeta = new ArrayList<>();
        int i = 0;
        while (i < body.length()) {
            Matcher metaMatcher = METADATA_PATTERN.matcher(body.substring(i));
            Matcher paramMatcher = PARAM_PATTERN.matcher(body.substring(i));
            
            int metaIdx = metaMatcher.find() ? metaMatcher.start() : Integer.MAX_VALUE;
            int paramIdx = paramMatcher.find() ? paramMatcher.start() : Integer.MAX_VALUE;
            
            if (Math.min(metaIdx, paramIdx) == Integer.MAX_VALUE) {
                break;
            }
            
            if (metaIdx < paramIdx) {
                pendingParamMeta.add(parseMetadata(metaMatcher.group(0)));
                i += metaMatcher.end();
            } else {
                SysMLParameter param = new SysMLParameter();
                String dir = paramMatcher.group(1);
                param.setName(paramMatcher.group(2));
                param.setTypeName(paramMatcher.group(3));
                param.setMultiplicity(paramMatcher.group(4));
                param.setRedefines(paramMatcher.group(5));
                param.setSubsets(paramMatcher.group(6));
                for (SysMLMetadata m : pendingParamMeta) {
                    param.addMetadata(m);
                }
                if ("in".equals(dir)) {
                    def.addInParam(param);
                } else if ("out".equals(dir)) {
                    def.addOutParam(param);
                }
                pendingParamMeta.clear();
                i += paramMatcher.end();
            }
        }
        return def;
    }

    private int findMatchingBrace(String content, int openPos) {
        int braces = 1;
        for (int i = openPos + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') braces++;
            else if (c == '}') {
                braces--;
                if (braces == 0) return i;
            }
        }
        return -1;
    }
}
