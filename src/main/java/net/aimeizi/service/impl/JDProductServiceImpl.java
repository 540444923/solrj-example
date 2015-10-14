package net.aimeizi.service.impl;

import net.aimeizi.domain.Product;
import net.aimeizi.service.JDProductService;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Administrator on 2015/10/14.
 */
@Service
public class JDProductServiceImpl implements JDProductService {

    @Autowired
    SolrServer solrServer;

    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    @Override
    public Map<String, Object> query(String queryString, String sort, int start, int pageSize) throws Exception {
        Map<String, Object> maps = new HashMap<String, Object>();
        List<Product> productList = new ArrayList<>();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("name:" + queryString); // *name:queryString*�����Ʋ�ѯ
        start = (start - 1) * pageSize;
        solrQuery.setStart(start); //������ʼλ��
        solrQuery.setRows(pageSize); // ����ҳ��С
        solrQuery.setHighlight(true); //���ø���
        solrQuery.addHighlightField("name"); //���ø����ֶ�
        solrQuery.addHighlightField("category"); //���ø����ֶ�
        solrQuery.setHighlightFragsize(200); // ���ø������ݴ�С
        solrQuery.setHighlightSimplePre("<em>"); //���ø���ǰ׺
        solrQuery.setHighlightSimplePost("</em>"); //���ø�����׺
        sort = StringUtils.isNotEmpty(sort) ? sort : "score";
        solrQuery.addSort(sort, SolrQuery.ORDER.desc); //�������� ��score��������
        QueryResponse queryResponse = solrServer.query(solrQuery);
        int qtime = queryResponse.getQTime();//��ѯ����ʱ��
        SolrDocumentList solrDocumentList = queryResponse.getResults();// ��ȡ��ѯ�����
        // ��ȡ�������� ��һ��Map�ļ����ĵ���ID���ڶ���Map�ļ��Ǹ�����ʾ���ֶ���
        Map<String, Map<String, List<String>>> highlightingMaps = queryResponse.getHighlighting();
        long totals = solrDocumentList.getNumFound();// ��ѯ�����ܼ�¼��
        if (!solrDocumentList.isEmpty()) {
            Iterator<SolrDocument> it = solrDocumentList.iterator();
            while (it.hasNext()) {
                SolrDocument solrDocument = it.next();
                // ��ȡ�ĵ�id
                String id = solrDocument.getFieldValue("id").toString();
                // �������
                Map<String, List<String>> highlightFieldMap = highlightingMaps.get(id);
                if (!highlightFieldMap.isEmpty()) {
                    List<String> highlightName = highlightFieldMap.get("name");
                    List<String> highlightCategory = highlightFieldMap.get("category");
                    if (highlightName != null && !highlightName.isEmpty()) {
                        String name = highlightName.get(0);
                        // ���ĵ�������е�name����Ϊ�������name
                        solrDocument.setField("name", name);
                    }
                    if (highlightCategory != null && !highlightCategory.isEmpty()) {
                        String category = highlightCategory.get(0);
                        // ���ĵ�������е�category����Ϊ�������category
                        solrDocument.setField("category", category);
                    }
                }
                // ����solrDocumentתjava bean
                Product product = doc2bean(solrDocument);
                String picture = product.getPic();
                // ����ͼƬ��ַΪ�ջ�ͼƬ��ַ��Ч
                if (StringUtils.isEmpty(picture) || "done".equals(picture)) {
                    product.setPic("images/nopicture.png");
                }
                productList.add(product);
            }
        }
        maps.put("qtime", qtime);
        maps.put("totals", totals);
        maps.put("results", productList);
        return maps;
    }

    /**
     * solrDocument��java beanת��
     *
     * @param solrDocument
     * @return
     */
    private Product doc2bean(SolrDocument solrDocument) {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        Product product = binder.getBean(Product.class, solrDocument);
        return product;
    }

    /**
     * solrDocument��java bean ����ת��
     *
     * @param solrDocumentList
     * @return
     */
    private List<Product> doc2beans(SolrDocumentList solrDocumentList) {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        List<Product> productList = binder.getBeans(Product.class, solrDocumentList);
        return productList;
    }

    /**
     * bean��SolrInputDocumentת��
     *
     * @param product
     * @return
     */
    private SolrInputDocument bean2doc(Product product) {
        DocumentObjectBinder binder = new DocumentObjectBinder();
        SolrInputDocument solrInputDocument = binder.toSolrInputDocument(product);
        return solrInputDocument;
    }

}
