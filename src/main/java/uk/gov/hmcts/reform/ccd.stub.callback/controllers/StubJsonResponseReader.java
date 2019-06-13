package uk.gov.hmcts.reform.ccd.stub.callback.controllers;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FilenameUtils.getBaseName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StubJsonResponseReader {

    private static final Logger LOG = LoggerFactory.getLogger(StubJsonResponseReader.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${callback.json.stubs.path}")
    private String jsonStubsPath;

    private Map<String, JsonNode> jsonStubsResponses = new HashMap<>();

    @PostConstruct
    private void init() {
        LOG.info("Json stubs path: {}", jsonStubsPath);
        File jsonStubsDirectory = new File(jsonStubsPath);
        FileFilter jsonFileFilter = pathname -> pathname.getName().toLowerCase().endsWith(".json");
        ofNullable(jsonStubsDirectory.listFiles(jsonFileFilter))
            .ifPresent(jsonFiles -> {
                for (File file : jsonFiles) {
                    try {
                        JsonNode jsonNode = OBJECT_MAPPER.readValue(file, JsonNode.class);
                        jsonStubsResponses.put(getBaseName(file.getName()), jsonNode);
                    } catch (IOException e) {
                        LOG.error("Error converting file:", e);
                    }
                }
            });
    }

    public JsonNode getStubJsonResponse(String jsonStubName) {
        return jsonStubsResponses.getOrDefault(jsonStubName, NullNode.getInstance());
    }

}
