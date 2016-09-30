# Lock-Google

[![Build Status](https://travis-ci.org/auth0/Lock-Google.Android.svg?branch=master)](https://travis-ci.org/auth0/Lock-Google.Android)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://doge.mit-license.org)
[![Maven Central](https://img.shields.io/maven-central/v/com.auth0.android/lock-google.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.auth0.android%22%20AND%20a%3A%22lock-google%22)
[![Bintray](https://api.bintray.com/packages/auth0/lock-android/lock-google/images/download.svg) ](https://bintray.com/auth0/lock-android/lock-google/_latestVersion)

[Auth0](https://auth0.com) is an authentication broker that supports social identity providers as well as enterprise identity providers such as Active Directory, LDAP, Google Apps and Salesforce.

Lock-Google helps you integrate native Login with [Google Android SDK](https://developers.google.com/+/mobile/android/) and [Lock](https://auth0.com/lock)

## Requirements

Android 4.0 or later & Google Play Services 9.+

## Install

The Lock-Google is available through [Maven Central](http://search.maven.org) and [JCenter](https://bintray.com/bintray/jcenter). To install it, simply add the following line to your `build.gradle`:

```gradle
compile 'com.auth0.android:lock-google:1.0.0'
```

### Google Developers Console
1. Go to the [Google Developers Console](https://console.developers.google.com/) and create a new Project.
2. Add a new **OAuth client ID** [credential](https://console.developers.google.com/apis/credentials/oauthclient) for a **Web Application**. Complete the **Authorized redirect URIs** by filling the field with your callback URL, which should look like `https://{YOUR_DOMAIN}.auth0.com/login/callback`. Take note of the `CLIENT ID` and `CLIENT SECRET` values as we're going to use them later, both in your android application as well as in the Auth0 dashboard configuration.
3. Add a new **OAuth client ID** [credential](https://console.developers.google.com/apis/credentials/oauthclient) for an **Android** application. Obtain the **SHA-1** of the certificate you're using to sign your application and complete the first field with it. Complete the last field with your android application **Package Name** and then click the Create button. Take note of the `CLIENT ID` value as we're going to use it later in the Auth0 dashboard configuration.

### Auth0 Dashboard
1. Go to the Auth0 Dashboard and click [Social Connections](https://manage.auth0.com/#/connections/social). Click **Google** and a dialog will prompt.
2. Complete the "Client ID" field with the `CLIENT ID` value obtained in the step 2 of the **Google Developers Console** section above.
3. Complete the "Client Secret" field with the `CLIENT SECRET` value obtained in the step 2 of the **Google Developers Console** section above.
4. Complete the "Allowed Mobile Client IDs" field with the `CLIENT ID` obtained in the step 3 of the **Google Developers Console** section above. Click the Save button.
5. Return to the Auth0 Dashboard and click [Clients](https://manage.auth0.com/#/clients). If you haven't created one yet, do that first and get into your client configuration page. At the bottom of the page, click the "Show Advanced Settings" link and go to the "Mobile Settings" tab.
6. In the Android section, complete the **Package Name** with your application's package name. Finally, complete the **Key Hashes** field with the SHA-256 of the certificate you're using to sign your application. Click the "Save Changes" button.

### Android Application
1. In your android application, create a new String resource in the `res/strings.xml` file. Name it `google_server_client_id` and set as value the `CLIENT_ID` obtained in the step 2 of the **Google Developers Console** setup section above.
2. Add the Google Play Services version MetaData to the `AndroidManifest.xml` file, inside the Application tag.

```xml
<meta-data
    android:name="com.google.android.gms.version"
    android:value="@integer/google_play_services_version" />
```

3. Add the Internet Android permission to your `AndroidManifest.xml` file.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

4. When creating a new instance of the `GoogleAuthProvider` pass the `google_server_client_id` value as the first parameter:

```java
public class MainActivity extends AppCompatActivity {
  private GoogleAuthProvider provider;
  // ...

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Auth0 auth0 = new Auth0(getString(R.string.com_auth0_client_id), getString(R.string.com_auth0_domain));
    final AuthenticationAPIClient client = new AuthenticationAPIClient(auth0);
    provider = new GoogleAuthProvider(getString(R.string.google_server_client_id), client);
  }

  // ...
}
```

Depending on your use case, you'll need to add a few more lines of code to capture the authorization result. Follow the guides below:

> If you need further help with the setup, please check Google's [Sign-In for Android Guide](https://developers.google.com/identity/sign-in/android).


## Authenticate with Lock

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

That's it! When **Lock** needs to authenticate using that connection name, it will ask the `GoogleAuthHandler` for a valid `AuthProvider`.

> We provide this demo in the `FilesActivity` class. We also use the Google Drive SDK to get the user's Drive Files and show them on a list. Because of the Drive Scope, the SDK requires the user to grant the `GET_ACCOUNTS` android permission first. Keep in mind that _this only affects this demo_ and that if you only need to authenticate the user and get his public profile, the `GoogleAuthProvider` won't ask for additional permissions.

## Authenticate without Lock

Just create a new instance of `GoogleAuthProvider` with an `AuthenticationAPIClient` and the `Server Client ID` obtained in the Project's Credentials page.

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

Call `start` with a custom authentication request code to begin the authentication flow.

```java
provider.start(this, callback, RC_PERMISSIONS, RC_AUTHENTICATION);
```

That's it! You'll receive the result in the `AuthCallback` you passed.

> We provide this demo in the `SimpleActivity` class.


## Additional options

### Using a custom connection name
To use a custom social connection name to authorize against Auth0, create the GoogleAuthProvider instance using the second constructor:

```java
GoogleAuthProvider provider = new GoogleAuthProvider("my-connection", "google-server-client-id", client);
```

### Requesting a custom Google Scope
By default, the scope `Scopes.PLUS_LOGIN` is requested. You can customize the Scopes by calling `setScopes` with the list of Scopes. Each Google API (Auth, Drive, Plus..) specify it's own list of Scopes.

```java
provider.setScopes(Arrays.asList(new Scope(Scopes.PLUS_ME), new Scope(Scopes.PLUS_LOGIN)));
```

### Requesting custom Android Runtime Permissions
This provider doesn't require any special Android Manifest Permission to authenticate the user. But if your use case requires them, you can let the AuthProvider handle them for you. Use the `setRequiredPermissions` method.

```java
provider.setRequiredPermissions(new String[]{"android.permission.GET_ACCOUNTS"});
```

If you're not using Lock, then you'll have to handle the permission request result yourself. To do so, make your activity implement `ActivityCompat.OnRequestPermissionsResultCallback` and override the `onRequestPermissionsResult` method, calling `provider.onRequestPermissionsResult` with the activity context and the received parameters.

### Log out / Clear account.
To log out the user so that the next time he's prompted to input his credentials call `clearSession`. After you do this the provider state will be invalid and you will need to call `start` again before trying to `authorize` a result. Calling `stop` has the same effect.

```java
provider.clearSession();
```

### Remember the Last Login
By default, this provider will remember the last account used to log in. If you want to change this behavior, use the following method.

```java
provider.rememberLastLogin(false);
```

## Issue Reporting

If you have found a bug or if you have a feature request, please report them at this repository issues section. Please do not report security vulnerabilities on the public GitHub issue tracker. The [Responsible Disclosure Program](https://auth0.com/whitehat) details the procedure for disclosing security issues.

## What is Auth0?

Auth0 helps you to:

* Add authentication with [multiple authentication sources](https://docs.auth0.com/identityproviders), either social like **Google, Facebook, Microsoft Account, LinkedIn, GitHub, Twitter, Box, Salesforce, among others**, or enterprise identity systems like **Windows Azure AD, Google Apps, Active Directory, ADFS or any SAML Identity Provider**.
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
