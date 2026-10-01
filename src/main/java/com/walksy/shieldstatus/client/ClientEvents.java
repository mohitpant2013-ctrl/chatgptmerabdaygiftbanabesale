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
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.settings.KeyBinding;

@Mod.EventBusSubscriber(modid = "shieldstatus", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private static final KeyBinding TOGGLE = new KeyBinding("key.shieldstatus.toggle", KeyConflictContext.IN_GAME, GLFW.GLFW_KEY_G, "key.categories.shieldstatus");

    static { ClientRegistry.registerKeyBinding(TOGGLE); }

    private ClientEvents() {}

    @SubscribeEvent
    public static void onKey(InputEvent.KeyInputEvent e) {
        while (TOGGLE.isPressed()) {
            boolean value = !ClientConfig.ENABLED.get();
            ClientConfig.ENABLED.set(value);
            ClientPlayerEntity p = Minecraft.getInstance().player;
            if (p != null) p.sendStatusMessage(new StringTextComponent("Shield Status: " + (value ? "ON" : "OFF")), true);
        }
    }

    @SubscribeEvent
    public static void render(RenderPlayerEvent.Post e) {
        if (!ClientConfig.ENABLED.get() || e.getPlayer().isSpectator()) return;
        PlayerEntity player = e.getPlayer();
        // Personal indicator: never draw a huge/duplicate indicator over other players.
        if (player != Minecraft.getInstance().player) return;
        HandSide side = shieldSide(player);
        if (side == null) return;

        MatrixStack ms = e.getMatrixStack();
        ms.push();
        ms.translate(side == HandSide.LEFT ? -0.30D : 0.30D, 1.02D, -0.34D);
        draw(player, ms, e.getBuffers());
        ms.pop();
    }

    private static HandSide shieldSide(PlayerEntity p) {
        if (shield(p.getHeldItem(Hand.MAIN_HAND))) return p.getPrimaryHand();
        if (shield(p.getHeldItem(Hand.OFF_HAND))) return p.getPrimaryHand().opposite();
        return null;
    }

    private static boolean shield(ItemStack s) { return !s.isEmpty() && s.getItem() instanceof ShieldItem; }

    private static void draw(PlayerEntity p, MatrixStack ms, IRenderTypeBuffer buffers) {
        boolean disabled = p.getCooldownTracker().hasCooldown(Items.SHIELD);
        float t = MathHelper.clamp(p.getCooldownTracker().getCooldown(Items.SHIELD, 0.0F), 0.0F, 1.0F);
        int r, g, b;
        if (ClientConfig.INTERPOLATE.get() && disabled) {
            float f = 1.0F - t;
            r = mix(ClientConfig.DISABLED_RED.get(), ClientConfig.ENABLED_RED.get(), f);
            g = mix(ClientConfig.DISABLED_GREEN.get(), ClientConfig.ENABLED_GREEN.get(), f);
            b = mix(ClientConfig.DISABLED_BLUE.get(), ClientConfig.ENABLED_BLUE.get(), f);
        } else {
            r = disabled ? ClientConfig.DISABLED_RED.get() : ClientConfig.ENABLED_RED.get();
            g = disabled ? ClientConfig.DISABLED_GREEN.get() : ClientConfig.ENABLED_GREEN.get();
            b = disabled ? ClientConfig.DISABLED_BLUE.get() : ClientConfig.ENABLED_BLUE.get();
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        IVertexBuilder v = buffers.getBuffer(RenderType.getEntityTranslucent(WHITE));
        Matrix4f pose = ms.getLast().getMatrix();
        quad(v, pose, -0.11F, 0.22F, 0.11F, 0.12F, r,g,b,90);
        quad(v, pose, -0.20F, 0.12F, 0.20F, -0.14F, r,g,b,90);
        quad(v, pose, -0.11F, -0.14F, 0.11F, -0.22F, r,g,b,90);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static int mix(int a, int b, float f) { return MathHelper.clamp(Math.round(a + (b-a)*f), 0, 255); }

    private static void quad(IVertexBuilder v, Matrix4f p, float l,float top,float r,float bot,int red,int green,int blue,int alpha) {
        vertex(v,p,l,top,red,green,blue,alpha); vertex(v,p,r,top,red,green,blue,alpha);
        vertex(v,p,r,bot,red,green,blue,alpha); vertex(v,p,l,bot,red,green,blue,alpha);
    }
    private static void vertex(IVertexBuilder v, Matrix4f p,float x,float y,int r,int g,int b,int a) {
        v.pos(p,x,y,0).color(r,g,b,a).tex(0,0).overlay(OverlayTexture.NO_OVERLAY).lightmap(0xF000F0).endVertex();
    }
}
