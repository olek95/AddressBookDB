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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

/**
 * Klasa <code>AddressBookDBFXMLController</code> reprezentuje sterowanie programem 
 * będącym książką zawierającą adresy mailowe. Ksiązka ta zapisana jest w bazie danych
 * gdzie w tabeli Person znajdują się dane, kolejno: id, imię, nazwisko, mail, 
 * gdzie imię i nazwisko składa się tylko z liter, a mail jest postaci: tekst@tekst.tekst, 
 * gdzie tekst (litery + liczby) po znaku @ może występować dowolną liczbę razy, 
 * lecz musi być oddzielony kropką. Indeks jest niedostępny dla użytkownika, jest tylko
 * po to, aby rekordy były unikalne. Dlatego generowany jest automatycznie w programie.
 * Przykładowy prawidłowy rekord: 0, Aleksander, Sklorz, olek1995@poczta.onet.pl
 * Klasa ta wyświetla wczytane, posortowane dane w tabeli. Posiada pola tekstowe
 * zezwalające na wyszukanie konkretnych rekordów. Pozostawienie pustego pola 
 * wyświetla dowolną wartość w danej kolumnie. Istnieje też możliwośc dodania nowego rekordu
 * (wszystkie pola tekstowe muszą być uzupełnione) lub usunięcia rekordu (niekoniecznie 
 * wszystkie muszą być uzupełnione - jeśli jest puste, przyjmuje wartość dowolną). 
 * Klasa zabezpieczona przed niepoprawnym formatem zawartości bazy danych 
 * lub wprowadzonych danych w polach tekstowych w programie. 
 * @author AleksanderSklorz
 */
public class AddressBookDBFXMLController implements Initializable {
    private String login, password, url;
    private boolean changed;
    private ArrayList<Person> people;
    private ObservableList<Person> rows;
    @FXML
    private Button exitButton, saveButton, deleteButton, addButton;
    @FXML
    private TextField nameTextField, lastNameTextField, emailTextField;
    @FXML
    private TableView<Person> informationTableView;
    @FXML
    private TableColumn<Person, String> nameColumn, lastNameColumn, emailColumn;
    @FXML
    private void searchAction(ActionEvent event) {
        if(changed){
            Alert changedAlert = new Alert(AlertType.CONFIRMATION);
            changedAlert.setHeaderText("Zmieniono zawartość tabeli w programie. Wciśnięcie OK usunie zmiany.");
            Optional<ButtonType> result = changedAlert.showAndWait();
            if(result.get().equals(ButtonType.CANCEL)) return;
        }
        changed = false;
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
        if(people != null){
            String name = nameTextField.getText().trim(), lastName = lastNameTextField.getText().trim(), 
                    email = emailTextField.getText().trim(); 
            if(isCorrectName(name) && isCorrectName(lastName) && isCorrectEmail(email)){
                changed = true;
                Person p = new Person(findAvailableId(), name, lastName, email); 
                people.add(p);
                rows.add(p);
            }else{
                showAlert(AlertType.ERROR, "Złe dane. Imię i nazwisko powinno składać się z liter, "
                    + "a email mieć postać: tekst@tekst.test,\n gdzie tekst to liczby, małe litery, duże litery. "
                    + "Tekst po @ może wystąpić w dowolnej liczbie,\n lecz ma być oddzielony kropką. Żadne pole nie może być puste.");
            }
        }
    }
    @FXML
    private void saveAction(ActionEvent event){
        if(people != null){
            try(Connection conn = DriverManager.getConnection(url, login, password)){
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
                changed = false;
            }catch(SQLException e){
                Logger.getLogger(AddressBookDBFXMLController.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
    @FXML
    private void deleteAction(ActionEvent event){
        if(people != null){
            String name = nameTextField.getText().trim(), lastName = lastNameTextField.getText().trim(), 
                    email = emailTextField.getText().trim(); 
            int sizeBefore = people.size(); 
            for(int i = 0; i < people.size(); i++){
                Person p = people.get(i);
                if((name.equals("") || p.getName().equals(name)) && (lastName.equals("") || p.getLastName().equals(lastName)) 
                        && (email.equals("") || p.getEmail().equals(email))){
                    rows.remove(p);
                    people.remove(p);
                    i--; // po usunięciu elementy listy się cofają, więc należy sprawdzić jeszcze raz ten sam indeks
                }
            }
            if(sizeBefore != people.size()) changed = true;
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showLoginInformationDialog();
        showURLDialog();
        exitButton.setOnAction(event -> {
            if(changed){
                Alert changedAlert = new Alert(AlertType.CONFIRMATION);
                changedAlert.setHeaderText("Zmieniono zawartość tabeli w programie. Wciśnięcie OK usunie zmiany.");
                Optional<ButtonType> result = changedAlert.showAndWait();
                if(result.get().equals(ButtonType.CANCEL)) return;
            }
           System.exit(0); 
        });
    }    
    private ArrayList<Person> readDatabase() throws IllegalArgumentException{
        ArrayList<Person> people = new ArrayList();
        try(Connection conn = DriverManager.getConnection(url, login, password)){
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
    private void showLoginInformationDialog(){
        Dialog<Pair<String, String>> dialog = new Dialog(); 
        dialog.setTitle("Okno logowania");
        dialog.setHeaderText("Podaj swoje dane: ");
        ButtonType signInButtonType = new ButtonType("Sign in", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(signInButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane(); 
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField usernameTextField = new TextField(); 
        usernameTextField.setPromptText("Login");
        PasswordField passwordField = new PasswordField(); 
        passwordField.setPromptText("Hasło");
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameTextField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        Node signInButton = dialog.getDialogPane().lookupButton(signInButtonType);
        signInButton.setDisable(true);
        usernameTextField.textProperty().addListener((observable, oldValue, newValue) -> signInButton.setDisable(newValue.trim().isEmpty()));
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == signInButtonType)
                return new Pair(usernameTextField.getText(), passwordField.getText());
            return null;
        });
        Optional<Pair<String, String>> result = dialog.showAndWait();
        if(result.isPresent()){
            Pair<String, String> data = result.get();
            login = data.getKey();
            password = data.getValue();
        }else System.exit(0);
    }
    private void showURLDialog(){
        TextInputDialog urlDialog = new TextInputDialog();
        urlDialog.setTitle("Okno URL");
        urlDialog.setContentText("Podaj adres URL (np. \n"
                + "jdbc:mysql://localhost:3306/ksiazkaadresowa)");
        Optional<String> result = urlDialog.showAndWait();
        if(result.isPresent()) url = result.get();
        else System.exit(0);
    }
}
