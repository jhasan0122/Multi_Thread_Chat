import java.io.*;
import java.net.*;
import java.util.*;

public class MultiClientServer {
    private static Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        int port = 65432;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                clientSockets.add(socket);
                System.out.println("New client connected: " + socket.getInetAddress());

                // Handle each client in a new thread
                new ClientHandler(socket).start();
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Broadcast message to all connected clients
    public static void broadcastMessage(String message, Socket excludeSocket) {
        synchronized (clientSockets) {
            for (Socket clientSocket : clientSockets) {
                if (clientSocket != excludeSocket) {
                    try {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println(message);
                    } catch (IOException e) {
                        System.out.println("Broadcast exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Remove a client from the list of clients
    public static void removeClient(Socket socket) {
        clientSockets.remove(socket);
        System.out.println("Client disconnected: " + socket.getInetAddress());
    }
}

class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                MultiClientServer.broadcastMessage("Client " + socket.getInetAddress() + ": " + message, socket);
            }

        } catch (IOException ex) {
            System.out.println("Client exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
                MultiClientServer.removeClient(socket);
            } catch (IOException ex) {
                System.out.println("Could not close socket");
                ex.printStackTrace();
            }
        }
    }
}
