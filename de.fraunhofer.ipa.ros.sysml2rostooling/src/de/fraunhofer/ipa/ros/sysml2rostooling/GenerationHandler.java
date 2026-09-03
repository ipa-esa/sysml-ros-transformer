// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

import de.fraunhofer.ipa.ros.sysml2rostooling.generator.RosSystemTextGenerator;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.SysMLParser;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLModel;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult;

public class GenerationHandler extends AbstractHandler implements IHandler {

    public GenerationHandler() {
        // Default constructor for direct Eclipse command instantiation
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        System.out.println("[SysML2RosTooling] GenerationHandler.execute() invoked.");
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        IFile file = null;

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object firstElement = structuredSelection.getFirstElement();
            if (firstElement instanceof IFile) {
                file = (IFile) firstElement;
            } else if (firstElement instanceof IAdaptable) {
                file = ((IAdaptable) firstElement).getAdapter(IFile.class);
            }
        }

        if (file == null) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null && editor.getEditorInput() instanceof IFileEditorInput) {
                file = ((IFileEditorInput) editor.getEditorInput()).getFile();
            }
        }

        if (file == null) {
            MessageDialog.openWarning(
                HandlerUtil.getActiveShell(event),
                "SysML Transformation",
                "Please select a .sysml file in the workspace to perform the transformation."
            );
            return null;
        }

        IProject project = file.getProject();

        try {
            System.out.println("[SysML2RosTooling] Processing file: " + file.getFullPath());

            List<String> sysmlFiles = new ArrayList<>();
            if (project != null && project.isAccessible()) {
                project.accept(resource -> {
                    if (resource instanceof IFolder) {
                        String name = resource.getName();
                        if (name.startsWith(".") || "target".equals(name) || "build".equals(name) || "bin".equals(name)) {
                            return false;
                        }
                    } else if (resource instanceof IFile && resource.getName().endsWith(".sysml")) {
                        IPath loc = resource.getLocation();
                        if (loc != null) {
                            sysmlFiles.add(loc.toOSString());
                        }
                    }
                    return true;
                });
            }
            if (sysmlFiles.isEmpty()) {
                IPath loc = file.getLocation();
                if (loc != null) {
                    sysmlFiles.add(loc.toOSString());
                }
            }

            System.out.println("[SysML2RosTooling] Discovered " + sysmlFiles.size() + " sysml file(s): " + sysmlFiles);

            SysMLParser parser = new SysMLParser();
            SysMLModel model = parser.parse(sysmlFiles);

            String selectedFilePath = (file.getLocation() != null) ? file.getLocation().toOSString() : file.getFullPath().toString();
            SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
            List<RosSystemResult> results = transformer.transform(model, selectedFilePath);

            IFolder srcGenFolder = (project != null) ? project.getFolder("src-gen") : null;
            if (srcGenFolder != null && !srcGenFolder.exists()) {
                srcGenFolder.create(true, true, new NullProgressMonitor());
            }

            RosSystemTextGenerator generator = new RosSystemTextGenerator();
            for (RosSystemResult result : results) {
                String outputFileName = result.getSystemName() + ".rossystem";
                System.out.println("[SysML2RosTooling] Generating: " + outputFileName);
                String content = generator.generate(result).toString();
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

                if (srcGenFolder != null) {
                    IFile outputFile = srcGenFolder.getFile(outputFileName);
                    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                    if (outputFile.exists()) {
                        outputFile.setContents(stream, true, true, new NullProgressMonitor());
                    } else {
                        outputFile.create(stream, true, new NullProgressMonitor());
                    }
                }
            }

            if (project != null) {
                project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            }

            MessageDialog.openInformation(
                HandlerUtil.getActiveShell(event),
                "SysML Transformation Success",
                "Successfully generated " + results.size() + " .rossystem file(s) into src-gen/."
            );

        } catch (IllegalArgumentException e) {
            System.err.println("[SysML2RosTooling] Validation error: " + e.getMessage());
            MessageDialog.openError(
                HandlerUtil.getActiveShell(event),
                "SysML Transformation Error",
                e.getMessage()
            );
            throw new ExecutionException(e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("[SysML2RosTooling] Error during transformation: " + e.getMessage());
            e.printStackTrace();
            MessageDialog.openError(
                HandlerUtil.getActiveShell(event),
                "SysML Transformation Error",
                "Error generating .rossystem from SysML: " + e.getMessage()
            );
            throw new ExecutionException("Error generating .rossystem from SysML", e);
        }

        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
