package com.example.fixture;

public class WireMockJavaDslScannerFixture {

    void getOrder() {
        stubFor(get(urlEqualTo("/orders/1")).willReturn(aResponse().withStatus(200)));
    }

    void createOrder() {
        stubFor(post(urlPathEqualTo("/orders")).willReturn(aResponse().withStatus(201)));
    }

    void usesUrlPathMatching() {
        stubFor(put(urlPathMatching("/orders/[0-9]+")).willReturn(aResponse().withStatus(204)));
    }

    void usesUrlPathTemplate() {
        stubFor(delete(urlPathTemplate("/orders/{id}")).willReturn(aResponse().withStatus(204)));
    }

    void chainsMultipleBuilderCallsBeforeWillReturn() {
        stubFor(get(urlEqualTo("/orders/priority")).atPriority(1)
                .willReturn(aResponse().withStatus(200)));
    }

    void usesHeadVerb() {
        stubFor(head(urlEqualTo("/orders/1")).willReturn(aResponse().withStatus(200)));
    }

    void usesOptionsVerb() {
        stubFor(options(urlEqualTo("/orders")).willReturn(aResponse().withStatus(200)));
    }

    void usesTraceVerb() {
        stubFor(trace(urlEqualTo("/orders/1")).willReturn(aResponse().withStatus(200)));
    }

    void usesInstanceScopedStubFor() {
        wireMockServer.stubFor(get(urlEqualTo("/orders/instance")).willReturn(aResponse().withStatus(200)));
    }

    void skipsAnyUrlMatcher() {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    void skipsDynamicPath() {
        stubFor(get(urlEqualTo(buildPath())).willReturn(aResponse().withStatus(200)));
    }

    void skipsBuilderAssembledInVariable() {
        MappingBuilder builder = get(urlEqualTo("/orders/indirect"));
        stubFor(builder.willReturn(aResponse().withStatus(200)));
    }

    void skipsUnrelatedStubForCall() {
        stubFor();
    }
}
