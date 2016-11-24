package addressbookdb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class AddressBookDBFXMLController implements Initializable {
    @FXML
    private Button exitButton;
    @FXML
    private TextField nameTextField, lastNameTextField, emailTextField;
    @FXML
    private TableView<Person> informationTableView;
    @FXML
    private TableColumn<Person, String> nameColumn, lastNameColumn, emailColumn;
    @FXML
    private void searchAction(ActionEvent event) {
        ArrayList<Person> people = null;
        try{
            people = readDatabase();
            String name = nameTextField.getText().trim(), lastName = lastNameTextField.getText().trim(),
                    email = emailTextField.getText().trim();
            if((name.equals("") || isCorrectName(name)) && (lastName.equals("") || isCorrectName(lastName)) && (email.equals("") || isCorrectEmail(email))){
                ObservableList<Person> rows = FXCollections.observableArrayList();
                for(Person p : people)
                    if(!name.equals("") && p.getName().equals(name) || name.equals(""))
                        if(!lastName.equals("") && p.getLastName().equals(lastName) || lastName.equals(""))
                            if(!email.equals("") && p.getEmail().equals(email) || email.equals(""))
                                rows.add(p);
                nameColumn.setCellValueFactory(new PropertyValueFactory("name"));
                lastNameColumn.setCellValueFactory(new PropertyValueFactory("lastName"));
                emailColumn.setCellValueFactory(new PropertyValueFactory("email"));
                informationTableView.setItems(rows);
            }else{
                Alert incorrectFormatAlert = new Alert(AlertType.ERROR);
                incorrectFormatAlert.setHeaderText("Złe dane. Imię i nazwisko powinno składać się z liter, "
                        + "a email mieć postać: tekst@tekst.test,\n gdzie tekst to liczby, małe litery, duże litery. "
                        + "Tekst po @ może wystąpić w dowolnej liczbie,\n lecz ma być oddzielony kropką.");
                incorrectFormatAlert.showAndWait();
            }
        }catch(IllegalArgumentException e){
            Alert incorrectFormatAlert = new Alert(AlertType.ERROR);
            incorrectFormatAlert.setHeaderText(e.getMessage());
            incorrectFormatAlert.showAndWait();
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        exitButton.setOnAction(event -> {
           System.exit(0); 
        });
    }    
    private ArrayList<Person> readDatabase() throws IllegalArgumentException{
        ArrayList<Person> people = new ArrayList();
        try{
            Class.forName("com.mysql.jdbc.Driver");
            try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/person", "olek", "haslo12345")){
                String[] row;
                Statement stat = conn.createStatement();
                ResultSet rs = stat.executeQuery("SELECT * FROM Person");
                while(rs.next()){
                    row = new String[3];
                    row[0] = rs.getString("name");
                    row[1] = rs.getString("lastname");
                    row[2] = rs.getString("email");
                    if(isCorrectDatabaseFormat(row))
                        people.add(new Person(row[0], row[1], row[2]));
                    else{
                        people = null;
                        throw new IllegalArgumentException("Niepoprawny format zawartości bazy danych."
                            + " Każdy wiersz powinien zawierać 3 wartości oddzielone.\n Imię i nazwisko "
                            + "powinno składać się z liter, a email mieć postać: tekst@tekst.tekst,\n gdzie tekst to liczby, małe litery, duże litery."
                            + " Tekst po @ może wystąpić w dowolnej liczbie,\n lecz ma być oddzielony kropką.");
                    }
                }
                Collections.sort(people);
            }catch(SQLException e){
                Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
            }
        }catch(ClassNotFoundException e){
            Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
        }
        return people;
    }
    private boolean isCorrectDatabaseFormat(String[] components){
        return components.length == 3 && isCorrectName(components[0]) && 
                isCorrectName(components[1]) && isCorrectEmail(components[2]);
    }
    private boolean isCorrectName(String name){
        return name.matches("[a-zA-z]+");
    }
    private boolean isCorrectEmail(String email){
        return email.matches("[a-zA-z0-9]+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)+");
    }
}
