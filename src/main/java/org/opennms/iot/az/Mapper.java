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
import java.util.Map;

import org.opennms.iot.ble.proto.Event;
import org.opennms.iot.ble.proto.Metric;
import org.opennms.iot.handlers.PolarH7Handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class Mapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toEventHubJson(Event event) throws JsonProcessingException {
        Message msg = new Message();
        msg.body.deviceId = event.getSensor().getHwAddress();
        addBPM(event, msg.body);
        return objectMapper.writeValueAsString(msg);
    }

    public static class Message {
        @JsonProperty
        Body body = new Body();
    }

    public static class Body {
        @JsonProperty
        String deviceId;
        // "2020-05-03T20:40:11Z
        @JsonProperty
        String endDate;
        @JsonProperty
        String heartRate;
        @JsonProperty(required = true)
        Map<String,String> properties = Maps.newLinkedHashMap();
        @JsonProperty
        Map<String,String> systemProperties = Maps.newLinkedHashMap();
    }

    public static Integer addBPM(Event event, Body body) {
        for (Metric metric : event.getMetricsList()) {
            if(PolarH7Handler.METRIC_NAME.equals(metric.getName())) {
                int bpm = (int)metric.getFieldsMap().get(PolarH7Handler.BPM_FIELD).getIntValue();
                body.heartRate = Integer.toString(bpm);
                body.endDate = Instant.ofEpochMilli(metric.getTimestamp()).toString();
            }
        }
        return null;
    }
}
