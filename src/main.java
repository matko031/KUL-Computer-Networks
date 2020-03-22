public class main {
    public static void main(String argv[]) throws Exception {
        Client clientSocket = new Client();
        while (true) {
            clientSocket.setArguments();
            if (clientSocket.argumentsCorrect) {
                clientSocket.connectSocket();
                clientSocket.sendRequest();
                clientSocket.readHeaders();
                if (clientSocket.responseCode == 200) {
                    clientSocket.readBody();
                    clientSocket.translateHtml();
                }
                //TODO if-modified-since part
            }
        }
    }
}
