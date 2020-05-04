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

import java.util.Objects;
import java.util.function.Function;

import org.opennms.iot.ble.proto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;

import io.grpc.stub.StreamObserver;

public class EventHubExporter implements StreamObserver<Event> {
    private static final Logger LOG = LoggerFactory.getLogger(EventHubExporter.class);

    private final String eventHubName;
    private final EventHubProducerClient producer;
    private final Function<Event, EventData> mapper;

    public EventHubExporter(String connectionString, String eventHubName, Function<Event, EventData> mapper) {
        Objects.requireNonNull(connectionString);
        this.eventHubName = Objects.requireNonNull(eventHubName);
        this.mapper = Objects.requireNonNull(mapper);
        producer = new EventHubClientBuilder()
                .connectionString(connectionString, eventHubName)
                .buildProducerClient();
    }

    @Override
    public void onNext(Event event) {
        LOG.trace("onNext(): {}", event);

        // Create a batch using the HW address as the partition key
        CreateBatchOptions batchOptions = new CreateBatchOptions();
        batchOptions.setPartitionKey(event.getSensor().getHwAddress());
        EventDataBatch batch = producer.createBatch(batchOptions);

        // Map & marshal the event
        EventData eventData;
        try {
            eventData = mapper.apply(event);
        } catch (Exception e) {
            LOG.error("Mapping event to event date failed. Event: {}", event, e);
            return;
        }

        // Forward
        LOG.debug("Sending event (eventHub={}, partition-key={}): {}", eventHubName, eventData.getPartitionKey(),
                eventData.getBodyAsString());
        if(!batch.tryAdd(eventData)) {
            LOG.error("Adding to new batch failed?");
        }
        producer.send(batch);
    }

    @Override
    public void onError(Throwable t) {
        LOG.error("Local event stream over gRPC threw an error with:", t);
    }

    @Override
    public void onCompleted() {
        LOG.info("Local event stream over gRPC completed.");
    }

}
