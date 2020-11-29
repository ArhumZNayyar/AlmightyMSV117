/*package gui;


import handling.channel.ChannelServer;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import server.ShutdownServer;


public class GUI extends JFrame {


    private JPanel contentPane;
    private JTextField txtCommand;
    private JTextField txtComment;
    private JLabel lblLbl;


    /**
     * Launch the application.
     
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    GUI frame = new GUI();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Create the frame.
     
    public GUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 499, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        
        JButton btnProcess = new JButton("Process Command");
        btnProcess.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if("shutdown".equals(txtCommand.getText().toLowerCase())){
                    final Thread t = new Thread(ShutdownServer.getInstance());
                    if ((t == null) || (!t.isAlive())) {
                        t.start();
                    }
                }
                if("servermessage".equals(txtCommand.getText().toLowerCase())){
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setServerMessage(txtComment.getText());
                      }
                }
            }
        });
        contentPane.add(btnProcess, BorderLayout.EAST);
        
        txtCommand = new JTextField();
        txtCommand.setText("comand");
        contentPane.add(txtCommand, BorderLayout.NORTH);
        txtCommand.setColumns(10);
        
        txtComment = new JTextField();
        txtComment.setText("comment");
        contentPane.add(txtComment, BorderLayout.CENTER);
        txtComment.setColumns(10);
        
        lblLbl = new JLabel("shutdown, servermessage");
        lblLbl.setFont(new Font("Tahoma", Font.PLAIN, 15));
        contentPane.add(lblLbl, BorderLayout.SOUTH);
    }


}
*/