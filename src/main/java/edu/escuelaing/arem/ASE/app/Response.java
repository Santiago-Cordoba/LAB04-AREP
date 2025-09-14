package edu.escuelaing.arem.ASE.app;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase para manejar las respuestas HTTP enviadas por el servidor
 */
public class Response {
    private final PrintWriter out;
    private final OutputStream outputStream;
    private boolean headersSent = false;
    private int statusCode = 200;
    private String statusMessage = "OK";
    private final Map<String, String> headers = new HashMap<>();

    public Response(PrintWriter out, OutputStream outputStream) {
        this.out = out;
        this.outputStream = outputStream;
        // Headers por defecto
        setHeader("Server", "Java HTTP Server");
        setContentType("text/plain"); // Valor por defecto
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void setStatusCode(int code) {
        this.statusCode = code;
        this.statusMessage = getDefaultStatusMessage(code);
    }

    public boolean isHeadersSent() {
        return headersSent;
    }

    /**
     * Envía una respuesta de texto plano
     */
    public void send(String content) throws IOException {
        if (!headersSent) {
            sendHeaders(content.length());
        }
        out.print(content);
        out.flush();
    }

    /**
     * Envía un archivo como respuesta
     */
    public void sendFile(byte[] content, String contentType) throws IOException {
        setContentType(contentType);
        if (!headersSent) {
            sendHeaders(content.length);
        }
        outputStream.write(content);
        outputStream.flush();
    }

    /**
     * Envía una respuesta JSON
     */
    public void json(String jsonContent) throws IOException {
        setContentType("application/json");
        send(jsonContent);
    }

    /**
     * Envía un error HTTP
     */
    public void sendError(int code, String message) throws IOException {
        setStatusCode(code);
        String errorContent = "<html><body><h1>" + code + " " + message + "</h1></body></html>";
        setContentType("text/html");
        send(errorContent);
    }

    /**
     * Redirige a otra URL
     */
    public void redirect(String url) throws IOException {
        setStatus(302, "Found");
        setHeader("Location", url);
        sendHeaders(0);
    }

    /**
     * Envía los headers HTTP
     */
    private void sendHeaders(int contentLength) {
        out.println("HTTP/1.1 " + statusCode + " " + statusMessage);
        setHeader("Content-Length", String.valueOf(contentLength));

        for (Map.Entry<String, String> header : headers.entrySet()) {
            out.println(header.getKey() + ": " + header.getValue());
        }

        out.println(); // Línea en blanco que separa headers del cuerpo
        out.flush();
        headersSent = true;
    }

    /**
     * Obtiene el mensaje de estado por defecto para un código HTTP
     */
    private String getDefaultStatusMessage(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return "Unknown Status";
        }
    }
}