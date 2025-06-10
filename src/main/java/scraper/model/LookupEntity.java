package scraper.model;

public class LookupEntity {
    private Integer id;
    private Object name;

    public LookupEntity() {}

    public LookupEntity(String name) {
        this.name = name;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Object getName() { return name; }
    public void setName(Object name) { this.name = name; }
}
