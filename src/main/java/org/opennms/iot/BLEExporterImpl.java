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
import java.util.concurrent.CopyOnWriteArrayList;

import org.opennms.iot.ble.proto.BLEExporterGrpc;
import org.opennms.iot.ble.proto.Client;
import org.opennms.iot.ble.proto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;

public class BLEExporterImpl extends BLEExporterGrpc.BLEExporterImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(BLEExporterImpl.class);

    private List<StreamObserver<Event>> observers = new CopyOnWriteArrayList<>();

    @Override
    public void streamEvents(Client request, StreamObserver<Event> observer) {
        observers.add(0, observer);
    }

    public synchronized void broadcast(Event event) {
        LOG.debug("Broadcasting event to {} observers: {}", observers.size(), event);
        observers.forEach(o -> {
            o.onNext(event);
        });
    }
}
