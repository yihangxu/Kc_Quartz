package com.kcschedule.core.utils;

import org.springframework.web.context.request.RequestContextHolder;

import java.lang.reflect.Method;

public class WebCompat {

    public static Object getSessionAttribute(String name) {
        try {
            Object attrs = RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            Object session = getSession(attrs, true);
            if (session == null) return null;
            Method getAttribute = session.getClass().getMethod("getAttribute", String.class);
            return getAttribute.invoke(session, name);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setSessionAttribute(String name, Object value) {
        try {
            Object attrs = RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            Object session = getSession(attrs, true);
            if (session == null) return;
            Method setAttribute = session.getClass().getMethod("setAttribute", String.class, Object.class);
            setAttribute.invoke(session, name, value);
        } catch (Exception ignored) {
        }
    }

    private static Object getSession(Object attrs, boolean create) {
        try {
            Method getRequest = attrs.getClass().getMethod("getRequest");
            Object request = getRequest.invoke(attrs);
            if (request == null) return null;
            return request.getClass().getMethod("getSession", boolean.class).invoke(request, create);
        } catch (Exception e) {
            return null;
        }
    }
}
