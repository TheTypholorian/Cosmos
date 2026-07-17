package net.hollowed.cosmos.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.hollowed.cosmos.Cosmos;
import net.hollowed.cosmos.config.CosmosConfig;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class CosmosStarRendering {

    public static final RenderPipeline COSMOS_STARS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Cosmos.id("pipeline/stars"))
                    .withVertexShader(Cosmos.id("core/stars"))
                    .withFragmentShader(Cosmos.id("core/stars"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static int cosmosStarIndexCount;

    public static GpuBuffer cosmosStarVertexBuffer;

    public static GpuBuffer createCosmosStars(TextureAtlas atlas) {
        RandomSource random = RandomSource.createThreadLocalInstance(10842L);

        GpuBuffer var19;
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * CosmosConfig.starCount * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            TextureAtlasSprite sprite = atlas.getSprite(Cosmos.id("star"));

            boolean northStar = false;
            int limit = CosmosConfig.northStar ? 1 : 0;

            for (int i = 0; i < CosmosConfig.starCount; i++) {
                float x = random.nextFloat() * 2.0F - 1.0F;
                float y = random.nextFloat() * 2.0F - 1.0F;
                float z = random.nextFloat() * 2.0F - 1.0F;
                float starSize = (CosmosConfig.sizeRange.getFirst().floatValue() + random.nextFloat() * (CosmosConfig.sizeRange.get(1).floatValue() - CosmosConfig.sizeRange.getFirst().floatValue()));

                if (x < -0.48F && y < -0.23F && y > -0.32F && Math.abs(z) < 0.07F && limit > 0) {
                    starSize *= 2;
                    northStar = true;
                    limit--;
                }

                float lengthSq = Mth.lengthSquared(x, y, z);
                if (!(lengthSq <= 0.010000001F) && !(lengthSq >= 1.0F)) {
                    Vector3f starCenter = (new Vector3f(x, y, z)).normalize(100.0F);
                    float zRot = (float)(random.nextDouble() * (double)(float)Math.PI * (double)2.0F);
                    Matrix3f rotation = (new Matrix3f()).rotateTowards((new Vector3f(starCenter)).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-zRot);

                    int alpha = CosmosConfig.alphaRange.getFirst() + random.nextInt(CosmosConfig.alphaRange.get(1) - CosmosConfig.alphaRange.getFirst());

                    String colorString = CosmosConfig.colors.get(random.nextInt(CosmosConfig.colors.size()));
                    int[] colorEntry = hexToRGB(colorString);

                    int color = ARGB.color(alpha, colorEntry[0], colorEntry[1], colorEntry[2]);

                    if (northStar) {
                        color = ARGB.color(255,255, 255, 255);
                        northStar = false;
                    }

                    bufferBuilder.addVertex(new Vector3f(starSize, -starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
                    bufferBuilder.addVertex(new Vector3f(starSize, starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
                    bufferBuilder.addVertex(new Vector3f(-starSize, starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
                    bufferBuilder.addVertex(new Vector3f(-starSize, -starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
                }
            }

            try (MeshData mesh = bufferBuilder.buildOrThrow()) {
                cosmosStarIndexCount = mesh.drawState().indexCount();
                var19 = RenderSystem.getDevice().createBuffer(() -> "Stars vertex buffer", 40, mesh.vertexBuffer());
            }
        }

        return var19;
    }

    public static int[] hexToRGB(String hexColor) {
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }

        int red = Integer.parseInt(hexColor.substring(0, 2), 16);
        int green = Integer.parseInt(hexColor.substring(2, 4), 16);
        int blue = Integer.parseInt(hexColor.substring(4, 6), 16);

        return new int[] { red, green, blue };
    }
}
