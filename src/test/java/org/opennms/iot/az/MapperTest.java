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

import com.fasterxml.jackson.core.JsonProcessingException;

public class MapperTest {

    @Test
    public void canMapAndMarshalEventToJson() throws JsonProcessingException, JSONException {
        String expectedJson = "{\"body\":{\"deviceId\":\"ag-apple-watch\",\"endDate\":\"2020-05-03T20:40:11Z\"," +
                "\"heartRate\":\"102\",\"properties\":{},\"systemProperties\":{}}}";

        Event event = Event.newBuilder()
                .setSensor(Sensor.newBuilder()
                        .setHwAddress("ag-apple-watch"))
                .addMetrics(Metric.newBuilder()
                        .setName(PolarH7Handler.METRIC_NAME)
                        .setTimestamp(1588538411000L)
                        .putFields(PolarH7Handler.BPM_FIELD, FieldValue.newBuilder().setIntValue(102).build()))
                .build();

        String json = Mapper.toEventHubJson(event);
        System.out.println("Expected JSON: " + expectedJson);
        System.out.println("Actual JSON:   " + json);
        JSONAssert.assertEquals(expectedJson, json, JSONCompareMode.LENIENT);
    }
}
