package addressbookdb;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
    private ArrayList<Person> people;
    private ObservableList<Person> rows;
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
        people = null;
        rows = null;
        try{
            people = readDatabase();
            String name = nameTextField.getText().trim(), lastName = lastNameTextField.getText().trim(),
                    email = emailTextField.getText().trim();
            if((name.equals("") || isCorrectName(name)) && (lastName.equals("") || isCorrectName(lastName)) && (email.equals("") || isCorrectEmail(email))){
                rows = FXCollections.observableArrayList();
                for(Person p : people)
                    if((name.equals("") || p.getName().equals(name)) && (lastName.equals("") || p.getLastName().equals(lastName))
                            && (email.equals("") || p.getEmail().equals(email)))
                                rows.add(p);
                nameColumn.setCellValueFactory(new PropertyValueFactory("name"));
                lastNameColumn.setCellValueFactory(new PropertyValueFactory("lastName"));
                emailColumn.setCellValueFactory(new PropertyValueFactory("email"));
                informationTableView.setItems(rows);
            }else{
                showAlert(AlertType.ERROR, "Złe dane. Imię i nazwisko powinno składać się z liter, "
                        + "a email mieć postać: tekst@tekst.test,\n gdzie tekst to liczby, małe litery, duże litery. "
                        + "Tekst po @ może wystąpić w dowolnej liczbie,\n lecz ma być oddzielony kropką.");
            }
        }catch(IllegalArgumentException e){
            showAlert(AlertType.ERROR, e.getMessage());
        }
    }
    @FXML
    private void addAction(ActionEvent event){
        String name = nameTextField.getText().trim(), lastName = lastNameTextField.getText().trim(), 
                email = emailTextField.getText().trim(); 
        if(isCorrectName(name) && isCorrectName(lastName) && isCorrectEmail(email)){
            Person p = new Person(findAvailableId(), name, lastName, email); 
            people.add(p);
            rows.add(p);
        }else{
            showAlert(AlertType.ERROR, "Złe dane. Imię i nazwisko powinno składać się z liter, "
                + "a email mieć postać: tekst@tekst.test,\n gdzie tekst to liczby, małe litery, duże litery. "
                + "Tekst po @ może wystąpić w dowolnej liczbie,\n lecz ma być oddzielony kropką. Żadne pole nie może być puste.");
        }
    }
    @FXML
    private void saveAction(ActionEvent event){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ksiazkaadresowa", "olek", "haslo12345")){
                Statement stat = conn.createStatement();
                ResultSet rs = stat.executeQuery("SELECT id FROM Person");
                ArrayList<Integer> usedId = new ArrayList(); 
                while(rs.next())
                    usedId.add(rs.getInt("id"));
                PreparedStatement prepStat = conn.prepareStatement("INSERT INTO Person VALUES(?, ?, ?, ?)");
                for(Person p : people){
                    int id = p.getId();
                    if(!usedId.contains(id)){
                        prepStat.setInt(1, p.getId());
                        prepStat.setString(2, p.getName());
                        prepStat.setString(3, p.getLastName());
                        prepStat.setString(4, p.getEmail());
                        prepStat.executeUpdate();
                    }else usedId.remove((Integer)id);
                    
                }
                prepStat = conn.prepareStatement("DELETE FROM Person WHERE id = ?");
                for(int id : usedId){
                    prepStat.setInt(1, id);
                    prepStat.executeUpdate();
                }
            }catch(SQLException e){
                Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
            }
        }catch(ClassNotFoundException e){
            Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    @FXML
    private void deleteAction(ActionEvent event){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ksiazkaadresowa", "olek", "haslo12345")){
                String name = nameTextField.getText().trim(), lastName = lastNameTextField.getText().trim(), 
                        email = emailTextField.getText().trim(); 
                for(int i = 0; i < people.size(); i++){
                    Person p = people.get(i);
                    if((name.equals("") || p.getName().equals(name)) && (lastName.equals("") || p.getLastName().equals(lastName))
                            && (email.equals("") || p.getEmail().equals(email))){
                        rows.remove(p);
                        people.remove(p);
                        i--;
                    }
                }
            }catch(SQLException e){
                Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
            }
        }catch(ClassNotFoundException e){
            Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
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
            try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ksiazkaadresowa", "olek", "haslo12345")){
                String[] row;
                Statement stat = conn.createStatement();
                ResultSet rs = stat.executeQuery("SELECT * FROM Person");
                while(rs.next()){
                    row = new String[4];
                    row[0] = rs.getString("id");
                    row[1] = rs.getString("name");
                    row[2] = rs.getString("lastname");
                    row[3] = rs.getString("email");
                    if(isCorrectDatabaseFormat(row))
                        people.add(new Person(Integer.parseInt(row[0]), row[1], row[2], row[3]));
                    else{
                        people = null;
                        throw new IllegalArgumentException("Niepoprawny format zawartości bazy danych."
                            + " Każdy wiersz powinien zawierać 4 wartości.\n Imię i nazwisko "
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
        return components.length == 4 && isCorrectName(components[1]) && 
                isCorrectName(components[2]) && isCorrectEmail(components[3]);
    }
    private boolean isCorrectName(String name){
        return name.matches("[a-zA-z]+");
    }
    private boolean isCorrectEmail(String email){
        return email.matches("[a-zA-z0-9]+@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)+");
    }
    private int findAvailableId(){
        boolean used;
        int i, id = -1, size = people.size();
        do{
            i = 0;
            used = false;
            id++;
            do{
                Person p = people.get(i);
                i++;
                if(id == p.getId()) used = true;
            }while(!used && i < size);
        }while(id < Integer.MAX_VALUE && used);
        return id;
    }
    private void showAlert(AlertType type, String text){
        Alert alert = new Alert(type);
        alert.setHeaderText(text);
        alert.showAndWait();
    }
}
