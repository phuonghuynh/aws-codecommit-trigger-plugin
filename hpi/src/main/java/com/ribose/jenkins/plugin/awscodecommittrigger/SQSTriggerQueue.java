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

package com.ribose.jenkins.plugin.awscodecommittrigger;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.ribose.jenkins.plugin.awscodecommittrigger.credentials.AwsCredentials;
import com.ribose.jenkins.plugin.awscodecommittrigger.credentials.AwsCredentialsHelper;
import com.ribose.jenkins.plugin.awscodecommittrigger.i18n.sqstriggerqueue.Messages;
import com.ribose.jenkins.plugin.awscodecommittrigger.interfaces.SQSQueue;
import com.ribose.jenkins.plugin.awscodecommittrigger.io.SQSFactory;
import com.ribose.jenkins.plugin.awscodecommittrigger.logging.Log;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;


public class SQSTriggerQueue extends AbstractDescribableImpl<SQSTriggerQueue> implements SQSQueue {
    private static final Log log = Log.get(SQSTriggerQueue.class);

    private String version;
    private String uuid;
    private Integer waitTimeSeconds;
    private Integer maxNumberOfMessages;
    private String url;
    private String credentialsId;
    private Integer urlInputIndex;

    //    private transient SQSFactory sqsFactory;
//    private transient AmazonSQS sqs;
    private transient boolean compatible;

    @Deprecated/*since 2.0*/
    private transient String accessKey;

    @Deprecated/*since 2.0*/
    private transient Secret secretKey;

    private Regions region = null;
//    private transient boolean queuesListable = false;

    @DataBoundConstructor
    public SQSTriggerQueue(final String uuid,
                           final String region,
                           final String url,
                           final String credentialsId,
                           final Integer waitTimeSeconds,
                           final Integer maxNumberOfMessages,
                           final String version,
                           final Integer urlInputIndex) {
//        Context.injector().injectMembers(this);

        this.version = version;
        this.urlInputIndex = urlInputIndex;

        this.uuid = StringUtils.isBlank(uuid) ? UUID.randomUUID().toString() : uuid;

        this.url = url;

//        if (StringUtils.isNotBlank(region)) {
        this.region = Regions.valueOf(region);
//        }
//        else {
//            this.region =  com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsRegion(this.url);
//        }
//        this.region = com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsRegion(this.url);

        this.credentialsId = credentialsId;

        this.waitTimeSeconds = this.limit(
            waitTimeSeconds,
            WAIT_TIME_SECONDS_MIN,
            WAIT_TIME_SECONDS_MAX,
            WAIT_TIME_SECONDS_DEFAULT);

        this.maxNumberOfMessages = this.limit(
            maxNumberOfMessages,
            MAX_NUMBER_OF_MESSAGES_MIN,
            MAX_NUMBER_OF_MESSAGES_MAX,
            MAX_NUMBER_OF_MESSAGES_DEFAULT);

        log.debug("Create new SQSTriggerQueue(%s, %s)", this.uuid, this.url);

//        this.queuesListable = true;//should be listable in the first-time loaded
//        if (StringUtils.isNotBlank(this.credentialsId)) {
//            List<String> queues = this.sqsFactory.getListQueues(this.credentialsId, this.region);
//            this.queuesListable = queues != null;
//        }
    }

//    public AmazonSQS getSQSClient() {
//        if (this.sqs == null) {
//            this.sqs = this.getSqsFactory().createSQSAsync(this);
//        }
//        return this.sqs;
//    }

//    public SQSFactory getSqsFactory() {
//        return this.sqsFactory;
//    }

    public void setRegion(Regions region) {
        this.region = region;
    }

//    public void setSqsFactory(SQSFactory sqsFactory) {
//        this.sqsFactory = sqsFactory;
//    }

//    public void setSqs(AmazonSQS sqs) {
//        this.sqs = sqs;
//    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setWaitTimeSeconds(Integer waitTimeSeconds) {
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
        this.maxNumberOfMessages = maxNumberOfMessages;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public Secret getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(Secret secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public int getWaitTimeSeconds() {
        if (this.waitTimeSeconds == null) {
            return WAIT_TIME_SECONDS_DEFAULT;
        }
        return this.waitTimeSeconds;
    }

    @Override
    public int getMaxNumberOfMessages() {
        if (this.maxNumberOfMessages == null) {
            return MAX_NUMBER_OF_MESSAGES_DEFAULT;
        }
        return this.maxNumberOfMessages;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public String getName() {
        return com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsQueueName(this.url);
    }

    @CheckForNull
    @Override
    public AwsCredentials lookupAwsCredentials() {
        if (this.credentialsId == null) {
            return null;
        }
        return AwsCredentialsHelper.getCredentials(this.credentialsId);
    }

    @Override
    public boolean hasCredentials() {
        return StringUtils.isNotBlank(this.credentialsId);
    }

    @Override
    public Regions getRegion() {
        return region;
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof SQSTriggerQueue)) {
            return false;
        }

        final SQSTriggerQueue other = (SQSTriggerQueue) obj;
        return this.uuid.equals(other.uuid);
    }

