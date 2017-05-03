// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A web crawler routine. Implemented as a BFS-like
 * traversal starting from the root url.
 * Handles shutdown gracefully.
 */
class Crawler implements Runnable {

    // Configuration instance.
    private final Config config;

    // Stop signal.
    private volatile boolean stop = false;

    Crawler(Config config) {
        this.config = config;
    }

    /**
     * Main loop. This is a plain BFS with graceful shutdown support
     * and error handling.
     */
    @Override
    public void run() {
        String rootUrl = config.rootUrl;

        Set<String> visited = new HashSet<>();
        List<String> queue = new ArrayList<>();

        visited.add(rootUrl);
        queue.add(rootUrl);

        try {
            Logger.setup();
        } catch (IOException ex) {
            System.err.println("error with logger setup");
            ex.printStackTrace();
            return; // stop now
        }

        while (!(queue.isEmpty() || shouldStop())) {
            String url = queue.remove(0);
            try {
                List<String> links = fetchAndParse(url);
                for (String link : links) {
                    if (!visited.contains(link)) {
                        visited.add(link);
                        queue.add(link);
                    }
                }
            } catch (Exception ex) {
                if (config.printErrors) {
                    System.err.println("error with url: " + url);
                    ex.printStackTrace();
                }
                if (config.haltOnError) {
                    break;
                }
            }
        }

        try {
            Logger.teardown();
        } catch (IOException ex) {
            System.err.println("error with logger teardown");
            ex.printStackTrace();
        }
    }

    /**
     * Sends a stop signal to the crawler.
     */
    public void stop() {
        stop = true;
    }

    /**
     * Checks if the traversal should be stopped.
     *
     * @return true if should stop, false otherwise.
     */
    private boolean shouldStop() {
        return stop || Thread.currentThread().isInterrupted();
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
        return new URL(url).getHost().equals(host);
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
    }

}
