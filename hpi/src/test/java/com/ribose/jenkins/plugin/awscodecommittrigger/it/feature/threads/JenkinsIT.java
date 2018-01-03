package com.ribose.jenkins.plugin.awscodecommittrigger.it.feature.threads;

import com.ribose.jenkins.plugin.awscodecommittrigger.it.AbstractFreestyleIT;
import com.ribose.jenkins.plugin.awscodecommittrigger.it.fixture.ProjectFixture;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class JenkinsIT extends AbstractFreestyleIT {

    public List<ProjectFixture> projectFixtures;

    public JenkinsIT() {
        projectFixtures = new ArrayList<>();
        for (int i = 0; i < 29; i++) {
            String branch = String.format("%s_%s", i, UUID.randomUUID().toString());
            projectFixtures.add(
                new ProjectFixture()
                    .setScm(defaultSCM)
                    .setSendBranches("refs/heads/" + branch)
                    .setSubscribeInternalScm(true)
                    .setShouldStarted(true)
            );
        }
    }

    @Test
    public void shouldPassProjectFixtures() throws Exception {
        for (final ProjectFixture projectFixture : this.projectFixtures) {
            this.mockAwsSqs.send(projectFixture.getSendBranches());
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        for (final ProjectFixture fixture : this.projectFixtures) {
            threadPool.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        subscribeProject(fixture);
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }
            });
        }

        threadPool.shutdown();
        threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

        for (ProjectFixture fixture : this.projectFixtures) {
            this.assertFixture(fixture);
        }
    }
}
