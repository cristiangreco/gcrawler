// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import java.net.URL;

/**
 * The gcrawler.
 */
class Main {

    public static void main(String[] args) {
        Crawler.Config config = parse(args);

        if (config == null) {
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(
                new Thread(new ShutdownHook(Thread.currentThread())));

        new Crawler(config).run();
    }

    /**
     * Parses command line flags into a configuration object.
     * It expects an odd number of arguments: several (optional) key-value
     * parameters followed by an url.
     *
     * @param args command line arguments.
     * @return a Crawler configuration.
     * @throws Exception in case of errors.
     */
    private static Crawler.Config parse(String[] args) {
        try {
            if (args.length % 2 == 0) {
                throw new IllegalArgumentException("wrong number of parameters");
            }

            String rootUrl = args[args.length - 1];
            if (!(rootUrl.startsWith("http://") || rootUrl.startsWith("https://"))) {
                rootUrl = "http://" + rootUrl; // be gentle, java.net.URL won't be
            }
            URL url = new URL(rootUrl);

            Crawler.Config config = new Crawler.Config();
            config.setRootUrl(rootUrl);
            config.setHost(url.getHost());

            String[] flags = new String[args.length - 1];
            System.arraycopy(args, 0, flags, 0, flags.length);

            for (int i = 0; i < flags.length; i += 2) {
                switch (flags[i]) {
                    case "--timeout-millis":
                        config.setTimeoutMillis(Integer.parseInt(flags[i + 1]));
                        break;
                    case "--print-errors":
                        config.setPrintErrors(Boolean.parseBoolean(flags[i + 1]));
                        break;
                    case "--halt-on-error":
                        config.setHaltOnError(Boolean.parseBoolean(flags[i + 1]));
                        break;
                    case "--normalize-urls":
                        config.setNormalizeUrls(Boolean.parseBoolean(flags[i + 1]));
                        break;
                    case "--concurrency":
                        config.setConcurrency(Integer.parseInt(flags[i + 1]));
                        break;
                    default:
                        throw new IllegalArgumentException("unknown option " + flags[i]);
                }
            }

            return config;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * A JVM shutdown hook that sends the stop signal to the crawler.
     */
    private static class ShutdownHook implements Runnable {
        private Thread main;

        public ShutdownHook(Thread main) {
            this.main = main;
        }

        @Override
        public void run() {
            main.interrupt();
            try {
                main.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
