// Copyright (c) 2022-2026 The CORESENSE Consortium. Apache License 2.0.

package SensorTypes {
    private import CSRosBridge::*;

    @RosTypeMapping { rosType = "sensor_msgs/msg/Image"; }
    part def Image specializes Modelet;

    @RosTypeMapping { rosType = "vision_msgs/msg/Detection2DArray"; }
    part def Detection2DArray specializes Modelet;
}
