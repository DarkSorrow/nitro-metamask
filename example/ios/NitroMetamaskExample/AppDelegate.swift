import UIKit
import React
import React_RCTAppDelegate
import ReactAppDependencyProvider
import metamask_ios_sdk

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
  var window: UIWindow?

  var reactNativeDelegate: ReactNativeDelegate?
  var reactNativeFactory: RCTReactNativeFactory?

  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    let delegate = ReactNativeDelegate()
    let factory = RCTReactNativeFactory(delegate: delegate)
    delegate.dependencyProvider = RCTAppDependencyProvider()

    reactNativeDelegate = delegate
    reactNativeFactory = factory

    window = UIWindow(frame: UIScreen.main.bounds)

    factory.startReactNative(
      withModuleName: "NitroMetamaskExample",
      in: window,
      launchOptions: launchOptions
    )

    return true
  }
  
  // Handle deep links from MetaMask wallet
  // MetaMask returns to the app via deep link after signing/connecting
  // Reference: https://github.com/MetaMask/metamask-ios-sdk
  func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    // Check if this is a MetaMask deep link (host="mmsdk")
    if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
       components.host == "mmsdk" {
      // Handle MetaMask deep link return
      MetaMaskSDK.sharedInstance?.handleUrl(url)
      return true
    }
    
    // Handle other deep links (e.g., React Native Linking)
    return false
  }
}

class ReactNativeDelegate: RCTDefaultReactNativeFactoryDelegate {
  override func sourceURL(for bridge: RCTBridge) -> URL? {
    self.bundleURL()
  }

  override func bundleURL() -> URL? {
#if DEBUG
    RCTBundleURLProvider.sharedSettings().jsBundleURL(forBundleRoot: "index")
#else
    Bundle.main.url(forResource: "main", withExtension: "jsbundle")
#endif
  }
}
