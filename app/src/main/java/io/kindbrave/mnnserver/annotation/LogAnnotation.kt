package io.kindbrave.mnnserver.annotation

import com.flyjingfish.android_aop_annotation.anno.AndroidAopPointCut

@AndroidAopPointCut(LogBeforeInterceptCut::class)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogBefore(val message: String)

@AndroidAopPointCut(LogAfterInterceptCut::class)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogAfter(val message: String)