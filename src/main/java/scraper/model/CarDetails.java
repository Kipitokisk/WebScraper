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

    private CarDetails(Builder builder) {
        this.link = builder.link;
        this.name = builder.name;
        this.eurPrice = builder.eurPrice;
        this.mileage = builder.mileage;
        this.updateDate = builder.updateDate;
        this.adType = builder.adType;
        this.region = builder.region;
        this.author = builder.author;
        this.yearOfFabrication = builder.yearOfFabrication;
        this.wheelSide = builder.wheelSide;
        this.nrOfSeats = builder.nrOfSeats;
        this.body = builder.body;
        this.nrOfDoors = builder.nrOfDoors;
        this.engineCapacity = builder.engineCapacity;
        this.horsepower = builder.horsepower;
        this.petrolType = builder.petrolType;
        this.gearsType = builder.gearsType;
        this.tractionType = builder.tractionType;
        this.color = builder.color;
    }

    public static class Builder {
        private String link;
        private String name;
        private Integer eurPrice;
        private Integer mileage;
        private String updateDate;
        private String adType;
        private String region;
        private String author;
        private Integer yearOfFabrication;
        private String wheelSide;
        private Integer nrOfSeats;
        private String body;
        private Integer nrOfDoors;
        private Integer engineCapacity;
        private Integer horsepower;
        private String petrolType;
        private String gearsType;
        private String tractionType;
        private String color;

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder eurPrice(Integer eurPrice) {
            this.eurPrice = eurPrice;
            return this;
        }

        public Builder mileage(Integer mileage) {
            this.mileage = mileage;
            return this;
        }

        public Builder updateDate(String updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder adType(String adType) {
            this.adType = adType;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder yearOfFabrication(Integer yearOfFabrication) {
            this.yearOfFabrication = yearOfFabrication;
            return this;
        }

        public Builder wheelSide(String wheelSide) {
            this.wheelSide = wheelSide;
            return this;
        }

        public Builder nrOfSeats(Integer nrOfSeats) {
            this.nrOfSeats = nrOfSeats;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder nrOfDoors(Integer nrOfDoors) {
            this.nrOfDoors = nrOfDoors;
            return this;
        }

        public Builder engineCapacity(Integer engineCapacity) {
            this.engineCapacity = engineCapacity;
            return this;
        }

        public Builder horsepower(Integer horsepower) {
            this.horsepower = horsepower;
            return this;
        }

        public Builder petrolType(String petrolType) {
            this.petrolType = petrolType;
            return this;
        }

        public Builder gearsType(String gearsType) {
            this.gearsType = gearsType;
            return this;
        }

        public Builder tractionType(String tractionType) {
            this.tractionType = tractionType;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public CarDetails build() {
            return new CarDetails(this);
        }
    }

    public String getLink() { return link; }
    public String getName() { return name; }
    public Integer getEurPrice() { return eurPrice; }
    public Integer getMileage() { return mileage; }
    public String getUpdateDate() { return updateDate; }
    public String getAdType() { return adType; }
    public String getRegion() { return region; }
    public String getAuthor() { return author; }
    public Integer getYearOfFabrication() { return yearOfFabrication; }
    public String getWheelSide() { return wheelSide; }
    public Integer getNrOfSeats() { return nrOfSeats; }
    public String getBody() { return body; }
    public Integer getNrOfDoors() { return nrOfDoors; }
    public Integer getEngineCapacity() { return engineCapacity; }
    public Integer getHorsepower() { return horsepower; }
    public String getPetrolType() { return petrolType; }
    public String getGearsType() { return gearsType; }
    public String getTractionType() { return tractionType; }
    public String getColor() { return color; }
}