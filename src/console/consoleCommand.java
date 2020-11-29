package console;

import client.MapleCharacter;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import client.MapleClient;
import server.ShutdownServer;
/**
 *
 * @author Hades
 */
public class consoleCommand {
    public static String Channel1 = null;
    public static String Channel2 = null;
    public static String Channel3 = null;
    public static String Channel4 = null;
    public static String Channel5 = null;
    public static String Channel6 = null;
    public static String Channel7 = null;
    public static String Channel8 = null;
    public static String Channel9 = null;
    public static String Channel10 = null;
    public static String Channel11 = null;
    public static String Channel12 = null;
    public static String Channel13 = null;
    public static String Channel14 = null;
    public static String Channel15 = null;
    public static String Channel16 = null;
    public static String Channel17 = null;
    public static void ServerMessage(String serverMessage) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(serverMessage);
            }
        System.out.println("[AlmightyMS] Server message has been changed to: " + serverMessage);
    }
    public static void Online() {
        
        Channel1 = ChannelServer.getInstance(1).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel2 = ChannelServer.getInstance(2).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel3 = ChannelServer.getInstance(3).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel4 = ChannelServer.getInstance(4).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel5 = ChannelServer.getInstance(5).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel6 = ChannelServer.getInstance(6).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel7 = ChannelServer.getInstance(7).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel8 = ChannelServer.getInstance(8).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel9 = ChannelServer.getInstance(9).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel10 = ChannelServer.getInstance(10).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel11 = ChannelServer.getInstance(11).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel12 = ChannelServer.getInstance(12).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel13 = ChannelServer.getInstance(13).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel14 = ChannelServer.getInstance(14).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel15 = ChannelServer.getInstance(15).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel16 = ChannelServer.getInstance(16).getPlayerStorage().getOnlinePlayers(true).toString();
        Channel17 = ChannelServer.getInstance(17).getPlayerStorage().getOnlinePlayers(true).toString();
        
    }
    public static void Ban(String victim, String reason) {
        // underdevelopment
    }
    public static void shutdownServer() {
        ShutdownServer.getInstance().run();
        ShutdownServer.getInstance().run();
    }
}