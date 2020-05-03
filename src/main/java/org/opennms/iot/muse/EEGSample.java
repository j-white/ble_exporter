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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EEGSample {
    private final Map<Integer, ChannelSamples<Integer>> data;
    private final List<Long> timestamps;

    public EEGSample(Map<Integer, ChannelSamples<Integer>> data, List<Long> timestamps) {
        this.data = Objects.requireNonNull(data);
        this.timestamps = Objects.requireNonNull(timestamps);
    }

    public EEGSample(EegChunkDTO eegChunkDTO) {
        data = new LinkedHashMap<>();
        int i = 1;
        for (List<Double> datas : eegChunkDTO.data) {
            data.put(i, new ChannelSamples(i, datas));
            i++;
        }
        timestamps = eegChunkDTO.timestamps.stream()
                .map(t -> (long)(t * 1000))
                .collect(Collectors.toList());
    }


    public Map<Integer, ChannelSamples<Integer>> getData() {
        return data;
    }

    public List<Long> getTimestamps() {
        return timestamps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EEGSample)) return false;
        EEGSample eegSample = (EEGSample) o;
        return Objects.equals(timestamps, eegSample.timestamps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamps);
    }

    @Override
    public String toString() {
        return "EEGSample{" +
                "data=" + data +
                ", timestamps=" + timestamps +
                '}';
    }
}
