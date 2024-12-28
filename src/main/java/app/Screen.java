package app;

import app.dto.FileDTO;
import app.manager.DownloadManager;
import app.manager.FileManager;
import app.manager.NetworkManager;
import app.socketHandler.BroadcastSocketHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Screen extends JFrame {
    private static Screen instance;

    private JPanel mainPanel;

    private JTextField txtRootFolder;
    private JTextField txtDestinationFolder;

    private JButton btnSetRoot;
    private JButton btnSetDestination;

    private JCheckBox chkOnlyRoot;

    public JList<String> excludeFoldersList;
    private JButton btnAddFolder;
    private JButton btnDelFolder;

    public JList<String> excludeMasksList;
    private JButton btnAddMask;
    private JButton btnDelMask;

    protected DefaultListModel<FileDTO> downloadFilesModel;
    private JList<FileDTO> downloadingFilesList;

    protected DefaultListModel<FileDTO> foundFilesModel;
    private JList<FileDTO> foundFilesList;

    private JTextField txtSearch;
    private JButton btnSearch;

    private GridBagConstraints gbc;

    public static Screen getInstance() {
        if(instance == null) {
            instance = new Screen();
        }
        return instance;
    }

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
            NetworkManager.getInstance().connect();
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

        JMenuItem addManuelPeer = new JMenuItem("Add Manuel app.Peer");
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

            if (!NetworkManager.getInstance().isConnected()) {
                JOptionPane.showMessageDialog(null, "Please First Connect", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtRootFolder.setText(fileChooser.getSelectedFile().getAbsolutePath());
                FileManager.getInstance().setRootFolder(fileChooser.getSelectedFile());
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

        btnSetDestination.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtDestinationFolder.setText(fileChooser.getSelectedFile().getAbsolutePath());
                FileManager.getInstance().setDestinationFolder(fileChooser.getSelectedFile());
            }
        });
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

        btnAddMask.addActionListener(e -> {
            String mask = JOptionPane.showInputDialog("Enter the mask to exclude:");
            if (mask == null) return;
            if (mask.isEmpty()) return;

            DefaultListModel<String> model = (DefaultListModel<String>) excludeMasksList.getModel();
            model.addElement(mask);

            mask = mask.replace(".", "\\.").replace("*", ".*");
            for (FileDTO file : NetworkManager.getInstance().getPeer().getUploadedFiles().values()){
                if (Pattern.matches(mask, file.filename())){
                    try {
                        String notify = "event=ENTRY_DELETE:filename=" + file.filename() + ":fileType=" + file.fileType() + ":fileSize=" + file.fileSize() + ":chunkCount=" + file.chunkCount() + ":hash=" + file.hash() + ":ip=" + NetworkManager.getInstance().getPeer().getIp() + ":port=" + NetworkManager.getInstance().getPeer().getPort();
                        NetworkManager.getInstance().getBroadcastSocketHandler().sendFileNotification(notify);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

        });

        btnDelMask.addActionListener(e -> {
            DefaultListModel<String> model = (DefaultListModel<String>) excludeMasksList.getModel();
            int index = excludeMasksList.getSelectedIndex();

            String mask = model.getElementAt(index);
            mask = mask.replace(".", "\\.").replace("*", ".*");

            for (FileDTO file : NetworkManager.getInstance().getPeer().getUploadedFiles().values()){
                if (Pattern.matches(mask, file.filename())){
                    try {
                        String notify = "event=ENTRY_CREATE:filename=" + file.filename() + ":fileType=" + file.fileType() + ":fileSize=" + file.fileSize() + ":chunkCount=" + file.chunkCount() + ":hash=" + file.hash() + ":ip=" + NetworkManager.getInstance().getPeer().getIp() + ":port=" + NetworkManager.getInstance().getPeer().getPort();
                        NetworkManager.getInstance().getBroadcastSocketHandler().sendFileNotification(notify);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            if (index != -1) {
                model.remove(index);
            }

        });
    }

    private void setupDownloadingPanel(){
        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));

        downloadFilesModel = new DefaultListModel<>();
        downloadingFilesList = new JList<>(downloadFilesModel);
        downloadingPanel.add(new JScrollPane(downloadingFilesList), BorderLayout.CENTER);

        startMonitoringDownloadedFiles();
        selectFileToOpen();

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.3; gbc.fill = GridBagConstraints.BOTH;

        mainPanel.add(downloadingPanel, gbc);
    }

    private void setupFoundFilesPanel(){
        JPanel foundPanel = new JPanel(new BorderLayout());
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));

        foundFilesModel  = new DefaultListModel<>();
        foundFilesList = new JList<>(foundFilesModel);
        foundPanel.add(new JScrollPane(foundFilesList), BorderLayout.CENTER);

        startMonitoringPeerFiles();
        selectFileToDownload();
        // Search
        setupSearchPanel(foundPanel);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.weighty = 0.3; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(foundPanel, gbc);

    }

    private void setupSearchPanel(JPanel foundPanel){
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

        btnSearch.addActionListener(e -> {
            String search = txtSearch.getText();

            DefaultListModel<FileDTO> searchResults = new DefaultListModel<>();
            for (int i = 0; i < foundFilesModel.size(); i++) {
                FileDTO file = foundFilesModel.get(i);
                if (file.filename().contains(search)) {
                    searchResults.addElement(file);
                }
            }

            if (searchResults.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No files found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            foundFilesList.setModel(searchResults);
        });
    }

    private void startMonitoringDownloadedFiles() {
        Timer timer = new Timer(1000, e -> updateDownloadingFilesList());
        timer.start();
    }

    private void updateDownloadingFilesList() {
        HashMap<String, FileDTO> downloadedFiles = NetworkManager.getInstance().getPeer().getDownloadedFiles();

        for (String file : downloadedFiles.keySet()) {
            if (!downloadFilesModel.contains(downloadedFiles.get(file))) {
                System.out.println("\nAdding file: " + downloadedFiles.get(file));
                downloadFilesModel.addElement(downloadedFiles.get(file));
            }
        }

        for (int i = 0; i < downloadFilesModel.size(); i++) {
            FileDTO fileInModel = downloadFilesModel.get(i);
            if (!downloadedFiles.containsKey(fileInModel.hash())) {
                downloadFilesModel.remove(i);
                continue;
            }

            if(downloadedFiles.containsKey(fileInModel.hash()) && !downloadedFiles.get(fileInModel.hash()).filename().equals(fileInModel.filename())){
                downloadFilesModel.set(i, downloadedFiles.get(fileInModel.hash()));
            }
        }
    }

    private void openFile(String path){
        try {
            System.out.println(path);
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("File does not exist: " + path);
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                System.err.println("Desktop is not supported on this system.");
            }
        } catch (IOException e) {
            System.err.println("Error opening the file: " + e.getMessage());
        }
    }

    private void selectFileToOpen(){
        downloadingFilesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = downloadingFilesList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        FileDTO selectedFile = downloadingFilesList.getModel().getElementAt(index);
                        String fullPath = FileManager.getInstance().getDestinationFolder() + File.separator + selectedFile.filename();
                        openFile(fullPath);
                    }
                }
            }
        });
    }

    private void startMonitoringPeerFiles() {
        Timer timer = new Timer(1000, e -> updateFoundFilesList());
        timer.start();
    }

    private void updateFoundFilesList() {
        HashMap<String, FileDTO> peerFiles = NetworkManager.getInstance().getPeer().getFiles();

        for (String file : peerFiles.keySet()) {
            if (!foundFilesModel.contains(peerFiles.get(file))) {
                System.out.println("Adding file: " + peerFiles.get(file));
                foundFilesModel.addElement(peerFiles.get(file));
            }
        }

        for (int i = 0; i < foundFilesModel.size(); i++) {
            FileDTO fileInModel = foundFilesModel.get(i);
            if (!peerFiles.containsKey(fileInModel.hash())) {
                foundFilesModel.remove(i);
                continue;
            }

            if(peerFiles.containsKey(fileInModel.hash()) && !peerFiles.get(fileInModel.hash()).filename().equals(fileInModel.filename())){
                foundFilesModel.set(i, peerFiles.get(fileInModel.hash()));
            }
        }
    }

    private void selectFileToDownload(){
        foundFilesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    if (FileManager.getInstance().getDestinationFolder() == null) {
                        JOptionPane.showMessageDialog(null, "Please set the destination folder first.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int index = foundFilesList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        FileDTO selectedFile = foundFilesList.getModel().getElementAt(index);

                        if(!NetworkManager.getInstance().getPeer().getOwnedChunks().containsKey(selectedFile.hash())) {
                            String[] chunks = new String[selectedFile.chunkCount()];
                            Arrays.fill(chunks, "");
                            NetworkManager.getInstance().getPeer().getOwnedChunks().put(selectedFile.hash(), chunks);
                        }
                        else {
                            System.out.println("Chunks already exist for file: " + selectedFile);
                            return;
                        }

                        JDialog progressDialog = new JDialog((Frame) null, "Downloading: " + selectedFile.filename(), false);
                        progressDialog.setSize(400, 100);
                        progressDialog.setLayout(new BorderLayout());

                        JLabel fileLabel = new JLabel("Downloading: " + selectedFile.filename());
                        JProgressBar downloadProgressBar = new JProgressBar(0, 100);
                        downloadProgressBar.setValue(0);
                        downloadProgressBar.setStringPainted(true);

                        progressDialog.add(fileLabel, BorderLayout.NORTH);
                        progressDialog.add(downloadProgressBar, BorderLayout.CENTER);
                        progressDialog.setLocationRelativeTo(null);
                        progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                        SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));

                        Thread downloadThread = new Thread(() -> {
                            try {
                                DownloadManager.getInstance().downloadFile(selectedFile);
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(null, "Error downloading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });

                        Thread progressThread = new Thread(() -> updateProgressBar(selectedFile, progressDialog, downloadProgressBar));

                        downloadThread.start();
                        progressThread.start();
                    }
                }
            }
        });
    }

    private void updateProgressBar(FileDTO file, JDialog progressDialog, JProgressBar downloadProgressBar) {
        while (true) {
            int percentage = FileManager.getInstance().getDownloadPercentage(file);
            SwingUtilities.invokeLater(() -> downloadProgressBar.setValue(percentage));
            if (percentage >= 100) {
                progressDialog.dispose();
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException interruptedException) {
                System.err.println("Error updating progress bar: " + interruptedException.getMessage());
            }
        }
    }
}
