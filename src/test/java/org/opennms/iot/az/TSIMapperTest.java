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

package org.opennms.iot.az;

import org.json.JSONException;
import org.junit.Test;
import org.opennms.iot.ble.proto.Event;
import org.opennms.iot.ble.proto.FieldValue;
import org.opennms.iot.ble.proto.Metric;
import org.opennms.iot.ble.proto.Sensor;
import org.opennms.iot.handlers.PolarH7Handler;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.azure.messaging.eventhubs.EventData;

public class TSIMapperTest {

    @Test
    public void canMapAndMarshalEventToJson() throws JSONException {
        String expectedJson = "{\n" +
                "        \"deviceId\": \"FXXX\",\n" +
                "        \"timestamp\": \"2018-01-17T01:17:00Z\",\n" +
                "        \"series\": [\n" +
                "            {\n" +
                "                \"Flow Rate ft3/s\": 1.0172575712203979,\n" +
                "                \"Engine Oil Pressure psi \": 34.7\n" +
                "            }\n" +
                "        ]\n" +
                "    }";

        Event event = Event.newBuilder()
                .setSensor(Sensor.newBuilder()
                        .setHwAddress("FXXX"))
                .addMetrics(Metric.newBuilder()
                        .setName(PolarH7Handler.METRIC_NAME)
                        .setTimestamp(1516151820000L)
                        .putFields("Flow Rate ft3/s", FieldValue.newBuilder().setFloatValue(1.0172575712203979).build())
                        .putFields("Engine Oil Pressure psi ", FieldValue.newBuilder().setFloatValue(34.7).build()))
                .build();

        TSIMapper tsiMapper = new TSIMapper();
        EventData eventData = tsiMapper.mapEvent(event);
        String json = eventData.getBodyAsString();
        System.out.println("Expected JSON: " + expectedJson);
        System.out.println("Actual JSON:   " + json);
        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT);
    }
}
