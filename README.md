# Parallel News Aggregator

A multithreaded Java application that processes news articles from JSON files, removes duplicates, and generates category and language indexes, keyword counts, and summary statistics.

## Features

- Configurable number of worker threads.
- Removal of all articles sharing a UUID or title.
- Grouping by category and language.
- Article listing ordered by publication date.
- English keyword analysis with a configurable list of ignored words.
- Consistent output regardless of the number of workers.

## Build and Run

Requires a JDK (OpenJDK 25 is the reference environment), GNU Make, and the project's JSON parsing dependency configured in the Makefile.

From the repository root:

```bash
cd src
make build
make run ARGS="4 ../checker/input/tests/test_small/articles.txt ../checker/input/tests/test_small/inputs.txt"
```

The entry point is `Main`. The three arguments are:

1. Number of worker threads.
2. Path to `articles.txt`, which lists the JSON files to process.
3. Path to `inputs.txt`, which lists the language, category, and ignored-word files.

Generated text files are written to the current working directory (`src/` in this example).

To remove build artifacts:

```bash
make clean
```

## Input and Output

Each article includes a UUID, title, author, URL, text, publication date, language, and categories.

Both input lists start with the number of files, followed by one file path per line. In `inputs.txt`, the order is `languages.txt`, `categories.txt`, then `english_linking_words.txt`. Each of these configuration files starts with an entry count, followed by one entry per line.

The application produces:

| Output | Contents |
| --- | --- |
| Category and language files | Article UUIDs sorted alphabetically. |
| `all_articles.txt` | Article UUIDs and publication dates, newest first. |
| `keywords_count.txt` | English words and the number of distinct articles containing each word. |
| `reports.txt` | Duplicate counts, remaining article counts, leading authors, languages, categories, and other summary statistics. |

## Parallel Processing

A fixed set of workers shares file reading, duplicate removal, classification, and text analysis. Concurrent collections and atomic counters protect shared data, while barriers synchronize processing stages. Workers build local article lists, and worker 0 combines the results and writes the final reports.

## Testing

From the repository root:

```bash
bash checker/checker.sh test_small  # Quick correctness check
bash checker/checker.sh            # Full test suite
```

The checker compiles the project automatically and compares results against reference files using 1, 2, and 4 workers. It requires Bash, `bc`, and standard Unix utilities.

**Note:** the checker deletes `.txt` files directly inside `src/` before each run. Keep input files elsewhere.

## Performance

Recorded on an Intel Core i7-12700H with approximately 7.6 GiB RAM, running Linux through WSL2:

| Dataset | 1 worker | 2 workers | 4 workers |
| --- | ---: | ---: | ---: |
| `test_1` | 28.71 s | 16.10 s | 12.25 s |
| `test_2` | 58.57 s | 29.85 s | 16.00 s |
| `test_3` | 81.66 s | 44.12 s | 28.13 s |
| `test_4` | 107.10 s | 57.35 s | 32.06 s |
| `test_5` | 134.10 s | 72.32 s | 45.52 s |

Four workers reduced runtime on every dataset, achieving up to **3.66x speedup** over one worker. Synchronization and final aggregation can limit further scaling. These are previously recorded measurements, not results from a new benchmark run.

## Data Source

Article collections are based on the [Webhose / Webz.io free news datasets](https://github.com/Webhose/free-news-datasets).
