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

package io.relution.jenkins.awssqs.model.constants;

/**
 * Defines constants for error codes returned by Amazon Web Services (AWS).
 */
public final class ErrorCode {

    /**
     * The X.509 certificate or AWS access key ID provided does not exist on AWS.
     * <p>
     * HTTP Status Code: 403
     */
    public static final String INVALID_CLIENT_TOKEN_ID = "InvalidClientTokenId";

    private ErrorCode() {
    }
}
