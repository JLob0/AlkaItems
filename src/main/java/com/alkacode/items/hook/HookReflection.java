package com.alkacode.items.hook;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chamadas reflexivas compartilhadas pelos hooks deste pacote (ItemsAdder,
 * AdvancedEnchantments) - nenhum dos dois e compileOnly aqui, mesmo padrao ja usado
 * em com.alkacode.vips.hook.HookReflection. Toda falha (classe/metodo ausente, versao
 * incompativel) cai no catch e vira log FINE - nunca propaga pro resto do plugin.
 */
final class HookReflection {

    private HookReflection() {
    }

    static Object invokeStatic(Logger logger, String hookName, String className, String methodName,
                                Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName, paramTypes);
            return method.invoke(null, args);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook " + hookName + " falhou (" + className + "#" + methodName + "): " + t, t);
            return null;
        }
    }

    static Object invokeInstance(Logger logger, String hookName, Object target, String methodName,
                                  Class<?>[] paramTypes, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            return method.invoke(target, args);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook " + hookName + " falhou (" + methodName + "): " + t, t);
            return null;
        }
    }
}
