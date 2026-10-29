package veil.gpu;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * Native Swing bridge for the SAME licensed PSX Horror UI Free source pack
 * already used by the OpenGL menu and gameplay HUD. EditorPalettePanel,
 * EditorProject and EditorHistory remain the actual portable editor module.
 */
public final class PsxHorrorSwingSkin {
    private static final Color INK=new Color(225,213,189);
    private static final Color BACKDROP=new Color(24,22,21);
    private static final Color FIELD=new Color(36,33,31);
    private final Map<String,BufferedImage> tiles=new HashMap<>();
    public PsxHorrorSwingSkin(){
        Path tmp=null;
        try{
            tmp=PsxHorrorUiSource.verifiedCopy();
            try(ZipFile zip=new ZipFile(tmp.toFile())){
                for(String id:new String[]{"button_normal","button_hover","button_pressed",
                        "button_disabled","window","panel","line_edit","menu_band"}){
                    var entry=zip.getEntry(PsxHorrorUiSource.FOG+id+".png");
                    if(entry==null)throw new IOException("Licensed UI sprite unavailable: "+id);
                    try(InputStream stream=zip.getInputStream(entry)){
                        BufferedImage image=ImageIO.read(stream);
                        if(image==null||image.getWidth()<6||image.getHeight()<6)
                            throw new IOException("Invalid source UI sprite: "+id);
                        tiles.put(id,image);
                    }
                }
            }
        }catch(IOException bad){throw new UncheckedIOException("PSX editor skin cannot load",bad);}
        finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}
    }
    public int sourceSprites(){return tiles.size();}
    private BufferedImage sprite(String name){
        BufferedImage image=tiles.get(name);
        if(image==null)throw new IllegalStateException("Original source sprite not loaded "+name);
        return image;
    }
    /** Draw each edge at native pixels; stretch ONLY the nine-slice center. */
    static void paintNine(Graphics2D g,BufferedImage src,int x,int y,int w,int h,
                          int left,int right,int top,int bottom,boolean center){
        int sw=src.getWidth(),sh=src.getHeight();
        int wl=Math.min(left,Math.max(1,w/3)),wr=Math.min(right,Math.max(1,w/3));
        int ht=Math.min(top,Math.max(1,h/3)),hb=Math.min(bottom,Math.max(1,h/3));
        int[] sx={0,left,sw-right,sw},sy={0,top,sh-bottom,sh};
        int[] dx={x,x+wl,x+w-wr,x+w},dy={y,y+ht,y+h-hb,y+h};
        Object before=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            if(!center&&row==1&&col==1)continue;
            if(dx[col+1]<=dx[col]||dy[row+1]<=dy[row])continue;
            g.drawImage(src,dx[col],dy[row],dx[col+1],dy[row+1],
                sx[col],sy[row],sx[col+1],sy[row+1],null);
        }
        if(before!=null)g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,before);
    }
    /** Actual pack art provides the editor's button idle/hover/pressed states. */
    public JButton button(String label,Runnable action){
        JButton result=new JButton(label){
            @Override protected void paintComponent(Graphics incoming){
                Graphics2D g=(Graphics2D)incoming.create();
                try{
                    String state=!isEnabled()?"button_disabled":
                        getModel().isPressed()?"button_pressed":
                        getModel().isRollover()?"button_hover":"button_normal";
                    paintNine(g,sprite(state),0,0,getWidth(),getHeight(),
                        4,4,4,4,true);
                }finally{g.dispose();}
                super.paintComponent(incoming);
            }
        };
        result.setFont(new Font(Font.MONOSPACED,Font.BOLD,11));
        result.setForeground(INK);
        result.setFocusPainted(false);result.setRolloverEnabled(true);
        result.setContentAreaFilled(false);result.setOpaque(false);
        result.setBorderPainted(false);
        result.setMargin(new Insets(3,5,3,5));
        result.addActionListener(e->action.run());
        return result;
    }
    public JPanel panel(LayoutManager layout){
        return new JPanel(layout){
            @Override protected void paintComponent(Graphics incoming){
                Graphics2D g=(Graphics2D)incoming.create();
                try{paintNine(g,sprite("panel"),0,0,getWidth(),getHeight(),
                    4,4,4,4,true);}
                finally{g.dispose();}
            }
        };
    }
    private Border frameBorder(String name){
        return new Border(){
            public boolean isBorderOpaque(){return false;}
            public Insets getBorderInsets(Component c){return new Insets(5,5,5,5);}
            public void paintBorder(Component c,Graphics incoming,int x,int y,int w,int h){
                Graphics2D g=(Graphics2D)incoming.create();
                try{paintNine(g,sprite(name),x,y,w,h,4,4,4,4,false);}
                finally{g.dispose();}
            }
        };
    }
    /** Apply locally; do not alter global Swing LookAndFeel or other dialogs. */
    public void style(Component component){
        if(component instanceof JLabel label){
            label.setForeground(INK);
            label.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        }else if(component instanceof JTextField field){
            field.setForeground(INK);field.setBackground(FIELD);
            field.setCaretColor(INK);
            field.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
            field.setBorder(frameBorder("line_edit"));
        }else if(component instanceof JList<?> list){
            list.setBackground(FIELD);list.setForeground(INK);
            list.setSelectionBackground(new Color(101,60,48));
            list.setSelectionForeground(INK);
            list.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        }else if(component instanceof JComboBox<?> combo){
            combo.setBackground(FIELD);combo.setForeground(INK);
            combo.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        }else if(component instanceof JSpinner spinner){
            spinner.setBackground(FIELD);spinner.setForeground(INK);
            spinner.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        }else if(component instanceof JScrollPane scroll){
            scroll.setBackground(BACKDROP);
            scroll.getViewport().setBackground(FIELD);
            scroll.setBorder(frameBorder("window"));
        }else if(component instanceof JPanel pane && !(pane.getClass().isAnonymousClass())){
            pane.setBackground(BACKDROP);pane.setForeground(INK);
        }
        if(component instanceof Container parent)
            for(Component child:parent.getComponents())style(child);
    }
    public static void main(String[] args){
        PsxHorrorSwingSkin skin=new PsxHorrorSwingSkin();
        if(skin.sourceSprites()!=8)throw new IllegalStateException("Swing PSX theme missing source");
        System.out.println("PASS PSX_HORROR_SWING_EDITOR_SKIN originalNineSliceSprites="+
            skin.sourceSprites()+" source="+PsxHorrorUiSource.SHA256);
    }
}
