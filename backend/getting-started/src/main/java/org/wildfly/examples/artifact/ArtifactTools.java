package org.wildfly.examples.artifact;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ApplicationScoped
public class ArtifactTools {
    private static final int MAX_BYTES = 50_000;

    @Inject
    ArtifactStorage storage;

    @Tool("Returns the contents of given artifact file")
    public String readFile(@P("The exact path specified in the artifact's structure") String path, InvocationParameters params) {
        final UUID uuid = params.get("uuid");

        try {
            final ArtifactStorage.StoredFileData data = storage.getFile(uuid, path);
            if (data == null) return "File specified by path doesn't exist";

            try (final InputStream stream = data.stream()) {
                final byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                final boolean truncated = bytes.length > MAX_BYTES;

                final String text = new String(bytes, 0, Math.min(bytes.length, MAX_BYTES), StandardCharsets.UTF_8);
                return truncated ? text + "\n[... file truncated to " + MAX_BYTES + " bytes]" : text;
            }
        } catch (IOException e) {
            return "Error opening file: " + e.getMessage();
        }
    }
}
