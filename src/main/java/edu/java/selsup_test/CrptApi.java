package edu.java.selsup_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.util.Date;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private Date lastResetTime = new Date();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void createDocument(Object document)
            throws IOException, InterruptedException, ParseException {

        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            long timePassed = currentTime - lastResetTime.getTime();
            if (timePassed >= timeUnit.toMillis(1)) {
                requestCounter.set(0);
                lastResetTime = new Date(currentTime);
            }

            while (requestCounter.get() >= requestLimit) {
                wait(timeUnit.toMillis(1) - timePassed);
                currentTime = System.currentTimeMillis();
                timePassed = currentTime - lastResetTime.getTime();

                if (timePassed >= timeUnit.toMillis(1)) {
                    requestCounter.set(0);
                    lastResetTime = new Date(currentTime);
                }
            }

            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(document);

            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);

            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity, "UTF-8");

            requestCounter.incrementAndGet();
            System.out.println("Document creation response: " + responseString);
        }
    }

    public static void main(String[] args)
            throws IOException, InterruptedException, ParseException {

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        // Создание объекта Description
        Description description = new Description("string");

        // Создание объектов Product
        Product product = new Product("string", "2020-01-23", "string", "string", "string", "2020-01-23", "string", "string", "string");
        List<Product> products = new ArrayList<>();
        products.add(product);

        // Создание объекта Document
        Document document = new Document(description, "string", "string", "LP_INTRODUCE_GOODS", true, "string", "string", "string", "2020-01-23", "string", products, "2020-01-23", "string");

        crptApi.createDocument(document);
    }
}
