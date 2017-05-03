// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Integration tests require network access.
 * We use the https://httpbin.org service to verify crawler's behaviour and output.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Logger.class)
public class CrawlerIntegrationTests {

    /**
     * Fetch an url without assets nor links.
     */
    @Test
    public void fetchUrl() throws IOException {
        PowerMockito.mockStatic(Logger.class);

        new Crawler(newHttpbinConfig("https://httpbin.org/html")).run();

        PowerMockito.verifyStatic();
        Logger.output(eq("https://httpbin.org/html"), argThat(new IsEmptyList()));
    }

    /**
     * Fetch and url with some links.
     */
    @Test
    public void urlWithLinks() throws IOException {
        PowerMockito.mockStatic(Logger.class);

        new Crawler(newHttpbinConfig("https://httpbin.org/links/10/0")).run();

        PowerMockito.verifyStatic(times(10));
        Logger.output(anyString(), argThat(new IsEmptyList()));
    }

    /**
     * Error on some common http 4xx and 5xx codes.
     */
    @Test
    public void httpErrorCodes() throws IOException {
        for (int code : new int[] {400, 401, 403, 404, 500, 502, 503, 504}) {
            PowerMockito.mockStatic(Logger.class);

            new Crawler(newHttpbinConfig("https://httpbin.org/status/" + code)).run();

            PowerMockito.verifyStatic(never());
            Logger.output(anyString(), anyList());
        }
    }

    /**
     * Honor socket read timeout.
     */
    @Test
    public void timeout() throws IOException {
        PowerMockito.mockStatic(Logger.class);

        Crawler.Config config = newHttpbinConfig("https://httpbin.org/delay/3");
        config.setTimeoutMillis(1_000);

        new Crawler(config).run();

        PowerMockito.verifyStatic(never());
        Logger.output(anyString(), anyList());
    }

    /**
     * Follow 301 redirect.
     */
    @Test
    public void follow301() throws IOException {
        PowerMockito.mockStatic(Logger.class);

        new Crawler(newHttpbinConfig("https://httpbin.org/redirect-to?url=https://httpbin.org/html&status_code=301")).run();

        PowerMockito.verifyStatic();
        Logger.output(anyString(), argThat(new IsEmptyList()));
    }

    /**
     * Follow 302 redirect.
     */
    @Test
    public void follow302() throws IOException {
        PowerMockito.mockStatic(Logger.class);

        new Crawler(newHttpbinConfig("https://httpbin.org/redirect-to?url=https://httpbin.org/html")).run();

        PowerMockito.verifyStatic();
        Logger.output(anyString(), argThat(new IsEmptyList()));
    }

    /**
     * Error on too many redirects.
     */
    @Test
    public void redirectLoop() throws IOException {
        PowerMockito.mockStatic(Logger.class);

        new Crawler(newHttpbinConfig("https://httpbin.org/redirect/50")).run();

        PowerMockito.verifyStatic(never());
        Logger.output(anyString(), anyList());
    }

    /**
     * Create a customized Config for httpbin.org urls.
     * Socket timeout should be high enough to avoid flaky tests,
     * and is overridden when needed.
     */
    private static Crawler.Config newHttpbinConfig(String rootUrl) {
        Crawler.Config config = new Crawler.Config();
        config.setRootUrl(rootUrl);
        config.setHost("httpbin.org");
        config.setTimeoutMillis(10_000);
        return config;
    }

    /**
     * A Mockito matcher for empty lists.
     */
    static class IsEmptyList extends ArgumentMatcher<List<String>> {
        @Override
        public boolean matches(Object arg) {
            return ((List<String>) arg).isEmpty();
        }
    }
}
