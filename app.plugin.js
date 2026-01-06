const { withAppDelegate } = require('@expo/config-plugins');

const withMetamaskAppDelegate = (config) => {
  return withAppDelegate(config, (config) => {
    const { modResults } = config;
    
    // Check if AppDelegate is Swift
    if (modResults.language === 'swift') {
      // Check if the method already exists
      if (modResults.contents.includes('MetaMaskSDK.shared.handleUrl')) {
        return config;
      }
      
      // Add import if not present
      if (!modResults.contents.includes('import MetaMaskSDK')) {
        // Find the last import statement and add after it
        const importRegex = /^import\s+.*$/gm;
        const imports = modResults.contents.match(importRegex);
        if (imports && imports.length > 0) {
          const lastImport = imports[imports.length - 1];
          const lastImportIndex = modResults.contents.lastIndexOf(lastImport);
          modResults.contents = 
            modResults.contents.slice(0, lastImportIndex + lastImport.length) +
            '\nimport MetaMaskSDK' +
            modResults.contents.slice(lastImportIndex + lastImport.length);
        } else {
          // No imports found, add at the top after the first line
          const firstLineIndex = modResults.contents.indexOf('\n');
          modResults.contents = 
            modResults.contents.slice(0, firstLineIndex + 1) +
            'import MetaMaskSDK\n' +
            modResults.contents.slice(firstLineIndex + 1);
        }
      }
      
      // Add the deep link handler method
      const deepLinkHandler = `
  // Handle deep links from MetaMask wallet
  // MetaMask returns to the app via deep link after signing/connecting
  // Added by @novastera-oss/nitro-metamask config plugin
  func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    // Check if this is a MetaMask deep link (host="mmsdk")
    if let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
       components.host == "mmsdk" {
      // Handle MetaMask deep link return
      MetaMaskSDK.shared.handleUrl(url)
      return true
    }
    
    // Handle other deep links (e.g., React Native Linking)
    return false
  }`;
      
      // Insert before the last closing brace of the AppDelegate class
      // Try to find the closing brace of the class before @end or end of file
      const classEndPattern = /(\s+)\}(?=\s*(?:@end|$))/;
      const match = modResults.contents.match(classEndPattern);
      if (match) {
        const indent = match[1];
        const insertIndex = match.index;
        modResults.contents = 
          modResults.contents.slice(0, insertIndex) +
          deepLinkHandler.replace(/^  /gm, indent) +
          '\n' + indent + '}' +
          modResults.contents.slice(insertIndex + match[0].length);
      } else {
        // Fallback: append before @end
        modResults.contents = modResults.contents.replace(
          /^(@end|}$)/m,
          deepLinkHandler + '\n$1'
        );
      }
    } else if (modResults.language === 'objc') {
      // Handle Objective-C AppDelegate
      if (modResults.contents.includes('MetaMaskSDK')) {
        return config;
      }
      
      // Add import
      if (!modResults.contents.includes('#import <MetaMaskSDK/MetaMaskSDK.h>')) {
        const importRegex = /^#import\s+.*$/gm;
        const imports = modResults.contents.match(importRegex);
        if (imports && imports.length > 0) {
          const lastImport = imports[imports.length - 1];
          const lastImportIndex = modResults.contents.lastIndexOf(lastImport);
          modResults.contents = 
            modResults.contents.slice(0, lastImportIndex + lastImport.length) +
            '\n#import <MetaMaskSDK/MetaMaskSDK.h>' +
            modResults.contents.slice(lastImportIndex + lastImport.length);
        }
      }
      
      const objcHandler = `
- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
  // Check if this is a MetaMask deep link (host="mmsdk")
  NSURLComponents *components = [NSURLComponents componentsWithURL:url resolvingAgainstBaseURL:YES];
  if ([components.host isEqualToString:@"mmsdk"]) {
    // Handle MetaMask deep link return
    [[MetaMaskSDK sharedInstance] handleUrl:url];
    return YES;
  }
  
  // Handle other deep links
  return NO;
}`;
      
      modResults.contents = modResults.contents.replace(
        /^(@end|}$)/m,
        objcHandler + '\n$1'
      );
    }
    
    return config;
  });
};

module.exports = withMetamaskAppDelegate;
