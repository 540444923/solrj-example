package net.aimeizi.webmagic;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/10/12.
 * JD��Ʒץȡ
 */
public class JDProductProcessor implements PageProcessor {

    private Site site = Site.me()
            .setRetryTimes(3)
            .setSleepTime(1000)
            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

    private static final String URL_LIST = "http://list\\.jd\\.com/.*\\.html";

    @Override
    public void process(Page page) {
        // ������Ʒ�������
        if (page.getUrl().regex("http://www\\.jd\\.com/allSort\\.aspx").match()) {
            List<String> links = page.getHtml().links().regex(URL_LIST).all();// ��ȡ��Ʒ��������ƥ��URL_LIST��������Ϣ
            // �����Ʒ����ҳ����ƥ�䵽��������Ʒ��Ϣ��������
            page.addTargetRequests(links);
        } else { //������Ʒ�б�ҳ
            List<String> names = page.getHtml().xpath("//li[@class='gl-item']/div[@class='gl-i-wrap j-sku-item']/div[@class='p-name']/a/em/text()").all();
            List<String> prices = page.getHtml().xpath("//li[@class='gl-item']/div[@class='gl-i-wrap j-sku-item']/div[@class='p-price']/strong[@class='J_price']/i/text()").all();
            List<String> comments = page.getHtml().xpath("//li[@class='gl-item']/div[@class='gl-i-wrap j-sku-item']/div[@class='p-commit']/strong/a/text()").all();
            List<String> links = page.getHtml().xpath("//li[@class='gl-item']/div[@class='gl-i-wrap j-sku-item']/div[@class='p-img']/a/@href").all();
            List<String> top3Pic = page.getHtml().xpath("//li[@class='gl-item']/div[@class='gl-i-wrap j-sku-item']/div[@class='p-img']/a/img/@src").all(); //��ȡҳ���ʼ����ǰ����ͼƬ��ַ
            List<String> lazyPic = page.getHtml().xpath("//li[@class='gl-item']/div[@class='gl-i-wrap j-sku-item']/div[@class='p-img']/a/img/@data-lazy-img").all(); // ��ȡ�����ص�ͼƬ��ַ
            List<String> pics = new ArrayList<>();
            pics.addAll(top3Pic.subList(0, 3)); //��ȡǰ����ͼƬ
            pics.addAll(lazyPic.subList(3, lazyPic.size())); //��ȡ��ǰ����֮���ͼƬ
            String category = page.getHtml().xpath("//div[@id='J_selector']/div[@class='s-title']/h3/b/text()").get();
            if ("".equals(category)) {
                category = page.getHtml().xpath("//div[@id='J_selector']/div[@class='s-title']/h3/em/text()").get();
            }
            page.putField("names", names);
            page.putField("prices", prices);
            page.putField("comments", comments);
            page.putField("links", links);
            page.putField("pics", pics);
            page.putField("category", category);

            // ��ȡ��ǰҳurl
            String url = page.getUrl().get();
            if (url.endsWith(".html")) {// ������url����.html��β���ʾ�Ǵ���Ʒ����ҳ���г�ȡ������Ʒurl����Ϊ����Ʒ�����µ���ҳ,���´����ȡ����Ʒ�����µ���ҳ����������Ʒ��ҳ��������ҳ��Ϣ��ӵ������б�
                // ��ȡ��ҳ��
                String pages = page.getHtml().xpath("//div[@id='J_filter']/div[@class='f-line top']/div[@id='J_topPage']/span[@class='fp-text']/i/text()").get();
                if (StringUtils.isNotEmpty(pages)) {
                    int pageCount = Integer.parseInt(pages);
                    // ����ҳ��Ϣ��ӵ������б�
                    for (int i = 2; i <= pageCount; i++) { //������Ҫ�ų���һҳ,Ĭ�ϵ�һ�δ򿪵�ҳ�漴Ϊ��һҳ����˴ӵ�2ҳ��ʼ���±�Ϊ2
                        String link = buildUrl(url, i);
                        page.addTargetRequest(link);
                    }
                }
            }
        }
    }

    public Site getSite() {
        return site;
    }

    public static void main(String[] args) throws Exception {

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
        JDPipeline jdPipeline = (JDPipeline) applicationContext.getBean("jdPipeline");

        String chromeDriverPath = JDProductProcessor.class.getClassLoader().getResource("chromedriver.exe").getFile();

        Spider.create(new JDProductProcessor())
                .addUrl("http://www.jd.com/allSort.aspx")// JDȫ������
                .addPipeline(jdPipeline)
                .setDownloader(new SeleniumDownloader(chromeDriverPath))
                .thread(5)
                .run();

    }

    /**
     * ������Ʒ��ҳurl
     *
     * @param str
     * @return
     */
    private static String buildUrl(String str, int pageNumber) {
        String link = "http://list.jd.com/list.html?cat=";
        str = str.replace("http://list.jd.com/", "").replace(".html", "");
        if (str.contains("-")) {
            str = str.replace("-", ",");
            link = link + str + "&page=" + pageNumber;
        }
        return link;
    }

}
