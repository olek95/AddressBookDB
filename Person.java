package addressbookdb;

/**
 * Obiekt klasy <code>Person</code> reprezentuje dane konkretnej osoby. Zawiera
 * takie dane jak imię, nazwisko i adres email. Implementuje interfejs pozwalający 
 * na porównywanie obiektów tej samej klasy. 
 * @author AleksanderSklorz
 */
public class Person implements Comparable<Person>{
    private String name, lastName, email;
    private int id; // indeks niewidoczny dla użytkownika, występuje w celu sprawdzenia istnienia wiersza w bazie
    public Person(int id, String name, String lastName, String email){
        this.id = id;
        this.name = name;
        this.lastName = lastName; 
        this.email = email; 
    }
    /**
     * Porównuje ten obiekt z innym obiektem tej samej klasy, podanym jako parametr. 
     * Pierwszą wartością do porównania jest nazwysko, jeśli obiekty mają je takie 
     * same, porównuje się imię, a jeśli to również jest takie same - adresy email. 
     * @param p porównywane dane innej osoby. 
     * @return -1 jeśli wartości tego obiektu są mniejsze niż parametru, 0 jeśli równe, a 1 jeśli większe.
     */
    public int compareTo(Person p){
        if(lastName.compareTo(p.lastName) < 0)return -1;
        else if(lastName.compareTo(p.lastName) > 0) return 1; 
        else if(lastName.compareTo(p.lastName) == 0)
            if(name.compareTo(p.name) < 0) return -1;
            else if(name.compareTo(p.name) > 0) return 1; 
            else if(name.compareTo(p.name) == 0)
                if(email.compareTo(p.email) < 0) return -1; 
                else if(email.compareTo(p.email) > 0) return 1;
        return 0;
    }
    /**
     * Zwraca imię osoby. 
     * @return imię
     */
    public String getName(){
        return name;
    }
    /**
     * Zwraca nazwisko osoby.
     * @return nazwisko
     */
    public String getLastName(){
        return lastName;
    }
    /**
     * Zwraca adres e-mail osoby. 
     * @return e-mail
     */
    public String getEmail(){
        return email;
    }
    /**
     * Zwraca nr id osoby w bazie danych. 
     * @return nr id
     */
    public int getId(){
        return id;
    }
}
