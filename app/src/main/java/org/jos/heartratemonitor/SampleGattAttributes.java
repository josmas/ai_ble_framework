package org.jos.heartratemonitor;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
  private static HashMap<String, String> attributes = new HashMap();
  public final static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
  public final static String HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
  public final static String DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";
  public final static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
  public final static String BATTERY_LEVEL_SER = "0000180f-0000-1000-8000-00805f9b34fb";
  public final static String BATTERY_LEVEL_CHAR = "00002a19-0000-1000-8000-00805f9b34fb";

  static {
    // Sample Services.
    attributes.put(HEART_RATE_SERVICE, "Heart Rate Service");
    attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");
    attributes.put(BATTERY_LEVEL_SER, "Battery Level Service");
    // Sample Characteristics.
    attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
    attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
    attributes.put(BATTERY_LEVEL_CHAR, "Battery Level Measurement");

  }

  public static String lookup(String uuid, String defaultName) {
    String name = attributes.get(uuid);
    return name == null ? defaultName : name;
  }
}