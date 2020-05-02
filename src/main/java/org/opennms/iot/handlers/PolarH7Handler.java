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

package org.opennms.iot.handlers;

import static org.opennms.iot.Bluetooth.buildSensorFromDevice;
import static org.opennms.iot.handlers.TICC2650Handler.getService;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.opennms.iot.Handler;
import org.opennms.iot.ble.proto.Event;
import org.opennms.iot.ble.proto.FieldValue;
import org.opennms.iot.ble.proto.Metric;
import org.opennms.iot.ble.proto.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

public class PolarH7Handler extends BaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PolarH7Handler.class);

    public static final String H7_HR_SVC = "0000180d-0000-1000-8000-00805f9b34fb";
    public static final String H7_HR_CHAR = "00002a37-0000-1000-8000-00805f9b34fb";

    private BluetoothDevice sensor;

    public PolarH7Handler(BluetoothDevice sensor) {
        this.sensor = Objects.requireNonNull(sensor);
    }

    public static boolean handles(BluetoothDevice sensor) {
        try {
            // FIXME: There is a better way to do this
            return getService(sensor, H7_HR_SVC)  != null;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void startGatheringData() throws Exception {
        BluetoothGattService hrService = getService(sensor, H7_HR_SVC);
        if (hrService == null) {
            throw new IllegalStateException("Could not find HR service.");
        }
        LOG.debug("Found HR service:  {}", hrService.getUUID());

        BluetoothGattCharacteristic hrValue = hrService.find(H7_HR_CHAR);
        hrValue.enableValueNotifications(this::handleHrValue);
    }

    private void handleHrValue(byte[] bytes) {
        long received_at = System.currentTimeMillis();
        byte hr_format_mask = 0x01;
        byte energy_expended_mask = 0x08;
        byte rr_interval_mask = 0x10;

        int offset = 0;
        // skip over flags
        offset += 1;

        // BPM calculation
        Optional<Integer> bpm;
        if (bytes.length >= 2) {
            if ((bytes[0] & hr_format_mask) == 0) {
                // HR is a uint8
                bpm = Optional.of((int)bytes[offset]);
                offset+=1;
            } else {
                // HR is a uint16
                bpm = Optional.of((int)(bytes[offset] & 0xff) | (bytes[offset + 1] << 8));
                offset+=2;
            }
        } else {
            bpm = Optional.empty();
        }
        LOG.info("Got BPM: {}", bpm);

        final Optional<Integer> ene;
        if ((bytes[0] & energy_expended_mask) != 0) {
            ene = Optional.of((bytes[offset] & 0xff) | (bytes[offset+1] << 8));
            offset+=2;
        } else {
            ene = Optional.empty();
        }
        LOG.info("Got ENE: {}", ene);

        final List<Double> rrs = new LinkedList<>();
        if ((bytes[0] & rr_interval_mask) != 0) {
            while(offset < bytes.length) {
                int rr = (bytes[offset] & 0xff) | (bytes[offset+1] << 8);
                double rr_value = rr/1024.0d * 1000.d;
                rrs.add(rr_value);
                offset+=2;
            }
        }
        LOG.info("Got RRs: {}", rrs);

        Event.Builder eventBuilder = Event.newBuilder()
                .setSensor(buildSensorFromDevice(sensor));

        Metric.Builder metricBuilder = Metric.newBuilder()
                .setName("polar-h7")
                .setTimestamp(received_at);
        bpm.ifPresent(val -> {
            metricBuilder.putFields("beats_per_minute", FieldValue.newBuilder().setIntValue(val).build());
        });
        ene.ifPresent(val -> {
            metricBuilder.putFields("energy_expended", FieldValue.newBuilder().setIntValue(val).build());
        });
        int i = 0;
        rrs.forEach(rr -> {
            metricBuilder.putFields("rr" + i, FieldValue.newBuilder().setFloatValue(rr).build());
        });
        eventBuilder.addMetrics(metricBuilder);

        broadcast(eventBuilder.build());
    }
}
