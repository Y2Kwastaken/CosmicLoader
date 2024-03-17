package sh.miles.cosmicloader.patch;

import com.google.common.util.concurrent.ExecutionError;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import sh.miles.cosmicloader.services.CosmicReachHooks;

import java.util.function.Consumer;
import java.util.function.Function;

public class CosmicReachPatch extends GamePatch {

    @Override
    public void process(final FabricLauncher launcher, final Function<String, ClassNode> classSource, final Consumer<ClassNode> classEmitter) {
        final String entryPoint = launcher.getEntrypoint();
        final EnvType environment = launcher.getEnvironmentType();
        Log.debug(LogCategory.GAME_PATCH, "Entry point at %s".formatted(environment));
        Log.debug(LogCategory.GAME_PATCH, "Environment Type is %s".formatted(environment.name()));

        ClassNode entryPointClass = classSource.apply(entryPoint);
        if (entryPointClass == null) {
            throw new LinkageError("Could not load entry point class %s!".formatted(entryPoint));
        }

        switch (environment) {
            case CLIENT -> injectClient(entryPointClass);
            case SERVER -> injectServer(entryPointClass);
        }

        classEmitter.accept(entryPointClass);
    }

    private void injectClient(ClassNode entrypoint) {
        MethodNode initMethod = findMethod(entrypoint, (method) -> method.name.equals("main"));
        if (initMethod == null) {
            throw new NoSuchMethodError("Could not find or inject into method \"main\"");
        }

        injectAboveApplication(initMethod, new MethodInsnNode(Opcodes.INVOKESTATIC, CosmicReachHooks.INTERNAL_NAME, "initClient", "()V", false));
    }

    private void injectServer(ClassNode entrypoint) {
        throw new InjectionError("Cosmic Reach currently has no server so an injection in the server environment can not occur");
    }

    private static void injectAboveApplication(final MethodNode method, AbstractInsnNode injectedInsn) {
        AbstractInsnNode createApplication = null;

        for (final AbstractInsnNode node : method.instructions) {
            if (node instanceof MethodInsnNode methodNode && node.getOpcode() == Opcodes.INVOKESTATIC) {
                Log.info(LogCategory.GAME_PATCH, "Found Method: " + methodNode.name);
                if (methodNode.name.equals("createApplication")) {
                    createApplication = methodNode;
                }
            }
        }

        if (createApplication == null) {
            throw new NoSuchMethodError("No such method createApplication in the visited method. This is a severe error!");
        }

        method.instructions.insertBefore(createApplication, injectedInsn);
    }

}
