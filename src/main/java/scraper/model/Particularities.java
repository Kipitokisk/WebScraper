package scraper.model;

public class Particularities {
    private Integer id;
    private String author;
    private Integer yearOfFabrication;
    private LookupEntity wheelSide;
    private LookupEntity nrOfSeats;
    private LookupEntity body;
    private LookupEntity nrOfDoors;
    private LookupEntity engineCapacity;
    private LookupEntity horsepower;
    private LookupEntity petrolType;
    private LookupEntity gearsType;
    private LookupEntity tractionType;
    private LookupEntity color;

    public Particularities() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getYearOfFabrication() { return yearOfFabrication; }
    public void setYearOfFabrication(Integer yearOfFabrication) { this.yearOfFabrication = yearOfFabrication; }
    public LookupEntity getWheelSide() { return wheelSide; }
    public void setWheelSide(LookupEntity wheelSide) { this.wheelSide = wheelSide; }
    public LookupEntity getNrOfSeats() { return nrOfSeats; }
    public void setNrOfSeats(LookupEntity nrOfSeats) { this.nrOfSeats = nrOfSeats; }
    public LookupEntity getBody() { return body; }
    public void setBody(LookupEntity body) { this.body = body; }
    public LookupEntity getNrOfDoors() { return nrOfDoors; }
    public void setNrOfDoors(LookupEntity nrOfDoors) { this.nrOfDoors = nrOfDoors; }
    public LookupEntity getEngineCapacity() { return engineCapacity; }
    public void setEngineCapacity(LookupEntity engineCapacity) { this.engineCapacity = engineCapacity; }
    public LookupEntity getHorsepower() { return horsepower; }
    public void setHorsepower(LookupEntity horsepower) { this.horsepower = horsepower; }
    public LookupEntity getPetrolType() { return petrolType; }
    public void setPetrolType(LookupEntity petrolType) { this.petrolType = petrolType; }
    public LookupEntity getGearsType() { return gearsType; }
    public void setGearsType(LookupEntity gearsType) { this.gearsType = gearsType; }
    public LookupEntity getTractionType() { return tractionType; }
    public void setTractionType(LookupEntity tractionType) { this.tractionType = tractionType; }
    public LookupEntity getColor() { return color; }
    public void setColor(LookupEntity color) { this.color = color; }
}