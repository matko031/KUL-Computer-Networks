public class main {
    public static void main(String argv[]) throws Exception {
        Client clientSocket = new Client();

        clientSocket.setArguments();
        clientSocket.connectSocket();
        clientSocket.sendCommand();
        clientSocket.readHeaders();
        clientSocket.readBody();
    }
}
