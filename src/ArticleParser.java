import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ArticleParser {

    public static List<Article> parseFile(String filename) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filename)));

        JSONArray array = new JSONArray(content);
        List<Article> result = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            Article a = new Article();
            a.uuid = obj.optString("uuid");
            a.title = obj.optString("title");
            a.author = obj.optString("author");
            a.url = obj.optString("url");
            a.text = obj.optString("text");
            a.published = obj.optString("published");
            a.language = obj.optString("language");

            JSONArray cats = obj.optJSONArray("categories");
            if (cats != null) {
                a.categories = new String[cats.length()];
                for (int j = 0; j < cats.length(); j++)
                    a.categories[j] = cats.getString(j);
            }

            result.add(a);
        }

        return result;
    }
}
