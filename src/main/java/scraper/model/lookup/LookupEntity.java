package scraper.model.lookup;

import java.util.Objects;

public class LookupEntity {
    private Integer id;
    private String name;

    public LookupEntity() {}

    public LookupEntity(String name) {
        this.name = name;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LookupEntity that = (LookupEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
