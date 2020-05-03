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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.Base64;
import java.util.List;

import org.junit.Test;

public class EEGHandlerTest {

    @Test
    public void canDecodeEegPacket() {
        /* From muselsl
        MOO1: bytearray(b'\x15\x06\x16\xda\x9f\x00\x01\xab\xe9\xeeg\x1d\xa0\x00\x8fQ\x80s\xa0\x00')
        MOO2: (5382, [ -821.77734375   327.63671875 -1000.          -791.50390625
           827.1484375    800.29296875  -768.5546875  -1000.
           119.62890625  -812.5          -96.6796875  -1000.        ])
        */
        byte[] eegChannelData = Base64.getDecoder().decode("FQYW2p8AAavp7mcdoACPUYBzoAA=");
        ChannelSamples samples = EEGHandler.unpack_eeg_channel(eegChannelData);

        assertThat(samples.getIndex(), equalTo(5382));

        List<Double> values = samples.getValues();
        assertThat(values, hasSize(12));
        double epsilon = 0.00001d;
        assertThat(values.get(0), closeTo(-821.77734375, epsilon));
        assertThat(values.get(1), closeTo(327.63671875, epsilon));
        assertThat(values.get(2), closeTo(-1000.0, epsilon));
        assertThat(values.get(3), closeTo(-791.50390625, epsilon));
        assertThat(values.get(4), closeTo(827.1484375, epsilon));
        assertThat(values.get(5), closeTo(800.29296875, epsilon));
        assertThat(values.get(6), closeTo(-768.5546875, epsilon));
        assertThat(values.get(7), closeTo(-1000., epsilon));
        assertThat(values.get(8), closeTo(119.62890625, epsilon));
        assertThat(values.get(9), closeTo(-812.5 , epsilon));
        assertThat(values.get(10), closeTo(-96.6796875 , epsilon));
        assertThat(values.get(11), closeTo(-1000. , epsilon));
    }
}
