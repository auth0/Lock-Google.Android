package com.auth0.android.google;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.request.AuthenticationRequest;
import com.auth0.android.result.Credentials;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsArrayContaining;
import org.hamcrest.collection.IsArrayWithSize;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
    private GoogleAPI google;
    @Mock
    private AuthenticationRequest authenticationRequest;
    @Mock
    private Credentials credentials;

    private GoogleAuthProviderMock provider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        provider = new GoogleAuthProviderMock(SERVER_CLIENT_ID, client, google);
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
    public void shouldHaveNonNullConnectionName() throws Exception {
        MatcherAssert.assertThat(provider.getConnection(), is(notNullValue()));
    }

    @Test
    public void shouldHaveDefaultConnectionName() throws Exception {
        MatcherAssert.assertThat(provider.getConnection(), is(CONNECTION_NAME));
    }

    @Test
    public void shouldCreateWithCustomConnectionName() throws Exception {
        provider = new GoogleAuthProviderMock("custom-connection", SERVER_CLIENT_ID, client, google);
        MatcherAssert.assertThat(provider.getConnection(), is(notNullValue()));
        MatcherAssert.assertThat(provider.getConnection(), is("custom-connection"));
    }

    @Test
    public void shouldCheckGooglePlayServicesAvailability() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        verify(google).isGooglePlayServicesAvailable();
    }

    @Test
    public void shouldFailWithDialogIfGooglePlayServicesIsNotAvailable() throws Exception {
        when(google.isGooglePlayServicesAvailable()).thenReturn(ConnectionResult.SERVICE_MISSING);
        Dialog dialog = mock(Dialog.class);
        when(google.getErrorDialog(ConnectionResult.SERVICE_MISSING, GoogleAuthProvider.REQUEST_RESOLVE_ERROR)).thenReturn(dialog);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldConnectTheGoogleAPIClientIfGooglePlayServicesIsAvailable() throws Exception {
        when(google.isGooglePlayServicesAvailable()).thenReturn(ConnectionResult.SUCCESS);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        verify(google).connectAndRequestGoogleAccount(AUTH_REQ_CODE, GoogleAuthProvider.REQUEST_RESOLVE_ERROR);
    }

    @Test
    public void shouldParseAuthorization() throws Exception {
        Intent intent = mock(Intent.class);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);

        verify(google).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        verify(google).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);
    }

    @Test
    public void shouldReturnDelegatedParseResult() throws Exception {
        Intent intent = mock(Intent.class);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        when(google.parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent)).thenReturn(true);
        when(google.parseSignInResult(999, Activity.RESULT_OK, intent)).thenReturn(false);

        assertThat(provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent), is(true));
        assertThat(provider.authorize(999, Activity.RESULT_OK, intent), is(false));
    }

    @Test
    public void shouldDoNothingWhenCalledFromNewIntent() throws Exception {
        assertThat(provider.authorize(null), is(false));
    }

    @Test
    public void shouldNotRequireAndroidPermissions() throws Exception {
        assertThat(provider.getRequiredAndroidPermissions(), is(notNullValue()));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayWithSize.<String>emptyArray()));
    }

    @Test
    public void shouldNotLogoutBeforeLoginByDefault() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        assertThat(provider.willLogoutBeforeLogin(), is(false));
    }

    @Test
    public void shouldLogoutBeforeLoginIfRequested() throws Exception {
        provider.rememberLastLogin(false);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        assertThat(provider.willLogoutBeforeLogin(), is(true));
    }

    @Test
    public void shouldSetRequireAndroidPermissions() throws Exception {
        String[] myPermissions = new String[]{"Permission.A", "Permission.B"};
        provider.setRequiredPermissions(myPermissions);

        assertThat(provider.getRequiredAndroidPermissions(), is(notNullValue()));
        assertThat(provider.getRequiredAndroidPermissions(), is(arrayWithSize(2)));
        assertThat(provider.getRequiredAndroidPermissions(), is(arrayContainingInAnyOrder("Permission.A", "Permission.B")));
    }

    @Test
    public void shouldNotCallAuth0OAuthEndpointWhenSomeScopesWereRejected() throws Exception {
        provider.setScopes(new Scope("some-scope"));
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(authenticationRequest);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, Collections.<Scope>emptySet()));

        verify(client, VerificationModeFactory.noMoreInteractions()).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldFailWithExceptionWhenSomeScopesWereRejected() throws Exception {
        provider.setScopes(new Scope("some-scope"));
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);

        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(authenticationRequest);
        final Set<Scope> scopes = Collections.emptySet();
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, scopes));
        verify(callback).onFailure(any(AuthenticationException.class));
    }


    @Test
    public void shouldCallAuth0OAuthEndpointWhenGoogleTokenIsReceived() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(authenticationRequest);
        final List<Scope> list = Arrays.asList(provider.getScopes());
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(list)));

        verify(client).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldCallAuth0OAuthEndpointWithCustomConnectionNameWhenGoogleTokenIsReceived() throws Exception {
        provider = new GoogleAuthProviderMock("my-custom-connection", SERVER_CLIENT_ID, client, google);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        when(client.loginWithOAuthAccessToken(TOKEN, "my-custom-connection")).thenReturn(authenticationRequest);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(Arrays.asList(provider.getScopes()))));

        verify(client).loginWithOAuthAccessToken(TOKEN, "my-custom-connection");
    }

    @Test
    public void shouldFailWithDialogWhenErrorOccurred() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        Dialog dialog = mock(Dialog.class);
        provider.googleCallback.onError(dialog);

        verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldFailWithExceptionWhenRequestIsCancelled() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onCancel();

        verify(callback).onFailure(any(AuthenticationException.class));
    }

    @Test
    public void shouldFailWithExceptionWhenCredentialsRequestFailed() {
        shouldFailRequest(authenticationRequest);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authenticationRequest);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(Arrays.asList(provider.getScopes()))));

        verify(callback).onFailure(any(AuthenticationException.class));
    }

    @Test
    public void shouldSucceedIfCredentialsRequestSucceeded() throws Exception {
        shouldYieldCredentialsForRequest(authenticationRequest, credentials);
        when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authenticationRequest);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<>(Arrays.asList(provider.getScopes()))));

        verify(callback).onSuccess(eq(credentials));
    }

    @Test
    public void shouldDisconnectGoogleClientBeforeReUsing() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        verify(google, times(1)).disconnect();
    }

    @Test
    public void shouldLogoutOnClearSession() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.clearSession();
        verify(google).logoutAndClearState();
    }

    @Test
    public void shouldLogoutOnStop() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.stop();
        verify(google).logoutAndClearState();
    }

    private GoogleSignInAccount createGoogleSignInAccountFromToken(@NonNull String token, @NonNull Set<Scope> grantedScopes) {
        final GoogleSignInAccount account = mock(GoogleSignInAccount.class);
        when(account.getIdToken()).thenReturn(token);
        when(account.getGrantedScopes()).thenReturn(grantedScopes);
        return account;
    }

    private void shouldYieldCredentialsForRequest(AuthenticationRequest request, final Credentials credentials) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                //noinspection unchecked
                BaseCallback<Credentials, AuthenticationException> callback = (BaseCallback<Credentials, AuthenticationException>) invocation.getArguments()[0];
                callback.onSuccess(credentials);
                return null;
            }
        }).when(request).start(Matchers.<BaseCallback<Credentials, AuthenticationException>>any());
    }

    private void shouldFailRequest(AuthenticationRequest request) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                //noinspection unchecked
                BaseCallback<Credentials, AuthenticationException> callback = (BaseCallback<Credentials, AuthenticationException>) invocation.getArguments()[0];
                callback.onFailure(new AuthenticationException("error"));
                return null;
            }
        }).when(request).start(Matchers.<BaseCallback<Credentials, AuthenticationException>>any());
    }
}