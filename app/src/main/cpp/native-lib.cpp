#include <jni.h>
#include <string>
#include <fcntl.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_omniremotes_remoteverify_service_CoreService_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_omniremotes_remoteverify_service_CoreService_initHidNative(JNIEnv *env, jobject instance) {
}