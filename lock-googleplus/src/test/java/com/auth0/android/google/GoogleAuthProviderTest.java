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

import java.util.HashSet;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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
    public void shouldParseAuthorization() throws Exception {
        Intent intent = Mockito.mock(Intent.class);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        provider.authorize(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);

        Mockito.verify(apiHelper).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent);
        Mockito.verify(apiHelper).parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_CANCELED, intent);
    }

    @Test
    public void shouldReturnDelegatedParseResult() throws Exception {
        Intent intent = Mockito.mock(Intent.class);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        Mockito.when(apiHelper.parseSignInResult(AUTH_REQ_CODE, Activity.RESULT_OK, intent)).thenReturn(true);
        Mockito.when(apiHelper.parseSignInResult(999, Activity.RESULT_OK, intent)).thenReturn(false);

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
        provider.forceRequestAccount(true);
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        assertThat(provider.willLogoutBeforeLogin(), is(true));
    }

    @Test
    public void shouldSetRequireAndroidPermissions() throws Exception {
        String[] myPermissions = new String[]{"Permission.A", "Permission.B"};
        provider.setRequiredPermissions(myPermissions);

        assertThat(provider.getRequiredAndroidPermissions(), is(notNullValue()));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayWithSize.arrayWithSize(2)));
        assertThat(provider.getRequiredAndroidPermissions(), is(IsArrayContainingInAnyOrder.arrayContainingInAnyOrder("Permission.A", "Permission.B")));
    }

    @Test
    public void shouldNotCallAuth0OAuthEndpointWhenSomeScopesWereRejected() throws Exception {
        provider.setScopes(new Scope("some-scope"));
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, Collections.emptySet()));

        Mockito.verify(client, VerificationModeFactory.noMoreInteractions()).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldFailWithTextWhenSomeScopesWereRejected() throws Exception {
        provider.setScopes(new Scope("some-scope"));
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, Collections.emptySet()));

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Integer> titleResCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> messageResCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(callback).onFailure(titleResCaptor.capture(), messageResCaptor.capture(), throwableCaptor.capture());
        MatcherAssert.assertThat(throwableCaptor.getValue(), Is.is(nullValue()));
        MatcherAssert.assertThat(titleResCaptor.getValue(), Is.is(R.string.com_auth0_google_authentication_failed_missing_permissions_title));
        MatcherAssert.assertThat(messageResCaptor.getValue(), Is.is(R.string.com_auth0_google_authentication_failed_missing_permissions_message));
    }


    @Test
    public void shouldCallAuth0OAuthEndpointWhenGoogleTokenIsReceived() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME)).thenReturn(request);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<Scope>(Arrays.asList(provider.getScopes()))));

        Mockito.verify(client).loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME);
    }

    @Test
    public void shouldCallAuth0OAuthEndpointWithCustomConnectionNameWhenGoogleTokenIsReceived() throws Exception {
        provider.setConnection("my-custom-connection");
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        final AuthenticationRequest request = Mockito.mock(AuthenticationRequest.class);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, "my-custom-connection")).thenReturn(request);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<Scope>(Arrays.asList(provider.getScopes()))));

        Mockito.verify(client).loginWithOAuthAccessToken(TOKEN, "my-custom-connection");
    }

    @Test
    public void shouldFailWithDialogWhenErrorOccurred() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        Dialog dialog = Mockito.mock(Dialog.class);
        provider.googleCallback.onError(dialog);

        Mockito.verify(callback).onFailure(dialog);
    }

    @Test
    public void shouldFailWithTextWhenFacebookRequestIsCancelled() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onCancel();

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Integer> titleResCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> messageResCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(callback).onFailure(titleResCaptor.capture(), messageResCaptor.capture(), throwableCaptor.capture());
        MatcherAssert.assertThat(throwableCaptor.getValue(), Is.is(nullValue()));
        MatcherAssert.assertThat(titleResCaptor.getValue(), Is.is(R.string.com_auth0_google_authentication_cancelled_title));
        MatcherAssert.assertThat(messageResCaptor.getValue(), Is.is(R.string.com_auth0_google_authentication_cancelled_error_message));
    }

    @Test
    public void shouldFailWithTextWhenCredentialsRequestFailed() throws Exception {
        final AuthenticationRequest authRequest = new AuthenticationRequestMock(false);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authRequest);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<Scope>(Arrays.asList(provider.getScopes()))));

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Integer> titleResCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> messageResCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(callback).onFailure(titleResCaptor.capture(), messageResCaptor.capture(), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue(), is(instanceOf(AuthenticationException.class)));
        assertThat(titleResCaptor.getValue(), is(R.string.com_auth0_google_authentication_failed_title));
        assertThat(messageResCaptor.getValue(), is(R.string.com_auth0_google_authentication_failed_message));
    }

    @Test
    public void shouldSucceedIfCredentialsRequestSucceeded() throws Exception {
        final AuthenticationRequest authRequest = new AuthenticationRequestMock(true);
        Mockito.when(client.loginWithOAuthAccessToken(TOKEN, CONNECTION_NAME))
                .thenReturn(authRequest);

        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.googleCallback.onSuccess(createGoogleSignInAccountFromToken(TOKEN, new HashSet<Scope>(Arrays.asList(provider.getScopes()))));

        ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
        Mockito.verify(callback).onSuccess(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue(), is(notNullValue()));
        assertThat(credentialsCaptor.getValue(), is(instanceOf(Credentials.class)));
    }

    @Test
    public void shouldLogoutOnClearSession() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.clearSession();
        Mockito.verify(apiHelper).logoutAndClearState();
    }

    @Test
    public void shouldLogoutOnStop() throws Exception {
        provider.start(activity, callback, PERMISSION_REQ_CODE, AUTH_REQ_CODE);
        provider.stop();
        Mockito.verify(apiHelper).logoutAndClearState();
    }

    private GoogleSignInAccount createGoogleSignInAccountFromToken(@NonNull String token, @NonNull Set<Scope> grantedScopes) {
        final GoogleSignInAccount account = Mockito.mock(GoogleSignInAccount.class);
        Mockito.when(account.getIdToken()).thenReturn(token);
        Mockito.when(account.getGrantedScopes()).thenReturn(grantedScopes);
        return account;
    }

}