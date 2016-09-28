package com.auth0.android.google;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.request.AuthenticationRequest;
import com.auth0.android.result.Credentials;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsArrayContaining;
import org.hamcrest.collection.IsArrayContainingInAnyOrder;
import org.hamcrest.collection.IsArrayWithSize;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class GoogleAuthProviderTest {

    private static final String CONNECTION_NAME = "google-oauth2";
    private static final int AUTH_REQ_CODE = 123;
    private static final int PERMISSION_REQ_CODE = 122;
    private static final String SERVER_CLIENT_ID = "server_client_id";
    private static final String TOKEN = "someR.andOm.Token";

    @Mock
    private AuthenticationAPIClient client;
    @Mock
    private AuthCallback callback;
    @Mock
    private Activity activity;
    @Mock
    private GoogleAPI apiHelper;

    private GoogleAuthProviderMock provider;

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

        verify(apiHelper).isGooglePlayServicesAvailable();
    }

    @Test
    public void shouldFailWithDialogIfGooglePlayServicesIsNotAvailable() throws Exception {
        when(apiHelper.isGooglePlayServicesAvailable()).thenReturn(ConnectionResult.SERVICE_MISSING);
        Dialog dialog = Mockito.mock(Dialog.class);
        when(apiHelper.getErrorDialog(ConnectionResult.SERVICE_MISSING, GoogleAuthProvider.REQUEST_RESOLVE_ERROR)).thenReturn(dialog);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldConnectTheGoogleAPIClientIfGooglePlayServicesIsAvailable() throws Exception {
        when(apiHelper.isGooglePlayServicesAvailable()).thenReturn(ConnectionResult.SUCCESS);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        verify(apiHelper).connectAndRequestGoogleAccount(AUTH_REQ_CODE, GoogleAuthProvider.REQUEST_RESOLVE_ERROR);
    }

    @Test
    public void shouldParseAuthorization() {
        Intent intent = Mockito.mock(Intent.class);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);

        verify(apiHelper).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        verify(apiHelper).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);
    }

    @Test
    public void shouldReturnDelegatedParseResult() {
        Intent intent = Mockito.mock(Intent.class);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        when(apiHelper.parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent)).thenReturn(true);
        when(apiHelper.parseSignInResult(999, Activity.RESULT_OK, intent)).thenReturn(false);

        assertThat(provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent), is(true));
        assertThat(provider.authorize(999, Activity.RESULT_OK, intent), is(false));
    }

    @Test
    public void shouldDoNothingWhenCalledFromNewIntent() {
        assertThat(provider.authorize(null), is(false));
    }

    @Test
    public void shouldNotRequireAndroidPermissions() {
        assertThat(provider.getRequiredAndroidPermissions(), is(notNullValue()));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayWithSize.<String>emptyArray()));
    }

    @Test
    public void shouldSetRequireAndroidPermissions() {
        String[] myPermissions = new String[]{"Permission.A", "Permission.B"};
        provider.setRequiredPermissions(myPermissions);

        assertThat(provider.getRequiredAndroidPermissions(), is(notNullValue()));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayWithSize.arrayWithSize(2)));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayContainingInAnyOrder.arrayContainingInAnyOrder("Permission.A", "Permission.B")));
    }

    @Test
    public void shouldNotCallAuth0OAuthEndpointWhenSomeScopesWereRejected() {
        provider.setScopes(new Scope("some-scope"));
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, Collections.<Scope>emptySet()));

        verify(client, VerificationModeFactory.noMoreInteractions()).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldFailWithTextWhenSomeScopesWereRejected() {
        provider.setScopes(new Scope("some-scope"));
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        final Set<Scope> scopes = Collections.emptySet();
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, scopes));
        verify(callback).onFailure(any(AuthenticationException.class));
    }


    @Test
    public void shouldCallAuth0OAuthEndpointWhenGoogleTokenIsReceived() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        final List<Scope> list = Arrays.asList(provider.getScopes());
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(list)));

        verify(client).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldCallAuth0OAuthEndpointWithCustomConnectionNameWhenGoogleTokenIsReceived() {
        provider.setConnection("my-custom-connection");
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        when(client.loginWithOAuthAccessToken(TOKEN, "my-custom-connection")).thenReturn(request);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(Arrays.asList(provider.getScopes()))));

        verify(client).loginWithOAuthAccessToken(TOKEN, "my-custom-connection");
    }

    @Test
    public void shouldFailWithDialogWhenErrorOccurred() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        Dialog dialog = Mockito.mock(Dialog.class);
        provider.googleCallback.onError(dialog);

        verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldFailWithTextWhenFacebookRequestIsCancelled() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onCancel();

        verify(callback).onFailure(any(AuthenticationException.class));
    }

    @Test
    public void shouldFailWithTextWhenCredentialsRequestFailed() {
        final AuthenticationRequest authRequest = new AuthenticationRequestMock(false);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authRequest);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(Arrays.asList(provider.getScopes()))));

        verify(callback).onFailure(any(AuthenticationException.class));
    }

    @Test
    public void shouldSucceedIfCredentialsRequestSucceeded() {
        final AuthenticationRequest authRequest = new AuthenticationRequestMock(true);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authRequest);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(Arrays.asList(provider.getScopes()))));

        ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
        verify(callback).onSuccess(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue(), is(notNullValue()));
        assertThat(credentialsCaptor.getValue(), is(instanceOf(Credentials.class)));
    }

    @Test
    public void shouldLogoutOnClearSession() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.clearSession();
        verify(apiHelper).logoutAndClearState();
    }

    @Test
    public void shouldLogoutOnStop() {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.stop();
        verify(apiHelper).logoutAndClearState();
    }

    private GoogleSignInAccount createGoogleSignInAccountFromToken(@NonNull String token, @NonNull Set<Scope> grantedScopes) {
        final GoogleSignInAccount account = Mockito.mock(GoogleSignInAccount.class);
        when(account.getIdToken()).thenReturn(token);
        when(account.getGrantedScopes()).thenReturn(grantedScopes);
        return account;
    }

}