import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.AbstractListModel;

public class Window {

	private JFrame frame;
	private JTextField textFieldSearch;
	private JButton btnSearch;
	private JList listResults;
	JTextArea textAreaConsole;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Window window = new Window();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		

		
		
	}

	/**
	 * Create the application.
	 */
	public Window() {
		initialize();


		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		final MulticastP2P p2p = new MulticastP2P();
		p2p.start();
		
		
		frame = new JFrame();
		frame.setBounds(100, 100, 663, 431);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		textFieldSearch = new JTextField();
		textFieldSearch.setBounds(10, 304, 305, 20);
		frame.getContentPane().add(textFieldSearch);
		textFieldSearch.setColumns(10);
		
		final JButton btnGet = new JButton("Get");
		btnGet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final int selected = listResults.getSelectedIndex();
				if(selected != -1){
					// Launches a new thread that is going to download the selected file;
					new Thread() {
						public void run() {
							p2p.getFile(selected);
						}
					}.start();

				}
			}
		});
		btnGet.setEnabled(false);
		btnGet.setBounds(424, 303, 89, 23);
		frame.getContentPane().add(btnGet);
		
		textAreaConsole = new JTextArea();
		textAreaConsole.setBounds(10, 11, 627, 130);
		frame.getContentPane().add(textAreaConsole);
		
		listResults = new JList(p2p.listModel);
		listResults.setVisibleRowCount(5);
		listResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listResults.setBounds(10, 152, 627, 125);
		frame.getContentPane().add(listResults);
		
		btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnGet.setEnabled(true);
				new Thread() {
					public void run() {
							p2p.search(textFieldSearch.getText());
							
					}
				}.start(); // Starts a thread that does a search
			}
		});
		btnSearch.setBounds(325, 303, 89, 23);
		frame.getContentPane().add(btnSearch);
		
		

	}
}
