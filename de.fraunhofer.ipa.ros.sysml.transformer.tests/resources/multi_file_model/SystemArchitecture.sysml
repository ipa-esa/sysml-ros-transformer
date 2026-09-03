// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package SystemArchitecture {
    private import CSRosBridge::*;
    private import Sensors::*;
    private import Perception::*;

    @RosSystemMapping { systemName = "test_system"; fromFile = "test_pkg/launch/test.launch.py"; }
    part def MultiFileTestSystem {
        part camera : CameraEngine;
        part detector : DetectorEngine;

        perform action cameraCapture : CameraCapture {
            in ref engine = camera;
        }

        perform action detectObjects : DetectObjects {
            in ref engine = detector;
        }

        flow from cameraCapture.image to detectObjects.image;
    }
}
