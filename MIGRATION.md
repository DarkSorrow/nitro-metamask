# Migration Guide: From Simple Nitro Element to Proper Nitro Package

This document tracks the migration of the MetaMask connector implementation from a simple Nitro element (`save/`) to a properly structured Nitro package template.

## 🎯 Quick Start: Decisions Already Made

**All critical decisions have been made** - use these consistently:

- ✅ **Package Name**: `@novastera-oss/nitro-metamask` (already set, no changes needed)
- ✅ **Namespace**: `com.nitrometamask` (template's namespace, already set in `build.gradle`)
- ✅ **Naming Convention**: `NitroMetamask` / `HybridNitroMetamask` (template's naming, already set in `nitro.json`)
- ✅ **Export Name**: `NitroMetamask` (already set in `src/index.ts`)

**What's Left**: Migrate functional code from `save/` directory, update files with MetaMask implementation, add dependencies, and test.

## Overview

### The `save/` Directory: Simple Nitro Element

The `save/` directory contains a **simple Nitro element** - a functional MetaMask connector implementation that was created before the proper package structure was set up. It has:
- ✅ Complete Android implementation (Kotlin) - **functional code to migrate**
- ✅ Complete iOS implementation (Swift) - **functional code to migrate**
- ✅ TypeScript spec with `connect()` and `signMessage()` methods - **functional spec to migrate**
- ✅ Context holder for Android - **working pattern to migrate**
- ✅ React Native package registration - **working configuration to migrate**
- ✅ MetaMask SDK dependencies configured - **dependencies to add**

**However**, it lacks:
- ❌ Proper Nitro package structure and wiring
- ❌ Correct build system integration (`react-native-builder-bob`)
- ❌ Proper file organization following Nitro best practices
- ❌ Example app integration
- ❌ Proper TypeScript build output structure

### The Current Package: Proper Nitro Template

The current package is a **properly structured Nitro package template** created using best practices, with:
- ✅ Correct Nitro package structure (`src/specs/`, proper exports)
- ✅ Proper build system (`react-native-builder-bob` for multi-format builds)
- ✅ Correct file organization following Nitro conventions
- ✅ Example app properly configured and wired
- ✅ Proper TypeScript build configuration
- ✅ Correct `nitro.json` structure
- ✅ Proper workspace setup

**However**, it currently has:
- ⚠️ Only a placeholder `sum()` function (template example)
- ⚠️ Missing the actual MetaMask implementation
- ⚠️ Missing Android native files (need to create in proper structure)
- ⚠️ Missing iOS native files (need to update from template)
- ⚠️ Missing MetaMask-specific TypeScript spec

### Migration Goal

**Extract the functional MetaMask implementation from `save/` and integrate it into the properly structured Nitro package template**, following Nitro best practices and the template's conventions.

**Important**: The package name is **`@novastera-oss/nitro-metamask`** (already set in `package.json`). This is the final published package name and must remain consistent across all files.

This means:
1. **Keep the new package structure** (it's correct and follows best practices)
2. **Migrate the functional code** from `save/` (it works, just needs proper integration)
3. **Follow the template's patterns** (file locations, naming, exports)
4. **Ensure everything is properly wired** (autolinking, build system, example app)
5. **Maintain package name consistency** - All references to `@novastera-oss/nitro-metamask` are correct

## Migration Checklist

### ✅ Phase 1: TypeScript Spec Migration

**Goal**: Replace the template's `sum()` function with the functional MetaMask connector spec from `save/`, following the template's file structure.

- [ ] **Migrate TypeScript spec**
  - **Source**: `save/src/MetamaskConnector.nitro.ts` (simple element - functional code)
  - **Target**: `src/specs/nitro-metamask.nitro.ts` (proper template structure - **UPDATE THIS FILE**)
  - **Action**: Replace the template's `sum()` interface with the functional MetaMask interface
  - **Important**: Keep the file in `src/specs/` (template's structure) but update the content
  - **Key Changes**:
    - **Use template's naming**: Keep `NitroMetamask` interface name (matches template and `nitro.json`)
    - **Replace template method**: Change `sum(num1: number, num2: number): number` to:
      ```typescript
      connect(): Promise<ConnectResult>
      signMessage(message: string): Promise<string>
      ```
    - **Add ConnectResult interface** (before the main interface):
      ```typescript
      export interface ConnectResult {
        address: string
        chainId: number
      }
      ```
    - **Follow template pattern**: Keep the `HybridObject` type annotation pattern from template
    - **Final result should look like**:
      ```typescript
      import { type HybridObject } from 'react-native-nitro-modules'
      
      export interface ConnectResult {
        address: string
        chainId: number
      }
      
      export interface NitroMetamask extends HybridObject<{ ios: 'swift', android: 'kotlin' }> {
        connect(): Promise<ConnectResult>
        signMessage(message: string): Promise<string>
      }
      ```

- [ ] **Update index.ts** (OPTIONAL - may already be correct)
  - **Source**: `save/src/index.ts` (simple element - functional export)
  - **Target**: `src/index.ts` (proper template - **CHECK IF UPDATE NEEDED**)
  - **Current Status**: Template's `index.ts` already has the correct structure! It just needs the spec file to be updated first.
  - **Action**: 
    - **If using template naming** (`NitroMetamask`): The current `index.ts` is already correct! Just add the `ConnectResult` export after updating the spec.
    - **If using save naming** (`MetamaskConnector`): Update to match save's export pattern
  - **Important**: 
    - The HybridObject name (second parameter) **MUST match** the key in `nitro.json` autolinking
    - Template uses `"NitroMetamask"` which matches current `index.ts` ✅
    - Save uses `"MetamaskConnector"` - only change if you're using save's naming
  - **Key Changes** (only if NOT using template naming):
    ```typescript
    // TEMPLATE (current - KEEP THIS if using template naming):
    import { NitroModules } from 'react-native-nitro-modules'
    import type { NitroMetamask as NitroMetamaskSpec } from './specs/nitro-metamask.nitro'
    
    export const NitroMetamask =
      NitroModules.createHybridObject<NitroMetamaskSpec>('NitroMetamask')
    
    // ADD THIS after spec is updated:
    export type { ConnectResult, NitroMetamaskSpec as NitroMetamask }
    
    // ONLY if using save's naming (MetamaskConnector):
    // import type { MetamaskConnector } from './specs/nitro-metamask.nitro'
    // export const metamaskConnector = 
    //   NitroModules.createHybridObject<MetamaskConnector>('MetamaskConnector')
    ```

### ✅ Phase 2: Android Native Implementation

**Goal**: Create the Android native files in the proper package structure, migrating functional code from `save/`.

- [ ] **Determine Android namespace** ✅ **DECISION MADE: Use template's namespace**
  - **Template uses**: `com.nitrometamask` (already set in `android/build.gradle`)
  - **Save uses**: `com.margelo.nitro.nitrometamask` (standard Nitro pattern)
  - **Decision**: ✅ **Use template's namespace** (`com.nitrometamask`) - simpler, matches template, already configured
  - **Action**: No changes needed - template already uses `com.nitrometamask`. Create directory structure matching this namespace.

- [ ] **Create Android directory structure** ✅ **Namespace: `com.nitrometamask`**
  - **Action**: Create the directory structure: `android/src/main/java/com/nitrometamask/`
  - **Note**: The template doesn't have these files yet - we're creating them following Nitro best practices
  - **Directory to create**: `android/src/main/java/com/nitrometamask/`

- [ ] **Migrate MetamaskContextHolder.kt**
  - **Source**: `save/android/src/main/java/com/margelo/nitro/nitrometamask/MetamaskContextHolder.kt` (functional code)
  - **Target**: `android/src/main/java/com/nitrometamask/MetamaskContextHolder.kt` (new file in proper structure)
  - **Action**: Copy the functional implementation, **update package declaration** to `package com.nitrometamask`
  - **Status**: ✅ Complete functional implementation - copy and adapt namespace
  - **Key Features** (from save - proven to work):
    - Thread-safe context holder with `@Volatile`
    - Logging for initialization verification (`Log.d("NitroMetamask", "Context initialized")`)
    - Proper error messages
  - **Important**: Update `package` declaration to `package com.nitrometamask`

- [ ] **Migrate NitroMetamaskPackage.kt**
  - **Source**: `save/android/src/main/java/com/margelo/nitro/nitrometamask/NitroMetamaskPackage.kt` (functional code)
  - **Target**: `android/src/main/java/com/nitrometamask/NitroMetamaskPackage.kt` (new file in proper structure)
  - **Action**: Copy the functional implementation, **update package declaration** to `package com.nitrometamask`
  - **Status**: ✅ Complete functional implementation - copy and adapt namespace
  - **Key Features** (from save - proven to work):
    - Extends `TurboReactPackage` (correct for Nitro modules)
    - Initializes `MetamaskContextHolder` in `getModule()` (critical for Context access)
    - Calls `NitroMetamaskOnLoad.initializeNative()` in companion object (required for Nitro)
  - **Important**: 
    - Update `package` declaration to `package com.nitrometamask`
    - **Keep `NitroMetamaskOnLoad` import as `com.margelo.nitro.nitrometamask.NitroMetamaskOnLoad`** - this is auto-generated by Nitrogen and always uses the `nitro.json` androidNamespace, regardless of your build.gradle namespace
    - Update import in `react-native.config.js` to use `com.nitrometamask` (your package namespace, not the generated one)

- [ ] **Migrate HybridMetamaskConnector.kt**
  - **Source**: `save/android/src/main/java/com/margelo/nitro/nitrometamask/HybridMetamaskConnector.kt` (functional code)
  - **Target**: `android/src/main/java/com/nitrometamask/HybridNitroMetamask.kt` (new file - use template naming)
  - **Action**: Copy the functional implementation, **update package to `package com.nitrometamask` and rename class** from `HybridMetamaskConnector` to `HybridNitroMetamask`
  - **Status**: ✅ Complete functional implementation - copy and adapt to template
  - **Key Features** (from save - proven to work):
    - Uses `MetamaskContextHolder` for Context access (correct Nitro pattern)
    - Implements `connect()` with MetaMask Android SDK (fully functional)
    - Implements `signMessage()` with `personal_sign` (fully functional)
    - Proper error handling and type conversions
    - Uses `Promise.async` with coroutines (Nitro best practice)
  - **Important**: 
    - Update `package` declaration to `package com.nitrometamask`
    - **Class name must match `nitro.json` autolinking**: Template uses `"kotlin": "HybridNitroMetamask"`, so rename class from `HybridMetamaskConnector` to `HybridNitroMetamask`
    - **Spec class name**: The class extends `HybridNitroMetamaskSpec` (auto-generated by Nitrogen based on `nitro.json` autolinking key)
    - Update `MetamaskContextHolder` import to `import com.nitrometamask.MetamaskContextHolder`

- [ ] **Update Android build.gradle**
  - **Source**: `save/android/build.gradle` (has dependencies we need)
  - **Target**: `android/build.gradle` (template file - **UPDATE THIS**)
  - **Action**: Add the functional dependencies from save to the template's build.gradle
  - **Changes Needed**:
    - [ ] **Add Kotlin Coroutines dependency** (required for `Promise.async` with coroutines):
      ```gradle
      // Add to dependencies block
      implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
      ```
    - [ ] **Add MetaMask Android SDK dependency** (required for MetaMask functionality):
      ```gradle
      // Add to dependencies block
      implementation "io.metamask.androidsdk:metamask-android-sdk:0.6.6"
      ```
    - [ ] **Update namespace** (if changing from template's default):
      ```gradle
      android {
        namespace "com.margelo.nitro.nitrometamask" // or keep template's namespace
      }
      ```
    - [ ] **Keep template's Gradle version** (8.8.0) - it's newer and compatible
    - [ ] **Keep template's structure** - don't copy everything from save, just add what's needed

### ✅ Phase 3: iOS Native Implementation

**Goal**: Replace the template's placeholder Swift file with the functional MetaMask implementation from `save/`.

- [ ] **Migrate HybridMetamaskConnector.swift**
  - **Source**: `save/ios/HybridMetamaskConnector.swift` (functional code)
  - **Target**: `ios/HybridNitroMetamask.swift` (template file - **REPLACE THIS**)
  - **Action**: Replace template's `sum()` implementation with functional MetaMask code
  - **Important**: 
    - **Class name must match `nitro.json`**: Template uses `"swift": "HybridNitroMetamask"`, so rename class from `HybridMetamaskConnector` to `HybridNitroMetamask`
    - **Spec protocol name**: The class conforms to `HybridNitroMetamaskSpec` (auto-generated by Nitrogen based on `nitro.json` autolinking key)
    - **Keep class as `final`**: Save uses `final class`, which is good practice
  - **Status**: ✅ Complete functional implementation - copy and adapt class name
  - **Key Features** (from save - proven to work):
    - Uses `MetaMaskSDK.shared` (correct iOS SDK usage)
    - Implements `connect()` with async/await (Nitro best practice)
    - Implements `signMessage()` with `EthereumRequest` (correct API usage)
    - Proper error handling and type conversions
    - Uses `Promise.async` with Swift concurrency (Nitro best practice)
  - **Action Steps**:
    1. Copy functional code from save
    2. Rename class from `HybridMetamaskConnector` to `HybridNitroMetamask`
    3. Update protocol from `HybridMetamaskConnectorSpec` to `HybridNitroMetamaskSpec`
    4. Ensure imports are correct: `import NitroModules`, `import MetaMaskSDK`, `import Foundation` (in that order)

- [ ] **Update NitroMetamask.podspec**
  - **Source**: `save/NitroMetamask.podspec` (has MetaMask SDK dependency)
  - **Target**: `NitroMetamask.podspec` (template file - **UPDATE THIS**)
  - **Action**: Add MetaMask iOS SDK dependency to template's podspec
  - **Changes Needed**:
    - [ ] **Add MetaMask iOS SDK dependency** (required for MetaMask functionality):
      ```ruby
      # Add to dependencies block (before install_modules_dependencies)
      s.dependency 'metamask-ios-sdk', '~> 0.8.10'
      ```
    - [ ] **Keep template's structure** - don't copy everything from save, just add the dependency
    - [ ] **Keep template's source URL** - it's already correct for the new package

### ✅ Phase 4: Configuration Files

**Goal**: Update the template's configuration files to wire up the functional MetaMask implementation.

- [ ] **Verify nitro.json** ✅ **Already correct - no changes needed**
  - **Source**: Current `nitro.json` (template - already has correct autolinking)
  - **Target**: `nitro.json` (no changes needed)
  - **Status**: ✅ Template already has correct autolinking:
    ```json
    "autolinking": {
      "NitroMetamask": {
        "swift": "HybridNitroMetamask",
        "kotlin": "HybridNitroMetamask"
      }
    }
    ```
  - **Action**: **No changes needed** - template's autolinking already matches the naming we're using
  - **Critical**: The autolinking is already correct and matches:
    - Autolinking key: `"NitroMetamask"` ✅
    - Swift class name: `"HybridNitroMetamask"` ✅
    - Kotlin class name: `"HybridNitroMetamask"` ✅
    - TypeScript: `NitroMetamask` ✅
    - Export: `NitroMetamask` ✅

- [ ] **Create react-native.config.js**
  - **Source**: `save/react-native.config.js` (has working package registration)
  - **Target**: `react-native.config.js` (create at root - template may not have this)
  - **Action**: Create file with Android package registration for autolinking
  - **Purpose**: Tells React Native autolinking to register `NitroMetamaskPackage` automatically
  - **Key Content** (use template namespace `com.nitrometamask`):
    ```javascript
    // https://github.com/react-native-community/cli/blob/main/docs/dependencies.md
    module.exports = {
      dependency: {
        platforms: {
          android: {
            packageImportPath: 'import com.nitrometamask.NitroMetamaskPackage;',
            packageInstance: 'new NitroMetamaskPackage()',
          },
        },
      },
    }
    ```
  - **Important**: Use `com.nitrometamask` namespace (template's namespace, not save's `com.margelo.nitro.nitrometamask`)

- [ ] **Verify package.json**
  - **Source**: Current `package.json` (template - already correct)
  - **Target**: `package.json` (no changes needed if using `react-native.config.js`)
  - **Note**: 
    - Template uses `react-native-builder-bob` for proper multi-format builds (commonjs, module, typescript)
    - This is **correct** and should be kept
    - If `react-native.config.js` exists, it takes precedence over `package.json` react-native config
    - No changes needed to `package.json` if we create `react-native.config.js`

### ✅ Phase 5: Verification & Testing

- [ ] **Run Nitrogen codegen**
  ```bash
  npm run codegen
  # or
  npx nitrogen
  ```
  - Verify that all native bindings are generated correctly
  - Check `nitrogen/generated/` directories

- [ ] **Build Android**
  ```bash
  cd example/android
  ./gradlew clean
  ./gradlew assembleDebug
  ```
  - Verify no compilation errors
  - Check that `MetamaskContextHolder` is initialized (look for log)

- [ ] **Build iOS**
  ```bash
  cd example/ios
  pod install
  ```
  - Verify CocoaPods installs MetaMask SDK
  - Build in Xcode to verify no compilation errors

- [ ] **Test in Example App**
  - Update `example/App.tsx` to use the migrated connector (currently uses template's `sum()` method - needs update)
  - Replace `NitroMetamask.sum(1, 2)` with MetaMask connector usage:
    ```typescript
    const connector = NitroMetamask.create()
    // Test connect() and signMessage()
    ```
  - Test `connect()` functionality
  - Test `signMessage()` functionality
  - Verify logs show "Context initialized" on Android

- [ ] **Verify Autolinking**
  - Ensure `NitroMetamaskPackage` is automatically registered
  - Check that no manual registration is needed in `MainApplication.kt`

## Critical Decisions Needed

### 1. Naming Convention ✅ **DECISION MADE: Use template's naming**

**The following must all match exactly:**
- `nitro.json` autolinking key
- `createHybridObject()` name parameter  
- Native class names (Swift and Kotlin)
- TypeScript interface name

**Decision**: ✅ **Use template's naming** (already configured, consistent with template)

**Final naming (already set in template):**
- `nitro.json`: `"NitroMetamask"` ✅
- `createHybridObject('NitroMetamask')` ✅
- Swift: `HybridNitroMetamask` ✅
- Kotlin: `HybridNitroMetamask` ✅
- TypeScript: `NitroMetamask` ✅
- Export: `NitroMetamask` ✅

**Action**: No changes needed - template already uses this naming consistently.

### 2. Package Namespace ✅ **DECISION MADE: Use template's namespace**

- **Template**: `com.nitrometamask` ✅ **USE THIS** (already configured, simpler, matches template)
- **Save**: `com.margelo.nitro.nitrometamask` (more standard for Nitro modules, but not needed)

**Decision**: ✅ **Use template's namespace** (`com.nitrometamask`) - already set in `android/build.gradle`, minimizes changes, stays consistent with template

**Impact**: This affects:
- Directory structure: `android/src/main/java/com/nitrometamask/` ✅
- `android/build.gradle` namespace declaration: Already set to `com.nitrometamask` ✅
- `react-native.config.js` packageImportPath: Use `com.nitrometamask` ✅
- All Kotlin `package` declarations: Use `package com.nitrometamask` ✅

**Important Note**: 
- `NitroMetamaskOnLoad` is auto-generated by Nitrogen and **always** uses `com.margelo.nitro.nitrometamask` package (based on `nitro.json` androidNamespace)
- You must import it as: `import com.margelo.nitro.nitrometamask.NitroMetamaskOnLoad` regardless of your package namespace
- This is **correct** and expected - the generated file package doesn't need to match your source file packages

### 3. Export Name ✅ **DECISION MADE: Use template's export**

- **Template**: `NitroMetamask` ✅ **USE THIS** (already set, matches interface name)
- **Save**: `metamaskConnector` (camelCase, more descriptive, but not used)

**Decision**: ✅ **Use template's export** (`NitroMetamask`) - already set in `src/index.ts`, consistent with template

**Note**: This is just the export name - the important part is that the HybridObject name matches `nitro.json`, which it does (`NitroMetamask`).

### 4. Package Name (CRITICAL - Already Set)

- **Package Name**: `@novastera-oss/nitro-metamask` ✅
- **Status**: Already correctly set in `package.json`
- **Important**: This is the **final published package name** and must remain consistent

**Files that reference the package name** (all should use `@novastera-oss/nitro-metamask`):
- ✅ `package.json` - `"name": "@novastera-oss/nitro-metamask"`
- ✅ `example/package.json` - Example app name (separate, can differ)
- ✅ `example/App.tsx` - Import statement: `import { NitroMetamask } from '@novastera-oss/nitro-metamask'`
- ✅ `example/tsconfig.json` - Path alias: `"@novastera-oss/nitro-metamask": ["../src"]`
- ⚠️ `NitroMetamask.podspec` - Source URL (verify GitHub repo URL is correct)
- ✅ `README.md` - Documentation references

**No changes needed** - package name is already correct throughout the codebase.

## File Mapping

| Old Package (save/) | New Package | Status | Notes |
|---------------------|-------------|--------|-------|
| `src/MetamaskConnector.nitro.ts` | `src/specs/nitro-metamask.nitro.ts` | ⚠️ Needs migration | Replace template's `sum()` with MetaMask methods |
| `src/index.ts` | `src/index.ts` | ⚠️ Minor update | Add `ConnectResult` export (structure already correct) |
| `android/.../MetamaskContextHolder.kt` | `android/.../MetamaskContextHolder.kt` | ❌ Missing | Create file - copy from save |
| `android/.../NitroMetamaskPackage.kt` | `android/.../NitroMetamaskPackage.kt` | ❌ Missing | Create file - copy from save |
| `android/.../HybridMetamaskConnector.kt` | `android/.../HybridNitroMetamask.kt` | ❌ Missing | Create file - copy from save, rename class |
| `ios/HybridMetamaskConnector.swift` | `ios/HybridNitroMetamask.swift` | ⚠️ Needs migration | Replace template's `sum()` with MetaMask code, rename class |
| `android/build.gradle` | `android/build.gradle` | ⚠️ Needs update | Add MetaMask SDK & Coroutines dependencies |
| `NitroMetamask.podspec` | `NitroMetamask.podspec` | ⚠️ Needs update | Add MetaMask iOS SDK dependency |
| `nitro.json` | `nitro.json` | ✅ Already correct | No changes needed - autolinking already matches template naming |
| `react-native.config.js` | `react-native.config.js` | ❌ Missing | Create file - copy from save, update namespace |

### Files to Keep from Template (Do NOT Migrate)

| File | Reason |
|------|--------|
| `android/CMakeLists.txt` | Template has `-DRN_SERIALIZABLE_STATE=1` flag (better) |
| `android/gradle.properties` | Template has correct SDK versions |
| `android/fix-prefab.gradle` | Both identical, keep template version |
| `android/src/main/cpp/cpp-adapter.cpp` | Auto-generated by Nitrogen, no manual changes needed |
| `ios/Bridge.h` | Both identical, keep template version |
| `post-script.js` | Template has this (required for codegen), save doesn't |
| `package.json` scripts | Template uses `codegen` (includes post-script), better |
| `example/` directory | Template's example is properly configured, save's wasn't working |

### Files to Ignore from Save

| File/Directory | Reason |
|----------------|--------|
| `save/example/` | Wasn't working, template's example is correct |
| `save/lib/` | Generated files, will be regenerated |
| `save/node_modules/` | Dependencies, not needed |
| `save/package-lock.json` | Lock file, not needed |
| `save/tsconfig.tsbuildinfo` | Build cache, not needed |
| `save/babel.config.js` | Template doesn't need this at root |
| `save/CODE_OF_CONDUCT.md` | Documentation, not needed for migration |
| `save/src/specs/Example.nitro.ts` | Just a TODO comment, not needed |

## Dependencies to Add

### Android (`android/build.gradle`)
```gradle
// Kotlin Coroutines
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"

// MetaMask Android SDK
implementation "io.metamask.androidsdk:metamask-android-sdk:0.6.6"
```

### iOS (`NitroMetamask.podspec`)
```ruby
s.dependency 'metamask-ios-sdk', '~> 0.8.10'
```

## Migration Steps (Recommended Order)

1. **Start with TypeScript spec** - This defines the contract
   - Update `src/specs/nitro-metamask.nitro.ts` with MetaMask methods
   - Update `src/index.ts` exports
2. **Migrate Android files** - Create directory structure and copy files
   - Create namespace directory structure
   - Copy `MetamaskContextHolder.kt` (update package declaration)
   - Copy `NitroMetamaskPackage.kt` (update package declaration)
   - Copy `HybridMetamaskConnector.kt` → rename to `HybridNitroMetamask.kt` (update package & class name)
3. **Update Android build.gradle** - Add dependencies
   - Add Kotlin Coroutines
   - Add MetaMask Android SDK
   - Update namespace if needed
4. **Migrate iOS file** - Copy and rename Swift file
   - Replace `ios/HybridNitroMetamask.swift` content with MetaMask code
   - Update class name to match `nitro.json`
5. **Update iOS podspec** - Add MetaMask SDK dependency
   - Add `metamask-ios-sdk` dependency
6. **Verify nitro.json** - Already correct, no changes needed
   - Autolinking already matches template naming (`NitroMetamask` → `HybridNitroMetamask`)
7. **Create react-native.config.js** - Add package registration
   - Copy from save, update namespace in packageImportPath
8. **Run codegen** - Generate native bindings
   - Use template's `npm run codegen` (includes post-script)
   - Verify `cpp-adapter.cpp` is auto-generated correctly
9. **Test build** - Verify everything compiles
   - Android: `cd example/android && ./gradlew assembleDebug`
   - iOS: `cd example/ios && pod install && build in Xcode`
10. **Update example app** - Test the migrated implementation
    - Update `example/App.tsx` to use MetaMask connector (import already correct: `import { NitroMetamask } from '@novastera-oss/nitro-metamask'`)
    - Test `connect()` and `signMessage()`
    - Verify the example app can create and use the connector
11. **Delete save/ directory** - Once everything is verified

## Post-Migration Checklist

- [ ] All files migrated from `save/` to current package
- [ ] All dependencies added to `build.gradle` and `podspec`
- [ ] `nitro.json` autolinking matches actual class names
- [ ] `react-native.config.js` properly configured
- [ ] Codegen runs successfully
- [ ] Android builds without errors
- [ ] iOS builds without errors
- [ ] Example app works with migrated code
- [ ] Logs show "Context initialized" on Android
- [ ] `connect()` works in example app
- [ ] `signMessage()` works in example app
- [ ] All tests pass (if any)
- [ ] Documentation updated
- [ ] `save/` directory deleted

## Important Notes

### About the Migration

- **Save directory contains functional code**: The `save/` directory has a working MetaMask implementation that was created as a simple Nitro element. It works, but lacks proper package structure.

- **Current package has proper structure**: The current package is a properly structured Nitro template with correct wiring, build system, and file organization following Nitro best practices.

- **Migration strategy**: We're extracting the **functional code** from `save/` and integrating it into the **proper structure** of the template, following the template's conventions.

### Files That Are Auto-Generated (No Manual Changes)

- **`android/src/main/cpp/cpp-adapter.cpp`**: Auto-generated by Nitrogen based on `nitro.json` namespace. The `initialize()` call namespace will be automatically set correctly. **Do not manually edit this file.**

- **`nitrogen/generated/` directory**: All files here are auto-generated by Nitrogen. They will be regenerated when you run `npm run codegen`. **Do not manually edit these files.**

- **`NitroMetamaskOnLoad.kt`**: Auto-generated in `nitrogen/generated/android/kotlin/com/margelo/nitro/nitrometamask/NitroMetamaskOnLoad.kt` based on `nitro.json` `androidNamespace`. 
  - **Important**: Even if your package uses `com.nitrometamask` namespace, you must import `NitroMetamaskOnLoad` from `com.margelo.nitro.nitrometamask` because that's where Nitrogen generates it.
  - **This is correct**: `import com.margelo.nitro.nitrometamask.NitroMetamaskOnLoad`
  - The `post-script.js` fixes the C++ namespace, but the Kotlin package stays as generated.

### Template Files to Keep (Do Not Replace)

- **`android/CMakeLists.txt`**: Template version has `-DRN_SERIALIZABLE_STATE=1` flag for Nitro Views support. Keep template version.

- **`android/gradle.properties`**: Template has correct SDK versions. Keep template version.

- **`android/fix-prefab.gradle`**: Both versions are identical, but keep template version for consistency.

- **`post-script.js`**: Template has this file (save doesn't). It's required for the `codegen` script to work correctly. **Keep template version.**

- **`package.json` scripts**: Template uses `codegen` script which runs `nitrogen && npm run build && node post-script.js`. This is the correct approach. **Keep template version.**

- **`example/` directory**: Template's example app is properly configured and wired. Save's example wasn't working. **Use template's example app.**

### Critical Consistency Requirements

**The Naming Chain (MUST MATCH):**
```
nitro.json autolinking key
    ↓
createHybridObject('Name') parameter
    ↓
Native class names (Swift & Kotlin)
    ↓
TypeScript interface name
```

**Example if using template naming:**
- `nitro.json`: `"NitroMetamask"` ✅
- `createHybridObject('NitroMetamask')` ✅
- Swift class: `HybridNitroMetamask` ✅
- Kotlin class: `HybridNitroMetamask` ✅
- TypeScript: `NitroMetamask` ✅

### What to Keep from Template

- ✅ File structure (`src/specs/`, proper exports)
- ✅ Build system (`react-native-builder-bob`)
- ✅ Workspace setup
- ✅ Example app configuration
- ✅ TypeScript build configuration

### What to Migrate from Save

- ✅ Functional MetaMask implementation (Android & iOS)
- ✅ Context holder pattern (proven to work)
- ✅ Package registration configuration
- ✅ MetaMask SDK dependencies
- ✅ Working TypeScript spec

### Migration Approach

1. **Follow template's structure** - Don't change file locations or organization
2. **Migrate functional code** - Copy working code from save
3. **Adapt to template conventions** - Update namespaces, class names, imports
4. **Maintain consistency** - Ensure all naming matches across files
5. **Test incrementally** - Verify after each phase

## Current Status: What's Missing or Has Problems

Based on the current package state, here's what needs to be fixed:

### ❌ Missing Files

1. **`android/src/main/java/com/nitrometamask/MetamaskContextHolder.kt`**
   - **Status**: Completely missing
   - **Action**: Copy from `save/android/src/main/java/com/margelo/nitro/nitrometamask/MetamaskContextHolder.kt`
   - **Changes needed**: Update package declaration to `package com.nitrometamask`

2. **`react-native.config.js`** (at root)
   - **Status**: Missing at root (only exists in `example/` and `save/`)
   - **Action**: Create at root, copy from `save/react-native.config.js`
   - **Changes needed**: Update `packageImportPath` to use `com.nitrometamask` namespace

### ⚠️ Files with Problems (Need Updates)

1. **`android/src/main/java/com/nitrometamask/NitroMetamaskPackage.kt`**
   - **Problems**:
     - ✅ Package declaration is correct (`com.nitrometamask`)
     - ⚠️ **Import clarification needed**: `NitroMetamaskOnLoad` is auto-generated by Nitrogen in `nitrogen/generated/android/kotlin/com/margelo/nitro/nitrometamask/NitroMetamaskOnLoad.kt`
     - ⚠️ **Namespace mismatch**: The generated file uses `com.margelo.nitro.nitrometamask` (from `nitro.json` androidNamespace), but your files use `com.nitrometamask` (from `build.gradle` namespace)
     - ⚠️ **Solution**: Import from generated location: `import com.margelo.nitro.nitrometamask.NitroMetamaskOnLoad` (this is correct, even though package differs)
     - ❌ Missing: `MetamaskContextHolder.initialize(reactContext)` call in `getModule()` (CRITICAL - this is why Context isn't initialized)
     - ❌ Wrong syntax: Uses `public class` (Java) instead of `class` (Kotlin)
     - ❌ Has `getReactModuleInfoProvider()` which isn't needed for TurboReactPackage
   - **Action**: Replace with correct version from save, update package to `com.nitrometamask`, keep the `NitroMetamaskOnLoad` import as-is (it's correct)

2. **`android/src/main/java/com/nitrometamask/HybridNitroMetamask.kt`**
   - **Problems**:
     - ✅ Package declaration is correct (`com.nitrometamask`)
     - ❌ Still has template's `sum()` method instead of MetaMask `connect()` and `signMessage()`
     - ❌ Wrong spec: Extends `HybridNitroMetamaskSpec` (template) instead of MetaMask spec
   - **Action**: Replace with MetaMask code from save, rename class to `HybridNitroMetamask`, update package

3. **`src/specs/nitro-metamask.nitro.ts`**
   - **Problems**:
     - ❌ Still has template's `sum()` method
     - ❌ Missing `ConnectResult` interface
     - ❌ Missing `connect()` and `signMessage()` methods
   - **Action**: Replace with MetaMask spec from save

4. **`ios/HybridNitroMetamask.swift`**
   - **Problems**:
     - ❌ Still has template's `sum()` method
     - ❌ Missing MetaMask SDK imports (`NitroModules`, `MetaMaskSDK`)
     - ❌ Missing `connect()` and `signMessage()` implementations
   - **Action**: Replace with MetaMask code from save, keep class name `HybridNitroMetamask`

5. **`android/build.gradle`**
   - **Problems**:
     - ❌ Missing Kotlin Coroutines dependency
     - ❌ Missing MetaMask Android SDK dependency
   - **Action**: Add dependencies from save

6. **`NitroMetamask.podspec`**
   - **Problems**:
     - ❌ Missing MetaMask iOS SDK dependency
     - ⚠️ Source URL format: `https://github.com/darksorrow/@novastera-oss/nitro-metamask.git` - **Verify this is the correct GitHub repository URL** (the `@novastera-oss` in the path might need to be `novastera-oss` without the `@`, depending on your actual repo structure)
   - **Action**: 
     - Add `s.dependency 'metamask-ios-sdk', '~> 0.8.10'`
     - Verify the source URL matches your actual GitHub repository (package name `@novastera-oss/nitro-metamask` is correct, but GitHub URLs typically don't include `@` in the path)

### ✅ Files That Are Correct (No Changes Needed)

1. **`src/index.ts`** - Already has correct structure, just needs to stay as-is (will work once spec is updated)
2. **`nitro.json`** - Autolinking is already correct for template naming (`NitroMetamask` → `HybridNitroMetamask`) ✅
3. **`android/CMakeLists.txt`** - Template version is correct (has extra flag)
4. **`android/gradle.properties`** - Template version is correct
5. **`post-script.js`** - Template version is correct
6. **`package.json`** - Template version is correct (has `codegen` script)

## Common Issues & Clarifications

### Namespace Confusion

**Q: Why does `NitroMetamaskOnLoad` import use `com.margelo.nitro.nitrometamask` when my package uses `com.nitrometamask`?**

**A**: `NitroMetamaskOnLoad` is auto-generated by Nitrogen based on `nitro.json` `androidNamespace` setting. It's always generated in `com.margelo.nitro.nitrometamask` package. Your source files can use any namespace (like `com.nitrometamask`), but you must import the generated class from its generated location. This is **correct and expected**.

### Spec Class Names

**Q: What should my Kotlin/Swift class extend/conform to?**

**A**: The spec class name is auto-generated by Nitrogen based on the `nitro.json` autolinking key:
- If autolinking key is `"NitroMetamask"` → Kotlin extends `HybridNitroMetamaskSpec`, Swift conforms to `HybridNitroMetamaskSpec`
- If autolinking key is `"MetamaskConnector"` → Kotlin extends `HybridMetamaskConnectorSpec`, Swift conforms to `HybridMetamaskConnectorSpec`

The pattern is: `Hybrid{AutolinkingKey}Spec`

### Import Paths

**Q: What imports do I need in Swift?**

**A**: Based on save's working code:
- `import NitroModules` (correct - this is the Nitro framework)
- `import MetaMaskSDK` (MetaMask iOS SDK)
- `import Foundation` (standard Swift foundation)

**Note**: The order matters - `NitroModules` should be first, then `MetaMaskSDK`, then `Foundation`.

## Questions?

If you encounter issues during migration:
1. Check that all file paths match the namespace (except auto-generated files)
2. Verify that `nitro.json` autolinking matches the actual class names
3. Ensure `react-native.config.js` has the correct package registration (your package namespace, not generated)
4. Run `npm run codegen` (not just `npx nitrogen`) to include post-script
5. Clean and rebuild after dependency changes
6. Verify `MetamaskContextHolder.initialize()` is called in `NitroMetamaskPackage.getModule()` (CRITICAL)
7. Remember: `NitroMetamaskOnLoad` import uses generated package (`com.margelo.nitro.nitrometamask`), not your source package

## Summary: Critical Issues to Fix

Based on the review, here are the **most critical** issues that will prevent the package from working:

### 🔴 Critical (Will Cause Runtime Errors)

1. **Missing `MetamaskContextHolder.initialize()` call** in `NitroMetamaskPackage.kt`
   - **Impact**: Context will never be initialized → `MetamaskContextHolder.get()` will throw
   - **Fix**: Add `MetamaskContextHolder.initialize(reactContext)` in `getModule()` method

2. **Missing `MetamaskContextHolder.kt` file**
   - **Impact**: Cannot compile, Context holder doesn't exist
   - **Fix**: Copy from save, update package to `com.nitrometamask`

3. **Wrong class implementations** - Still have template's `sum()` method
   - **Impact**: MetaMask methods don't exist
   - **Fix**: Replace `HybridNitroMetamask.kt` and `HybridNitroMetamask.swift` with MetaMask code

### 🟡 Important (Will Cause Build Errors)

4. **Missing dependencies** in `build.gradle` and `podspec`
   - **Impact**: Cannot compile - missing MetaMask SDK and Coroutines
   - **Fix**: Add dependencies as documented

5. **Missing `react-native.config.js`** at root
   - **Impact**: Package won't be autolinked → Context never initialized
   - **Fix**: Create file with correct namespace

### 🟢 Minor (Code Quality)

6. **Wrong syntax** in `NitroMetamaskPackage.kt` (Java syntax instead of Kotlin)
   - **Impact**: Code quality issue, but may still compile
   - **Fix**: Use `class` instead of `public class`

7. **Unnecessary method** `getReactModuleInfoProvider()` in `NitroMetamaskPackage.kt`
   - **Impact**: Code quality, not needed for TurboReactPackage
   - **Fix**: Remove it

## Quick Reference: What to Do

### Decisions Already Made ✅
- **Namespace**: `com.nitrometamask` (template's namespace, already set)
- **Naming**: `NitroMetamask` / `HybridNitroMetamask` (template's naming, already set)
- **Package Name**: `@novastera-oss/nitro-metamask` (already correct)

### Migration Steps

1. **Copy 3 files from save** → Update package declarations to `com.nitrometamask`:
   - `MetamaskContextHolder.kt` → `android/src/main/java/com/nitrometamask/MetamaskContextHolder.kt`
   - `NitroMetamaskPackage.kt` → `android/src/main/java/com/nitrometamask/NitroMetamaskPackage.kt` (fix: add `MetamaskContextHolder.initialize()`, remove Java syntax)
   - `HybridMetamaskConnector.kt` → `android/src/main/java/com/nitrometamask/HybridNitroMetamask.kt` (rename class to `HybridNitroMetamask`)

2. **Update 2 files** with MetaMask code:
   - `src/specs/nitro-metamask.nitro.ts` → Replace `sum()` with MetaMask methods (`connect()`, `signMessage()`)
   - `ios/HybridNitroMetamask.swift` → Replace `sum()` with MetaMask code, rename class to `HybridNitroMetamask`

3. **Add dependencies**:
   - `android/build.gradle` → Add Coroutines + MetaMask Android SDK
   - `NitroMetamask.podspec` → Add MetaMask iOS SDK (and verify source URL if needed)

4. **Create 1 file**:
   - `react-native.config.js` → Copy from save, use `com.nitrometamask` namespace

5. **Update 2 files** (minor):
   - `src/index.ts` → Add `ConnectResult` export (structure already correct)
   - `example/App.tsx` → Replace `sum()` usage with MetaMask connector usage

6. **Verify** (already correct):
   - ✅ Package name `@novastera-oss/nitro-metamask` is set correctly
   - ✅ `nitro.json` autolinking already matches template naming
   - ⚠️ Verify `NitroMetamask.podspec` source URL matches your GitHub repository

7. **Run codegen**: `npm run codegen`

8. **Test**: Build and verify Context initialization log appears