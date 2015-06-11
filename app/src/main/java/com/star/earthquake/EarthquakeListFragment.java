package com.star.earthquake;


import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class EarthquakeListFragment extends ListFragment {

    private ArrayAdapter<Quake> arrayAdapter;
    private ArrayList<Quake> earthquakes;

    private static final String TAG = "EARTHQUAKE";
    private Handler handler = new Handler();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        earthquakes = new ArrayList<>();

        arrayAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, earthquakes);

        setListAdapter(arrayAdapter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshEarthquakes();
            }
        }).start();
    }

    public void refreshEarthquakes() {

        URL url;

        try {
            url = new URL(getString(R.string.quake_feed));

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = httpURLConnection.getInputStream();

                DocumentBuilderFactory documentBuilderFactory =
                        DocumentBuilderFactory.newInstance();

                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

                Document document = documentBuilder.parse(inputStream);
                Element element = document.getDocumentElement();

                earthquakes.clear();

                NodeList nodeList = element.getElementsByTagName("entry");
                if ((nodeList != null) && (nodeList.getLength() > 0)) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Element entry = (Element) nodeList.item(i);

                        Element title = (Element)
                                entry.getElementsByTagName("title").item(0);
                        Element point = (Element)
                                entry.getElementsByTagName("georss:point").item(0);
                        Element when = (Element)
                                entry.getElementsByTagName("updated").item(0);
                        Element link = (Element)
                                entry.getElementsByTagName("link").item(0);

                        if (entry != null && point != null && when != null && link != null) {
                            String details = title.getFirstChild().getNodeValue();

                            String linkString = link.getAttribute("href");

                            String pointString = point.getFirstChild().getNodeValue();

                            String whenString = when.getFirstChild().getNodeValue();

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                    "yyyy-MM-dd'T'hh:mm:ss'Z'"
                            );

                            Date quakeDate = new Date();

                            try {
                                quakeDate = simpleDateFormat.parse(whenString);
                            } catch (ParseException e) {
                                Log.d(TAG, "ParseException");
                            }

                            String[] points = pointString.split(" ");
                            Location location = new Location("dummyGPS");
                            location.setLatitude(Double.parseDouble(points[0]));
                            location.setLongitude(Double.parseDouble(points[0]));

                            String magnitudeString = details.split(" ")[1];
                            int end = magnitudeString.length() - 1;
                            double magnitude = Double.parseDouble(magnitudeString.substring(0, end));

                            details = details.split(",")[1].trim();

                            final Quake quake = new Quake(
                                    quakeDate, details, location, magnitude, linkString
                            );

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    addNewQuake(quake);
                                }
                            });
                        }
                    }
                }
            }

        } catch (MalformedURLException e) {
            Log.d(TAG, "MalformedURLException");
        } catch (IOException e) {
            Log.d(TAG, "IOException");
        } catch (ParserConfigurationException e) {
            Log.d(TAG, "ParserConfigurationException");
        } catch (SAXException e) {
            Log.d(TAG, "SAXException");
        }

    }

    private void addNewQuake(Quake quake) {

        EarthquakeActivity earthquakeActivity = (EarthquakeActivity) getActivity();

        if (quake.getMagnitude() >= earthquakeActivity.getMinMag()) {
            earthquakes.add(quake);
        }

        arrayAdapter.notifyDataSetChanged();
    }
}
