package client.anticheat;
import client.anticheat.HWID;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuildCharacter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.ScriptEngine;
import org.apache.mina.common.IoSession;
import server.CharacterCardFactory;
import server.Timer.PingTimer;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import server.shops.IMaplePlayerShop;
import tools.FileoutputUtil;
import tools.MapleAESOFB;
import tools.Pair;
import tools.packet.CField;
import tools.packet.LoginPacket; 
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.UUID;

 
/*
* Created By Rummy
* This will grab the HWID of a player. This is used for ban's to prevent them from making multiple accounts and keep playing by spoofing
*/
public class HWID {
        private transient IoSession session;
        private static String hwid = null;
        private static String convertToHex(byte[] data)
    {
        StringBuffer buf = new StringBuffer();
        
        for (int i = 0; i < data.length; i++)
        {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do
            {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            }
            while(two_halfs++ < 1);
        }
        return buf.toString();
    }
 
    private static String SHA512(String text)
    throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-512");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("UTF-8"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }
        public String getHWID() {
                try {
                        if(hwid != null)
                                return hwid;
                        hwid = "";
                        session.getRemoteAddress().toString().split("IP");
                        
                        hwid += System.getenv("PROCESSOR_IDENTIFIER");
                        hwid += System.getenv("COMPUTERNAME");
                        Enumeration<NetworkInterface> netenum = NetworkInterface.getNetworkInterfaces();
                        if(netenum.hasMoreElements())
                                hwid += netenum.nextElement().getHardwareAddress();
                        hwid = SHA512(hwid);
                        return hwid;
                } catch(NoSuchAlgorithmException nsae) {} catch (SocketException e) {} catch (UnsupportedEncodingException e) {}
                return null;
        }
        
     
}