// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * A web crawler routine. Implemented as a BFS-like
 * traversal starting from the root url.
 * Handles shutdown gracefully.
 */
class Crawler implements Runnable {

    // Configuration instance.
    private final Config config;

    // Set of visited urls.
    private ConcurrentMap<String, Boolean> visited = new ConcurrentHashMap<>();

    // A self-reference to handle interruption.
    private Thread self = Thread.currentThread();

    Crawler(Config config) {
        this.config = config;
    }

    /**
     * Main crawling loop. Supports graceful shutdown and error handling.
     */
    @Override
    public void run() {
        String rootUrl = config.rootUrl;

        ForkJoinPool pool = new ForkJoinPool(config.concurrency);

        visited.put(rootUrl, true);

        try {
            Logger.setup();
        } catch (IOException ex) {
            handleError("error with logger setup", ex);
            return; // stop now
        }

        try {
            pool.submit(new CrawlingAction(rootUrl)).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            handleError("task execution error", ex);
        }

        pool.shutdownNow();

        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        try {
            Logger.teardown();
        } catch (IOException ex) {
            handleError("error with logger teardown", ex);
        }
    }

    /**
     * Crawling action. After fetching the given url,
     * will fork other actions to crawl links.
     */
    private class CrawlingAction extends RecursiveAction {

        private String url;

        CrawlingAction(String url) {
            this.url = url;
        }

        @Override
        protected void compute() {
            if (isCancelled()) {
                return;
            }

            List<CrawlingAction> tasks = new ArrayList<>();

            try {
                List<String> links = fetchAndParse(url);
                for (String link : links) {
                    if (visited.putIfAbsent(link, true) == null) {
                        tasks.add(new CrawlingAction(link));
                    }
                }
            } catch (Exception ex) {
                handleUrlError(url, ex);
                if (config.haltOnError) {
                    self.interrupt();
                    return;
                }
            }

            if (!tasks.isEmpty()) {
                invokeAll(tasks);
            }
        }
    }

    /**
     * Work routine. Fetches the HTML document at the given url,
     * parses assets and links, prints to stdout, returns a
     * list of links from the same domain.
     *
     * @param url the url to be fetched and parsed.
     * @return list of links to pages on the same domain.
     * @throws Exception in case of processing errors.
     */
    private List<String> fetchAndParse(String url) throws Exception {
        url = config.normalizeUrls ? normalizeUrl(url) : url;
        Document doc = Fetcher.getPage(url, config.timeoutMillis);

        Logger.output(url, Parser.extractAssets(doc));

        List<String> links = Parser.extractLinks(doc);
        links = filterHostDomain(links, config.host);
        links = normalizeUrls(links);

        return links;
    }

    /**
     * Given a list of urls, removes the entries with an host part different from the host
     * provided.
     *
     * @param urls the urls to be filtered.
     * @param host the host domain to be compared.
     * @return the list of urls matching the given host, or an empty list.
     * @throws Exception in case of errors.
     */
    private List<String> filterHostDomain(List<String> urls, String host) throws Exception {
        for (ListIterator<String> it = urls.listIterator(); it.hasNext();) {
            if (!checkHostDomain(it.next(), host)) {
                it.remove();
            }
        }
        return urls;
    }

    /**
     * Checks whether the host part of given url is the same as the host provided.
     *
     * @param url the url to be checked.
     * @param host the host domain to be compared.
     * @return true if url has the host part equals to the given host, false otherwise.
     * @throws Exception in case of malformed url.
     */
    private boolean checkHostDomain(String url, String host) throws Exception {
        try {
            return new URL(url).getHost().equals(host);
        } catch (MalformedURLException ex) {
            handleUrlError(url, ex);
            if (config.haltOnError) {
                throw ex;
            }
            return false;
        }
    }

    /**
     * Normalizes a list of urls.
     *
     * @param urls the urls to be normalized.
     * @return the urls after normalization.
     */
    private List<String> normalizeUrls(List<String> urls) {
        return urls
            .stream()
            .map(url -> config.normalizeUrls ? normalizeUrl(url) : url)
            .collect(Collectors.toList());
    }

    /**
     * Applies very basic url normalization, actually by just removing trailing
     * fragment and question mark, if any.
     *
     * @param url the url to be normalized.
     * @return the url after normalization.
     */
    private String normalizeUrl(String url) {
        if ((url.endsWith("#") || url.endsWith("?")) && url.length() > 1) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Prints message and exception to stderr, if logging
     * errors is enabled.
     *
     * @param msg the error message to be printed.
     * @param ex the exception to be logged.
     */
    private void handleError(String msg, Exception ex) {
        if (config.printErrors) {
            System.err.println(msg);
            ex.printStackTrace();
        }
    }

    /**
     * Convenience method for printing error messages
     * about crawled urls.
     *
     * @param url the url that caused the exception.
     * @param ex the exception to be logged.
     */
    private void handleUrlError(String url, Exception ex) {
        handleError("error with url: " + url, ex);
    }

    /**
     * Configuration options for crawler.
     */
    static class Config {

        // Starting url.
        private String rootUrl;

        // Host of the starting url (for cross-domain check).
        private String host;

        // Socket connect and read timeout.
        private int timeoutMillis = 5_000;

        // Whether errors should be printed or not (to stderr).
        private boolean printErrors = false;

        // Whether to halt traversal after first error.
        private boolean haltOnError = false;

        // Whether to normalize urls before access.
        private boolean normalizeUrls = true;

        // Number of concurrent threads.
        private int concurrency = Runtime.getRuntime().availableProcessors();

        public String getRootUrl() {
            return rootUrl;
        }

        public void setRootUrl(String rootUrl) {
            this.rootUrl = rootUrl;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public boolean isPrintErrors() {
            return printErrors;
        }

        public void setPrintErrors(boolean printErrors) {
            this.printErrors = printErrors;
        }

        public boolean isHaltOnError() {
            return haltOnError;
        }

        public void setHaltOnError(boolean haltOnError) {
            this.haltOnError = haltOnError;
        }

        public boolean isNormalizeUrls() {
            return normalizeUrls;
        }

        public void setNormalizeUrls(boolean normalizeUrls) {
            this.normalizeUrls = normalizeUrls;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

    }

}
