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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

public class CC2650Handler {
    private static final Logger LOG = LoggerFactory.getLogger(CC2650Handler.class);

    private BluetoothDevice sensor;

    public CC2650Handler(BluetoothDevice sensor) {
        this.sensor = Objects.requireNonNull(sensor);
    }

    public void startGatheringHumidity() throws InterruptedException {
        BluetoothGattService humidityService = getService(sensor, Constants.CC2650_HUMIDITY_SVC);
        if (humidityService == null) {
            System.err.println("This device does not have the temperature service we are looking for.");
            sensor.disconnect();
            System.exit(-1);
        }
        LOG.debug("Found humidity service:  {}", humidityService.getUUID());

        BluetoothGattCharacteristic humValue = humidityService.find(Constants.CC2650_HUMIDITY_VALUE_CHAR);
        BluetoothGattCharacteristic humConfig = humidityService.find(Constants.CC2650_HUMIDITY_CONFIG_CHAR);
        BluetoothGattCharacteristic humPeriod = humidityService.find(Constants.CC2650_HUMIDITY_PERIOD_CHAR);

        if (humValue == null || humConfig == null || humPeriod == null) {
            System.err.println("Could not find the correct characteristics.");
            sensor.disconnect();
            System.exit(-1);
        }

        LOG.debug("Found the humidity characteristics");


        byte[] config = { 0x01 };
        humConfig.writeValue(config);

        subscribe(humConfig, (bytes) -> {
            LOG.info("Got callback!");
            onNewHumidityValue(bytes);
        });

        LOG.debug("Subscribing to humidity value changes.");
        Thread t = new Thread(() -> {
            while(true) {
                onNewHumidityValue(humValue.readValue());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Thread interrupted.");
                    return;
                }
            }
        });
        t.start();
    }


    private void onNewHumidityValue(byte[] bytes) {
        LOG.debug("Got new humidity value: {}", bytes);

        int rawTemp = (bytes[0] & 0xff) | (bytes[1] << 8);
        int rawHum  = (bytes[2] & 0xff) | (bytes[3] << 8);

        double temp = (rawTemp / 65536d) * 165 - 40;
        double hum = (rawHum / 65536d) * 100;
        LOG.debug("temperature: {}, humidity: {}", temp, hum);
    }

    public void startGatheringTemperature() throws InterruptedException {
        BluetoothGattService tempService = getService(sensor, Constants.CC2650_TEMPERATURE_SVC);
        if (tempService == null) {
            System.err.println("This device does not have the temperature service we are looking for.");
            sensor.disconnect();
            System.exit(-1);
        }
        LOG.debug("Found temeperature service:  {}", tempService.getUUID());

        BluetoothGattCharacteristic tempValue = tempService.find(Constants.CC2650_TEMPERATURE_VALUE_CHAR);
        BluetoothGattCharacteristic tempConfig = tempService.find(Constants.CC2650_TEMPERATURE_CONFIG_CHAR);
        BluetoothGattCharacteristic tempPeriod = tempService.find(Constants.CC2650_TEMPERATURE_PERIOD_CHAR);

        if (tempValue == null || tempConfig == null || tempPeriod == null) {
            System.err.println("Could not find the correct characteristics.");
            sensor.disconnect();
            System.exit(-1);
        }

        LOG.debug("Found the temperature characteristics");

        /*
         * Turn on the Temperature Service by writing 1 in the configuration characteristic, as mentioned in the PDF
         * mentioned above. We could also modify the update interval, by writing in the period characteristic, but the
         * default 1s is good enough for our purposes.
         */
        byte[] config = { 0x01 };
        tempConfig.writeValue(config);

        subscribe(tempConfig, (bytes) -> {
            LOG.info("Got callback!");
            onNewTempValue(bytes);
        });

        LOG.debug("Subscribing to temperature value changes.");
        Thread t = new Thread(() -> {
            while(true) {
                onNewTempValue(tempValue.readValue());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("Thread interrupted.");
                    return;
                }
            }
        });
        t.start();
    }

    private void onNewTempValue(byte[] bytes) {
        LOG.debug("Got new temp value: {}", bytes);

        /*
         * The temperature service returns the data in an encoded format which can be found in the wiki. Convert the
         * raw temperature format to celsius and print it. Conversion for object temperature depends on ambient
         * according to wiki, but assume result is good enough for our purposes without conversion.
         */
        int objectTempRaw = (bytes[0] & 0xff) | (bytes[1] << 8);
        int ambientTempRaw = (bytes[2] & 0xff) | (bytes[3] << 8);

        float objectTempCelsius = convertCelsius(objectTempRaw);
        float ambientTempCelsius = convertCelsius(ambientTempRaw);

        LOG.info(String.format(" Temp: Object = %fC, Ambient = %fC", objectTempCelsius, ambientTempCelsius));
    }

    static float convertCelsius(int raw) {
        return raw / 128f;
    }

    private static void subscribe(BluetoothGattCharacteristic characteristic, Consumer<byte[]> callback) {
        try {
            characteristic.enableValueNotifications(callback::accept);
        } catch (BluetoothException be) {
            LOG.warn("enabling notifications failed.");
        }
    }

    /*
     * Our device should expose a temperature service, which has a UUID we can find out from the data sheet. The service
     * description of the SensorTag can be found here:
     * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf. The service we are looking for has the
     * short UUID AA00 which we insert into the TI Base UUID: f000XXXX-0451-4000-b000-000000000000
     */
    static BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
        System.out.println("Services exposed by device:");
        BluetoothGattService tempService = null;
        List<BluetoothGattService> bluetoothServices = null;

        boolean first = true;
        do {
            if (first) {
                first = false;
            } else {
                Thread.sleep(4000);
            }
            bluetoothServices = device.getServices();
            if (bluetoothServices == null)
                return null;

            for (BluetoothGattService service : bluetoothServices) {
                System.out.println("UUID: " + service.getUUID());
                if (service.getUUID().equals(UUID))
                    tempService = service;
            }
        } while (bluetoothServices.isEmpty());
        return tempService;
    }
}
