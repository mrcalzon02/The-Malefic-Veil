package veil;
import javax.swing.*;
import java.awt.*;
import java.util.function.*;

/** Compact live inventory and host-validated recipe browser. */
public final class FabricationPanel extends JPanel {
    private final Timer refresh;
    public FabricationPanel(Supplier<ArsenalSystems.View> view,IntConsumer craft,IntConsumer equip,IntConsumer faction,Runnable upgrade,Runnable build){
        super(new BorderLayout(8,8));setBorder(BorderFactory.createEmptyBorder(12,12,12,12));setPreferredSize(new Dimension(820,500));
        JLabel status=new JLabel();add(status,BorderLayout.NORTH);
        DefaultListModel<String> model=new DefaultListModel<>();for(var w:ArsenalCatalog.WEAPONS)model.addElement(w.name());for(int i=4;i<9;i++)model.addElement(ArsenalCatalog.MATERIALS[i]);
        JList<String> list=new JList<>(model);list.setSelectedIndex(0);JTextArea detail=new JTextArea();detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);detail.setMargin(new Insets(12,12,12,12));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(list),new JScrollPane(detail));split.setDividerLocation(260);add(split,BorderLayout.CENTER);
        JPanel controls=new JPanel(new GridLayout(2,4,6,6));JButton make=new JButton("Fabricate"),select=new JButton("Equip"),ammo=new JButton("Craft ammo"),bench=new JButton("Build bench"),up=new JButton("Upgrade bench");
        JComboBox<String> factions=new JComboBox<>(new String[]{"Choose faction","Foundry","Lumen","Veil"});controls.add(make);controls.add(select);controls.add(ammo);controls.add(bench);controls.add(up);controls.add(factions);controls.add(new JLabel("Use [ / ] to cycle owned guns"));add(controls,BorderLayout.SOUTH);
        make.addActionListener(e->craft.accept(list.getSelectedIndex()));select.addActionListener(e->{if(list.getSelectedIndex()<32)equip.accept(list.getSelectedIndex());});ammo.addActionListener(e->craft.accept(40));bench.addActionListener(e->build.run());up.addActionListener(e->upgrade.run());factions.addActionListener(e->{if(factions.getSelectedIndex()>0)faction.accept(factions.getSelectedIndex()-1);});
        Runnable update=()->{
            var v=view.get();if(v==null)return;int[] m=v.materials();StringBuilder materials=new StringBuilder();for(int i=0;i<4;i++)materials.append(ArsenalCatalog.MATERIALS[i]).append(' ').append(m[i]).append("   ");
            status.setText("Bench "+(v.benchTier()==0?"out of reach":v.benchTier())+"   "+(v.faction()<0?"No faction":ArsenalCatalog.FACTIONS[v.faction()])+"   "+materials);
            int i=list.getSelectedIndex();if(i<0)return;StringBuilder text=new StringBuilder();
            if(i<32){var w=ArsenalCatalog.get(i);boolean owned=(v.owned()&(1L<<i))!=0;text.append(w.name()).append(owned?" — OWNED\n\n":" — NOT OWNED\n\n");text.append(w.mapSpawn()?"Available as map weapon loot.\n":"Crafted at your base.\n");text.append("Bench tier: ").append(Math.max(1,w.tier())).append("\nFaction: ").append(w.faction()<0?"Shared":ArsenalCatalog.FACTIONS[w.faction()]);if(w.prerequisite()>=0)text.append("\nPrevious design: ").append(ArsenalCatalog.get(w.prerequisite()).name());text.append("\n\nPrimary: ").append(w.primary().kind()).append(" / ").append(w.primary().damage()).append(" damage × ").append(w.primary().pellets());text.append("\nAlternate: ").append(w.alternate().kind()).append(" / ").append(w.alternate().damage()).append(" damage × ").append(w.alternate().pellets());text.append("\n\nFabrication cost:\n");cost(text,w.cost(),m);select.setEnabled(owned);}
            else{text.append(ArsenalCatalog.MATERIALS[i-28]).append("\n\nComponent recipe:\n");cost(text,ArsenalCatalog.COMPONENT_COSTS[i-32],m);select.setEnabled(false);}
            text.append("\n\nA bench needs $150 and 12 scrap. Stand clear of roads, spawns and other structures before building. Higher-tier benches and components unlock specialist branches. Faction locks after the first specialist weapon is made.");detail.setText(text.toString());
        };
        list.addListSelectionListener(e->update.run());refresh=new Timer(350,e->update.run());refresh.start();update.run();
    }
    private static void cost(StringBuilder t,int[] cost,int[] have){for(int i=0;i<9;i++)if(cost[i]>0)t.append(ArsenalCatalog.MATERIALS[i]).append(": ").append(cost[i]).append(" (have ").append(have[i]).append(")\n");}
    @Override public void removeNotify(){refresh.stop();super.removeNotify();}
    @Override public void addNotify(){super.addNotify();if(refresh!=null)refresh.start();}
}
