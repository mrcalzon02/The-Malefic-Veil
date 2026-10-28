package hbnet;

import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/** Randomized presentation sentinels; never emits a sentinel or stores a secret screenshot. */
public final class PrivacyUiTest {
    private static void inspect(Component component,java.util.List<String> secrets){
        String text=component instanceof JLabel label?label.getText():component instanceof AbstractButton b?b.getText():component instanceof JTextField f&&!(f instanceof JPasswordField)?f.getText():"";
        for(String secret:secrets)if(text!=null&&text.contains(secret))throw new AssertionError("Visible privacy sentinel");
        if(component instanceof JList<?> list)for(int i=0;i<list.getModel().getSize();i++)for(String secret:secrets)if(String.valueOf(list.getModel().getElementAt(i)).contains(secret))throw new AssertionError("Visible list privacy sentinel");
        if(component instanceof JPasswordField password){if(password.getEchoChar()==0)throw new AssertionError("Input reveal");var accessible=password.getAccessibleContext().getAccessibleText();if(accessible!=null)for(int i=0;i<accessible.getCharCount();i++){String at=accessible.getAtIndex(javax.accessibility.AccessibleText.CHARACTER,i);if(at!=null&&at.length()==1&&at.charAt(0)!=password.getEchoChar())throw new AssertionError("Accessible password leak");}}
        if(component instanceof Container parent)for(Component child:parent.getComponents())inspect(child,secrets);
    }
    private static JCheckBox toggle(Container parent){for(Component c:parent.getComponents()){if(c instanceof JCheckBox box&&box.getText().equals("Stream Safe"))return box;if(c instanceof Container child){JCheckBox found=toggle(child);if(found!=null)return found;}}return null;}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("hbnet-privacy-");try{
            LocalProfile profile=new LocalProfile(root);String unique=UUID.randomUUID().toString().replace("-","");String name="name"+unique,host="private-"+unique+".test",key="key"+unique;
            profile.server(null,name,host,37491,"a".repeat(64));String friend=profile.importCard(Hbnet.Identity.create().card(),name);profile.edit(friend,name,key,false);
            java.util.List<String> sentinels=java.util.List.of(name,host,key,"37491","a".repeat(64));
            SwingUtilities.invokeAndWait(()->{
                NetworkPanel panel=new NetworkPanel(profile,s->{});panel.add(NetworkPanel.masked(host,30));panel.add(NetworkPanel.masked(key,30));panel.setSize(800,600);panel.doLayout();inspect(panel,sentinels);
                panel.paint(new java.awt.image.BufferedImage(800,600,java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics());
                JCheckBox safe=toggle(panel);if(safe==null)throw new AssertionError("Privacy control missing");safe.doClick();safe.doClick();inspect(panel,sentinels);
            });System.out.println("PASS PrivacyUiTest: randomized labels, endpoint, port, key, fingerprint, masked paste backing, accessibility and OFF-to-ON existing-widget checks");
        }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
