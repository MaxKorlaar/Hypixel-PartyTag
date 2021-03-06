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

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Max Korlaar on 05-03-16.
 * Ben's tutorial has inspired me to create this simple mod.
 * Features:
 * - Show a tag above someone's name if they are in your party
 * - It does not hide the /party list its response, so it's not a bug. It's an actual feature, or rather something which just hasn't been tinkered with.
 * Bugs:
 * - Not sure, the mod is not that big either so it shouldn't be too buggy. I have not tested it with large parties yet, and members with different ranks.
 *
 * @author Max Korlaar
 * @copyright 2016 Max Korlaar
 * @credits codename_b for the modding tutorial and Forge basics (well, there are quite a lot of guides but This Just WorksTM)
 */
@Mod(modid = PartyTag.MODID, version = PartyTag.VERSION)
public class PartyTag {
    public static final String MODID = "PartyTag";
    public static final String VERSION = "1.2.1";
    private static Collection<String> partyMembers = new ArrayList<String>();
    public static Map<UUID, String> stringCache = new HashMap<java.util.UUID, String>();
    long waitUntil = System.currentTimeMillis();
    int updates = 0;
    int justJoined = 1;
    private Logger logger;
    public static UUID UUID;
    private int displayColor = 0;
    private int tickCount = 0;
    private String logPrefix = "[PartyTag v" + VERSION + "] ";
    private String leaderName;

    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLPostInitializationEvent event) {
        logger.info(logPrefix + "PartyTag has loaded! Holy ####! Moving on...");
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(new PartyTagRender());
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        UUID = minecraft.getSession().getProfile().getId();

    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
    public void tick(TickEvent.ClientTickEvent event) {
        // fire once per tick
        if (event.phase == TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isGamePaused() && mc.thePlayer != null && mc.theWorld != null) {
            if ((System.currentTimeMillis() < waitUntil) || justJoined == 1) {
                if (updates > 0) {
                    updates = 0;
                }
                if (justJoined == 1) justJoined = 0;
                updateMembers();
                return;
            }
            if (tickCount == 20) {
                displayColor = displayColor == 1 ? 0 : 1;
                tickCount = 0;
            }
            tickCount++;


            if (partyMembers == null || partyMembers.isEmpty()) return;
            for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                final String name = entityPlayer.getName();

                if (partyMembers.contains(name)) {
                    boolean isLeader = false;
                    if (name.equals(leaderName)) isLeader = true;
                    // This spams since it would get activated on every freaking tick: logger.info("[PARTY] Bingo! " + name + " is in your current world and in your party!");
                    EnumChatFormatting color;
                    if (displayColor == 0) {
                        color = EnumChatFormatting.BLUE;
                    } else {
                        if (isLeader) {
                            color = EnumChatFormatting.RED;
                        } else {
                            color = EnumChatFormatting.YELLOW;
                        }
                    }
                    // The first party member is always the party leader, as it turns out.
                    String displayMessage;
                    if (isLeader) {
                        displayMessage = "" + color + name + " is your party leader!";
                    } else {
                        displayMessage = "" + color + name + " is in your party!";
                    }
                    stringCache.put(entityPlayer.getUniqueID(), displayMessage);
                }
            }
        }
    }

    protected void updateMembers() {
        if (updates >= 1) { // Only request it once per 60 seconds - This can be configured to be higher, but since we're checking for party stuff
            // it shouldn't be needed to be higher. You can always type /p list manually if desired.
            waitUntil = System.currentTimeMillis() + 60 * 1000;
            return;
        }
        updates++;
        Minecraft mc = Minecraft.getMinecraft();
        logger.info(logPrefix + "Now sending the /p list command!");

        mc.thePlayer.sendChatMessage("/party list");
    }

    private void clearParty() {
        partyMembers.clear();
        stringCache.clear();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void chatMessageHandler(ClientChatReceivedEvent event) {
        String chatMessage = event.message.getUnformattedText();
        if (chatMessage.startsWith("Party members")) {
            // We've got the /p list command its response. Let's intercept it.*
            // Break down the message.
            String brokenMessage[] = chatMessage.split(": ");
            if (brokenMessage.length == 2) {
                clearParty();
                // 0: Party members (i):
                // 1: [ADMIN] AgentKid, [MOD] Alexmaster, [HELPER] oznek98, [MVP+] MaxKorlaar, [BELGIAN] DirtyShooter, [DÖNER] lilablassblue, [PURPLE] Rhune
                String rawMembers[] = brokenMessage[1].split(",");
                int i = 0;
                for (String rawMemberString : rawMembers) {
                    i++;
                    // Get rid of the rank tag
                    String rawMemberStringTrimmed = rawMemberString.trim();
                    String memberName;
                    if (rawMemberStringTrimmed.startsWith("[")) {
                        int endPos = rawMemberStringTrimmed.indexOf("] ");
                        memberName = rawMemberStringTrimmed.substring(endPos + 2);
                    } else {
                        memberName = rawMemberStringTrimmed;
                    }
                    partyMembers.add(memberName);
                    if (i == 1) leaderName = memberName;
                    logger.info(logPrefix + "Found party member: " + memberName);
                }
            }
        } else if (chatMessage.startsWith("You are not in a party") || chatMessage.startsWith("The party was disbanded") || chatMessage.startsWith("You have been kicked from the party")) {
            clearParty();
        } else if (chatMessage.contains("has disbanded the party!") && !ChatTools.isSentByPlayer(chatMessage)) {
            clearParty();
        } else if (chatMessage.contains("joined the party!") && !ChatTools.isSentByPlayer(chatMessage) ||
                chatMessage.contains("left the party") && !ChatTools.isSentByPlayer(chatMessage) ||
                chatMessage.contains("has promoted") && !ChatTools.isSentByPlayer(chatMessage) ||
                chatMessage.startsWith("You joined") && chatMessage.endsWith("party!")) {
            updates = 0;
            updateMembers();
        }
    }


// * with intercept, I mean parse, of course ;)
}
