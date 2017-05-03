// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Crawler.class, Logger.class, Fetcher.class, Parser.class})
public class CrawlerTests {

    /**
     * Fetch an url with asset and link.
     */
    @Test
    public void fetchUrl() throws IOException {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenReturn(HtmlDocs.homePage())
                .thenReturn(HtmlDocs.aboutPage());

        new Crawler(newCrawlerConfig()).run();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic();
        Logger.output(eq("https://www.example.org"), argThat(new IsSingleElementList()));

        PowerMockito.verifyStatic();
        Logger.output(eq("https://www.example.org/about"), argThat(new IsSingleElementList()));

        PowerMockito.verifyStatic();
        Logger.teardown();
    }

    /**
     * Cross domain links are skipped.
     */
    @Test
    public void crossDomainLink() throws IOException {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenReturn(HtmlDocs.productsPage());

        new Crawler(newCrawlerConfig()).run();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic();
        Logger.output(anyString(), anyList());

        PowerMockito.verifyStatic();
        Logger.teardown();
    }

    /**
     * By default, do not halt on network error.
     */
    @Test
    public void fetchUrlThenError() throws IOException {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenReturn(HtmlDocs.jobsPage())
                .thenThrow(new IOException("network error"))
                .thenReturn(HtmlDocs.homePage());

        new Crawler(newCrawlerConfig()).run();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic(times(2));
        Logger.output(anyString(), anyList());

        PowerMockito.verifyStatic();
        Logger.teardown();
    }

    /**
     * Halt at first network error.
     */
    @Test
    public void haltOnError() throws IOException {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenReturn(HtmlDocs.jobsPage())
                .thenThrow(new IOException("network error"))
                .thenReturn(HtmlDocs.homePage());

        Crawler.Config config = newCrawlerConfig();
        config.setHaltOnError(true);
        new Crawler(config).run();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic();
        Logger.output(anyString(), anyList());

        PowerMockito.verifyStatic();
        Logger.teardown();
    }

    /**
     * Print errors to stderr.
     */
    @Test
    public void printErrors() throws IOException {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenReturn(HtmlDocs.jobsPage())
                .thenThrow(new IOException("network error"))
                .thenReturn(HtmlDocs.homePage());

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(stderr));

        Crawler.Config config = newCrawlerConfig();
        config.setPrintErrors(true);
        new Crawler(config).run();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic(times(2));
        Logger.output(anyString(), anyList());

        PowerMockito.verifyStatic();
        Logger.teardown();

        Assert.assertThat(stderr.toString(), containsString("network error"));
        System.setErr(null);
    }

    /**
     * Normalization removes trailing '#' fragment.
     */
    @Test
    public void disableUrlNormalization() throws IOException {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenReturn(HtmlDocs.pricingPage())
                .thenReturn(HtmlDocs.pricingPage());


        Crawler.Config config = newCrawlerConfig();
        config.setNormalizeUrls(false);
        new Crawler(config).run();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic(times(2));
        Logger.output(anyString(), anyList());

        PowerMockito.verifyStatic();
        Logger.teardown();
    }

    /**
     * Stopping the crawler is safe.
     */
    @Test
    public void stopOnSignal() throws Exception {
        PowerMockito.mockStatic(Logger.class, Fetcher.class);
        Mockito.when(Fetcher.getPage(anyString(), anyInt()))
                .thenAnswer(invocation -> HtmlDocs.randomLinkPage());

        Crawler crawler = new Crawler(newCrawlerConfig());
        Thread thread = new Thread(crawler);
        thread.start();

        TimeUnit.SECONDS.sleep(1);
        crawler.stop();

        PowerMockito.verifyStatic();
        Logger.setup();

        PowerMockito.verifyStatic(Mockito.atLeastOnce());
        Logger.output(anyString(), anyList());

        PowerMockito.verifyStatic();
        Logger.teardown();
    }

    /**
     * Mock html pages.
     */
    private static class HtmlDocs {
        static Document homePage() {
            return Jsoup.parse(
                    "<html><head><title>Home</title><link href='/style.css' rel='stylesheet'></head><body><a href='/about'>About</a></body></html>",
                    "https://www.example.org"
            );
        }

        static Document aboutPage() {
            return Jsoup.parse(
                    "<html><head><title>About</title><link href='/style.css' rel='stylesheet'></head><body><a href='/'>Home</a></body></html>",
                    "https://www.example.org"
            );
        }

        static Document jobsPage() {
            return Jsoup.parse(
                    "<html><head><title>Jobs</title></head><body><a href='/'>Home</a><a href='/about'>About</a></body></html>",
                    "https://www.example.org"
            );
        }

        static Document pricingPage() {
            return Jsoup.parse(
                    "<html><head><title>Pricing</title></head><body><a href='#'>Pricing</a></body></html>",
                    "https://www.example.org"
            );
        }

        static Document productsPage() {
            return Jsoup.parse(
                    "<html><head><title>Products</title></head><body><a href='http://forum.example.org'>Forum</a></body></html>",
                    "https://www.example.org"
            );
        }

        static Document randomLinkPage() {
            return Jsoup.parse(
                    "<html><head><title>Random</title></head><body><a href='/" + ThreadLocalRandom.current().nextLong() + "'>Link</a></body></html>",
                    "https://www.example.org"
            );
        }
    }

    /**
     * A crawler config for www.example.org.
     */
    private static Crawler.Config newCrawlerConfig() {
        Crawler.Config config = new Crawler.Config();
        config.setRootUrl("https://www.example.org");
        config.setHost("www.example.org");
        return config;
    }

    /**
     * A Mockito matcher for one-element lists.
     */
    static class IsSingleElementList extends ArgumentMatcher<List<String>> {
        @Override
        public boolean matches(Object arg) {
            return ((List<String>) arg).size() == 1;
        }
    }

}
