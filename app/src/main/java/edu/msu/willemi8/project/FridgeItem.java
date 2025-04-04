package edu.msu.willemi8.project;

public class FridgeItem {
    private int id;
    private String name;
    private String expirationDate;

    public FridgeItem(int id, String name, String expirationDate) {
        this.id = id;
        this.name = name;
        this.expirationDate = expirationDate;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExpirationDate() {
        return expirationDate;
    }
}
