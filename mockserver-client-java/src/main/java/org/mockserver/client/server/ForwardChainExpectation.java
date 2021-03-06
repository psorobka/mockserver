package org.mockserver.client.server;

import com.google.common.annotations.VisibleForTesting;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpForward;
import org.mockserver.model.HttpResponse;

/**
 * @author jamesdbloom
 */
public class ForwardChainExpectation {

    private final MockServerClient mockServerClient;
    private final Expectation expectation;

    public ForwardChainExpectation(MockServerClient mockServerClient, Expectation expectation) {
        this.mockServerClient = mockServerClient;
        this.expectation = expectation;
    }

    public void respond(HttpResponse httpResponse) {
        expectation.thenRespond(httpResponse);
        mockServerClient.sendExpectation(expectation);
    }

    public void forward(HttpForward httpForward) {
        expectation.thenForward(httpForward);
        mockServerClient.sendExpectation(expectation);
    }

    @VisibleForTesting
    Expectation getExpectation() {
        return expectation;
    }
}
