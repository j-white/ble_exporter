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
import static org.opennms.iot.muse.MuseConstants.MUSE_SAMPLING_EEG_RATE;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.opennms.iot.ble.proto.Event;
import org.opennms.iot.ble.proto.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// See 10-20 system
// AF7, AF8 - forehead
// TP9, TP10 - behind ears
//
// Ref: https://cdn-images-1.medium.com/max/1000/1*pFYtvublnXnl8hZehCAriw.png
//
/*
     for c in ['TP9', 'AF7', 'AF8', 'TP10', 'Right AUX']:
    eeg_channels.append_child("channel") \
        .append_child_value("label", c) \
        .append_child_value("unit", "microvolts") \
        .append_child_value("type", "EEG")

    def push(data, timestamps, outlet):
      for ii in range(data.shape[1]):
        outlet.push_sample(data[:, ii], timestamps[ii])

     push_eeg = partial(push, outlet=eeg_outlet)
 */
public class EEGHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MuseHandler.class);

    private final MuseHandler parent;

    private int sampleIndexEeg = 0;
    private long regParmsEegTime;
    private double regParmsEegRatio;
    private long lastTimestampEeg = 0;

    private int lastTmEeg = 0;
    private Map<Integer, ChannelSamples> eegData = new HashMap<>();

    public EEGHandler(MuseHandler parent) {
        this.parent = Objects.requireNonNull(parent);

        // initial params for the timestamp correction
        // initial params for the timestamp correction
        sampleIndexEeg = 0;

        regParmsEegTime = System.currentTimeMillis();
        regParmsEegRatio = 1.0d / MUSE_SAMPLING_EEG_RATE;
    }

    public synchronized void handle_eeg(int handle, byte[] packet) {
        if (LOG.isInfoEnabled()) {
            LOG.info("handle_eeg({}): {}", handle, Arrays.toString(packet));
        }
        //  samples are received in this order : 44, 41, 38, 32, 35
        //  wait until we get 35 and call the data callback
        Instant timestamp = Instant.now();
        int index = Math.floorDiv(handle - 32, 3);

        ChannelSamples samples = unpack_eeg_channel(packet);
        samples.setTimestamp(timestamp);
        int tm = samples.getIndex();

        if (lastTmEeg == 0) {
            lastTmEeg = tm - 1;
        }

        // Store the samples
        eegData.put(index, samples);

        if (handle != 35) {
            return;
        }

        if (tm != (lastTmEeg + 1)) {
            LOG.warn("Missing sample {}: {}", tm, lastTmEeg);
        }
        lastTmEeg = tm;

        // Calculate index of time samples
        List<Integer> idxs = IntStream.range(0, 12)
                .mapToObj(k->k+sampleIndexEeg)
                .collect(Collectors.toList());
        sampleIndexEeg += 12;

        // Timestamps are extrapolated backwards based on sampling rate and current time
        List<Long> timestamps = idxs.stream()
                .map(idx -> (long)(regParmsEegRatio * idx) + regParmsEegTime)
                .collect(Collectors.toList());

        // Push the data
        parent.broadcastEegSample(new EEGSample(eegData, timestamps));

        // Save last timestamp for disconnection time
        lastTimestampEeg = timestamps.get(11);

        // Reset sample
        eegData.clear();
    }

    /**
     * Decode data packet of one EEG channel.
     *
     * Each packet is encoded with a 16bit timestamp followed by 12 time
     * samples with a 12 bit resolution.
     *
     * @param bytes
     */
    protected static ChannelSamples unpack_eeg_channel(byte[] bytes) {
        // pattern = "uint:16,uint:12,uint:12,uint:12,uint:12,uint:12,uint:12, \
        //               uint:12,uint:12,uint:12,uint:12,uint:12,uint:12"
        int index = (bytes[0] << 8) | (bytes[1] & 0xFF);

        List<Integer> values = new LinkedList<>();
        // Unpack the 12-bit integers in pairs so we can deal with 3 bytes at a time
        for (int i = 2; i < bytes.length; i+=3) {
            values.add(0xFFF & ((bytes[i] << 4) | ((bytes[i+1] >> 4) & 0x0F)));
            values.add(0xFFF & (((bytes[i+1] << 8) & 0xF00) | (bytes[i+2] & 0xFF)));
        }

        // 12 bits on a 2 mVpp range
        List<Double> data = values.stream()
                .map(v -> 0.48828125 * (v - 2048))
                .collect(Collectors.toList());
        return new ChannelSamples(index, data);
    }

}
