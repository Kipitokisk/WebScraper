package scraper.model;

public class CarDetails {
    private final String link;
    private final String name;
    private final Integer eurPrice;
    private final Integer mileage;
    private final String updateDate;
    private final String adType;
    private final String region;
    private final String author;
    private final Integer yearOfFabrication;
    private final String wheelSide;
    private final Integer nrOfSeats;
    private final String body;
    private final Integer nrOfDoors;
    private final Integer engineCapacity;
    private final Integer horsepower;
    private final String petrolType;
    private final String gearsType;
    private final String tractionType;
    private final String color;

    public CarDetails(String link, String name, Integer eurPrice, Integer mileage,
               String updateDate, String adType, String region, String author, Integer yearOfFabrication,
               String wheelSide, Integer nrOfSeats, String body, Integer nrOfDoors, Integer engineCapacity,
               Integer horsepower, String petrolType, String gearsType, String tractionType, String color) {
        this.link = link;
        this.name = name;
        this.eurPrice = eurPrice;
        this.mileage = mileage;
        this.updateDate = updateDate;
        this.adType = adType;
        this.region = region;
        this.author = author;
        this.yearOfFabrication = yearOfFabrication;
        this.wheelSide = wheelSide;
        this.nrOfSeats = nrOfSeats;
        this.body = body;
        this.nrOfDoors = nrOfDoors;
        this.engineCapacity = engineCapacity;
        this.horsepower = horsepower;
        this.petrolType = petrolType;
        this.gearsType = gearsType;
        this.tractionType = tractionType;
        this.color = color;
    }

    public String getLink() {
        return link;
    }

    public String getName() {
        return name;
    }

    public Integer getEurPrice() {
        return eurPrice;
    }

    public Integer getMileage() {
        return mileage;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public String getAdType() {
        return adType;
    }

    public String getRegion() {
        return region;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getYearOfFabrication() {
        return yearOfFabrication;
    }

    public String getWheelSide() {
        return wheelSide;
    }

    public Integer getNrOfSeats() {
        return nrOfSeats;
    }

    public String getBody() {
        return body;
    }

    public Integer getNrOfDoors() {
        return nrOfDoors;
    }

    public Integer getEngineCapacity() {
        return engineCapacity;
    }

    public Integer getHorsepower() {
        return horsepower;
    }

    public String getPetrolType() {
        return petrolType;
    }

    public String getGearsType() {
        return gearsType;
    }

    public String getTractionType() {
        return tractionType;
    }

    public String getColor() {
        return color;
    }
}