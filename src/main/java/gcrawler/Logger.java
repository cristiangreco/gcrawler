// Copyright 2017 Cristian Greco <cristian@regolo.cc>
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package gcrawler;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Logging utility. Outputs a json stream to stdout.
 */
class Logger {

    private static JsonGenerator jsonGenerator;

    /**
     * Prepares the json streamer.
     */
    static void setup() throws IOException {
        jsonGenerator = new JsonFactory().createGenerator(System.out, JsonEncoding.UTF8);
        jsonGenerator.setCodec(new ObjectMapper());
        jsonGenerator.useDefaultPrettyPrinter();
        jsonGenerator.writeStartArray();
    }

    /**
     * Emit a json object for the given parameters.
     *
     * @param url the url of the page to be printed.
     * @param assets the list of assets of the page to be printed.
     */
    static void output(String url, List<String> assets) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("url", url);
        jsonGenerator.writeObjectField("assets", assets);
        jsonGenerator.writeEndObject();
    }

    /**
     * Emits the closing character and releases resources.
     */
    static void teardown() throws IOException {
        jsonGenerator.writeEndArray();
        jsonGenerator.close();
    }

}
