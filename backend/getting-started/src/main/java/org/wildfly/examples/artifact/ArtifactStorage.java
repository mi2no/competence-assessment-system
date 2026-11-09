package org.wildfly.examples.artifact;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class ArtifactStorage {

    @Inject
    @ConfigProperty(name = "artifact.dir")
    String dir;

    public record FileData(
            String name,
            InputStream stream
    ) {}

    public UUID store(List<FileData> files) throws IOException {
        final UUID id = UUID.randomUUID();
        final String name = id + ".zip";
        final Path path = Path.of(dir);

        Files.createDirectories(path);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path.resolve(name)))) {

            for (final var entry : files)
                try (final InputStream in = entry.stream()) {
                    zip.putNextEntry(new ZipEntry(entry.name()));

                    in.transferTo(zip);

                    zip.closeEntry();
                }
        }

        return id;
    }
}
