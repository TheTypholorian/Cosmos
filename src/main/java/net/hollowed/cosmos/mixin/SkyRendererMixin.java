package net.hollowed.cosmos.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.hollowed.cosmos.config.CosmosConfig;
import net.hollowed.cosmos.renderer.CosmosStarRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Shadow
    @Final
    private RenderSystem.AutoStorageIndexBuffer quadIndices;

    @Shadow
    @Final
    private TextureAtlas celestialsAtlas;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(TextureManager textureManager, AtlasManager atlasManager, CallbackInfo ci) {
        CosmosStarRendering.cosmosStarVertexBuffer = CosmosStarRendering.createCosmosStars(this.celestialsAtlas);
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void renderStars(float starBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (CosmosConfig.enabled) {
            if (Minecraft.getInstance().level != null) {
                starBrightness *= CosmosConfig.brightnessMultiplier;

                Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
                matrix4fStack.pushMatrix();
                matrix4fStack.mul(poseStack.last().pose());
                GpuTextureView color = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
                GpuTextureView depth = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
                GpuBuffer gpuBuffer = this.quadIndices.getBuffer(CosmosStarRendering.cosmosStarIndexCount);
                float time = Minecraft.getInstance().level.getGameTime() % 24000 / 20.0F;

                GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                        .writeTransform(matrix4fStack, new Vector4f(starBrightness, CosmosConfig.twinkleFrequency.getFirst().floatValue(), CosmosConfig.twinkleFrequency.get(1).floatValue(), time), new Vector3f(), new Matrix4f());

                if (color != null) {
                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "Stars", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
                        renderPass.setPipeline(CosmosStarRendering.COSMOS_STARS);
                        RenderSystem.bindDefaultUniforms(renderPass);
                        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                        renderPass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
                        renderPass.setVertexBuffer(0, CosmosStarRendering.cosmosStarVertexBuffer);
                        renderPass.setIndexBuffer(gpuBuffer, this.quadIndices.type());
                        renderPass.drawIndexed(0, 0, CosmosStarRendering.cosmosStarIndexCount, 1);
                    }
                }

                matrix4fStack.popMatrix();
            }
            ci.cancel();
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void close(CallbackInfo ci) {
        CosmosStarRendering.cosmosStarVertexBuffer.close();
    }
}
