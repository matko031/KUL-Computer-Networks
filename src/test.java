import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


import org.json.JSONArray;
import org.json.JSONObject;


public class test {

    private static String html = "<!DOCTYPE html> <html lang=\"en\"><head><meta charset=\"UTF-8\"><title>My website</title></head><body>This is text without paragraph <p> This is my website with some text on it</p><p>And this is second paragraph</p><img src=\"my_website/spaceodyssey.png\"></body></html>";

    /*
        Read the the given file and return the string with the content
         */
    public static String readFile(String fileName) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(fileName))); // encapsulate the FileInputStream with a BufferedReader
        String strLine;
        String fileText = "";
        while ( (strLine = reader.readLine()) != null){ // while there is something to read, read new line from the file
            fileText += strLine + "\r\n";
        }
        return fileText; // return the string with content
    }


    public static void writeFile(String filename, String text) throws IOException {
        // overwrite the html file with the adapted one from the sb
        File file = new File(filename);
        FileWriter fr = new FileWriter(file);
        fr.write(text);
        fr.flush();
    }


    private static HttpURLConnection setupHttpURLConnection(String API_KEY, String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        return con;
    }


    public static void main(String[] args) throws IOException {

        String htmlText = readFile("websites/tcpipguide/file.html");
        Document doc = Jsoup.parse(htmlText);

        ArrayList<String> elements= new ArrayList<String>();
        for (Element child : doc.body().getAllElements()) {
            if (!child.ownText().equals("")) {
                elements.add(child.ownText());
            }
        }

        String API_KEY = "trnsl.1.1.20200314T182311Z.d201cdaa9f1c8b73.d7dd33ae0a61a84c693f2b3911de8d09fd5da66e";
        String url = "https://translate.yandex.net/api/v1.5/tr.json/translate?lang=" + "nl" + "&key=" + API_KEY;
        HttpURLConnection con = setupHttpURLConnection(API_KEY, url);
        OutputStream os = con.getOutputStream();

        for (String e : elements ) {
            String textToTranslate = "text=" + e + "&";
            os.write((textToTranslate).getBytes());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        con.disconnect();

        String translation = response.toString();

        JSONObject json = new JSONObject(translation);
        JSONArray text = json.getJSONArray("text");

        int index = 0;
        for (Element child : doc.body().getAllElements()) {
            if (!child.ownText().equals("")) {
                child.text((String)text.get(index));
                index++;
            }
        }
        System.out.println(doc.toString());
    }


}