// JmolMultiTouchDLL.cpp
//

#include "stdafx.h"
#include <stdio.h>
#include "JmolMultiTouchJNI.h"

BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
                                         )
{
    return TRUE;
}

JNIEXPORT void JNICALL
Java_org_jmol_multitouch_jni_InstanceMethodCall_nativeMethod(JNIEnv *env, jobject obj) {
    printf("In C\n");
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, "callback", "()V");
    if (mid == NULL) {
        return; /* method not found */
    }
    (*env)->CallVoidMethod(env, obj, mid);
}

