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

import static org.opennms.iot.Bluetooth.buildSensorFromDevice;
import static org.opennms.iot.Bluetooth.getCharacteristic;
import static org.opennms.iot.Bluetooth.getService;
import static org.opennms.iot.muse.MuseConstants.EEG_UUID_TO_HANDLE_MAP;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_ACCELEROMETER;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_GYRO;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_PPG1;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_PPG2;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_PPG3;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_STREAM_TOGGLE;
import static org.opennms.iot.muse.MuseConstants.MUSE_GATT_ATTR_TELEMETRY;
import static org.opennms.iot.muse.MuseConstants.MUSE_SVC;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.opennms.iot.ble.proto.Event;
import org.opennms.iot.ble.proto.FieldValue;
import org.opennms.iot.ble.proto.Metric;
import org.opennms.iot.handlers.BaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

public class MuseHandler extends BaseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MuseHandler.class);

    private final BluetoothDevice sensor;
    private final EEGHandler eegHandler = new EEGHandler(this);

    public MuseHandler(BluetoothDevice sensor) {
        this.sensor = Objects.requireNonNull(sensor);
    }

    public static boolean handles(BluetoothDevice sensor) {
        try {
            // FIXME: There is a better way to do this
            return getService(sensor, MUSE_SVC)  != null;
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public void startGatheringData() throws InterruptedException {
        BluetoothGattService museSvc = getService(sensor, MUSE_SVC);
        if (museSvc == null) {
            throw new IllegalStateException("Could not find Muse service.");
        }
        BluetoothGattCharacteristic control = getCharacteristic(museSvc, MUSE_GATT_ATTR_STREAM_TOGGLE);
        if (control == null) {
            throw new IllegalStateException("Could not find the correct characteristics.");
        }
        LOG.info("Found the control characteristic. Subscribing.");
        control.enableValueNotifications(this::handle_control);

        LOG.info("Subscribing to telemetry");
        BluetoothGattCharacteristic telemetry = getCharacteristic(museSvc, MUSE_GATT_ATTR_TELEMETRY);
        telemetry.enableValueNotifications(this::handle_telemetry);

        LOG.info("Subscribing to EEG stream.");
        for (String uuid : EEG_UUID_TO_HANDLE_MAP.keySet()) {
            BluetoothGattCharacteristic eeg = getCharacteristic(museSvc, uuid);
            if (eeg == null) {
                throw new IllegalStateException("Could not find the EEG characteristic with UUID: " + uuid);
            }
            eeg.enableValueNotifications((bytes) -> {
                eegHandler.handle_eeg(EEG_UUID_TO_HANDLE_MAP.get(uuid), bytes);
            });
        }
        LOG.info("Done subscribing to EEG stream.");

        LOG.info("Subscribing to accelerometer");
        BluetoothGattCharacteristic acc = getCharacteristic(museSvc, MUSE_GATT_ATTR_ACCELEROMETER);
        acc.enableValueNotifications(this::handle_acc);

        LOG.info("Subscribing to gyro");
        BluetoothGattCharacteristic gyro = getCharacteristic(museSvc, MUSE_GATT_ATTR_GYRO);
        gyro.enableValueNotifications(this::handle_gyro);

        LOG.info("Subscribing to PPG");
        for (String uuid : Arrays.asList(MUSE_GATT_ATTR_PPG1, MUSE_GATT_ATTR_PPG2, MUSE_GATT_ATTR_PPG3)) {
            BluetoothGattCharacteristic ppg = getCharacteristic(museSvc, uuid);
            if (ppg == null) {
                throw new IllegalStateException("Could not find the PPG characteristic with UUID: " + uuid);
            }
            ppg.enableValueNotifications( (bytes) -> {
                handle_ppg(uuid, bytes);
            });
        }
        LOG.info("Done subscribing to PPG stream.");

        LOG.info("Resume streaming, sending 'd' command.");
        control.writeValue(new byte[]{0x02, 0x64, 0x0a});

        LOG.info("Asking for control status.");
        control.writeValue(new byte[]{0x02, 0x73, 0x0a});

        LOG.info("Asking for device info.");
        control.writeValue(new byte[]{0x03, 0x76, 0x31, 0x0a});
    }


    private StringBuilder controlStringBuilder = new StringBuilder();

    /**
     * Handle the incoming messages from the 0x000e handle.
     * Each message is 20 bytes
     * The first byte, call it n, is the length of the incoming string.
     * The rest of the bytes are in ASCII, and only n chars are useful
     * Multiple messages together are a json object (or dictionary in python)
     * If a message has a '}' then the whole dict is finished.
     * Example:
     *    {'key': 'value',
     *      'key2': 'really-long
     *      -value',
     *       'key3': 'value3'}
     *   each line is a message, the 4 messages are a json object.
     *
     * See https://github.com/alexandrebarachant/muse-lsl/blob/71a45b7e062f81ffa23b96e86823a2507b8198fa/muselsl/muse.py#L353
     *
     * @param packet
     */
    public void handle_control(byte[] packet) {
        if (LOG.isInfoEnabled()) {
            LOG.info("handle_control: {}", Arrays.toString(packet));
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(packet);
        int n_chars = (int)byteBuffer.get();

        byte[] stringBytes = new byte[n_chars];
        byteBuffer.get(stringBytes);
        String contents = new String(stringBytes, StandardCharsets.US_ASCII);
        controlStringBuilder.append(contents);
        if (contents.contains("}")) {
            onControlMessage(controlStringBuilder.toString());
            controlStringBuilder = new StringBuilder();
        }
    }

    public void handle_telemetry(byte[] bytes) {
        if (LOG.isInfoEnabled()) {
            LOG.info("handle_telemetry: {}", Arrays.toString(bytes));
        }
    }

    public void handle_acc(byte[] bytes) {
        if (LOG.isInfoEnabled()) {
            LOG.info("handle_acc: {}", Arrays.toString(bytes));
        }
    }

    public void handle_gyro(byte[] bytes) {
        if (LOG.isInfoEnabled()) {
            LOG.info("handle_gyro: {}", Arrays.toString(bytes));
        }
    }

    public void handle_ppg(String uuid, byte[] bytes) {
        if (LOG.isInfoEnabled()) {
            LOG.info("handle_ppg: {}", Arrays.toString(bytes));
        }
    }

    public void onControlMessage(String controlMessage) {
        // hn: device name
        // sn: serial number
        // ma: mac address
        // id:
        // bp: battery percentage
        // ts:
        // ps: preset selected
        // tc:
        // rc: return status, 0 is ok
        //
        // {"hn":"Muse-355A","sn":"1111-1XX1-111X","ma":"00-55-da-b5-35-5a","id":"005f005b 54475016 20313547","bp":100,"ts":0,"ps":81,"tc":19687,"rc":0}

        // ap:
        // sp:
        // tp: firmware type
        // hw: hardware version
        // bn: build number?
        // fw: firmware version?
        // bl:
        // pv: protocol version?
        // rc: return status, 0 is ok
        // {"ap":"headset","sp":"Blackcomb_revB","tp":"consumer","hw":"00.8","bn":1,"fw":"1.0.17","bl":"1.0.17","pv":1,"rc":0}
        LOG.info("Got control message: {}", controlMessage);
    }

    public void broadcastEegSample(EEGSample eegSample) {
        Event.Builder eventBuilder = Event.newBuilder()
                .setSensor(buildSensorFromDevice(sensor));

        int i = 0;
        for (Long ts : eegSample.getTimestamps()) {
            Metric.Builder metricBuilder = Metric.newBuilder()
                    .setName("eeg")
                    .setTimestamp(ts);

            for (Map.Entry<Integer,ChannelSamples> entry : eegSample.getData().entrySet()) {
                int channelIndex = entry.getKey();
                double value = entry.getValue().getValues().get(i);
                metricBuilder.putFields(getChannelFieldName(channelIndex), FieldValue.newBuilder().setFloatValue(value).build());
            }

            eventBuilder.addMetrics(metricBuilder);
        }

        broadcast(eventBuilder.build());
    }

    private static String getChannelFieldName(int channelIndex) {
        switch(channelIndex) {
            case 0:
                return "tp9";
            case 1:
                return "af7";
            case 2:
                return "af8";
            case 3:
                return "tp10";
            case 4:
                return "right_aux";
            default:
                return "unknown";
        }
    }
}
