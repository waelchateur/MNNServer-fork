package io.kindbrave.mnnserver.annotation

import com.elvishew.xlog.XLog
import com.flyjingfish.android_aop_annotation.ProceedJoinPoint
import com.flyjingfish.android_aop_annotation.base.BasePointCut

class LogAfterInterceptCut : BasePointCut<LogAfter> {

    override fun invoke(
        joinPoint: ProceedJoinPoint,
        anno: LogAfter
    ): Any? {
        val tag = joinPoint.targetClass.simpleName
        val methodName = joinPoint.targetMethod.name
        val args = joinPoint.args?.joinToString(", ") { it.toString() }
        try {
            val result = joinPoint.proceed()
            XLog.tag(tag).d("$methodName:${anno.message} after args:$args result:$result")
            return result
        } catch (e: Exception) {
            XLog.tag(tag).e("$methodName:${anno.message} after args:$args exception: ${e.message}")
            throw e
        }
    }
}