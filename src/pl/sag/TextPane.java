package pl.sag;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTextArea;


public class TextPane extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 712368129156703980L;
	private JTextArea text;

	public TextPane(String name) {
		super(false);
		this.setName(name);
        text = new JTextArea();
        text.setAlignmentX(CENTER_ALIGNMENT);
        text.setAlignmentY(CENTER_ALIGNMENT);
        this.setLayout(new GridLayout(1, 1));
        this.add(text);
        this.setText("");
    }
	
	public void setColor(Color color) {
		text.setBackground(color);
	}
	
	public void setText(String string) {
		text.setText(getName().toUpperCase() + '\n' + string);
	}
	
	public void clear() {
		text.setText(getName().toUpperCase() + "\n\n");
	}
	
	public void append(String string) {
		text.append(string + '\n');
	}
}
