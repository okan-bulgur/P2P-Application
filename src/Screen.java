package src;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Screen extends JFrame {

    private JPanel mainPanel;

    private JTextField txtRootFolder;
    private JTextField txtDestinationFolder;

    private JButton btnSetRoot;
    private JButton btnSetDestination;

    private JCheckBox chkOnlyRoot;

    private JList<String> excludeFoldersList;
    private JButton btnAddFolder;
    private JButton btnDelFolder;

    private JList<String> excludeMasksList;
    private JButton btnAddMask;
    private JButton btnDelMask;

    private JList<String> downloadingFilesList;
    private JList<String> foundFilesList;

    private JTextField txtSearch;
    private JButton btnSearch;

    private GridBagConstraints gbc;

    public Screen() {
        super("P2P File Sharing Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);
        setLocationRelativeTo(null);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);

        setupMenuBar();
        setupMainPanel();
    }

    private void setupMenuBar(){
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFiles = new JMenu("Files");
        JMenuItem menuItemConnect = new JMenuItem("Connect");
        JMenuItem menuItemDisconnect = new JMenuItem("Disconnect");
        JMenuItem menuItemExit = new JMenuItem("Exit");

        menuFiles.add(menuItemConnect);
        menuFiles.add(menuItemDisconnect);
        menuFiles.addSeparator();
        menuFiles.add(menuItemExit);

        JMenu menuHelp = new JMenu("Help");
        JMenuItem menuItemAbout = new JMenuItem("About");

        menuHelp.add(menuItemAbout);

        menuBar.add(menuFiles);
        menuBar.add(menuHelp);

        setJMenuBar(menuBar);

        menuItemConnect.addActionListener(e -> {
            try {
                NetworkManager.getInstance().connect();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        menuItemDisconnect.addActionListener(e -> {
            NetworkManager.getInstance().disconnect();
        });

        menuItemExit.addActionListener(e -> {
            NetworkManager.getInstance().disconnect();
            System.exit(0);
        });

        menuItemAbout.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "P2P File Sharing Application\n\nOkan Bulgur\n20200702017", "About", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem addManuelPeer = new JMenuItem("Add Manuel Peer");
        menuFiles.add(addManuelPeer);
        addManuelPeer.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog("Enter IP Address:");
            String port = JOptionPane.showInputDialog("Enter Port:");

            if (ip == null || port == null) return;
            if (ip.isEmpty() || port.isEmpty()) return;
            if (!port.matches("\\d+")) return;
            if (Integer.parseInt(port) < 0 || Integer.parseInt(port) > 65535) return;

            NetworkManager.getInstance().addManuelPeer(ip, Integer.parseInt(port));
        });

        JMenuItem showPeers = new JMenuItem("Show Peers");
        menuFiles.add(showPeers);
        showPeers.addActionListener(e -> NetworkManager.getInstance().showPeers());

    }

    private void setupMainPanel(){
        mainPanel = new JPanel(new GridBagLayout());

        setupSharedFolderPanel();
        setupDestinationFolderPanel();
        setupSettingsPanel();
        setupDownloadingPanel();
        setupFoundFilesPanel();

        setContentPane(mainPanel);
    }

    private void setupSharedFolderPanel(){

        // Root of the P2P shared folder

        JPanel sharedFolderPanel = new JPanel(new GridBagLayout());
        sharedFolderPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));

        txtRootFolder = new JTextField("C:\\My Shared Folder\\");
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.95; gbc.fill = GridBagConstraints.HORIZONTAL;
        sharedFolderPanel.add(txtRootFolder, gbc);

        btnSetRoot = new JButton("Set");
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.05; gbc.fill = GridBagConstraints.NONE;
        sharedFolderPanel.add(btnSetRoot, gbc);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(sharedFolderPanel, gbc);

        btnSetRoot.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtRootFolder.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }

            try {
                NetworkManager.getInstance().sendSearchRequest(fileChooser.getSelectedFile().getAbsolutePath().split(":")[1]);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

    }

    private void setupDestinationFolderPanel(){
        // Destination folder

        JPanel destFolderPanel = new JPanel(new GridBagLayout());
        destFolderPanel.setBorder(BorderFactory.createTitledBorder("Destination folder"));

        txtDestinationFolder = new JTextField("C:\\P2P Downloads\\");
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1 ; gbc.weightx = 0.95; gbc.fill = GridBagConstraints.HORIZONTAL;
        destFolderPanel.add(txtDestinationFolder, gbc);

        btnSetDestination = new JButton("Set");
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.05; gbc.fill = GridBagConstraints.NONE;
        destFolderPanel.add(btnSetDestination, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;

        mainPanel.add(destFolderPanel, gbc);
    }

    private void setupSettingsPanel(){
        // Settings
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5,5,5,5);

        setupFolderExclusionPanel(settingsPanel, sgbc);
        setupMaskExclusionPanel(settingsPanel, sgbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.BOTH;

        mainPanel.add(settingsPanel, gbc);
    }

    private void setupFolderExclusionPanel(JPanel settingsPanel, GridBagConstraints sgbc){
        // Folder exclusion
        JPanel exclusionPanel = new JPanel(new GridBagLayout());
        exclusionPanel.setBorder(BorderFactory.createTitledBorder("Folder exclusion"));
        chkOnlyRoot = new JCheckBox("Check new files only in the root", true);
        sgbc.gridx = 0; sgbc.gridy = 0; sgbc.gridwidth = 2; sgbc.weightx = 1.0; sgbc.fill = GridBagConstraints.HORIZONTAL;
        exclusionPanel.add(chkOnlyRoot, sgbc);

        // Exclude files under these folders
        JPanel folderExclusionPanel = new JPanel(new BorderLayout());
        folderExclusionPanel.setBorder(BorderFactory.createTitledBorder("Exclude files under these folders"));
        excludeFoldersList = new JList<>(new DefaultListModel<>());
        folderExclusionPanel.add(new JScrollPane(excludeFoldersList), BorderLayout.CENTER);

        JPanel folderButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        btnAddFolder = new JButton("Add");
        btnDelFolder = new JButton("Del");
        folderButtonPanel.add(btnAddFolder);
        folderButtonPanel.add(btnDelFolder);
        folderExclusionPanel.add(folderButtonPanel, BorderLayout.SOUTH);

        sgbc.gridx = 0; sgbc.gridy = 1; sgbc.gridwidth = 1; sgbc.weightx = 0.5; sgbc.weighty = 1.0; sgbc.fill = GridBagConstraints.BOTH;

        exclusionPanel.add(folderExclusionPanel, sgbc);
        settingsPanel.add(exclusionPanel, sgbc);
    }

    private void setupMaskExclusionPanel(JPanel settingsPanel, GridBagConstraints sgbc){
        // Exclude files matching these masks
        JPanel maskPanel = new JPanel(new BorderLayout());
        maskPanel.setBorder(BorderFactory.createTitledBorder("Exclude files matching these masks"));
        excludeMasksList = new JList<>(new DefaultListModel<>());
        maskPanel.add(new JScrollPane(excludeMasksList), BorderLayout.CENTER);

        JPanel maskInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        btnAddMask = new JButton("Add");
        btnDelMask = new JButton("Del");
        maskInputPanel.add(btnAddMask);
        maskInputPanel.add(btnDelMask);
        maskPanel.add(maskInputPanel, BorderLayout.SOUTH);

        sgbc.gridx = 1; sgbc.gridy = 1; sgbc.gridwidth = 1; sgbc.weightx = 0.5; sgbc.weighty = 1.0; sgbc.fill = GridBagConstraints.BOTH;

        settingsPanel.add(maskPanel, sgbc);
    }

    private void setupDownloadingPanel(){
        // Downloading files
        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));

        downloadingFilesList = new JList<>(new DefaultListModel<>());
        downloadingPanel.add(new JScrollPane(downloadingFilesList), BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.3; gbc.fill = GridBagConstraints.BOTH;

        mainPanel.add(downloadingPanel, gbc);
    }

    private void setupFoundFilesPanel(){
        // Found files

        JPanel foundPanel = new JPanel(new BorderLayout());
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));

        foundFilesList = new JList<>(new DefaultListModel<>());
        foundPanel.add(new JScrollPane(foundFilesList), BorderLayout.CENTER);

        // Search
        setupSearchPanel(foundPanel);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.3; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(foundPanel, gbc);

    }

    private void setupSearchPanel(JPanel foundPanel){
        // Search

        JPanel searchPanel = new JPanel(new GridBagLayout());

        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(5,5,5,5);

        txtSearch = new JTextField(20);
        sgbc.gridx = 0; sgbc.gridy = 0; sgbc.gridwidth = 1 ; sgbc.weightx = 0.95; sgbc.fill = GridBagConstraints.HORIZONTAL;
        searchPanel.add(txtSearch, sgbc);

        btnSearch = new JButton("Search");
        sgbc.gridx = 1; sgbc.gridy = 0; sgbc.gridwidth = 1; sgbc.weightx = 0.05; sgbc.fill = GridBagConstraints.HORIZONTAL;
        searchPanel.add(btnSearch, sgbc);

        foundPanel.add(searchPanel, BorderLayout.SOUTH);
    }
}
