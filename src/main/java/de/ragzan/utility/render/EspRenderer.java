package de.ragzan.utility.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.ragzan.utility.ClientLogic;
import de.ragzan.utility.config.ModConfig;
import de.ragzan.utility.mixin.RenderLayerAccessor;
import de.ragzan.utility.mixin.RenderPipelinesAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexConsumer;

public final class EspRenderer {
    private static RenderLayer X_RAY_LINES;
    private static final ModConfig C = ModConfig.INSTANCE;

    private EspRenderer() {}

    public static void initialize() {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelinesAccessor.ragzan$positionColorSnippet())
                .withLocation(Identifier.of("ragzan_utility", "pipeline/xray_lines"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.DrawMode.LINES)
                .build();
        pipeline = RenderPipelinesAccessor.ragzan$register(pipeline);
        X_RAY_LINES = RenderLayerAccessor.ragzan$create("ragzan_utility_xray_lines", RenderSetup.builder(pipeline)
                .expectedBufferSize(4096)
                .build());
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(EspRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || X_RAY_LINES == null) return;
        if (!C.playerEsp && !C.blockEsp && !C.stashEsp) return;

        PoseStack matrices = context.matrices();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumerProvider consumers = context.consumers();

        if (C.playerEsp) {
            RenderSystem.lineWidth(C.playerThickness);
            VertexConsumer lines = consumers.getBuffer(X_RAY_LINES);
            float[] color = rgba(C.playerColor());
            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == client.player || target.isSpectator() || !target.isAlive()) continue;
                if (client.player.squaredDistanceTo(target) > (double) C.playerRange * C.playerRange) continue;
                Box box = target.getBoundingBox().expand(0.025);
                WorldRenderer.drawBox(matrices, lines, box, color[0], color[1], color[2], color[3]);
            }
            flushLines(consumers);
        }

        if (C.blockEsp) {
            RenderSystem.lineWidth(C.blockThickness);
            VertexConsumer lines = consumers.getBuffer(X_RAY_LINES);
            float[] color = rgba(C.blockColor());
            for (BlockPos p : ClientLogic.blockEspPositions()) {
                Box box = new Box(p).expand(0.002);
                WorldRenderer.drawBox(matrices, lines, box, color[0], color[1], color[2], color[3]);
            }
            flushLines(consumers);
        }

        if (C.stashEsp) {
            RenderSystem.lineWidth(C.stashThickness);
            VertexConsumer lines = consumers.getBuffer(X_RAY_LINES);
            float[] color = rgba(C.stashColor());
            for (BlockPos p : ClientLogic.stashPositions()) {
                Box box = new Box(p).expand(0.004);
                WorldRenderer.drawBox(matrices, lines, box, color[0], color[1], color[2], color[3]);
            }
            flushLines(consumers);
        }

        matrices.popPose();
    }

    private static void flushLines(VertexConsumerProvider consumers) {
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(X_RAY_LINES);
        }
    }

    private static float[] rgba(int argb) {
        return new float[] {
                ((argb >> 16) & 0xFF) / 255.0f,
                ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >>> 24) & 0xFF) / 255.0f
        };
    }
}
