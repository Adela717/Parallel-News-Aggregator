import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class MyThread implements Runnable {

    private final List<String> filesToProcess;
    ConcurrentHashMap<String, AtomicInteger> countMap;
    ConcurrentHashMap<String, AtomicInteger> countMapTitle;
    List<ConcurrentSkipListSet<String>> categorySets;
    ConcurrentHashMap<String, Integer> categories;
    ConcurrentHashMap<String, Integer> languages;
    ConcurrentHashMap<String, Integer> linkingWords;
    List<ConcurrentSkipListSet<String>> languageSets = new ArrayList<>();
    int threadID;
    int nrThreads;
    List<List<Article>> articlePart;
    ConcurrentHashMap<String, AtomicInteger> appearances;
    ConcurrentSkipListSet<AbstractMap.SimpleEntry<String, Integer>> resultingAppearances;
    static AtomicInteger duplicates = new AtomicInteger(0);
    ConcurrentHashMap<String, AtomicInteger> authors;
    ConcurrentSkipListSet<AbstractMap.SimpleEntry<String, Integer>> ordoredAuthors;
    static ConcurrentHashMap<String, AtomicInteger> languagesAppearances = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, AtomicInteger> categoriesAppearances = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Article> finalArticles;

    public MyThread(List<String> filesToProcess, ConcurrentHashMap<String, AtomicInteger> countMap, List<ConcurrentSkipListSet<String>> categorySets, ConcurrentHashMap<String, Integer> categories, ConcurrentHashMap<String, Integer> languages, ConcurrentHashMap<String, Integer> linkingWords, int threadID, int nrThreads,ConcurrentHashMap<String, AtomicInteger> countMapTitle, List<ConcurrentSkipListSet<String>> languageSets, List<List<Article>> articlePart, ConcurrentHashMap<String, AtomicInteger> appearances, ConcurrentSkipListSet<AbstractMap.SimpleEntry<String, Integer>> resultingAppearances, ConcurrentHashMap<String, AtomicInteger> authors, ConcurrentSkipListSet<AbstractMap.SimpleEntry<String, Integer>> ordoredAuthors, ConcurrentHashMap<String, Article> finalArticles) {
        this.filesToProcess = filesToProcess;
        this.countMap = countMap;
        this.categorySets= categorySets;
        this.categories = categories;
        this.languages = languages;
        this.linkingWords = linkingWords;
        this.threadID = threadID;
        this.nrThreads = nrThreads;
        this.countMapTitle = countMapTitle;
        this.languageSets = languageSets;
        this.articlePart = articlePart;
        this.appearances = appearances;
        this.resultingAppearances = resultingAppearances;
        this.authors = authors;
        this.ordoredAuthors = ordoredAuthors;
        this.finalArticles = finalArticles;
    }

    @Override
    public void run() {
        List<String> languageKeys = new ArrayList<>(languages.keySet());
        List<String> categoryKeys = new ArrayList<>(categories.keySet());

        if(threadID == 0) {
            for(int i = 0; i < languages.size(); i++) {
                String key = languageKeys.get(i);
                languagesAppearances.put(key,new AtomicInteger(0));
            }

            for(int i = 0; i < categories.size(); i++) {
                String key = categoryKeys.get(i);
                categoriesAppearances.put(key,new AtomicInteger(0));
            }
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        for (String file : filesToProcess) {
            try {
                List<Article> articles = ArticleParser.parseFile(file);

                for (Article a : articles) {
                    String key = a.uuid;
                    countMap.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();

                    if(finalArticles.containsKey(key)) {
                        duplicates.incrementAndGet();
                    }
                    finalArticles.putIfAbsent(key, a);

                    String key2 = a.title;
                    countMapTitle.computeIfAbsent(key2, k -> new AtomicInteger(0)).incrementAndGet();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Map.Entry<String, Article>> entries3 = new ArrayList<>(finalArticles.entrySet());

        int total3 = entries3.size();
        int start3 = threadID * total3 / nrThreads;
        int end3   = Math.min((threadID + 1) * total3 / nrThreads, total3);

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = start3; i < end3; i++) {
            Map.Entry<String, Article> entry = entries3.get(i);
            Article article = entry.getValue();
            String key = article.uuid;

            // daca value > 1, uuid-uri egale
            int value = countMap.get(key).get();
            String title = article.title;

            int titleCount = countMapTitle.get(title).get();
            if(value > 1 || titleCount > 1) {
                finalArticles.remove(key);
                duplicates.incrementAndGet();
            }
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Map.Entry<String, Article>> entries2 = new ArrayList<>(finalArticles.entrySet());

        int total2 = entries2.size();
        int start2 = threadID * total2 / nrThreads;
        int end2   = Math.min((threadID + 1) * total2 / nrThreads, total2);

        for (int i = start2; i < end2; i++) {
            Map.Entry<String, Article> entry = entries2.get(i);
            Article article = entry.getValue();

            for (String category : article.categories) {
                if(categories.containsKey(category)) {
                    int index = categories.get(category).intValue();
                    if(!categorySets.get(index).contains(article.uuid)) {
                        categorySets.get(index).add(article.uuid);
                        categoriesAppearances.get(category).incrementAndGet();
                    }

                }
            }

            String language = article.language;
            if(languages.containsKey(language)) {
                int index = languages.get(language).intValue();
                languageSets.get(index).add(article.uuid);

                languagesAppearances.get(language).incrementAndGet();
            }
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        categoryKeys = new ArrayList<>(categories.keySet());
        int total = categoryKeys.size();

        int start = threadID * total / nrThreads;
        int end = Math.min(total, (threadID + 1) * total / nrThreads);

        for (int i = start; i < end; i++) {
            String key = categoryKeys.get(i);
            Integer index = categories.get(key);

            String noCommas = key.replace(",", "");
            String fileToWrite = noCommas.trim().replaceAll("\\s+", "_") + ".txt";

            Path outputPath = Paths.get(fileToWrite);

            if(!categorySets.get(index).isEmpty()) {
                try (BufferedWriter writer = Files.newBufferedWriter(outputPath, CREATE, TRUNCATE_EXISTING)) {
                    for (String uuid : categorySets.get(index)) {
                        writer.write(uuid);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        languageKeys = new ArrayList<>(languages.keySet());

        total = languageKeys.size();

        start = threadID * total / nrThreads;
        end = Math.min(total, (threadID + 1) * total / nrThreads);

        for (int i = start; i < end; i++) {
            String key = languageKeys.get(i);
            Integer index = languages.get(key);

            String fileToWrite = key + ".txt";

            Path outputPath = Paths.get(fileToWrite);

            if(!languageSets.get(index).isEmpty()) {
                try (BufferedWriter writer = Files.newBufferedWriter(outputPath, CREATE, TRUNCATE_EXISTING)) {
                    for (String uuid : languageSets.get(index)) {
                        writer.write(uuid);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        total = total2;
        start = threadID * total / nrThreads;
        end   = (threadID + 1) * total / nrThreads;

        int uniqueArticles = total;

        for (int i = start; i < end; i++) {
            Map.Entry<String, Article> entry = entries2.get(i);
            Article art = entry.getValue();

            articlePart.get(threadID).add(art);
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Article> merged = new ArrayList<>();
        if(threadID == 0) {
            for (List<Article> bucket : articlePart) {
                merged.addAll(bucket);
            }

            merged.sort((a, b) -> {
                int cmp = -a.published.compareTo(b.published);
                return (cmp != 0) ? cmp : a.uuid.compareTo(b.uuid);
            });

            String fileToWrite = "all_articles.txt";
            Path outputPath = Paths.get(fileToWrite);

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, CREATE, TRUNCATE_EXISTING)) {
                for (Article art : merged) {
                    writer.write(art.uuid + " " + art.published);
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        for (int i = start; i < end; i++) {
            Map.Entry<String, Article> entry = entries2.get(i);
            Article art = entry.getValue();
            ConcurrentHashMap<String, Integer> localAppearances = new ConcurrentHashMap<>();

            String text = art.text;
            String lowerCaseText = text.toLowerCase();
            String[] words = lowerCaseText.split("\\s+");

            if(art.language.equalsIgnoreCase("english")) {
                for (String s : words) {
                    String cleanWord = s.replaceAll("[^a-z]", "");
                    if (!linkingWords.containsKey(cleanWord) && !cleanWord.isEmpty()) {
                        if (!localAppearances.containsKey(cleanWord)) {
                            appearances.computeIfAbsent(cleanWord, k -> new AtomicInteger(0))
                                    .incrementAndGet();
                        }
                        localAppearances.putIfAbsent(cleanWord, 1);
                    }
                }
            }
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Map.Entry<String, AtomicInteger>> globalAppearances = new ArrayList<>(appearances.entrySet());

        total = globalAppearances.size();
        start = threadID * total / nrThreads;
        end   = (threadID + 1) * total / nrThreads;

        for (int i = start; i < end; i++) {
            Map.Entry<String, AtomicInteger> app = globalAppearances.get(i);
            AtomicInteger at = app.getValue();
            String s = app.getKey();

            AbstractMap.SimpleEntry<String, Integer> pair = new AbstractMap.SimpleEntry<String, Integer>(s, at.get());

            resultingAppearances.add(pair);
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        if(threadID == 0) {
            String fileToWrite = "keywords_count.txt";
            Path outputPath = Paths.get(fileToWrite);

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (AbstractMap.SimpleEntry<String, Integer> pair : resultingAppearances) {
                    writer.write(pair.getKey() + " " + pair.getValue().toString());
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        total = total2;
        start = threadID * total / nrThreads;
        end   = (threadID + 1) * total / nrThreads;

        for(int i = start; i < end; i++) {
            Map.Entry<String, Article> entry = entries2.get(i);
            Article art = entry.getValue();

            String auth = art.author;
            authors.computeIfAbsent(auth, k -> new AtomicInteger(0)).incrementAndGet();
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Map.Entry<String, AtomicInteger>> authList = new ArrayList<>(authors.entrySet());

        total = authList.size();
        start = threadID * total / nrThreads;
        end   = (threadID + 1) * total / nrThreads;

        for (int i = start; i < end; i++) {
            Map.Entry<String, AtomicInteger> au = authList.get(i);
            AtomicInteger at = au.getValue();
            String s = au.getKey();

            AbstractMap.SimpleEntry<String, Integer> pair = new AbstractMap.SimpleEntry<String, Integer>(s, at.get());

            ordoredAuthors.add(pair);
        }

        try {
            Main.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        if(threadID == 0) {
            String fileToWrite = "reports.txt";

            Path outputPath = Paths.get(fileToWrite);

            AbstractMap.SimpleEntry<String, Integer> first = ordoredAuthors.first();
            String auth = first.getKey();
            int nr = first.getValue();

            int mostLanguages = 0;
            String bestLanguage = "";

            for (Map.Entry<String, AtomicInteger> entry : languagesAppearances.entrySet()) {
                String lg = entry.getKey();
                Integer appears = entry.getValue().get();

                if(appears == mostLanguages) {
                    if(bestLanguage.compareTo(lg) > 0) {
                        bestLanguage = lg;
                    }
                }

                if(appears > mostLanguages) {
                    mostLanguages = appears;
                    bestLanguage = lg;
                }
            }

            int bestCategoryCount = 0;
            String bestCategory = "";

            for (Map.Entry<String, AtomicInteger> entry : categoriesAppearances.entrySet()) {
                String ct = entry.getKey();
                int appears = entry.getValue().get();

                if(appears == bestCategoryCount) {
                    if(bestCategory.compareTo(ct) > 0) {
                        bestCategory = ct;
                    }
                }

                if(appears > bestCategoryCount) {
                    bestCategoryCount = appears;
                    bestCategory = ct;
                }
            }

            Article mostRecentArticle = merged.get(0);


            AbstractMap.SimpleEntry<String, Integer> pair = resultingAppearances.first();

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                writer.write("duplicates_found - " + duplicates.get());
                writer.newLine();

                writer.write("unique_articles - " + uniqueArticles);
                writer.newLine();

                writer.write("best_author - " + auth + " " + nr);
                writer.newLine();

                writer.write("top_language - " + bestLanguage + " " + mostLanguages);
                writer.newLine();

                String noCommas = bestCategory.replace(",", "");
                String bestCategoryFinal =noCommas.trim().replaceAll("\\s+", "_");

                writer.write("top_category - " + bestCategoryFinal + " " + bestCategoryCount);
                writer.newLine();

                writer.write("most_recent_article - " + mostRecentArticle.published + " " + mostRecentArticle.url);
                writer.newLine();

                writer.write("top_keyword_en - " + pair.getKey() + " " + pair.getValue().toString());
                writer.newLine();


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
