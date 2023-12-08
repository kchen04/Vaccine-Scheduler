package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

public class Appointments {

    private int id;
    private String patient;
    private String careGiver;
    private Date date;
    private String vaccine;

    public Appointments(int id, String patient, String careGiver, Date date, String vaccine) {
        this.id = id;
        this.patient = patient;
        this.careGiver = careGiver;
        this.date = date;
        this.vaccine = vaccine;
    }

    public Appointments() {}

    public String getPatient() {
        return patient;
    }

    public String getCareGiver() {
        return careGiver;
    }

    public Date getDate() {
        return date;
    }

    public String getVaccine() {
        return vaccine;
    }

    public int getID() {
        return id;
    }

    public int getMaxID() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getMaxID = "SELECT MAX(id) FROM Appointments";
        try {
            PreparedStatement statement = con.prepareStatement(getMaxID);
            ResultSet resultSet = statement.executeQuery();

            int id = 0;
            if (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when getting Max ID!");
        } finally {
            cm.closeConnection();
        }
    }

    public int addAppointment(String patient, String careGiver, Date date, String vaccine) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setDate(1, date);
            statement.setString(2, vaccine);
            statement.setString(3, patient);
            statement.setString(4, careGiver);
            statement.executeUpdate();
            return getMaxID();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when adding new appointment!");
        } finally {
            cm.closeConnection();
        }
    }

    public List<Appointments> showAppointments(String type, String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        List<Appointments> appointments = new ArrayList<>();

        String findAppointment;
        if (type.equals("Patient")) {
            findAppointment = "SELECT * FROM Appointments WHERE PatientName = ?";
        } else {
            findAppointment = "SELECT * FROM Appointments WHERE CaregiverName = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(findAppointment);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                java.sql.Date date = resultSet.getDate("Date");
                appointments.add(new Appointments(id, patient, caregiver, date, vaccine));
            }
            return appointments;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when showing appointment for user!");
        } finally {
            cm.closeConnection();
        }
    }

    public Appointments getInfo(int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String select = "SELECT * FROM Appointments WHERE id = ?";
        try {
            PreparedStatement selectStatement = con.prepareStatement(select);
            selectStatement.setInt(1, id);
            ResultSet resultSet = selectStatement.executeQuery();

            Appointments app = null;

            while (resultSet.next()) {
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                Date date = resultSet.getDate("Date");
                app = new Appointments(id, patient, caregiver, date, vaccine);
            }
            return app;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when getting appointment information!");
        } finally {
            cm.closeConnection();
        }
    }

    public void cancelAppointment(int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String cancel = "DELETE FROM Appointments WHERE id = ?";
        try {
            PreparedStatement cancelStatement = con.prepareStatement(cancel);
            cancelStatement.setInt(1, id);
            cancelStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when canceling appointment!");
        } finally {
            cm.closeConnection();
        }
    }

    public List<Integer> showAllID() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        List<Integer> list = new ArrayList<Integer>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT id FROM Appointments");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getInt("id"));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when searching for all ID!");
        } finally {
            cm.closeConnection();
        }
    }
}