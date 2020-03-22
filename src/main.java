public class main {
    public static void main(String argv[]) throws Exception {
        Client clientSocket = new Client();
        while (true) {
            HttpRequest req =clientSocket.newRequest();
            if (req.isArgumentsCorrect()) {
                clientSocket.connectSocket(req);
                clientSocket.sendRequest(req);
                clientSocket.handleResponse(req);
            }
        }
    }
}
