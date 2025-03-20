package info.sup.proj.backend.model;

public class User {
    private String name;
    private String picture;

    public User(String name, String picture) {
        this.name = name;
        this.picture = picture;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }
}