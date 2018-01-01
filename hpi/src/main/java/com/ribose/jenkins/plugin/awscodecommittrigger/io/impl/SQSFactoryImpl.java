/*
 * Copyright 2017 Ribose Inc. <https://www.ribose.com>
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

package com.ribose.jenkins.plugin.awscodecommittrigger.io.impl;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.*;
import com.google.inject.Inject;
import com.ribose.jenkins.plugin.awscodecommittrigger.SQSTriggerQueue;
import com.ribose.jenkins.plugin.awscodecommittrigger.credentials.AwsCredentials;
import com.ribose.jenkins.plugin.awscodecommittrigger.credentials.AwsCredentialsHelper;
import com.ribose.jenkins.plugin.awscodecommittrigger.interfaces.SQSQueue;
import com.ribose.jenkins.plugin.awscodecommittrigger.io.RequestFactory;
import com.ribose.jenkins.plugin.awscodecommittrigger.io.SQSChannel;
import com.ribose.jenkins.plugin.awscodecommittrigger.io.SQSFactory;
import com.ribose.jenkins.plugin.awscodecommittrigger.io.threads.SQSExecutorFactory;
import com.ribose.jenkins.plugin.awscodecommittrigger.logging.Log;
import com.ribose.jenkins.plugin.awscodecommittrigger.mornitor.SQSQueueMonitor;
import com.ribose.jenkins.plugin.awscodecommittrigger.mornitor.impl.SQSQueueMonitorImpl;
import hudson.ProxyConfiguration;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.http.HttpStatus;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class SQSFactoryImpl implements SQSFactory {

    private static final Log log = Log.get(SQSFactoryImpl.class);

    private final RequestFactory requestFactory;
    private final SQSExecutorFactory sqsExecutorFactory;

    @Inject
    public SQSFactoryImpl(final SQSExecutorFactory sqsExecutorFactory, final RequestFactory requestFactory) {
        this.sqsExecutorFactory = sqsExecutorFactory;
        this.requestFactory = requestFactory;
    }

    @Override
    public AmazonSQS createSQSAsync(final SQSQueue queue) {
        AWSCredentialsProvider credentials = queue.hasCredentials() ? queue.lookupAwsCredentials() : DefaultAWSCredentialsProviderChain.getInstance();
        AmazonSQSAsyncClientBuilder sqsAsyncBuilder = createStandardAsyncClientBuilder(queue, credentials);
        final QueueBufferConfig queueBufferConfig = this.getQueueBufferConfig(queue);
        return new AmazonSQSBufferedAsyncClient(sqsAsyncBuilder.build(), queueBufferConfig);
    }

    @Override
    public AmazonSQS createSQSAsync(String accessKey, String secretKey) {
        AmazonSQSAsyncClientBuilder sqsAsyncBuilder = createStandardAsyncClientBuilder(null, new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        final QueueBufferConfig queueBufferConfig = this.getQueueBufferConfig(null);
        return new AmazonSQSBufferedAsyncClient(sqsAsyncBuilder.build(), queueBufferConfig);
    }

    @Override
    public AmazonSQS createSQSAsync(String accessKey, String secretKey, Regions region) {
        AmazonSQSAsyncClientBuilder sqsAsyncBuilder = createStandardAsyncClientBuilder(null, new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        if (region != null) {
            sqsAsyncBuilder.withRegion(region);
        }
        final QueueBufferConfig queueBufferConfig = this.getQueueBufferConfig(null);
        return new AmazonSQSBufferedAsyncClient(sqsAsyncBuilder.build(), queueBufferConfig);
    }

    private AmazonSQSAsyncClientBuilder createStandardAsyncClientBuilder(SQSQueue queue, AWSCredentialsProvider credentials) {
        ClientConfiguration clientConfiguration = this.getClientConfiguration(queue);
        AmazonSQSAsyncClientBuilder builder = AmazonSQSAsyncClientBuilder.standard()
            .withClientConfiguration(clientConfiguration)
            .withCredentials(credentials)
            .withExecutorFactory(this.sqsExecutorFactory);

        if (queue != null) {
            Regions region = queue.getRegion();
            if (region != null) {
                builder.withRegion(region);
            }
        }

        return builder;
    }

    private SQSChannel createChannel(final SQSQueue queue) {
        final AmazonSQS sqs = this.createSQSAsync(queue);
        return new SQSChannelImpl(sqs, queue, this.requestFactory);
    }

    @Override
    public SQSQueueMonitor createMonitor(final ExecutorService executor, final SQSQueue queue) {
        final AmazonSQS sqs = this.createSQSAsync(queue);
        final SQSChannel channel = new SQSChannelImpl(sqs, queue, this.requestFactory);
        return new SQSQueueMonitorImpl(executor, queue, channel);
    }

    @Override
    public SQSQueueMonitor createMonitor(final SQSQueueMonitor monitor, final SQSQueue queue) {
        final SQSChannel channel = this.createChannel(queue);
        return monitor.clone(queue, channel);
    }

    private ClientConfiguration getClientConfiguration(@Nullable final SQSQueue queue) {
        ProxyConfiguration proxyConfig = Jenkins.getActiveInstance().proxy;

        String proxyUrl = queue == null ?
            "sqs.*.amazonaws.com" :
            com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsEndpoint(queue.getUrl());

        Proxy proxy = proxyConfig == null ? Proxy.NO_PROXY : proxyConfig.createProxy(proxyUrl);
        return getClientConfiguration(proxy);
    }

    @Override
    public ClientConfiguration getClientConfiguration(final Proxy proxy) {
        ClientConfiguration config = PredefinedClientConfigurations.defaultConfig();

        //TODO review proxy configuration
        if (!Proxy.NO_PROXY.equals(proxy) && proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            config.setProxyHost(address.getHostName());
            config.setProxyPort(address.getPort());
//            config.setNonProxyHosts("169.254.169.254");

            ProxyConfiguration proxyConfig = Jenkins.getActiveInstance().proxy;
            if (StringUtils.isNotBlank(proxyConfig.getUserName())) {
                config.setProxyUsername(proxyConfig.getUserName());
                config.setProxyPassword(proxyConfig.getPassword());
            }

            log.debug("Proxy settings for SQS: %s:%s", config.getProxyHost(), config.getProxyPort());
        }
        return config;
    }

    @Override
    public boolean hasSufficientPermissions(@Nonnull String url, @Nonnull String credentialsId) {

//        if (org.apache.commons.lang3.StringUtils.isBlank(credentialsId)) {
//            return false;
//        }

//        String endpoint = com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsEndpoint(url);
//        if (StringUtils.isEmpty(endpoint)) {
//            return false;
//        }

        AwsCredentials credentials = AwsCredentialsHelper.getCredentials(credentialsId);
        if (credentials == null) {
            return false;
        }

        String queueUrl = org.apache.commons.lang3.StringUtils.trimToEmpty(url);
        Regions region = com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsRegion(url);
        if (region == null) {
            return false;
        }

//        if (org.apache.commons.lang3.StringUtils.isEmpty(region)) {
//            return FormValidation.error(String.format("Region not found from Queue: %s", url));
//        }

        AmazonSQS client = createSQSAsync(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey(), region);

        try {
            AmazonWebServiceRequest request = this.requestFactory.createReceiveMessageRequest(queueUrl);
            client.receiveMessage((ReceiveMessageRequest) request);

            request = this.requestFactory.createDeleteMessageBatchRequest(queueUrl, Collections.singletonList(new Message()));
            client.deleteMessageBatch((DeleteMessageBatchRequest) request);
        }
        catch (EmptyBatchRequestException e) {
            return true;
//            return FormValidation.okWithMarkup("<span class=\"info\">Access to SQS successful</span>");
        }
        catch (Exception e) {
//            return FormValidation.error(e, "Error validating SQS access");
            log.debug("Error validating SQS access", e);
//            return false;
        }
        finally {
            client.shutdown();
        }

        return false;
    }

    @CheckForNull
    @Override
    public List<String> getListQueues(@Nonnull String credentialsId, @Nonnull Regions region) {
        AwsCredentials credentials = AwsCredentialsHelper.getCredentials(credentialsId);
        if (credentials == null) {
            return null;
        }

        AmazonSQS client = createSQSAsync(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey(), region);

        try {
            return client.listQueues().getQueueUrls();
        }
        catch (Exception e) {
            log.debug("Error listing SQS access", e);
        }
        finally {
            client.shutdown();
        }

        return null;
    }

    //@param queue might be null
    private QueueBufferConfig getQueueBufferConfig(final SQSQueue queue) {
        final QueueBufferConfig config = new QueueBufferConfig();

        // TODO Add more options
        config.setLongPollWaitTimeoutSeconds(queue == null ? SQSQueue.WAIT_TIME_SECONDS_DEFAULT : queue.getWaitTimeSeconds());
        config.setLongPoll(true);

        return config;
    }
}
