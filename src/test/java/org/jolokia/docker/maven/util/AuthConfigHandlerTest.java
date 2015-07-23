package org.jolokia.docker.maven.util;

import java.util.*;

import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jolokia.docker.maven.access.AuthConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import static mockit.Deencapsulation.getField;
import static org.junit.Assert.*;

/**
 * @author roland
 * @since 29.07.14
 */
@RunWith(JMockit.class)
public class AuthConfigHandlerTest {

    @Mocked
    Settings settings;

    private AuthConfigFactory factory;

    public static final class MockSecDispatcher extends MockUp<SecDispatcher> {
        @Mock
        public String decrypt(String password) {
            return password;
        }
    }

    @Mocked
    PlexusContainer container;

    @Before
    public void containerSetup() throws ComponentLookupException {
        final SecDispatcher secDispatcher = new MockSecDispatcher().getMockInstance();
        new NonStrictExpectations() {{
            container.lookup(SecDispatcher.ROLE, "maven"); result = secDispatcher;

        }};
        factory = new AuthConfigFactory(container);
    }

    @Test
    public void testEmpty() throws Exception {
        assertNull(factory.createAuthConfig(null, settings, null, null));
    }

    @Test
    public void testSystemProperty() throws Exception {
        System.setProperty("docker.username","roland");
        System.setProperty("docker.password", "secret");
        System.setProperty("docker.email", "roland@jolokia.org");
        try {
            AuthConfig config = factory.createAuthConfig(null,settings, null, null);
            verifyAuthConfig(config,"roland","secret","roland@jolokia.org");
        } finally {
            System.clearProperty("docker.username");
            System.clearProperty("docker.password");
            System.clearProperty("docker.email");
        }
    }

    @Test(expected = MojoExecutionException.class)
    public void testSystemPropertyNoUser() throws Exception {
        checkException("docker.password");
    }

    @Test(expected = MojoExecutionException.class)
    public void testSystemPropertyNoPassword() throws Exception {
        checkException("docker.username");
    }

    private void checkException(String key) throws MojoExecutionException {
        System.setProperty(key, "secret");
        try {
            factory.createAuthConfig(null, settings, null, null);
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    public void testFromPluginConfiguration() throws MojoExecutionException {
        Map pluginConfig = new HashMap();
        pluginConfig.put("username", "roland");
        pluginConfig.put("password", "secret");
        pluginConfig.put("email", "roland@jolokia.org");

        AuthConfig config = factory.createAuthConfig(pluginConfig,settings,null,null);
        verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
    }


    @Test(expected = MojoExecutionException.class)
    public void testFromPluginConfigurationFailed() throws MojoExecutionException {
        Map pluginConfig = new HashMap();
        pluginConfig.put("password", "secret");
        factory.createAuthConfig(pluginConfig, settings, null, null);
    }

    @Test
    public void testFromSettingsSimple() throws MojoExecutionException {
        setupServers();
        AuthConfig config = factory.createAuthConfig(null,settings, "roland", "test.org");
        assertNotNull(config);
        verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
    }

    @Test
    public void testFromSettingsDefault() throws MojoExecutionException {
        setupServers();
        AuthConfig config = factory.createAuthConfig(null,settings, "rhuss", "test.org");
        assertNotNull(config);
        verifyAuthConfig(config, "rhuss", "secret2", "rhuss@redhat.com");
    }

    @Test
    public void testFormSettingsDefault() throws MojoExecutionException {
        setupServers();
        AuthConfig config = factory.createAuthConfig(null,settings,"tanja",null);
        assertNotNull(config);
        verifyAuthConfig(config,"tanja","doublesecret","tanja@jolokia.org");
    }

    @Test
    public void testWrongUserName() throws MojoExecutionException {
        setupServers();
        assertNull(factory.createAuthConfig(null, settings, "roland", "another.repo.org"));
    }


    private void setupServers() {
        new NonStrictExpectations() {{
            List<Server> servers = new ArrayList<>();
            String data[] = {
                    "test.org", "rhuss", "secret2", "rhuss@redhat.com",
                    "test.org/roland", "roland", "secret", "roland@jolokia.org",
                    "docker.io", "tanja", "doublesecret", "tanja@jolokia.org",
                    "another.repo.org/joe", "joe", "3secret", "joe@foobar.com"
            };
            for (int i = 0; i < data.length; i += 4) {
                Server server = new Server();
                server.setId(data[i]);
                server.setUsername(data[i+1]);
                server.setPassword(data[i+2]);
                Xpp3Dom dom = new Xpp3Dom("configuration");
                Xpp3Dom email = new Xpp3Dom("email");
                email.setValue(data[i+3]);
                dom.addChild(email);
                server.setConfiguration(dom);
                servers.add(server);
            }
            settings.getServers();
            result = servers;
        }};
    }

    private void verifyAuthConfig(AuthConfig config, String username, String password, String email) {
        Map params = getField(config,"params");
        assertEquals(username,params.get("username"));
        assertEquals(password,params.get("password"));
        assertEquals(email, params.get("email"));
    }

}
