package scraper.model;

import scraper.model.lookup.LookupEntity;

public class Cars {
    private Integer id;
    private String link;
    private String region;
    private Integer mileage;
    private Integer priceEur;
    private String updateDate;
    private LookupEntity adType;
    private Integer particularities;

    public Cars() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Integer getMileage() { return mileage; }
    public void setMileage(Integer mileage) { this.mileage = mileage; }
    public Integer getPriceEur() { return priceEur; }
    public void setPriceEur(Integer priceEur) { this.priceEur = priceEur; }
    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }
    public LookupEntity getAdType() { return adType; }
    public void setAdType(LookupEntity adType) { this.adType = adType; }
    public Integer getParticularities() { return particularities; }
    public void setParticularities(Integer particularities) { this.particularities = particularities; }
}

