package org.jolokia.docker.maven.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import org.apache.maven.plugin.MojoExecutionException;

import org.codehaus.plexus.PlexusContainer;

import org.jolokia.docker.maven.access.AuthConfig;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;

/**
 * @author  jbellmann
 */
public class AuthConfigFactoryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testAuthConfigFactoryFromDockerCfg() throws MojoExecutionException, FileNotFoundException, IOException {

        File userDockerCfgFile = temporaryFolder.newFile("testDockerCfg");
        IOUtils.copy(getClass().getResourceAsStream("/dockercfg/valid.json"), new FileOutputStream(userDockerCfgFile));

        TestAuthConfigFactory factory = new TestAuthConfigFactory(null, userDockerCfgFile);
        AuthConfig authConfig = factory.fromDockerCfg("docker.registry.example.org");
        Assert.assertTrue(authConfig != null);

        String result = authConfig.toHeaderValue();
        String decoded = new String(Base64.decodeBase64(result));

        Assert.assertTrue(decoded.contains("b2F1dGgyOmVkYjEyMzcyLTRjYzItNDU5Yy1iNWMyLWM3YTNlMDExMGQyMg=="));
        Assert.assertTrue(decoded.contains("no-mail-required@example.org"));
        Assert.assertTrue(decoded.contains("oauth2"));
        Assert.assertTrue(decoded.contains("edb12372-4cc2-459c-b5c2-c7a3e0110d22"));
    }

    static class TestAuthConfigFactory extends AuthConfigFactory {

        private File userDockerCfgFile;

        public TestAuthConfigFactory(final PlexusContainer container, final File userDockerCfgFile) {
            super(container);
            this.userDockerCfgFile = userDockerCfgFile;
        }

        @Override
        protected File getUserDockerConfigFile() {
            return this.userDockerCfgFile;
        }

    }

}
