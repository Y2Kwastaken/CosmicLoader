package sh.miles.cosmicloader.services;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.lib.accesswidener.AccessWidener;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.util.ASMifier;
import org.spongepowered.asm.launch.MixinBootstrap;
import sh.miles.cosmicloader.patch.CosmicReachPatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CosmicReachProvider implements GameProvider {

    public static final String CLIENT_ENTRY_POINT = "finalforeach.cosmicreach.lwjgl3.Lwjgl3Launcher";
    private static final String[] ENTRY_POINTS = new String[]{CLIENT_ENTRY_POINT};
    private static final GameTransformer TRANSFORMER = new GameTransformer(new CosmicReachPatch());

    private Arguments arguments;
    private String entrypoint;
    private Path gameJar;
    private EnvType envType;
    private String version;

    @Override
    public String getGameId() {
        return "cosmic_reach";
    }

    @Override
    public String getGameName() {
        return "Cosmic Reach";
    }

    @Override
    public String getRawGameVersion() {
        return version;
    }

    @Override
    public String getNormalizedGameVersion() {
        return version;
    }

    @Override
    public String getEntrypoint() {
        return this.entrypoint;
    }

    @Override
    public Path getLaunchDirectory() {
        if (arguments == null) {
            return Paths.get(".");
        }

        return getLaunchDirectory(arguments);
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    @Override
    public void unlockClassPath(final FabricLauncher fabricLauncher) {
        fabricLauncher.addToClassPath(gameJar);
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public String[] getLaunchArguments(final boolean b) {
        return arguments == null ? new String[0] : arguments.toArray();
    }

    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        HashMap<String, String> contactMap = new HashMap<>();
        contactMap.put("homepage", "https://finalforeach.itch.io/cosmic-reach");
        contactMap.put("wiki", "https://finalforeach.itch.io/cosmic-reach");


        BuiltinModMetadata.Builder modMetadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                .setName(getGameName())
                .addAuthor("FinalForEach", contactMap)
                .setContact(new ContactInformationImpl(contactMap))
                .setDescription("Cosmic Reach Game");

        return Collections.singletonList(new BuiltinMod(Collections.singletonList(gameJar), modMetadata.build()));
    }

    @Override
    public boolean locateGame(final FabricLauncher launcher, final String[] args) {
        this.arguments = new Arguments();
        this.arguments.parse(args);
        processArgumentMap(arguments);

        List<String> appLocations = new ArrayList<>();
        // Respect "fabric.gameJarPath" if it is set.
        if (System.getProperty(SystemProperties.GAME_JAR_PATH) != null) {
            appLocations.add(System.getProperty(SystemProperties.GAME_JAR_PATH));
        }

        appLocations.add("./cosmic-reach.jar");
        appLocations.add("./game/cosmic-reach.jar");

        List<Path> existingAppLocations = appLocations.stream().map(p -> Paths.get(p).toAbsolutePath().normalize()).filter(Files::exists).toList();

        GameProviderHelper.FindResult result = GameProviderHelper.findFirst(existingAppLocations, new HashMap<>(), true, ENTRY_POINTS);

        if (result == null || result.path == null) {
            String appLocationsString = appLocations.stream().map(p -> (String.format("* %s", Paths.get(p).toAbsolutePath().normalize()))).collect(Collectors.joining("\n"));

            Log.error(LogCategory.GAME_PROVIDER, "Could not locate the application JAR! We looked in: \n" + appLocationsString);

            return false;
        }

        entrypoint = result.name;
        version = CosmicReachVersionLookup.getVersionFromJar(result.path);
        gameJar = result.path;

        return true;
    }

    @Override
    public void launch(ClassLoader loader) {
        String targetClass = entrypoint;
        try {
            Class<?> c = loader.loadClass(targetClass);
            Method m = c.getMethod("main", String[].class);
            m.invoke(null, (Object) arguments.toArray());
        } catch (InvocationTargetException e) {
            throw new FormattedException("Cosmic Reach has crashed!", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new FormattedException("Failed to start Cosmic Reach", e);
        }
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        try {
            launcher.setValidParentClassPath(ImmutableList.of(
                    Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(MixinBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(FabricLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(AnnotationVisitor.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(AbstractInsnNode.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(Analyzer.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(ASMifier.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(AdviceAdapter.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(MixinExtrasBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(AccessWidener.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(Gson.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    Path.of(BiMap.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            ));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        TRANSFORMER.locateEntrypoints(launcher, Collections.singletonList(gameJar));
    }


    private void processArgumentMap(Arguments arguments) {
        if (!arguments.containsKey("gameDir")) {
            arguments.put("gameDir", getLaunchDirectory(arguments).toAbsolutePath().normalize().toString());
        }

        Path launchDir = Path.of(arguments.get("gameDir"));
        Log.debug(LogCategory.GAME_PROVIDER, "Launch directory is " + launchDir);
    }

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault("gameDir", "."));
    }
}
