import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
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
import java.awt.ScrollPane;
import javax.swing.JLabel;

public class Window {

	private JFrame frmMulticastPp;
	private JTextField textFieldSearch;
	private JButton btnSearch;
	private JButton btnRefresh;
	private JList listResults;
	MulticastP2P p2p;

	static // default values
	String path = "./files"; 
	static String ip = "224.0.2.10";
	static int controlPort = 8967;
	static int dataPort = 8966;
	static String hash = "SHA-256";

	/**
	 * Launch the application.
	 */
	public static void main(final String[] args) { //TODO see if final ok in arguments

		//arguments:  -p Path -i IP -c CONTROLPORT -d DATAPORT -h HashType
		if (args.length%2 != 0) {
			System.out.println("Usage: MulticastP2P -p Path -i IP -c CONTROLPORT -d DATAPORT -h HashType");
		}


		for (int i=0; i!=args.length; i+=2) {
			if (args[i].equalsIgnoreCase("-p")) {
				path = args[i+1];
			}

			else if (args[i].equalsIgnoreCase("-i")) {
				ip = args[i+1];
			}

			else if (args[i].equalsIgnoreCase("-c")) {
				controlPort = Integer.parseInt(args[i+1]);
			}

			else if (args[i].equalsIgnoreCase("-d")) {
				dataPort = Integer.parseInt(args[i+1]);
			}

			else if (args[i].equalsIgnoreCase("-h")) {
				hash = args[i+1];
			}

			else {
				System.out.println("Usage: MulticastP2P -p Path -i IP -c CONTROLPORT -d DATAPORT -h HashType");
				System.exit(-1);
			}
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {

					Window window = new Window();
					window.frmMulticastPp.setVisible(true);					
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
		p2p = new MulticastP2P();
		p2p.start(path, ip, controlPort, dataPort, hash);
		initialize();

		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		listResults = new JList(p2p.listModel);
		
		frmMulticastPp = new JFrame();
		frmMulticastPp.setResizable(false);
		frmMulticastPp.setTitle("Multicast P2P");
		frmMulticastPp.setBounds(100, 100, 663, 431);
		frmMulticastPp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmMulticastPp.getContentPane().setLayout(null);
		

		listResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listResults.setBounds(10, 243, 627, 125);
		frmMulticastPp.getContentPane().add(listResults);
		
		textFieldSearch = new JTextField();
		textFieldSearch.setBounds(10, 11, 305, 20);
		frmMulticastPp.getContentPane().add(textFieldSearch);
		textFieldSearch.setColumns(10);
		

		
		final JButton btnGet = new JButton("Download Selected");
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
		btnGet.setBounds(489, 216, 148, 23);
		frmMulticastPp.getContentPane().add(btnGet);
		
		JTextArea textAreaConsole = p2p.console;
		textAreaConsole.setBounds(10, 11, 627, 130);
		frmMulticastPp.getContentPane().add(textAreaConsole);
		

		
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
		btnSearch.setBounds(325, 10, 89, 23);
		frmMulticastPp.getContentPane().add(btnSearch);
	
		// refresh button
		btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnGet.setEnabled(true);
				new Thread() {
					public void run() {
						p2p.indexFiles();
					}
				}.start(); // Starts a thread that indexes local files again
			}
		});
		btnRefresh.setBounds(425, 10, 89, 23);
		frmMulticastPp.getContentPane().add(btnRefresh);
		
		
		
		JScrollPane scrollPaneResults = new JScrollPane(listResults);
		scrollPaneResults.setBounds(10, 61, 627, 145);
		frmMulticastPp.getContentPane().add(scrollPaneResults);
		
		JScrollPane scrollPaneConsole = new JScrollPane(p2p.console);
		scrollPaneConsole.setBounds(10, 250, 627, 132);
		frmMulticastPp.getContentPane().add(scrollPaneConsole);
		
		JLabel lblSearchResults = new JLabel("Search Results:");
		lblSearchResults.setBounds(10, 42, 117, 14);
		frmMulticastPp.getContentPane().add(lblSearchResults);
		
		JLabel lblConsole = new JLabel("Console:");
		lblConsole.setBounds(10, 231, 89, 14);
		frmMulticastPp.getContentPane().add(lblConsole);
		

		
		

	}
}
