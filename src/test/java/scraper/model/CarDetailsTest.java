package scraper.model;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CarDetailsTest {
    private Document doc;
    private Elements asideItems;
    private Element titleElement;
    private Element priceElement;
    private Element dateElement;
    private Element typeElement;
    private Element regionElement;
    private Element authorElement;
    private Elements leftFeatureItems;
    private Elements rightFeatureItems;
    private CarDetails carDetails;

    @Before
    public void setUp() {
        doc = mock(Document.class);
        asideItems = mock(Elements.class);
        titleElement = mock(Element.class);
        priceElement = mock(Element.class);
        dateElement = mock(Element.class);
        typeElement = mock(Element.class);
        regionElement = mock(Element.class);
        authorElement = mock(Element.class);
        leftFeatureItems = mock(Elements.class);
        rightFeatureItems = mock(Elements.class);

        when(doc.selectFirst("h1")).thenReturn(titleElement);
        when(titleElement.text()).thenReturn("Renault Megane");
        when(doc.select("div.styles_aside__0m8KW")).thenReturn(asideItems);
        when(doc.select("div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li")).thenReturn(new Elements());
        when(doc.select("div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li")).thenReturn(new Elements());
        carDetails = new CarDetails(doc, "https://999.md/", "/car/123", "Renault", "Megane");
    }

    @Test
    public void testConstructor_SuccessfulExtraction() {
        when(doc.selectFirst("h1")).thenReturn(titleElement);
        when(titleElement.text()).thenReturn("Renault Megane");
        when(doc.select("div.styles_aside__0m8KW")).thenReturn(asideItems);
        when(asideItems.selectFirst("span.styles_sidebar__main__DaXQC")).thenReturn(priceElement);
        when(priceElement.text()).thenReturn("Preț: 25000 EUR");
        when(asideItems.selectFirst("p.styles_date__voWnk")).thenReturn(dateElement);
        when(dateElement.text()).thenReturn("Updated: 2023-10-01");
        when(asideItems.selectFirst("p.styles_type___J9Dy")).thenReturn(typeElement);
        when(typeElement.text()).thenReturn("Tipul: Vând");
        when(asideItems.selectFirst("span.styles_address__text__duvKg")).thenReturn(regionElement);
        when(regionElement.text()).thenReturn("Chisinau");
        when(asideItems.selectFirst("a.styles_owner__login__VKE71")).thenReturn(authorElement);
        when(authorElement.text()).thenReturn("John");

        when(doc.select("div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li")).thenReturn(leftFeatureItems);
        when(doc.select("div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li")).thenReturn(rightFeatureItems);

        Element leftFeature = mock(Element.class);
        Element leftKey = mock(Element.class);
        Element leftValue = mock(Element.class);
        when(leftFeatureItems.iterator()).thenReturn(java.util.Collections.singletonList(leftFeature).iterator());
        when(leftFeature.selectFirst("span.styles_group__key__uRhnQ")).thenReturn(leftKey);
        when(leftFeature.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI")).thenReturn(leftValue);

        Element rightFeature = mock(Element.class);
        Element rightKey = mock(Element.class);
        Element rightValue = mock(Element.class);
        when(rightFeatureItems.iterator()).thenReturn(java.util.Collections.singletonList(rightFeature).iterator());
        when(rightFeature.selectFirst("span.styles_group__key__uRhnQ")).thenReturn(rightKey);
        when(rightFeature.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI")).thenReturn(rightValue);
        when(rightKey.text()).thenReturn("An de fabricație");
        when(rightValue.text()).thenReturn("2018");

        CarDetails car = new CarDetails(doc, "https://999.md", "/car/123", "Renault", "Megane");

        assertEquals("https://999.md/car/123", car.getLink());
        assertEquals("Renault Megane", car.getName());
        assertEquals(Integer.valueOf(25000), car.getEurPrice());
        assertEquals("2023-10-01", car.getUpdateDate());
        assertEquals("Vând", car.getAdType());
        assertEquals("Chisinau", car.getRegion());
        assertEquals("John", car.getAuthor());
        assertEquals(Integer.valueOf(2018), car.getYearOfFabrication());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_InvalidTitle() {
        when(doc.selectFirst("h1")).thenReturn(titleElement);
        when(titleElement.text()).thenReturn("Dacia Logan");
        new CarDetails(doc, "https://999.md", "/car/123", "Renault", "Megane");
    }

    @Test
    public void testParseInteger_NonNumeric() {
        when(doc.selectFirst("h1")).thenReturn(titleElement);
        when(titleElement.text()).thenReturn("Renault Megane");
        when(doc.select("div.styles_aside__0m8KW")).thenReturn(asideItems);
        when(asideItems.selectFirst("span.styles_sidebar__main__DaXQC")).thenReturn(priceElement);
        when(priceElement.text()).thenReturn("Price: Not available");
        when(doc.select("div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li")).thenReturn(new Elements());
        when(doc.select("div.styles_features__right__Sn6fV > div.styles_group__aota8 > ul > li")).thenReturn(new Elements());

        CarDetails car = new CarDetails(doc, "https://999.md/", "/car/123", "Renault", "Megane");
        assertNull(car.getEurPrice());
    }


    @Test
    public void testExtractTitle_Success() {
        when(doc.selectFirst("h1")).thenReturn(titleElement);
        when(titleElement.text()).thenReturn("BMW X5");

        String result = carDetails.extractTitle(doc);
        assertEquals("BMW X5", result);
    }

    @Test
    public void testExtractTitle_NoTitleElement() {
        when(doc.selectFirst("h1")).thenReturn(null);

        String result = carDetails.extractTitle(doc);
        assertNull(result);
    }

    @Test
    public void testValidateTitle_Valid() {
        carDetails.validateTitle("Renault Megane III (2008 - 2016)", "Renault", "Megane");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateTitle_NullTitle() {
        carDetails.validateTitle(null, "Renault", "Megane");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateTitle_MissingBrandModel() {
        carDetails.validateTitle("Dacia Logan", "Renault", "Megane");
    }

    @Test
    public void testExtractLabeledValue_WithColon() {
        Element element = mock(Element.class);
        when(asideItems.selectFirst("span.styles_sidebar__main__DaXQC")).thenReturn(element);
        when(element.text()).thenReturn("Preț: 25000 EUR");

        String result = carDetails.extractLabeledValue(asideItems, "span.styles_sidebar__main__DaXQC");
        assertEquals("25000 EUR", result);
    }

    @Test
    public void testExtractLabeledValue_NoElement() {
        when(asideItems.selectFirst("p.styles_date__voWnk")).thenReturn(null);

        String result = carDetails.extractLabeledValue(asideItems, "p.styles_date__voWnk");
        assertNull(result);
    }

    @Test
    public void testExtractText_Success() {
        Element element = mock(Element.class);
        when(asideItems.selectFirst("span.styles_address__text__duvKg")).thenReturn(element);
        when(element.text()).thenReturn("Chisinau");

        String result = carDetails.extractText(asideItems, "span.styles_address__text__duvKg");
        assertEquals("Chisinau", result);
    }

    @Test
    public void testExtractText_NoElement() {
        when(asideItems.selectFirst("span.styles_address__text__duvKg")).thenReturn(null);

        String result = carDetails.extractText(asideItems, "span.styles_address__text__duvKg");
        assertNull(result);
    }

    @Test
    public void testExtractFeatureMap_Success() {
        Elements items = mock(Elements.class);
        Element item = mock(Element.class);
        Element key = mock(Element.class);
        Element value = mock(Element.class);

        when(doc.select("div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li")).thenReturn(items);
        when(items.iterator()).thenReturn(java.util.Collections.singletonList(item).iterator());
        when(item.selectFirst("span.styles_group__key__uRhnQ")).thenReturn(key);
        when(item.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI")).thenReturn(value);
        when(key.text()).thenReturn("Generație");
        when(value.text()).thenReturn("III (2008 - 2016)");

        Map<String, String> result = carDetails.extractFeatureMap(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li");
        assertEquals(1, result.size());
        assertEquals("III (2008 - 2016)", result.get("Generație"));
    }

    @Test
    public void testExtractFeatureMap_MissingKeyOrValue() {
        Elements items = mock(Elements.class);
        Element item = mock(Element.class);

        when(doc.select("div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li")).thenReturn(items);
        when(items.iterator()).thenReturn(java.util.Collections.singletonList(item).iterator());
        when(item.selectFirst("span.styles_group__key__uRhnQ")).thenReturn(null);
        when(item.selectFirst("span.styles_group__value__XN7OI, a.styles_group__value__XN7OI")).thenReturn(null);

        Map<String, String> result = carDetails.extractFeatureMap(doc, "div.styles_features__left__ON_QP > div.styles_group__aota8 > ul > li");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseInteger_ValidNumber() {
        Integer result = carDetails.parseInteger("12345 km");
        assertEquals(Integer.valueOf(12345), result);
    }

    @Test
    public void testParseInteger_NullInput() {
        Integer result = carDetails.parseInteger(null);
        assertNull(result);
    }

    @Test
    public void testParseInteger_EmptyAfterReplace() {
        Integer result = carDetails.parseInteger("abc");
        assertNull(result);
    }
}