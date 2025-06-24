package io.kindbrave.mnn.server.annotation

import com.elvishew.xlog.XLog
import com.flyjingfish.android_aop_annotation.ProceedJoinPoint
import com.flyjingfish.android_aop_annotation.base.BasePointCut

class LogBeforeInterceptCut : BasePointCut<LogBefore> {

    override fun invoke(
        joinPoint: ProceedJoinPoint,
        anno: LogBefore
    ): Any? {
        val tag = joinPoint.targetClass.simpleName
        val methodName = joinPoint.targetMethod.name
        val args = joinPoint.args?.joinToString(", ") { it.toString() }
        XLog.tag(tag).d("$methodName:${anno.message} before args=$args")
        return joinPoint.proceed()
    }
}