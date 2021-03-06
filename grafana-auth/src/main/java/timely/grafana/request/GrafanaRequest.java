package timely.grafana.request;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import timely.api.annotation.Http;
import timely.api.request.AuthenticatedRequest;
import timely.api.request.HttpGetRequest;
import timely.api.request.HttpPostRequest;

@Http(path = "/")
public class GrafanaRequest extends AuthenticatedRequest implements HttpGetRequest, HttpPostRequest {

    private FullHttpRequest httpRequest = null;

    @Override
    public HttpGetRequest parseQueryParameters(QueryStringDecoder decoder) throws Exception {
        return new GrafanaRequest();
    }

    @Override
    public HttpPostRequest parseBody(String content) throws Exception {
        return new GrafanaRequest();
    }

    public void setHttpRequest(FullHttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public FullHttpRequest getHttpRequest() {
        return httpRequest;
    }
}
