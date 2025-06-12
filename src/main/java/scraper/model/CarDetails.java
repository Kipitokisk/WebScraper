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

    public CarDetails(Document doc, String baseUrl, String carLink) {
        this.link = baseUrl + carLink;

        String title = getTitle(doc);
        String carBrand = System.getenv("CAR_BRAND");
        String carModel = System.getenv("CAR_MODEL");
        if (!title.contains(carBrand + " " + carModel)) {
            throw new IllegalArgumentException("Invalid car model in title: " + title);
        }

        Elements items = doc.select("div.styles_aside__0m8KW");

        this.updateDate = getAdInfo(items, "p.styles_date__voWnk");
        this.adType = getAdInfo(items, "p.styles_type___J9Dy");
        this.region = getString(items, "span.styles_address__text__duvKg");
        this.author = getString(items, "a.styles_owner__login__VKE71");

        String eurPriceText = getAdInfo(items, "span.styles_sidebar__main__DaXQC");
        this.eurPrice = getEurPrice(eurPriceText);

        Map<String, String> generalities = new HashMap<>();
        extractSection(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li", generalities);
        String generation = generalities.get("Generație");

        Map<String, String> particularities = new HashMap<>();
        extractSection(doc, "div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li", particularities);

        this.name = title + (generation != null ? " " + generation : "");

        this.yearOfFabrication = getIntegerFromSection(particularities, "An de fabricație");
        this.wheelSide = particularities.get("Volan");
        this.body = particularities.get("Tip caroserie");
        this.color = particularities.get("Culoare");
        this.nrOfSeats = particularities.get("Număr de locuri");
        this.nrOfDoors = particularities.get("Număr uși");
        this.engineCapacity = particularities.get("Capacitate cilindrică");
        this.horsepower = particularities.get("Putere");
        this.petrolType = particularities.get("Tip combustibil");
        this.gearsType = particularities.get("Cutie de viteze");
        this.tractionType = particularities.get("Tip tracțiune");
        this.mileage = getIntegerFromSection(particularities, "Rulaj");
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

    Integer getIntegerFromSection(Map<String, String> map, String element) {
        Integer result = null;
        String text = map.get(element);
        if (text != null) {
            try {
                result = Integer.parseInt(text.replaceAll("\\D", ""));
            } catch (NumberFormatException e) {
                throw new NumberFormatException(e.getMessage());
            }
        }
        return result;
    }

    void extractSection(Document doc, String cssQuery, Map<String, String> map) {
        Elements generalitiesItems = doc.select(cssQuery);
        for (Element item : generalitiesItems) {
            Element keyElement = item.selectFirst("span.styles_group__key__uRhnQ");
            Element valueElement = item.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI");
            if (keyElement != null && valueElement != null) {
                map.put(keyElement.text(), valueElement.text());
            }
        }
    }

    Integer getEurPrice(String eurPriceText) {
        Integer result = null;
        if (eurPriceText == null) {
            throw new NullPointerException("Price empty.");
        }
        try {
            if (eurPriceText.contains("€")) {
                result = Integer.parseInt(eurPriceText.replaceAll("\\D", ""));
            }
        } catch (NumberFormatException e) {
            throw new NumberFormatException(e.getMessage());
        }
        return result;
    }

    String getTitle(Document doc) {
        String result = null;
        Element titleElement = doc.selectFirst("h1");
        if (titleElement != null) {
            result = titleElement.text();
        }
        return result;
    }

    String getAdInfo(Elements items, String cssQuery) {
        String result = null;
        Element element = items.selectFirst(cssQuery);
        if (element != null) {
            String text = element.text();
            result = text.substring(text.indexOf(":") + 1).trim();
        }
        return result;
    }

    String getString(Elements items, String cssQuery) {
        String result = null;
        Element element = items.selectFirst(cssQuery);
        if (element != null) {
            result = element.text();
        }
        return result;
    }
}