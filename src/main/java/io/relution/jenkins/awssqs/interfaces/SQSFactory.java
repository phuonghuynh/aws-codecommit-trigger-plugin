/*
 * Copyright 2016 M-Way Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.relution.jenkins.awssqs.interfaces;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;

import java.util.concurrent.ExecutorService;

import io.relution.jenkins.awssqs.net.SQSChannel;


/**
 * Interface definition for factories that can create {@link SQSQueueMonitor} instances and related
 * classes. The instances returned by a factory implementation can be used to monitor a particular
 * {@link io.relution.jenkins.awssqs.interfaces.SQSQueue} by polling the queue for new messages.
 */
public interface SQSFactory {

    /**
     * Returns a new Amazon SQS instance that can be used to access the specified queue.
     * @param queue The {@link io.relution.jenkins.awssqs.interfaces.SQSQueue} for which to create a client.
     * @return A new instance of an {@link AmazonSQS} that is suitable for synchronous access to
     * the specified queue.
     */
    AmazonSQS createSQS(final io.relution.jenkins.awssqs.interfaces.SQSQueue queue);

    /**
     * Returns a new Amazon SQS instance that can be used to access the specified queue.
     * @param queue The {@link io.relution.jenkins.awssqs.interfaces.SQSQueue} for which to create a client.
     * @return A new instance of an {@link AmazonSQSAsync} that is suitable for asynchronous access
     * to the specified queue.
     */
    AmazonSQSAsync createSQSAsync(final io.relution.jenkins.awssqs.interfaces.SQSQueue queue);

    /**
     * Returns a new channel instance that can be used to communicate with the specified queue.
     * @param queue The {@link io.relution.jenkins.awssqs.interfaces.SQSQueue} for which to create the channel.
     * @return A new {@link SQSChannel} for the specified queue.
     */
    SQSChannel createChannel(final io.relution.jenkins.awssqs.interfaces.SQSQueue queue);

    /**
     * Returns a new monitor instance that can be used to poll the specified queue for new
     * messages.
     * @param executor The {@link ExecutorService} used to execute the monitor.
     * @param queue The {@link io.relution.jenkins.awssqs.interfaces.SQSQueue} for which to create a monitor.
     * @return A new {@link SQSQueueMonitor} instance suitable for monitoring the specified queue.
     */
    SQSQueueMonitor createMonitor(final ExecutorService executor, final io.relution.jenkins.awssqs.interfaces.SQSQueue queue);

    /**
     * Returns a new monitor instance that can be used to poll the specified queue for new
     * messages.
     * <p>
     * The new monitor has the same listeners as the specified monitor. This should be used to
     * create a new monitor instance in case a queue was reconfigured.
     * @param monitor The monitor used to initialize internal fields of the new instance.
     * @param queue The {@link io.relution.jenkins.awssqs.interfaces.SQSQueue} for which to create a monitor.
     * @return A new {@link SQSQueueMonitor} instance suitable for monitoring the specified queue,
     * that has the same listeners as the specified monitor.
     */
    SQSQueueMonitor createMonitor(final SQSQueueMonitor monitor, final io.relution.jenkins.awssqs.interfaces.SQSQueue queue);
}
