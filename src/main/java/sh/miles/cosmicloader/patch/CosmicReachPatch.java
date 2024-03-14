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

        injectTailInsn(initMethod, new MethodInsnNode(Opcodes.INVOKESTATIC, CosmicReachHooks.INTERNAL_NAME, "initClient", "()V", false));
    }

    private void injectServer(ClassNode entrypoint) {
        throw new InjectionError("Cosmic Reach currently has no server so an injection in the server environment can not occur");
    }

    /**
     * @see org.spongepowered.asm.mixin.injection.points.BeforeFinalReturn#find
     */
    private static void injectTailInsn(MethodNode method, AbstractInsnNode injectedInsn) {
        AbstractInsnNode ret = null;

        // RETURN opcode varies based on return type, thus we calculate what opcode we're actually looking for by inspecting the target method
        int returnOpcode = Type.getReturnType(method.desc).getOpcode(Opcodes.IRETURN);
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof InsnNode && insn.getOpcode() == returnOpcode) {
                ret = insn;
            }
        }

        // WAT?
        if (ret == null) {
            throw new RuntimeException("TAIL could not locate a valid RETURN in the target method!");
        }

        method.instructions.insertBefore(ret, injectedInsn);
    }

}
