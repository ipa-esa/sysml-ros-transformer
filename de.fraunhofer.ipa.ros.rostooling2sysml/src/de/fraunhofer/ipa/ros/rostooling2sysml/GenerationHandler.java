//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.builder.EclipseResourceFileSystemAccess2;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;
import org.eclipse.xtext.generator.OutputConfiguration;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import system.System;
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer;
import de.fraunhofer.ipa.ros.rostooling2sysml.transform.RosSystem2SysMLTransformer.SysMLResult;
import de.fraunhofer.ipa.ros.rostooling2sysml.generator.SysMLTextGenerator;

public class GenerationHandler extends AbstractHandler implements IHandler {

    @Inject(optional = true)
    private Provider<EclipseResourceFileSystemAccess2> fileAccessProvider;

    @Inject(optional = true)
    private IResourceDescriptions resourceDescriptions;

    @Inject(optional = true)
    private IResourceSetProvider resourceSetProvider;

    static Map<String, OutputConfiguration> getOutputConfigurationsAsMap(IOutputConfigurationProvider provider) {
        Map<String, OutputConfiguration> outputs = new HashMap<>();
        for (OutputConfiguration c : provider.getOutputConfigurations()) {
            outputs.put(c.getName(), c);
        }
        return outputs;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        java.lang.System.out.println("[RosTooling2SysML] GenerationHandler.execute() invoked.");
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        IFile selectedFile = null;

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object firstElement = structuredSelection.getFirstElement();
            if (firstElement instanceof IFile) {
                selectedFile = (IFile) firstElement;
            } else if (firstElement instanceof IAdaptable) {
                selectedFile = ((IAdaptable) firstElement).getAdapter(IFile.class);
            }
        }

        if (selectedFile == null) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null && editor.getEditorInput() instanceof IFileEditorInput) {
                selectedFile = ((IFileEditorInput) editor.getEditorInput()).getFile();
            }
        }

        if (selectedFile == null) {
            MessageDialog.openWarning(
                HandlerUtil.getActiveShell(event),
                "RosTooling to SysML",
                "Please select a .rossystem file in the workspace to perform the transformation."
            );
            return null;
        }

        IFile file = selectedFile;
        IProject project = file.getProject();

        try {
            java.lang.System.out.println("[RosTooling2SysML] Processing file: " + file.getFullPath());

            // 1. Discover all .ros2 and .ros model files in the project to resolve referenced types
            List<IFile> rosModelFiles = new ArrayList<>();
            if (project != null && project.isAccessible()) {
                project.accept(resource -> {
                    if (resource instanceof IFolder) {
                        String name = resource.getName();
                        if (name.startsWith(".") || "target".equals(name) || "build".equals(name) || "bin".equals(name)) {
                            return false;
                        }
                    } else if (resource instanceof IFile) {
                        String ext = resource.getFileExtension();
                        if ("ros2".equals(ext) || "ros".equals(ext)) {
                            rosModelFiles.add((IFile) resource);
                        }
                    }
                    return true;
                });
            }

            StringBuilder ros2Combined = new StringBuilder();
            for (IFile rosFile : rosModelFiles) {
                if (rosFile.getLocation() != null) {
                    try {
                        ros2Combined.append(Files.readString(Paths.get(rosFile.getLocation().toOSString()))).append("\n");
                    } catch (Exception ignored) {}
                }
            }

            String rossystemContent = (file.getLocation() != null)
                ? Files.readString(Paths.get(file.getLocation().toOSString()))
                : "";

            RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
            SysMLTextGenerator generator = new SysMLTextGenerator();
            boolean generated = false;
            String generatedFileName = null;

            // Attempt 1: Load via Xtext/EMF ResourceSet
            try {
                ResourceSet rs = (resourceSetProvider != null && project != null)
                    ? resourceSetProvider.get(project)
                    : new ResourceSetImpl();

                // Pre-load referenced ROS model resources into the ResourceSet for cross-reference resolution
                for (IFile rosFile : rosModelFiles) {
                    try {
                        URI rosUri = URI.createPlatformResourceURI(rosFile.getFullPath().toString(), true);
                        rs.getResource(rosUri, true);
                    } catch (Exception ignored) {}
                }

                URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
                Resource r = rs.getResource(uri, true);

                if (r != null && r.getContents() != null) {
                    for (Object content : r.getContents()) {
                        if (content instanceof System) {
                            System rossystem = (System) content;
                            SysMLResult result = transformer.transform(rossystem, ros2Combined.toString(), rossystemContent);
                            generatedFileName = result.packageName + ".sysml";
                            writeSysMLFile(project, generatedFileName, generator.generate(result).toString());
                            generated = true;
                        }
                    }
                }
            } catch (Exception e) {
                java.lang.System.err.println("[RosTooling2SysML] EMF resource loading notice: " + e.getMessage() + ". Falling back to textual parser.");
            }

            // Attempt 2: Textual fallback if EMF content was not extracted
            if (!generated && !rossystemContent.isEmpty()) {
                SysMLResult result = transformer.transformText(rossystemContent, ros2Combined.toString());
                generatedFileName = result.packageName + ".sysml";
                writeSysMLFile(project, generatedFileName, generator.generate(result).toString());
                generated = true;
            }

            if (project != null) {
                project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            }

            if (generated) {
                MessageDialog.openInformation(
                    HandlerUtil.getActiveShell(event),
                    "RosTooling to SysML Success",
                    "Successfully generated " + generatedFileName + " into src-gen/."
                );
            } else {
                MessageDialog.openWarning(
                    HandlerUtil.getActiveShell(event),
                    "RosTooling to SysML",
                    "No system root found in " + file.getName() + " to transform."
                );
            }

        } catch (Exception e) {
            java.lang.System.err.println("[RosTooling2SysML] Error during transformation: " + e.getMessage());
            e.printStackTrace();
            MessageDialog.openError(
                HandlerUtil.getActiveShell(event),
                "RosTooling to SysML Error",
                "Error generating SysML from .rossystem: " + e.getMessage()
            );
            throw new ExecutionException("Error generating SysML from .rossystem", e);
        }

        return null;
    }

    private void writeSysMLFile(IProject project, String fileName, String content) throws Exception {
        IFolder srcGenFolder = (project != null) ? project.getFolder("src-gen") : null;
        if (srcGenFolder != null && !srcGenFolder.exists()) {
            srcGenFolder.create(true, true, new NullProgressMonitor());
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (srcGenFolder != null) {
            IFile outputFile = srcGenFolder.getFile(fileName);
            ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
            if (outputFile.exists()) {
                outputFile.setContents(stream, true, true, new NullProgressMonitor());
            } else {
                outputFile.create(stream, true, new NullProgressMonitor());
            }
            java.lang.System.out.println("[RosTooling2SysML] Generated: " + outputFile.getFullPath());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
