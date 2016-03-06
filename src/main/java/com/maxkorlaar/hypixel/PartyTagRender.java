/*
The MIT License (MIT)

Copyright (c) 2016 Max Korlaar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.maxkorlaar.hypixel;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * So basically this class renders the actual text above one of your party members' (or anyone else in the stringCache HashMap – A HashMap is some sort of mapped array) nametags
 * I would never have figured this out on my own so I'm going to give @VladToBeHere/codename_B credit right here:
 * @credit https://github.com/codename-B/Facepixel/blob/master/src/main/java/de/bananaco/FacepixelRender.java
 * I'm going to try and add some comments. It will hopefully help me (and you?) figure out what everything means, so we can learn from it. Yay, learning!
 */

public class PartyTagRender {

    @SubscribeEvent
    public void render(RenderPlayerEvent.Pre event) {
        String s;
        if(event.entityPlayer.getUniqueID().equals(PartyTag.UUID)) return;
        if(!event.entityPlayer.isSneaking() && (s = PartyTag.stringCache.get(event.entityPlayer.getUniqueID())) != null) {

            double offset = 0.3; // The distance from the nametag
            Scoreboard scoreboard = event.entityPlayer.getWorldScoreboard(); // Get the scoreboard object, so that we can check for additional tags above one's head
            // Hypixel sometimes puts additional information in the scoreboard slot above player heads. Check if it's being used, and if so, just double the offset (distance) to the player's nametag,
            // so that we won't overlap it.
            ScoreObjective scoreObjective = scoreboard.getObjectiveInDisplaySlot(2);

            if(scoreObjective != null) {
                offset *= 2;
            }

            renderName(event.renderer, s, event.entityPlayer, event.x, event.y+offset, event.z); // Call the renderName function below. This makes the code a bit easier to re-use and a bit more organized.
        }
    }

    public void renderName(RendererLivingEntity renderer, String str, EntityPlayer entityIn, double x, double y, double z) {
        FontRenderer fontrenderer = renderer.getFontRendererFromRenderManager(); // The thingy that can render text on our screen
        float f = 1.6F; // Okay this is where the witchcraft begins. A float is like an integer, with decimals. For some reason, it also has an F at the end of it to remind us of the fact that it is, in fact, a float.
        // We would simply call it a number, although unfortunately programmers turn out to be a bit picky.
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix(); // * random green chars fall down on a black screen *
        GlStateManager.translate((float)x + 0.0F, (float)y + entityIn.height + 0.5F, (float)z); // So, are you going to take the red pill or the blue pill?
        // Actually, the above means that we're deciding where the stuff should be rendered. Translating here doesn't mean from English to Dutch or something, but rather moving the 'object' in a threedimensional space.
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-renderer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderer.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        // I guess that makes sure that it stays aligned with the player's nametag, as seen by us?
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        // Some advanced stuff making it look OK!
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        // But.. Will it blend??
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer(); // This object makes it possible to just dump our renders into the actual rendered world
        // Actually just ignore it from here on. As most programmers tend to do, just copy the code, see if it works and make sure NOT TO TOUCH IT IF IT WORKS SUCCESSFULLY!
        int i = 0;

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double)(-j - 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(-j - 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double)(j + 1), (double)(-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, 553648127);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        fontrenderer.drawString(str, -fontrenderer.getStringWidth(str) / 2, i, -1);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();

        // Advanced stuff, Ben! I doubt that I myself am ever going to become a 'professional' / 'good' Java programmer :P
    }
}