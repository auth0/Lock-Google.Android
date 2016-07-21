package com.auth0.android.google;

import com.auth0.android.Auth0Exception;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.request.AuthenticationRequest;
import com.auth0.android.result.Credentials;

import java.util.Map;

public class AuthenticationRequestMock implements AuthenticationRequest {

    private final boolean willSucceed;

    public AuthenticationRequestMock(boolean willSucceed) {
        this.willSucceed = willSucceed;
    }

    @Override
    public AuthenticationRequest setGrantType(String grantType) {
        return null;
    }

    @Override
    public AuthenticationRequest setConnection(String connection) {
        return null;
    }

    @Override
    public AuthenticationRequest setScope(String scope) {
        return null;
    }

    @Override
    public AuthenticationRequest setDevice(String device) {
        return null;
    }

    @Override
    public AuthenticationRequest setAccessToken(String accessToken) {
        return null;
    }

    @Override
    public AuthenticationRequest addAuthenticationParameters(Map<String, Object> parameters) {
        return null;
    }

    @Override
    public void start(BaseCallback<Credentials, AuthenticationException> callback) {
        if (willSucceed) {
            callback.onSuccess(new Credentials("idToken", "accessToken", "type", "refreshToken"));
        } else {
            callback.onFailure(new AuthenticationException("error"));
        }
    }

    @Override
    public Credentials execute() throws Auth0Exception {
        if (willSucceed) {
            return new Credentials("idToken", "accessToken", "type", "refreshToken");
        } else {
            throw new AuthenticationException("error");
        }
    }
}
