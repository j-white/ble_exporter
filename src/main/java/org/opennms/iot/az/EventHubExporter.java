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

import org.opennms.iot.ble.proto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.google.protobuf.util.JsonFormat;

import io.grpc.stub.StreamObserver;

public class EventHubExporter implements StreamObserver<Event> {
    private static final Logger LOG = LoggerFactory.getLogger(EventHubExporter.class);

    private final JsonFormat.Printer jsonPrinter;

    private final EventHubProducerClient producer;

    public EventHubExporter(String connectionString, String eventHubName) {
        Objects.requireNonNull(connectionString);
        Objects.requireNonNull(eventHubName);
        producer = new EventHubClientBuilder()
                .connectionString(connectionString, eventHubName)
                .buildProducerClient();

        final JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.newBuilder()
                .add(Event.getDescriptor())
                .build();
        jsonPrinter = JsonFormat.printer()
                .usingTypeRegistry(typeRegistry);
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
            eventData = eventToEventHubData(event);
        } catch (Exception e) {
            LOG.error("Marshaling JSON for event failed: {}", event, e);
            return;
        }

        // Forward
        LOG.debug("Sending event (partition-key={}): {}", eventData.getPartitionKey(),
                eventData.getBodyAsString());
        if(!batch.tryAdd(eventData)) {
            LOG.error("Adding to new batch failed?");
        }
        producer.send(batch);
    }

    /** Render the event to JSON in the model we are forwarding to AZ */
    public EventData eventToEventHubData(Event event) throws Exception {
        return new EventData(Mapper.toEventHubJson(event));
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
