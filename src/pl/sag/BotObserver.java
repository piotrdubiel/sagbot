package pl.sag;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Semaphore;


/**
 * GUI to observe the bot's internals : relations etc
 * 
 * @author alex
 *
 */
public class BotObserver implements Observer, ItemListener, ActionListener {
	
	private final String powerName;
	private final JTabbedPane tabbedPane;
	private HashMap<String, PowerStatsPane> powerInfoPanes;
	private final JCheckBox stepwiseCheckBox;
	private final JButton nextStepButton;
	private final Semaphore nextStepSemaphore;
	private final JLabel nextStepLabel;
	private final TextPane logPane;
	private final JFrame frame;

	public BotObserver(KnowledgeBase base, Semaphore nextStep) {
		this.nextStepSemaphore = nextStep;
		frame = new JFrame("BotObserver");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		powerInfoPanes = new HashMap<String, PowerStatsPane>();
		
		tabbedPane = new JTabbedPane();
		tabbedPane.setMinimumSize(new Dimension(800, 400));

		powerName = base.getPowerName();
		PowerStatsPane botPanel = new BotStatsPane(powerName);
		tabbedPane.addTab(botPanel.getName(), botPanel);
		powerInfoPanes.put(powerName, botPanel);
		
		for (String name : base.getOtherPowerNames()) {
			PowerStatsPane panel = new PowerStatsPane(name);
			tabbedPane.addTab(panel.getName(), panel);
			powerInfoPanes.put(panel.getName(), panel);
		}
		logPane = new TextPane("");
		JScrollPane scroll = new JScrollPane(logPane);
		tabbedPane.addTab("GameLog", scroll);
		
		JPanel controlPane = new JPanel();
		controlPane.add(new JLabel("Auto mode (if not, stepwise): "));
		stepwiseCheckBox = new JCheckBox();
		stepwiseCheckBox.addItemListener(this);
		controlPane.add(stepwiseCheckBox);
		nextStepButton = new JButton("Next step");
		nextStepButton.setActionCommand("step");
		nextStepButton.addActionListener(this);
		nextStepButton.setMnemonic(KeyEvent.VK_X);
		controlPane.add(nextStepButton);
		nextStepLabel = new JLabel("click button (Alt+x) to continue..");
		controlPane.add(nextStepLabel);
		controlPane.setMaximumSize(controlPane.getPreferredSize());
		
		Container contentPane = frame.getContentPane();
		SpringLayout layout = new SpringLayout();
		contentPane.setLayout(layout);
		frame.getContentPane().add(controlPane);
		frame.getContentPane().add(tabbedPane);
		layout.putConstraint(SpringLayout.WEST, controlPane,
				5,
				SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, controlPane,
				5,
				SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.EAST, contentPane,
				5,
				SpringLayout.EAST, controlPane);
		layout.putConstraint(SpringLayout.SOUTH, contentPane,
				5,
				SpringLayout.SOUTH, controlPane);
		
		layout.putConstraint(SpringLayout.WEST, tabbedPane,
				5,
				SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.NORTH, tabbedPane,
				5,
				SpringLayout.SOUTH, controlPane);
		layout.putConstraint(SpringLayout.EAST, contentPane,
				5,
				SpringLayout.EAST, tabbedPane);
		layout.putConstraint(SpringLayout.SOUTH, contentPane,
				5,
				SpringLayout.SOUTH, tabbedPane);
		frame.pack();
		
		Toolkit tk = Toolkit.getDefaultToolkit();  
		int xSize = ((int) tk.getScreenSize().getWidth());  
		int ySize = ((int) tk.getScreenSize().getHeight());  
		frame.setSize(xSize,ySize);  
		frame.setVisible(true);
	}
	
	public void log(String string) {
		logPane.append(string);
	}
	
	public void powerWon(String power) {
		log(power + " wins!");
		JOptionPane.showMessageDialog(frame, power + " wins!");
	}
	
	public boolean getAutoMode() {
		return !stepwiseCheckBox.isSelected();
	}
	
	@Override
	public void update(Observable object, Object param) {
		if (object instanceof KnowledgeBase) {
			KnowledgeBase base = (KnowledgeBase) object;
			for (String name : base.otherPowerNames) {
				PowerStatsPane pane = powerInfoPanes.get(name);
				PowerKnowledgeBase powerBase = base.getPowerKnowledge(name);
				pane.updateFrom(powerBase);
			}
			BotStatsPane pane = (BotStatsPane) powerInfoPanes.get(powerName);
			pane.updateFrom(base);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent event) {

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if ("step".equals(event.getActionCommand())) {
			if (nextStepSemaphore.availablePermits() <= 0) {
				nextStepSemaphore.release();
			}
	    }
	}
}