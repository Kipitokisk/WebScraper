package scraper.model;

import java.util.Objects;

public class Particularities {
    private Integer id;
    private String author;
    private String link;
    private Integer yearOfFabrication;
    private Integer wheelSide;
    private Integer nrOfSeats;
    private Integer body;
    private Integer nrOfDoors;
    private Integer engineCapacity;
    private Integer horsepower;
    private Integer petrolType;
    private Integer gearsType;
    private Integer tractionType;
    private Integer color;

    public Particularities() {
    }

    public Particularities(Builder builder) {
        this.id = builder.id;
        this.author = builder.author;
        this.link = builder.link;
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
        private Integer id;
        private String author;
        private String link;
        private Integer yearOfFabrication;
        private Integer wheelSide;
        private Integer nrOfSeats;
        private Integer body;
        private Integer nrOfDoors;
        private Integer engineCapacity;
        private Integer horsepower;
        private Integer petrolType;
        private Integer gearsType;
        private Integer tractionType;
        private Integer color;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder yearOfFabrication(Integer yearOfFabrication) {
            this.yearOfFabrication = yearOfFabrication;
            return this;
        }

        public Builder wheelSide(Integer wheelSide) {
            this.wheelSide = wheelSide;
            return this;
        }

        public Builder nrOfSeats(Integer nrOfSeats) {
            this.nrOfSeats = nrOfSeats;
            return this;
        }

        public Builder nrOfDoors(Integer nrOfDoors) {
            this.nrOfDoors = nrOfDoors;
            return this;
        }

        public Builder body(Integer body) {
            this.body = body;
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

        public Builder petrolType(Integer petrolType) {
            this.petrolType = petrolType;
            return this;
        }

        public Builder gearsType(Integer gearsType) {
            this.gearsType = gearsType;
            return this;
        }

        public Builder tractionType(Integer tractionType) {
            this.tractionType = tractionType;
            return this;
        }

        public Builder color(Integer color) {
            this.color = color;
            return this;
        }

        public Particularities build() {return new Particularities(this);}
    }

    public Integer getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getYearOfFabrication() {
        return yearOfFabrication;
    }

    public Integer getWheelSide() {
        return wheelSide;
    }

    public Integer getNrOfSeats() {
        return nrOfSeats;
    }

    public Integer getBody() {
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

    public Integer getPetrolType() {
        return petrolType;
    }

    public Integer getGearsType() {
        return gearsType;
    }

    public Integer getTractionType() {
        return tractionType;
    }

    public Integer getColor() {
        return color;
    }

    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "Particularities{" +
                "id=" + id +
                ", author='" + author + '\'' +
                ", link='" + link + '\'' +
                ", yearOfFabrication=" + yearOfFabrication +
                ", wheelSide=" + wheelSide +
                ", nrOfSeats=" + nrOfSeats +
                ", body=" + body +
                ", nrOfDoors=" + nrOfDoors +
                ", engineCapacity=" + engineCapacity +
                ", horsepower=" + horsepower +
                ", petrolType=" + petrolType +
                ", gearsType=" + gearsType +
                ", tractionType=" + tractionType +
                ", color=" + color +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Particularities that = (Particularities) o;
        return Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(link);
    }
}