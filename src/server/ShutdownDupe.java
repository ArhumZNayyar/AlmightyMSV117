package server;

import java.sql.SQLException;

import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import static server.ShutdownServer.instance;
import server.Timer.*;
import tools.packet.CWvsContext;

public class ShutdownDupe implements ShutdownServerMBean {

    public static ShutdownServer instance;

    public static void registerMBean() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            instance = new ShutdownServer();
            mBeanServer.registerMBean(instance, new ObjectName("server:type=ShutdownServer"));
        } catch (Exception e) {
            System.out.println("Error registering Shutdown MBean");
            e.printStackTrace();
        }
    }

    public static ShutdownServer getInstance() {
	return instance;
    }

    public int mode = 0;

    public void shutdown() {//can execute twice
	run();
    }

    @Override
    public void run() {
	if (mode == 0) {
	    int ret = 0;
	    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "The server is shutting down in 1 minute. Please log off safely."));
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.setShutdown();
				cs.setServerMessage("The world is going to shutdown soon. Please log off safely.");
                
            }
            /*AtomicInteger FinishedThreads = new AtomicInteger(0);
            HiredMerchantSave.Execute(this);
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ShutdownServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            while (FinishedThreads.incrementAndGet() != HiredMerchantSave.NumSavingThreads) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ShutdownServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }*/
            
	    System.out.println("This is a DUPE shutdown!: " + ret);
	    mode++;
	} else if (mode == 1) {
	    mode++;
			System.out.println("This is a DUPE shutdown!...");
            try {
	        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "The world is going to shutdown now. Please log off safely."));
                Integer[] chs =  ChannelServer.getAllInstance().toArray(new Integer[0]);
        
                for (int i : chs) {
                    try {
                        ChannelServer cs = ChannelServer.getInstance(i);
                        synchronized (this) {
                            cs.shutdown();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
	        LoginServer.shutdown();
                CashShopServer.shutdown();
                DatabaseConnection.closeAll();
            } catch (SQLException e) {
                System.err.println("THROW" + e);
            }
            WorldTimer.getInstance().stop();
            MapTimer.getInstance().stop();
            BuffTimer.getInstance().stop();
            CloneTimer.getInstance().stop();
            EventTimer.getInstance().stop();
	    EtcTimer.getInstance().stop();
	    PingTimer.getInstance().stop();
		System.out.println("This is a DUPE shutdown!");
		try{
                Thread.sleep(5000);
            }catch(Exception e) {
                //shutdown
            }
            System.exit(0); //not sure if this is really needed for ChannelServer
	}
    }
}
