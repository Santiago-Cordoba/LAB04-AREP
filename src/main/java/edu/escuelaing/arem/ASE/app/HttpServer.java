package edu.escuelaing.arem.ASE.app;

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class HttpServer {
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();
    private static final List<Song> songs = new ArrayList<>();
    private static boolean running = true;

    static Map<String, BiFunction<Request, Response, String>> getRoutes = new ConcurrentHashMap<>();
    private static String staticFilesBase = "src/main/resources";
    private static String contextPath = "/App";
    public static Map<String, BiFunction<Request, Response, String>> postRoutes = new ConcurrentHashMap<>();

    private static ExecutorService threadPool;
    private static int threadPoolSize = 10;

    static {
        CONTENT_TYPES.put("html", "text/html");
        CONTENT_TYPES.put("js", "text/javascript");
        CONTENT_TYPES.put("css", "text/css");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("json", "application/json");

        songs.add(new Song("Bohemian Rhapsody", "Queen"));
        songs.add(new Song("Imagine", "John Lennon"));
        songs.add(new Song("Hotel California", "Eagles"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (running) {
                shutdownServer();
            }
        }));
    }

    public static void staticfiles(String path) {
        staticFilesBase = path;
    }

    public static void get(String route, BiFunction<Request, Response, String> handler) {
        getRoutes.put(route, handler);
    }

    public static void setContextPath(String path) {
        contextPath = path;
    }

    public static void setThreadPoolSize(int size) {
        threadPoolSize = size;
    }

    public static int getThreadPoolSize() {
        return threadPoolSize;
    }

    public static void configureForEnvironment(String environment) {
        switch (environment.toLowerCase()) {
            case "production":
                setThreadPoolSize(50);
                staticfiles("/app/resources");
                break;
            case "development":
                setThreadPoolSize(10);
                staticfiles("src/main/resources");
                break;
            default:
                setThreadPoolSize(20);
                staticfiles("src/main/resources");
        }
    }

    private static boolean isRunningInDocker() {
        File dockerEnv = new File("/.dockerenv");
        return dockerEnv.exists() || System.getenv("DOCKER_CONTAINER") != null;
    }

    private static void initializeStaticFilesPath() {
        if (isRunningInDocker()) {
            staticFilesBase = "/usrapp/bin/resources";
            System.out.println("Modo Docker detectado. Static files en: " + staticFilesBase);
        } else {
            staticFilesBase = "src/main/resources";
            System.out.println("Modo desarrollo. Static files en: " + staticFilesBase);
        }
    }

    public static void main(String[] args) throws IOException {

        initializeStaticFilesPath();

        threadPool = Executors.newFixedThreadPool(threadPoolSize);

        get("/hello", (req, resp) -> "Hello " + req.getQueryParam("name"));

        ServerSocket serverSocket = startServer(35000);
        while (running) {
            handleClientConnection(serverSocket);
        }
        serverSocket.close();

        shutdownServer();
    }

    private static ServerSocket startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor de música iniciado en http://localhost:" + port);
        System.out.println("Context path: " + contextPath);
        System.out.println("Static files base: " + staticFilesBase);
        System.out.println("Thread pool size: " + threadPoolSize);
        return serverSocket;
    }

    private static void handleClientConnection(ServerSocket serverSocket) {
        try {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     OutputStream outputStream = clientSocket.getOutputStream()) {

                    Request request = parseRequest(in);
                    if (request != null) {
                        processRequest(outputStream, request);
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error en conexión con cliente: " + e.getMessage());
                    }
                } finally {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error cerrando socket: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            if (running) {
                System.err.println("Error aceptando conexión: " + e.getMessage());
            }
        }
    }

    static Request parseRequest(BufferedReader in) throws IOException {
        String inputLine;
        String method = "";
        String path = "";
        Map<String, String> headers = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();

        boolean isFirstLine = true;
        while ((inputLine = in.readLine()) != null) {
            if (isFirstLine) {
                if (inputLine.startsWith("GET") || inputLine.startsWith("POST")) {
                    String[] requestParts = inputLine.split(" ");
                    method = requestParts[0];

                    String fullPath = requestParts[1];
                    int queryIndex = fullPath.indexOf('?');
                    if (queryIndex != -1) {
                        path = fullPath.substring(0, queryIndex);
                        String queryString = fullPath.substring(queryIndex + 1);
                        queryParams = Request.parseQueryParams(queryString);
                    } else {
                        path = fullPath;
                    }
                }
                isFirstLine = false;
            } else {

                if (inputLine.isEmpty()) break;
                int colonIndex = inputLine.indexOf(':');
                if (colonIndex > 0) {
                    String headerName = inputLine.substring(0, colonIndex).trim();
                    String headerValue = inputLine.substring(colonIndex + 1).trim();
                    headers.put(headerName, headerValue);
                }
            }
            if (!in.ready()) break;
        }

        return !method.isEmpty() ? new Request(method, path, queryParams, headers) : null;
    }

    private static void processRequest(OutputStream outputStream, Request request) throws IOException {

        if (request.getPath().startsWith(contextPath)) {
            String frameworkPath = request.getPath().substring(contextPath.length());
            BiFunction<Request, Response, String> handler = getRoutes.get(frameworkPath);

            if (handler != null) {
                handleFrameworkRequest(outputStream, request, handler);
                return;
            }
        }

        if ("/image".equals(request.getPath())) {
            serveImage(outputStream, staticFilesBase);
            return;
        }


        if (request.getPath().startsWith("/api/songs")) {
            handleApiRequest(outputStream, request);
        } else {
            serveStaticFile(outputStream, request.getPath());
        }
    }

    private static void handleFrameworkRequest(OutputStream outputStream, Request request,
                                               BiFunction<Request, Response, String> handler) throws IOException {
        PrintWriter out = new PrintWriter(outputStream, true);
        Response response = new Response(out, outputStream);

        try {
            String result = handler.apply(request, response);
            if (!response.isHeadersSent()) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/plain");
                out.println();
            }
            out.println(result);
        } catch (Exception e) {
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Content-Type: text/plain");
            out.println();
            out.println("Error processing request: " + e.getMessage());
        }
    }

    /**
     * Maneja las solicitudes a la API de canciones.
     *
     * @param outputStream Flujo de salida para la respuesta
     * @param request Solicitud a procesar
     * @throws IOException Si hay error de E/S
     */
    static void handleApiRequest(OutputStream outputStream, Request request) throws IOException {
        PrintWriter out = new PrintWriter(outputStream, true);

        try {
            if ("GET".equals(request.getMethod()) && "/api/songs".equals(request.getPath())) {
                sendJsonResponse(out, 200, formatSongsJson());
            }
            else if ("POST".equals(request.getMethod()) && request.getPath().startsWith("/api/songs/add")) {
                Song newSong = parseNewSong(request);
                songs.add(newSong);
                sendJsonResponse(out, 200, "{\"status\":\"success\", \"message\":\"Canción agregada\"}");
            }
            else {
                sendJsonResponse(out, 404, "{\"error\":\"Endpoint no encontrado\"}");
            }
        } catch (Exception e) {
            sendJsonResponse(out, 500, "{\"error\":\"Error en el servidor: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Extrae los parámetros de título y artista de la URL.
     *
     * @param request La solicitud con parámetros
     * @return Nueva instancia de Song
     */
    private static Song parseNewSong(Request request) {
        String title = request.getQueryParam("title");
        String artist = request.getQueryParam("artist");
        return new Song(title, artist);
    }

    private static String formatSongsJson() {
        StringBuilder json = new StringBuilder("{\"songs\":[");
        for (int i = 0; i < songs.size(); i++) {
            if (i > 0) json.append(",");
            json.append(songs.get(i).toString());
        }
        json.append("]}");
        return json.toString();
    }

    private static void sendJsonResponse(PrintWriter out, int statusCode, String jsonBody) {
        out.println("HTTP/1.1 " + statusCode + " " + getStatusMessage(statusCode));
        out.println("Content-Type: application/json");
        out.println();
        out.println(jsonBody);
    }

    private static String getStatusMessage(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            default: return "Unknown Status";
        }
    }

    /**
     * Sirve archivos estáticos desde el directorio configurado.
     *
     * @param outputStream Flujo de salida para la respuesta
     * @param requestPath Ruta solicitada por el cliente
     * @throws IOException Si hay error de E/S
     */
    static void serveStaticFile(OutputStream outputStream, String requestPath) throws IOException {
        String path = requestPath.equals("/") ? "/index.html" : requestPath;
        Path filePath = Paths.get(staticFilesBase + path);

        try (PrintWriter out = new PrintWriter(outputStream, true)) {
            if (Files.exists(filePath)) {
                String extension = path.substring(path.lastIndexOf(".") + 1);
                String contentType = CONTENT_TYPES.getOrDefault(extension, "text/plain");
                byte[] fileContent = Files.readAllBytes(filePath);

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + contentType);
                out.println("Content-Length: " + fileContent.length);
                out.println();
                out.flush();

                outputStream.write(fileContent);
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<h1>404 Not Found</h1>");
            }
        }
    }

    private static void serveImage(OutputStream outputStream, String imagePath) throws IOException {
        Path filePath = Paths.get(imagePath);

        try (PrintWriter out = new PrintWriter(outputStream, true)) {
            if (Files.exists(filePath)) {

                String fileName = filePath.getFileName().toString();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");

                byte[] fileContent = Files.readAllBytes(filePath);

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + contentType);
                out.println("Content-Length: " + fileContent.length);
                out.println("Cache-Control: max-age=3600");
                out.println();
                out.flush();

                outputStream.write(fileContent);
            } else {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<h1>404 Not Found - Image not available</h1>");
                out.println("<p>La imagen no se encuentra en: " + imagePath + "</p>");
            }
        }
    }

    private static void shutdownServer() {
        System.out.println("Iniciando apagado elegante del servidor...");
        running = false;

        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                        System.err.println("El pool de hilos no terminó correctamente");
                    }
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Servidor apagado correctamente");
    }
}