package Server;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class NoteHttpServer {
    private static final int PORT = 8080;
    private static final List<String> notes = new ArrayList<>();
    private static final File PUBLIC_DIR = new File("src/Server/public");

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Note HTTP Server запущен на порту " + PORT);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     OutputStream rawOut = clientSocket.getOutputStream();
                     PrintWriter out = new PrintWriter(rawOut, true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                    String inputLine;
                    StringBuilder request = new StringBuilder();
                    while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
                        request.append(inputLine).append("\n");
                    }

                    if (request.isEmpty()) continue;

                    String[] requestLines = request.toString().split("\n");
                    String method = requestLines[0].split(" ")[0];
                    String path = requestLines[0].split(" ")[1];
                    String response;

                    if (method.equals("GET") && path.equals("/notes")) {
                        response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n\r\n" +
                                "<html><body><h1>Notes</h1><ul>";
                        for (String note : notes) {
                            response += "<li>" + note + "</li>";
                        }
                        response += "</ul></body></html>";
                        out.print(response);

                    } else if (method.equals("POST") && path.equals("/add")) {
                        String note = requestLines[requestLines.length - 1].trim();
                        if (!note.isEmpty()) {
                            notes.add(note);
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/html\r\n\r\n" +
                                    "<html><body><h1>Note added</h1></body></html>";
                        } else {
                            response = "HTTP/1.1 400 Bad Request\r\n" +
                                    "Content-Type: text/html\r\n\r\n" +
                                    "<html><body><h1>Empty note</h1></body></html>";
                        }
                        out.print(response);

                    } else if (method.equals("POST") && path.equals("/remove")) {
                        if (!notes.isEmpty()) {
                            notes.remove(notes.size() - 1);
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/html\r\n\r\n" +
                                    "<html><body><h1>Last note removed</h1></body></html>";
                        } else {
                            response = "HTTP/1.1 400 Bad Request\r\n" +
                                    "Content-Type: text/html\r\n\r\n" +
                                    "<html><body><h1>No notes to remove</h1></body></html>";
                        }
                        out.print(response);

                    } else if (method.equals("GET")) {
                        File file = new File(PUBLIC_DIR, path);
                        if (file.exists() && file.isFile()) {
                            String contentType = getContentType(file);
                            byte[] fileBytes = Files.readAllBytes(file.toPath());

                            String headers = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + contentType + "\r\n" +
                                    "Content-Length: " + fileBytes.length + "\r\n\r\n";

                            rawOut.write(headers.getBytes());
                            rawOut.write(fileBytes);
                            rawOut.flush();
                        } else {
                            response = "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Type: text/html\r\n\r\n" +
                                    "<html><body><h1>404 Not Found</h1></body></html>";
                            out.print(response);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getContentType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}

//http://localhost:8080/index.html
//http://localhost:8080/image.jpg