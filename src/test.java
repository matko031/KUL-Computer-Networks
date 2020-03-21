import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
//import org.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class test {

    private static String html = "<!DOCTYPE html> <html lang=\"en\"><head><meta charset=\"UTF-8\"><title>My website</title></head><body><p> This is my website with some text on it</p><img src=\"my_website/spaceodyssey.png\"></body></html>";


    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.parse(html);
        System.out.println(doc.body().text());

        String API_KEY = "trnsl.1.1.20200314T182311Z.d201cdaa9f1c8b73.d7dd33ae0a61a84c693f2b3911de8d09fd5da66e";
        URL obj = new URL("https://translate.yandex.net/api/v1.5/tr.json/translate?&lang=en-hr&key="+API_KEY);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        String text = "text=" + doc.body().text();
        os.write(text.getBytes());
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }

        System.out.println(response.toString());
        //Document res = Jsoup.parse(response.toString());
        //System.out.println(res.body().text());
    }
}