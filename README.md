gcrawler
===

[![Build Status](https://travis-ci.org/cristiangreco/gcrawler.svg?branch=master)](https://travis-ci.org/cristiangreco/gcrawler)

> :warning: :fire: A pull request will be merged soon that adds [concurrency support](https://github.com/cristiangreco/gcrawler/pull/1). :tada:

gcrawler is a simple (not concurrent) web crawler written in Java.

Given a starting url, it visits every reachable page under that domain, not crossing subdomains.

gcrawler outputs a json to stdout listing every visited page and every static asset (images, js, css) found on that page.

Example output:

```
[
  {
    "url": "http://www.debian.org",
    "assets": [
      "http://www.debian.org/Pics/openlogo-50.png",
      "http://www.debian.org/Pics/identica.png",
      "http://www.debian.org/Pics/planet.png",
      "http://www.debian.org/debhome.css",
      "http://www.debian.org/debian-en.css",
      "http://www.debian.org/favicon.ico"
    ]
  },
  {
    "url": "http://www.debian.org/",
    "assets": [
      "http://www.debian.org/Pics/openlogo-50.png",
      "http://www.debian.org/Pics/identica.png",
      "http://www.debian.org/Pics/planet.png",
      "http://www.debian.org/debhome.css",
      "http://www.debian.org/debian-en.css",
      "http://www.debian.org/favicon.ico"
    ]
  },
  {
    "url": "http://www.debian.org#content",
    "assets": [
      "http://www.debian.org/Pics/openlogo-50.png",
      "http://www.debian.org/Pics/identica.png",
      "http://www.debian.org/Pics/planet.png",
      "http://www.debian.org/debhome.css",
      "http://www.debian.org/debian-en.css",
      "http://www.debian.org/favicon.ico"
    ]
  },
  {
    "url": "http://www.debian.org/intro/about",
    "assets": [
      "http://www.debian.org/Pics/openlogo-50.png",
      "http://www.debian.org/Pics/identica.png",
      "http://www.debian.org/Pics/planet.png",
      "http://www.debian.org/debian.css",
      "http://www.debian.org/debian-en.css"
    ]
  },
  ...
]
```

## How to build

gcrawler uses Gradle as build system. You don't need to install Gradle as it is shipped with sources.

The following will create an executable uber-jar under `build/libs/gcrawler-all.jar`:

```sh
$ ./gradlew clean shadowJar
```

## How to run

Run the gcrawler executable jar passing the starting url as parameter:

```sh
$ java -jar build/libs/gcrawler-all.jar http://www.debian.org
```

gcrawler supports some command line options, see below.

## High level design

gcrawler is written in Java and uses the fantastic [JSoup](https://jsoup.org) library to fetch and parse HTML documents, plus the [Jackson](https://github.com/FasterXML/jackson) json serializer.

The implementation is single-threaded by purpose, but it could easily be parallelized with minor changes.

CPU usage is very low because the main cpu-intensive activity (DOM parsing) happens only in the main thread.

Memory usage is generally low, because documents are not retained after parsing and json results are emitted as soon as possible. In fact, gcrawler uses the Jackson streaming APIs to flush output as soon as documents are parsed.

## Stopping the crawler

gcrawler will try hard to emit a valid json to stdout.

Upon task cancellation (e.g. when hitting ^C) the crawler will stop as soon as possible (i.e. at the end of the currently running task) and will emit a valid json for the pages successfully visited.

## Error handling

Again, gcrawler will try hard to emit a valid json to stdout and, in general, it will avoid to intermingle stacktraces with json output.

Errors are printed to stderr instead.
 
Errors happening before any json character has been emitted (e.g. wrong command line parameters), will be printed to stderr and will cause the application to exit early.

Errors happening while the crawler is running and json is being printed, are muted by default. To enable printing crawling errors to stderr use the `--print-errors true` flag. 

## Command line parameters

gcrawler requires an odd number of command line parameters, e.g.:

```sh
$ java -jar build/libs/gcrawler-all.jar [param1 value1 param2 value2 ...] <url>
```

Each parameter must be followed by its value. The starting url must be passed as last parameter.

flag | values | default | description
---- | ------ | ------- | -----------
`--print-errors` | boolean | false | Print any exception to stderr
`--halt-on-error` | boolean | false | Stop crawling after any error occurs
`--normalize-urls` | boolean | true | Normalize urls before crawling (remove traling `#` and `?`) 
`--timeout-millis` | integer | 5000 | Socket timeout in milliseconds for connect and read operations

For example, the following:

```sh
java -jar build/libs/gcrawler-all.jar --print-errors true --timeout-millis 10000 www.debian.org 2>errors.txt >output.txt
```

will apply a connection timeout of 10 seconds, will print to file `errors.txt` any skipped url and will save the json to `output.txt`. 

## Notes

- In case the starting url returns any error, the empty json list `[]` is printed.
- In case a page does not import any static asset, the empty json list `"assets":[]` is printed.
- gcrawler does not apply any url normalization, except for removing trailing fragment `#` and question mark `?` in order to avoid too much duplicate url. This can be configured with the `--normalize-urls` parameter.
- At the moment, it is possible to configure only the JSoup connection timeout. Anyway JSoup provides sensible defaults: it will follow redirects, will not ignore http errors, will validate certificates, and will handle response body sizes up to 1M.
- gcrawler will cross between http and https without problems, but will refuse to cross between domains, therefore crawling `debian.org` is different than crawling `www.debian.org`. 

## License

The gcrawler source files are distributed under the BSD-style license.
