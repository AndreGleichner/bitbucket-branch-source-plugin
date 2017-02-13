/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.bitbucket;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApiFactory;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import java.util.List;

import javax.annotation.CheckForNull;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import com.cloudbees.jenkins.plugins.bitbucket.client.BitbucketCloudApiClient;
import com.cloudbees.jenkins.plugins.bitbucket.server.client.BitbucketServerAPIClient;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.Util;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;

public class BitbucketApiConnector {

    private String serverUrl;

    public BitbucketApiConnector() {
    }

    public BitbucketApiConnector(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public BitbucketApi create(String owner, String repository, StandardUsernamePasswordCredentials creds) {
        return BitbucketApiFactory.newInstance(serverUrl, creds, owner, repository);
    }

    public BitbucketApi create(String owner, StandardUsernamePasswordCredentials creds) {
        return BitbucketApiFactory.newInstance(serverUrl, creds, owner, null);
    }

    @CheckForNull 
    public <T extends StandardCredentials> T lookupCredentials(@CheckForNull SCMSourceOwner context, @CheckForNull String id, Class<T> type) {
        if (StringUtils.isNotBlank(id)) {
            return CredentialsMatchers.firstOrNull(
                      CredentialsProvider.lookupCredentials(
                              type,
                              context,
                              context instanceof Queue.Task
                                      ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                                      : ACL.SYSTEM,
                              bitbucketDomainRequirements()
                      ),
                      CredentialsMatchers.allOf(
                          CredentialsMatchers.withId(id),
                          CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(type))
                      )
            );
        }
        return null;
    }

    public StandardListBoxModel fillCheckoutCredentials(StandardListBoxModel result, SCMSourceOwner context) {
        result.includeMatchingAs(
                context instanceof Queue.Task
                ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                : ACL.SYSTEM,
                context,
                StandardCredentials.class,
                bitbucketDomainRequirements(),
                bitbucketCheckoutCredentialsMatcher()
        );
        return result;
    }

    public StandardListBoxModel fillCredentials(StandardListBoxModel result, SCMSourceOwner context) {
        result.includeMatchingAs(
                context instanceof Queue.Task
                ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                : ACL.SYSTEM,
                context,
                StandardUsernameCredentials.class,
                bitbucketDomainRequirements(),
                bitbucketCredentialsMatcher()
        );
        return result;
    }

    /* package */ static CredentialsMatcher bitbucketCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
    }

    /* package */ static CredentialsMatcher bitbucketCheckoutCredentialsMatcher() {
        return CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardCredentials.class));
    }

    /* package */ List<DomainRequirement> bitbucketDomainRequirements() {
        if (serverUrl == null) {
            return URIRequirementBuilder.fromUri("https://bitbucket.org").build();
        } else {
            return URIRequirementBuilder.fromUri(serverUrl).build();
        }
    }

}
