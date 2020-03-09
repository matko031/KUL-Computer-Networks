import javax.lang.model.util.Types;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;
import java.lang.*;

class Client  {

    private static List<String> HTTPCommands = new ArrayList<String>(Arrays.asList("GET", "POST", "PUT", "HEAD")); // define all supported http commands;

    private static String URIPatternString = "^(https?://)?((www\\.)?(.*?\\.)+(.+?))(/(.*))?$"; // regex to test if the URI is valid, not 100% waterproof
    private static Pattern URIPatternRegex = Pattern.compile(URIPatternString); // compile regex

    private static String isIntPatternString = "\\d+"; //regex to test if the given port is an int
    private static Pattern isIntRegex = Pattern.compile(isIntPatternString); // compile regex

    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private Socket clientSocket;
    private BufferedWriter bw;

    private String HTTPCommand;
    private String host;
    private String port;
    private String path;
    private String lang;
    private String encoding = "";
    private char[] buffer = new char[0];
    private int bytesToRead = 0;


    public void Client() {

    }



    /*
    Takes in the list of arguments and check if they are all valid
    The order of arguments is HTTPCommand - URI - port - language
    Returns HTTPCommand, host, path, port, language
     */
    static boolean checkArguments(String[] arguments){
        if(arguments.length != 4){
            System.out.println("Wrong number of arguments"); // if there are not exactly four arguments, error message is displayed and new input requested
            return false;
        }

        String HTTPcommand = arguments[0];
        if( ! HTTPCommands.contains(HTTPcommand)){
            System.out.println("Wrong http command"); // checks if a supported http command is given
            return false;
        }

        Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
        if(! uriMatcher.matches()){
            System.out.println("Unsupported URI"); // checks if the URI matches the expected URI pattern
            return false;
        }


        String port = arguments[2];
        Matcher portMatcher = isIntRegex.matcher(port);  // create a matcher that will check if the given port is an int
        if(! portMatcher.matches()){
            System.out.println("Port must be an integer"); // checks if the given port is an int
            return false;
        }

        return true;
    }

    public String[] readArguments(){
        Scanner myObj = new Scanner(System.in); // create scanner object
        String command = myObj.nextLine();  // read user input
        //String command = "GET www.google.com/ 80 en";
        return command.split(" "); // split user input on every space and return the array
    }

    public void setArguments() throws IOException{
        String[] arguments = readArguments();
        if (this.checkArguments(arguments)) {
            this.HTTPCommand = arguments[0];
            this.port = arguments[2];
            this.lang = arguments[3];


            // save found groups in their respective variables
            String[] url = arguments[1].split("/");
            this.host = url[0];
            this.path = url.length == 1 ? "/" : url[1];

            this.bw = new BufferedWriter(new FileWriter(host + ".html")); // initialise the buffered writer

        }
    }


    public void connectSocket() throws IOException {

        this.clientSocket = new Socket(this.host, Integer.parseInt(this.port)); // open a socket with the host
        this.clientSocket.setSoTimeout(2000);

        this.outToServer = new DataOutputStream(clientSocket.getOutputStream()); // create output stream object
        this.inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }


    /*
    Method that takes DataOutputStream to server and array of output strings as arguments.
    Newline characters are not expected at the end of each string, they are added in the method if not present
     */
    public void sendGETHEADRequest() throws IOException{

        String header1 = this.HTTPCommand + " " + this.path + " HTTP/1.1";
        String header2 = "host: " + this.host;
        String[] outputs =  new String[]{header1, header2};

        for (String output : outputs) {
            this.outToServer.writeBytes(output);
            this.outToServer.writeBytes("\n");
        }
        this.outToServer.writeBytes("\n");

    }

    public void sendPOSTPUTRequest(){

    }

    public void sendCommand() {
        try {
            if (this.HTTPCommand.equals("GET") || this.HTTPCommand.equals("HEAD")) {
                this.sendGETHEADRequest();
            } else {
                this.sendPOSTPUTRequest();
            }
        }
        catch (IOException e){
            System.out.println(e);
        }
    }

    public String readLine(BufferedReader inFromServer) throws IOException{
        int b;
        char c;
        String line = "";
        while(( b = inFromServer.read()) != -1){
            c = (char) b;
            line += c;

            if (b == 10){
                break;
            }
        }

        return line;
    }


    public void readHeaders() throws IOException{
        this.encoding = "";
        this.buffer = new char[0];
        this.bytesToRead = 0;

        String line; // initialize string where current line from the response is saved
        while (true) {
            line = readLine(inFromServer);
            System.out.println(line);
            bw.write(line + "\n");

            if (this.encoding.equals("")) {
                if (line.contains("Transfer-Encoding: chunked")) {
                    this.encoding = "Transfer-Encoding";
                } else if (line.contains("Content-Length")) {
                    this.encoding = "Content-Length";
                    this.bytesToRead = Integer.parseInt(line.split(" ")[1]);
                    this.buffer = new char[this.bytesToRead];
                }
            }

            if (line.equals("\r\n")) {
                bw.flush();
                break;
            }
        }
    }

    public void readBody() throws IOException{

        String line;

        if (this.encoding.equals("Content-Length")) {
            this.inFromServer.read(this.buffer, 0, this.bytesToRead);
            line = new String(this.buffer);

            this.bw.write(line + "\n");
            System.out.print(line);

        } else {

            while (true) {

                try {
                    line = readLine(inFromServer);
                    System.out.print(line);

                    if(line.equals("0\r\n")){
                        break;
                    }

                    if (!line.equals("\r\n")) {
                        bytesToRead = Integer.parseInt(line.substring(0 , line.length()-2), 16);
                        buffer = new char[bytesToRead];
                        inFromServer.read(buffer, 0, bytesToRead);
                        line = new String(buffer);
                    }

                    System.out.print(line);
                    bw.write(line + "\n");
                } catch (SocketTimeoutException ioe) {
                    break;
                }
            }
        }
        bw.close();
    }


}


