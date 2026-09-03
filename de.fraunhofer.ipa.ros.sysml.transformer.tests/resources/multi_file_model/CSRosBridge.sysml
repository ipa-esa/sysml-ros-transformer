// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package CSRosBridge {
    metadata def RosArtifactMapping {
        attribute rosPackage : String;
        attribute rosArtifact : String;
        attribute rosNamespace : String [0..1];
    }

    metadata def RosTypeMapping {
        attribute rosType : String;
    }

    metadata def RosInterfaceMapping {
        attribute interfaceName : String;
    }

    metadata def RosSystemMapping {
        attribute systemName : String;
        attribute fromFile : String [0..1];
    }
}
