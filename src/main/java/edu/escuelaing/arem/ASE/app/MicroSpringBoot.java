package edu.escuelaing.arem.ASE.app;

import edu.escuelaing.arem.ASE.app.Annotations.*;

import java.io.IOException;
import java.lang.reflect.*;


public class MicroSpringBoot {

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("Uso: java MicroSpringBoot <clase-controladora>");
                System.exit(1);
            }

            String controllerClassName = args[0];
            Class<?> controllerClass = Class.forName(controllerClassName);

            System.out.println("=== Iniciando MicroSpringBoot ===");


            configureHttpServer();

            registerController(controllerClass);

            System.out.println("=== MicroSpringBoot configurado correctamente ===");
            System.out.println("Endpoints registrados:");
            HttpServer.getRoutes.keySet().forEach(route ->
                    System.out.println("  GET " + route));

            // Iniciar el servidor (esto bloqueará el hilo actual)
            System.out.println("Iniciando servidor HTTP...");
            HttpServer.main(new String[0]);

        } catch (Exception e) {
            System.err.println("Error iniciando MicroSpringBoot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void configureHttpServer() {
        // Configurar automáticamente el HttpServer
        HttpServer.staticfiles("src/main/resources");
        HttpServer.setContextPath("");
        System.out.println("Servidor configurado con:");
        System.out.println("  - Archivos estáticos en: src/main/resources");
        System.out.println("  - Context path: /");
    }

    private static void registerController(Class<?> controllerClass) throws Exception {
        if (controllerClass.isAnnotationPresent(RestController.class)) {
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            System.out.println("Registrando controlador: " + controllerClass.getSimpleName());

            // Registrar todos los métodos con @GetMapping
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    registerGetMapping(controllerInstance, method);
                }
            }
        } else {
            System.err.println("La clase " + controllerClass.getName() + " no tiene la anotación @RestController");
        }
    }

    private static void registerGetMapping(Object controllerInstance, Method method) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        String path = getMapping.value();

        HttpServer.get(path, (request, response) -> {
            try {

                Object[] parameters = prepareMethodParameters(method, request, response);

                // Invocar el método del controlador
                Object result = method.invoke(controllerInstance, parameters);

                if (!response.isHeadersSent() && result != null) {
                    response.send(result.toString());
                }

                return "";
            } catch (Exception e) {
                // Manejar errores
                return handleException(e, response);
            }
        });

        System.out.println("  → GET " + path + " → " + method.getName());
    }

    private static Object[] prepareMethodParameters(Method method, Request request, Response response) {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> paramType = parameter.getType();


            if (paramType.equals(Request.class)) {
                parameterValues[i] = request;
            }

            else if (paramType.equals(Response.class)) {
                parameterValues[i] = response;
            }

            else if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam annotation = parameter.getAnnotation(RequestParam.class);
                String paramValue = request.getQueryParam(annotation.value());

                if (paramValue == null || paramValue.isEmpty()) {
                    paramValue = annotation.defaultValue();
                }


                parameterValues[i] = convertToType(paramValue, paramType);
            } else {

                parameterValues[i] = getDefaultValue(paramType);
            }
        }

        return parameterValues;
    }

    private static Object convertToType(String value, Class<?> targetType) {
        if (targetType.equals(String.class)) {
            return value;
        } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return value.isEmpty() ? 0 : Integer.parseInt(value);
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return value.isEmpty() ? 0.0 : Double.parseDouble(value);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type.equals(String.class)) {
            return "";
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return 0;
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return 0.0;
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return false;
        }
        return null;
    }

    private static String handleException(Exception e, Response response) {
        try {
            if (!response.isHeadersSent()) {
                response.sendError(500, "Error interno del servidor: " + e.getMessage());
            }
        } catch (IOException ioException) {
            System.err.println("Error enviando respuesta de error: " + ioException.getMessage());
        }

        Throwable cause = e.getCause() != null ? e.getCause() : e;
        System.err.println("Error en endpoint: " + cause.getMessage());
        cause.printStackTrace();

        return "";
    }
}