package me.cortex.voxy.client.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.cortex.voxy.client.LoadException;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockableEventLoop.class)
public abstract class MixinThreadExecutor {
    @WrapOperation(method = "doRunTask", at = @At(value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Lorg/slf4j/Marker;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void voxy$forceCrashOnError(Logger logger, Marker marker, String message,
                                      Object name, Object error, Operation<Void> original) {
        if (error instanceof LoadException exception) throw exception;
        original.call(logger, marker, message, name, error);
    }
}
