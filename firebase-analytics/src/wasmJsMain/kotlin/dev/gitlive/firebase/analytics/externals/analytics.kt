@file:Suppress("ktlint:standard:property-naming", "PropertyName")
@file:JsModule("firebase/analytics")

package dev.gitlive.firebase.analytics.externals

import dev.gitlive.firebase.externals.FirebaseApp

public external fun getAnalytics(app: FirebaseApp? = definedExternally): FirebaseAnalytics

public external fun logEvent(app: FirebaseAnalytics, name: String, parameters: JsAny?)
public external fun setUserProperties(app: FirebaseAnalytics, properties: JsAny)
public external fun setUserId(app: FirebaseAnalytics, id: String?)
public external fun setDefaultEventParameters(app: FirebaseAnalytics, parameters: JsAny)
public external fun setAnalyticsCollectionEnabled(app: FirebaseAnalytics, enabled: Boolean)
public external fun setConsent(consentSettings: ConsentSettings)

public external interface FirebaseAnalytics : JsAny

public external interface ConsentSettings : JsAny {
    public var ad_personalization: String?
    public var ad_storage: String?
    public var ad_user_data: String?
    public var analytics_storage: String?
    public var functionality_storage: String?
    public var personalization_storage: String?
    public var security_storage: String?
}
