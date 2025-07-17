import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class CollegeManagementSystem extends JFrame {
    static Connection conn;

    CollegeManagementSystem() {
        setTitle("College Management System");
        setSize(600, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLayout(new GridLayout(7, 1, 10, 10));

        JButton studentBtn = new JButton("Manage Students");
        JButton courseBtn = new JButton("Manage Courses");
        JButton facultyBtn = new JButton("Manage Faculty");
        JButton attendanceBtn = new JButton("Manage Attendance");
        JButton viewAttendanceBtn = new JButton("View Attendance");
        JButton studentDetailBtn = new JButton("Student Details");
        JButton exitBtn = new JButton("Exit");

        add(studentBtn);
        add(courseBtn);
        add(facultyBtn);
        add(attendanceBtn);
        add(viewAttendanceBtn);
        add(studentDetailBtn);
        add(exitBtn);

        studentBtn.addActionListener(e -> showActionDialog("Student", new String[]{"name", "roll_no", "course"}, "students"));
        courseBtn.addActionListener(e -> showActionDialog("Course", new String[]{"code", "title", "credits"}, "courses"));
        facultyBtn.addActionListener(e -> new FacultyFrame(conn));
        attendanceBtn.addActionListener(e -> new AttendanceFrame(conn));
        viewAttendanceBtn.addActionListener(e -> new ViewAttendanceFrame(conn));
        studentDetailBtn.addActionListener(e -> new StudentDetailsFrame(conn));
        exitBtn.addActionListener(e -> System.exit(0));

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/college1", "root", "Admin");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Connection Failed: " + e.getMessage());
        }

        setVisible(true);
    }

    void showActionDialog(String entity, String[] columns, String tableName) {
        String[] options = {"Add", "Update", "Delete"};
        int choice = JOptionPane.showOptionDialog(this, "What would you like to do with " + entity + "?", entity + " Management",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice >= 0) {
            new ManagementFrame(conn, entity + " Management", tableName, columns, options[choice]);
        }
    }

    public static void main(String[] args) {
        new CollegeManagementSystem();
    }
}

class ManagementFrame extends JFrame {
    JTextField[] fields;
    DefaultTableModel model;
    JTable table;
    String tableName;
    String[] columns;
    String action;
    Connection conn;

