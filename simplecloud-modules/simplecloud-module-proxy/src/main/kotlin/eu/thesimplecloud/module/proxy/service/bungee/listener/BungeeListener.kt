/*
 * MIT License
 *
 * Copyright (C) 2020 The SimpleCloud authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package eu.thesimplecloud.module.proxy.service.bungee.listener

import eu.thesimplecloud.api.player.text.CloudText
import eu.thesimplecloud.module.proxy.extensions.mapToLowerCase
import eu.thesimplecloud.module.proxy.service.ProxyHandler
import eu.thesimplecloud.module.proxy.service.bungee.BungeePluginMain
import eu.thesimplecloud.plugin.proxy.bungee.text.CloudTextBuilder
import eu.thesimplecloud.plugin.startup.CloudPlugin
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.event.ServerSwitchEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

/**
 * Created by IntelliJ IDEA.
 * User: Philipp.Eistrach
 * Date: 14.03.2020
 * Time: 22:14
 */
class BungeeListener(val plugin: BungeePluginMain) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun on(event: ServerConnectEvent) {
        val player = event.player

        val config = ProxyHandler.configHolder.getValue()
        val proxyConfiguration = ProxyHandler.getProxyConfiguration() ?: return

        if (CloudPlugin.instance.thisService().getServiceGroup().isInMaintenance()) {
            if (!player.hasPermission(ProxyHandler.JOIN_MAINTENANCE_PERMISSION) &&
                !proxyConfiguration.whitelist.mapToLowerCase().contains(player.name.lowercase())
            ) {
                player.disconnect(CloudTextBuilder().build(CloudText(ProxyHandler.replaceString(config.maintenanceKickMessage))))
                event.isCancelled = true
                return
            }
        }


        val maxPlayers = CloudPlugin.instance.thisService().getServiceGroup().getMaxPlayers()

        if (ProxyHandler.getOnlinePlayers() > maxPlayers) {
            if (!player.hasPermission(ProxyHandler.JOIN_FULL_PERMISSION) &&
                !proxyConfiguration.whitelist.mapToLowerCase().contains(player.name.lowercase())
            ) {
                player.disconnect(CloudTextBuilder().build(CloudText(ProxyHandler.replaceString(config.fullProxyKickMessage))))
                event.isCancelled = true
            }
        }

    }

    @EventHandler
    fun on(event: ServerConnectedEvent) {
        val player = event.player
        val tablistConfiguration = ProxyHandler.getCurrentTablistConfiguration() ?: return
        plugin.sendHeaderAndFooter(player, tablistConfiguration)
    }

    @EventHandler
    fun on(event: ServerSwitchEvent) {
        val player = event.player

        val tablistConfiguration = ProxyHandler.getCurrentTablistConfiguration() ?: return
        plugin.sendHeaderAndFooter(player, tablistConfiguration)
    }

    @EventHandler
    fun on(event: ProxyPingEvent) {
        val response = event.response

        val proxyConfiguration = ProxyHandler.getProxyConfiguration() ?: return
        val motdConfiguration = if (CloudPlugin.instance.thisService().getServiceGroup().isInMaintenance())
            proxyConfiguration.maintenanceMotds else proxyConfiguration.motds

        val line1 = motdConfiguration.firstLines.random()
        val line2 = motdConfiguration.secondLines.random()

        val motdComponent = ProxyHandler.getHexColorComponent(ProxyHandler.replaceString("$line1\n$line2"))
        response.descriptionComponent = BungeeComponentSerializer.get().serialize(motdComponent)[0]

        val playerInfo = motdConfiguration.playerInfo
        val onlinePlayers = ProxyHandler.getOnlinePlayers()

        val versionName = motdConfiguration.versionName
        if (versionName != null && versionName.isNotEmpty()) {
            response.version = ServerPing.Protocol(ProxyHandler.replaceString(versionName), -1)
        }

        val maxPlayers = CloudPlugin.instance.thisService().getServiceGroup().getMaxPlayers()

        if (playerInfo != null && playerInfo.isNotEmpty()) {
            val nullArrayPlayerInfo = arrayOfNulls<ServerPing.PlayerInfo>(playerInfo.size)
            for (i in nullArrayPlayerInfo.indices) nullArrayPlayerInfo[i] =
                ServerPing.PlayerInfo(playerInfo[i], "0-0-0-0-0")
            response.players = ServerPing.Players(maxPlayers, onlinePlayers, nullArrayPlayerInfo)
            return
        }

        response.players.max = maxPlayers
        response.players.online = onlinePlayers
    }

}