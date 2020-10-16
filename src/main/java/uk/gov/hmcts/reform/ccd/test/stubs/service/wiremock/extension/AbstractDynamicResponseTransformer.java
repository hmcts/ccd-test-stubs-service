package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

/*
 * Customises the static stubbed response before sending it back to the client
 */
public abstract class AbstractDynamicResponseTransformer extends ResponseTransformer {

    @Override
    public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
        return Response.Builder.like(response)
            .but()
            .body(dynamiseResponse(request, response, parameters))
            .build();
    }

    @Override
    public boolean applyGlobally() {
        // This flag will ensure this transformer is used only for those request mappings that have the transformer
        // configured
        return false;
    }

    protected abstract String dynamiseResponse(Request request, Response response, Parameters parameters);
}
