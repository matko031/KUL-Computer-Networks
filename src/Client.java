import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;

class Client  {
    public static void main(String argv[]) throws Exception {

        List<String> HTTPcommands = new ArrayList<String>(Arrays.asList("GET", "POST", "PUT", "DELETE")); // define all supported http commands

        String URIPatternString = "^(https?://)?((www\\.)?(.*?\\.)+(.+?))(/(.*))?$"; // regex to test if the URI is valid, not 100% waterproof
        Pattern URIPatternRegex = Pattern.compile(URIPatternString); // compile regex

        String isIntPatternString = "\\d+"; //regex to test if the given port is an int
        Pattern isIntRegex = Pattern.compile(isIntPatternString); // compile regex


        // initialize argument strings
        String HTTPcommand;
        String host;
        String path;
        String query;
        String port;
        String lang;


        while (true){
            Scanner myObj = new Scanner(System.in); // create scanner object
            String command = myObj.nextLine();  // read user input
            //String command = "GET www.google.com/ 80 en";
            String[] arguments = command.split(" "); // split user input on every space

            if(arguments.length != 4){
                System.out.println("Wrong number of arguments"); // if there are not exactly four arguments, error message is displayed and new input requested
                continue;
            }

            HTTPcommand = arguments[0];
            if( ! HTTPcommands.contains(HTTPcommand)){
                System.out.println("Wrong http command"); // checks if valid http command is given
                continue;
            }

            Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
            uriMatcher.find(); // find all matches
            // save found groups in their respective variables
            host = uriMatcher.group(2);
            path = uriMatcher.group(6) == null ? "" : uriMatcher.group(6);

            if(! uriMatcher.matches()){
                System.out.println("Unsupported URI"); // checks if the URI matches the expected URI pattern
                continue;
            }


            port = arguments[2];
            Matcher portMatcher = isIntRegex.matcher(port);  // create a matcher that will check if the given port is an int
            if(! portMatcher.matches()){
                System.out.println("Port must be an integer"); // checks if the given port is an int
                continue;
            }

            Socket clientSocket = new Socket(host, Integer.parseInt(port)); // open a socket with the host
            clientSocket.setSoTimeout(2000);
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream()); // create output stream object

            String header1 = HTTPcommand + " /" + path + " HTTP/1.1\n";
            String header2 = "HOST " + host + "\n\n";
            outToServer.writeBytes(header1); // send request to the host
            outToServer.writeBytes(header2); // specify host


            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // read response from the server

            FileWriter writer = new FileWriter(host+".txt"); // initialise the file writer
            BufferedWriter bw = new BufferedWriter(writer); // initialise the buffer writer

            String encoding = "";
            boolean readingBody = false;
            int bytesToRead = 0;

            String line; // initiate string where current line from the response is saved
            while (true) { // read response from server line by line
                try{
                    line = inFromServer.readLine();
                    System.out.println(line);
                    bw.write(line + "\n");

                    if(!readingBody) {
                        if (encoding.equals("")) {
                            if (line.contains("Transfer-Encoding: chunked")) {
                                encoding = "Transfer-Encoding";
                            } else if (line.contains("Content-Length")) {
                                encoding = "Content-Length";
                                int bodySize = Integer.parseInt(line.split(" ")[1]);
                            }
                        }
                    }

                    else{
                        if (encoding.equals("Transfer-Encoding")){
                            if (bytesToRead == 0){
                                bytesToRead = Integer.parseInt(line,16);
                                if (bytesToRead == 0){
                                    break;
                                }
                            }

                            else{
                                bytesToRead -= line.length();
                            }
                        }

                        else{
                            
                        }
                    }

                    if (!readingBody && line.equals("")){
                        readingBody = true;
                    }
                }

                catch (SocketTimeoutException s){
                    bw.close(); // write the buffer at the end of the response
                    break;
                }
            }
        }
    }
}
