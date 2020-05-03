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

import java.util.List;

// {"data": [[-0.48828125, -0.48828125, 39.55078125, -103.02734375, 385.25390625, -971.6796875, -254.8828125, -1000.0, -211.9140625, -830.56640625, 222.16796875, -1000.0],
// [0.0, 0.0, -32.71484375, 70.3125, -334.9609375, -1000.0, -264.6484375, -813.96484375, 240.234375, -1000.0, -507.8125, -928.22265625],
// [0.0, -3.41796875, 23.92578125, -114.2578125, 407.71484375, -38.57421875, -1000.0, -659.1796875, -760.7421875, -1000.0, -895.99609375, -520.01953125],
// [-0.48828125, -0.48828125, 31.73828125, -74.21875, 330.078125, -862.3046875, 143.5546875, -1000.0, -247.55859375, -808.59375, 242.67578125, -1000.0],
// [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]],
// "timestamps": [1588222530.7055793, 1588222530.7094855, 1588222530.7133918, 1588222530.717298, 1588222530.7212043, 1588222530.7251105, 1588222530.7290168,
//  1588222530.732923, 1588222530.7368293, 1588222530.7407355, 1588222530.7446418, 1588222530.748548]}
public class EegChunkDTO {
    List<List<Double>> data;
    List<Double> timestamps;
}