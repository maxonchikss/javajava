package server;
import java.io.*;
import java.net.*;
import java.util.*;
public class NoteHttpServer {
    private static final int PORT = 8080;
    private static final List<String> notes = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Note HTTP Server запущен на порту " + PORT);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    // Чтение первой строки (метод + путь)
                    String firstLine = in.readLine();
                    if (firstLine == null || firstLine.isEmpty()) {
                        continue;
                    }

                    String[] startLine = firstLine.split(" ", 3);
                    String method = startLine[0];
                    String path = startLine[1];

                    // Чтение заголовков
                    Map<String, String> headers = new HashMap<>();
                    String line;
                    while (!(line = in.readLine()).isEmpty()) {
                        String[] parts = line.split(": ", 2);
                        if (parts.length == 2) {
                            headers.put(parts[0].toLowerCase(), parts[1]);
                        }
                    }

                    // Чтение тела, если есть Content-Length
                    String body = "";
                    if (headers.containsKey("content-length")) {
                        int contentLength = Integer.parseInt(headers.get("content-length"));
                        char[] bodyChars = new char[contentLength];
                        int totalRead = 0;
                        while (totalRead < contentLength) {
                            int read = in.read(bodyChars, totalRead, contentLength - totalRead);
                            if (read == -1) break;
                            totalRead += read;
                        }
                        body = new String(bodyChars);
                    }

                    String response;

                    if (method.equals("GET") && path.equals("/notes")) {
                        response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n" +
                                "\r\n" +
                                "<html><body><h1>Notes</h1><ul>";
                        for (int i = 0; i < notes.size(); i++) {
                            response += "<li>[" + i + "] " + notes.get(i) + "</li>";
                        }
                        response += "</ul></body></html>";

                    } else if (method.equals("POST") && path.equals("/add")) {
                        String note = body.trim();
                        if (!note.isEmpty()) {
                            notes.add(note);
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    "<html><body><h1>Note added</h1></body></html>";
                        } else {
                            response = "HTTP/1.1 400 Bad Request\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    "<html><body><h1>Empty note</h1></body></html>";
                        }

                    } else if (method.equals("DELETE") && path.startsWith("/notes")) {
                        // Извлекаем index из query-параметра
                        int index = -1;
                        boolean valid = false;
                        if (path.contains("?")) {
                            String query = path.split("\\?", 2)[1];
                            if (query.startsWith("index=")) {
                                try {
                                    index = Integer.parseInt(query.substring(6));
                                    if (index >= 0 && index < notes.size()) {
                                        valid = true;
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }

                        if (valid) {
                            notes.remove(index);
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    "<html><body><h1>Note deleted (index=" + index + ")</h1></body></html>";
                        } else {
                            response = "HTTP/1.1 400 Bad Request\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    "<html><body><h1>Invalid or missing index</h1></body></html>";
                        }

                    } else {
                        response = "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Type: text/html\r\n" +
                                "\r\n" +
                                "<html><body><h1>404 Not Found</h1></body></html>";
                    }

                    out.print(response);
                    out.flush(); // на всякий случай

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//curl -X POST http://localhost:8080/add -d "Buy milk"
//curl -X POST http://localhost:8080/add -d "Walk dog"
//curl http://localhost:8080/notes
//curl -X DELETE "http://localhost:8080/notes?index=0"
//curl http://localhost:8080/notes