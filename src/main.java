public class main {
    public static void main(String argv[]) throws Exception {
        Client clientSocket = new Client();
        while (true) {
            clientSocket.setArguments();
            if (clientSocket.argumentsCorrect) {
                clientSocket.connectSocket();
                clientSocket.sendCommand();
                clientSocket.readHeaders();
                clientSocket.readBody();
                clientSocket.getImgs();
            }
        }
    }
}
