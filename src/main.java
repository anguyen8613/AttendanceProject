import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JFileChooser;

//Both files needed to test this project is in the project folder. (outside of the src folder).

public class Main {

	public static void main(String[] args) {

		// 1. Create a new course (use student_roster.csv as a sample roster)
		createCourse();

		// 2. Load a course (use chemistryMW2-3.csv as a sample course)
		Course currentCourse = null;
		currentCourse = load();

		// 3. Take attendance
		takeAttendance(currentCourse);
		
		// 4. Send emails to parents for missing students
		sendEmail();

	}

	// This method will be called when the professor wished to create a new
	// Course.
	// it prompts the professor to upload a csv file and uses it to create a new
	// file
	// this new file will have the course info and student
	// roster(course.students). to be edited each attendance day
	public static void createCourse() {

		// Choose a correctly formatted CSV file to upload
		System.out.println("Upload CSV file");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		int result = fileChooser.showOpenDialog(fileChooser);

		File selectedFile = fileChooser.getSelectedFile();
		System.out.println("Selected file: " + selectedFile.getAbsolutePath());

		// Read from CSV File
		ArrayList<String> records = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
			String line;
			while ((line = reader.readLine()) != null) {
				records.add(line);
			}
			reader.close();

		} catch (Exception e) {
			System.err.format("Exception occurred trying to read '%s'.", selectedFile);
			e.printStackTrace();

		}

		// Prompt user to enter Course information
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter course name: ");
		String name = scanner.nextLine();
		System.out.println("Enter course number: ");
		String number = scanner.nextLine();
		System.out.println("Enter course days: ");
		String days = scanner.nextLine();
		System.out.println("Enter course time: ");
		String time = scanner.nextLine();

		// Crate a Course from above information
		Course currentCourse = new Course(name, number, days, time);

		// Add students into Course (students taken from the CSV File)
		for (int i = 1; i < records.size(); i++) {

			String[] tokens = records.get(i).split(",");
			Student newStudent = new Student(tokens[0], tokens[1], Integer.parseInt(tokens[2]), tokens[3]);
			currentCourse.students.add(newStudent);

		}

		// Select the Directory to put the Attendance Tracker File into. (We
		// will edit this file for every attendance taken)

		try {
			System.out.println("Select Save Directory");
			JFileChooser directory = new JFileChooser();

			directory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			directory.showSaveDialog(null);

			String path = directory.getSelectedFile().getAbsolutePath().toString() + "\\"
					+ currentCourse.getCourseName() + currentCourse.getDays() + currentCourse.getTime() + ".csv";
			System.out.println(path);
			File newfile = new File(path);
			FileWriter fw = new FileWriter(newfile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(currentCourse.getCourseName() + "," + currentCourse.getCourseNumber() + ","
					+ currentCourse.getDays() + "," + currentCourse.getTime());
			bw.newLine();
			for (int i = 0; i < currentCourse.students.size(); i++) {
				bw.write(currentCourse.students.get(i).getFirstName() + ",");
				bw.write(currentCourse.students.get(i).getLastName() + ",");
				bw.write(currentCourse.students.get(i).getId() + ",");
				bw.write(currentCourse.students.get(i).getGuardianEmail());
				bw.newLine();
			}
			bw.close();
		} catch (Exception e) {

		}

	} // end of createCourse

	public static Course load() {

		System.out.println("Select Course to Take Attendance");
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		int result = fileChooser.showOpenDialog(fileChooser);

		File selectedFile = null;

		selectedFile = fileChooser.getSelectedFile();
		System.out.println("Selected file: " + selectedFile.getAbsolutePath());

		// Read from CSV File
		ArrayList<String> records = new ArrayList<String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
			String line;
			while ((line = reader.readLine()) != null) {
				records.add(line);
			}

		} catch (Exception e) {
			System.err.format("Exception occurred trying to read '%s'.", selectedFile);
			e.printStackTrace();

		}

