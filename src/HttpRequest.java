import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequest {

    private static List<String> HTTPCommands = new ArrayList<String> (Arrays.asList("GET", "POST", "PUT", "HEAD")); // define all supported http commands;
    private static List<String> requiresBody = new ArrayList<String>((Arrays.asList("POST", "PUT")));

    private static String URIPatternString = "^(https?://)?((((www)\\.)?((.*?)\\.)+?(.+?))|localhost)(/.*)?$"; // regex to test if the URI is valid, not 100% waterproof
    private static Pattern URIPatternRegex = Pattern.compile(URIPatternString); // compile regex

    private static String isIntPatternString = "\\d+"; //regex to test if the given port is an int
    private static Pattern isIntRegex = Pattern.compile(isIntPatternString); // compile regex

    private String HTTPCommand;
    private String host; //  host typed by the user
    private String hostDir;
    private String path; // path to the file specified by the user

    private String port; // port typed by the user, default value of 80
    private String lang; // language specified by the user, default en
    private String body;

    private boolean argumentsCorrect;



    HttpRequest() throws IOException {
        setArguments( readArguments() );
    }

    HttpRequest(String host, String command, String path) throws IOException {
        setArguments(new String[] {command, host+path});
    }

    String getHTTPCommand(){
        return HTTPCommand;
    }

    String getHost(){
        return host;
    }

    String getHostDir(){
        return hostDir;
    }

    String getPath(){
        if(path.equals("/") || path.equals("")){
            return "/file.html";
        }
        return path;
    }

    String getPort(){
        return port;
    }

    String getLang(){
        return lang;
    }

    String getBody() {
        return body;
    }

    /*
    Takes in the list of arguments and check if they are all valid
    The order of arguments is HTTPCommand - URI - port - language
    Returns a boolean
     */
    boolean checkArguments(String[] arguments){
        if(arguments.length < 2 || arguments.length > 4){
            System.out.println("Wrong number of arguments"); // port and language are optional, therefore valid number of arguments is two, three or four
            return false;
        }

        String HTTPcommand = arguments[0];
        if( ! HTTPCommands.contains(HTTPcommand)){
            System.out.println("Wrong http command"); // checks if a supported http command is given
            return false;
        }

        Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
        if( ! uriMatcher.matches() && ! arguments[1].equals("localhost")){
            System.out.println("Unsupported URI"); // checks if the URI matches the expected URI pattern
            return false;
        }

        if (arguments.length > 2) {
            String port = arguments[2];
            Matcher portMatcher = isIntRegex.matcher(port);  // initialise a matcher that will check if the given port is an int
            if (!portMatcher.matches()) {
                System.out.println("Port must be an integer"); // checks if the given port is an int
                return false;
            }
        }
        return true;
    }

    /*
    Prompts the user for the argument and returns them as a list of strings
     */
    String[] readArguments(){
        System.out.println("Put your request in this format: HTTPcommand URI port(default: 80) lang(default: en)");
        Scanner scanner = new Scanner(System.in); // create scanner object
        String command = scanner.nextLine();  // read user input
        return command.split("\\s+"); // split user input on every space and return the array
    }

    String getUserBody() {
        System.out.println("Put your post body underneath:");
        Scanner scanner = new Scanner(System.in); // create scanner object
        return scanner.nextLine();  // read user input
    }


    /*
    Reads the arguments, checks them and saves them in their respective variables
     */
    public void setArguments(String[] arguments) throws IOException {
        if (this.checkArguments(arguments)) {
            this.HTTPCommand = arguments[0];

            Matcher uriMatcher = URIPatternRegex.matcher(arguments[1]);  // create a matcher that will match the given URI against the pattern
            uriMatcher.find(); // apply the Matcher to the URI and extract the host and file path groups
            // save found groups in their respective variables
            host = uriMatcher.group(2);
            path = uriMatcher.group(9) == null ? "/" : uriMatcher.group(9);
            hostDir = uriMatcher.group(7) == null ? "localhost" : uriMatcher.group(7);

            this.port = arguments.length > 2 ? arguments[2] : "80"; // if port is given save it to its respective variable
            this.lang = arguments.length > 3 ? arguments[3] : "en"; // same for language

            if (requiresBody.contains(HTTPCommand)){
                body = getUserBody();
            }

            this.argumentsCorrect = true;
        }

        else {
            this.argumentsCorrect = false;
        }
    }


    public String constructRequest(){
        String headers1 = HTTPCommand + " " + path + " HTTP/1.1\r\n" +
                "host:  " + host + "\r\n";
        String headers2 = "\r\n";

        if (requiresBody.contains(HTTPCommand)) {
            int bodySize = body.getBytes().length;
            headers2 = "Content-Length: " + bodySize + "\r\n" +
                    "Content-Type: text/txt\r\n" +
                    "\r\n" + body;
        }
        return headers1+headers2;
    }
}
