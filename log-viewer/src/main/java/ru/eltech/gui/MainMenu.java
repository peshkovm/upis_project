package ru.eltech.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import ru.eltech.core.WorkingWithFilesUtils;

class MainMenu {
  private JPanel menuPanel;
  private JPanel fileTreePanel;
  private JPanel fileContentPanel;
  private JPanel formPanel;
  private JTree fileTree;
  private JTextField folderLocationTextField;
  private JTextField findingTextTextField;
  private JButton folderLocationButton;
  private JLabel folderLocationLabel;
  private JLabel findingTextLabel;
  private JSeparator menuPanelSeparator;
  private JSeparator fileTreeFileContentTextPaneSeparator;
  private JTextField fileExtensionTextField;
  private JButton searchButton;
  private JScrollPane fileTreeScrollPane;
  private JTabbedPane fileContentTabbedPane;
  private JProgressBar fileTreeProgressBar;
  private JPanel fileTreeFileContentTextPaneSeparatorPanel;
  private JPanel searchAndCancelButtonsPanel;
  private JButton cancelButton;

  // Содержит пути файлов (нод) в дереве
  private Map<Path, DefaultMutableTreeNode> fileTreeMap;
  // Содержит пути нод в дереве, которые надо раскрыть
  private Set<TreePath> fileTreeExpandedPaths;
  // Содержит все открытые FileContentTableScrollPane
  // чтобы до них можно было доступиться и очистить занимаемые ресурсы
  private final java.util.List<FileContentTableScrollPane> fileContentTableScrollPaneList =
      new ArrayList<>();
  private final JFileChooser fileLocationChooser = new JFileChooser();
  private static final String FILE_TREE_ROOT_NAME = "Root";
  private static JFrame frame;
  private SwingWorker<Void, Path> worker;
  // Определеят нужно ли останавливать поиск файлов
  private boolean isFindFilesContainingTextCancelled = false;

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    SwingUtilities.invokeLater(
        () -> {
          frame = new JFrame("iFuture task 1");
          frame.setContentPane(new MainMenu().formPanel);
        });
  }

  private MainMenu() {
    createUIComponents();
  }

  private void createUIComponents() {
    initializeFrame();

    initializeFolderLocationButton();

    initializeFileTree();

    initializeSearchButton();

    initializeCancelButton();

    initializeFileContentPanel();

    initializeFileTreePanel();

    initializeFileTreeFileContentTextPaneSeparatorPanel();
  }

  private void initializeFrame() {
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1000, 900);
    frame.setMinimumSize(new Dimension(950, 855));
    // frame.setResizable(false);
    // frame.pack();
    frame.setVisible(true);

    frame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            System.out.println("Closed");
            fileContentTableScrollPaneList.stream()
                .map(FileContentTableScrollPane::getFileContentTable)
                .map(FileContentTable::getModel)
                .forEach(
                    model -> {
                      model.close();
                      System.out.println("model is closed");
                    });
            e.getWindow().dispose();
          }
        });
  }

  private void initializeSearchButton() {
    searchButton.addActionListener(
        e -> {
          if (folderLocationTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "File location must not be empty");
            return;
          } else if (findingTextTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Text to find must not be empty");
            return;
          } /* else if (fileExtensionTextField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "File extension must not be empty");
                return;
            }*/
          /* else if (!fileExtensionTextField.getText().startsWith(".")) {
              JOptionPane.showMessageDialog(frame, "File extension must start with .");
              return;
          }*/

          // fileContentTableModel.close();
          final File selectedFolder = new File(folderLocationTextField.getText());

          if (selectedFolder.exists() && selectedFolder.isDirectory()) {

            final String textToFind = findingTextTextField.getText();
            final String fileExtension = fileExtensionTextField.getText();

            java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/any_type.png");
            if (imgURL != null) {
              Icon imageIcon = new ImageIcon(imgURL);

              DefaultTreeCellRenderer renderer =
                  (DefaultTreeCellRenderer) fileTree.getCellRenderer();
              renderer.setLeafIcon(imageIcon);
            }

            fileTreeMap = new HashMap<>();
            fileTreeExpandedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

            DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
            ((DefaultMutableTreeNode) model.getRoot()).removeAllChildren();
            model.reload((DefaultMutableTreeNode) model.getRoot());

            // background task
            worker =
                new SwingWorker<Void, Path>() {

                  @Override
                  protected Void doInBackground() {
                    searchButton.setEnabled(false);
                    folderLocationButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    fileTreeProgressBar.setIndeterminate(true);

                    WorkingWithFilesUtils.findFilesContainingTextAndLazyFillTree(
                        selectedFolder,
                        textToFind,
                        fileExtension,
                        this::publish,
                        () -> isFindFilesContainingTextCancelled);

                    return null;
                  }

                  // edt
                  @Override
                  protected void process(List<Path> nodes) {
                    if (!isDone()) nodes.forEach(filePath -> addNodeToFileTree(filePath));
                  }

                  @Override
                  protected void done() {
                    isFindFilesContainingTextCancelled = false;
                    fileTreeProgressBar.setIndeterminate(false);
                    folderLocationButton.setEnabled(true);
                    searchButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                  }
                };

            worker.execute();
            System.out.println("After fill file tree");
          }
        });
  }

  private void initializeCancelButton() {
    cancelButton.setEnabled(false);
    cancelButton.addActionListener(e -> isFindFilesContainingTextCancelled = true);
  }

  private void initializeFolderLocationButton() {
    folderLocationButton.addActionListener(
        e -> {
          fileLocationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

          int option = fileLocationChooser.showDialog(null, "Choose folder");

          if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileLocationChooser.getSelectedFile();
            folderLocationTextField.setText(selectedFolder.toString());
          }
        });
  }

  private void initializeFileTree() {
    // create the root node
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(FILE_TREE_ROOT_NAME);

    DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
    model.setRoot(root);
    fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    fileTree.addTreeExpansionListener(
        new TreeExpansionListener() {
          @Override
          public void treeExpanded(TreeExpansionEvent event) {
            fileTreeExpandedPaths.add(event.getPath());
            // System.out.println("expanded path:" + event.getPath().getLastPathComponent());
            // fileTreeExpandedPaths.keySet().forEach(System.out::println);
          }

          @Override
          public void treeCollapsed(TreeExpansionEvent event) {
            fileTreeExpandedPaths.remove(event.getPath());
            // System.out.println("collapsed path:" + event.getPath().getLastPathComponent());
            // fileTreeExpandedPaths.keySet().forEach(System.out::println);
          }
        });

    fileTree.addMouseListener(
        new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            // int selRow = fileTree.getRowForLocation(e.getX(), e.getY());

            TreePath selPath = fileTree.getPathForLocation(e.getX(), e.getY());

            int tabCount = fileContentTabbedPane.getTabCount();

            for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
              if (((ButtonTabComponent) fileContentTabbedPane.getTabComponentAt(tabIndex))
                  .getFilePath()
                  .equals(selPath)) {
                fileContentTabbedPane.setSelectedIndex(tabIndex);
                return;
              }
            }

            // if(selRow != -1) {
            if (selPath != null) {
              if (e.getClickCount() == 1) {
                fileTree.getSelectionModel().clearSelection();
              } else if (e.getClickCount() == 2) {
                if (fileTree.getModel().isLeaf(selPath.getLastPathComponent())) {

                  // String filePath = selPath.toString().replaceAll("\\]|\\[",
                  // "").replaceFirst(FILE_TREE_ROOT_NAME + ", ", "").replaceAll(", ",
                  // Matcher.quoteReplacement(File.separator));
                  String filePath = "";

                  for (int pathCompIndex = 1;
                      pathCompIndex < selPath.getPathCount();
                      pathCompIndex++) {
                    filePath = filePath.concat(selPath.getPathComponent(pathCompIndex).toString());
                    if (pathCompIndex < selPath.getPathCount() - 1)
                      filePath = filePath.concat(Matcher.quoteReplacement(File.separator));
                  }

                  System.out.println("filePath = " + filePath);

                  // Если нашлись файлы
                  if (!filePath.equals("")) {
                    BigFileTableModel fileContentTableModel = new BigFileTableModel(filePath);
                    FileContentTable fileContentTable = new FileContentTable(fileContentTableModel);

                    FileContentTableScrollPane fileContentTableScrollPane =
                        new FileContentTableScrollPane(fileContentTable);
                    fileContentTableScrollPane
                        .getFileContentTable()
                        .getModel()
                        .fireTableDataChanged();

                    ButtonTabComponent buttonTabComponent = new ButtonTabComponent(selPath);
                    buttonTabComponent.setBackground(null);

                    fileContentTabbedPane.add(fileContentTableScrollPane);
                    fileContentTabbedPane.setTabComponentAt(
                        fileContentTabbedPane.getTabCount() - 1, buttonTabComponent);
                    fileContentTabbedPane.setSelectedIndex(fileContentTabbedPane.getTabCount() - 1);

                    fileContentTableScrollPaneList.add(fileContentTableScrollPane);

                    for (int i = 0; i < fileContentTableScrollPaneList.size(); i++) {
                      System.out.println(
                          "index = "
                              + i
                              + " pane = "
                              + ((ButtonTabComponent) fileContentTabbedPane.getTabComponentAt(i))
                                  .getFilePath());
                    }

                    // fileContentTable.changeSelection(100, 1, false, false);
                  }
                }
              }
            }
            // }
          }
        });

    // fileTree.setRootVisible(false);
  }

  private void initializeFileTreePanel() {
    fileTreePanel.setPreferredSize(new Dimension(300, -1));
    fileTreePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
  }

  private void initializeFileContentPanel() {
    fileContentPanel.setPreferredSize(new Dimension(600, -1));
    fileContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
  }

  private void initializeFileTreeFileContentTextPaneSeparatorPanel() {
    fileTreeFileContentTextPaneSeparatorPanel.setBorder(
        BorderFactory.createEmptyBorder(0, 0, 5, 0));
  }

  // Добавляет ноду в fileTree
  // Обходит дерево, начиная с имени файла,
  // тем самым экономя количество обращений к HashMap
  private DefaultMutableTreeNode addNodeToFileTree(final Path foundFile) {
    DefaultMutableTreeNode node =
        new DefaultMutableTreeNode(
            foundFile.getNameCount() == 0 ? foundFile : foundFile.getFileName());

    if (fileTreeMap.putIfAbsent(foundFile, node) == null) {
      DefaultMutableTreeNode parentNode;
      DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();

      if (foundFile.getParent() != null) parentNode = addNodeToFileTree(foundFile.getParent());
      else {
        // Root of file tree
        parentNode = (DefaultMutableTreeNode) model.getRoot();
      }

      parentNode.add(node);

      model.reload(parentNode);
      fileTreeExpandedPaths.forEach(fileTree::expandPath);
    }

    return fileTreeMap.get(foundFile);
  }

  private class ButtonTabComponent extends JPanel {
    private final TreePath filePath;

    ButtonTabComponent(TreePath filePath) {
      this.filePath = filePath;
      String tabText = filePath.getLastPathComponent().toString();
      JLabel label = new JLabel(tabText);
      add(label);
      // add more space between the label and the button
      label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
      // tab button
      JButton button = new TabButton();
      add(button);
      // add more space to the top of the component
      // setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    private class TabButton extends JButton implements ActionListener {
      TabButton() {
        setContentAreaFilled(false);
        setFocusable(false);
        setBorder(BorderFactory.createEtchedBorder());
        setBorderPainted(false);
        setRolloverEnabled(true);
        int size = 17;
        setPreferredSize(new Dimension(size, size));

        java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/close.png");
        if (imgURL != null) {
          Icon closeIcon = new ImageIcon(imgURL);
          setIcon(closeIcon);
        }

        addActionListener(this);

        // paint tab border
        // TODO не работает
        addMouseListener(
            new MouseAdapter() {
              public void mouseEntered(MouseEvent e) {
                setBorderPainted(true);
              }

              public void mouseExited(MouseEvent e) {
                setBorderPainted(false);
              }
            });
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        int tabIndex = fileContentTabbedPane.indexOfTabComponent(ButtonTabComponent.this);
        System.out.println("close button index = " + tabIndex);
        if (tabIndex != -1) {
          fileContentTableScrollPaneList.get(tabIndex).getFileContentTable().getModel().close();
          fileContentTabbedPane.remove(tabIndex);
          fileContentTableScrollPaneList.remove(tabIndex);
        }
      }
    }

    TreePath getFilePath() {
      return filePath;
    }
  }

  private static class FileContentTableScrollPane extends JScrollPane {
    FileContentTableScrollPane(FileContentTable fileContentTable) {
      super(
          fileContentTable,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      final int FILE_MAX_SIZE = 1000;

      // Для файла размера больше 1000 при зажатии и перемещении
      // вертикального и горизонтального скролбаров соднржимое файла не будет грузиться.
      // Содержимое файла загрузится только при отпуске скролбара.
      // Благодаря этому, пользователь может быстро перемещатся по большому файлу
      // без тормозов скролбара.
      getVerticalScrollBar()
          .addMouseListener(
              new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                  if (fileContentTable.getModel().getFileSize() > FILE_MAX_SIZE) {
                    System.out.println("Released");
                    fileContentTable.getModel().isMousePressed = false;
                    fileContentTable.getModel().fireTableDataChanged();
                  }
                }
              });
      getVerticalScrollBar()
          .addMouseListener(
              new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                  if (fileContentTable.getModel().getFileSize() > FILE_MAX_SIZE) {
                    System.out.println("Pressed");
                    fileContentTable.getModel().isMousePressed = true;
                  }
                }
              });

      getHorizontalScrollBar()
          .addMouseListener(
              new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                  if (fileContentTable.getModel().getFileSize() > FILE_MAX_SIZE) {
                    System.out.println("Released");
                    fileContentTable.getModel().isMousePressed = false;
                    fileContentTable.getModel().fireTableDataChanged();
                  }
                }
              });
      getHorizontalScrollBar()
          .addMouseListener(
              new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                  if (fileContentTable.getModel().getFileSize() > FILE_MAX_SIZE) {
                    System.out.println("Pressed");
                    fileContentTable.getModel().isMousePressed = true;
                  }
                }
              });

      // getViewport().setBackground(Color.WHITE);
    }

    FileContentTable getFileContentTable() {
      return (FileContentTable) getViewport().getView();
    }
  }

  private static class FileContentTable extends JTable {
    FileContentTable(BigFileTableModel fileContentTableModel) {
      super(fileContentTableModel);

      setShowGrid(false);
      setIntercellSpacing(new Dimension(5, 0));
      setTableHeader(null);
      // getColumnModel().getColumn(0).setPreferredWidth(50);
      // getColumnModel().getColumn(0).setMaxWidth(50);
      setAutoscrolls(true);
      getColumnModel()
          .getColumn(0)
          .setCellRenderer(
              new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {
                  Component rendererComp =
                      super.getTableCellRendererComponent(
                          table, value, isSelected, hasFocus, row, column);

                  rendererComp.setBackground(new Color(228, 228, 228));
                  rendererComp.setForeground(new Color(128, 128, 128));
                  setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
                  setHorizontalAlignment(JLabel.CENTER);
                  return rendererComp;
                }
              });
      setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      adjustColumnsSize();
    }

    private void adjustColumnsSize() {
      for (int colIndex = 0; colIndex < 2; colIndex++) {
        TableColumn tableColumn = getColumnModel().getColumn(colIndex);
        int preferredWidth = tableColumn.getMinWidth();
        int row;

        if (colIndex == 0) row = getModel().getRowCount() - 1;
        else row = getModel().getNumOfLongestRow();

        System.out.println("longest row = " + row);

        TableCellRenderer cellRenderer = getCellRenderer(row, colIndex);
        Component c = prepareRenderer(cellRenderer, row, colIndex);
        int width = c.getPreferredSize().width + getIntercellSpacing().width;
        preferredWidth = Math.max(preferredWidth, width);

        tableColumn.setPreferredWidth(preferredWidth);
      }
    }

    @Override
    public BigFileTableModel getModel() {
      return (BigFileTableModel) super.getModel();
    }
  }
}
