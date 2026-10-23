package veil.gpu;

import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.EditorPalettePanel;
import org.beckenburg.editor.EditorProject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Thin project-specific Swing dock. The palette and transactional editing
 * history come from the separately published LevelEditorKit-Java17.jar.
 * The live libGDX viewport is NOT duplicated in Swing.
 */
public final class MaleficEditorDock implements AutoCloseable {
    public record View(double x,double z,double yaw){}
    private final MaleficEditorProject project;
    private final Supplier<View> view;
    private EditorHistory<MaleficEditorProject.Snapshot> history;
    private volatile String selectedId;
    private EditorProject.Asset sourceAsset;
    private JFrame frame;
    private JList<EditorProject.Node> objects;
    private DefaultListModel<EditorProject.Node> rows;
    private final JSpinner x=new JSpinner(new SpinnerNumberModel(66.0,-8192.0,8192.0,.25));
    private final JSpinner z=new JSpinner(new SpinnerNumberModel(66.0,-8192.0,8192.0,.25));
    private final JSpinner height=new JSpinner(new SpinnerNumberModel(0.0,-512.0,512.0,.25));
    private final JSpinner yaw=new JSpinner(new SpinnerNumberModel(0.0,-100000.0,100000.0,.1));
    private final JSpinner scale=new JSpinner(new SpinnerNumberModel(1.0,.05,12.0,.1));
    private JLabel status;
    public MaleficEditorDock(MaleficEditorProject project,Supplier<View> view){
        this.project=project;this.view=view;
        history=new EditorHistory<>(project,80);
    }
    public void open(){SwingUtilities.invokeLater(this::show);}
    private JPanel fields(){
        JPanel panel=new JPanel(new GridLayout(2,5,3,2));
        for(String name:List.of("X","Z","Height","Yaw","Scale"))
            panel.add(new JLabel(name));
        for(JSpinner spinner:List.of(x,z,height,yaw,scale))
            panel.add(spinner);
        return panel;
    }
    private double val(JSpinner s){return ((Number)s.getValue()).doubleValue();}
    private void operation(String description,Runnable action){
        try{
            action.run();
            refresh();
            status.setText(description+" ✓  (not saved)");
        }catch(RuntimeException failure){
            JOptionPane.showMessageDialog(frame,failure.getMessage(),"Editor",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    private void modify(){
        String selected=selectedId;
        if(selected==null||selected.equals("generated-city-A")){
            status.setText("Select an authored object, not protected world geometry");return;
        }
        operation("Transform",()->history.execute(()->project.transform(selected,
            val(x),val(z),val(yaw),val(scale),val(height))));
    }
    private void place(){
        if(sourceAsset==null){
            status.setText("Choose a model from the real asset palette");return;
        }
        View camera=view.get();
        operation("Placed",()->{
            final MaleficEditorProject.Placement[] result={null};
            history.execute(()->result[0]=project.add(
                MaleficEditorProject.modelFromAsset(sourceAsset),
                camera.x()+Math.cos(camera.yaw())*5,
                camera.z()+Math.sin(camera.yaw())*5,0,1,0));
            selectedId=result[0].id();
        });
    }
    private void delete(){
        String id=selectedId;
        if(id==null||id.equals("generated-city-A")){status.setText("Select an authored object");return;}
        operation("Deleted",()->{history.execute(()->project.delete(id));selectedId=null;});
    }
    private void save(){
        try{project.save();status.setText("Saved "+project.documentPath().getFileName());}
        catch(IOException failure){JOptionPane.showMessageDialog(frame,failure.getMessage(),
            "Cannot save editor document",JOptionPane.ERROR_MESSAGE);}
    }
    private void reload(){
        int result=JOptionPane.showConfirmDialog(frame,"Discard unsaved changes and reload?",
            "Reload scene",JOptionPane.YES_NO_OPTION);
        if(result!=JOptionPane.YES_OPTION)return;
        try{
            project.load();selectedId=null;history=new EditorHistory<>(project,80);
            refresh();status.setText("Reloaded authored layer");
        }catch(IOException failure){JOptionPane.showMessageDialog(frame,
            failure.getMessage(),"Cannot reload scene",JOptionPane.ERROR_MESSAGE);}
    }
    private JButton action(String name,Runnable callback){
        JButton control=new JButton(name);
        control.setMargin(new Insets(3,5,3,5));
        control.addActionListener(e->callback.run());
        return control;
    }
    private void show(){
        if(frame!=null){frame.toFront();return;}
        frame=new JFrame("LEVEL EDITOR KIT / THE MALEFIC VEIL");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setLayout(new BorderLayout(4,4));
        rows=new DefaultListModel<>();
        objects=new JList<>(rows);
        objects.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objects.addListSelectionListener(e->{
            if(e.getValueIsAdjusting()||objects.getSelectedValue()==null)return;
            selectedId=objects.getSelectedValue().id();
            if(selectedId.equals("generated-city-A"))return;
            MaleficEditorProject.Placement chosen=project.get(selectedId);
            x.setValue(chosen.x());z.setValue(chosen.z());
            height.setValue(chosen.elevation());yaw.setValue(chosen.yaw());
            scale.setValue(chosen.scale());
        });
        JPanel content=new JPanel(new BorderLayout(4,4));
        content.add(new JLabel("AUTHORING LAYER / ORIGINAL SOURCE MODELS"),BorderLayout.NORTH);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(objects),
            new EditorPalettePanel(project,asset->{
                sourceAsset=asset;
                status.setText("Selected asset: "+asset.label());
            }));
        split.setResizeWeight(.38);
        content.add(split,BorderLayout.CENTER);
        frame.add(content,BorderLayout.CENTER);
        JPanel inspector=new JPanel(new BorderLayout(3,3));
        inspector.add(fields(),BorderLayout.NORTH);
        JPanel controls=new JPanel(new GridLayout(2,4,3,3));
        controls.add(action("Place",this::place));
        controls.add(action("Apply",this::modify));
        controls.add(action("Delete",this::delete));
        controls.add(action("Undo",()->operation("Undo",()->{
            if(!history.undo())throw new IllegalStateException("Nothing to undo");
        })));
        controls.add(action("Redo",()->operation("Redo",()->{
            if(!history.redo())throw new IllegalStateException("Nothing to redo");
        })));
        controls.add(action("Save",this::save));
        controls.add(action("Reload",this::reload));
        controls.add(action("Camera",()->{
            View c=view.get();x.setValue(c.x());z.setValue(c.z());
        }));
        inspector.add(controls,BorderLayout.CENTER);
        status=new JLabel("Choose source model, then Place or select a layer object");
        inspector.add(status,BorderLayout.SOUTH);
        frame.add(inspector,BorderLayout.SOUTH);
        frame.setSize(410,690);
        frame.setLocationByPlatform(true);
        refresh();
        frame.setVisible(true);
    }
    private void refresh(){
        if(rows==null)return;
        List<EditorProject.Node> nodes=project.hierarchy();
        rows.clear();
        int selection=-1,index=0;
        for(EditorProject.Node n:nodes){
            rows.addElement(n);
            if(n.id().equals(selectedId))selection=index;
            index++;
        }
        if(selection>=0)objects.setSelectedIndex(selection);
    }
    /** Viewport picking targets only authored layer objects. */
    public void select(String id){
        SwingUtilities.invokeLater(()->{
            selectedId=id;refresh();if(frame!=null)frame.toFront();
        });
    }
    public void focus(){SwingUtilities.invokeLater(()->{if(frame!=null)frame.toFront();});}
    public void close(){
        SwingUtilities.invokeLater(()->{if(frame!=null){frame.dispose();frame=null;}});
    }
}
