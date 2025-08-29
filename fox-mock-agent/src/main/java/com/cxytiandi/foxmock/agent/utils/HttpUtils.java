package com.cxytiandi.foxmock.agent.utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class HttpUtils {

	public static String get(String url) {
		HttpURLConnection connection = null;
		try {
			URL getUrl = new URL(url);
			connection = (HttpURLConnection) getUrl.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "*/*");
			connection.setRequestProperty("User-Agent",
							"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; CIBA)");
			connection.setRequestProperty("Accept-Language", "zh-cn");
			connection.connect();
			return IOUtils.toString(connection.getInputStream(), "UTF-8");
		} catch (Exception e) {
			System.out.println("http request exception, url is {}");
		} finally {
			if (Objects.nonNull(connection)) {
				connection.disconnect();
			}
		}

		return null;
	}
	
}
