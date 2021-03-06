package org.mockserver.integration.server;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.http.ApacheHttpClient;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.model.*;
import sun.misc.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockserver.configuration.SystemProperties.bufferSize;
import static org.mockserver.configuration.SystemProperties.maxTimeout;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.ParameterBody.params;
import static org.mockserver.model.StringBody.*;

/**
 * @author jamesdbloom
 */
public abstract class AbstractClientServerIntegrationTest {

    protected static MockServerClient mockServerClient;
    protected static String servletContext = "";
    private final ApacheHttpClient apacheHttpClient;

    public AbstractClientServerIntegrationTest() {
        bufferSize(1024);
        maxTimeout(TimeUnit.SECONDS.toMillis(10));
        apacheHttpClient = new ApacheHttpClient(true);
    }

    public abstract int getMockServerPort();

    public abstract int getMockServerSecurePort();

    public abstract int getTestServerPort();

    public abstract int getTestServerSecurePort();

    @Before
    public void resetServer() {
        mockServerClient.reset();
    }

    @Test
    public void clientCanCallServerForSimpleResponse() {
        // when
        mockServerClient.when(request()).respond(response().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : ""))
                )
        );
    }

    @Test
    public void clientCanCallServerForForwardInHTTP() {
        // when
        mockServerClient
                .when(
                        request()
                                .withPath("/test_headers_and_body")
                )
                .forward(
                        forward()
                                .withHost("127.0.0.1")
                                .withPort(getTestServerPort())
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header("X-Test", "test_headers_and_body"),
                                new Header("Content-Type", "text/plain")
                        )
                        .withBody("an_example_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header("X-Test", "test_headers_and_body"),
                                new Header("Content-Type", "text/plain")
                        )
                        .withBody("an_example_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
    }

    @Test
    public void clientCanCallServerForForwardInHTTPS() {
        // when
        mockServerClient
                .when(
                        request()
                                .withPath("/test_headers_and_body")
                )
                .forward(
                        forward()
                                .withHost("127.0.0.1")
                                .withPort(getTestServerSecurePort())
                                .withScheme(HttpForward.Scheme.HTTPS)
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header("X-Test", "test_headers_and_body"),
                                new Header("Content-Type", "text/plain")
                        )
                        .withBody("an_example_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header("X-Test", "test_headers_and_body"),
                                new Header("Content-Type", "text/plain")
                        )
                        .withBody("an_example_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
    }

    @Test
    public void clientCanCallServerForResponseThenForward() {
        // when
        mockServerClient
                .when(
                        request()
                                .withPath("/test_headers_and_body"),
                        once()
                )
                .forward(
                        forward()
                                .withHost("127.0.0.1")
                                .withPort(getTestServerPort())
                );
        mockServerClient
                .when(
                        request()
                                .withPath("/test_headers_and_body"),
                        once()
                )
                .respond(
                        response()
                                .withBody("some_body")
                );

        // then
        // - forward
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header("X-Test", "test_headers_and_body"),
                                new Header("Content-Type", "text/plain")
                        )
                        .withBody("an_example_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
        // - respond
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
        // - no response or forward
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "test_headers_and_body")
                )
        );
    }

    @Test
    public void clientCanCallServerForResponseWithNoBody() {
        // when
        mockServerClient
                .when(
                        request().withMethod("POST").withPath("/some_path")
                )
                .respond(
                        response().withStatusCode(200)
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                )
        );
    }

    @Test
    public void clientCanCallServerMatchPath() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path1")
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_body1")
                );
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path2")
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_body2")
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body2"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body1"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body2"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body1"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
    }

    @Test
    public void clientCanCallServerMatchPathXTimes() {
        // when
        mockServerClient.when(new HttpRequest().withPath("/some_path"), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
    }

    @Test
    public void clientCanVerifyRequestsReceived() {
        // when
        mockServerClient.when(new HttpRequest().withPath("/some_path"), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                .withPath("/some_path"));
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                .withPath("/some_path"), org.mockserver.client.proxy.Times.exactly(1));

        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        mockServerClient.verify(new HttpRequest()
                .withURL("https{0,1}\\:\\/\\/localhost\\:\\d*\\/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("\\/") ? "\\/" : "") + "some_path")
                .withPath("/some_path"), org.mockserver.client.proxy.Times.atLeast(1));
        mockServerClient.verify(new HttpRequest()
                .withURL("https{0,1}\\:\\/\\/localhost\\:\\d*\\/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("\\/") ? "\\/" : "") + "some_path")
                .withPath("/some_path"), org.mockserver.client.proxy.Times.exactly(2));
    }

    @Test
    public void clientCanVerifyRequestsReceivedWithNoBody() {
        // when
        mockServerClient.when(new HttpRequest().withPath("/some_path"), exactly(2)).respond(new HttpResponse());

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                .withPath("/some_path"));
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                .withPath("/some_path"), org.mockserver.client.proxy.Times.exactly(1));
    }

    @Test(expected = AssertionError.class)
    public void clientCanVerifyNotEnoughRequestsReceived() {
        // when
        mockServerClient.when(new HttpRequest().withPath("/some_path"), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                .withPath("/some_path"), org.mockserver.client.proxy.Times.atLeast(2));
    }

    @Test(expected = AssertionError.class)
    public void clientCanVerifyTooManyRequestsReceived() {
        // when
        mockServerClient.when(new HttpRequest().withPath("/some_path"), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                .withPath("/some_path"), org.mockserver.client.proxy.Times.exactly(0));
    }

    @Test(expected = AssertionError.class)
    public void clientCanVerifyNotMatchingRequestsReceived() {
        // when
        mockServerClient.when(new HttpRequest().withPath("/some_path"), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                )
        );
        mockServerClient.verify(new HttpRequest()
                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_other_path")
                .withPath("/some_other_path"), org.mockserver.client.proxy.Times.exactly(2));
    }

    @Test
    public void clientCanCallServerMatchBodyWithXPath() {
        // when
        mockServerClient.when(new HttpRequest().withBody(xpath("/bookstore/book[price>35]/price")), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody(new StringBody("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                                        "\n" +
                                        "<bookstore>\n" +
                                        "\n" +
                                        "<book category=\"COOKING\">\n" +
                                        "  <title lang=\"en\">Everyday Italian</title>\n" +
                                        "  <author>Giada De Laurentiis</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>30.00</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"CHILDREN\">\n" +
                                        "  <title lang=\"en\">Harry Potter</title>\n" +
                                        "  <author>J K. Rowling</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>29.99</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"WEB\">\n" +
                                        "  <title lang=\"en\">Learning XML</title>\n" +
                                        "  <author>Erik T. Ray</author>\n" +
                                        "  <year>2003</year>\n" +
                                        "  <price>39.95</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "</bookstore>", Body.Type.EXACT))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody(new StringBody("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                                        "\n" +
                                        "<bookstore>\n" +
                                        "\n" +
                                        "<book category=\"COOKING\">\n" +
                                        "  <title lang=\"en\">Everyday Italian</title>\n" +
                                        "  <author>Giada De Laurentiis</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>30.00</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"CHILDREN\">\n" +
                                        "  <title lang=\"en\">Harry Potter</title>\n" +
                                        "  <author>J K. Rowling</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>29.99</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"WEB\">\n" +
                                        "  <title lang=\"en\">Learning XML</title>\n" +
                                        "  <author>Erik T. Ray</author>\n" +
                                        "  <year>2003</year>\n" +
                                        "  <price>39.95</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "</bookstore>", Body.Type.EXACT))
                )
        );
    }

    @Test
    public void clientCanCallServerMatchBodyWithJson() {
        // when
        mockServerClient.when(new HttpRequest().withBody(json("{\n" +
                "    \"GlossDiv\": {\n" +
                "        \"title\": \"S\", \n" +
                "        \"GlossList\": {\n" +
                "            \"GlossEntry\": {\n" +
                "                \"ID\": \"SGML\", \n" +
                "                \"SortAs\": \"SGML\", \n" +
                "                \"GlossTerm\": \"Standard Generalized Markup Language\", \n" +
                "                \"Acronym\": \"SGML\", \n" +
                "                \"Abbrev\": \"ISO 8879:1986\", \n" +
                "                \"GlossDef\": {\n" +
                "                    \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \n" +
                "                    \"GlossSeeAlso\": [\n" +
                "                        \"GML\", \n" +
                "                        \"XML\"\n" +
                "                    ]\n" +
                "                }, \n" +
                "                \"GlossSee\": \"markup\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}")), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody("{\n" +
                                        "    \"title\": \"example glossary\", \n" +
                                        "    \"GlossDiv\": {\n" +
                                        "        \"title\": \"S\", \n" +
                                        "        \"GlossList\": {\n" +
                                        "            \"GlossEntry\": {\n" +
                                        "                \"ID\": \"SGML\", \n" +
                                        "                \"SortAs\": \"SGML\", \n" +
                                        "                \"GlossTerm\": \"Standard Generalized Markup Language\", \n" +
                                        "                \"Acronym\": \"SGML\", \n" +
                                        "                \"Abbrev\": \"ISO 8879:1986\", \n" +
                                        "                \"GlossDef\": {\n" +
                                        "                    \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \n" +
                                        "                    \"GlossSeeAlso\": [\n" +
                                        "                        \"GML\", \n" +
                                        "                        \"XML\"\n" +
                                        "                    ]\n" +
                                        "                }, \n" +
                                        "                \"GlossSee\": \"markup\"\n" +
                                        "            }\n" +
                                        "        }\n" +
                                        "    }\n" +
                                        "}")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody("{\n" +
                                        "    \"title\": \"example glossary\", \n" +
                                        "    \"GlossDiv\": {\n" +
                                        "        \"title\": \"S\", \n" +
                                        "        \"GlossList\": {\n" +
                                        "            \"GlossEntry\": {\n" +
                                        "                \"ID\": \"SGML\", \n" +
                                        "                \"SortAs\": \"SGML\", \n" +
                                        "                \"GlossTerm\": \"Standard Generalized Markup Language\", \n" +
                                        "                \"Acronym\": \"SGML\", \n" +
                                        "                \"Abbrev\": \"ISO 8879:1986\", \n" +
                                        "                \"GlossDef\": {\n" +
                                        "                    \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \n" +
                                        "                    \"GlossSeeAlso\": [\n" +
                                        "                        \"GML\", \n" +
                                        "                        \"XML\"\n" +
                                        "                    ]\n" +
                                        "                }, \n" +
                                        "                \"GlossSee\": \"markup\"\n" +
                                        "            }\n" +
                                        "        }\n" +
                                        "    }\n" +
                                        "}")
                )
        );
    }

    @Test
    public void clientCanSetupExpectationForPDF() throws IOException {
        // when
        byte[] pdfBytes = IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("test.pdf"), -1, true);
        mockServerClient
                .when(
                        request()
                                .withPath("/ws/rest/user/[0-9]+/document/[0-9]+\\.pdf")
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.PDF.toString()),
                                        new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.pdf\"; filename=\"test.pdf\""),
                                        new Header(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0")
                                )
                                .withBody(pdfBytes)
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PDF.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.pdf\"; filename=\"test.pdf\""),
                                new Header(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0")
                        )
                        .withBody(pdfBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/document/2.pdf")
                                .withMethod("GET")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PDF.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.pdf\"; filename=\"test.pdf\""),
                                new Header(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0")
                        )
                        .withBody(pdfBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/document/2.pdf")
                                .withMethod("GET")
                )
        );
    }

    @Test
    public void clientCanSetupExpectationForJPG() throws IOException {
        // when
        byte[] pngBytes = IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("test.png"), -1, true);
        mockServerClient
                .when(
                        request()
                                .withPath("/ws/rest/user/[0-9]+/icon/[0-9]+\\.png")
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.PNG.toString()),
                                        new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.png\"; filename=\"test.png\"")
                                )
                                .withBody(pngBytes)
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PNG.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.png\"; filename=\"test.png\"")
                        )
                        .withBody(pngBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/icon/1.png")
                                .withMethod("GET")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PNG.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.png\"; filename=\"test.png\"")
                        )
                        .withBody(pngBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/icon/1.png")
                                .withMethod("GET")
                )
        );
    }

    @Test
    public void clientCanSetupExpectationForPDFAsBinaryBody() throws IOException {
        // when
        byte[] pdfBytes = IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("test.pdf"), -1, true);
        mockServerClient
                .when(
                        request().withBody(binary(pdfBytes))
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.PDF.toString()),
                                        new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.pdf\"; filename=\"test.pdf\""),
                                        new Header(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0")
                                )
                                .withBody(pdfBytes)
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PDF.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.pdf\"; filename=\"test.pdf\""),
                                new Header(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0")
                        )
                        .withBody(pdfBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/document/2.pdf")
                                .withMethod("GET")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PDF.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.pdf\"; filename=\"test.pdf\""),
                                new Header(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0")
                        )
                        .withBody(pdfBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/document/2.pdf")
                                .withMethod("GET")
                )
        );
    }

    @Test
    public void clientCanSetupExpectationForJPGAsBinaryBody() throws IOException {
        // when
        byte[] pngBytes = IOUtils.readFully(getClass().getClassLoader().getResourceAsStream("test.png"), -1, true);
        mockServerClient
                .when(
                        request().withBody(binary(pngBytes))
                )
                .respond(
                        response()
                                .withStatusCode(HttpStatusCode.OK_200.code())
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.PNG.toString()),
                                        new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.png\"; filename=\"test.png\"")
                                )
                                .withBody(pngBytes)
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PNG.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.png\"; filename=\"test.png\"")
                        )
                        .withBody(pngBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/icon/1.png")
                                .withMethod("GET")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withHeaders(
                                new Header(HttpHeaders.CONTENT_TYPE, MediaType.PNG.toString()),
                                new Header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"test.png\"; filename=\"test.png\"")
                        )
                        .withBody(pngBytes),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "/ws/rest/user/1/icon/1.png")
                                .withMethod("GET")
                )
        );
    }

    @Test
    public void clientCanCallServerMatchPathWithDelay() {
        // when
        mockServerClient.when(
                new HttpRequest()
                        .withPath("/some_path1")
        ).respond(
                new HttpResponse()
                        .withBody("some_body1")
                        .withDelay(new Delay(TimeUnit.MILLISECONDS, 10))
        );
        mockServerClient.when(
                new HttpRequest()
                        .withPath("/some_path2")
        ).respond(
                new HttpResponse()
                        .withBody("some_body2")
                        .withDelay(new Delay(TimeUnit.MILLISECONDS, 20))
        );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body2"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body1"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body2"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body1"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForGETAndMatchingPath() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_pathRequest")
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse"),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse"),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForGETAndMatchingPathAndBody() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withBody("some_bodyRequest")
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForGETAndMatchingPathAndParameters() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForGETAndMatchingPathAndHeaders() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("requestHeaderNameOne", "requestHeaderValueOne_One", "requestHeaderValueOne_Two"),
                                        new Header("requestHeaderNameTwo", "requestHeaderValueTwo")
                                )
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("requestHeaderNameOne", "requestHeaderValueOne_One", "requestHeaderValueOne_Two"),
                                        new Header("requestHeaderNameTwo", "requestHeaderValueTwo")
                                )
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("requestHeaderNameOne", "requestHeaderValueOne_One", "requestHeaderValueOne_Two"),
                                        new Header("requestHeaderNameTwo", "requestHeaderValueTwo")
                                )
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForGETAndMatchingPathAndCookies() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_pathRequest")
                                .withCookies(
                                        new Cookie("requestCookieNameOne", "requestCookieValueOne_One", "requestCookieValueOne_Two"),
                                        new Cookie("requestCookieNameTwo", "requestCookieValueTwo")
                                )
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withCookies(
                                        new Cookie("responseCookieNameOne", "responseCookieValueOne_One", "responseCookieValueOne_Two"),
                                        new Cookie("responseCookieNameTwo", "responseCookieValueTwo")
                                )
                );

        // then
        // - in http - cookie objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(
                                new Cookie("responseCookieNameOne", "responseCookieValueOne_One", "responseCookieValueOne_Two"),
                                new Cookie("responseCookieNameTwo", "responseCookieValueTwo")
                        )
                        .withHeaders(
                                new Header("Set-Cookie", "responseCookieNameOne=responseCookieValueOne_One", "responseCookieNameOne=responseCookieValueOne_Two", "responseCookieNameTwo=responseCookieValueTwo")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("headerNameRequest", "headerValueRequest")
                                )
                                .withCookies(
                                        new Cookie("requestCookieNameOne", "requestCookieValueOne_One", "requestCookieValueOne_Two"),
                                        new Cookie("requestCookieNameTwo", "requestCookieValueTwo")
                                )
                )
        );
        // - in http - cookie header
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(
                                new Cookie("responseCookieNameOne", "responseCookieValueOne_One", "responseCookieValueOne_Two"),
                                new Cookie("responseCookieNameTwo", "responseCookieValueTwo")
                        )
                        .withHeaders(
                                new Header("Set-Cookie", "responseCookieNameOne=responseCookieValueOne_One", "responseCookieNameOne=responseCookieValueOne_Two", "responseCookieNameTwo=responseCookieValueTwo")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("headerNameRequest", "headerValueRequest"),
                                        new Header("Cookie", "requestCookieNameOne=requestCookieValueOne_One; requestCookieNameOne=requestCookieValueOne_Two; requestCookieNameTwo=requestCookieValueTwo")
                                )
                )
        );
        // - in https - cookie objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(
                                new Cookie("responseCookieNameOne", "responseCookieValueOne_One", "responseCookieValueOne_Two"),
                                new Cookie("responseCookieNameTwo", "responseCookieValueTwo")
                        )
                        .withHeaders(
                                new Header("Set-Cookie", "responseCookieNameOne=responseCookieValueOne_One", "responseCookieNameOne=responseCookieValueOne_Two", "responseCookieNameTwo=responseCookieValueTwo")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("headerNameRequest", "headerValueRequest")
                                )
                                .withCookies(
                                        new Cookie("requestCookieNameOne", "requestCookieValueOne_One", "requestCookieValueOne_Two"),
                                        new Cookie("requestCookieNameTwo", "requestCookieValueTwo")
                                )
                )
        );
        // - in https - cookie header
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(
                                new Cookie("responseCookieNameOne", "responseCookieValueOne_One", "responseCookieValueOne_Two"),
                                new Cookie("responseCookieNameTwo", "responseCookieValueTwo")
                        )
                        .withHeaders(
                                new Header("Set-Cookie", "responseCookieNameOne=responseCookieValueOne_One", "responseCookieNameOne=responseCookieValueOne_Two", "responseCookieNameTwo=responseCookieValueTwo")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withHeaders(
                                        new Header("headerNameRequest", "headerValueRequest"),
                                        new Header("Cookie", "requestCookieNameOne=requestCookieValueOne_One; requestCookieNameOne=requestCookieValueOne_Two; requestCookieNameTwo=requestCookieValueTwo")
                                )
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForPOSTAndMatchingPathAndParameters() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                );

        // then
        // - in http - url query string
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path" +
                                        "?queryStringParameterOneName=queryStringParameterOneValueOne" +
                                        "&queryStringParameterOneName=queryStringParameterOneValueTwo" +
                                        "&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withBody(params(new Parameter("bodyParameterName", "bodyParameterValue")))
                )
        );
        // - in https - query string parameter objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_body"),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(params(new Parameter("bodyParameterName=bodyParameterValue")))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForPOSTAndMatchingPathBodyAndQueryParameters() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // - in http - url query string
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest" +
                                        "?queryStringParameterOneName=queryStringParameterOneValueOne" +
                                        "&queryStringParameterOneName=queryStringParameterOneValueTwo" +
                                        "&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in http - query string parameter objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in https - url string and query string parameter objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest" +
                                        "?queryStringParameterOneName=queryStringParameterOneValueOne" +
                                        "&queryStringParameterOneName=queryStringParameterOneValueTwo" +
                                        "&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForPOSTAndMatchingPathBodyParametersAndQueryParameters() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // - in http - body string
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(new StringBody("bodyParameterOneName=Parameter+One+Value+One" +
                                        "&bodyParameterOneName=Parameter+One+Value+Two" +
                                        "&bodyParameterTwoName=Parameter+Two", Body.Type.EXACT))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in http - body parameter objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // - in https - url string and query string parameter objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest" +
                                        "?bodyParameterOneName=bodyParameterOneValueOne" +
                                        "&bodyParameterOneName=bodyParameterOneValueTwo" +
                                        "&bodyParameterTwoName=bodyParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerPositiveMatchForPUTAndMatchingPathBodyParametersAndHeadersAndCookies() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("PUT")
                                .withPath("/some_pathRequest")
                                .withBody(new StringBody("bodyParameterOneName=Parameter+One+Value+One" +
                                        "&bodyParameterOneName=Parameter+One+Value+Two" +
                                        "&bodyParameterTwoName=Parameter+Two", Body.Type.EXACT))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // - in http - body string
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("PUT")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(new StringBody("bodyParameterOneName=Parameter+One+Value+One" +
                                        "&bodyParameterOneName=Parameter+One+Value+Two" +
                                        "&bodyParameterTwoName=Parameter+Two", Body.Type.EXACT))
                                .withHeaders(
                                        new Header("headerNameRequest", "headerValueRequest"),
                                        new Header("Cookie", "cookieNameRequest=cookieValueRequest")
                                )
                )
        );
        // - in http - body parameter objects
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                        .withBody("some_bodyResponse")
                        .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                        .withHeaders(
                                new Header("headerNameResponse", "headerValueResponse"),
                                new Header("Set-Cookie", "cookieNameResponse=cookieValueResponse")
                        ),
                makeRequest(
                        new HttpRequest()
                                .withMethod("PUT")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchBodyOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_other_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_other_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchXPathBodyOnly() {
        // when
        mockServerClient.when(new HttpRequest().withBody(new StringBody("/bookstore/book[price>35]/price", Body.Type.XPATH)), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody(new StringBody("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                                        "\n" +
                                        "<bookstore>\n" +
                                        "\n" +
                                        "<book category=\"COOKING\">\n" +
                                        "  <title lang=\"en\">Everyday Italian</title>\n" +
                                        "  <author>Giada De Laurentiis</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>30.00</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"CHILDREN\">\n" +
                                        "  <title lang=\"en\">Harry Potter</title>\n" +
                                        "  <author>J K. Rowling</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>29.99</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"WEB\">\n" +
                                        "  <title lang=\"en\">Learning XML</title>\n" +
                                        "  <author>Erik T. Ray</author>\n" +
                                        "  <year>2003</year>\n" +
                                        "  <price>31.95</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "</bookstore>", Body.Type.EXACT))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody(new StringBody("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                                        "\n" +
                                        "<bookstore>\n" +
                                        "\n" +
                                        "<book category=\"COOKING\">\n" +
                                        "  <title lang=\"en\">Everyday Italian</title>\n" +
                                        "  <author>Giada De Laurentiis</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>30.00</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"CHILDREN\">\n" +
                                        "  <title lang=\"en\">Harry Potter</title>\n" +
                                        "  <author>J K. Rowling</author>\n" +
                                        "  <year>2005</year>\n" +
                                        "  <price>29.99</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "<book category=\"WEB\">\n" +
                                        "  <title lang=\"en\">Learning XML</title>\n" +
                                        "  <author>Erik T. Ray</author>\n" +
                                        "  <year>2003</year>\n" +
                                        "  <price>31.95</price>\n" +
                                        "</book>\n" +
                                        "\n" +
                                        "</bookstore>", Body.Type.EXACT))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchJsonBodyOnly() {
        // when
        mockServerClient.when(new HttpRequest().withBody(json("{\n" +
                "    \"title\": \"example glossary\", \n" +
                "    \"GlossDiv\": {\n" +
                "        \"title\": \"wrong_value\", \n" +
                "        \"GlossList\": {\n" +
                "            \"GlossEntry\": {\n" +
                "                \"ID\": \"SGML\", \n" +
                "                \"SortAs\": \"SGML\", \n" +
                "                \"GlossTerm\": \"Standard Generalized Markup Language\", \n" +
                "                \"Acronym\": \"SGML\", \n" +
                "                \"Abbrev\": \"ISO 8879:1986\", \n" +
                "                \"GlossDef\": {\n" +
                "                    \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \n" +
                "                    \"GlossSeeAlso\": [\n" +
                "                        \"GML\", \n" +
                "                        \"XML\"\n" +
                "                    ]\n" +
                "                }, \n" +
                "                \"GlossSee\": \"markup\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}")), exactly(2)).respond(new HttpResponse().withBody("some_body"));

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody("{\n" +
                                        "    \"title\": \"example glossary\", \n" +
                                        "    \"GlossDiv\": {\n" +
                                        "        \"title\": \"S\", \n" +
                                        "        \"GlossList\": {\n" +
                                        "            \"GlossEntry\": {\n" +
                                        "                \"ID\": \"SGML\", \n" +
                                        "                \"SortAs\": \"SGML\", \n" +
                                        "                \"GlossTerm\": \"Standard Generalized Markup Language\", \n" +
                                        "                \"Acronym\": \"SGML\", \n" +
                                        "                \"Abbrev\": \"ISO 8879:1986\", \n" +
                                        "                \"GlossDef\": {\n" +
                                        "                    \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \n" +
                                        "                    \"GlossSeeAlso\": [\n" +
                                        "                        \"GML\", \n" +
                                        "                        \"XML\"\n" +
                                        "                    ]\n" +
                                        "                }, \n" +
                                        "                \"GlossSee\": \"markup\"\n" +
                                        "            }\n" +
                                        "        }\n" +
                                        "    }\n" +
                                        "}")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path")
                                .withMethod("POST")
                                .withBody("{\n" +
                                        "    \"title\": \"example glossary\", \n" +
                                        "    \"GlossDiv\": {\n" +
                                        "        \"title\": \"S\", \n" +
                                        "        \"GlossList\": {\n" +
                                        "            \"GlossEntry\": {\n" +
                                        "                \"ID\": \"SGML\", \n" +
                                        "                \"SortAs\": \"SGML\", \n" +
                                        "                \"GlossTerm\": \"Standard Generalized Markup Language\", \n" +
                                        "                \"Acronym\": \"SGML\", \n" +
                                        "                \"Abbrev\": \"ISO 8879:1986\", \n" +
                                        "                \"GlossDef\": {\n" +
                                        "                    \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\", \n" +
                                        "                    \"GlossSeeAlso\": [\n" +
                                        "                        \"GML\", \n" +
                                        "                        \"XML\"\n" +
                                        "                    ]\n" +
                                        "                }, \n" +
                                        "                \"GlossSee\": \"markup\"\n" +
                                        "            }\n" +
                                        "        }\n" +
                                        "    }\n" +
                                        "}")
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchPathOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_other_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_other_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_other_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_other_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchQueryStringParameterNameOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // wrong query string parameter name
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest" +
                                        "?OTHERQueryStringParameterOneName=queryStringParameterOneValueOne" +
                                        "&queryStringParameterOneName=queryStringParameterOneValueTwo" +
                                        "&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_pathRequest")
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchBodyParameterNameOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // wrong query string parameter name
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("OTHERBodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // wrong query string parameter name
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(new StringBody("OTHERBodyParameterOneName=Parameter+One+Value+One" +
                                        "&bodyParameterOneName=Parameter+One+Value+Two" +
                                        "&bodyParameterTwoName=Parameter+Two", Body.Type.EXACT))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchQueryStringParameterValueOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // wrong query string parameter value
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "OTHERqueryStringParameterOneValueOne", "queryStringParameterOneValueTwo"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody("some_bodyRequest")
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchBodyParameterValueOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("POST")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_bodyResponse")
                                .withHeaders(new Header("headerNameResponse", "headerValueResponse"))
                                .withCookies(new Cookie("cookieNameResponse", "cookieValueResponse"))
                );

        // then
        // wrong body parameter value
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(params(
                                        new Parameter("bodyParameterOneName", "Other Parameter One Value One", "Parameter One Value Two"),
                                        new Parameter("bodyParameterTwoName", "Parameter Two")
                                ))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
        // wrong body parameter value
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("POST")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_pathRequest")
                                .withPath("/some_pathRequest")
                                .withBody(new StringBody("bodyParameterOneName=Other Parameter+One+Value+One" +
                                        "&bodyParameterOneName=Parameter+One+Value+Two" +
                                        "&bodyParameterTwoName=Parameter+Two", Body.Type.EXACT))
                                .withHeaders(new Header("headerNameRequest", "headerValueRequest"))
                                .withCookies(new Cookie("cookieNameRequest", "cookieValueRequest"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchCookieNameOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieOtherName", "cookieValue"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieOtherName", "cookieValue"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchCookieValueOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieOtherValue"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieOtherValue"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchHeaderNameOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerOtherName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerOtherName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
    }

    @Test
    public void clientCanCallServerNegativeMatchHeaderValueOnly() {
        // when
        mockServerClient
                .when(
                        new HttpRequest()
                                .withMethod("GET")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
                .respond(
                        new HttpResponse()
                                .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
                                .withBody("some_body")
                                .withHeaders(new Header("headerName", "headerValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerOtherValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withMethod("GET")
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path?queryStringParameterOneName=queryStringParameterOneValue&queryStringParameterTwoName=queryStringParameterTwoValue")
                                .withPath("/some_path")
                                .withQueryStringParameters(
                                        new Parameter("queryStringParameterOneName", "queryStringParameterOneValue"),
                                        new Parameter("queryStringParameterTwoName", "queryStringParameterTwoValue")
                                )
                                .withBody(exact("some_body"))
                                .withHeaders(new Header("headerName", "headerOtherValue"))
                                .withCookies(new Cookie("cookieName", "cookieValue"))
                )
        );
    }

    @Test
    public void clientCanClearServerExpectations() {
        // given
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path1")
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_body1")
                );
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path2")
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_body2")
                );

        // when
        mockServerClient
                .clear(
                        new HttpRequest()
                                .withPath("/some_path1")
                );

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body2"),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.OK_200.code())
                        .withBody("some_body2"),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
    }

    @Test
    public void clientCanResetServerExpectations() {
        // given
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path1")
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_body1")
                );
        mockServerClient
                .when(
                        new HttpRequest()
                                .withPath("/some_path2")
                )
                .respond(
                        new HttpResponse()
                                .withBody("some_body2")
                );

        // when
        mockServerClient.reset();

        // then
        // - in http
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("http://localhost:" + getMockServerPort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
        // - in https
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path1")
                                .withPath("/some_path1")
                )
        );
        assertEquals(
                new HttpResponse()
                        .withStatusCode(HttpStatusCode.NOT_FOUND_404.code()),
                makeRequest(
                        new HttpRequest()
                                .withURL("https://localhost:" + getMockServerSecurePort() + "/" + servletContext + (servletContext.length() > 0 && !servletContext.endsWith("/") ? "/" : "") + "some_path2")
                                .withPath("/some_path2")
                )
        );
    }

    protected HttpResponse makeRequest(HttpRequest httpRequest) {
        HttpResponse httpResponse = apacheHttpClient.sendRequest(httpRequest);
        List<Header> headers = new ArrayList<Header>();
        for (Header header : httpResponse.getHeaders()) {
            if (!(header.getName().equals("Server") || header.getName().equals("Expires") || header.getName().equals("Date") || header.getName().equals("Connection"))) {
                headers.add(header);
            }
        }
        httpResponse.withHeaders(headers);
        return httpResponse;
    }
}
