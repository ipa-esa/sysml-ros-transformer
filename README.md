# SysML ↔ RosTooling Bi-directional M2M Transformer
[![SysML ↔ RosTooling Bi-directional M2M Transformer Tests](https://github.com/ipa-esa/sysml-ros-transformer/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ipa-esa/sysml-ros-transformer/actions/workflows/ci.yml)

Bi-directional model-to-model transformation between CoreSense SysML v2 models and RosTooling `.rossystem` models.

## Plugins

| Plugin | Direction | Input | Output |
|:---|:---|:---|:---|
| `de.fraunhofer.ipa.ros.sysml2rostooling` | Forward | Annotated `.sysml` with `@Ros*` metadata | `.rossystem` text |
| `de.fraunhofer.ipa.ros.rostooling2sysml` | Reverse | `.rossystem` (EMF via Xtext) | Annotated `.sysml` text |

## Prerequisites

- Eclipse 4.38+ with Xtext 2.39+ and EMF
- RosTooling plugins installed (`de.fraunhofer.ipa.ros`, `de.fraunhofer.ipa.rossystem`, etc.)
- Java 21+

## Usage

### Forward (SysML → .rossystem)
1. Annotate your SysML model with `@RosArtifactMapping`, `@RosTypeMapping`, `@RosSystemMapping` from the `CSRosBridge` package
2. Right-click the `.sysml` file in Eclipse → **Generate .rossystem from SysML**
3. Output appears in `src-gen/`

### Reverse (.rossystem → SysML)
1. Right-click a `.rossystem` file in Eclipse → **Generate SysML architecture from .rossystem**
2. Output appears in `src-gen/`

### Standalone CLI (Forward only)
```bash
java -cp <classpath> de.fraunhofer.ipa.ros.sysml2rostooling.transform.SysML2RosSystemTransformer input.sysml [additional_imports.sysml ...]
```

## Running Tests & CI

Run the automated CI test suite covering both transformation directions:

From the root directory/parent project, run:
```bash
mvn clean verify --batch-mode
```

Alternatively, test this project by running the test project as a JUnit Plugin Test in Eclipse


- **Test 1 (`SysML2RosToolingTest`)**: Transforms `de.fraunhofer.ipa.ros.sysml.transformer.tests/resources/test_model/test_annotated.sysml` and asserts the generated output matches `de.fraunhofer.ipa.ros.sysml.transformer.tests/resources/expected/test_system.rossystem`.
- **Test 2 (`RosTooling2SysMLTest`)**: Transforms `de.fraunhofer.ipa.ros.sysml.transformer.tests/resources/expected/test_system.rossystem` (with `de.fraunhofer.ipa.ros.sysml.transformer.tests/resources/test_ros_models/test_nodes.ros2`) and asserts the generated output matches `de.fraunhofer.ipa.ros.sysml.transformer.tests/resources/expected/test_system_architecture.sysml`.

A GitHub Actions CI workflow is configured in `.github/workflows/ci.yml`.

## License

Apache License 2.0 — Copyright (c) 2022-2026 The CORESENSE Consortium.
