package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Availabilities;
import scheduler.model.Appointments;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExists(username, "Caregiver")) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExists(username, "Caregiver")) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExists(String username, String type) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername;
        if (type.equals("Patient")) {
            selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        } else {
            selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentPatient != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.err.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.err.println("Please Try Again!");
            return;
        }
        try {

            Date date = Date.valueOf(tokens[1]);

            List<Availabilities> availabilities = new Availabilities().getAvailabilities(date);
            if (availabilities.size() == 0) {
                System.err.println("No Caregivers are available on this day:");
            } else {
                System.out.println("Caregivers available on this day:");

                for (Availabilities availability : availabilities) {
                    System.out.println("Caregiver name:" + availability.getUsername() + ", on Date:" + availability.getTime());
                }
                List<Vaccine> allVaccines = new Vaccine().getAll();
                if (allVaccines.size() == 0) {
                    System.err.println("Oops! No vaccines are on the market now!");
                } else {
                    System.out.println();
                    System.out.println("These are available doses of vaccines!");
                    for (Vaccine allVaccine : allVaccines) {
                        System.out.println(allVaccine);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Date");
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null) {
            System.err.println("Please login as a patient!");
            return;
        }
        if (tokens.length != 3) {
            System.err.println("Try Again");
            return;
        }
        String date = tokens[1];
        String vaccine = tokens[2];

        try {
            Appointments appointments = new Appointments();
            List<Appointments> patient = appointments.showAppointments("Patient", currentPatient.getUsername());
            if (patient.size() != 0) {
                System.err.println("Only one appointment per patient");
                return;
            }
            Date d = Date.valueOf(date);
            List<Availabilities> availabilities = new Availabilities().getAvailabilities(d);
            Vaccine vaccines1 = new Vaccine.VaccineGetter(vaccine).get();
            if (availabilities.size() == 0) {
                System.err.println("No Caregiver is available!");
                return;
            } else if (vaccines1 == null) {
                System.err.println("Vacccine non-existance");
                return;
            } else if (vaccines1.getAvailableDoses() == 0) {
                System.err.println("Not enough available doses!");
            } else {

                Random random = new Random();
                int index = random.nextInt(availabilities.size());
                String caregiver = availabilities.get(index).getUsername();

                vaccines1.decreaseAvailableDoses(1);

                new Availabilities().removeCaregiver(caregiver, d);

                int id = new Appointments().addAppointment(currentPatient.getUsername(), caregiver, d, vaccine);
                System.out.println("Your appointment id is:" + id + ", Your assigned caregiver is:" +
                        caregiver + ", Your selected vaccine is:" + tokens[2]);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Please Try Again!");
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.err.println("Please login your account first!");
            return;
        }
        if (tokens.length != 2) {
            System.err.println("Error: Try Again");
            return;
        }
        try {
            Appointments app = new Appointments();
            int id = Integer.parseInt(tokens[1]);
            List<Integer> integers = app.showAllID();
            if (!integers.contains(id)) {
                System.err.println("INVALID ID");
                return;
            }

            String type;
            String username;
            if (currentPatient != null) {
                type = "Patient";
                username = currentPatient.getUsername();
            } else {
                type = "Caregiver";
                username = currentCaregiver.getUsername();
            }
            List<Appointments> appointments = app.showAppointments(type, username);
            List<Integer> ids = new ArrayList<Integer>();
            for (Appointments appointment : appointments) {
                ids.add(appointment.getID());
            }
            if (!ids.contains(id)) {
                System.err.println("Appointment not Valid");
                return;
            } else {
                Appointments appointments1 = app.getInfo(id);
                app.cancelAppointment(id);
                new Availabilities().upLoadAvailability(appointments1.getCareGiver(), appointments1.getDate());
                Vaccine vaccines = new Vaccine.VaccineGetter(appointments1.getVaccine()).get();
                vaccines.increaseAvailableDoses(1);
                System.out.println("Delete Successful");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Try Again");
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.err.println("Please Login first!");
            return;
        }
        String type;
        String username;
        try {
            if (currentCaregiver != null) {
                type = "Caregiver";
                username = currentCaregiver.getUsername();
            } else {
                type = "Patient";
                username = currentPatient.getUsername();
            }

            List<Appointments> appointments = new Appointments().showAppointments(type, username);
            if (appointments.size() == 0) {
                System.err.println("No Upcoming Appointments");
            } else {
                System.out.println("Appointment List");
            }

            if (currentCaregiver != null) {
                for (Appointments appointment : appointments) {
                    System.out.println("    Appointment ID:" + appointment.getID() + "    Date:" + appointment.getDate() + "    Vaccine Name:" + appointment.getVaccine()
                            + "    Patient Name:" + appointment.getPatient());
                }
            } else {
                for (Appointments appointment : appointments) {
                    System.out.println("Appointment ID:" + appointment.getID() + "    Date:" + appointment.getDate() + "    Vaccine Name:" + appointment.getVaccine()
                            + "    Caregiver Name:" + appointment.getCareGiver());
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void logout(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.err.println("Please login first!");
            return;
        }

        if (currentCaregiver != null) {
            currentCaregiver = null;

        } else {
            currentPatient = null;
        }

        System.out.println("Successfully logged out!");
    }

}

