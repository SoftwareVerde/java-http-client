package com.softwareverde.http.v2;

import com.softwareverde.http.HttpMethod;
import com.softwareverde.http.HttpRequest;
import com.softwareverde.http.HttpResponse;
import com.softwareverde.util.Container;
import org.junit.Assert;
import org.junit.Test;

public class HttpRequestTests {
    @Test
    public void should_cancel_request() throws Exception {
        // Setup
        final Container<HttpResponse> responseContainer = new Container<>();

        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod(HttpMethod.GET);
        httpRequest.setUrl("https://speed.hetzner.de/100MB.bin");

        final Object pin = new Object();

        // Action
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    responseContainer.value = httpRequest.execute();
                }
                finally {
                    synchronized (pin) {
                        pin.notifyAll();
                    }
                }
            }
        })).start();

        Thread.sleep(500L);
        Assert.assertTrue(httpRequest.isExecuting());
        httpRequest.cancel();

        synchronized (pin) {
            pin.wait();
        }

        // Assert
        Assert.assertNull(responseContainer.value);
    }
}
