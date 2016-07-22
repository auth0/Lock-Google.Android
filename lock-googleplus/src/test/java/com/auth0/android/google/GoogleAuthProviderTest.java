package com.auth0.android.google;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.request.AuthenticationRequest;
import com.auth0.android.result.Credentials;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsArrayContaining;
import org.hamcrest.collection.IsArrayWithSize;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.junit.Assert.assertThat;

public class GoogleAuthProviderTest {

    private static final String CONNECTION_NAME = "google-oauth2";
    private static final int AUTH_REQ_CODE = 123;
    private static final int PERMISSION_REQ_CODE = 122;
    private static final String SERVER_CLIENT_ID = "server_client_id";
    private static final String TOKEN = "someR.andOm.Token";

    @Mock
    AuthenticationAPIClient client;
    @Mock
    AuthCallback callback;
    @Mock
    Activity activity;
    @Mock
    GoogleAPIHelper apiHelper;

    GoogleAuthProviderMock provider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        provider = new GoogleAuthProviderMock(client, SERVER_CLIENT_ID, apiHelper);
    }

    @Test
    public void shouldSetScopes() throws Exception {
        Scope scope1 = new Scope(Scopes.PLUS_LOGIN);
        Scope scope2 = new Scope(Scopes.EMAIL);
        Scope scope3 = new Scope(Scopes.PROFILE);

        provider.setScopes(scope1, scope2, scope3);

        assertThat(provider.getScopes(), is(arrayWithSize(3)));
        assertThat(provider.getScopes(), IsArrayContaining.hasItemInArray(scope1));
        assertThat(provider.getScopes(), IsArrayContaining.hasItemInArray(scope2));
        assertThat(provider.getScopes(), IsArrayContaining.hasItemInArray(scope3));
    }

    @Test
    public void shouldHaveNonNullScopes() throws Exception {
        assertThat(provider.getScopes(), is(notNullValue()));
    }

    @Test
    public void shouldHaveDefaultScope() throws Exception {
        assertThat(provider.getScopes(), is(arrayWithSize(1)));
        assertThat(provider.getScopes()[0].toString(), is(new Scope(Scopes.PLUS_LOGIN).toString()));
    }

    @Test
    public void shouldSetConnectionName() throws Exception {
        provider.setConnection("my-custom-connection");

        MatcherAssert.assertThat(provider.getConnection(), Is.is("my-custom-connection"));
    }

    @Test
    public void shouldHaveNonNullConnectionName() throws Exception {
        MatcherAssert.assertThat(provider.getConnection(), Is.is(notNullValue()));
    }

    @Test
    public void shouldHaveDefaultConnectionName() throws Exception {
        MatcherAssert.assertThat(provider.getConnection(), Is.is(CONNECTION_NAME));
    }

    @Test
    public void shouldCheckGooglePlayServicesAvailability() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        Mockito.verify(apiHelper).isGooglePlayServicesAvailable();
    }

    @Test
    public void shouldFailWithDialogIfGooglePlayServicesIsNotAvailable() throws Exception {
        Mockito.when(apiHelper.isGooglePlayServicesAvailable()).thenReturn(ConnectionResult.SERVICE_MISSING);
        Dialog dialog = Mockito.mock(Dialog.class);
        Mockito.when(apiHelper.getErrorDialog(ConnectionResult.SERVICE_MISSING, GoogleAuthProvider.REQUEST_RESOLVE_ERROR)).thenReturn(dialog);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        Mockito.verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldConnectTheGoogleAPIClientIfGooglePlayServicesIsAvailable() throws Exception {
        Mockito.when(apiHelper.isGooglePlayServicesAvailable()).thenReturn(ConnectionResult.SUCCESS);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        Mockito.verify(apiHelper).connectAndRequestGoogleAccount(AUTH_REQ_CODE, GoogleAuthProvider.REQUEST_RESOLVE_ERROR);
    }

    @Test
    public void shouldParseAuthorization() {
        Intent intent = Mockito.mock(Intent.class);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);

        Mockito.verify(apiHelper).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        Mockito.verify(apiHelper).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);
    }

    @Test
    public void shouldReturnDelegatedParseResult() {
        Intent intent = Mockito.mock(Intent.class);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        Mockito.when(apiHelper.parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent)).thenReturn(true);
        Mockito.when(apiHelper.parseSignInResult(999, Activity.RESULT_OK, intent)).thenReturn(false);

        assertThat(provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent), is(true));
        assertThat(provider.authorize(999, Activity.RESULT_OK, intent), is(false));
    }

    @Test
    public void shouldDoNothingWhenCalledFromNewIntent() {
        assertThat(provider.authorize(null), is(false));
    }

    @Test
    public void shouldNotRequirePermissions() {
        assertThat(provider.getRequiredAndroidPermissions(), is(notNullValue()));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayWithSize.<String>emptyArray()));
    }

    @Test
    public void shouldCallAuth0OAuthEndpointWhenGoogleTokenIsReceived() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        provider.tokenListener.onTokenReceived(TOKEN);

        Mockito.verify(client).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldCallAuth0OAuthEndpointWithCustomConnectionNameWhenGoogleTokenIsReceived() {
        provider.setConnection("my-custom-connection");
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, "my-custom-connection")).thenReturn(request);
        provider.tokenListener.onTokenReceived(TOKEN);

        Mockito.verify(client).loginWithOAuthAccessToken(TOKEN, "my-custom-connection");
    }

    @Test
    public void shouldFailWithDialogWhenErrorOcurred() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        Dialog dialog = Mockito.mock(Dialog.class);
        provider.tokenListener.onErrorReceived(dialog);

        Mockito.verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldFailWithTextWhenCredentialsRequestFailed() {
        final AuthenticationRequest authRequest = new AuthenticationRequestMock(false);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authRequest);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.tokenListener.onTokenReceived(TOKEN);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Integer> titleResCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> messageResCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(callback).onFailure(titleResCaptor.capture(), messageResCaptor.capture(), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue(), is(instanceOf(AuthenticationException.class)));
        assertThat(titleResCaptor.getValue(), is(R.string.com_auth0_google_authentication_failed_title));
        assertThat(messageResCaptor.getValue(), is(R.string.com_auth0_google_authentication_failed_message));
    }

    @Test
    public void shouldSucceedIfCredentialsRequestSucceeded() {
        final AuthenticationRequest authRequest = new AuthenticationRequestMock(true);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authRequest);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.tokenListener.onTokenReceived(TOKEN);

        ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
        Mockito.verify(callback).onSuccess(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue(), is(notNullValue()));
        assertThat(credentialsCaptor.getValue(), is(instanceOf(Credentials.class)));
    }

    @Test
    public void shouldLogoutOnClearSession() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.clearSession();
        Mockito.verify(apiHelper).logoutAndClearState();
    }

    @Test
    public void shouldLogoutOnStop() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.stop();
        Mockito.verify(apiHelper).logoutAndClearState();
    }

}