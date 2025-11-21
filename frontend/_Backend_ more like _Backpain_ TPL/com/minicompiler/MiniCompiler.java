package com.minicompiler;

import com.minicompiler.analyzer.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class MiniCompiler {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("\nMiniCompiler 2025 is running!");
        System.out.println("Open your browser → http://localhost:" + port + "\n");

        while (true) {
            Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private static void handleClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null) return;

            // Read headers
            String header;
            String phase = "lexical"; // default
            int contentLength = 0;

            while ((header = in.readLine()) != null && !header.isEmpty()) {
                if (header.startsWith("X-Phase:")) {
                    phase = header.substring(8).trim().toLowerCase();
                }
                if (header.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(header.substring(15).trim());
                }
            }

            // Read body (source code)
            char[] body = new char[contentLength];
            in.read(body, 0, contentLength);
            String code = new String(body);

            // Serve files for GET
            if (requestLine.startsWith("GET")) {
                String path = requestLine.split(" ")[1];
                if (path.equals("/") || path.equals("/index.html")) path = "/index.html";

                File file = new File("." + path);
                if (file.exists() && file.isFile()) {
                    String contentType = "text/html";
                    if (path.endsWith(".css")) contentType = "text/css";
                    if (path.endsWith(".js")) contentType = "application/javascript";

                    byte[] content = Files.readAllBytes(file.toPath());
                    String response = "HTTP/1.1 200 OK\r\nContent-Type: " + contentType + "\r\n\r\n";
                    out.write(response.getBytes());
                    out.write(content);
                } else {
                    out.write("HTTP/1.1 404 Not Found\r\n\r\n<h1>404 - File Not Found</h1>".getBytes());
                }
                return;
            }

            // Handle POST /analyze
            if (requestLine.startsWith("POST /analyze")) {
                String result = runAnalysis(code.trim(), phase);
                String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\n" + result;
                out.write(response.getBytes());
            }

        } catch (Exception e) {
            // Silent fail (won't crash server)
        }
    }

    private static String runAnalysis(String code, String phase) {
        if (code.isEmpty()) {
            return "Error: No code provided.\nPlease load a file first.";
        }

        var tokens = LexicalAnalyzer.tokenize(code);

        // LEXICAL
        if (!LexicalAnalyzer.isValidLexically(tokens)) {
            return "LEXICAL ANALYSIS FAILED!\n\nInvalid tokens found. Check your syntax (e.g. missing ;, wrong quotes, unknown symbols).";
        }

        if ("lexical".equals(phase)) {
            return "Performing Lexical Analysis...\nLEXICAL ANALYSIS PASSED!";
        }

        // SYNTAX
        if (!SyntaxAnalyzer.analyze(tokens)) {
            return "SYNTAX ERROR!\n\nCheck your code structure:\nExpected: data_type identifier [= value] ;\nExamples:\nint x = 10;\nString name = \"John\";";
        }

        if ("syntax".equals(phase)) {
            return "Performing Syntax Analysis...\nSYNTAX PASSED!";
        }

        // SEMANTIC
        if (!SemanticAnalyzer.analyze(tokens)) {
            return "SEMANTIC ANALYSIS FAILED!\n\nPossible issues:\n• Duplicate variable declaration\n• Type mismatch (e.g. int x = \"hello\";)\n• Invalid value for type";
        }

        return "Performing Semantic Analysis...\nSEMANTIC PASSED!";
    }
}