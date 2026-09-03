//****************************************************************************/
//  Copyright (c) 2022-2026 The CORESENSE Consortium.                        //
//  Licensed under the Apache License, Version 2.0                           //
//****************************************************************************/

package de.fraunhofer.ipa.ros.sysml.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.fraunhofer.ipa.ros.sysml2rostooling.parser.SysMLParser;
import de.fraunhofer.ipa.ros.sysml2rostooling.parser.model.SysMLModel;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer;
import de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer.RosSystemResult;

/**
 * Standard JUnit / OSGi Plugin Test for Forward Transformation: SysML v2 -> RosTooling (.rossystem).
 */
public class SysML2RosToolingTest {

    public static Path findResource(String relativePath) {
        // 1. Try OSGi bundle entry if running inside Eclipse OSGi runtime
        try {
            org.osgi.framework.Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("de.fraunhofer.ipa.ros.sysml.transformer.tests");
            if (bundle != null) {
                java.net.URL url = bundle.getEntry("resources/" + relativePath);
                if (url != null) {
                    java.net.URL fileUrl = org.eclipse.core.runtime.FileLocator.toFileURL(url);
                    return Paths.get(fileUrl.toURI());
                }
            }
        } catch (Throwable ignored) {
            // Not running in OSGi runtime or bundle not found, fallback to filesystem
        }

        // 2. Try standard filesystem locations
        Path[] candidates = new Path[] {
            Paths.get("resources").resolve(relativePath),
            Paths.get("test").resolve(relativePath),
            Paths.get("de.fraunhofer.ipa.ros.sysml.transformer.tests/resources").resolve(relativePath),
            Paths.get("src/sysml-ros-transformer/test").resolve(relativePath),
            Paths.get("src/sysml-ros-transformer/de.fraunhofer.ipa.ros.sysml.transformer.tests/resources").resolve(relativePath)
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        return Paths.get(relativePath);
    }

    @Test
    public void testSysMLToRosSystemTransformation() throws IOException {
        Path inputPath = findResource("test_model/test_annotated.sysml");
        Path expectedPath = findResource("expected/test_system.rossystem");

        Assert.assertTrue("Input SysML file must exist at " + inputPath, Files.exists(inputPath));
        Assert.assertTrue("Expected rossystem file must exist at " + expectedPath, Files.exists(expectedPath));

        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(Arrays.asList(inputPath.toString()));
        Assert.assertFalse("Model packages should not be empty", model.getPackages().isEmpty());

        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        List<RosSystemResult> results = transformer.transform(model);
        Assert.assertEquals("Should have generated 1 system", 1, results.size());

        RosSystemResult result = results.get(0);
        String actualOutput = transformer.generateText(result).trim().replace("\r\n", "\n");
        String expectedOutput = Files.readString(expectedPath).trim().replace("\r\n", "\n");

        Assert.assertEquals("Generated .rossystem should match expected output", expectedOutput, actualOutput);
    }

    @Test
    public void testMultiFileSysMLTransformation() throws IOException {
        Path bridgePath = findResource("multi_file_model/CSRosBridge.sysml");
        Path typesPath = findResource("multi_file_model/Types.sysml");
        Path sensorsPath = findResource("multi_file_model/Sensors.sysml");
        Path perceptionPath = findResource("multi_file_model/Perception.sysml");
        Path archPath = findResource("multi_file_model/SystemArchitecture.sysml");
        Path expectedPath = findResource("expected/test_system.rossystem");

        List<String> files = Arrays.asList(
            bridgePath.toString(),
            typesPath.toString(),
            sensorsPath.toString(),
            perceptionPath.toString(),
            archPath.toString()
        );

        for (String f : files) {
            Assert.assertTrue("Multi-file input must exist: " + f, Files.exists(Paths.get(f)));
        }

        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(files);
        Assert.assertEquals("Should have parsed 5 packages from 5 files", 5, model.getPackages().size());

        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        List<RosSystemResult> results = transformer.transform(model);
        Assert.assertEquals("Should have discovered exactly 1 annotated root system", 1, results.size());

        RosSystemResult result = results.get(0);
        String actualOutput = transformer.generateText(result).trim().replace("\r\n", "\n");
        String expectedOutput = Files.readString(expectedPath).trim().replace("\r\n", "\n");

        Assert.assertEquals("Multi-file generated .rossystem should match expected output", expectedOutput, actualOutput);
    }

    @Test
    public void testMissingRosSystemMappingError() throws IOException {
        Path invalidPath = findResource("invalid_model/UnannotatedSystem.sysml");
        Assert.assertTrue("Invalid SysML fixture must exist at " + invalidPath, Files.exists(invalidPath));

        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(Arrays.asList(invalidPath.toString()));

        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();
        try {
            transformer.transform(model);
            Assert.fail("Transformation should have thrown IllegalArgumentException due to missing @RosSystemMapping");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(
                "Error message should clearly instruct the user to annotate the main system with @RosSystemMapping",
                e.getMessage().contains("No system composition annotated with @RosSystemMapping was found") &&
                e.getMessage().contains("@RosSystemMapping(systemName = \"...\")")
            );
        }
    }

    @Test
    public void testTargetedFileTransformationOnlyTransformsSelectedSystem() throws IOException {
        Path singlePath = findResource("test_model/test_annotated.sysml");
        Path bridgePath = findResource("multi_file_model/CSRosBridge.sysml");
        Path typesPath = findResource("multi_file_model/Types.sysml");
        Path sensorsPath = findResource("multi_file_model/Sensors.sysml");
        Path perceptionPath = findResource("multi_file_model/Perception.sysml");
        Path archPath = findResource("multi_file_model/SystemArchitecture.sysml");

        // Parse ALL files (containing multiple distinct systems) into one model
        List<String> allFiles = Arrays.asList(
            singlePath.toString(),
            bridgePath.toString(),
            typesPath.toString(),
            sensorsPath.toString(),
            perceptionPath.toString(),
            archPath.toString()
        );

        SysMLParser parser = new SysMLParser();
        SysMLModel model = parser.parse(allFiles);

        SysML2RosSystemTransformer transformer = new SysML2RosSystemTransformer();

        // 1. Without target file, all systems in the model are found (2 systems)
        List<RosSystemResult> allResults = transformer.transform(model);
        Assert.assertEquals("Without target filter, both systems are discovered", 2, allResults.size());

        // 2. With archPath targeted, only the system in SystemArchitecture.sysml is transformed
        List<RosSystemResult> archResults = transformer.transform(model, archPath.toString());
        Assert.assertEquals("Targeting SystemArchitecture.sysml transforms only 1 system", 1, archResults.size());
        Assert.assertEquals("test_system", archResults.get(0).getSystemName());

        // 3. With singlePath targeted, only the system in test_annotated.sysml is transformed
        List<RosSystemResult> singleResults = transformer.transform(model, singlePath.toString());
        Assert.assertEquals("Targeting test_annotated.sysml transforms only 1 system", 1, singleResults.size());
        Assert.assertEquals("test_system", singleResults.get(0).getSystemName());

        // 4. If user selects a component file with no system root (e.g. Sensors.sysml), clear error is thrown
        try {
            transformer.transform(model, sensorsPath.toString());
            Assert.fail("Targeting a file without @RosSystemMapping should fail");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(
                "Error should mention selected file",
                e.getMessage().contains("No system composition annotated with @RosSystemMapping was found in the selected file")
            );
        }
    }
}

