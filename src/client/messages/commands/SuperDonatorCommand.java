/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.MapleClient;
import client.SkillFactory;
import static client.messages.commands.AdminCommand.Shutdown.t;
import constants.ServerConstants.PlayerGMRank;
import handling.world.World;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.ShutdownServer;
import tools.packet.CWvsContext;

/**
 *
 * @author Emilyx3
 */
public class SuperDonatorCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERDONATOR;
    }
    
     
}
