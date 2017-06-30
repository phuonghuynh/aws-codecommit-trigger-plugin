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

import com.amazonaws.auth.AWSCredentials;


/**
 * Interface definition for classes that represent the necessary configuration that is required to
 * access an Amazon SQS queue.
 */
public interface SQSQueue extends AWSCredentials {

    /**
     * Returns the identifier used to uniquely identify the queue configuration.
     * @return The unique identifier of this configuration.
     */
    String getUuid();

    /**
     * Returns the URL of the queue the configuration is associated with.
     * @return The URL of a queue.
     */
    String getUrl();

    /**
     * Returns the name of the queue the configuration is associated with.
     * @return The name of a queue.
     */
    String getName();

    /**
     * Returns the endpoint of the queue the configuration is associated with.
     * @return The endpoint of a queue.
     */
    String getEndpoint();

    /**
     * Returns the time, in seconds, requests should wait for new messages to arrive in the queue.
     * @return The wait time, in seconds, before a receive message request should time out.
     */
    int getWaitTimeSeconds();

    /**
     * Returns the maximum number of messages that a request should request.
     * @return The maximum number of messages a receive message request should request from the
     * queue.
     */
    int getMaxNumberOfMessages();

    /**
     * Returns a value indicating whether the configuration is valid.
     * <p>
     * A configuration is considered valid if all information required to access the associated
     * queue has been defined.
     * @return {@code true} if the configuration is valid; otherwise, {@code false}.
     */
    boolean isValid();
}
