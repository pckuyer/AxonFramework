/*
 * Copyright (c) 2010-2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.monitoring.MessageMonitor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class EventProcessorLatencyMonitorTest {

    private final EventProcessorLatencyMonitor testSubject = new EventProcessorLatencyMonitor();
    private final Map<String, Metric> metricSet = testSubject.getMetrics();

    @Test
    void testMessages() {
        EventMessage<?> firstEventMessage = mock(EventMessage.class);
        when(firstEventMessage.getTimestamp()).thenReturn(Instant.ofEpochMilli(0));

        EventMessage<?> secondEventMessage = mock(EventMessage.class);
        when(secondEventMessage.getTimestamp()).thenReturn(Instant.now().minusMillis(1000));

        Map<? super EventMessage<?>, MessageMonitor.MonitorCallback> callbacks = testSubject.onMessagesIngested(Arrays.asList(firstEventMessage, secondEventMessage));
        callbacks.get(firstEventMessage).reportSuccess();


        Gauge<Long> latency = (Gauge<Long>) metricSet.get("latency");

        assertTrue(latency.getValue() >= 1000);
    }

    @Test
    void testFailureMessage() {
        EventMessage<?> firstEventMessage = mock(EventMessage.class);
        when(firstEventMessage.getTimestamp()).thenReturn(Instant.ofEpochMilli(0));

        EventMessage<?> secondEventMessage = mock(EventMessage.class);
        when(secondEventMessage.getTimestamp()).thenReturn(Instant.now().minusMillis(1000));

        Map<? super EventMessage<?>, MessageMonitor.MonitorCallback> callbacks = testSubject.onMessagesIngested(Arrays.asList(firstEventMessage, secondEventMessage));
        callbacks.get(firstEventMessage).reportFailure(null);

        Gauge<Long> latency = (Gauge<Long>) metricSet.get("latency");

        assertTrue(latency.getValue() >= 1000);
    }

    @Test
    void testNullMessage() {
        testSubject.onMessageIngested(null).reportSuccess();

        Gauge<Long> latency = (Gauge<Long>) metricSet.get("latency");

        assertEquals(0, latency.getValue(), 0);
    }
}
