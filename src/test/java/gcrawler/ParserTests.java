package gcrawler;

import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ParserTests {

    @Test
    public void assets() {
        List<String> assets = Parser.extractAssets(Jsoup.parse(
                "<html>" +
                "<head><title>Home</title>" +
                "<link href='/style.css' rel='stylesheet'>" +
                "<link href='/style_vendor.css' rel='stylesheet'>" +
                "<link href='/favicon.ico' rel='shortcut icon'>" +
                "</head>" +
                "<body><img alt='example' src='/images/ex.png'></body>" +
                "</html>",
                "https://www.example.org"
        ));

        Assert.assertEquals(4, assets.size());
    }

    @Test
    public void links() {
        List<String> assets = Parser.extractLinks(Jsoup.parse(
                "<html>" +
                "<head><title>Home</title>" +
                "<link href='/style.css' rel='stylesheet'>" +
                "</head>" +
                "<body>" +
                "<a href='/about'>About</a>" +
                "<a href='/about#'>AboutAgain</a>" +
                "<a href='tel:+39123456789'>Call Us</a>" +
                "</body>" +
                "</html>",
                "https://www.example.org"
        ));

        Assert.assertEquals(2, assets.size());
    }

}
