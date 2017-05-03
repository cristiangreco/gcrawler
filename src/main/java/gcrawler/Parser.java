// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTML parsing utilities.
 */
class Parser {

    /**
     * Extracts static assets (e.g. images, js, css) from the given
     * document by parsing various tags pointing to external resources,
     * and returns their absolute url.
     *
     * @param doc an HTML document.
     * @return the list of static assets parsed from HTML,
     * or an empty list.
     */
    static List<String> extractAssets(Document doc) {
        List<String> results = new ArrayList<>();

        Elements media = doc.select("[src]");
        for (Element elem : media) {
            results.add(elem.attr("abs:src"));
        }

        Elements imports = doc.select("link[href][rel~=(?i)(icon|stylesheet)]");
        for (Element elem : imports) {
            results.add(elem.attr("abs:href"));
        }

        return filterNullOrEmpty(results);
    }

    /**
     * Extract hyperlinks from the given document by parsing the href
     * attribute of &lt;a&gt; tags, and returns their absolute url.
     *
     * @param doc an HTML document.
     * @return the list of links parsed from HTML,
     * or an empty list.
     */
    static List<String> extractLinks(Document doc) {
        List<String> results = new ArrayList<>();

        Elements links = doc.select("a[href]");
        for (Element elem : links) {
            results.add(elem.attr("abs:href"));
        }

        return filterNullOrEmpty(results);
    }

    /**
     * Given a list of urls, removes null or empty strings.
     *
     * @param urls the urls to be filtered.
     * @return the list of non-null and non-empty urls.
     */
    private static List<String> filterNullOrEmpty(List<String> urls) {
        return urls
                .stream()
                .filter(url -> !(url == null || url.isEmpty()))
                .collect(Collectors.toList());
    }

}
