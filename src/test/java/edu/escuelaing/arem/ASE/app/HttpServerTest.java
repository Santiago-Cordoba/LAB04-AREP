package edu.escuelaing.arem.ASE.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class HttpServerTest {

    private HttpServer server;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void setUp() {
        server = new HttpServer();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testParseRequestGET() throws IOException {
        String request = "GET /api/songs HTTP/1.1\nHost: localhost\n\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        Request parsed = HttpServer.parseRequest(reader);

        assertAll(
                () -> assertEquals("GET", parsed.getMethod()),
                () -> assertEquals("/api/songs", parsed.getPath())
        );
    }

    @Test
    public void testParseRequestPOST() throws IOException {
        String request = "POST /api/songs/add?title=Test&artist=Test HTTP/1.1\nHost: localhost\n\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        Request parsed = HttpServer.parseRequest(reader);

        assertAll(
                () -> assertEquals("POST", parsed.getMethod()),
                () -> assertEquals("/api/songs/add", parsed.getPath()),
                () -> assertEquals("Test", parsed.getQueryParam("title")),
                () -> assertEquals("Test", parsed.getQueryParam("artist"))
        );
    }

    @Test
    public void testParseRequestWithQueryParams() throws IOException {
        String request = "GET /test?name=John&age=30 HTTP/1.1\nHost: localhost\n\n";
        BufferedReader reader = new BufferedReader(new StringReader(request));

        Request parsed = HttpServer.parseRequest(reader);

        assertAll(
                () -> assertEquals("GET", parsed.getMethod()),
                () -> assertEquals("/test", parsed.getPath()),
                () -> assertEquals("John", parsed.getQueryParam("name")),
                () -> assertEquals("30", parsed.getQueryParam("age"))
        );
    }

    @Test
    public void testHandleApiRequestGetSongs() throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        Request request = new Request("GET", "/api/songs", queryParams);
        
        HttpServer.handleApiRequest(outputStream, request);
        String response = outputStream.toString();

        assertAll(
                () -> assertTrue(response.contains("HTTP/1.1 200 OK")),
                () -> assertTrue(response.contains("Content-Type: application/json")),
                () -> assertTrue(response.contains("Bohemian Rhapsody")),
                () -> assertTrue(response.contains("Queen"))
        );
    }

    @Test
    public void testHandleApiRequestAddSong() throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("title", "NewSong");
        queryParams.put("artist", "NewArtist");
        Request request = new Request("POST", "/api/songs/add", queryParams);

        HttpServer.handleApiRequest(outputStream, request);
        String response = outputStream.toString();

        assertAll(
                () -> assertTrue(response.contains("HTTP/1.1 200 OK")),
                () -> assertTrue(response.contains("Canción agregada"))
        );
    }

    @Test
    public void testHandleApiRequestInvalidEndpoint() throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        Request request = new Request("GET", "/api/invalid", queryParams);

        HttpServer.handleApiRequest(outputStream, request);
        String response = outputStream.toString();

        assertAll(
                () -> assertTrue(response.contains("HTTP/1.1 404 Not Found")),
                () -> assertTrue(response.contains("Endpoint no encontrado"))
        );
    }

    @Test
    public void testServeStaticFileExisting() throws IOException {
        // Crear archivo de prueba temporal
        String testContent = "<html><body>Test</body></html>";
        Path testFile = Paths.get("src/main/resources/test.html");
        Files.write(testFile, testContent.getBytes());

        HttpServer.serveStaticFile(outputStream, "/test.html");
        String response = outputStream.toString();

        assertAll(
                () -> assertTrue(response.contains("HTTP/1.1 200 OK")),
                () -> assertTrue(response.contains("text/html")),
                () -> assertTrue(response.contains(testContent))
        );


        Files.deleteIfExists(testFile);
    }

    @Test
    public void testServeStaticFileNotFound() throws IOException {
        HttpServer.serveStaticFile(outputStream, "/nonexistent.html");
        String response = outputStream.toString();

        assertAll(
                () -> assertTrue(response.contains("HTTP/1.1 404 Not Found")),
                () -> assertTrue(response.contains("text/html"))
        );
    }

    @Test
    public void testRequestParseQueryParams() {
        String queryString = "name=John&age=30&city=New+York";
        Map<String, String> params = Request.parseQueryParams(queryString);

        assertAll(
                () -> assertEquals("John", params.get("name")),
                () -> assertEquals("30", params.get("age")),
                () -> assertEquals("New York", params.get("city")),
                () -> assertEquals(3, params.size())
        );
    }

    @Test
    public void testRequestParseQueryParamsWithEncoding() {
        String queryString = "message=Hello%20World&user=Juan%20Pérez";
        Map<String, String> params = Request.parseQueryParams(queryString);

        assertAll(
                () -> assertEquals("Hello World", params.get("message")),
                () -> assertEquals("Juan Pérez", params.get("user"))
        );
    }

    @Test
    public void testRequestGetQueryParam() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name", "John");
        queryParams.put("age", "30");
        
        Request request = new Request("GET", "/test", queryParams);

        assertAll(
                () -> assertEquals("John", request.getQueryParam("name")),
                () -> assertEquals("30", request.getQueryParam("age")),
                () -> assertNull(request.getQueryParam("nonexistent"))
        );
    }

    @Test
    public void testRequestGetHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "TestBrowser");
        headers.put("Accept", "application/json");
        
        Request request = new Request("GET", "/test", new HashMap<>(), headers);

        assertAll(
                () -> assertEquals("TestBrowser", request.getHeader("User-Agent")),
                () -> assertEquals("application/json", request.getHeader("Accept")),
                () -> assertNull(request.getHeader("Nonexistent")),
                () -> assertEquals(2, request.getHeaders().size())
        );
    }
}