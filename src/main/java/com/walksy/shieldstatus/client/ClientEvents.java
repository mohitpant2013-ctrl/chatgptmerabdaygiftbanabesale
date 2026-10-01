package com.walksy.shieldstatus.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;

import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.StringTextComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = "shieldstatus",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientEvents {

    private static final ResourceLocation WHITE =
            new ResourceLocation(
                    "minecraft",
                    "textures/block/white_concrete.png"
            );

    private static final KeyBinding TOGGLE = new KeyBinding(
            "key.shieldstatus.toggle",
            KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G),
            "key.categories.shieldstatus"
    );

    static {
        ClientRegistry.registerKeyBinding(TOGGLE);
    }

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.KeyInputEvent event) {
        while (TOGGLE.consumeClick()) {
            boolean value = !ClientConfig.ENABLED.get();
            ClientConfig.ENABLED.set(value);

            ClientPlayerEntity player = Minecraft.getInstance().player;

            if (player != null) {
                player.displayClientMessage(
                        new StringTextComponent(
                                "Shield Status: " + (value ? "ON" : "OFF")
                        ),
                        true
                );
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderPlayerEvent.Post event) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }

        PlayerEntity player = event.getPlayer();

        if (player.isSpectator()) {
            return;
        }

        if (player != Minecraft.getInstance().player) {
            return;
        }

        HandSide side = shieldSide(player);

        if (side == null) {
            return;
        }

        MatrixStack matrix = event.getMatrixStack();

        matrix.pushPose();

        matrix.translate(
                side == HandSide.LEFT ? -0.30D : 0.30D,
                1.02D,
                -0.34D
        );

        draw(player, matrix, event.getBuffers());

        matrix.popPose();
    }

    private static HandSide shieldSide(PlayerEntity player) {
        if (isShield(player.getItemInHand(Hand.MAIN_HAND))) {
            return player.getMainArm();
        }

        if (isShield(player.getItemInHand(Hand.OFF_HAND))) {
            return player.getMainArm() == HandSide.LEFT
                    ? HandSide.RIGHT
                    : HandSide.LEFT;
        }

        return null;
    }

    private static boolean isShield(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof ShieldItem;
    }

    private static void draw(
            PlayerEntity player,
            MatrixStack matrix,
            IRenderTypeBuffer buffers
    ) {
        boolean disabled =
                player.getCooldowns().isOnCooldown(Items.SHIELD);

        float cooldown =
                player.getCooldowns()
                        .getCooldownPercent(Items.SHIELD, 0.0F);

        cooldown = MathHelper.clamp(
                cooldown,
                0.0F,
                1.0F
        );

        int red;
        int green;
        int blue;

        if (ClientConfig.INTERPOLATE.get() && disabled) {
            float progress = 1.0F - cooldown;

            red = mix(
                    ClientConfig.DISABLED_RED.get(),
                    ClientConfig.ENABLED_RED.get(),
                    progress
            );

            green = mix(
                    ClientConfig.DISABLED_GREEN.get(),
                    ClientConfig.ENABLED_GREEN.get(),
                    progress
            );

            blue = mix(
                    ClientConfig.DISABLED_BLUE.get(),
                    ClientConfig.ENABLED_BLUE.get(),
                    progress
            );
        } else {
            red = disabled
                    ? ClientConfig.DISABLED_RED.get()
                    : ClientConfig.ENABLED_RED.get();

            green = disabled
                    ? ClientConfig.DISABLED_GREEN.get()
                    : ClientConfig.ENABLED_GREEN.get();

            blue = disabled
                    ? ClientConfig.DISABLED_BLUE.get()
                    : ClientConfig.ENABLED_BLUE.get();
        }

        RenderSystem.enableBlend();

        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        RenderSystem.depthMask(false);

        IVertexBuilder builder =
                buffers.getBuffer(
                        RenderType.entityTranslucent(WHITE)
                );

        Matrix4f pose = matrix.last().pose();

        quad(
                builder,
                pose,
                -0.11F,
                0.22F,
                0.11F,
                0.12F,
                red,
                green,
                blue,
                90
        );

        quad(
                builder,
                pose,
                -0.20F,
                0.12F,
                0.20F,
                -0.14F,
                red,
                green,
                blue,
                90
        );

        quad(
                builder,
                pose,
                -0.11F,
                -0.14F,
                0.11F,
                -0.22F,
                red,
                green,
                blue,
                90
        );

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static int mix(
            int a,
            int b,
            float factor
    ) {
        return MathHelper.clamp(
                Math.round(a + (b - a) * factor),
                0,
                255
        );
    }

    private static void quad(
            IVertexBuilder builder,
            Matrix4f pose,
            float left,
            float top,
            float right,
            float bottom,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertex(builder, pose, left, top, red, green, blue, alpha);
        vertex(builder, pose, right, top, red, green, blue, alpha);
        vertex(builder, pose, right, bottom, red, green, blue, alpha);
        vertex(builder, pose, left, bottom, red, green, blue, alpha);
    }

    private static void vertex(
            IVertexBuilder builder,
            Matrix4f pose,
            float x,
            float y,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        builder
                .vertex(pose, x, y, 0.0F)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
