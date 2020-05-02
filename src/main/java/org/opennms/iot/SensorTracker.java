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

package org.opennms.iot;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.opennms.iot.handlers.PolarH7Handler;
import org.opennms.iot.handlers.TICC2650Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;

public class SensorTracker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(SensorTracker.class);

    private final BLEExporterImpl bleExporterSvc;

    private final String sensorMac;
    private Thread thread;

    public SensorTracker(BLEExporterImpl bleExporterSvc, String sensorMac) {
        this.bleExporterSvc = Objects.requireNonNull(bleExporterSvc);
        this.sensorMac = Objects.requireNonNull(sensorMac);
    }

    public void start() {
        thread = new Thread(this);
    }

    @Override
    public void run() {
        try {
            boolean first = true;
            while(true) {
                if (first) {
                    first = false;
                } else {
                   Thread.sleep(5000);
                }

                BluetoothDevice sensor = Bluetooth.getDevice(sensorMac);
                if (sensor == null) {
                    LOG.warn("No sensor found with the provided address: {}", sensorMac);
                    continue;
                }
                LOG.info("Found sensor: {}", sensorMac);

                if (sensor.connect())
                    LOG.info("Connected to sensor: {}", sensorMac);
                else {
                    LOG.warn("Could not connect to sensor: {}", sensorMac);
                    continue;
                }

                Handler handler;
                // FIXME - we need a nicer way to register these
                if (TICC2650Handler.handles(sensor)) {
                    handler = new TICC2650Handler(sensor);
                } else if (PolarH7Handler.handles(sensor)) {
                    handler = new PolarH7Handler(sensor);
                } else {
                    throw new UnsupportedOperationException("Unsupported sensor :(");
                }

                handler.registerConsumer(bleExporterSvc::broadcast);
                handler.startGatheringData();
            }
        } catch (InterruptedException e) {
            LOG.info("Interrupted. Stopping tracking thread for: {}", sensorMac);
        }
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                LOG.warn("Thread for sensor {} did not exit in time.", sensorMac);
            }
        }
    }


}