		String[] courseInfo = records.get(0).split(",");
		Course currentCourse = new Course(courseInfo[0], courseInfo[1], courseInfo[2], courseInfo[3]);
		if (courseInfo.length > 4) {
			for (int i = 4; i < courseInfo.length; i++) {
				String temp_date = courseInfo[i];
				String pattern = "MM/dd/yyyy";
				SimpleDateFormat format = new SimpleDateFormat(pattern);
				try {
					Date date = format.parse(temp_date);
					System.out.println(date);
					AttendanceRecord currentAttendanceRecord = new AttendanceRecord(date);
					currentCourse.attendanceRecords.add(currentAttendanceRecord);
					for (int j = 1; j < records.size(); j++) {
						String[] studentInfo = records.get(j).split(",");
						Student currentStudent = new Student(studentInfo[0], studentInfo[1],
								Integer.parseInt(studentInfo[2]), studentInfo[3]);
						String[] timeStamps = studentInfo[i].split("/");
						String delims = "[-,:,. ]+";

						String[] firstTimeStamp = timeStamps[0].split(delims);
						Long loginStamp = Long.parseLong(firstTimeStamp[0]
								.concat(firstTimeStamp[1].concat(firstTimeStamp[2].concat(firstTimeStamp[3].concat(
										firstTimeStamp[4].concat(firstTimeStamp[5].concat(firstTimeStamp[6])))))));
						Timestamp loginTime = new Timestamp(loginStamp);

						String[] secondTimeStamp = timeStamps[0].split(delims);
						Long logoutStamp = Long.parseLong(secondTimeStamp[0]
								.concat(secondTimeStamp[1].concat(secondTimeStamp[2].concat(secondTimeStamp[3].concat(
										secondTimeStamp[4].concat(secondTimeStamp[5].concat(secondTimeStamp[6])))))));
						Timestamp logoutTime = new Timestamp(logoutStamp);

						currentStudent.setLoginTime(loginTime);
						currentStudent.setLogoutTime(logoutTime);
						currentAttendanceRecord.students.add(currentStudent);
						currentCourse.attendanceRecords.add(currentAttendanceRecord);
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

		}
		System.out.println(records.size());

		for (int i = 1; i < records.size(); i++) {

			String[] tokens = records.get(i).split(",");
			Student newStudent = new Student(tokens[0], tokens[1], Integer.parseInt(tokens[2]), tokens[3]);
			currentCourse.students.add(newStudent);
			System.out.println(i);
		}

		return currentCourse;

	} // end of Course load

	public static void takeAttendance(Course currentCourse) {
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		Date date = new Date();
		dateFormat.format(date);
		AttendanceRecord currentAttendanceRecord = new AttendanceRecord(date);

		Student currentStudent = new Student();
		System.out.println("System in Login Mode");
		do {
			currentStudent = parse(currentCourse);
			if (currentStudent.getId() == 304999154) {
				break;
			}
			Timestamp loginTime = new Timestamp(System.currentTimeMillis());
			currentStudent.setLoginTime(loginTime);
			currentAttendanceRecord.students.add(currentStudent);

		} while (currentStudent.getId() != 304999154);

		System.out.println("System in Standby Mode");
		currentStudent = new Student();
		do {
			currentStudent = parse(currentCourse);
			if (currentStudent.getId() == 304999154) {
				break;
			}
			System.out.println("System in Standby Mode");

		} while (currentStudent.getId() != 304999154);
		System.out.println("System in Logout Mode");
		currentStudent = new Student();
		do {
			currentStudent = parse(currentCourse);
			if (currentStudent.getId() == 304999154) {
				break;
			}

			for (int i = 0; i < currentAttendanceRecord.students.size(); i++) {
				if (currentStudent.getId() == currentAttendanceRecord.students.get(i).getId()) {
					Timestamp logoutTime = new Timestamp(System.currentTimeMillis());
					currentAttendanceRecord.students.get(i).setLogoutTime(logoutTime);
				}
			}
		} while (currentStudent.getId() != 304999154);

		currentCourse.attendanceRecords.add(currentAttendanceRecord);

	} // end of takeAttendance()

	public static Student parse(Course currentCourse) {

		Administrator test = new Administrator("Name", "Mr", "Richards", "email", "richards", "ilovejose");
		// example: student card scan
		String student = "";
		do {
			System.out.println("scan now");
			Scanner scanner = new Scanner(System.in);
			student = scanner.nextLine();

			if (student.length() < 95) {
				System.out.println("Scan Failed!!!!");
			}

		} while (student.length() < 95);

		String firstName;
		String lastName;
		String temp_id;

		int number = 1;
		String delims = "[%,/,?,=,;,^, ]+";
		String[] tokens = student.split(delims);

		firstName = tokens[2];
		lastName = tokens[3];
		temp_id = tokens[6];
		int id = Integer.parseInt(temp_id.substring(7, temp_id.length()));

		Student currentStudent = new Student();
		for (int i = 0; i < currentCourse.students.size(); i++) {
			if (id == currentCourse.students.get(i).getId()) {
				currentStudent = currentCourse.students.get(i);
			}
		}

		System.out.println("firstName: " + firstName);
		System.out.println("lastName: " + lastName);

		System.out.println("id: " + id);

		return currentStudent;

	} // end of Student parse
	
	public static void sendEmail() {
		String studentFirstName = "Alvin";
		String studentLastName = "Nguyen";
		String parentEmail = "cesar_rojas714@icloud.com"; // change to your email for testing
		
		String adminEmail = "moloch714@gmail.com"; // gmail admin created for this project
		String adminPassword = "Barce@714"; // gmail admin password
		
		String to = parentEmail;
		String subject = "Attendance Alert"; //
		String message = studentFirstName + " " + studentLastName + " missed class today."; //default message
		final String userEmail = adminEmail;
		final String userPassword = adminPassword;
		
		Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");	
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        Session session = Session.getDefaultInstance(props,new Authenticator() 
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() 
            {
                return new PasswordAuthentication(userEmail, userPassword);
            }
        });
        
        try 
        {
            Message msg = new MimeMessage(session); // create message object
            
            msg.setFrom(new InternetAddress(userEmail)); // send from
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to)); //send to
            msg.setSubject(subject); // send default subject
            msg.setText(message); // send body message

            Transport.send(msg); // send message object
            
            System.out.println("email successfully sent");
            
        } catch (MessagingException e) {
            System.out.println("alert: error");
            
            throw new RuntimeException(e);
        }
		
	} // end of sendEmail()

}// end of Main()