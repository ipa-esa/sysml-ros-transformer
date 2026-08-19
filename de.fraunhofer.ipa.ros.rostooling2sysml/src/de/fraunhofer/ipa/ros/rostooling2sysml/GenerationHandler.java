//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.rostooling2sysml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.builder.EclipseResourceFileSystemAccess2;
import org.eclipse.xtext.generator.GeneratorContext;
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
    @Inject
    private Provider<EclipseResourceFileSystemAccess2> fileAccessProvider;

    @Inject
    IResourceDescriptions resourceDescriptions;

    @Inject
    IResourceSetProvider resourceSetProvider;

    static Map<String, OutputConfiguration> getOutputConfigurationsAsMap(IOutputConfigurationProvider provider) {
        Map<String, OutputConfiguration> outputs = new HashMap<>();
        for (OutputConfiguration c : provider.getOutputConfigurations()) {
            outputs.put(c.getName(), c);
        }
        return outputs;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object firstElement = structuredSelection.getFirstElement();
            if (firstElement instanceof IFile) {
                IFile file = (IFile) firstElement;
                IProject project = file.getProject();

                final EclipseResourceFileSystemAccess2 fsa = fileAccessProvider.get();
                fsa.setProject(project);
                fsa.setOutputConfigurations(getOutputConfigurationsAsMap(new CustomOutputProvider()));
                fsa.setMonitor(new NullProgressMonitor());

                URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
                ResourceSet rs = resourceSetProvider.get(project);
                Resource r = rs.getResource(uri, true);

                RosSystem2SysMLTransformer transformer = new RosSystem2SysMLTransformer();
                SysMLTextGenerator generator = new SysMLTextGenerator();

                for (Object content : r.getContents()) {
                    if (content instanceof System) {
                        System rossystem = (System) content;
                        SysMLResult result = transformer.transform(rossystem);
                        String outputFileName = result.packageName + ".sysml";
                        fsa.generateFile(outputFileName, generator.generate(result));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
