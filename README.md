# Lock-Google

[![Build Status](https://travis-ci.org/auth0/Lock-Google.Android.svg?branch=master)](https://travis-ci.org/auth0/Lock-Google.Android)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://doge.mit-license.org)
[![Maven Central](https://img.shields.io/maven-central/v/com.auth0.android/lock-googleplus.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.auth0.android%22%20AND%20a%3A%22lock-googleplus%22)
[![Bintray](https://api.bintray.com/packages/auth0/lock-android/lock-googleplus/images/download.svg) ](https://bintray.com/auth0/lock-android/lock-googleplus/_latestVersion)

[Auth0](https://auth0.com) is an authentication broker that supports social identity providers as well as enterprise identity providers such as Active Directory, LDAP, Google Apps and Salesforce.

Lock-Google helps you integrate native Login with [Google+ Android SDK](https://developers.google.com/+/mobile/android/) and [Lock](https://auth0.com/lock)

## Requirements

Android 4.0 or later & Google Play Services 9.+

## Before you start using Lock-Google

In order to use Google APIs you'll need to register your Android application in [Google Developer Console](https://console.developers.google.com/project) and get your `Web Client ID`.
We recommend following Google's [quickstart](https://developers.google.com/mobile/add?platform=android), just pick `Google Sign-In`. Then generate a new `OAuth 2.0 Client ID` for a Web Application in the [Credentials](https://console.developers.google.com/apis/credentials?project=_) page. Take the value and use it to configure your Google Connection's `Client ID` in [Auth0 Dashboard](https://manage.auth0.com/#/connections/social). Save this value for later as it will be used in the android provider configuration.


> For more information please check Google's [documentation](https://developers.google.com/identity/sign-in/android/)

### Auth0 Connection with multiple Google clientIDs (Web & Mobile)

If you also have a Web Application, and a Google clientID & secret for it configured in Auth0, you need to whitelist the Google clientID of your mobile application in your Auth0 connection. With your Mobile clientID from Google, go to [Social Connections](https://manage.auth0.com/#/connections/social), select **Google** and add the clientID to the field named `Allowed Mobile Client IDs`

## Install

The Lock-Google is available through [Maven Central](http://search.maven.org) and [JCenter](https://bintray.com/bintray/jcenter). To install it, simply add the following line to your `build.gradle`:

```gradle
compile 'com.auth0.android:lock-google:1.0.+'
```

Then in your project's `AndroidManifest.xml` add the following entry inside the application tag.

```xml
<meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
```


### Android Permissions

In your project's `AndroidManifest.xml` add the following permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Google Sign-In does not require additional android permissions.


## Usage


### With Lock

This library includes an implementation of the `AuthHandler` interface for you to use it directly with **Lock**. Create a new instance of the `GoogleAuthHandler` class passing a valid `GoogleAuthProvider`. Don't forget to customize the scopes if you need to. 
 
```java
Auth0 auth0 = new Auth0("auth0-client-id", "auth0-domain");
AuthenticationAPIClient client = new AuthenticationAPIClient(auth0);

GoogleAuthProvider provider = new GoogleAuthProvider("google-server-client-id", client);
provider.setScopes(new Scope(DriveScopes.DRIVE_METADATA_READONLY));
provider.setRequiredPermissions(new String[]{"android.permission.GET_ACCOUNTS"});

GoogleAuthHandler handler = new GoogleAuthHandler(provider);
```

Finally in the Lock Builder, call `withAuthHandlers` passing the recently created instance. 

```java
lock = Lock.newBuilder(auth0, authCallback)
        .withAuthHandlers(handler)
        //...
        .build(this);
```

That's it! When **Lock** needs to authenticate using that connection name, it will ask the `AuthProviderResolver` for a valid `AuthProvider`.

> We provide this demo in the `FilesActivity` class. We also use the Google Drive SDK to get the user's Drive Files and show them on a list. Because of the Drive Scope, the SDK requires the user to grant the `GET_ACCOUNTS` android permission first. Keep in mind that _this only affects this demo_ and that if you only need to authenticate the user and get his public profile, the `GoogleAuthProvider` won't ask for additional permissions.

### Without Lock

Just create a new instance of `GoogleAuthProvider` with an `AuthenticationAPIClient` and the `Server Client ID` obtained in the Project Credential's page.

```java
Auth0 auth0 = new Auth0("auth0-client-id", "auth0-domain");
AuthenticationAPIClient client = new AuthenticationAPIClient(auth0);
GoogleAuthProvider provider = new GoogleAuthProvider("google-server-client-id", client);
```

Override your activity's `onActivityResult` method and redirect the received parameters to the provider instance's `authorize` method.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (provider.authorize(requestCode, resultCode, data)) {
        return;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

Call `start` with a custom authentication request code to begin the authentication flow. The permissions request code is ignored as this provider doesn't need any custom android permissions.

```java
provider.start(this, callback, RC_PERMISSIONS, RC_AUTHENTICATION);
```

That's it! You'll receive the result in the `AuthCallback` you passed.

> We provide this demo in the `SimpleActivity` class.

## Using a custom connection name
To use a custom social connection name to authorize against Auth0, create the GoogleAuthProvider instance using the second constructor:

```java
GoogleAuthProvider provider = new GoogleAuthProvider("my-connection", "google-server-client-id", client);
```

## Requesting a custom Scope
By default, the scope `Scopes.PLUS_LOGIN` is requested. You can customize the Scopes by calling `setScopes` with the list of Scopes. Each Google API (Auth, Drive, Plus..) specify it's own list of Scopes.

```java
provider.setScopes(Arrays.asList(new Scope(Scopes.PLUS_ME), new Scope(Scopes.PLUS_LOGIN)));
```

## Using custom Android Runtime Permissions
This provider doesn't require any special Android Manifest Permission to authenticate the user. But if your use case requires them, you can let the AuthProvider handle them for you. Use the `setRequiredPermissions` method.
 
```java
provider.setRequiredPermissions(new String[]{"android.permission.GET_ACCOUNTS"});
```

If you're not using Lock, then you'll have to handle the permission request result yourself. To do so, make your activity implement `ActivityCompat.OnRequestPermissionsResultCallback` and override the `onRequestPermissionsResult` method, calling `provider.onRequestPermissionsResult` with the activity context and the received parameters.

## Log out / Clear account.
To log out the user so that the next time he's prompted to select an account call `clearSession`. After you do this the provider state will be invalid and you will need to call `start` again before trying to `authorize` a result.

```java
provider.clearSession();
```

> Calling `stop` has the same effect.


## Issue Reporting

If you have found a bug or if you have a feature request, please report them at this repository issues section. Please do not report security vulnerabilities on the public GitHub issue tracker. The [Responsible Disclosure Program](https://auth0.com/whitehat) details the procedure for disclosing security issues.

## What is Auth0?

Auth0 helps you to:

* Add authentication with [multiple authentication sources](https://docs.auth0.com/identityproviders), either social like **Google, Facebook, Microsoft Account, LinkedIn, GitHub, Twitter, Box, Salesforce, amont others**, or enterprise identity systems like **Windows Azure AD, Google Apps, Active Directory, ADFS or any SAML Identity Provider**.
* Add authentication through more traditional **[username/password databases](https://docs.auth0.com/mysql-connection-tutorial)**.
* Add support for **[linking different user accounts](https://docs.auth0.com/link-accounts)** with the same user.
* Support for generating signed [Json Web Tokens](https://docs.auth0.com/jwt) to call your APIs and **flow the user identity** securely.
* Analytics of how, when and where users are logging in.
* Pull data from other sources and add it to the user profile, through [JavaScript rules](https://docs.auth0.com/rules).

## Create a free account in Auth0

1. Go to [Auth0](https://auth0.com) and click Sign Up.
2. Use Google, GitHub or Microsoft Account to login.

## Author

[Auth0](auth0.com)

## License

Lock-Google is available under the MIT license. See the [LICENSE](LICENSE) file for more info.