    @Override
    public String toString() {
        return url;//return url present this queue
    }

    private int limit(final Integer value, final int min, final int max, final int fallbackValue) {
        if (value == null || value < min || value > max) {
            return fallbackValue;
        } else {
            return value;
        }
    }

//    public boolean isQueuesListable() {
//        return queuesListable;
//    }

    @JavaScriptMethod
    public Integer getUrlInputIndex() {
        if (urlInputIndex == null) {
            urlInputIndex = 0;
        }
        return urlInputIndex;
    }

    public void setUrlInputIndex(Integer urlInputIndex) {
        this.urlInputIndex = urlInputIndex;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SQSTriggerQueue> {

        private transient SQSFactory sqsFactory;
//        private transient RequestFactory requestFactory;

        public DescriptorImpl() {
            super();
            this.sqsFactory = Context.injector().getBinding(SQSFactory.class).getProvider().get();
//            this.requestFactory = Context.injector().getBinding(RequestFactory.class).getProvider().get();
            this.load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.displayName();
        }

        public FormValidation doCheckWaitTimeSeconds(@QueryParameter final String value) {
            return this.validateNumber(
                value,
                WAIT_TIME_SECONDS_MIN,
                WAIT_TIME_SECONDS_MAX,
                Messages.errorWaitTimeSeconds()
            );
        }

        public FormValidation doCheckMaxNumberOfMessage(@QueryParameter final String value) {
            return this.validateNumber(
                value,
                MAX_NUMBER_OF_MESSAGES_MIN,
                MAX_NUMBER_OF_MESSAGES_MAX,
                Messages.errorMaxNumberOfMessages()
            );
        }

        public FormValidation doValidate(@QueryParameter(required = true) final String url,
                                         @QueryParameter(required = true, fixEmpty = true) final String credentialsId,
                                         @QueryParameter int urlInputIndex) throws IOException, ServletException {
            String queueUrl = StringUtils.trimToEmpty(url);
            if (StringUtils.isBlank(queueUrl)) {
                return FormValidation.error("Queue Url is blank");
            }

            if (com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsEndpoint(queueUrl) == null) {
                return FormValidation.error("Queue Url is invalid");
            }

            Regions region = com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsRegion(queueUrl);
            if (region == null) {
                return FormValidation.error("Unable to get Region from Queue Url");
            }

            if (StringUtils.isBlank(credentialsId)) {
                return FormValidation.error("No Credential selected");
            }

            StringBuilder message = new StringBuilder();
            if (urlInputIndex == 0 && this.sqsFactory.getListQueues(credentialsId, region) == null) {//select-box is shown
                AwsCredentials credentials = AwsCredentialsHelper.getCredentials(credentialsId);
                message.append(String.format(
                    "<div class=\"warning-inline\">Unable to access List Queues in Region %s using Credentials %s </div>",
                    region.getName(),
                    credentials == null ? "NULL" : credentials.getDisplayName()
                ));
            }

//            boolean hasSufficientPermissions = this.sqsFactory.hasSufficientPermissions(queueUrl, credentialsId);
//            if (hasSufficientPermissions) {
////                validation = FormValidation.okWithMarkup("<div class=\"info\">Access to SQS successful</div>");
//
//            }

//            validation = FormValidation.error("Error validating SQS access");

            message.append(
                this.sqsFactory.hasSufficientPermissions(queueUrl, credentialsId) ?
                    "<div class=\"info\">Access to SQS successful</div>" :
                    "<div class=\"error\">Error validating SQS access</div>"
            );

            return FormValidation.okWithMarkup(message.toString());

//            AwsCredentials credentials = AwsCredentialsHelper.getCredentials(credentialsId);
//            if (credentials == null) {
//                return FormValidation.error("Credentials is null");
//            }

//            String region = com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsRegion(url);
//            if (org.apache.commons.lang3.StringUtils.isEmpty(region)) {
//                return FormValidation.error(String.format("Region not found from Queue: %s", url));
//            }

//            String queueUrl = StringUtils.trimToEmpty(url);
//            AmazonSQS client = this.sqsFactory.createSQSAsync(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey(), region);
//
//            try {
//                AmazonWebServiceRequest request = this.requestFactory.createReceiveMessageRequest(queueUrl);
//                client.receiveMessage((ReceiveMessageRequest) request);
//
//                request = this.requestFactory.createDeleteMessageBatchRequest(queueUrl, Collections.singletonList(new Message()));
//                client.deleteMessageBatch((DeleteMessageBatchRequest) request);
//            }
//            catch (EmptyBatchRequestException e) {
//                return FormValidation.okWithMarkup("<span class=\"info\">Access to SQS successful</span>");
//            }
//            catch (Exception e) {
//                return FormValidation.error(e, "Error validating SQS access");
//            }
//            finally {
//                client.shutdown();
//            }
//
//            return FormValidation.error("Error validating SQS access");
        }

        public ListBoxModel doFillRegionItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("", "");
            for (Regions region : Regions.values()) {
                Region r = Region.getRegion(region);
                if (r.isServiceSupported(AmazonSQS.ENDPOINT_PREFIX) && r.isServiceSupported("codecommit")) {
                    items.add(region.getName(), region.name());
                }
            }
            return items;
        }

        public ListBoxModel doFillUrlItems(
//            @QueryParameter String url,
            @QueryParameter(required = true, fixEmpty = true) final String region,
            @QueryParameter(required = true, fixEmpty = true) final String credentialsId
        ) throws IOException {
            ListBoxModel items = new ListBoxModel();

//            if (StringUtils.isNotBlank(url)) {
//                url = url.trim();
//                items.add(com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsQueueName(url), url);
//            }

            if (region == null || credentialsId == null) {
                return items;
            }

//            try {
//                AwsCredentials credentials = AwsCredentialsHelper.getCredentials(credentialsId);
//                assert credentials != null;
//
//                AmazonSQS client = this.sqsFactory.createSQSAsync(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey(), region);
//                List<String> queueUrls = client.listQueues().getQueueUrls();
//                for (String queueUrl : queueUrls) {
//                    items.add(com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsQueueName(queueUrl), queueUrl);
//                }
//            } catch (AmazonServiceException e) {//com.amazonaws.SdkClientException: Unable to find a region via the region provider chain. Must provide an explicit region in the builder or setup environment to supply a region.
//                //TODO detect default Region setting in http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
//                items.clear();
//                if (e.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
//                    items.add("--forbidden--", "SC_FORBIDDEN");
////                    Stapler.getCurrentResponse().setHeader("script", "alert('List Queues is Forbidden')");
//                }
//////                else {
////                    throw e;
//////                }
//            } catch (Exception e) {//com.amazonaws.services.sqs.model.AmazonSQSException: Access to the resource https://sqs.us-west-2.amazonaws.com/ is denied. (Service: AmazonSQS; Status Code: 403; Error Code: AccessDenied; Request ID: 165762d0-bd84-5b9a-aaaa-308446528a6d)
//                items.clear();
//            }//TODO check region == ""

            Regions regions = com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.parseRegions(region);
            List<String> queueUrls = this.sqsFactory.getListQueues(credentialsId, regions);
            if (queueUrls != null) {
                for (String queueUrl : queueUrls) {
//                    if (queueUrl.equalsIgnoreCase(url)) {
//                        continue;
//                    }
                    items.add(com.ribose.jenkins.plugin.awscodecommittrigger.utils.StringUtils.getSqsQueueName(queueUrl), queueUrl);
                }
            }
            return items;
        }

        private FormValidation validateNumber(final String value, final int min, final int max, final String message) {
            try {
                if (StringUtils.isBlank(value)) {
                    return FormValidation.error(message);
                }

                final int number = Integer.parseInt(value);

                if (number < min || number > max) {
                    return FormValidation.error(message);
                }

                return FormValidation.ok();
            } catch (final NumberFormatException e) {
                return FormValidation.error(message);
            }
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String credentialsId) {
            return new StandardListBoxModel()
                .includeEmptyValue()
                .includeAs(ACL.SYSTEM, context, AwsCredentials.class)
                .includeCurrentValue(credentialsId);
        }

        public String getVersion() {
            return PluginInfo.version;
        }

        public void setSqsFactory(SQSFactory sqsFactory) {
            this.sqsFactory = sqsFactory;
        }

//        public void setRequestFactory(RequestFactory requestFactory) {
//            this.requestFactory = requestFactory;
//        }

    }
}
