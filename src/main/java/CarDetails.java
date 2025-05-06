public class CarDetails {
    String link;
    String name;
    Integer eurPrice;
    Integer mileage;
    String updateDate;
    String adType;
    String region;
    String author;
    Integer yearOfFabrication;
    String wheelSide;
    Integer nrOfSeats;
    String body;
    Integer nrOfDoors;
    Integer engineCapacity;
    Integer horsepower;
    String petrolType;
    String gearsType;
    String tractionType;
    String color;

    CarDetails(String link, String name, Integer eurPrice, Integer mileage,
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
}