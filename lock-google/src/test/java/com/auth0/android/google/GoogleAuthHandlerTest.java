package com.auth0.android.google;

import com.auth0.android.provider.AuthProvider;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GoogleAuthHandlerTest {

    private GoogleAuthProvider provider;
    private GoogleAuthHandler handler;

    @Before
    public void setUp() throws Exception {
        provider = mock(GoogleAuthProvider.class);
        handler = new GoogleAuthHandler(provider);
    }

    @Test
    public void shouldGetFacebookProvider() throws Exception {
        final AuthProvider p = handler.providerFor("google-oauth2", "google-oauth2");
        assertThat(p, is(notNullValue()));
        assertThat(p, is(CoreMatchers.instanceOf(GoogleAuthProvider.class)));
        assertThat(p, is(equalTo((AuthProvider) provider)));
    }

    @Test
    public void shouldGetNullProvider() throws Exception {
        final AuthProvider p = handler.providerFor("some-strategy", "some-connection");
        assertThat(p, is(nullValue()));
    }

}