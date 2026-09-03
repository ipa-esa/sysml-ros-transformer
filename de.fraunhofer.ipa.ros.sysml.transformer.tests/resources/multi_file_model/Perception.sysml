// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package Perception {
    private import CSRosBridge::*;
    private import SensorTypes::*;

    @RosArtifactMapping { rosPackage = "test_perception"; rosArtifact = "object_detector"; }
    part def DetectorEngine specializes Engine;

    action def DetectObjects specializes Exert {
        in image : Image subsets Exert::modelets;
        in ref engine : DetectorEngine redefines Exert::engine;
        out detections : Detection2DArray subsets Exert::output;
    }
}
