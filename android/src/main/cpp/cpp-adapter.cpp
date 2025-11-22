#include <jni.h>
#include "NitroMetamaskOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::nitrometamask::initialize(vm);
}
