/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.iot.muse;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Adapted from https://github.com/alexandrebarachant/muse-lsl/blob/master/muselsl/constants.py
 */
public class MuseConstants {
    public static final int MUSE_NB_EEG_CHANNELS = 5;
    public static final int MUSE_SAMPLING_EEG_RATE = 256;
    public static final int LSL_EEG_CHUNK = 12;

    public static final int MUSE_NB_PPG_CHANNELS = 3;
    public static final int MUSE_SAMPLING_PPG_RATE = 64;
    public static final int LSL_PPG_CHUNK = 6;

    public static final int MUSE_NB_ACC_CHANNELS = 3;
    public static final int MUSE_SAMPLING_ACC_RATE = 52;
    public static final int LSL_ACC_CHUNK = 1;

    public static final int MUSE_NB_GYRO_CHANNELS = 3;
    public static final int MUSE_SAMPLING_GYRO_RATE = 52;
    public static final int LSL_GYRO_CHUNK = 1;

    public static final String MUSE_SVC = "0000fe8d-0000-1000-8000-00805f9b34fb";

    public static final String MUSE_GATT_ATTR_STREAM_TOGGLE = "273e0001-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_TP9 = "273e0003-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_AF7 = "273e0004-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_AF8 = "273e0005-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_TP10 = "273e0006-4c4d-454d-96be-f03bac821358";
    public static final String  MUSE_GATT_ATTR_RIGHTAUX = "273e0007-4c4d-454d-96be-f03bac821358";
    public static final String  MUSE_GATT_ATTR_GYRO = "273e0009-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_ACCELEROMETER = "273e000a-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_TELEMETRY = "273e000b-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_PPG1 = "273e000f-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_PPG2 = "273e0010-4c4d-454d-96be-f03bac821358";
    public static final String MUSE_GATT_ATTR_PPG3 = "273e0011-4c4d-454d-96be-f03bac821358";

    public static final Map<String, Integer> EEG_UUID_TO_HANDLE_MAP = ImmutableMap.<String, Integer>builder()
            .put(MUSE_GATT_ATTR_TP9, 32)
            .put(MUSE_GATT_ATTR_AF7, 35)
            .put(MUSE_GATT_ATTR_AF8, 38)
            .put(MUSE_GATT_ATTR_TP10, 41)
            .put(MUSE_GATT_ATTR_RIGHTAUX, 44)
            .build();

}
