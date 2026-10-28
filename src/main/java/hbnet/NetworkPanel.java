package hbnet;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.function.Consumer;

/** Embeddable local server/contact management; no background polling or listeners. */
public final class NetworkPanel extends JPanel {
    private final LocalProfile profile;
    private final DefaultListModel<String> servers=new DefaultListModel<>(),friends=new DefaultListModel<>();
    private final JList<String> serverList=new JList<>(servers),friendList=new JList<>(friends);
    private final JLabel status=new JLabel("Presence: Unknown/unreachable. Exchange cards privately.");
    private final JCheckBox safe;
    private final Consumer<LocalProfile.SavedServer> connect;
    private final java.util.List<Runnable> privacyListeners=new ArrayList<>();
    public NetworkPanel(LocalProfile profile,Consumer<LocalProfile.SavedServer> connect){
        this(profile,connect,"beckenburg");
    }
    public NetworkPanel(LocalProfile profile,Consumer<LocalProfile.SavedServer> connect,String game){
        this.profile=profile;this.connect=connect;setLayout(new BorderLayout(6,6));
        safe=new JCheckBox("Stream Safe",profile.streamSafe());safe.addActionListener(e->act(()->{profile.streamSafe(safe.isSelected());privacyListeners.forEach(Runnable::run);refresh();}));add(safe,BorderLayout.NORTH);
        JPanel columns=new JPanel(new GridLayout(1,2,8,8));
        JPanel saved=new JPanel(new BorderLayout());saved.setBorder(BorderFactory.createTitledBorder("Saved Servers"));saved.add(new JScrollPane(serverList));
        JPanel actions=new JPanel(new GridLayout(0,2,5,5));
        button(actions,"Add",()->server(null));button(actions,"Edit",()->server(selectedServer()));
        button(actions,"Connect",()->{LocalProfile.SavedServer s=selectedServer();if(s!=null)connect.accept(s);});
        button(actions,"Favorite",()->{var s=selectedServer();if(s!=null)profile.favoriteServer(s.id(),!s.favorite());refresh();});
        button(actions,"Remove",()->{var s=selectedServer();if(s!=null)profile.removeServer(s.id());refresh();});saved.add(actions,BorderLayout.SOUTH);columns.add(saved);
        button(actions,"Import private invite",()->{JPasswordField input=masked("",32);if(JOptionPane.showConfirmDialog(this,input,"Private signed invitation",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){var invite=Invitation.parse(new String(input.getPassword()),game);if(JOptionPane.showConfirmDialog(this,"Save only after verifying the sender and fingerprint privately. A signed package alone does not establish trust. Room password/access policy still applies.","Import invitation",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)profile.server(null,"Private Server",invite.host(),invite.port(),invite.fingerprint());refresh();}});
        JPanel contacts=new JPanel(new BorderLayout());contacts.setBorder(BorderFactory.createTitledBorder("Friends"));contacts.add(new JScrollPane(friendList));JPanel ca=new JPanel(new GridLayout(0,2,5,5));
        button(ca,"Import card",this::importCard);button(ca,"Export own card",()->privateExport(profile.identity.card()));
        button(ca,"Accept",()->state(LocalProfile.ContactState.ACCEPTED));button(ca,"Decline",()->state(LocalProfile.ContactState.DECLINED));
        button(ca,"Block",()->state(LocalProfile.ContactState.BLOCKED));button(ca,"Unblock",()->state(LocalProfile.ContactState.PENDING));
        button(ca,"Edit / favorite",this::editFriend);button(ca,"Remove",()->{var c=selectedFriend();if(c!=null)profile.removeFriend(c.id());refresh();});
        button(ca,"Export contacts",()->privateExport(profile.exportContacts()));button(ca,"Import contacts",()->{JPasswordField input=masked("",32);if(JOptionPane.showConfirmDialog(this,input,"Private contact backup (imported contacts require review)",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){profile.importContacts(new String(input.getPassword()));refresh();}});
        contacts.add(ca,BorderLayout.SOUTH);columns.add(contacts);add(columns,BorderLayout.CENTER);add(status,BorderLayout.SOUTH);refresh();
    }
    public void onPrivacyChange(Runnable listener){privacyListeners.add(listener);}
    private interface Action{void run()throws Exception;}
    private void button(JPanel panel,String text,Action action){JButton b=new JButton(text);b.addActionListener(e->act(action));panel.add(b);}
    private void act(Action action){try{action.run();}catch(Exception e){status.setText("Action unavailable. Check private input or local profile (NET-20).");}}
    private void refresh(){servers.clear();friends.clear();var ss=profile.servers();for(int i=0;i<ss.size();i++)servers.addElement(profile.serverLabel(ss.get(i),i)+(ss.get(i).favorite()?" *":""));var fs=profile.friends();for(int i=0;i<fs.size();i++)friends.addElement(profile.friendLabel(fs.get(i),i)+" / "+fs.get(i).state()+" / "+fs.get(i).presence());}
    private LocalProfile.SavedServer selectedServer(){int i=serverList.getSelectedIndex();return i<0?null:profile.servers().get(i);}
    private LocalProfile.Contact selectedFriend(){int i=friendList.getSelectedIndex();return i<0?null:profile.friends().get(i);}
    public static JPasswordField masked(String value,int columns){JPasswordField field=new JPasswordField(value,columns);field.setEchoChar('\u2022');field.enableInputMethods(false);field.putClientProperty("JPasswordField.cutCopyAllowed",false);return field;}
    private void server(LocalProfile.SavedServer existing)throws Exception{
        JPasswordField label=masked(existing==null?"":existing.label(),24),host=masked(existing==null?"":existing.host(),24),port=masked(existing==null?"27717":Integer.toString(existing.port()),8),pin=masked(existing==null?"":existing.pin(),24);
        JPanel p=form(new String[]{"Private label","DNS/IP (masked)","Port (masked)","Privately verified SHA-256 fingerprint"},new JComponent[]{label,host,port,pin});
        if(JOptionPane.showConfirmDialog(this,p,"Saved server",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){profile.server(existing==null?null:existing.id(),new String(label.getPassword()),new String(host.getPassword()),Hbnet.port(new String(port.getPassword())),new String(pin.getPassword()));refresh();}
    }
    private static JPanel form(String[] labels,JComponent[] values){JPanel p=new JPanel(new GridLayout(0,2,5,5));for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(values[i]);}return p;}
    private void importCard()throws Exception{
        JPasswordField card=masked("",32),label=masked("",20);if(JOptionPane.showConfirmDialog(this,form(new String[]{"Friend card","Private label"},new JComponent[]{card,label}),"Import contact for review",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){profile.importCard(new String(card.getPassword()),new String(label.getPassword()));refresh();}
    }
    private void state(LocalProfile.ContactState s)throws Exception{var c=selectedFriend();if(c!=null){profile.state(c.id(),s);refresh();}}
    private void editFriend()throws Exception{var c=selectedFriend();if(c==null)return;JPasswordField label=masked(c.label(),20),notes=masked(c.notes(),25);JCheckBox fav=new JCheckBox("Favorite",c.favorite());JPanel p=form(new String[]{"Private label","Private notes","Favorite"},new JComponent[]{label,notes,fav});if(JOptionPane.showConfirmDialog(this,p,"Edit friend",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){profile.edit(c.id(),new String(label.getPassword()),new String(notes.getPassword()),fav.isSelected());refresh();}}
    private void privateExport(String value){if(JOptionPane.showConfirmDialog(this,"Copy private card? Clipboard history and other apps may be captured.","Private export",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(value),null);status.setText("Private export copied. Presence remains unknown until directly observed.");}}
}
