package com.ribose.jenkins.plugin.awscodecommittrigger.it;

import com.ribose.jenkins.plugin.awscodecommittrigger.*;
import com.ribose.jenkins.plugin.awscodecommittrigger.it.fixture.ProjectFixture;
import com.ribose.jenkins.plugin.awscodecommittrigger.it.mock.MockAwsSqs;
import com.ribose.jenkins.plugin.awscodecommittrigger.it.mock.MockContext;
import com.ribose.jenkins.plugin.awscodecommittrigger.it.mock.MockGitSCM;
import com.ribose.jenkins.plugin.awscodecommittrigger.matchers.impl.ScmJobEventTriggerMatcher;
import com.ribose.jenkins.plugin.awscodecommittrigger.rule.RegexLoggerRule;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public abstract class AbstractJenkinsIT {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @ClassRule
    public static RegexLoggerRule jobScheduledLoggerRule = new RegexLoggerRule(
        Pattern.compile("(Try to trigger the build for message)"),
        Logger.getLogger(SQSTriggerBuilder.class.getName()),
        Level.INFO
    ).capture(10);

    @ClassRule
    public static RegexLoggerRule noEventMatchedLoggerRule = new RegexLoggerRule(
        Pattern.compile("(No event matched)"),
        Logger.getLogger(ScmJobEventTriggerMatcher.class.getName()),
        Level.WARNING
    ).capture(10);

    protected MockAwsSqs mockAwsSqs;

    protected SQSTriggerQueue sqsQueue;

    protected static final GitSCM defaultSCM;
    protected static final String defaultSqsMessageTemplate;
    protected static final String defaultSCMUrl;

    static {
        try {
            defaultSqsMessageTemplate = IOUtils.toString(Utils.getResource(AbstractJenkinsIT.class, "sqsmsg.json.tpl", true), StandardCharsets.UTF_8);
            defaultSCM = MockGitSCM.fromSqsMessage(defaultSqsMessageTemplate);

            defaultSCMUrl = ((MockGitSCM) defaultSCM).getUrl();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Before
    public void before() throws Exception {
        this.mockAwsSqs = MockAwsSqs.get();

        jenkinsRule.getInstance().lookup.set(InternalInjector.class, MockContext.getInjector());

        File workDir = jenkinsRule.getInstance().getRootDir();
        String configName = "com.ribose.jenkins.plugin.awscodecommittrigger.SQSTrigger.xml";

        File configFile = new File(Utils.getResource(AbstractJenkinsIT.class, configName, true).toURI());
        FileUtils.copyFileToDirectory(configFile, workDir);
        configFile = new File(FilenameUtils.concat(workDir.getPath(), configName));

        String config = IOUtils.toString(configFile.toURI(), "UTF-8").replace("${URL}", mockAwsSqs.getSqsUrl());
        IOUtils.write(config, new FileOutputStream(configFile), "UTF-8");

        this.sqsQueue = ((SQSTrigger.DescriptorImpl) jenkinsRule.jenkins.getDescriptor(SQSTrigger.class)).getSqsQueues().get(0);//SQSTrigger.DescriptorImpl.get().getSqsQueues().get(0);

        this.mockAwsSqs.setSqsMessageTemplate(defaultSqsMessageTemplate);
    }

    @After
    public void after() {
        this.mockAwsSqs.clearAndShutdown();
    }

    protected abstract void subscribeProject(ProjectFixture fixture) throws Exception;

    protected void submitAndAssertFixture(ProjectFixture fixture) throws Exception {
        this.subscribeProject(fixture);
        this.assertFixture(fixture);
    }

    protected void assertFixture(ProjectFixture fixture) throws InterruptedException {
        OneShotEvent event = fixture.getEvent();
        Assertions.assertThat(fixture.getEvent()).as("OneShotEvent should be registered").isNotNull();

        event.block(fixture.getTimeout());

        try {
            Assertions.assertThat(event.isSignaled()).as("Job should be started? %s", fixture.getShouldStarted()).isEqualTo(fixture.getShouldStarted());
        } catch (AssertionError e) {
            if (fixture.getShouldStarted()) {
                Assertions.assertThat(jobScheduledLoggerRule.getMessages()).as("Jenkins Log should has lines match with pattern: %s", jobScheduledLoggerRule.getRegex().pattern()).isNotEmpty();
            }
            else {
                Assertions.assertThat(noEventMatchedLoggerRule.getMessages()).as("Jenkins Log should has lines match with pattern: %s", noEventMatchedLoggerRule.getRegex().pattern()).isNotEmpty();
                throw e;
            }
        }
    }
}
