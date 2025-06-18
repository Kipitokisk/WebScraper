package scraper.model;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

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
    private final String nrOfSeats;
    private final String body;
    private final String nrOfDoors;
    private final String engineCapacity;
    private final String horsepower;
    private final String petrolType;
    private final String gearsType;
    private final String tractionType;
    private final String color;

    public CarDetails(Document doc, String baseUrl, String carLink, String carBrand, String carModel) {
        this.link = baseUrl + carLink;

        String title = extractTitle(doc);
        validateTitle(title, carBrand, carModel);

        Elements asideItems = doc.select("div.styles_aside__0m8KW");

        this.updateDate = extractLabeledValue(asideItems, "p.styles_date__voWnk");
        this.adType = extractLabeledValue(asideItems, "p.styles_type___J9Dy");
        this.region = extractText(asideItems, "span.styles_address__text__duvKg");
        this.author = extractText(asideItems, "a.styles_owner__login__VKE71");

        String eurPriceText = extractLabeledValue(asideItems, "span.styles_sidebar__main__DaXQC");
        this.eurPrice = parseInteger(eurPriceText);

        Map<String, String> leftFeatures = extractFeatureMap(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li");
        Map<String, String> rightFeatures = extractFeatureMap(doc, "div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li");

        String generation = leftFeatures.get("Generație");
        this.name = title + (generation != null ? " " + generation : "");

        this.yearOfFabrication = parseInteger(rightFeatures.get("An de fabricație"));
        this.wheelSide = rightFeatures.get("Volan");
        this.body = rightFeatures.get("Tip caroserie");
        this.color = rightFeatures.get("Culoare");
        this.nrOfSeats = rightFeatures.get("Număr de locuri");
        this.nrOfDoors = rightFeatures.get("Număr uși");
        this.engineCapacity = rightFeatures.get("Capacitate cilindrică");
        this.horsepower = rightFeatures.get("Putere");
        this.petrolType = rightFeatures.get("Tip combustibil");
        this.gearsType = rightFeatures.get("Cutie de viteze");
        this.tractionType = rightFeatures.get("Tip tracțiune");
        this.mileage = parseInteger(rightFeatures.get("Rulaj"));
    }


    String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("h1");
        return (titleElement != null) ? titleElement.text() : null;
    }

    void validateTitle(String title, String brand, String model) {
        if (title == null || !title.contains(brand + " " + model)) {
            throw new IllegalArgumentException("Invalid title for car: " + title);
        }
    }

    String extractLabeledValue(Elements container, String cssQuery) {
        Element element = container.selectFirst(cssQuery);
        if (element != null) {
            String text = element.text();
            int colonIdx = text.indexOf(":");
            return (colonIdx >= 0) ? text.substring(colonIdx + 1).trim() : text.trim();
        }
        return null;
    }

    String extractText(Elements container, String cssQuery) {
        Element element = container.selectFirst(cssQuery);
        return element != null ? element.text() : null;
    }

    Map<String, String> extractFeatureMap(Document doc, String cssQuery) {
        Map<String, String> map = new HashMap<>();
        Elements items = doc.select(cssQuery);
        for (Element item : items) {
            Element key = item.selectFirst("span.styles_group__key__uRhnQ");
            Element value = item.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI");
            if (key != null && value != null) {
                map.put(key.text(), value.text());
            }
        }
        return map;
    }

    Integer parseInteger(String text) {
        if (text == null) return null;
        try {
            return Integer.parseInt(text.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return null;
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
    public String getNrOfSeats() { return nrOfSeats; }
    public String getBody() { return body; }
    public String getNrOfDoors() { return nrOfDoors; }
    public String getEngineCapacity() { return engineCapacity; }
    public String getHorsepower() { return horsepower; }
    public String getPetrolType() { return petrolType; }
    public String getGearsType() { return gearsType; }
    public String getTractionType() { return tractionType; }
    public String getColor() { return color; }
}
