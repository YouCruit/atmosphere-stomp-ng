package com.youcruit.atmosphere.stomp.util

import java.lang.reflect.Method


fun <T : Annotation> Method.recursiveAnnotationFinder(
    vararg annotationClasses: Class<out T>
): List<T> {
    val foundAnnotations = ArrayList<T>()
    recursiveAnnotationFinder(annotationClasses, declaringClass, name, parameterTypes, foundAnnotations)
    return foundAnnotations
}

private fun <T : Annotation> recursiveAnnotationFinder(
    annotationClasses: Array<out Class<out T>>,
    resourceClass: Class<*>,
    methodName: String,
    parameterTypes: Array<Class<*>>,
    foundAnnotations: MutableList<T>
) {
    visitMethods(
        resourceClass,
        methodName,
        parameterTypes
    ) { method ->
        for (annotationClass in annotationClasses) {
            method
                .getAnnotation(annotationClass)
                ?.let { foundAnnotations.add(it) }
        }

        for (annotationClass in annotationClasses) {
            resourceClass
                .getAnnotation(annotationClass)
                ?.let { foundAnnotations.add(it) }
        }
    }
}


private fun Class<*>.getMethodOrNull(
    methodName: String,
    parameterTypes: Array<Class<*>>
): Method? {
    return try {
        this.getDeclaredMethod(methodName, *parameterTypes)
    } catch (x: NoSuchMethodException) {
        null
    }
}

private fun visitMethods(clazz: Class<*>, methodName: String, parameters: Array<Class<*>>, walker: (Method) -> Unit) {
    val method = clazz.getMethodOrNull(methodName, parameters)
        ?: return

    for (aClass in clazz.interfaces) {
        visitMethods(aClass, methodName, parameters, walker)
    }

    val superclass = clazz.superclass
    if (superclass != null && superclass.name != "java.lang.Object") {
        visitMethods(clazz, methodName, parameters, walker)
    }

    walker(method)
}