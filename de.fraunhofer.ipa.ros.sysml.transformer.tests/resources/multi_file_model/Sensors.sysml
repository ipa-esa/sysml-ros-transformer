// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package Sensors {
    private import CSRosBridge::*;
    private import SensorTypes::*;

    @RosArtifactMapping { rosPackage = "test_drivers"; rosArtifact = "camera_driver"; }
    part def CameraEngine specializes Engine;

    action def CameraCapture specializes Exert {
        in ref modelets : Modelet[*] redefines Exert::modelets;
        in ref engine : CameraEngine redefines Exert::engine;
        out image : Image subsets Exert::output;
    }
}
