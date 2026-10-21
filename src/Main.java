import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static CyclicBarrier barrier;
    public static void main(String[] args) throws Exception{
        int nrThreads = Integer.parseInt(args[0]);
        String articlesFile = args[1];
        String auxFile = args[2];

        barrier = new CyclicBarrier(nrThreads);

        List<String> articlePaths = new ArrayList<>();
        int N, M;

        try (BufferedReader br = new BufferedReader(new FileReader(articlesFile))) {
            N = Integer.parseInt(br.readLine().trim());

            Path parentDir = Paths.get(auxFile).getParent();

            for (int i = 0; i < N; i++) {
                String path = br.readLine().trim();
                Path resultingPath = parentDir.resolve(path);
                articlePaths.add(resultingPath.toString());
            }
        }

        List<String> auxPaths = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(auxFile))) {
            M = Integer.parseInt(br.readLine().trim());

            Path parentDir = Paths.get(auxFile).getParent();

            for (int i = 0; i < M; i++) {
                String fileName = br.readLine().trim();
                Path resultingPath = parentDir.resolve(fileName);
                auxPaths.add(resultingPath.toString());
            }
        }


        List<List<String>> articlesDividedByThread = new ArrayList<>();

        for (int i = 0; i < nrThreads; i++) {
            articlesDividedByThread.add(new ArrayList<>());
        }

        int total = articlePaths.size();

        for (int i = 0; i < total; i++) {
            int threadIndex = i * nrThreads / total;
            articlesDividedByThread.get(threadIndex).add(articlePaths.get(i));
        }

        Thread threads[] = new Thread[nrThreads];

        ConcurrentHashMap<String, AtomicInteger> countMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, AtomicInteger> countMapTitle = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> categories = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> languages = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> linkingWords = new ConcurrentHashMap<>();
        List<ConcurrentSkipListSet<String>> categorySets = new ArrayList<>();
        List<ConcurrentSkipListSet<String>> languageSets = new ArrayList<>();
        List<List<Article>> articlePart = new ArrayList<>();
        for (int i = 0; i < nrThreads; i++) {
            articlePart.add(new ArrayList<>());
        }

        ConcurrentHashMap<String, AtomicInteger> appearances = new ConcurrentHashMap<>();

        ConcurrentSkipListSet<AbstractMap.SimpleEntry<String, Integer>> resultingAppearances =
                new ConcurrentSkipListSet<>((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getKey().compareTo(b.getKey());
                });
        ConcurrentHashMap<String, AtomicInteger> authors = new ConcurrentHashMap<>();

        ConcurrentSkipListSet<AbstractMap.SimpleEntry<String, Integer>> ordoredAuthors =
                new ConcurrentSkipListSet<>((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getKey().compareTo(b.getKey());
                });
        ConcurrentHashMap<String, Article> finalArticles = new ConcurrentHashMap<>();


        int categoriesCount = 0, languagesCount = 0, wordsCount = 0;


        for(int i = 0; i < M; i++) {
            Path realFile = Paths.get(auxPaths.get(i)).getFileName();
            if(realFile.toString().equals("categories.txt")) {
                try (BufferedReader br = new BufferedReader(new FileReader(auxPaths.get(i)))) {
                    categoriesCount = Integer.parseInt(br.readLine().trim());

                    for(int j = 0; j < categoriesCount; j++) {
                        String category = br.readLine().trim();
                        categories.put(category, j);
                    }
                }
            }

            if(realFile.toString().equals("languages.txt")) {
                try (BufferedReader br = new BufferedReader(new FileReader(auxPaths.get(i)))) {
                    languagesCount = Integer.parseInt(br.readLine().trim());

                    for(int j = 0; j < languagesCount; j++) {
                        String language = br.readLine().trim();
                        languages.put(language, j);
                    }
                }
            }

            if(realFile.toString().equals("english_linking_words.txt")) {
                try (BufferedReader br = new BufferedReader(new FileReader(auxPaths.get(i)))) {
                    wordsCount = Integer.parseInt(br.readLine().trim());

                    for(int j = 0; j < wordsCount; j++) {
                        String linkingWord = br.readLine().trim();
                        linkingWords.put(linkingWord, j);
                    }
                }
            }
        }

        for (int i = 0; i < categories.size(); i++) {
            categorySets.add(new ConcurrentSkipListSet<>());
        }

        for (int i = 0; i < languages.size(); i++) {
            languageSets.add(new ConcurrentSkipListSet<>());
        }

        for (int i = 0; i < nrThreads; i++) {
            Runnable aux = new MyThread(articlesDividedByThread.get(i), countMap, categorySets, categories, languages, linkingWords, i, nrThreads, countMapTitle, languageSets, articlePart, appearances, resultingAppearances, authors, ordoredAuthors, finalArticles);
            threads[i] = new Thread(aux);
        }

        for (int i = 0; i < nrThreads; i++) {
            threads[i].start();
        }

        for (int i = 0; i < nrThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
