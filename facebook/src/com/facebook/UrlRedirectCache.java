package com.facebook;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

class UrlRedirectCache {
    static final String TAG = UrlRedirectCache.class.getSimpleName();
    private static final String REDIRECT_CONTENT_TAG = TAG + "_Redirect";

    private volatile static FileLruCache urlRedirectCache;

    synchronized static FileLruCache getCache(Context context) throws IOException{
        if (urlRedirectCache == null) {
            urlRedirectCache = new FileLruCache(context.getApplicationContext(), TAG, new FileLruCache.Limits());
        }
        return urlRedirectCache;
    }

    static URL getRedirectedUrl(Context context, URL url) {
        if (url == null) {
            return null;
        }

        String urlString = url.toString();
        URL finalUrl = null;
        InputStreamReader reader = null;
        try {
            InputStream stream;
            FileLruCache cache = getCache(context);
            boolean redirectExists = false;
            while ((stream = cache.get(urlString, REDIRECT_CONTENT_TAG)) != null) {
                redirectExists = true;

                // Get the redirected url
                reader = new InputStreamReader(stream);
                char[] buffer = new char[128];
                int bufferLength;
                StringBuilder urlBuilder = new StringBuilder();
                while ((bufferLength = reader.read(buffer, 0, buffer.length)) > 0) {
                    urlBuilder.append(buffer, 0, bufferLength);
                }
                Utility.closeQuietly(reader);

                // Iterate to the next url in the redirection
                urlString = urlBuilder.toString();
            }

            if (redirectExists) {
                finalUrl = new URL(urlString);
            }
        } catch (MalformedURLException e) {
            // caching is best effort, so ignore the exception
        } catch (IOException ioe) {
        } finally {
            Utility.closeQuietly(reader);
        }

        return finalUrl;
    }

    static void cacheUrlRedirect(Context context, URL fromUrl, URL toUrl) {
        if (fromUrl == null || toUrl == null) {
            return;
        }

        OutputStream redirectStream = null;
        try {
            FileLruCache cache = getCache(context);
            redirectStream = cache.openPutStream(fromUrl.toString(), REDIRECT_CONTENT_TAG);
            redirectStream.write(toUrl.toString().getBytes());
        } catch (IOException e) {
            // Caching is best effort
        } finally {
            Utility.closeQuietly(redirectStream);
        }
    }
}