/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apereo.cas;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.github.GitHubOperations;
import org.apereo.cas.github.GitHubTemplate;
import org.apereo.cas.github.RegexLinkParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@Slf4j
@EnableScheduling
@EnableConfigurationProperties(GitHubProperties.class)
public class CasBotApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CasBotApplication.class, args);
    }

    @Bean
    public GitHubTemplate gitHubTemplate(final GitHubProperties gitHubProperties) {
        return new GitHubTemplate(gitHubProperties.getCredentials().getToken(), new RegexLinkParser());
    }

    @Bean
    public MonitoredRepository monitoredRepository(final GitHubOperations gitHub,
                                                   final GitHubProperties gitHubProperties) {
        return new MonitoredRepository(gitHub, gitHubProperties);
    }

    @Bean
    public RepositoryMonitor repositoryMonitor(final GitHubOperations gitHub,
                                               final MonitoredRepository repository,
                                               final List<PullRequestListener> pullRequestListeners) {
        return new RepositoryMonitor(gitHub, repository, pullRequestListeners);
    }
    
    @EventListener
    public void applicationReady(final ApplicationReadyEvent event) {
        log.info("CAS GitHub bot is now ready");

        val repository = event.getApplicationContext().getBean(MonitoredRepository.class);
        log.info("Current version in master branch: {}", repository.getCurrentVersionInMaster());
        repository.getMilestoneForMaster().ifPresentOrElse(ms ->
            log.info("Current milestone for master branch: {}", ms),
            () -> log.warn("Unable to determine current milestone for master branch"));

    }

    @RestController
    public static class HomeController {

        @Autowired
        private MonitoredRepository repository;

        @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, String> home() {
            var map = new LinkedHashMap<String, String>();
            try {
                map.put("name", repository.getOrganization() + '/' + repository.getName());
                map.put("repository", repository.getGitHubProperties().getRepository().getUrl());
                map.put("version", repository.getCurrentVersionInMaster().toString());
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
            }
            return map;
        }
    }

    @RestController
    public static class RepositoryController {

        @Autowired
        private MonitoredRepository repository;

        @PostMapping(value = "/repository/mergeWithBase/{pullRequestNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
        public HttpStatus mergeWithMaster(@PathVariable final String pullRequestNumber) {
            try {
                val pr = this.repository.getPullRequest(pullRequestNumber);
                if (pr == null) {
                    return HttpStatus.NOT_FOUND;
                }

                if (pr.isLocked()) {
                    return HttpStatus.LOCKED;
                }
                this.repository.mergePullRequestWithBase(pr);
                return HttpStatus.OK;
            } catch (final Exception e) {
                log.error(e.getMessage());
            }
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
