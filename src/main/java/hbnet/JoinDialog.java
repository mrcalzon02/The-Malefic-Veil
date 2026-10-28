package hbnet;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;

/** Existing-menu embeddable connection form. No raw endpoint or secret is ever rendered. */
public final class JoinDialog {
    private JoinDialog(){}
    public record Request(String host,int port,Hbnet.Client transport){@Override public String toString(){return "Private Join Request";}}
    public static Request show(Component parent,LocalProfile profile,String game,int defaultPort)throws java.io.IOException,java.security.GeneralSecurityException{
        JPasswordField host=NetworkPanel.masked("127.0.0.1",24),port=NetworkPanel.masked(Integer.toString(defaultPort),6),pin=NetworkPanel.masked("",24),password=NetworkPanel.masked("",24);
        JCheckBox secure=new JCheckBox("TLS: fingerprint verified privately",true),save=new JCheckBox("Save this endpoint after successful connection",false);
        JPanel form=new JPanel(new GridLayout(0,2,6,6));String[] labels={"DNS / IP (masked)","Port (masked)","Server fingerprint (masked)","Room password / key (masked)"};JComponent[] fields={host,port,pin,password};
        for(int i=0;i<fields.length;i++){form.add(new JLabel(labels[i]));form.add(fields[i]);}form.add(secure);form.add(new JLabel("Unencrypted mode: trusted LAN only"));
        JTabbedPane pages=new JTabbedPane();pages.addTab("Connect",form);pages.addTab("Saved Servers & Friends",new NetworkPanel(profile,s->{host.setText(s.host());port.setText(Integer.toString(s.port()));pin.setText(s.pin());secure.setSelected(true);pages.setSelectedIndex(0);},game));
        if(JOptionPane.showConfirmDialog(parent,pages,"Private multiplayer",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return null;
        String address=Hbnet.host(new String(host.getPassword()));int p=Hbnet.port(new String(port.getPassword()));char[] secret=password.getPassword();
        try{return new Request(address,p,secure.isSelected()?new Hbnet.Client(new String(pin.getPassword()),game,profile.identity,secret):null);}finally{Arrays.fill(secret,'\0');password.setText("");}
    }
}
