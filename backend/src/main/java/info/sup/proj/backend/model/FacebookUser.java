package info.sup.proj.backend.model;

public class FacebookUser {
    private String id;
    private String name;
    private String email;

    public FacebookUser(String id, String name, String email, String pictureUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}