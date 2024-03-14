package sh.miles.cosmicloader.services;

import net.fabricmc.loader.impl.util.ExceptionUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CosmicReachVersionLookup {

    public static String getVersionFromJar(final Path path) {
        final String errorVersion = "0.0.0";
        try (ZipFile file = new ZipFile(path.toFile())) {
            final ZipEntry versionEntry = file.getEntry("build_assets/version.txt");
            if (versionEntry == null) {
                return errorVersion;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(versionEntry)))) {
                String realVersion = reader.readLine();
                if (realVersion == null) {
                    return errorVersion;
                } else {
                    return realVersion;
                }
            }
        } catch (IOException e) {
            throw ExceptionUtil.wrap(e);
        }
    }

}
