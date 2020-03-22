public class main {
    public static void main(String argv[]) throws Exception {
        Client clientSocket = new Client();
        while (true) {
            HttpRequest req =clientSocket.newRequest();
            clientSocket.connectSocket(req);
            clientSocket.sendRequest(req);
            clientSocket.handleResponse(req);
            clientSocket.getImgs( "websites/"+req.getHostDir()+req.getPath(), req.getHost());
        }
    }
}
