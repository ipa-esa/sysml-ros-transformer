# SysML ↔ RosTooling Bi-directional M2M Transformer

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

## Building

```bash
mvn clean verify
```

## Installing

```bash
mvn clean install
```

## License

Apache License 2.0 — Copyright (c) 2022-2026 The CORESENSE Consortium.
