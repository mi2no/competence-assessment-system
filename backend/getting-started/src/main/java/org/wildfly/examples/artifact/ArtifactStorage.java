package org.wildfly.examples.artifact;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

    public record FileInfo (
            String name,
            long size
    ) {}

    public List<FileInfo> getStructure(UUID artifactId) {
        final File file = Path.of(dir).resolve(artifactId + ".zip").toFile();

        if (!file.exists()) return null;

        try (final ZipFile zip = new ZipFile(file)) {
            final List<FileInfo> result = new ArrayList<>();
            final Enumeration<? extends  ZipEntry> en = zip.entries();

            while (en.hasMoreElements()) {
                final var entry = en.nextElement();

                result.add(new FileInfo(entry.getName(), entry.getSize()));
            }

            return result;
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
