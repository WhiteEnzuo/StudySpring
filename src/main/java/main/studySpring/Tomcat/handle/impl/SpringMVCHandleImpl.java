package main.studySpring.Tomcat.handle.impl;

import main.studySpring.Spring.annotation.controller.Param;
import main.studySpring.Spring.annotation.controller.RequestBody;
import main.studySpring.Spring.inteceptor.WebHttpInterceptor;
import main.studySpring.Spring.inteceptor.context.HttpInterceptorContext;
import main.studySpring.Tomcat.agreement.Request;
import main.studySpring.Tomcat.agreement.Response;
import main.studySpring.Tomcat.enmus.StatusCode;
import main.studySpring.Tomcat.handle.HttpHandle;
import main.studySpring.Spring.annotation.bean.Bean;
import main.studySpring.global.Interceptor;
import main.studySpring.global.UrlMap;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * @Classname HttpHandleImpl
 * @Description
 * @Version 1.0.0
 * @Date 2023/4/21 19:27
 * @Created by Enzuo
 */
@Bean
@Slf4j
public class SpringMVCHandleImpl implements HttpHandle {
    @Override

    public void handle(Request request, Response response) {

        List<String> list = new ArrayList<>();
        getUrl(list, request.getUri());
        if (list.size() == 0) {
            response.setHeaderKey("Content-Type", "application/json;charset=UTF-8");
            response.sentResponse("note found", StatusCode.NOTFOUND);
            return;
        }
        boolean methodRequest = UrlMap.controllerContext.get(request.getMethod().toLowerCase(Locale.ROOT)).containsKey(list.get(0));
        boolean allRequest = UrlMap.controllerContext.get("all").containsKey(list.get(0));

        HttpInterceptorContext httpInterceptorContext = Interceptor.httpInterceptorContext;
        if (httpInterceptorContext != null) {
            Set<WebHttpInterceptor> httpInterceptors = httpInterceptorContext.getHttpInterceptors();
            if (httpInterceptors != null) {
                for (WebHttpInterceptor httpInterceptor : httpInterceptors) {
                    boolean handle = httpInterceptor.handle(request, response);
                    if (!handle) {
                        if (response.isHandle()) {

                            return;
                        }
                        response.setHeaderKey("Content-Type", "application/json;charset=UTF-8");
                        response.sentResponse("Forbidden", StatusCode.Forbidden);
                        return;
                    } else {
                        response.setHandle(false);
                    }
                }
            }
        }
        if (!methodRequest && !allRequest) {
            response.setHeaderKey("Content-Type", "application/json;charset=UTF-8");
            response.sentResponse("note found", StatusCode.NOTFOUND);
            return;
        }

        Map<String, Map<String, List<Object>>> methodMap = UrlMap.controllerContext.get(request.getMethod().toLowerCase(Locale.ROOT));
        List<Object> objectList = returnMethod(methodMap.get(list.get(0)), list);
        Method method = (Method) objectList.get(1);

        Parameter[] params = returnMethodParams(method);
        Object[] objects = returnMethodParamsObject(params, request);
        if (method == null) {
            response.setHeaderKey("Content-Type", "application/json;charset=UTF-8");
            response.sentResponse("note found", StatusCode.NOTFOUND);
            return;
        }
        Object result;
        try {
            result = method.invoke(objectList.get(0), objects);
            if (result == null) {
                response.setHeaderKey("Content-Type", "application/json;charset=UTF-8");
                response.sentResponse("Error", StatusCode.ERROR);
            }
            response.setHeaderKey("Content-Type", "application/json;charset=UTF-8");
            assert result != null;
            if (result instanceof String) {
                response.sentResponse((String) result, StatusCode.OK);
                return;
            }
            if (result instanceof Integer) {
                response.sentResponse(String.valueOf(result), StatusCode.OK);
                return;
            }
            if (result instanceof Character) {
                response.sentResponse(String.valueOf(result), StatusCode.OK);
                return;
            }
            response.sentResponse(new JSONObject(result).toString(), StatusCode.OK);
            log.info(request.getMethod() + " " + request.getUri() + " " + StatusCode.OK.getCode());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

    }

    private void getUrl(List<String> list, String url) {
        String[] split = url.split("/");
        for (int i = 1; i < split.length; i++) {
            list.add("/" + split[i]);
        }
    }

    private List<Object> returnMethod(Map<String, List<Object>> map, List<String> list) {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 1; i < list.size(); i++) {
            stringBuffer.append(list.get(i));
        }
        if (!map.containsKey(stringBuffer.toString())) {
            Map<String, List<Object>> all = UrlMap.controllerContext.get("all").get(list.get(0));
            return all.get(stringBuffer.toString());
        }
        return map.get(stringBuffer.toString());
    }

    private Parameter[] returnMethodParams(Method method) {
        if (method == null) return new Parameter[]{};
        return method.getParameters();
    }

    private Object[] returnMethodParamsObject(Parameter[] params, Request request) {
        if (params.length == 0) return new Object[]{};
        Map<String, String> query = request.getQuery();
        String body = request.getBody();
        Object[] objects = new Object[params.length];
        Gson gson = new Gson();
        int n = 0;
        for (Parameter param : params) {
            Param paramAnnotation = param.getAnnotation(Param.class);
            if (paramAnnotation != null) {
                String value = paramAnnotation.value();
                if (value == null || value.equals("")) {
                    objects[n++] = null;
                    continue;
                }
                if (query == null) throw new RuntimeException("queryMap is null");
                if (!query.containsKey(value)) {
                    objects[n++] = null;
                    continue;
                }
                String s = query.get(value);

                Object o = gson.fromJson(s, param.getParameterizedType());
                objects[n++] = o;
                continue;
            }
            RequestBody requestBody = param.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                if (body == null || body.equals("")) {
                    objects[n++] = null;
                    continue;
                }
                Object o = gson.fromJson(body, param.getParameterizedType());
                objects[n++] = o;
                continue;
            }
            throw new RuntimeException("params not is RequestBody or query");
        }
        return objects;
    }
}