    ManagementFrame(Connection conn, String title, String tableName, String[] columns, String action) {
        this.conn = conn;
        this.tableName = tableName;
        this.columns = columns;
        this.action = action;

        setTitle(title + " - " + action);
        setSize(600, 400);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JPanel formPanel = new JPanel(new GridLayout(columns.length + 1, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder(title + " Details"));

        fields = new JTextField[columns.length];
        for (int i = 0; i < columns.length; i++) {
            formPanel.add(new JLabel(columns[i].substring(0, 1).toUpperCase() + columns[i].substring(1) + ":"));
            fields[i] = new JTextField();
            formPanel.add(fields[i]);
        }

        JButton actionBtn = new JButton(action);
        actionBtn.addActionListener(e -> performAction());
        formPanel.add(new JLabel());
        formPanel.add(actionBtn);

        add(formPanel, BorderLayout.NORTH);

        model = new DefaultTableModel();
        String[] tableHeaders = new String[columns.length + 1];
        tableHeaders[0] = "ID";
        for (int i = 0; i < columns.length; i++) {
            tableHeaders[i + 1] = columns[i].substring(0, 1).toUpperCase() + columns[i].substring(1);
        }
        model.setColumnIdentifiers(tableHeaders);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        loadTable();

        setVisible(true);
    }

    void performAction() {
        try {
            if (action.equals("Add")) {
                StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " (");
                for (String col : columns) query.append(col).append(",");
                query.setLength(query.length() - 1);
                query.append(") VALUES (");
                for (int i = 0; i < columns.length; i++) query.append("?,");
                query.setLength(query.length() - 1);
                query.append(")");

                PreparedStatement ps = conn.prepareStatement(query.toString());
                for (int i = 0; i < fields.length; i++) ps.setString(i + 1, fields[i].getText());
                ps.executeUpdate();

            } else if (action.equals("Update")) {
                int selected = table.getSelectedRow();
                if (selected == -1) {
                    JOptionPane.showMessageDialog(this, "Select a row to update.");
                    return;
                }
                int id = (int) model.getValueAt(selected, 0);
                StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
                for (String col : columns) query.append(col).append(" = ?,");

                query.setLength(query.length() - 1);
                query.append(" WHERE id = ?");

                PreparedStatement ps = conn.prepareStatement(query.toString());
                for (int i = 0; i < fields.length; i++) ps.setString(i + 1, fields[i].getText());
                ps.setInt(fields.length + 1, id);
                ps.executeUpdate();

            } else if (action.equals("Delete")) {
                int selected = table.getSelectedRow();
                if (selected == -1) {
                    JOptionPane.showMessageDialog(this, "Select a row to delete.");
                    return;
                }
                int id = (int) model.getValueAt(selected, 0);
                String query = "DELETE FROM " + tableName + " WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            loadTable();
            for (JTextField field : fields) field.setText("");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void loadTable() {
        try {
            model.setRowCount(0);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            while (rs.next()) {
                Object[] row = new Object[columns.length + 1];
                row[0] = rs.getInt("id");
                for (int i = 0; i < columns.length; i++) {
                    row[i + 1] = rs.getString(columns[i]);
                }
                model.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }
}



class FacultyFrame extends JFrame {
    Connection conn;
    JTextField nameField, emailField, departmentField;
    JComboBox<String> courseBox;
    DefaultTableModel model;
    JTable table;

    FacultyFrame(Connection conn) {
        this.conn = conn;
        setTitle("Manage Faculty");
        setSize(600, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Faculty Details"));

        nameField = new JTextField();
        emailField = new JTextField();
        departmentField = new JTextField();
        courseBox = new JComboBox<>();

        loadCourses();

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Department:"));
        panel.add(departmentField);
        panel.add(new JLabel("Course:"));
        panel.add(courseBox);

        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Add"), updateBtn = new JButton("Update"), deleteBtn = new JButton("Delete");
        btnPanel.add(addBtn); btnPanel.add(updateBtn); btnPanel.add(deleteBtn);
        panel.add(new JLabel()); panel.add(btnPanel);

        add(panel, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Department", "Course"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int i = table.getSelectedRow();
                nameField.setText(model.getValueAt(i, 1).toString());
                emailField.setText(model.getValueAt(i, 2).toString());
                departmentField.setText(model.getValueAt(i, 3).toString());
                courseBox.setSelectedItem(model.getValueAt(i, 4).toString());
            }
        });

        loadFaculty();

        addBtn.addActionListener(e -> {
            try {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO faculty (name, email, department, course) VALUES (?, ?, ?, ?)");
                ps.setString(1, nameField.getText());
                ps.setString(2, emailField.getText());
                ps.setString(3, departmentField.getText());
                ps.setString(4, (String) courseBox.getSelectedItem());
                ps.executeUpdate();
                loadFaculty();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to update.");
                return;
            }
            try {
                int id = (int) model.getValueAt(row, 0);
                PreparedStatement ps = conn.prepareStatement("UPDATE faculty SET name=?, email=?, department=?, course=? WHERE id=?");
                ps.setString(1, nameField.getText());
                ps.setString(2, emailField.getText());
                ps.setString(3, departmentField.getText());
                ps.setString(4, (String) courseBox.getSelectedItem());
                ps.setInt(5, id);
                ps.executeUpdate();
                loadFaculty();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row to delete.");
                return;
            }
            try {
                int id = (int) model.getValueAt(row, 0);
                PreparedStatement ps = conn.prepareStatement("DELETE FROM faculty WHERE id=?");
                ps.setInt(1, id);
                ps.executeUpdate();
                loadFaculty();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        setVisible(true);
    }

    void loadCourses() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT title FROM courses");
            courseBox.removeAllItems();
            while (rs.next()) {
                courseBox.addItem(rs.getString("title"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading courses: " + e.getMessage());
        }
    }

    void loadFaculty() {
        try {
            model.setRowCount(0);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM faculty");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("department"),
                        rs.getString("course")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading faculty: " + e.getMessage());
        }
    }
}



class StudentDetailsFrame extends JFrame {
    Connection conn;
    JTextField rollField;
    JTextArea resultArea;

    StudentDetailsFrame(Connection conn) {
        this.conn = conn;
        setTitle("Student Details");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        rollField = new JTextField();
        JButton searchBtn = new JButton("Search");

        panel.add(new JLabel("Enter Roll No:"));
        panel.add(rollField);
        panel.add(new JLabel(""));
        panel.add(searchBtn);

        add(panel, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> fetchStudent());

        setVisible(true);
    }

    void fetchStudent() {
    try {
        String roll = rollField.getText().trim();

        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rsColumns = meta.getColumns(null, null, "students", "roll_no");

        if (!rsColumns.next()) {
            resultArea.setText("Error: 'roll_no' column not found in 'students' table.\nCheck your database schema.");
            return;
        }

        PreparedStatement ps = conn.prepareStatement("SELECT * FROM students WHERE roll_no = ?");
        ps.setString(1, roll);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            resultArea.setText("Name: " + rs.getString("name") + "\nCourse: " + rs.getString("course"));
        } else {
            resultArea.setText("No student found with that Roll Number.");
        }
    } catch (SQLException e) {
        resultArea.setText("Error: " + e.getMessage());
    }
}

}



class AttendanceFrame extends JFrame {
    Connection conn;
    JComboBox<String> studentBox, courseBox;
    JRadioButton presentBtn, absentBtn;
    ButtonGroup statusGroup;
    JButton markBtn;

    AttendanceFrame(Connection conn) {
        this.conn = conn;
        setTitle("Mark Attendance");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 2, 10, 10));

        studentBox = new JComboBox<>();
        courseBox = new JComboBox<>();

        presentBtn = new JRadioButton("Present");
        absentBtn = new JRadioButton("Absent");

        statusGroup = new ButtonGroup();
        statusGroup.add(presentBtn);
        statusGroup.add(absentBtn);
        presentBtn.setSelected(true);

        markBtn = new JButton("Mark Attendance");

        add(new JLabel("Select Student:"));
        add(studentBox);
        add(new JLabel("Select Course:"));
        add(courseBox);

        add(new JLabel("Select Status:"));

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radioPanel.add(presentBtn);
        radioPanel.add(absentBtn);
        add(radioPanel); 

        add(new JLabel(""));
        add(markBtn);

        loadStudents();
        loadCourses();

        markBtn.addActionListener(e -> markAttendance());

        setVisible(true);
    }

    void loadStudents() {
        try {
            studentBox.removeAllItems();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM students");
            while (rs.next()) {
                studentBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading students: " + e.getMessage());
        }
    }

    void loadCourses() {
        try {
            courseBox.removeAllItems();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT title FROM courses");
            while (rs.next()) {
                courseBox.addItem(rs.getString("title"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading courses: " + e.getMessage());
        }
    }

    void markAttendance() {
        try {
            String student = (String) studentBox.getSelectedItem();
            String course = (String) courseBox.getSelectedItem();
            String status = presentBtn.isSelected() ? "Present" : "Absent"; 

            PreparedStatement ps = conn.prepareStatement("INSERT INTO attendance "
                    + "(student_name, course_title, date, status) VALUES (?, ?, CURRENT_DATE(), ?)");
            ps.setString(1, student);
            ps.setString(2, course);
            ps.setString(3, status);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Attendance marked successfully!");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error marking attendance: " + e.getMessage());
        }
    }
}



class ViewAttendanceFrame extends JFrame {
    Connection conn;
    JTable table;
    DefaultTableModel model;

    ViewAttendanceFrame(Connection conn) {
        this.conn = conn;
        setTitle("View Attendance Records");
        setSize(600, 400);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(new String[]{"ID", "Student", "Course", "Date", "Status"}, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        loadAttendance();

        setVisible(true);
    }

    void loadAttendance() {
        try {
            model.setRowCount(0);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM attendance");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("student_name"),
                        rs.getString("course_title"),
                        rs.getDate("date"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading attendance: " + e.getMessage());
        }
    }
}
