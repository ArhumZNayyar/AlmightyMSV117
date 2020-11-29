/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Chris
 * Edited and fixed by Rummy
 */
public class MesoBank {

    public static boolean createBank(MapleCharacter chr, int mesos) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM MesoBank WHERE accountid = ?");
            ps.setInt(1, chr.getAccountID());
            ResultSet rs = ps.executeQuery();
            while (!rs.next()) {
                ps.close();
                rs.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO MesoBank VALUES (DEFAULT, ?, ?)");
                ps2.setInt(1, chr.getAccountID());
                ps2.setInt(2, mesos > 1000 ? mesos : 1000);
                int result = ps2.executeUpdate();
                if (result > 0) {
                    chr.dropMessage(-3, "My bank has been created!");
                    chr.gainMeso(mesos > 1000 ? -mesos : -1000, true, true);
                    ps2.close();
                    return true;
                } else {
                    System.out.println("Problem creating meso bank. Account ID " + chr.getAccountID());
                    ps2.close();
                    return false;
                }
            } 
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println(e);
        }
        return false;
    }

    public static long checkBank(MapleCharacter chr) {
        long mesos = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT mesos FROM MesoBank WHERE accountid = ?");
            ps.setInt(1, chr.getAccountID());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mesos = rs.getLong("mesos");
            } else {
                mesos = 0;
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {

        }
        return mesos;
    }

    public static boolean updateBank(MapleCharacter chr, int mesos, boolean deposit) {
        if (mesos < 0 ) {//Should never even get here.
            return false;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            if (!deposit && checkBank(chr) - mesos < 0 || !deposit && chr.getMeso() + mesos > Integer.MAX_VALUE || deposit && chr.getMeso() - mesos < 0) {
                return false;
            }
            PreparedStatement ps = con.prepareStatement(deposit ? "UPDATE MesoBank SET mesos = mesos + ? where accountid = ?" : "UPDATE MesoBank SET mesos = mesos - ? where accountid = ?");
            ps.setLong(1, mesos);
            ps.setInt(2, chr.getAccountID());
            int result = ps.executeUpdate();
            if (result > 0) {
                chr.gainMeso(deposit ? -mesos : mesos, true, true);
                chr.dropMessage(-3, "My bank now contains " + checkBank(chr) + " mesos.");
                return true;
            } else {
                System.out.println("Unable to update bank account. Account ID is " + chr.getAccountID());
            }
            ps.close();
        } catch (SQLException e) {

        }

        return false;
    }
}
