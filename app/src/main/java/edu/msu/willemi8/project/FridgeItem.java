package edu.msu.willemi8.project;//package edu.msu.willemi8.project;
//
//public class FridgeItem {
//    private int id;
//    private String name;
//    private String expirationDate;
//
//    public FridgeItem(int id, String name, String expirationDate) {
//        this.id = id;
//        this.name = name;
//        this.expirationDate = expirationDate;
//    }
//
//    public int getId() {
//        return id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public String getExpirationDate() {
//        return expirationDate;
//    }
//}


public class FridgeItem {
    public String id;          // ← was int
    public String name;
    public String expirationDate;

    // Required for Firebase
    public FridgeItem() {}

    public FridgeItem(String id, String name, String expirationDate) {
        this.id = id;
        this.name = name;
        this.expirationDate = expirationDate;
    }
}

