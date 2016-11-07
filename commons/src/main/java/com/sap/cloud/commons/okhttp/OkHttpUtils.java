/**
 * Copyright 2016 SAP SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sap.cloud.commons.okhttp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import okhttp3.Connection;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpEngine;
import okio.Buffer;
import okio.BufferedSource;

public final class OkHttpUtils {
  private static final Charset UTF8 = Charset.forName("UTF-8");

  public static StringBuilder parseRequest(final Request request, final Connection connection) throws IOException {

    final RequestBody requestBody = request.body();
    final boolean hasRequestBody = requestBody != null;

    final Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;

    final StringBuilder requestStrBldr = new StringBuilder();

    requestStrBldr.append("--> ").append(request.method()).append(" ").append(request.url()).append(" ")
        .append(protocol).append("\n");

    final okhttp3.Headers requestHeaders = request.headers();

    if (hasRequestBody) {
      // Request body headers are only present when installed as a network interceptor. Force
      // them to be included (when available) so there values are known.
      if (requestBody.contentType() != null) {
        requestStrBldr.append("Content-Type: ").append(requestBody.contentType()).append("\n");
      }
      if (requestBody.contentLength() != -1) {
        requestStrBldr.append("Content-Length: ").append(requestBody.contentLength()).append("\n");
      }
    }

    requestHeaders.toMultimap().forEach((key, value) -> {
      if (!"Content-Type".equalsIgnoreCase(key) && !"Content-Length".equalsIgnoreCase(key)) {
        requestStrBldr.append(key).append(": ");
        if (value != null) {
          value.forEach(v -> {
            requestStrBldr.append(v).append(" ");
          });
        }
        requestStrBldr.append("\n");
      }
    });

    if (!hasRequestBody) {
      requestStrBldr.append("--> END ").append(request.method());
    } else if (bodyEncoded(request.headers())) {
      requestStrBldr.append("--> END ").append(request.method()).append(" (encoded body omitted)");
    } else {
      final Buffer buffer = new Buffer();
      requestBody.writeTo(buffer);

      Charset charset = UTF8;
      final MediaType contentType = requestBody.contentType();
      if (contentType != null) {
        charset = contentType.charset(UTF8);
      }

      requestStrBldr.append(buffer.readString(charset)).append("\n");
      requestStrBldr.append("--> END ").append(request.method()).append(" (").append(requestBody.contentLength())
          .append("-byte body)");
    }
    return requestStrBldr;
  };

  public static StringBuilder parseResponse(final Response response, final long tookMs, final ResponseBody responseBody)
      throws IOException {
    final StringBuilder responseStrBldr = new StringBuilder();
    final long contentLength = responseBody.contentLength();
    final String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
    responseStrBldr.append("<-- ").append(response.code()).append(" ").append(response.message()).append(" ")
        .append(response.request().url()).append(" (").append(tookMs).append("ms, ").append(bodySize).append(" body)")
        .append("\n");

    final okhttp3.Headers responseHeaders = response.headers();

    responseHeaders.toMultimap().forEach((key, value) -> {
      responseStrBldr.append(key).append(": ");
      if (value != null) {
        value.forEach(v -> {
          responseStrBldr.append(v).append(" ");
        });
      }
      responseStrBldr.append("\n");
    });

    if (!HttpEngine.hasBody(response)) {
      responseStrBldr.append("<-- END HTTP");
    } else if (bodyEncoded(response.headers())) {
      responseStrBldr.append("<-- END HTTP (encoded body omitted)");
    } else {
      final BufferedSource source = responseBody.source();
      source.request(Long.MAX_VALUE); // Buffer the entire body.
      final Buffer buffer = source.buffer();

      Charset charset = UTF8;
      final MediaType contentType = responseBody.contentType();

      try {
        if (contentType != null) {
          charset = contentType.charset(UTF8);
        }
        if (contentLength != 0) {
          responseStrBldr.append("\n").append(buffer.clone().readString(charset)).append("\n");
        }

        responseStrBldr.append("<-- END HTTP (").append(buffer.size()).append("-byte body)");
      } catch (final UnsupportedCharsetException e) {
        responseStrBldr.append("Couldn't decode the response body; charset is likely malformed.").append("\n");
        responseStrBldr.append("<-- END HTTP");
      }
    }
    return responseStrBldr;
  }

  private static boolean bodyEncoded(final okhttp3.Headers headers) {
    final String contentEncoding = headers.get("Content-Encoding");
    return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
  }

  public static StringBuilder parseResponse(final Response response, final long tookMs) throws IOException {
    return parseResponse(response, tookMs, response.body());
  }
}
