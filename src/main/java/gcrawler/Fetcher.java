// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * HTTP download utility.
 */
class Fetcher {

    /**
     * Connects and downloads the resource at the given url
     * by issuing an HTTP GET request.
     *
     * @param url the url of the resource to be downloaded.
     * @param timeoutMillis the socket connect and read timeout (zero means infinite timeout).
     * @return the HTML document at the given url.
     * @throws IOException in case of errors.
     */
    static Document getPage(String url, int timeoutMillis) throws IOException {
        return Jsoup.connect(url).timeout(timeoutMillis).get();
    }

}
