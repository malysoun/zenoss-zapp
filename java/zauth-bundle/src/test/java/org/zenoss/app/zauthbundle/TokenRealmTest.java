package org.zenoss.app.zauthbundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yammer.dropwizard.client.HttpClientBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.junit.Before;
import org.junit.Test;

import org.zenoss.app.config.ProxyConfiguration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class TokenRealmTest {


    HttpServletRequest request;
    HttpServletResponse response;
    TokenRealm realm;
    HttpClient mockClient;

    @Before
    public void setup() {
        this.request = mock(HttpServletRequest.class);
        this.response = mock(HttpServletResponse.class);
        this.mockClient = mock(HttpClient.class);
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        ClientConnectionManager connectionManager = mock(ClientConnectionManager.class);
        when(builder.build()).thenReturn(mockClient);
        when(mockClient.getConnectionManager()).thenReturn(connectionManager);
        this.realm = new TokenRealm(builder);
        TokenRealm.setProxyConfiguration(new ProxyConfiguration());
    }

    @Test
    public void testAuthenticationTokenClassIsPresent() {
        Class<? extends AuthenticationToken> cls = realm.getAuthenticationTokenClass();
        assertEquals(cls, StringAuthenticationToken.class);
    }

    @Test
    public void testgetPostMethod() throws Exception{
        HttpPost method = realm.getPostMethod("test");
        // make sure we set our token we passed in into the params
        assertEquals("test", method.getParams().getParameter("id"));
        assertEquals(true, method.getURI().toString().startsWith("http://"));
    }

    @Test
    public void testSuccessfulResponse() throws Exception {
        HttpResponse response = getOkResponse();
        AuthenticationInfo info = realm.handleResponse("test", response);
        assertFalse(info.getPrincipals().isEmpty());
        assertEquals(info.getCredentials().toString(), "test");
    }

    private HttpResponse getOkResponse() {
        StatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
        return new BasicHttpResponse(status);
    }

    @Test(expected = AuthenticationException.class)
    public void testHandleBadResponse() throws Exception {
        StatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, 401, "Unauthorized");
        HttpResponse response = new BasicHttpResponse(status);
        AuthenticationInfo info = realm.handleResponse("test", response);
    }

    @Test
    public void testDoGetAuthorization() throws Exception {
        when(this.mockClient.execute(any(HttpPost.class))).thenReturn(getOkResponse());
        AuthenticationToken token = new StringAuthenticationToken("test");
        AuthenticationInfo results = realm.doGetAuthenticationInfo(token);
        assertEquals(results.getCredentials().toString(), "test");
    }

}
