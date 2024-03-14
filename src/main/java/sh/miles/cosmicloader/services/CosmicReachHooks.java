package sh.miles.cosmicloader.services;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.nio.file.Path;

public class CosmicReachHooks {
    public static final String INTERNAL_NAME = CosmicReachHooks.class.getName().replace('.', '/');

    public static void initClient() {
        Path cwd = Path.of(".");
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.prepareModInit(cwd, loader.getGameInstance());
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        loader.invokeEntrypoints("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
    }

}
