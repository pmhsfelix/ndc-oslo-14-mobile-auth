# ndc-oslo-14-mobile-auth #

Demos for the NDC Oslo 2014 session on mobile authentication and authorization.
See the [video](http://vimeo.com/97349269) and the [slides](https://speakerdeck.com/pmhsfelix/single-sign-on-for-mobile-native-applications) for more information.

# Web View Client #

This demo shows how to use the `WebView` class to perform a OAuth 2.0 code flow on an Android device.
Uses the [GitHub API OAuth 2.0](https://developer.github.com/v3/oauth/) _Authorization Server_ and _Resource Server_. 
Before usage, this demo requires the provision of an OAuth 2.0 client at [https://github.com/settings/applications](https://github.com/settings/applications).
The provisioned client ID, client secret and redirect URI must be configured at `res\values\strings.xml`

# Google client #

This demo shows how to use Google Play Services to authenticate users and access tokens and ID tokens.
Before usage, this demo requires the provision of two clients at the [Google Developers Console](https://console.developers.google.com).
The first one should be an Android client and it's provision requires the package name (`com.example.googleclient`) and the associated certificate's hash value (`keytool -exportcert -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -list –v
`).
The second client should be a Web client and its client ID must be configured at `res\values\strings.xml`.
