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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opennms.iot.ble.proto.Event;
import org.opennms.iot.ble.proto.FieldValue;
import org.opennms.iot.ble.proto.Metric;
import org.opennms.iot.handlers.PolarH7Handler;

import com.azure.messaging.eventhubs.EventData;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class TSIMapper implements EventMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toTSIJson(Event event) throws JsonProcessingException {
        Message msg = new Message();
        for (Metric metric : event.getMetricsList()) {
            msg.deviceId = event.getSensor().getHwAddress();
            msg.timestamp = Instant.ofEpochMilli(metric.getTimestamp()).toString();
            for (String key : metric.getFieldsMap().keySet()) {
                double valueAsDouble = Double.NaN;
                FieldValue value = metric.getFieldsMap().get(key);
                switch(value.getValueCase()) {
                    case FLOAT_VALUE:
                        valueAsDouble = value.getFloatValue();
                        break;
                    case INT_VALUE:
                        valueAsDouble = value.getIntValue();
                        break;
                    case BOOL_VALUE:
                        valueAsDouble = value.getBoolValue() ? 0 : 1;
                        break;
                    case STRING_VALUE:
                    case VALUE_NOT_SET:
                        // Skip
                        continue;
                }
                msg.addValue(key, valueAsDouble);
            }
            break;
        }
        return objectMapper.writeValueAsString(msg);
    }

    @Override
    public EventData mapEvent(Event event) {
        try {
            return new EventData(toTSIJson(event));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class Message {
        @JsonProperty
        String deviceId;
        @JsonProperty // "2020-05-03T20:40:11Z
        String timestamp;
        @JsonProperty
        List<Map<String,Double>> series = new LinkedList<>();

        public void addValue(String key, double value) {
            if (series.isEmpty()) {
                series.add(new LinkedHashMap<>());
            }
            Map<String,Double> values = series.get(0);
            values.put(key, value);
        }
    }
}
