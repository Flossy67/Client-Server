import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
    private static final String PASSWD_FILE = "passwd.txt";
    private static final int MAX_ATTEMPTS = 3;

    public static void main(String args[]) {
        Map<String, String[]> credentials = loadCredentials(PASSWD_FILE);
        if (credentials == null) {
            System.out.println("Could not load " + PASSWD_FILE + ", shutting down");
            return;
        }

        try (ServerSocket server = new ServerSocket(5001)) {
            System.out.println("Server started");

            while (true) {
                System.out.println("Waiting for a client...");
                Socket socket = server.accept();
                System.out.println("Client accepted!");
                new Thread(new ClientHandler(socket, credentials)).start();
            }
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    private static Map<String, String[]> loadCredentials(String path) {
        Map<String, String[]> credentials = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(",");
                if (fields.length < 4) {
                    continue;
                }
                String username = fields[0];
                String password = fields[1];
                String major = fields[2];
                String advisor = fields[3];
                credentials.put(username, new String[] { password, major, advisor });
            }
        } catch (IOException i) {
            System.out.println(i);
            return null;
        }
        return credentials;
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final Map<String, String[]> credentials;

        ClientHandler(Socket socket, Map<String, String[]> credentials) {
            this.socket = socket;
            this.credentials = credentials;
        }

        @Override
        public void run() {
            try (Socket s = socket;
                 DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                 DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

                String major = null;
                String advisor = null;
                String authenticatedUser = null;

                for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                    out.writeUTF("Username: ");
                    String username = in.readUTF();

                    out.writeUTF("Password: ");
                    String password = in.readUTF();

                    String[] record = credentials.get(username);
                    if (record != null && record[0].equals(password)) {
                        authenticatedUser = username;
                        major = record[1];
                        advisor = record[2];
                        break;
                    }

                    int remaining = MAX_ATTEMPTS - attempt;
                    if (remaining > 0) {
                        out.writeUTF("Invalid username or password. " + remaining + " attempt(s) remaining.");
                    }
                }

                if (authenticatedUser == null) {
                    out.writeUTF("Only for ESU CPSC Students taking CMSC445\nYou are not yet invited yet");
                    System.out.println("Access denied for " + s.getRemoteSocketAddress());
                    return;
                }

                out.writeUTF("Welcome to CMSC445-Comp Networking class\nYou are invited to use Your Own Name Machine");
                System.out.println(authenticatedUser + " logged in");

                String line = "";
                while (!line.equals("Over")) {
                    line = in.readUTF();
                    System.out.println("[" + authenticatedUser + "] " + line);
                }
                System.out.println("Closing connection for " + authenticatedUser);
            } catch (EOFException e) {
                System.out.println("Client disconnected!");
            } catch (IOException i) {
                System.out.println(i);
            }
        }
    }
}
