package server.collection;

import shared.model.Country;
import shared.model.Location;
import shared.model.Person;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionManager {
    private final HashSet<Person> collection;
    private final LocalDateTime initializationDate;

    public CollectionManager() {
        this.collection = new HashSet<>();
        this.initializationDate = LocalDateTime.now();
    }

    public synchronized void loadFromDB(HashSet<Person> persons) {
        collection.clear();
        collection.addAll(persons);
    }

    public synchronized String getInfo() {
        return "Тип коллекции: " + collection.getClass().getName() + "\n" +
                "Дата инициализации: " + initializationDate + "\n" +
                "Количество элементов: " + collection.size() + "\n" +
                "Тип элементов: " + Person.class.getName();
    }

    public synchronized int size() { return collection.size(); }
    public synchronized boolean isEmpty() { return collection.isEmpty(); }

    public synchronized List<Person> getSortedByLocation() {
        return collection.stream().sorted(this::compareByLocation).collect(Collectors.toList());
    }

    private int compareByLocation(Person p1, Person p2) {
        Location l1 = p1.getLocation(), l2 = p2.getLocation();
        if (l1 == null && l2 == null) return 0;
        if (l1 == null) return -1; if (l2 == null) return 1;
        int x = Float.compare(l1.getX(), l2.getX());
        if (x != 0) return x;
        int y = Long.compare(l1.getY(), l2.getY());
        if (y != 0) return y;
        String n1 = l1.getName() == null ? "" : l1.getName();
        String n2 = l2.getName() == null ? "" : l2.getName();
        return n1.compareTo(n2);
    }

    public synchronized Person getById(Integer id) {
        if (id == null) return null;
        return collection.stream().filter(p -> id.equals(p.getId())).findFirst().orElse(null);
    }

    public synchronized boolean existsById(Integer id) {
        if (id == null) return false;
        return collection.stream().anyMatch(p -> p.getId() != null && p.getId().equals(id));
    }

    public synchronized Person getMin() {
        return collection.stream().min(Person::compareTo).orElse(null);
    }

    public synchronized boolean canAddIfMin(Person person) {
        if (person == null) return false;
        if (collection.isEmpty()) return true;
        Person min = collection.stream().min(Person::compareTo).get();
        return person.compareTo(min) < 0;
    }

    public synchronized boolean add(Person person) {
        if (person == null || person.getId() == null) return false;
        return collection.add(person);
    }

    public synchronized boolean canModify(Integer id, String creator) {
        Person p = getById(id);
        return p != null && Objects.equals(creator, p.getCreator());
    }

    public synchronized boolean update(Integer id, Person newPerson) {
        if (id == null || newPerson == null) return false;
        Person existing = getById(id);
        if (existing == null) return false;
        collection.remove(existing);
        newPerson.setId(id);
        return collection.add(newPerson);
    }

    public synchronized boolean removeById(Integer id) {
        if (id == null) return false;
        return collection.removeIf(p -> id.equals(p.getId()));
    }

    public synchronized int clearByCreator(String creator) {
        if (creator == null) return 0;
        int before = collection.size();
        collection.removeIf(p -> Objects.equals(p.getCreator(), creator));
        return before - collection.size();
    }

    public synchronized boolean addIfMin(Person person) {
        if (person == null) return false;
        Person min = getMin();
        if (min == null || person.compareTo(min) < 0) return add(person);
        return false;
    }

    public synchronized int removeGreater(Person person, String creator) {
        if (person == null || creator == null) return 0;
        int before = collection.size();
        collection.removeIf(p -> Objects.equals(p.getCreator(), creator) && p.compareTo(person) > 0);
        return before - collection.size();
    }

    public synchronized int removeLower(Person person, String creator) {
        if (person == null || creator == null) return 0;
        int before = collection.size();
        collection.removeIf(p -> Objects.equals(p.getCreator(), creator) && p.compareTo(person) < 0);
        return before - collection.size();
    }

    public synchronized long countLessThanNationality(Country nat) {
        if (nat == null) return 0;
        return collection.stream().map(Person::getNationality).filter(Objects::nonNull)
                .filter(n -> n.ordinal() < nat.ordinal()).count();
    }

    public synchronized Set<Country> getUniqueNationalities() {
        return collection.stream().map(Person::getNationality).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public synchronized List<Date> getBirthdaysDescending() {
        return collection.stream().map(Person::getBirthday).filter(Objects::nonNull)
                .sorted(Collections.reverseOrder()).collect(Collectors.toList());
    }
}