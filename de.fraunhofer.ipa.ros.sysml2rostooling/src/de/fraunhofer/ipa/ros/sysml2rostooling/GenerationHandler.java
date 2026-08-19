// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package de.fraunhofer.ipa.ros.sysml2rostooling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.builder.EclipseResourceFileSystemAccess2;
import org.eclipse.xtext.generator.OutputConfiguration;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.fraunhofer.ipa.ros.sysml2rostooling.parser.SysMLParser;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLModel;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult;
import de.fraunhofer.ipa.ros.sysml2rostooling.generator.RosSystemTextGenerator;

public class GenerationHandler extends AbstractHandler implements IHandler {
    @Inject
    private Provider<EclipseResourceFileSystemAccess2> fileAccessProvider;

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
                
                try {
                    String filePath = file.getLocation().toOSString();
                    SysMLParser parser = new SysMLParser();
                    SysMLModel model = parser.parse(Arrays.asList(filePath));
                    
                    SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
                    java.util.List<RosSystemResult> results = transformer.transform(model);
                    
                    RosSystemTextGenerator generator = new RosSystemTextGenerator();
                    for (RosSystemResult result : results) {
                        String outputFileName = result.getSystemName() + ".rossystem";
                        fsa.generateFile(outputFileName, generator.generate(result));
                    }
                } catch (Exception e) {
                    throw new ExecutionException("Error generating .rossystem from SysML", e);
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
